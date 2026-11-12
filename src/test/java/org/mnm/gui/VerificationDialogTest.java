package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GraphicsEnvironment;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.mnm.api.ApiConnector;
import org.mnm.api.ApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mnm.config.Messages.TwoFactorAuthentication.BACKUP_INSTRUCTIONS;
import static org.mnm.config.Messages.TwoFactorAuthentication.EMAIL_INSTRUCTIONS;
import static org.mnm.config.Messages.TwoFactorAuthentication.TOTP_INSTRUCTIONS;
import static org.mnm.gui.ReflectionTestTools.get;

class VerificationDialogTest {

    private static final String CHALLENGE_TOKEN = "my-challenge-token";

    private ApiConnector apiConnector;
    private VerificationDialog dialog;

    @BeforeEach
    void setup() {
        // JDialog cannot be created without a display (Linux CI).
        assumeFalse(GraphicsEnvironment.isHeadless(), "No display available");
        apiConnector = Mockito.mock(ApiConnector.class);
    }

    @AfterEach
    void tearDown() {
        if (dialog != null) {
            dialog.dispose();
        }
    }

    @Test
    void shouldShowLabelAndNoSelectorForSingleMethod() {
        dialog = new VerificationDialog("email", List.of("email"), CHALLENGE_TOKEN, apiConnector);

        assertThat(label("methodLabel").getText()).isEqualTo("Authentication method: Email");
        assertThat(methodSelector()).isNull();
    }

    @Test
    void shouldShowEmailInstructionsAndResendButton() {
        dialog = new VerificationDialog("email", List.of("email"), CHALLENGE_TOKEN, apiConnector);

        assertThat(label("instructionsLabel").getText()).isEqualTo(EMAIL_INSTRUCTIONS);
        assertThat(button("resendButton").isVisible()).isTrue();
    }

    @Test
    void shouldShowTotpInstructionsWithoutResendButton() {
        dialog = new VerificationDialog("totp", List.of("totp"), CHALLENGE_TOKEN, apiConnector);

        assertThat(label("instructionsLabel").getText()).isEqualTo(TOTP_INSTRUCTIONS);
        assertThat(button("resendButton").isVisible()).isFalse();
    }

    @Test
    void shouldShowSelectorWithServerMethods() {
        dialog = new VerificationDialog("totp", List.of("email", "totp", "backup"), CHALLENGE_TOKEN, apiConnector);

        JComboBox<VerificationMethod> selector = methodSelector();
        assertThat(selector.getItemCount()).isEqualTo(3);
        assertThat(selector.getItemAt(0).value()).isEqualTo("email");
        assertThat(selector.getItemAt(1).value()).isEqualTo("totp");
        assertThat(selector.getItemAt(2).value()).isEqualTo("backup");
        assertThat(((VerificationMethod) selector.getSelectedItem()).value()).isEqualTo("totp");
    }

    @Test
    void shouldUpdateUiWhenMethodChanges() {
        dialog = new VerificationDialog("totp", List.of("email", "totp", "backup"), CHALLENGE_TOKEN, apiConnector);

        methodSelector().setSelectedItem(VerificationMethod.of("email"));

        assertThat(label("instructionsLabel").getText()).isEqualTo(EMAIL_INSTRUCTIONS);
        assertThat(button("resendButton").isVisible()).isTrue();

        methodSelector().setSelectedItem(VerificationMethod.of("backup"));

        assertThat(label("instructionsLabel").getText()).isEqualTo(BACKUP_INSTRUCTIONS);
        assertThat(button("resendButton").isVisible()).isFalse();
    }

    @Test
    void shouldSendSelectedMethod() {
        Mockito.when(apiConnector.twoFactorAuthentication("backup", "abcd-1234", CHALLENGE_TOKEN)).thenReturn("token");
        dialog = new VerificationDialog("totp", List.of("email", "totp", "backup"), CHALLENGE_TOKEN, apiConnector);

        methodSelector().setSelectedItem(VerificationMethod.of("backup"));
        codeField().setText("abcd-1234");
        button("okButton").doClick();

        assertThat(dialog.isConfirmed()).isTrue();
        assertThat(dialog.getToken()).isEqualTo("token");
    }

