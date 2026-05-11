package org.mnm;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
}
