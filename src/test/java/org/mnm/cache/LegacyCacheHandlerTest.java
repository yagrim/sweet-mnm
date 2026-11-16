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
    void shouldDeleteKnownContentOfEachClientAndKeepDownloadsFolder(@TempDir Path tempDir) throws IOException {
        Path sharedPath = tempDir.resolve("shared");
        Path otherPath = tempDir.resolve("other");
        Path bundle = legacyFile(sharedPath, "bundles/a.bin", "123");
        Path manifest = legacyFile(otherPath, "abc.manifest", "12");

        CacheCleaner.Result result = handler.deleteLegacyDownloads(List.of(sharedPath, otherPath, sharedPath));

        assertThat(result).isEqualTo(new CacheCleaner.Result(2, 5));
        assertThat(bundle).doesNotExist();
        assertThat(sharedPath.resolve("downloads/bundles")).doesNotExist();
        assertThat(manifest).doesNotExist();
        assertThat(sharedPath.resolve("downloads")).isDirectory();
        assertThat(otherPath.resolve("downloads")).isDirectory();
    }

    @Test
    void shouldNotDeleteUnknownContentOfDownloadsFolder(@TempDir Path tempDir) throws IOException {
        Path userFile = legacyFile(tempDir, "report.pdf", "user");
        Path userFolderFile = legacyFile(tempDir, "docs/notes.txt", "notes");
        Path nestedManifest = legacyFile(tempDir, "docs/x.manifest", "nested");
        legacyFile(tempDir, "bundles/a.bin", "123");

        CacheCleaner.Result result = handler.deleteLegacyDownloads(List.of(tempDir));

        assertThat(result).isEqualTo(new CacheCleaner.Result(1, 3));
        assertThat(userFile).exists();
        assertThat(userFolderFile).exists();
        assertThat(nestedManifest).exists();
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
