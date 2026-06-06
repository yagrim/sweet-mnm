package org.mnm.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.OS;
import org.mnm.config.Token;
import org.mnm.tools.PanicException;
import org.mnm.tools.ProcessUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.TestUtils.validToken;
import static org.mnm.config.Client.Status.COMPLETED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

class ClientRunnerTest {

    private static final String SLUG = "mnm";
    private static final String SLUG_1 = "mnm-1";
    private static final String SLUG_2 = "mnm-2";

    @Test
    void shouldPanicWhenNoClientIsConfigured() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients()).thenReturn(java.util.List.of());

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, false)))
            .isInstanceOf(PanicException.class)
            .hasMessage("No client found: run 'install --username ...' first");
    }

    @Test
    void shouldPanicWhenMultipleClientsExistWithoutSlug() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients())
            .thenReturn(List.of(
                new Client(SLUG_1, "v1.0.0", COMPLETED, Path.of("install-1")),
                new Client(SLUG_2, "v1.0.0", COMPLETED, Path.of("install-2"))));

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, false)))
            .isInstanceOf(PanicException.class)
            .hasMessage("Could not identify client: use --slug");
    }

    @Test
    void shouldPanicWhenNoTokenIsConfigured() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients())
            .thenReturn(List.of(new Client(SLUG, "v1.0.0", COMPLETED, Path.of("install"))));
        Mockito.when(configDb.getTokens(SLUG)).thenReturn(java.util.List.of());

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, false)))
            .isInstanceOf(PanicException.class)
            .hasMessage("No token found: run 'install --username ...' first");
    }

    @Test
    void shouldPanicWhenMultipleTokensExistWithoutId() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients())
            .thenReturn(List.of(new Client(SLUG, "v1.0.0", COMPLETED, Path.of("install"))));
        Mockito.when(configDb.getTokens(SLUG))
            .thenReturn(List.of(new Token(1, SLUG, validToken()), new Token(2, SLUG, validToken())));

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, false)))
            .isInstanceOf(PanicException.class)
            .hasMessage("Could not identify token: use --id");
    }

    @Test
    void shouldPanicWhenTokenIdDoesNotMatchSlug() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClient(SLUG_2))
            .thenReturn(new Client(SLUG_2, "v1.0.0", COMPLETED, Path.of("install-2")));
        Mockito.when(configDb.getToken(1))
            .thenReturn(new Token(1, SLUG_1, validToken()));

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(SLUG_2, 1, true, false)))
            .isInstanceOf(PanicException.class)
            .hasMessage("No token found for id 1");
    }

    @Test
    void shouldPanicWhenSlugDoesNotMatchAnyClient() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClient(SLUG))
            .thenReturn(null);

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(SLUG, null, true, false)))
            .isInstanceOf(PanicException.class)
            .hasMessage("No client found: run 'install --username ...' first");
    }

    @Test
    void shouldPanicWhenTokenIsExpired() {
        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients())
            .thenReturn(List.of(new Client(SLUG, "v1.0.0", COMPLETED, Path.of("install"))));
        Mockito.when(configDb.getTokens(SLUG))
            .thenReturn(List.of(new Token(1, SLUG, expiredToken())));

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, false)))
            .isInstanceOf(PanicException.class)
            .hasMessage("Token expired: run 'install --username ...' to create a new one");
    }

    @Test
    void shouldCheckVersionBeforeRunning(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();
        Client client = new Client(SLUG, "v1.0.0", COMPLETED, installPath);

        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients()).thenReturn(List.of(client));
        Mockito.when(configDb.getTokens(SLUG)).thenReturn(List.of(new Token(1, SLUG, token)));

        ClientRunner runner = new ClientRunner(configDb);

        try (MockedStatic<OS> osMock = Mockito.mockStatic(OS.class);
             MockedStatic<Validators> validatorsMock = Mockito.mockStatic(Validators.class);
             MockedStatic<ProcessUtils> processMock = Mockito.mockStatic(ProcessUtils.class)) {

            osMock.when(OS::isWindows).thenReturn(true);
            validatorsMock.when(() -> Validators.checkVersion(token, client)).thenAnswer(invocation -> null);
            processMock.when(() -> ProcessUtils.run(any(Path.class), any(String[].class), anyMap()))
                .thenReturn("");

            runner.run(new RunnerOptions(null, null, false, false));

            validatorsMock.verify(() -> Validators.checkVersion(token, client));
        }
    }

    @Test
    void shouldRunWindowsClientCommand(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, false);
        var result = runClient(installPath, token, true, options);

        assertThat(result.workingDir).isEqualTo(installPath);
        assertThat(result.command).containsExactly("%s\\%s".formatted(SLUG, "mnm.exe"), "--token", token);
        assertThat(result.environment).isEmpty();
    }

    @Test
    void shouldRunWindowsClientCommandWithMangoHud(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, false);
        var result = runClient(installPath, token, true, options);

        assertThat(result.workingDir).isEqualTo(installPath);
        assertThat(result.command).containsExactly("%s\\%s".formatted(SLUG, "mnm.exe"), "--token", token);
        assertThat(result.environment).isEmpty();
    }

    @Test
    void shouldRunLinuxClient(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, false);
        var result = runClient(installPath, token, false, options);

        assertThat(result.workingDir).isEqualTo(installPath);
        assertThat(result.command)
            .containsExactly("umu-run", "%s/%s".formatted(SLUG, "mnm.exe"), "--token", token);
        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", SLUG,
                "PROTONPATH", "GE-Proton10-33",
                "WINEPREFIX", installPath.toAbsolutePath().resolve("mnm_prefix").toString()
            ));
    }

    @Test
    void shouldRunLinuxClientWithMangoHud(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, true);
        var result = runClient(installPath, token, false, options);

        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", SLUG,
                "PROTONPATH", "GE-Proton10-33",
                "WINEPREFIX", installPath.toAbsolutePath().resolve("mnm_prefix").toString(),
                "MANGOHUD", "1"
            ));
    }

    private RunResult runClient(Path installPath, String token, boolean isWindows, RunnerOptions options) {
        Client client = new Client(SLUG, "v1.0.0", COMPLETED, installPath);

        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients()).thenReturn(List.of(client));
        Mockito.when(configDb.getTokens(SLUG)).thenReturn(List.of(new Token(1, SLUG, token)));

        AtomicReference<Path> workingDirectory = new AtomicReference<>();
        AtomicReference<String[]> command = new AtomicReference<>();
        AtomicReference<Map<String, String>> environment = new AtomicReference<>();

        ClientRunner runner = new ClientRunner(configDb);

        try (MockedStatic<OS> osMock = Mockito.mockStatic(OS.class);
             MockedStatic<ProcessUtils> processMock = Mockito.mockStatic(ProcessUtils.class)) {

            osMock.when(OS::isWindows).thenReturn(isWindows);
            processMock.when(() -> ProcessUtils.run(any(Path.class), any(String[].class), anyMap()))
                .thenAnswer(invocation -> {
                    workingDirectory.set(invocation.getArgument(0));
                    command.set(invocation.getArgument(1));
                    environment.set(invocation.getArgument(2));
                    return "";
                });

            runner.run(options);
        }

        return new RunResult(workingDirectory.get(), command.get(), environment.get());
    }

    record RunResult(Path workingDir, String[] command, Map<String, String> environment) {

    }

}
