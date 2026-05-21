package org.mnm.client;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mnm.ConfigTestDatabase;
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

        assertThat(command.help()).isEqualTo(expectedHelp("Installs MnM client in the current location", "install"));
    }

    @Test
    void shouldReturnRepairHelp() {
        final Command command = new RepairCommand(null);

        assertThat(command.help()).isEqualTo(expectedHelp("Checks installation and updates if necessary", "repair"));
    }

    private static String expectedHelp(String header, String command) {
        return """
            %s
            
            Usage:
              sweet %2$s --username <username> --password <password>
              sweet %2$s --slug <slug>
            
            Options:
              --username      MnM account username (required when --slug is not set)
              --password      MnM account password (required when --username is set)
              --slug          Existing configured client slug, can be used instead of credentials
              --file-check    Check files using external process or in-memory method (in-memory, xxhsum (default))
              --help          Shows this help
            """.formatted(header, command);
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
            .hasMessage("No client found: run 'install --username ...' first");

        try (ConfigTestDatabase.TestDatabase testDatabase = ConfigTestDatabase.open(dbFile)) {
            assertThat(testDatabase.getTables())
                .containsExactlyInAnyOrder("clients", "sessions");
            testDatabase.assertThatTable("clients").isEmpty();
            testDatabase.assertThatTable("sessions").isEmpty();
        }
    }

}
