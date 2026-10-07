package org.mnm.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.mnm.tools.ByteUtils.readAllBytes;

public class Zstd {

    private Zstd() {
    }

    public class InMemory {

        private InMemory() {
        }

        /**
         * @param destination     final location of the assembled file.
         * @param sections        sources to assemble into a single file.
         * @param sectionCallback callback to run when a section has been processed (yes, hacky solution).
         */
        public static void decompress(Path destination, Section[] sections, Consumer<Section> sectionCallback) {
            if (sections.length == 0) {
                throw new IllegalArgumentException("sections must not be empty");
            }

            try (OutputStream out = Files.newOutputStream(destination, CREATE, TRUNCATE_EXISTING)) {
                for (Section section : sections) {
                    byte[] bytesUncompressed = new byte[section.size];
                    byte[] src = readAllBytes(section.source);
                    com.github.luben.zstd.Zstd.decompress(bytesUncompressed, src);
                    out.write(bytesUncompressed);
                    sectionCallback.accept(section);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public record Section(Path source, int size) {
    }
}
