package org.mnm.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyCacheHandlerTest {

    private final LegacyCacheHandler handler = new LegacyCacheHandler();

    @Test
    void shouldDeleteDownloadsFolderOfEachClientEvenWhenFilesAreNew(@TempDir Path tempDir) throws IOException {
        Path sharedPath = tempDir.resolve("shared");
        Path otherPath = tempDir.resolve("other");
        Path newFile = legacyFile(sharedPath, "bundles/a.bin", "123");
        Path otherFile = legacyFile(otherPath, "b.bin", "12");

        CacheCleaner.Result result = handler.deleteLegacyDownloads(List.of(sharedPath, otherPath, sharedPath));

        assertThat(result).isEqualTo(new CacheCleaner.Result(2, 5));
        assertThat(newFile).doesNotExist();
        assertThat(otherFile).doesNotExist();
        assertThat(sharedPath.resolve("downloads")).doesNotExist();
        assertThat(sharedPath).isDirectory();
    }

    @Test
    void shouldNotDeleteOtherFilesOfTheClient(@TempDir Path tempDir) throws IOException {
        legacyFile(tempDir, "a.bin", "123");
        Path gameFile = Files.writeString(tempDir.resolve("game.bin"), "game");

        handler.deleteLegacyDownloads(List.of(tempDir));

        assertThat(gameFile).exists();
    }

    @Test
    void shouldIgnoreMissingDownloadsFolder(@TempDir Path tempDir) {
        CacheCleaner.Result result = handler.deleteLegacyDownloads(List.of(tempDir.resolve("missing")));

        assertThat(result).isEqualTo(new CacheCleaner.Result(0, 0));
    }

    @Test
    void shouldReturnEmptyResultWhenThereAreNoClients() {
        assertThat(handler.deleteLegacyDownloads(List.of())).isEqualTo(new CacheCleaner.Result(0, 0));
    }

    private static Path legacyFile(Path clientPath, String name, String content) throws IOException {
        Path file = clientPath.resolve("downloads").resolve(name);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }
}
