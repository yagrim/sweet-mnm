package org.mnm.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.client.RunnerOptions.LinuxOptions;
import org.mnm.client.RunnerOptions.UmuOptions;
import org.mnm.config.Client;
import org.mnm.config.CursorSize;
import org.mnm.config.ConfigDb;
import org.mnm.config.OS;
import org.mnm.config.Token;
import org.mnm.tools.PanicException;
import org.mnm.tools.ProcessUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.TestUtils.validToken;
import static org.mnm.client.LinuxOptionsTestFactory.defaultLinuxOptions;
import static org.mnm.client.LinuxOptionsTestFactory.linuxOptions;
import static org.mnm.client.LinuxOptionsTestFactory.protonOptions;
import static org.mnm.client.LinuxOptionsTestFactory.umuOptions;
import static org.mnm.client.LinuxOptionsTestFactory.wineOptions;
import static org.mnm.config.Client.Status.UPDATED;
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

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, defaultLinuxOptions())))
            .isInstanceOf(PanicException.class)
            .hasMessage("No client found: run 'install --username ...' first");
    }

    @Test
    void shouldPanicWhenMultipleClientsExistWithoutSlug() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients())
            .thenReturn(List.of(
                new Client(SLUG_1, "v1.0.0", UPDATED, Path.of("install-1")),
                new Client(SLUG_2, "v1.0.0", UPDATED, Path.of("install-2"))));

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, defaultLinuxOptions())))
            .isInstanceOf(PanicException.class)
            .hasMessage("Could not identify client: use --slug");
    }

    @Test
    void shouldPanicWhenNoTokenIsConfigured() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients())
            .thenReturn(List.of(new Client(SLUG, "v1.0.0", UPDATED, Path.of("install"))));
        Mockito.when(configDb.getTokens(SLUG)).thenReturn(java.util.List.of());

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, defaultLinuxOptions())))
            .isInstanceOf(PanicException.class)
            .hasMessage("No token found: run 'install --username ...' first");
    }

    @Test
    void shouldPanicWhenMultipleTokensExistWithoutId() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients())
            .thenReturn(List.of(new Client(SLUG, "v1.0.0", UPDATED, Path.of("install"))));
        Mockito.when(configDb.getTokens(SLUG))
            .thenReturn(List.of(new Token(1, SLUG, validToken()), new Token(2, SLUG, validToken())));

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, defaultLinuxOptions())))
            .isInstanceOf(PanicException.class)
            .hasMessage("Could not identify token: use --id");
    }

    @Test
    void shouldPanicWhenTokenIdDoesNotMatchSlug() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClient(SLUG_2))
            .thenReturn(new Client(SLUG_2, "v1.0.0", UPDATED, Path.of("install-2")));
        Mockito.when(configDb.getToken(1))
            .thenReturn(new Token(1, SLUG_1, validToken()));

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(SLUG_2, 1, true, defaultLinuxOptions())))
            .isInstanceOf(PanicException.class)
            .hasMessage("No token found for id 1");
    }

    @Test
    void shouldPanicWhenSlugDoesNotMatchAnyClient() {
        final ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClient(SLUG))
            .thenReturn(null);

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(SLUG, null, true, defaultLinuxOptions())))
            .isInstanceOf(PanicException.class)
            .hasMessage("No client found: run 'install --username ...' first");
    }

    @Test
    void shouldPanicWhenTokenIsExpired() {
        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients())
            .thenReturn(List.of(new Client(SLUG, "v1.0.0", UPDATED, Path.of("install"))));
        Mockito.when(configDb.getTokens(SLUG))
            .thenReturn(List.of(new Token(1, SLUG, expiredToken())));

        ClientRunner runner = new ClientRunner(configDb);

        assertThatThrownBy(() -> runner.run(new RunnerOptions(null, null, true, defaultLinuxOptions())))
            .isInstanceOf(PanicException.class)
            .hasMessage("Token expired: run 'install --username ...' to create a new one");
    }

    @Test
    void shouldCheckVersionBeforeRunning(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();
        Client client = new Client(SLUG, "v1.0.0", UPDATED, installPath);

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

            runner.run(new RunnerOptions(null, null, false, defaultLinuxOptions()));

            validatorsMock.verify(() -> Validators.checkVersion(token, client));
        }
    }

    @Test
    void shouldRunWindowsClientCommand(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, defaultLinuxOptions());
        var result = runClient(installPath, token, true, options);

        assertThat(result.workingDir).isEqualTo(installPath);
        assertThat(result.command).containsExactly("%s\\%s".formatted(SLUG, "mnm.exe"), "--token", token);
        assertThat(result.environment).isEmpty();
    }

    @Test
    void shouldRunWindowsClientCommandWithMangoHud(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, defaultLinuxOptions());
        var result = runClient(installPath, token, true, options);

        assertThat(result.workingDir).isEqualTo(installPath);
        assertThat(result.command).containsExactly("%s\\%s".formatted(SLUG, "mnm.exe"), "--token", token);
        assertThat(result.environment).isEmpty();
    }

    @Test
    void shouldFailWhenLinuxOptionsAreNull(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, null);

        assertThatThrownBy(() -> runClient(installPath, token, false, options))
            .isInstanceOfAny(NullPointerException.class)
            .hasMessage("linuxOptions cannot be null");
    }

    @Test
    void shouldFailWhenUmuOptionsAreNull(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var linuxOptions = new LinuxOptions(true, true, null, protonOptions(false, false), wineOptions(CursorSize.DEFAULT));
        var options = new RunnerOptions(null, null, true, linuxOptions);

        assertThatThrownBy(() -> runClient(installPath, token, false, options))
            .isInstanceOfAny(NullPointerException.class)
            .hasMessage("linuxOptions.umuOptions cannot be null");
    }

    @Test
    void shouldFailWhenWineOptionsAreNull(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var linuxOptions = new LinuxOptions(false, true, umuOptions(), protonOptions(false, false), null);
        var options = new RunnerOptions(null, null, true, linuxOptions);

        assertThatThrownBy(() -> runClient(installPath, token, false, options))
            .isInstanceOfAny(NullPointerException.class)
            .hasMessage("linuxOptions.wineOptions cannot be null");
    }

    @Test
    void shouldRunLinuxClientWithDefaults(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, defaultLinuxOptions());
        var result = runClient(installPath, token, false, options);

        assertThat(result.workingDir).isEqualTo(installPath);
        assertThat(result.command)
            .containsExactly("umu-run", "%s/%s".formatted(SLUG, "mnm.exe"), "--token", token);
        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", SLUG,
                "PROTONPATH", "GE-Proton",
                "WINEPREFIX", clientRelativeWinePrefix(installPath)
            ));
    }

    @Test
    void shouldUseSlugWinePrefixForLinuxClient(@TempDir Path tempDir) {
        final String slug = "other-game";
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, defaultLinuxOptions());
        var result = runClient(slug, installPath, token, false, options);

        assertThat(result.command)
            .containsExactly("umu-run", "%s/%s".formatted(slug, "mnm.exe"), "--token", token);
        assertThat(result.environment)
            .containsEntry("WINEPREFIX", installPath.toAbsolutePath().resolve("other-game_prefix").toString());
    }

    @Test
    void shouldUseSanitizedSlugWinePrefixForLinuxClient(@TempDir Path tempDir) {
        final String slug = "other game";
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, defaultLinuxOptions());
        var result = runClient(slug, installPath, token, false, options);

        assertThat(result.environment)
            .containsEntry("WINEPREFIX", installPath.toAbsolutePath().resolve("other_game_prefix").toString());
    }

    @Test
    void shouldRunLinuxClientWithMangoHud(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var options = new RunnerOptions(null, null, true, linuxOptions(true));

        var result = runClient(installPath, token, false, options);

        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", SLUG,
                "PROTONPATH", "GE-Proton",
                "WINEPREFIX", clientRelativeWinePrefix(installPath),
                "MANGOHUD", "1"
            ));
    }

    @Test
    void shouldRunLinuxClientWithEnabledProtonVariables(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();
        var options = new RunnerOptions(null, null, true,
            new LinuxOptions(false, true, umuOptions(), protonOptions(true, true), wineOptions(CursorSize.DEFAULT)));

        var result = runClient(installPath, token, false, options);

        assertThat(result.environment)
            .containsEntry("PROTON_USE_WINED3D", "1")
            .containsEntry("PROTON_ENABLE_WAYLAND", "1");
    }

    @Test
    void shouldBuildEnvironmentWithGameId(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        final String gameId = "customm-mnm";
        var umuOptions = new UmuOptions(gameId, "GE-Proton", null);
        var options = new RunnerOptions(null, null, true, linuxOptions(umuOptions));

        var result = runClient(installPath, token, false, options);

        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", gameId,
                "PROTONPATH", "GE-Proton",
                "WINEPREFIX", clientRelativeWinePrefix(installPath)
            ));
    }

    @Test
    void shouldBuildEnvironmentWithProtonPath(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        final String protonPath = "my-proton-distro";
        var umuOptions = new UmuOptions("mnm", protonPath, "/some/absolute");
        var options = new RunnerOptions(null, null, true, linuxOptions(umuOptions));

        var result = runClient(installPath, token, false, options);

        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", "mnm",
                "PROTONPATH", protonPath,
                "WINEPREFIX", clientRelativeWinePrefix(installPath)
            ));
    }

    @Test
    void shouldBuildEnvironmentWithAbsoluteWinePrefix(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        final String winePrefix = "/home/me/some/prefix";
        var umuOptions = new UmuOptions("mnm", "GE-Proton", winePrefix);
        var options = new RunnerOptions(null, null, true, linuxOptions(false, umuOptions));

        var result = runClient(installPath, token, false, options);

        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", "mnm",
                "PROTONPATH", "GE-Proton",
                "WINEPREFIX", OS.isWindows() ? "C:\\home\\me\\some\\prefix" : winePrefix
            ));
    }

    @Test
    void shouldBuildEnvironmentWithClientRelativeWinePrefix(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        final String winePrefix = "my/prefix";
        var umuOptions = new UmuOptions("mnm", "GE-Proton", winePrefix);
        var options = new RunnerOptions(null, null, true, linuxOptions(false, umuOptions));

        var result = runClient(installPath, token, false, options);

        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", "mnm",
                "PROTONPATH", "GE-Proton",
                "WINEPREFIX", installPath.toAbsolutePath().resolve(winePrefix).toString()
            ));
    }

    @Test
    void shouldBuildEnvironmentWithClientAsWinePrefixAndIgnoreWinePrefix(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        final String winePrefix = "/home/me/my-protong-distro";
        var umuOptions = new UmuOptions("mnm", "GE-Proton", winePrefix);
        var options = new RunnerOptions(null, null, true, linuxOptions(true, umuOptions));

        var result = runClient(installPath, token, false, options);

        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", "mnm",
                "PROTONPATH", "GE-Proton",
                "WINEPREFIX", clientRelativeWinePrefix(installPath)
            ));
    }

    @Test
    void shouldBuildEnvironmentWithClientAsWinePrefixFallbackWhenPrefixIsNull(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        String token = validToken();

        var umuOptions = new UmuOptions("mnm", "GE-Proton", null);
        var options = new RunnerOptions(null, null, true, linuxOptions(false, umuOptions));

        var result = runClient(installPath, token, false, options);

        assertThat(result.environment)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "GAMEID", "mnm",
                "PROTONPATH", "GE-Proton",
                "WINEPREFIX", clientRelativeWinePrefix(installPath)
            ));
    }

    @Test
    void shouldSetLogPixelsInWinePrefixForLinuxClient(@TempDir Path tempDir) throws IOException {
        Path installPath = tempDir.resolve("install");
        Path systemReg = writeSystemReg(Path.of(clientRelativeWinePrefix(installPath)));
        var options = new RunnerOptions(null, null, true,
            new LinuxOptions(false, true, umuOptions(), protonOptions(false, false), wineOptions(CursorSize.SCALE_200)));

        runClient(installPath, validToken(), false, options);

        assertThat(Files.readString(systemReg)).isEqualTo(SYSTEM_REG.replace("00000060", "000000c0"));
    }

    @Test
    void shouldNotChangeWinePrefixWhenCursorSizeIsDefault(@TempDir Path tempDir) throws IOException {
        Path installPath = tempDir.resolve("install");
        Path systemReg = writeSystemReg(Path.of(clientRelativeWinePrefix(installPath)));
        var options = new RunnerOptions(null, null, true, defaultLinuxOptions());

        runClient(installPath, validToken(), false, options);

        assertThat(Files.readString(systemReg)).isEqualTo(SYSTEM_REG);
        assertThat(systemReg.resolveSibling("system.reg.sweet-backup")).doesNotExist();
    }

    @Test
    void shouldRestoreOriginalSystemRegWhenCursorSizeIsDefault(@TempDir Path tempDir) throws IOException {
        Path installPath = tempDir.resolve("install");
        Path systemReg = writeSystemReg(Path.of(clientRelativeWinePrefix(installPath)));
        runClient(installPath, validToken(), false, new RunnerOptions(null, null, true,
            new LinuxOptions(false, true, umuOptions(), protonOptions(false, false), wineOptions(CursorSize.SCALE_200))));

        runClient(installPath, validToken(), false, new RunnerOptions(null, null, true, defaultLinuxOptions()));

        assertThat(Files.readString(systemReg)).isEqualTo(SYSTEM_REG);
        assertThat(systemReg.resolveSibling("system.reg.sweet-backup")).doesNotExist();
    }

    @Test
    void shouldNotChangeWinePrefixOnWindows(@TempDir Path tempDir) throws IOException {
        Path installPath = tempDir.resolve("install");
        Path systemReg = writeSystemReg(Path.of(clientRelativeWinePrefix(installPath)));
        var options = new RunnerOptions(null, null, true,
            new LinuxOptions(false, true, umuOptions(), protonOptions(false, false), wineOptions(CursorSize.SCALE_200)));

        runClient(installPath, validToken(), true, options);

        assertThat(Files.readString(systemReg)).isEqualTo(SYSTEM_REG);
    }

    @Test
    void shouldRunLinuxClientWhenWinePrefixDoesNotExist(@TempDir Path tempDir) {
        Path installPath = tempDir.resolve("install");
        var options = new RunnerOptions(null, null, true,
            new LinuxOptions(false, true, umuOptions(), protonOptions(false, false), wineOptions(CursorSize.SCALE_200)));

        var result = runClient(installPath, validToken(), false, options);

        assertThat(result.command).isNotNull();
        assertThat(Path.of(clientRelativeWinePrefix(installPath))).doesNotExist();
    }

    private static final String SYSTEM_REG = """
        WINE REGISTRY Version 2
        [System\\CurrentControlSet\\Hardware Profiles\\Current\\Software\\Fonts] 1700000000
        "LogPixels"=dword:00000060
        """;

    private static Path writeSystemReg(Path winePrefix) throws IOException {
        Files.createDirectories(winePrefix);
        return Files.writeString(winePrefix.resolve("system.reg"), SYSTEM_REG);
    }

    // defaults to client path if prefix is null


    private RunResult runClient(Path installPath, String token, boolean isWindows, RunnerOptions options) {
        return runClient(SLUG, installPath, token, isWindows, options);
    }

    private RunResult runClient(String slug, Path installPath, String token, boolean isWindows, RunnerOptions options) {
        Client client = new Client(slug, "v1.0.0", UPDATED, installPath);

        ConfigDb configDb = Mockito.mock(ConfigDb.class);
        Mockito.when(configDb.getClients()).thenReturn(List.of(client));
        Mockito.when(configDb.getTokens(slug)).thenReturn(List.of(new Token(1, slug, token)));

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

    private static String clientRelativeWinePrefix(Path installPath) {
        return installPath.toAbsolutePath().resolve("mnm_prefix").toString();
    }
}
