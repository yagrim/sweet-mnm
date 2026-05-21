package org.mnm.launcher;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.LauncherTestDatabase.*;

class LauncherDbTest {

    @Test
    void shouldReadSettings() throws Exception {
        try (LauncherDb db = new LauncherDb(fromClasspath())) {

            Map<String, String> settings = db.getSettings();

            assertThat(settings)
                .containsEntry("current_game", "mnm")
                .containsEntry("remember", "true")
                .containsEntry("token", INITIAL_TOKEN)
                .containsEntry("username", "a-username@some-email.com")
                .hasSize(4);
        }
    }

    @Test
    void shouldUpdateSettings(@TempDir Path tempDir) throws Exception {
        final TestDatabase testDb = withSettings(tempDir);
        try (LauncherDb db = new LauncherDb(testDb.path())) {
            db.updateSetting("token", "999.999.999");
        }

        try (LauncherDb launcherDb = new LauncherDb(testDb.path())) {
            assertThat(launcherDb.getSettings())
                .containsEntry("current_game", "mnm")
                .containsEntry("remember", "true")
                .containsEntry("token", "999.999.999")
                .containsEntry("username", "a-username@some-email.com")
                .hasSize(4);
        }
    }

    @Test
    void shouldUpdateSettingsToEmpty(@TempDir Path tempDir) throws Exception {
        final TestDatabase testDb = withSettings(tempDir);
        try (LauncherDb db = new LauncherDb(testDb.path())) {
            db.updateSetting("token", "");
        }

        try (LauncherDb launcherDb = new LauncherDb(testDb.path())) {
            assertThat(launcherDb.getSettings())
                .containsEntry("current_game", "mnm")
                .containsEntry("remember", "true")
                .containsEntry("token", "")
                .containsEntry("username", "a-username@some-email.com")
                .hasSize(4);
        }
    }

    @Test
    void shouldUpdateSettingsToNull(@TempDir Path tempDir) throws Exception {
        final TestDatabase testDb = withSettings(tempDir);
        try (LauncherDb db = new LauncherDb(testDb.path())) {
            db.updateSetting("token", null);
        }

        try (LauncherDb launcherDb = new LauncherDb(testDb.path())) {
            assertThat(launcherDb.getSettings())
                .containsEntry("current_game", "mnm")
                .containsEntry("remember", "true")
                .containsEntry("token", null)
                .containsEntry("username", "a-username@some-email.com")
                .hasSize(4);
        }
    }

    @Test
    void shouldFailIfFileCantBeFound() {
        Throwable throwable = catchThrowable(() -> new LauncherDb(Path.of("some-file.db")));

        assertThat(throwable)
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining("some-file.db");
    }

}
