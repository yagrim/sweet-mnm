package org.mnm.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Command;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.LauncherTestDatabase.*;

@ExtendWith(SystemOutCaptureExtension.class)
class LogoutCommandTest {

    @Test
    void shouldClearToken(SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final TestDatabase testDb = withSettings(tempDir);
        final Command command = new LogoutCommand(testDb::path);

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);

        command.run(null);
        assertThat(out.getOutput()).isEqualTo("""
                Token removed from launcher database
                """);

        testDb.assertThatToken().isEqualTo("");
    }

    @Test
    void shouldNotFailWhenTokenVariableIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final TestDatabase testDb = withSchema(tempDir);
        final Command command = new LogoutCommand(() -> testDb.path());

        testDb.assertThatSettings().isEmpty();

        command.run(null);

        assertThat(out.getOutput()).isEqualTo("""
                Token removed from launcher database
                """);

        testDb.assertThatSettings().isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldNotFailWhenTokenValueIsNotSet(String value, SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final TestDatabase testDb = withSchema(tempDir);
        final Command command = new LogoutCommand(() -> testDb.path());

        testDb.insertSettingsToken(value);
        testDb.assertThatToken().isEqualTo(value);

        command.run(null);

        assertThat(out.getOutput()).isEqualTo("""
                Token removed from launcher database
                """);

        // we sanitize value since null cause official launcher to fail
        testDb.assertThatToken().isEqualTo("");
    }

}
