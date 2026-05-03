package org.mnm.launcher;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
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
                .containsEntry("token", "1234567890")
                .containsEntry("username", "a-username@some-maill.com")
                .hasSize(4);
    }

    @Test
    void shouldFailIfFileCantBeFound() {
        Throwable throwable = catchThrowable(() -> new LauncherDb(Path.of("some-file.db")));

        assertThat(throwable)
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("some-file.db");
    }

}
