package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import org.mnm.api.VerificationCodeSupplier;

import static org.mnm.gui.Style.SCALE;

public class PopUpVerificationCodeSupplier implements VerificationCodeSupplier {

    private final ApiConnector apiConnector;

    public PopUpVerificationCodeSupplier(ApiConnector apiConnector) {
        this.apiConnector = apiConnector;
    }

    @Override
    public String getVerificationCode(String method, List<String> methods, String challengeToken) {

        VerificationDialog dialog = new VerificationDialog(method, challengeToken, apiConnector);
        dialog.setVisible(true);

        // TODO close login process without a popup or error window
        if (!dialog.isConfirmed()) {
            throw new CancelException("2FA cancelled");
        }

        String code = dialog.getCode();
        // TODO any validation?
        return code;
    }

    private static class VerificationDialog extends JDialog {

        private static final Logger logger = LoggerFactory.getLogger(VerificationDialog.class);

        public static final String EMAIL_METHOD = "email";
        private final JTextField codeField = new JTextField(20);
        private final JLabel methodLabel = new JLabel();
        private final JLabel instructionsLabel = new JLabel();
        private final JLabel errorLabel = new JLabel();

        private boolean confirmed;

        VerificationDialog(String method, String challengeToken, ApiConnector apiConnector) {
            super(
                null,
                "Enter verification code",
                Dialog.ModalityType.APPLICATION_MODAL
            );

            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            createUi(method, challengeToken, apiConnector);

            pack();
            setMinimumSize(new Dimension(0, 0));
            setLocationRelativeTo(null);
        }

        private void createUi(String method, String challengeToken, ApiConnector apiConnector) {

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
                        errorLabel.setForeground(Color.GREEN);
                        errorLabel.setText("Verification code sent");
                    } catch (Exception ex) {
                        logger.error("Error calling API", ex);
                        if (ex instanceof ApiException) {
                            errorLabel.setText(((ApiException) ex).getError());
                        } else {
                            // TODO handle messages that are too long and don't fit a Label
                            errorLabel.setText(ex.getMessage());
                        }
                    }
                });
            } else {
                // TODO implement support for other with a visitor or subclasses for each method
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

                confirmed = true;
                dispose();
            });


            cancelButton.addActionListener(event -> {
                confirmed = false;
                dispose();
            });

            codeField.addActionListener(event -> {
                okButton.doClick();
            });

            codeField.getDocument().addDocumentListener(
                new DocumentListener() {

                    private void updateError() {
                        if (!codeField.getText().trim().isEmpty()) {
                            errorLabel.setText("");
                            pack();
                        }
                    }

                    @Override
                    public void insertUpdate(DocumentEvent event) {
                        updateError();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent event) {
                        updateError();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent event) {
                        updateError();
                    }
                }
            );
        }

        boolean isConfirmed() {
            return confirmed;
        }

        String getCode() {
            return codeField.getText().trim();
        }
    }

}
