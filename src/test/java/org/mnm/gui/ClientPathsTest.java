package org.mnm.gui;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.config.Client;

import static java.nio.file.Files.createDirectories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.client.ClientRunner.DEFAULT_MNM_PREFIX;
import static org.mnm.config.Client.Status.UPDATED;

class ClientPathsTest {

    private static final String GAME_SETTINGS = "drive_c/users/steamuser/AppData/LocalLow/Niche Worlds Cult/Monsters and Memories";
    private static final String WINDOWS_PREFIX = "C:/steam-compat-data";

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldUseSteamCompatibilityDataPathOnWindows() throws IOException {
        ClientPaths paths = new ClientPaths(null, true);
        String expected = WINDOWS_PREFIX + "/pfx/" + GAME_SETTINGS;
        Path expectedPath = Path.of(expected);

        try {
            Files.createDirectories(expectedPath);
            assertThat(paths.getGameSettingsPath()).isEqualTo(expected);
        } finally {
            deleteRecursively(Path.of(WINDOWS_PREFIX));
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldReportMissingInstallationOnWindows() {
        deleteRecursively(Path.of(WINDOWS_PREFIX));
        ClientPaths paths = new ClientPaths(null, true);

        assertThat(paths.getGameSettingsPath()).isEqualTo("Error: installation not found, run Install or Repair");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldUseClientDirectoryAsWinePrefixOnLinux(@TempDir Path tempDir) throws IOException {
        Client client = new Client("mnm", "1.2.3", UPDATED, tempDir);
        Path gameSettingsPath = tempDir.resolve(DEFAULT_MNM_PREFIX).resolve("pfx").resolve(GAME_SETTINGS);
        createDirectories(gameSettingsPath);
        ClientPaths paths = new ClientPaths(new ClientStatus(client, true, null), true);

        String expected = client.path().toAbsolutePath() + "/" + DEFAULT_MNM_PREFIX + "/pfx/" + GAME_SETTINGS;

        assertThat(paths.getGameSettingsPath()).isEqualTo(expected);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldReportMissingInstallation(@TempDir Path tempDir) {
        Client client = new Client("mnm", "1.2.3", UPDATED, tempDir);
        ClientPaths paths = new ClientPaths(new ClientStatus(client, true, null), true);

        assertThat(paths.getGameSettingsPath()).isEqualTo("Error: installation not found, run Install or Repair");
    }

    static boolean deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                    for (Path child : children) {
                        deleteRecursively(child);
                    }
                }
            }
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }
}
