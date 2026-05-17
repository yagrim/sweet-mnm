package org.mnm.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mnm.ConfigTestDatabase;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.tools.PanicException;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    void shouldValidateOptionsBeforeOpeningConfigDatabase() {
        AtomicBoolean configFileLocated = new AtomicBoolean(false);
        Command command = new InstallCommand(() -> {
            configFileLocated.set(true);
            return null;
        });
        Arguments arguments = new Arguments(Map.of("password", "password"));

        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--username' or '--slug'");
        assertThat(configFileLocated).isFalse();
    }

    @Test
    void shouldValidateMissingPasswordBeforeOpeningConfigDatabase() {
        AtomicBoolean configFileLocated = new AtomicBoolean(false);
        Command command = new InstallCommand(() -> {
            configFileLocated.set(true);
            return null;
        });
        Arguments arguments = new Arguments(Map.of("username", "username"));

        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--password'");
        assertThat(configFileLocated).isFalse();
    }

    @Test
    void shouldInitializeConfigDatabaseBeforeInstalling(@TempDir Path tempDir) throws Exception {
        final Path dbFile = tempDir.resolve("config.db");

        Command command = new InstallCommand(() -> dbFile);
        Arguments arguments = new Arguments(Map.of("slug", "mnm"));

        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
                .isInstanceOf(PanicException.class)
                .hasMessage("No client found: run 'install' command first.");

        try (ConfigTestDatabase.TestDatabase testDatabase = ConfigTestDatabase.open(dbFile)) {
            assertThat(testDatabase.getTables())
                    .containsExactlyInAnyOrder("clients", "sessions");
            testDatabase.assertThatTable("clients").isEmpty();
            testDatabase.assertThatTable("sessions").isEmpty();
        }
    }
}
