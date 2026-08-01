package org.mnm.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mnm.cli.Arguments;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.client.LinuxOptionsTestFactory.defaultLinuxOptions;
import static org.mnm.client.LinuxOptionsTestFactory.linuxOptions;

class RunnerOptionsTest {

    @Nested
    class Parse {

        @Test
        void shouldReturnDefaults() {
            Arguments arguments = Arguments.parse();

            RunnerOptions options = RunnerOptions.parse(arguments);

            assertThat(options)
                .isEqualTo(new RunnerOptions(null, null, false, defaultLinuxOptions()));
        }

        @Test
        void shouldParseAllOptions() {
            Arguments arguments = Arguments.parse(
                "--slug", "mnm",
                "--id", "42",
                "--skip-version-check",
                "--enable-mangohud");

            RunnerOptions options = RunnerOptions.parse(arguments);

            assertThat(options)
                .isEqualTo(new RunnerOptions("mnm", 42, true, linuxOptions(true)));
        }

        @Test
        void shouldPanicWhenTokenIdIsInvalid() {
            Arguments arguments = Arguments.parse("--id", "not-a-number");

            assertThatThrownBy(() -> RunnerOptions.parse(arguments))
                .isInstanceOf(PanicException.class)
                .hasMessage("Invalid token id: not-a-number");
        }
    }

}