    @Test
    void shouldRequireCode() {
        dialog = new VerificationDialog("totp", List.of("totp"), CHALLENGE_TOKEN, apiConnector);

        button("okButton").doClick();

        assertThat(dialog.isConfirmed()).isFalse();
        assertThat(label("errorLabel").getText()).isEqualTo("Validation code is required");
        Mockito.verifyNoInteractions(apiConnector);
    }

    @Test
    void shouldShowApiErrorAndStayOpen() {
        ApiException exception = Mockito.mock(ApiException.class);
        Mockito.when(exception.getError()).thenReturn("Invalid verification code. Please try again.");
        Mockito.when(apiConnector.twoFactorAuthentication("totp", "123456", CHALLENGE_TOKEN)).thenThrow(exception);
        dialog = new VerificationDialog("totp", List.of("totp"), CHALLENGE_TOKEN, apiConnector);

        codeField().setText("123456");
        button("okButton").doClick();

        assertThat(dialog.isConfirmed()).isFalse();
        assertThat(dialog.getToken()).isNull();
        assertThat(label("errorLabel").getText()).isEqualTo("Invalid verification code. Please try again.");
    }

    @Test
    void shouldCancel() {
        dialog = new VerificationDialog("totp", List.of("totp"), CHALLENGE_TOKEN, apiConnector);

        button("cancelButton").doClick();

        assertThat(dialog.isConfirmed()).isFalse();
        Mockito.verifyNoInteractions(apiConnector);
    }

    @Test
    void shouldResendEmailCode() {
        dialog = new VerificationDialog("email", List.of("email"), CHALLENGE_TOKEN, apiConnector);

        button("resendButton").doClick();

        Mockito.verify(apiConnector).resendVerificationCode(CHALLENGE_TOKEN, "email");
        assertThat(label("errorLabel").getText()).isEqualTo("Verification code sent");
    }

    @Test
    void shouldShowResendError() {
        ApiException exception = Mockito.mock(ApiException.class);
        Mockito.when(exception.getError()).thenReturn("Too many attempts. Wait a minute and try again.");
        Mockito.doThrow(exception).when(apiConnector).resendVerificationCode(CHALLENGE_TOKEN, "email");
        dialog = new VerificationDialog("email", List.of("email"), CHALLENGE_TOKEN, apiConnector);

        button("resendButton").doClick();

        assertThat(label("errorLabel").getText()).isEqualTo("Too many attempts. Wait a minute and try again.");
    }

    @Test
    void shouldRejectInvalidTotpInput() {
        dialog = new VerificationDialog("totp", List.of("totp"), CHALLENGE_TOKEN, apiConnector);

        codeField().setText("12a456");
        assertThat(codeField().getText()).isEmpty();

        codeField().setText("1234567");
        assertThat(codeField().getText()).isEmpty();

        codeField().setText("123456");
        assertThat(codeField().getText()).isEqualTo("123456");
    }

    @Test
    void shouldClearInvalidCodeWhenChangingToTotp() {
        dialog = new VerificationDialog("backup", List.of("backup", "totp"), CHALLENGE_TOKEN, apiConnector);
        codeField().setText("abcd-1234");

        methodSelector().setSelectedItem(VerificationMethod.of("totp"));

        assertThat(codeField().getText()).isEmpty();
    }

    private JLabel label(String name) {
        return (JLabel) get(dialog, name);
    }

    private JButton button(String name) {
        return (JButton) get(dialog, name);
    }

    private JTextField codeField() {
        return (JTextField) get(dialog, "codeField");
    }

    @SuppressWarnings("unchecked")
    private JComboBox<VerificationMethod> methodSelector() {
        return (JComboBox<VerificationMethod>) get(dialog, "methodSelector");
    }
}
