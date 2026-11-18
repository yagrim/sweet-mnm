package org.mnm.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void shouldRedactToken() {
        String[] command = {"mnm.exe", "--token", "secret-token"};

        assertThat(StringUtils.redact(command)).containsExactly("mnm.exe", "--token", "***");
    }

    @Test
    void shouldNotChangeOriginalCommand() {
        String[] command = {"mnm.exe", "--token", "secret-token"};

        StringUtils.redact(command);

        assertThat(command).containsExactly("mnm.exe", "--token", "secret-token");
    }

    @Test
    void shouldKeepCommandWithoutToken() {
        String[] command = {"xxhsum", "-H2", "file"};

        assertThat(StringUtils.redact(command)).containsExactly("xxhsum", "-H2", "file");
    }

    @Test
    void shouldKeepTokenFlagWithoutValue() {
        String[] command = {"mnm.exe", "--token"};

        assertThat(StringUtils.redact(command)).containsExactly("mnm.exe", "--token");
    }
}
