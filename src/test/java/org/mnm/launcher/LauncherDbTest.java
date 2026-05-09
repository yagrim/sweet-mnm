package org.mnm.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.TestUtils.classpathFile;

class LauncherDbTest {

    @Test
    void shouldReadSettings() throws FileNotFoundException {
        final LauncherDb db = new LauncherDb(classpathFile("launcher_test_db.db"));

        Map<String, String> settings = db.getSettings();

        assertThat(settings)
                .containsEntry("current_game", "mnm")
                .containsEntry("remember", "true")
                .containsEntry("token", "123.456.789")
                .containsEntry("username", "a-username@some-email.com")
                .hasSize(4);
    }

    @Test
    void shouldUpdateSettings(@TempDir Path tempDir) throws Exception {
        final Path dbCopy = copyTestDb(tempDir);
        final LauncherDb db = new LauncherDb(dbCopy);

        db.updateSettings("token", "999.999.999");
        db.close();

        assertThat(new LauncherDb(dbCopy).getSettings())
                .containsEntry("current_game", "mnm")
                .containsEntry("remember", "true")
                .containsEntry("token", "999.999.999")
                .containsEntry("username", "a-username@some-email.com")
                .hasSize(4);
    }

    @Test
    void shouldUpdateSettingsToNull(@TempDir Path tempDir) throws Exception {
        final Path dbCopy = copyTestDb(tempDir);
        final LauncherDb db = new LauncherDb(dbCopy);

        db.updateSettings("token", null);
        db.close();

        assertThat(new LauncherDb(dbCopy).getSettings())
                .containsEntry("current_game", "mnm")
                .containsEntry("remember", "true")
                .containsEntry("token", null)
                .containsEntry("username", "a-username@some-email.com")
                .hasSize(4);
    }

    @Test
    void shouldFailIfFileCantBeFound() {
        Throwable throwable = catchThrowable(() -> new LauncherDb(Path.of("some-file.db")));

        assertThat(throwable)
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("some-file.db");
    }

    private static Path copyTestDb(Path tempDir) throws IOException {
        Path dbCopy = tempDir.resolve("launcher_test_db.db");
        Files.copy(classpathFile("launcher_test_db.db"), dbCopy);
        return dbCopy;
    }
}
