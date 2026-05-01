package org.mnm.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileUtils {

    public static void assemble(List<byte[]> fileSections, Path destination) {
        try (OutputStream out = Files.newOutputStream(destination)) {
            for (byte[] section : fileSections) {
                if (section != null && section.length > 0) {
                    out.write(section);
                }
            }
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
}
