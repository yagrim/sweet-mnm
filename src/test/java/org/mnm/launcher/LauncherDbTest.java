package org.mnm.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.LauncherTestDatabase.*;

class LauncherDbTest {

    @Test
    void shouldReadSettings() throws FileNotFoundException {
        final LauncherDb db = new LauncherDb(fromClasspath());

        Map<String, String> settings = db.getSettings();

        assertThat(settings)
                .containsEntry("current_game", "mnm")
                .containsEntry("remember", "true")
                .containsEntry("token", INITIAL_TOKEN)
                .containsEntry("username", "a-username@some-email.com")
                .hasSize(4);
    }

    @Test
    void shouldUpdateSettings(@TempDir Path tempDir) throws Exception {
        final Path dbCopy = copyTestDb(tempDir);
        final LauncherDb db = new LauncherDb(dbCopy);

        db.updateSetting("token", "999.999.999");
        db.close();

        try (LauncherDb launcherDb = new LauncherDb(dbCopy)) {
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
        final Path dbCopy = copyTestDb(tempDir);
        final LauncherDb db = new LauncherDb(dbCopy);

        db.updateSetting("token", "");
        db.close();

        try (LauncherDb launcherDb = new LauncherDb(dbCopy)) {
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
        final Path dbCopy = copyTestDb(tempDir);
        final LauncherDb db = new LauncherDb(dbCopy);

        db.updateSetting("token", null);
        db.close();

        try (LauncherDb launcherDb = new LauncherDb(dbCopy)) {
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
