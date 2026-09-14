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

import org.mnm.api.RestClient;
import org.mnm.api.VerificationCodeSupplier;

public class PopUpVerificationCodeSupplier implements VerificationCodeSupplier {

    private static final String ACCOUNT_API_URL = "https://account.monstersandmemories.com/api";

    private final RestClient restClient;

    public PopUpVerificationCodeSupplier() {
        this(new RestClient(ACCOUNT_API_URL));
    }

    PopUpVerificationCodeSupplier(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String getVerificationCode(String method, List<String> methods, String challengeToken) {

        VerificationDialog dialog = new VerificationDialog();
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

        private final JTextField codeField = new JTextField(20);
        private final JLabel errorLabel = new JLabel();

        private boolean confirmed;

        VerificationDialog() {
            super(
                null,
                "Input verification code",
                Dialog.ModalityType.APPLICATION_MODAL
            );

            createUi();

            setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
            );

            pack();
            setMinimumSize(new Dimension(0, 0));
            setLocationRelativeTo(null);
        }

        private void createUi() {
            errorLabel.setForeground(Color.RED);
            errorLabel.setText("");

            JPanel codePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            codePanel.add(new JLabel("Code"));
            codePanel.add(codeField);

            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

            inputPanel.add(codePanel);
            inputPanel.add(Box.createVerticalStrut(5));
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
                String code = getCode();

                if (code.isEmpty()) {
                    errorLabel.setText("Empty code found");
                    pack();
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
