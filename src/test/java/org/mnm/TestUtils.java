package org.mnm;

import org.mnm.tools.FileUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {

    public static Path classpathFile(String path) {
        final ClassLoader classLoader = FileUtils.class.getClassLoader();
        try {
            URI uri = classLoader.getResource(path).toURI();
            return Paths.get(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
