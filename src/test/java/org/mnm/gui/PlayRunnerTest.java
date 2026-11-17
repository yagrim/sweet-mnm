package org.mnm.gui;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.client.ClientRunner;
import org.mnm.client.RunnerOptions;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.OS;
import org.mnm.config.Token;
import org.mnm.gui.GuiCommand.DetachedPlayRunner;
import org.mnm.tools.ProcessUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.TestUtils.validToken;
import static org.mnm.client.LinuxOptionsTestFactory.defaultLinuxOptions;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

class PlayRunnerTest {

    private static final String SLUG = "mnm-test";
    private static final long EARLY_EXIT_SECONDS = 3;

    @TempDir
    Path tempDir;

    private Path dbFile;
    private Thread.UncaughtExceptionHandler previousHandler;
    private final CompletableFuture<Throwable> uncaughtError = new CompletableFuture<>();
    private final String token = validToken();

    private Path startedWorkingDirectory;
    private String[] startedCommand;
    private Map<String, String> startedEnvironment;
    private Path startedLogFile;

    @BeforeEach
    void beforeEach() {
        dbFile = tempDir.resolve("config.db");
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            uncaughtError.complete(error);
        });
    }

    @AfterEach
    void afterEach() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler);
    }

    @Test
    void shouldStartGameWithClientCommandAndLogFile() {
        addClient();
        Process process = runningProcess();

        Process result = start(process);

        assertThat(result).isSameAs(process);
        assertThat(startedWorkingDirectory).isEqualTo(tempDir.resolve("install"));
        assertThat(startedCommand).containsExactly("umu-run", SLUG + "/mnm.exe", "--token", token);
        assertThat(startedEnvironment).containsKeys("GAMEID", "PROTONPATH", "WINEPREFIX");
        assertThat(startedLogFile).isEqualTo(ClientRunner.logFile(SLUG));
    }

    @Test
    void shouldFailWhenThereIsNoClient() {
        var runner = new DetachedPlayRunner(() -> dbFile);

        assertThatThrownBy(() -> runner.run(runnerOptions()))
            .isInstanceOf(RuntimeException.class)
            // TODO issue #155
            .hasMessageStartingWith("No client found:");
        assertThat(uncaughtError).isNotCompleted();
    }

    @Test
    void shouldReportErrorWhenGameStopsWithErrorEarly() throws Exception {
        addClient();
        Process process = Mockito.mock(Process.class);
        Mockito.when(process.waitFor(EARLY_EXIT_SECONDS, TimeUnit.SECONDS)).thenReturn(true);
        int expectedExitCode = 3;
        Mockito.when(process.exitValue()).thenReturn(expectedExitCode);

        start(process);

        assertThat(uncaughtError.get(5, TimeUnit.SECONDS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Game client stopped with exit code %d. See the log:".formatted(expectedExitCode))
            .hasMessageContaining("run-%s.log".formatted(SLUG));
    }

    @Test
    void shouldNotReportErrorWhenGameStopsWithSuccessEarly() throws Exception {
        addClient();
        Process process = Mockito.mock(Process.class);
        Mockito.when(process.waitFor(EARLY_EXIT_SECONDS, TimeUnit.SECONDS)).thenReturn(true);
        Mockito.when(process.exitValue()).thenReturn(0);

        start(process);

        Mockito.verify(process, Mockito.timeout(5000)).exitValue();
        assertThat(uncaughtError.completeOnTimeout(null, 300, TimeUnit.MILLISECONDS).get()).isNull();
    }

    @Test
    void shouldNotReportErrorWhenGameKeepsRunning() throws Exception {
        addClient();
        Process process = runningProcess();

        start(process);

        Mockito.verify(process, Mockito.timeout(5000)).waitFor(EARLY_EXIT_SECONDS, TimeUnit.SECONDS);
        assertThat(uncaughtError.completeOnTimeout(null, 300, TimeUnit.MILLISECONDS).get()).isNull();
        Mockito.verify(process, Mockito.never()).exitValue();
    }

    private Process start(Process process) {
        try (MockedStatic<OS> osMock = Mockito.mockStatic(OS.class, Mockito.CALLS_REAL_METHODS);
             MockedStatic<ProcessUtils> processMock = Mockito.mockStatic(ProcessUtils.class)) {
            osMock.when(OS::isWindows).thenReturn(false);
            processMock.when(() -> ProcessUtils.runDetached(any(Path.class), any(String[].class), anyMap(), any(Path.class)))
                .thenAnswer(invocation -> {
                    startedWorkingDirectory = invocation.getArgument(0);
                    startedCommand = invocation.getArgument(1);
                    startedEnvironment = invocation.getArgument(2);
                    startedLogFile = invocation.getArgument(3);
                    return process;
                });

            return new DetachedPlayRunner(() -> dbFile).run(runnerOptions());
        }
    }

    private static Process runningProcess() {
        Process process = Mockito.mock(Process.class);
        try {
            Mockito.when(process.waitFor(EARLY_EXIT_SECONDS, TimeUnit.SECONDS)).thenReturn(false);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        return process;
    }

    private void addClient() {
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client(SLUG, "1.0.0", UPDATED, tempDir.resolve("install")));
            configDb.addToken(new Token(SLUG, token));
        }
    }

    private static RunnerOptions runnerOptions() {
        return new RunnerOptions(SLUG, null, true, defaultLinuxOptions());
    }
}
