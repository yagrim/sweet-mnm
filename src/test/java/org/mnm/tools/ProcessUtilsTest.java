package org.mnm.tools;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.config.Environment.getWorkDir;

class ProcessUtilsTest {

    @Test
    void shouldHandleMissingCommand() {
        assertThatThrownBy(() -> ProcessUtils.run(getWorkDir(), new String[]{"no-no-no", "arg0", "arg-two"}))
            .isInstanceOf(CommandNotFound.class)
            .hasMessage("no-no-no")
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldHandleUnexpectedError() {
        assertThatThrownBy(() -> ProcessUtils.run(getWorkDir(), new String[]{"ls", "thingy"}))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Process failed: exitCode=2, stdout=, stderr=ls: cannot access 'thingy': No such file or directory")
            .hasNoCause();
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldRunCommand() {
        String output = ProcessUtils.run(getWorkDir(), new String[]{"ls", "-la"});

        assertThat(output)
            .contains("src")
            .contains("build.gradle")
            .contains("settings.gradle");
    }
}
