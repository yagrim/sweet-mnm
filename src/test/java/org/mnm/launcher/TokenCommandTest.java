package org.mnm.launcher;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mnm.LauncherTestDatabase;
import org.mnm.LauncherTestDatabase.TestDatabase;
import org.mnm.LinuxOnlyCommand;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.LauncherTestDatabase.withSchema;

@ExtendWith(SystemOutCaptureExtension.class)
class TokenCommandTest extends LinuxOnlyCommand {

    @Test
    void shouldReturnHelp() {
        final Command command = new TokenCommand(null);

        assertThat(command.help()).isEqualTo("""
            Shows official launcher current token
            
            Usage:
              sweet token
            
            Options:
              --help   Shows this help
            """);
    }

    @Test
    void shouldReturnToken(SystemOutCaptureExtension out) {
        Command token = new TokenCommand(LauncherTestDatabase::fromClasspath);

        token.run(null);

        assertThat(out.getOutput()).isEqualTo("""
            123.456.789
            """);
    }

    @Test
    void shouldPrintMessageWhenTokenIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final TestDatabase testDb = withSchema(tempDir);
        Command token = new TokenCommand(testDb::path);

        token.run(null);

        assertThat(out.getOutput()).isEqualTo("""
            No token found
            """);
    }

    @Override
    protected Command buildCommand() {
        return new TokenCommand(null);
    }
}
