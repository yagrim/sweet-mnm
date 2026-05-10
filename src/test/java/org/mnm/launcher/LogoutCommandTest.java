package org.mnm.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Command;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.LauncherTestDatabase.*;

@ExtendWith(SystemOutCaptureExtension.class)
class LogoutCommandTest {

    @Test
    void shouldClearToken(SystemOutCaptureExtension out, @TempDir Path tempDir) throws Exception {
        final Path path = copyTestDb(tempDir);
        final Command command = new LogoutCommand(() -> path);

        try (LauncherDb launcherDb = new LauncherDb(path)) {
            assertThat(launcherDb.getSettings())
                    .containsEntry("token", INITIAL_TOKEN);
        }

        command.run(null);
        assertThat(out.getOutput()).isEqualTo("""
                Token removed from launcher database
                """);

        try (LauncherDb launcherDb = new LauncherDb(path)) {
            assertThat(launcherDb.getSettings())
                    .containsEntry("token", "");
        }
    }

    @Test
    void shouldNotFailWhenTokenVariableIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) throws Exception {
        final Path path = initTestDatabase(tempDir);
        final Command command = new LogoutCommand(() -> path);

        try (LauncherDb launcherDb = new LauncherDb(path)) {
            assertThat(launcherDb.getSettings()).isEmpty();

            command.run(null);

            assertThat(out.getOutput()).isEqualTo("""
                    Token removed from launcher database
                    """);

            assertThat(launcherDb.getSettings()).isEmpty();
        }
    }

    @Test
    void shouldNotFailWhenTokenValueIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) throws Exception {
        final Path path = initTestDatabase(tempDir);
        final Command command = new LogoutCommand(() -> path);

        try (LauncherDb launcherDb = new LauncherDb(path)) {
            launcherDb.insertSetting("command", null);
            assertThat(launcherDb.getSettings()).containsEntry("command", null);

            command.run(null);

            assertThat(out.getOutput()).isEqualTo("""
                    Token removed from launcher database
                    """);

            assertThat(launcherDb.getSettings())
                    .containsEntry("command", null);
        }
    }

}
