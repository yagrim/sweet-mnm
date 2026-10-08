package org.mnm.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mnm.cli.Arguments;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialsTest {

    @Nested
    class Parse {

        @Test
        void shouldReadUsernameAndPassword() {
            Credentials credentials = Credentials.parse(Arguments.parse(
                "--username", "alice",
                "--password", "secret"));

            assertThat(credentials).isEqualTo(new Credentials("alice", "secret"));
        }
    }

    @Nested
    class Validate {

        @Test
        void shouldAcceptNonEmptyUsernameAndPassword() {
            Credentials credentials = new Credentials("alice", "secret");

            assertThatCode(credentials::validate).doesNotThrowAnyException();
        }

        @Test
        void shouldPanicWhenUsernameIsMissing() {
            Credentials credentials = new Credentials(null, "secret");

            assertThatThrownBy(credentials::validate)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--username'");
        }

        @Test
        void shouldPanicWhenUsernameIsEmpty() {
            Credentials credentials = new Credentials("", "secret");

            assertThatThrownBy(credentials::validate)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--username'");
        }

        @Test
        void shouldPanicWhenPasswordIsMissing() {
            Credentials credentials = new Credentials("alice", null);

            assertThatThrownBy(credentials::validate)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--password'");
        }

        @Test
        void shouldPanicWhenPasswordIsEmpty() {
            Credentials credentials = new Credentials("alice", "");

            assertThatThrownBy(credentials::validate)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--password'");
        }
    }
}
