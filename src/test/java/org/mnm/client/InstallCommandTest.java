package org.mnm.client;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(SystemOutCaptureExtension.class)
class InstallCommandTest {

    @Test
    void shouldReturnName() {
        final Command command = new InstallCommand(null);
        assertThat(command.name()).isEqualTo("install");
    }

    @Test
    void shouldReturnDescription() {
        final Command command = new InstallCommand(null);
        assertThat(command.description()).isEqualTo("Installs MnM client in the current location");
    }

    @Test
    void shouldReturnHelp() {
        final Command command = new InstallCommand(null);

        assertThat(command.help()).isEqualTo("""
            Installs MnM client in the current location
            
            Usage:
              sweet install --username <username> --password <password>
            
            Options:
              --username      MnM account username (required when --slug is not set)
              --password      MnM account password (required when --username is set)
              --file-check    Check files using external process or in-memory method (in-memory, xxhsum (default))
              --debug         Enables debug messages
              --help          Shows this help
            """);
    }

    @Test
    void shouldValidateOptionsBeforeOpeningConfigDatabase() {
        AtomicBoolean configFileLocated = new AtomicBoolean(false);
        Command command = new InstallCommand(() -> {
            configFileLocated.set(true);
            return null;
        });
        Arguments arguments = Arguments.parse("--password", "password");

        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--username'");
        assertThat(configFileLocated).isFalse();
    }

    @Test
    void shouldValidateMissingPasswordBeforeOpeningConfigDatabase() {
        AtomicBoolean configFileLocated = new AtomicBoolean(false);
        Command command = new InstallCommand(() -> {
            configFileLocated.set(true);
            return null;
        });
        Arguments arguments = Arguments.parse("--username", "username");

        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--password'");
        assertThat(configFileLocated).isFalse();
    }

    @Test
    void shouldValidateMissingUsernameBeforeOpeningConfigDatabase(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");

        Command command = new InstallCommand(() -> dbFile);
        Arguments arguments = Arguments.parse("--slug", "mnm");

        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--username'");
    }

    @Test
    void shouldCallInstallerWhenConditionsAreMet(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final AtomicBoolean installerCalled = new AtomicBoolean(false);

        Command command = new InstallCommand(() -> dbFile, (_, _, _) -> installerCalled.set(true));
        Arguments arguments = Arguments.parse("--username", "username", "--password", "password");

        command.run(arguments);

        assertThat(installerCalled).isTrue();
    }

}
