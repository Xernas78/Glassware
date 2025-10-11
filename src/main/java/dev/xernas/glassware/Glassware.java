package dev.xernas.glassware;

import dev.xernas.atom.file.FileUtils;
import dev.xernas.atom.resource.ResourceUtils;
import dev.xernas.lithium.io.http.Status;
import dev.xernas.lithium.plugin.Plugin;
import dev.xernas.lithium.plugin.PluginManager;
import dev.xernas.lithium.plugin.Priority;
import dev.xernas.lithium.plugin.listeners.ListenerManager;
import dev.xernas.lithium.plugin.routes.RouteManager;
import dev.xernas.lithium.response.HTMLResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

public class Glassware implements Plugin {

    private final RouteManager routeManager = PluginManager.getRouteManager(this);
    private final Map<String, WebFile> webFilePaths = new HashMap<>();

    public static final HTMLResponse NOT_FOUND_RESPONSE = new HTMLResponse("<h1>404 Not Found</h1><br><p>The requested resource was not found on this server.</p>", Status.NOT_FOUND);
    public static final HTMLResponse METHOD_NOT_ALLOWED_RESPONSE = new HTMLResponse("<h1>405 Method Not Allowed</h1><br><p>This endpoint only supports GET requests.</p>", Status.METHOD_NOT_ALLOWED);

    @Override
    public void onEnable() {
        System.out.println("[GLASSWARE] Glassware plugin enabled");
        System.out.println("[GLASSWARE] Loading web files");
        final Path glasswareData = FileUtils.directories(PluginManager.getPluginDataFolder(this), Path.of("static"));
        loadRoutes(glasswareData);
        System.out.println("[GLASSWARE] Loaded " + webFilePaths.size() + " web files");

        ListenerManager listenerManager = PluginManager.getListenerManager(this);
        listenerManager.registerListener(new ReloadChecker(this, glasswareData));

        PluginManager.setFallbackResponse(NOT_FOUND_RESPONSE);
    }

    @Override
    public void onDisable() {
        System.out.println("[GLASSWARE] Glassware plugin disabled");
    }

    @Override
    public Priority getPriority() {
        return Priority.HIGHEST;
    }

    public boolean pathExists(Path path) {
        return webFilePaths.values().stream().anyMatch(webFile -> webFile.getPath().equals(path));
    }

    public void reloadWebRoutes(Path staticPath) {
        webFilePaths.clear();
        routeManager.clearRoutes();
        loadRoutes(staticPath);
        System.out.println("[GLASSWARE] Reloaded " + webFilePaths.size() + " web files");
    }

    public void loadRoutes(final Path staticPath) {
        webFilePaths.putAll(scanForPaths(staticPath, 5));
        webFilePaths.forEach((path, file) -> {
            String webPath = path.replace(staticPath.toString(), "").replace("\\", "/");
            URI webUri = URI.create(webPath);
            URI staticPathUri = staticPath.toUri();
            URI relativeWebPath = staticPathUri.relativize(webUri);
            String finalWebPath = relativeWebPath.getPath();

            WebFileHandler handler = new WebFileHandler(this, staticPath, file);
            if (finalWebPath.toLowerCase().endsWith("/index.html")) {
                String finalWebPathWithoutIndex = finalWebPath.substring(0, finalWebPath.length() - "index.html".length());
                routeManager.registerRoute(handler, finalWebPathWithoutIndex, finalWebPathWithoutIndex + "index", finalWebPath);
            }
            else routeManager.registerRoute(handler, finalWebPath);
        });
    }

    private Map<String, WebFile> scanForPaths(Path directory, int depth) {
        Map<String, WebFile> paths = new HashMap<>();
        if (depth < 0) return paths;
        try {
            for (Path resource : ResourceUtils.list(directory)) {
                if (resource.toFile().isFile()) {
                    String fileName = resource.getFileName().toString();
                    WebFile webFile = new WebFile(fileName, resource);
                    webFile.read(resource);
                    paths.put(resource.toString(), webFile);
                }
                if (resource.toFile().isDirectory()) {
                    paths.putAll(scanForPaths(resource, depth - 1));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return paths;
    }

}
