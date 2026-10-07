package org.mnm.tools;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.TestUtils.classpathFile;

class ZstdTest {

    public static final Consumer<Zstd.Section> NO_OP = c -> {
    };


    @Test
    void shouldDecompressNewFile(@TempDir Path tempDir) {
        final Path source = classpathFile("test-file-1.zst");
        final Path destination = tempDir.resolve("output.txt");

        assertThat(destination).doesNotExist();
        Zstd.Section[] section = new Zstd.Section[]{new Zstd.Section(source, 70)};
        Zstd.InMemory.decompress(destination, section, NO_OP);

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
        var sections = new Zstd.Section[]{
            new Zstd.Section(classpathFile("test-file-1.zst"), 70),
            new Zstd.Section(classpathFile("test-file-2.zst"), 30)
        };
        Zstd.InMemory.decompress(destination, sections, NO_OP);

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

    @Test
    void shouldInvokeCallback(@TempDir Path tempDir) {
        final Path destination = tempDir.resolve("output.txt");

        AtomicReference<Integer> signal = new AtomicReference<>(0);

        assertThat(destination).doesNotExist();
        var sections = new Zstd.Section[]{
            new Zstd.Section(classpathFile("test-file-1.zst"), 70),
            new Zstd.Section(classpathFile("test-file-2.zst"), 30)
        };
        Zstd.InMemory.decompress(destination, sections, c -> signal.set(signal.get() + c.size()));

        assertThat(signal.get()).isEqualTo(100);
    }

}
