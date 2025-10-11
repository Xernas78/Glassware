package dev.xernas;

import dev.xernas.lithium.plugin.listeners.Listener;
import dev.xernas.lithium.request.Request;
import dev.xernas.lithium.response.Response;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReloadChecker implements Listener {

    private final Glassware glassware;
    private final Path staticPath;

    public ReloadChecker(Glassware glassware, Path staticPath) {
        this.glassware = glassware;
        this.staticPath = staticPath;
    }

    @Override
    public void onRequest(Request request) {
        String path = request.path().endsWith("/") ? request.path() + "index.html" : request.path();
        path = path.startsWith("/") ? path.substring(1) : path;
        Path requestedFile = staticPath.resolve(path);
        if (Files.exists(requestedFile))
            if (!glassware.pathExists(requestedFile))
                glassware.reloadWebRoutes(staticPath);
    }

    @Override
    public void onResponse(Response response) {

    }

}
