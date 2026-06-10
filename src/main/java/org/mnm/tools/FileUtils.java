package org.mnm.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.mnm.gui.GuiCommand;


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

    public static long getFolderSize(Path dir) {
        try (Stream<Path> stream = Files.walk(dir, 2)) {
            return stream
                .filter(Files::isRegularFile)
                .mapToLong(path -> path.toFile().length())
                .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    public static void deleteFolder(Path folder) throws IOException {
        if (!Files.exists(folder)) return;

        try (Stream<Path> stream = Files.walk(folder)) {
            stream.sorted(Comparator.reverseOrder())
                .filter(path -> !path.equals(folder))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }

    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
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

    public static Path installClasspathResource(String resource) {
        try (InputStream inputStream = GuiCommand.class.getClassLoader().getResourceAsStream(resource)) {
            if (inputStream == null) {
                ProcessUtils.panic(resource + " not found on the classpath");
            }
            Path fontconfig = Files.createTempFile("fontconfig-", ".properties");
            Files.copy(inputStream, fontconfig, StandardCopyOption.REPLACE_EXISTING);
            fontconfig.toFile().deleteOnExit();
            return fontconfig.toAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resource + " from the classpath", e);
        }
    }
}
