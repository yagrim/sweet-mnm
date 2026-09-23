package org.mnm.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.mnm.config.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessUtilsTest {

    @Test
    void shouldFailIfCommandDoesNotExist() {
        assertThatThrownBy(() -> ProcessUtils.run(Environment.getWorkDir(), new String[]{"no-no-no", "arg0", "arg-two"}))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Process failed: no-no-no arg0 arg-two")
            .cause()
            .isInstanceOf(java.io.IOException.class);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldRunCommand() {
        String output = ProcessUtils.run(Environment.getWorkDir(), new String[]{"ls", "-la"});

        assertThat(output)
            .contains("src")
            .contains("build.gradle")
            .contains("settings.gradle");
    }
}
