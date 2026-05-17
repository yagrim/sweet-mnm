package org.mnm.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallOptionsTest {

    @Test
    void shouldValidateUsernameAndPassword() {
        InstallOptions options = new InstallOptions("username", "password", null);

        assertThatCode(options::validate)
                .doesNotThrowAnyException();
    }

    @Test
    void shouldValidateSlugWithoutCredentials() {
        InstallOptions options = new InstallOptions(null, null, "slug");

        assertThatCode(options::validate)
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldFailWhenUsernameAndSlugAreMissingOrEmpty(String value) {
        InstallOptions options = new InstallOptions(value, "password", value);

        assertThatThrownBy(options::validate)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--username' or '--slug'");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldFailWhenPasswordIsMissingOrEmpty(String value) {
        InstallOptions options = new InstallOptions("username", value, null);

        assertThatThrownBy(options::validate)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--password'");
    }
}
