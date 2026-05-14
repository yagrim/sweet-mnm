package org.mnm.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.mnm.tools.ByteUtils.readAllBytes;

public class Zstd {

    private Zstd() {
    }

    public class InMemory {

        private InMemory() {
        }

        public static void decompress(Path destination, Section... sections) {
            if (sections.length == 0) {
                throw new IllegalArgumentException("sections must not be empty");
            }

            try (OutputStream out = Files.newOutputStream(destination, CREATE, TRUNCATE_EXISTING)) {
                for (Section section : sections) {
                    byte[] bytesUncompressed = new byte[section.size];
                    byte[] src = readAllBytes(section.source);
                    com.github.luben.zstd.Zstd.decompress(bytesUncompressed, src);
                    out.write(bytesUncompressed);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public record Section(Path source, int size) {
    }
}
