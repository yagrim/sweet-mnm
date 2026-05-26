package org.mnm.client;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.TestUtils.validToken;
import static org.mnm.config.Client.Status.COMPLETED;

class RunCommandTest {

    private static final String TEST_SLUG = "mnm";

    private static final String VALID_TOKEN = validToken();
    private static final String EXPIRED_TOKEN = expiredToken();


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
              sweet run [--slug <slug>] [--id <id>] [--skip-version-check]
            
            Options:
              --slug                 Client slug to run (optional)
              --id                   Token id to use when multiple tokens exist
              --skip-version-check   Skip client version validation
              --debug                Enables debug messages
              --help                 Shows this help
            """);
    }

    @Test
    void shouldRunWindowsCommandForSingleClientWithoutSlug(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final Path installPath = tempDir.resolve("install");
        initDb(dbFile, testClient(TEST_SLUG, installPath), validTestToken());

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(() -> dbFile, runner, windows(), this::dummyCheckVersion);
        command.run(Arguments.parse());

        assertThat(runner.workingDirectory()).isEqualTo(installPath);
        assertThat(runner.command()).containsExactly(Path.of(TEST_SLUG, "mnm.exe").toString(), "--token", VALID_TOKEN);
        assertThat(runner.environment()).isEmpty();
    }

    @Test
    void shouldRunLinuxCommandForSingleClientWithoutSlug(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final Path installPath = tempDir.resolve("install");
        initDb(dbFile, testClient(TEST_SLUG, installPath), validTestToken());

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(() -> dbFile, runner, linux(), this::dummyCheckVersion);
        command.run(Arguments.parse());

        assertThat(runner.workingDirectory()).isEqualTo(installPath);
        assertThat(runner.command()).containsExactly("umu-run", Path.of(".", TEST_SLUG, "mnm.exe").toString(), "--token", VALID_TOKEN);
        assertThat(runner.environment())
            .containsEntry("GAMEID", TEST_SLUG)
            .containsEntry("PROTONPATH", "GE-Proton10-33")
            .containsEntry("WINEPREFIX", installPath.toAbsolutePath().resolve("mnm_prefix").toString());
    }

    @Test
    void shouldPreserveExistingLinuxEnvironmentVariables(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final Path installPath = tempDir.resolve("install");
        initDb(dbFile, testClient(TEST_SLUG, installPath), validTestToken());

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(
            () -> dbFile,
            runner,
            linux(),
            this::dummyCheckVersion,
            () -> Map.of(
                "GAMEID", "custom-game",
                "PROTONPATH", "custom-proton",
                "WINEPREFIX", "/tmp/custom-prefix"));
        command.run(Arguments.parse());

        assertThat(runner.environment())
            .containsEntry("GAMEID", "custom-game")
            .containsEntry("PROTONPATH", "custom-proton")
            .containsEntry("WINEPREFIX", "/tmp/custom-prefix");
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
            config.addToken(new Token("mnm-2", VALID_TOKEN));
        }

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(() -> dbFile, runner, windows(), this::dummyCheckVersion);
        command.run(Arguments.parse("--slug", "mnm-2"));

        assertThat(runner.workingDirectory()).isEqualTo(installPath2);
        assertThat(runner.command()).containsExactly(Path.of("mnm-2", "mnm.exe").toString(), "--token", VALID_TOKEN);
    }

    @Test
    void shouldRunVersionCheck(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final Path installPath = tempDir.resolve("install");
        initDb(dbFile, testClient(TEST_SLUG, installPath), validTestToken());

        final AtomicBoolean run = new AtomicBoolean(false);

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(() -> dbFile, runner, windows(), (s, c) -> {
            run.set(true);
        });
        command.run(Arguments.parse());

        assertThat(run).isTrue();
        assertThat(runner.workingDirectory()).isEqualTo(installPath);
        assertThat(runner.command()).containsExactly(Path.of(TEST_SLUG, "mnm.exe").toString(), "--token", VALID_TOKEN);
    }

    @Test
    void shouldSkipVersionCheckWhenFlagIsPresent(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final Path installPath = tempDir.resolve("install");
        initDb(dbFile, testClient(TEST_SLUG, installPath), validTestToken());

        final AtomicBoolean run = new AtomicBoolean(false);

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(() -> dbFile, runner, windows(), (s, c) -> {
            run.set(true);
        });
        command.run(Arguments.parse("--skip-version-check"));

        assertThat(run).isFalse();
        assertThat(runner.workingDirectory()).isEqualTo(installPath);
        assertThat(runner.command()).containsExactly(Path.of(TEST_SLUG, "mnm.exe").toString(), "--token", VALID_TOKEN);
    }

    @Test
    void shouldPanicWhenNoClientsExist(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");

        Command command = new RunCommand(() -> dbFile, failingRunner(), windows(), this::dummyCheckVersion);

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

        Command command = new RunCommand(() -> dbFile, failingRunner(), windows(), this::dummyCheckVersion);

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("Could not identify client: use --slug");
    }

    @Test
    void shouldPanicWhenNoTokensExist(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        initDb(
            dbFile,
            testClient(TEST_SLUG, tempDir.resolve("install").resolve(TEST_SLUG)));

        Command command = new RunCommand(() -> dbFile, failingRunner(), windows(), this::dummyCheckVersion);

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("No token found: run 'install --username ...' first");
    }

    @Test
    void shouldPanicWhenTokenIsExpired(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        initDb(
            dbFile,
            testClient(TEST_SLUG, tempDir.resolve("install").resolve(TEST_SLUG)),
            new Token(TEST_SLUG, EXPIRED_TOKEN));

        Command command = new RunCommand(() -> dbFile, failingRunner(), windows(), this::dummyCheckVersion);

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("Token expired: run 'install --username ...' to create a new one");
    }

    @Test
    void shouldPanicWhenMultipleTokensExist(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        initDb(
            dbFile,
            testClient(TEST_SLUG, tempDir.resolve("install").resolve(TEST_SLUG)),
            validTestToken(),
            new Token(TEST_SLUG, VALID_TOKEN));

        Command command = new RunCommand(() -> dbFile, failingRunner(), windows(), this::dummyCheckVersion);

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("Could not identify token: use --id");
    }

    @Test
    void shouldRunWithTokenIdWhenMultipleTokensExist(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        final Path installPath = tempDir.resolve("install");
        initDb(dbFile, testClient(TEST_SLUG, installPath), validTestToken());

        final int tokenId;
        try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
            tokenId = config.addToken(new Token(TEST_SLUG, VALID_TOKEN));
        }

        CapturingRunner runner = new CapturingRunner();
        Command command = new RunCommand(() -> dbFile, runner, windows(), this::dummyCheckVersion);
        command.run(Arguments.parse("--id", String.valueOf(tokenId)));

        assertThat(runner.workingDirectory()).isEqualTo(installPath);
        assertThat(runner.command()).containsExactly(Path.of(TEST_SLUG, "mnm.exe").toString(), "--token", VALID_TOKEN);
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

    private static Token validTestToken() {
        return new Token(TEST_SLUG, VALID_TOKEN);
    }

    private void dummyCheckVersion(String token, Client client) {
    }

}
