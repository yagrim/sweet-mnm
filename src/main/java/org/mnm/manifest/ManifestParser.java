package org.mnm.manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

public class ManifestParser {

    private static final Logger logger = LoggerFactory.getLogger(ManifestParser.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<Manifest.File> parse(Path manifestPath) {
        long init = System.currentTimeMillis();
        Manifest manifest = objectMapper.readValue(manifestPath, Manifest.class);
        logger.info("Manifest parsed: found {} entries, {} ms", manifest.manifest().size(), System.currentTimeMillis() - init);
        return manifest.manifest();
    }

}
