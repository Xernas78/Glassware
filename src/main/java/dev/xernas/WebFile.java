package dev.xernas;

import dev.xernas.atom.file.FileUtils;
import dev.xernas.atom.resource.ResourceUtils;
import dev.xernas.lithium.io.headers.ContentType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class WebFile {

    private final ContentType contentType;
    private final Path path;
    private byte[] content;

    public WebFile(String fileName, Path path) {
        this.contentType = getContentType(fileName);
        this.path = path;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public Path getPath() {
        return path;
    }

    public byte[] getContent() {
        return content;
    }

    public void read(Path path) throws IOException {
        content = ResourceUtils.getResourceBytes(path);
    }

    public static ContentType getContentType(String fileName) {
        switch (FileUtils.getFileExtension(fileName)) {
            case "html", "htm" -> {
                return ContentType.TEXT_HTML;
            }
            case "css" -> {
                return ContentType.TEXT_CSS;
            }
            case "js" -> {
                return ContentType.APPLICATION_JAVASCRIPT;
            }
            case "json" -> {
                return ContentType.APPLICATION_JSON;
            }
            case "png" -> {
                return ContentType.IMAGE_PNG;
            }
            case "jpg", "jpeg" -> {
                return ContentType.IMAGE_JPEG;
            }
            case "gif" -> {
                return ContentType.IMAGE_GIF;
            }
            case "svg" -> {
                return ContentType.IMAGE_SVG;
            }
            case "ico" -> {
                return ContentType.IMAGE_ICO;
            }
            case "txt" -> {
                return ContentType.TEXT_PLAIN;
            }
            case "php" -> {
                return ContentType.APPLICATION_PHP;
            }
            default -> {
                return ContentType.TEXT_PLAIN; // Default to plain text if unknown
            }
        }
    }

}
