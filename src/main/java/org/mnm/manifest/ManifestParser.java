package org.mnm.manifest;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.mnm.tools.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManifestParser {

    private static final Logger logger = LoggerFactory.getLogger(ManifestParser.class);

    public List<Manifest.File> parse(Path manifestPath) {
        long init = System.currentTimeMillis();
        try (Reader reader = Files.newBufferedReader(manifestPath)) {
            Manifest manifest = JsonParser.read(reader, Manifest.class);
            logger.info("Manifest parsed: found {} entries ({} ms)", manifest.manifest().size(), System.currentTimeMillis() - init);
            return manifest.manifest();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse manifest: " + manifestPath, e);
        }
    }
}
