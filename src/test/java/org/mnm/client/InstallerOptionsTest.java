package org.mnm.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mnm.cli.Arguments;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.*;
import static org.mnm.client.InstallerOptions.FileCheck.inmemory;
import static org.mnm.client.InstallerOptions.FileCheck.xxhsum;

class InstallerOptionsTest {

    @Nested
    class Parse {

        @Test
        void shouldReturnDefaults() {
            Arguments arguments = Arguments.parse();

            InstallerOptions options = InstallerOptions.parse(arguments);

            assertThat(options)
                .isEqualTo(new InstallerOptions(null, null, null, xxhsum));
        }

        @Test
        void shouldParseAllOptions() {
            Arguments arguments = Arguments.parse(
                "--username", "me",
                "--password", "42",
                "--slug", "mnm",
                "--file-check", "in-memory");

            InstallerOptions options = InstallerOptions.parse(arguments);

            assertThat(options)
                .isEqualTo(new InstallerOptions("me", "42", "mnm", inmemory));
        }

        @Test
        void shouldNotParseInvalidFileCheck() {
            Arguments arguments = Arguments.parse("--file-check", "in-valid");

            InstallerOptions options = InstallerOptions.parse(arguments);

            assertThat(options)
                .isEqualTo(new InstallerOptions(null, null, null, null));
        }

    }

    @Nested
    class ValidateInstall {

        @Test
        void shouldAllowValidOptions() {
            InstallerOptions options = new InstallerOptions("me", "secret", null, xxhsum);

            assertThatCode(options::validateInstall)
                .doesNotThrowAnyException();
        }

        @Test
        void shouldPanicWhenUsernameIsMissing() {
            InstallerOptions options = new InstallerOptions(null, "secret", null, xxhsum);

            assertThatThrownBy(options::validateInstall)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--username'");
        }

        @Test
        void shouldPanicWhenPasswordIsMissing() {
            InstallerOptions options = new InstallerOptions("me", null, null, xxhsum);

            assertThatThrownBy(options::validateInstall)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--password'");
        }

        @Test
        void shouldPanicWhenFileCheckIsInvalid() {
            InstallerOptions options = new InstallerOptions("me", "secret", null, null);

            assertThatThrownBy(options::validateInstall)
                .isInstanceOf(PanicException.class)
                .hasMessage("Invalid File Check value, use 'in-memory' or 'xxhsum'");
        }
    }

    @Nested
    class ValidateRepair {

        @Test
        void shouldAllowValidOptions() {
            InstallerOptions options = new InstallerOptions(null, null, "mnm", inmemory);

            assertThatCode(options::validateRepair)
                .doesNotThrowAnyException();
        }

        @Test
        void shouldPanicWhenSlugIsMissing() {
            InstallerOptions options = new InstallerOptions(null, null, null, inmemory);

            assertThatThrownBy(options::validateRepair)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--slug'");
        }

        @Test
        void shouldPanicWhenFileCheckIsInvalid() {
            InstallerOptions options = new InstallerOptions(null, null, "mnm", null);

            assertThatThrownBy(options::validateRepair)
                .isInstanceOf(PanicException.class)
                .hasMessage("Invalid File Check value, use 'in-memory' or 'xxhsum'");
        }
    }
}
