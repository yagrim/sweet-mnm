package org.mnm.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class FileUtils {

    public static List<Path> getAllFiles(Path base) {
        if (!base.toFile().exists()) {
            return new ArrayList<>();
        }
        try (var stream = Files.walk(base)) {
            return stream
                .filter(Files::isRegularFile)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean fileExists(Path downloadPath) {
        File file = downloadPath.toFile();
        return file.exists() && file.length() > 0;
    }

    public static void createDirectories(Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            double kb = bytes / 1024.0;
            return String.format("%.2f KB", kb);
        } else {
            double mb = bytes / (1024.0 * 1024);
            return String.format("%.2f MB", mb);
        }
    }

    public static String readFromClasspath(String path) {
        final InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(path);
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readFromClasspathAsArray(String path) {
        final InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(path);
        try {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
