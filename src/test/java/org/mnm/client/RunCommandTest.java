package org.mnm.client;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.config.Client.Status.COMPLETED;

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
              sweet run [--slug <slug>]
            
            Options:
              --slug    Client slug to run (optional)
              --debug   Enables debug messages
              --help    Shows this help
            """);
    }

    @Test
    void shouldRunWindowsCommandForSingleClientWithoutSlug(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final Path installPath = tempDir.resolve("install");
        initDb(dbFile, testClient("mnm", installPath), testToken());

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(() -> dbFile, runner, windows());
        command.run(Arguments.parse());

        assertThat(runner.workingDirectory()).isEqualTo(installPath);
        assertThat(runner.command()).containsExactly(Path.of("mnm", "mnm.exe").toString(), "--token", "token-1");
        assertThat(runner.environment()).isEmpty();
    }

    @Test
    void shouldRunLinuxCommandForSingleClientWithoutSlug(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final Path installPath = tempDir.resolve("install");
        initDb(dbFile, testClient("mnm", installPath), testToken());

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(() -> dbFile, runner, linux());
        command.run(Arguments.parse());

        assertThat(runner.workingDirectory()).isEqualTo(installPath);
        assertThat(runner.command()).containsExactly("umu-run", Path.of(".", "mnm", "mnm.exe").toString(), "--token", "token-1");
        assertThat(runner.environment())
            .containsEntry("GAMEID", "mnm")
            .containsEntry("PROTONPATH", "GE-Proton10-33")
            .containsEntry("WINEPREFIX", installPath.toAbsolutePath().resolve("mnm_prefix").toString());
    }

    @Test
    void shouldSelectClientBySlug(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final Path installPath1 = tempDir.resolve("install").resolve("mnm-1");
        final Path installPath2 = tempDir.resolve("install").resolve("mnm-2");
        initDb(dbFile,
            testClient("mnm-1", installPath1),
            testClient("mnm-2", installPath2));

        try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
            config.addToken(new Token("mnm-2", "token-2"));
        }

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(() -> dbFile, runner, windows());
        command.run(Arguments.parse("--slug", "mnm-2"));

        assertThat(runner.workingDirectory()).isEqualTo(installPath2);
        assertThat(runner.command()).containsExactly(Path.of("mnm-2", "mnm.exe").toString(), "--token", "token-2");
    }

    @Test
    void shouldPanicWhenNoClientsExist(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");

        Command command = new RunCommand(() -> dbFile, failingRunner(), windows());

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("No client found: run 'install --username ...' first");
    }

    @Test
    void shouldPanicWhenMultipleClientsAndSlugMissing(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        initDb(
            dbFile,
            testClient("mnm-1", tempDir.resolve("install").resolve("mnm-1")),
            testClient("mnm-2", tempDir.resolve("install").resolve("mnm-2")));

        Command command = new RunCommand(() -> dbFile, failingRunner(), windows());

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("Could not identify client: use --slug");
    }

    @Test
    void shouldPanicWhenNoTokensExist(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        initDb(
            dbFile,
            testClient("mnm", tempDir.resolve("install").resolve("mnm")));

        Command command = new RunCommand(() -> dbFile, failingRunner(), windows());

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("No token found: run 'install --username ...' first");
    }

    @Test
    void shouldPanicWhenMultipleTokensExist(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        initDb(
            dbFile,
            testClient("mnm", tempDir.resolve("install").resolve("mnm")),
            testToken(),
            new Token("mnm", "token-2"));

        Command command = new RunCommand(() -> dbFile, failingRunner(), windows());

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("Could not identify token");
    }

    private static void initDb(Path dbFile, Client client, Token... tokens) {
        try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
            config.addClient(client);
            for (Token token : tokens) {
                config.addToken(token);
            }
        }
    }

    private static void initDb(Path dbFile, Client client1, Client client2) {
        try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
            config.addClient(client1);
            config.addClient(client2);
        }
    }

    private static BooleanSupplier windows() {
        return () -> true;
    }

    private static BooleanSupplier linux() {
        return () -> false;
    }

    private static RunCommand.ProcessRunner failingRunner() {
        return (_workingDirectory, _command, _environment) -> {
            throw new AssertionError("Process runner should not be called");
        };
    }

    private static final class CapturingRunner implements RunCommand.ProcessRunner {
        private final AtomicReference<Path> workingDirectory = new AtomicReference<>();
        private final AtomicReference<String[]> command = new AtomicReference<>();
        private final AtomicReference<Map<String, String>> environment = new AtomicReference<>();

        @Override
        public String run(Path workingDirectory, String[] command, Map<String, String> environment) {
            this.workingDirectory.set(workingDirectory);
            this.command.set(command.clone());
            this.environment.set(Map.copyOf(environment));
            return "";
        }

        Path workingDirectory() {
            return workingDirectory.get();
        }

        String[] command() {
            return command.get();
        }

        Map<String, String> environment() {
            return environment.get();
        }
    }

    private static Client testClient(String mnm, Path installPath) {
        return new Client(mnm, "v1.0.0", COMPLETED, installPath);
    }

    private static Token testToken() {
        return new Token("mnm", "token-1");
    }
}
