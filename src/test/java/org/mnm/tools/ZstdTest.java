package org.mnm.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.TestUtils.classpathFile;

class ZstdTest {

    @Test
    void shouldDecompressNewFile(@TempDir Path tempDir) {
        final Path source = classpathFile("test-file-1.zst");
        final Path destination = tempDir.resolve("output.txt");

        assertThat(destination).doesNotExist();
        Zstd.InMemory.decompress(destination, new Zstd.Section(source, 70));

        assertThat(destination)
                .hasContent("""
                        ----
                        Hello test!!
                        Generated with 'zstd --compress test-file.txt'
                        ----
                        """)
                .hasSize(70);

    }

    @Test
    void shouldDecompressAndAppendToExistingFile(@TempDir Path tempDir) {
        final Path destination = tempDir.resolve("output.txt");

        assertThat(destination).doesNotExist();
        Zstd.InMemory.decompress(destination,
                new Zstd.Section(classpathFile("test-file-1.zst"), 70),
                new Zstd.Section(classpathFile("test-file-2.zst"), 30)
        );

        assertThat(destination)
                .hasContent("""
                        ----
                        Hello test!!
                        Generated with 'zstd --compress test-file.txt'
                        ----
                        ----
                        Another test file!!
                        ----
                        """)
                .hasSize(100);

    }

}
