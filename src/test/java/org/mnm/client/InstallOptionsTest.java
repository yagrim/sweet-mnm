package org.mnm.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import org.mnm.cli.Arguments;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.*;
import static org.mnm.client.InstallOptions.FileCheck.inmemory;
import static org.mnm.client.InstallOptions.FileCheck.xxhsum;

class InstallOptionsTest {

    @Test
    void shouldValidateUsernameAndPassword() {
        InstallOptions options = new InstallOptions("username", "password", null, xxhsum);

        assertThatCode(options::validate)
            .doesNotThrowAnyException();
    }

    @Test
    void shouldValidateSlugWithoutCredentials() {
        InstallOptions options = new InstallOptions(null, null, "slug", xxhsum);

        assertThatCode(options::validate)
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldFailWhenUsernameAndSlugAreMissingOrEmpty(String value) {
        InstallOptions options = new InstallOptions(value, "password", value, xxhsum);

        assertThatThrownBy(options::validate)
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--username' or '--slug'");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldFailWhenPasswordIsMissingOrEmpty(String value) {
        InstallOptions options = new InstallOptions("username", value, null, xxhsum);

        assertThatThrownBy(options::validate)
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--password'");
    }

    @Test
    void shouldFailWhenFileCheckIsNull() {
        InstallOptions options = new InstallOptions(null, null, "mnm", null);

        assertThatThrownBy(options::validate)
            .isInstanceOf(PanicException.class)
            .hasMessage("Invalid File Check value, use 'in-memory' or 'xxhsum'");
    }

    @Nested
    class Parse {

        @Test
        void shouldReturnDefaults() {
            Arguments arguments = Arguments.parse();

            InstallOptions options = InstallOptions.parse(arguments);

            assertThat(options)
                .isEqualTo(new InstallOptions(null, null, null, xxhsum));
        }

        @Test
        void shouldParseAllOptions() {
            Arguments arguments = Arguments.parse(
                "--username", "me",
                "--password", "42",
                "--slug", "mnm",
                "--file-check", "in-memory");

            InstallOptions options = InstallOptions.parse(arguments);

            assertThat(options)
                .isEqualTo(new InstallOptions("me", "42", "mnm", inmemory));
        }

        @Test
        void shouldNotParseInvalidFileCheck() {
            Arguments arguments = Arguments.parse("--file-check", "in-valid");

            InstallOptions options = InstallOptions.parse(arguments);

            assertThat(options)
                .isEqualTo(new InstallOptions(null, null, null, null));
        }

    }
}
