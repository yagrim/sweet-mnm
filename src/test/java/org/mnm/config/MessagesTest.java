package org.mnm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.Messages.TwoFactorAuthentication.BACKUP_INSTRUCTIONS;
import static org.mnm.config.Messages.TwoFactorAuthentication.EMAIL_INSTRUCTIONS;
import static org.mnm.config.Messages.TwoFactorAuthentication.TOTP_INSTRUCTIONS;
import static org.mnm.config.Messages.TwoFactorAuthentication.instructions;

class MessagesTest {

    @Test
    void shouldReturnInstructionsForKnownMethods() {
        assertThat(instructions("email")).isEqualTo(EMAIL_INSTRUCTIONS);
        assertThat(instructions("totp")).isEqualTo(TOTP_INSTRUCTIONS);
        assertThat(instructions("backup")).isEqualTo(BACKUP_INSTRUCTIONS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sms", ""})
    void shouldReturnNullForUnknownMethods(String method) {
        assertThat(instructions(method)).isNull();
    }

    @Test
    void shouldReturnNullForNullMethod() {
        assertThat(instructions(null)).isNull();
    }
}
