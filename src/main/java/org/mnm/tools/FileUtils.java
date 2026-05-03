package org.mnm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static void assemble(List<byte[]> fileSections, Path destination) {
        long start = System.currentTimeMillis();
        try (OutputStream out = Files.newOutputStream(destination)) {
            for (byte[] section : fileSections) {
                if (section != null && section.length > 0) {
                    out.write(section);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.debug("assembled file: {} {}ms", destination, System.currentTimeMillis() - start);
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

}
