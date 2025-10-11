package dev.xernas;

import dev.xernas.lithium.io.headers.ContentType;
import dev.xernas.lithium.io.headers.Header;
import dev.xernas.lithium.io.http.Method;
import dev.xernas.lithium.io.http.Status;
import dev.xernas.lithium.plugin.PluginManager;
import dev.xernas.lithium.plugin.commands.Command;
import dev.xernas.lithium.plugin.commands.CommandManager;
import dev.xernas.lithium.plugin.routes.RouteHandler;
import dev.xernas.lithium.request.Request;
import dev.xernas.lithium.response.HTMLResponse;
import dev.xernas.lithium.response.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebFileHandler implements RouteHandler {

    private final Glassware glassware;
    private final Path staticPath;
    private final WebFile file;

    private boolean triedReloading = false;

    public WebFileHandler(Glassware glassware, Path staticPath, WebFile file) {
        this.glassware = glassware;
        this.staticPath = staticPath;
        this.file = file;
    }

    @Override
    public Response handle(Request request) {
        if (request.method() != Method.GET) return Glassware.METHOD_NOT_ALLOWED_RESPONSE;
        try {
            // BE WARY, avoid calling it at every request
            file.read(file.getPath());
        } catch (IOException ignore) {
            if (!triedReloading) {
                glassware.reloadWebRoutes(staticPath);
                triedReloading = true;
                return handle(request);
            }
            return Glassware.NOT_FOUND_RESPONSE;
        }
        triedReloading = false;
        return new Response() {
            @Override
            public ContentType getContentType() {
                return file.getContentType();
            }

            @Override
            public List<Header> getHeaders() {
                return List.of();
            }

            @Override
            public byte[] getBody() {
                if (getContentType() != ContentType.TEXT_HTML) return file.getContent();
                String fileContent = new String(file.getContent(), StandardCharsets.UTF_8);
                return replaceBraced(fileContent, key -> {
                    if (key.isEmpty()) return key;
                    String[] parts = key.split(" ");
                    String commandName = parts[0];
                    String[] commandArgs = new String[parts.length - 1];
                    if (parts.length > 1) System.arraycopy(parts, 1, commandArgs, 0, parts.length - 1);
                    CommandManager commandManager = PluginManager.getCommandManager();
                    Command command = commandManager.getCommand(commandName);
                    if (command == null) return "[ERROR] Command not found: " + commandName;
                    return command.execute(commandArgs);
                }).getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public Status getStatus() {
                return Status.OK;
            }

        };
    }

    private static String replaceBraced(String input, Function<String, String> replacer) {
        Pattern pattern = Pattern.compile("\\{([^}]*)\\}");
        Matcher matcher = pattern.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1); // text inside braces
            String replacement = replacer.apply(key);
            // Escape $ and \ to avoid regex issues
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

}
