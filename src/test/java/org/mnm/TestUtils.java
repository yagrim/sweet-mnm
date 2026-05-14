package org.mnm;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class TestUtils {

    public static Path classpathFile(String path) {
        final ClassLoader classLoader = new TestUtils().getClass().getClassLoader();
        try {
            URI uri = classLoader.getResource(path).toURI();
            return Paths.get(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendToFile(Path path, String content) {
        try {
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deletePath(Path path) {
        try {
            FileUtils.forceDelete(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
