package org.mnm.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.config.Environment.getWorkDir;

class ProcessUtilsTest {

    private static final String SECRET_TOKEN = "secret-token";
    private static final ClientCommand INVALID_COMMAND = ClientCommand.of("tool", "bad\u0000arg", "--token", SECRET_TOKEN);

    @Test
    void shouldHandleMissingCommand() {
        assertThatThrownBy(() -> ProcessUtils.runAndWait(getWorkDir(), ClientCommand.of("no-no-no", "arg0", "arg-two")))
            .isInstanceOf(CommandNotFound.class)
            .hasMessage("no-no-no")
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldHandleUnexpectedError() {
        assertThatThrownBy(() -> ProcessUtils.runAndWait(getWorkDir(), ClientCommand.of("ls", "thingy")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Process failed: exitCode=2, stdout=, stderr=ls: cannot access 'thingy': No such file or directory")
            .hasNoCause();
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldRunCommand() {
        String output = ProcessUtils.runAndWait(getWorkDir(), ClientCommand.of("ls", "-la"));

        assertThat(output)
            .contains("src")
            .contains("build.gradle")
            .contains("settings.gradle");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldStartProcessWithoutWaiting(@TempDir Path tempDir) throws Exception {
        long start = System.nanoTime();
        Process process = ProcessUtils.runDetached(getWorkDir(), ClientCommand.of("sleep", "5"), null, tempDir.resolve("out.log"));
        try {
            long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

            assertThat(elapsedMillis).isLessThan(2000);
            assertThat(process.isAlive()).isTrue();
        } finally {
            process.destroyForcibly();
        }
    }

    @Test
    void shouldNotShowTokenWhenRunFails() {
        assertThatThrownBy(() -> ProcessUtils.runAndWait(getWorkDir(), INVALID_COMMAND))
            .hasMessageEndingWith("--token ***")
            .hasMessageNotContaining(SECRET_TOKEN);
    }

    @Test
    void shouldNotShowTokenWhenStartFails() {
        assertThatThrownBy(() -> ProcessUtils.runDetached(getWorkDir(), INVALID_COMMAND, Map.of(), null))
            .hasMessageEndingWith("--token ***")
            .hasMessageNotContaining(SECRET_TOKEN);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldWriteProcessOutputToLogFile(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("logs").resolve("out.log");

        Process process = ProcessUtils.runDetached(getWorkDir(),
            ClientCommand.of("sh", "-c", "echo hello; echo problem >&2"), null, logFile);
        process.waitFor(5, TimeUnit.SECONDS);

        assertThat(Files.readString(logFile)).contains("hello").contains("problem");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldPassEnvironmentToStartedProcess(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("out.log");

        Process process = ProcessUtils.runDetached(getWorkDir(),
            ClientCommand.of("sh", "-c", "echo $SWEET_TEST_VALUE"), Map.of("SWEET_TEST_VALUE", "abc"), logFile);
        process.waitFor(5, TimeUnit.SECONDS);

        assertThat(Files.readString(logFile)).contains("abc");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldStartProcessWithoutLogFile() throws Exception {
        Process process = ProcessUtils.runDetached(getWorkDir(), ClientCommand.of("echo", "hello"), null, null);

        assertThat(process.waitFor(5, TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).isZero();
    }

    @Test
    void shouldHandleMissingCommandOnStart(@TempDir Path tempDir) {
        assertThatThrownBy(() -> ProcessUtils.runDetached(getWorkDir(), ClientCommand.of("no-no-no", "arg0"), null, tempDir.resolve("out.log")))
            .isInstanceOf(CommandNotFound.class)
            .hasMessage("no-no-no")
            .hasCauseInstanceOf(IOException.class);
    }
}
