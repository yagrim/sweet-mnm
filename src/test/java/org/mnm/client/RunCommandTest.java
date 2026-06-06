package org.mnm.client;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;

import static org.assertj.core.api.Assertions.assertThat;

class RunCommandTest {

    @Test
    void shouldReturnName() {
        final Command command = new RunCommand(null);

        assertThat(command.name()).isEqualTo("run");
    }

    @Test
    void shouldReturnDescription() {
        final Command command = new RunCommand(null);

        assertThat(command.description()).isEqualTo("Runs configured client");
    }

    @Test
    void shouldReturnHelp() {
        final Command command = new RunCommand(null);

        assertThat(command.help()).isEqualTo("""
            Runs configured client
            
            Usage:
              sweet run [--slug <slug>] [--id <id>] [--skip-version-check] [--enable-mangohud]
            
            Options:
              --slug                 Client slug to run (optional)
              --id                   Token id to use when multiple tokens exist
              --skip-version-check   Skip client version validation
              --enable-mangohud      Enable MangoHud if available
              --debug                Enables debug messages
              --help                 Shows this help
            """);
    }

    @Test
    void shouldExecuteRunner(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final AtomicBoolean runnerInvoked = new AtomicBoolean(false);
        final AtomicReference<RunnerOptions> receivedOptions = new AtomicReference<>();

        Command command = new RunCommand(
            () -> dbFile,
            (runOptions, configDb) -> {
                runnerInvoked.set(true);
                receivedOptions.set(runOptions);
                assertThat(configDb.getClients()).isEmpty();
            });

        command.run(Arguments.parse());

        assertThat(runnerInvoked).isTrue();
        assertThat(receivedOptions.get()).isEqualTo(new RunnerOptions(null, null, false, false));
        assertThat(dbFile).exists();
    }

    @Test
    void shouldParseMangoHudFlagIntoRunnerOptions(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final AtomicReference<RunnerOptions> receivedOptions = new AtomicReference<>();

        Command command = new RunCommand(
            () -> dbFile,
            (runOptions, configDb) -> {
                receivedOptions.set(runOptions);
                assertThat(configDb.getClients()).isEmpty();
            });

        command.run(Arguments.parse("--enable-mangohud"));

        assertThat(receivedOptions.get()).isEqualTo(new RunnerOptions(null, null, false, true));
    }

}
