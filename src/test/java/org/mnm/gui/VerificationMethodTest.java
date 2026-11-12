package org.mnm.gui;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.config.Messages.TwoFactorAuthentication.BACKUP_INSTRUCTIONS;
import static org.mnm.config.Messages.TwoFactorAuthentication.EMAIL_INSTRUCTIONS;
import static org.mnm.config.Messages.TwoFactorAuthentication.TOTP_INSTRUCTIONS;

class VerificationMethodTest {

    @Test
    void shouldShowResendButtonForEmail() {
        VerificationMethod method = VerificationMethod.of("email");

        assertThat(method.label()).isEqualTo("Email");
        assertThat(method.instructions()).isEqualTo(EMAIL_INSTRUCTIONS);
        assertThat(method.resendable()).isTrue();
        assertThat(method.toString()).isEqualTo("Email");
    }

    @Test
    void shouldNotShowResendButtonForTotp() {
        VerificationMethod method = VerificationMethod.of("totp");

        assertThat(method.label()).isEqualTo("Authenticator app");
        assertThat(method.instructions()).isEqualTo(TOTP_INSTRUCTIONS);
        assertThat(method.resendable()).isFalse();
    }

    @Test
    void shouldNotShowResendButtonForBackup() {
        VerificationMethod method = VerificationMethod.of("backup");

        assertThat(method.label()).isEqualTo("Backup code");
        assertThat(method.instructions()).isEqualTo(BACKUP_INSTRUCTIONS);
        assertThat(method.resendable()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sms", ""})
    @NullSource
    void shouldFailWithUnexpectedMethod(String method) {
        assertThatThrownBy(() -> VerificationMethod.of(method))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unexpected authentication method: " + method);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1", "123456"})
    void shouldAcceptDigitsUpToSixForTotp(String code) {
        assertThat(VerificationMethod.of("totp").accepts(code)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "12a456", "abc", "123 45"})
    void shouldRejectInvalidTotpCodes(String code) {
        assertThat(VerificationMethod.of("totp").accepts(code)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "backup"})
    void shouldAcceptAnyCodeForOtherMethods(String method) {
        assertThat(VerificationMethod.of(method).accepts("abcd-1234-efgh")).isTrue();
    }

    @Test
    void shouldKeepServerOrderOfMethods() {
        List<VerificationMethod> methods = VerificationMethod.of("totp", List.of("email", "totp", "backup"));

        assertThat(methods).extracting(VerificationMethod::value).containsExactly("email", "totp", "backup");
    }

    @Test
    void shouldAddSelectedMethodWhenServerDoesNotListIt() {
        List<VerificationMethod> methods = VerificationMethod.of("totp", List.of("email"));

        assertThat(methods).extracting(VerificationMethod::value).containsExactly("totp", "email");
    }

    @Test
    void shouldUseSelectedMethodWhenServerSendsNoList() {
        List<VerificationMethod> methods = VerificationMethod.of("email", null);

        assertThat(methods).extracting(VerificationMethod::value).containsExactly("email");
    }

    @Test
    void shouldUseSelectedMethodWhenServerSendsEmptyList() {
        List<VerificationMethod> methods = VerificationMethod.of("backup", List.of());

        assertThat(methods).extracting(VerificationMethod::value).containsExactly("backup");
    }

    @Test
    void shouldFailWhenSelectedMethodIsUnexpected() {
        assertThatThrownBy(() -> VerificationMethod.of("sms", List.of("email")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unexpected authentication method: sms");
    }
}
