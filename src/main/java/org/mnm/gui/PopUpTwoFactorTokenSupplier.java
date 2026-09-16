package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.ApiConnector;
import org.mnm.api.ApiException;
import org.mnm.api.TokenSupplier;

import static org.mnm.gui.Style.SCALE;
import static org.mnm.tools.StringUtils.isEmpty;

/**
 * Handles full 2FA flow, including errors and requesting a code resend.
 */
public class PopUpTwoFactorTokenSupplier implements TokenSupplier {

    private final ApiConnector apiConnector;

    public PopUpTwoFactorTokenSupplier(ApiConnector apiConnector) {
        this.apiConnector = apiConnector;
    }

    @Override
    public String getToken(String method, List<String> methods, String challengeToken) {

        VerificationDialog dialog = new VerificationDialog(method, challengeToken, apiConnector);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            throw new CancelException("2FA cancelled");
        }

        return dialog.getToken();
    }

    private static class VerificationDialog extends JDialog {

        private static final Logger logger = LoggerFactory.getLogger(VerificationDialog.class);

        public static final String EMAIL_METHOD = "email";
        private final JTextField codeField = new JTextField(20);
        private final JLabel methodLabel = new JLabel();
        private final JLabel instructionsLabel = new JLabel();
        private final JLabel errorLabel = new JLabel();

        private final ApiConnector apiConnector;

        private boolean confirmed;
        private String token;

        VerificationDialog(String method, String challengeToken, ApiConnector apiConnector) {
            super(null, "Enter verification code", Dialog.ModalityType.APPLICATION_MODAL);
            this.apiConnector = apiConnector;

            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            createUi(method, challengeToken);

            pack();
            setMinimumSize(new Dimension(0, 0));
            setLocationRelativeTo(null);
        }

        private void createUi(String method, String challengeToken) {

            methodLabel.setAlignmentX(CENTER_ALIGNMENT);
            instructionsLabel.setAlignmentX(CENTER_ALIGNMENT);
            if (EMAIL_METHOD.equals(method)) {
                methodLabel.setText("Authentication method: " + method);
                instructionsLabel.setText("Enter the six-digit code sent to your email. Check spam if it hasn't arrived.");
            } else {
                methodLabel.setText("Authentication method UNKNOWN: " + method);
                instructionsLabel.setText(" ");
            }

            errorLabel.setAlignmentX(CENTER_ALIGNMENT);
            errorLabel.setForeground(Color.RED);
            errorLabel.setText(" ");

            JPanel codePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            codePanel.add(new JLabel("CODE"));
            codePanel.add(Box.createHorizontalStrut(SCALE * 3));
            codePanel.add(codeField);
            if (EMAIL_METHOD.equals(method)) {
                JButton resendButton = new JButton("Resend email code");
                codePanel.add(Box.createHorizontalStrut(SCALE * 3));
                codePanel.add(resendButton);
                resendButton.addActionListener(e -> {
                    try {
                        apiConnector.resendVerificationCode(challengeToken, method);
                        setInfoMessage("Verification code sent");
                    } catch (Exception ex) {
                        logger.error("Error calling API", ex);
                        String message = ex instanceof ApiException ? ((ApiException) ex).getError() : ex.getMessage();
                        setErrorMessage(message);
                    }
                });
            } else {
                // TODO implement support for others with a visitor or subclasses for each method
            }

            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

            inputPanel.add(methodLabel);
            inputPanel.add(Box.createVerticalStrut(SCALE));
            inputPanel.add(instructionsLabel);
            inputPanel.add(Box.createVerticalStrut(SCALE * 3));
            inputPanel.add(codePanel);
            inputPanel.add(Box.createVerticalStrut(SCALE));
            inputPanel.add(errorLabel);

            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            JPanel content = new JPanel(new BorderLayout(0, 10));
            content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            content.add(inputPanel, BorderLayout.CENTER);
            content.add(buttonPanel, BorderLayout.SOUTH);

            setContentPane(content);

            okButton.addActionListener(event -> {
                if (getCode().isEmpty()) {
                    errorLabel.setText("Validation code is required");
                    return;
                }

                this.token = getToken(method, codeField.getText(), challengeToken);
                if (token != null) {
                    confirmed = true;
                    dispose();
                }

            });

            cancelButton.addActionListener(event -> {
                confirmed = false;
                dispose();
            });
        }

        private String getToken(String method, String code, String challengeToken) {
            try {
                return apiConnector.twoFactorAuthentication(method, code, challengeToken);
            } catch (ApiException ex) {
                String error = ex.getError();
                if (!isEmpty(error)) {
                    setErrorMessage(error);
                }
                return null;
            }
        }

        boolean isConfirmed() {
            return confirmed;
        }

        private String getCode() {
            return codeField.getText().trim();
        }

        void setErrorMessage(String message) {
            errorLabel.setForeground(Color.RED);
            errorLabel.setText(message);
        }

        void setInfoMessage(String message) {
            errorLabel.setForeground(Color.DARK_GRAY);
            errorLabel.setText(message);
        }

        String getToken() {
            return token;
        }
    }

}
