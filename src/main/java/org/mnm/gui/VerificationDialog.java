package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.ApiConnector;
import org.mnm.api.ApiException;

import static org.mnm.gui.Style.SCALE;
import static org.mnm.tools.StringUtils.isEmpty;

class VerificationDialog extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(VerificationDialog.class);

    private final JTextField codeField = new JTextField(20);
    private final JLabel methodLabel = new JLabel();
    private final JLabel instructionsLabel = new JLabel();
    private final JLabel errorLabel = new JLabel();
    private final JButton resendButton = new JButton("Resend email code");
    private final Component resendGap = Box.createHorizontalStrut(SCALE * 3);
    private final JButton okButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");
    private JComboBox<VerificationMethod> methodSelector;

    private final ApiConnector apiConnector;

    private VerificationMethod method;
    private boolean confirmed;
    private String token;

    VerificationDialog(String method, List<String> methods, String challengeToken, ApiConnector apiConnector) {
        super(null, "Enter verification code", Dialog.ModalityType.APPLICATION_MODAL);
        this.apiConnector = apiConnector;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        createUi(method, methods, challengeToken);

        pack();
        setMinimumSize(new Dimension(0, 0));
        setLocationRelativeTo(null);
    }

    private void createUi(String selectedMethod, List<String> methods, String challengeToken) {

        List<VerificationMethod> verificationMethods = VerificationMethod.of(selectedMethod, methods);

        JComponent methodComponent;
        if (verificationMethods.size() > 1) {
            methodSelector = new JComboBox<>(verificationMethods.toArray(new VerificationMethod[]{}));
            methodSelector.setSelectedItem(VerificationMethod.of(selectedMethod));
            methodSelector.addActionListener(_ -> selectMethod((VerificationMethod) methodSelector.getSelectedItem()));
            JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            methodPanel.add(new JLabel("Authentication method: "));
            methodPanel.add(methodSelector);
            methodComponent = methodPanel;
        } else {
            methodLabel.setText("Authentication method: " + VerificationMethod.of(selectedMethod).label());
            methodComponent = methodLabel;
        }
        methodComponent.setAlignmentX(CENTER_ALIGNMENT);
        instructionsLabel.setAlignmentX(CENTER_ALIGNMENT);

        errorLabel.setAlignmentX(CENTER_ALIGNMENT);
        errorLabel.setForeground(Color.RED);
        errorLabel.setText(" ");

        ((AbstractDocument) codeField.getDocument()).setDocumentFilter(new CodeFilter());

        JPanel codePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        codePanel.add(new JLabel("CODE"));
        codePanel.add(Box.createHorizontalStrut(SCALE * 3));
        codePanel.add(codeField);
        codePanel.add(resendGap);
        codePanel.add(resendButton);
        resendButton.addActionListener(_ -> {
            try {
                apiConnector.resendVerificationCode(challengeToken, method.value());
                setInfoMessage("Verification code sent");
            } catch (Exception ex) {
                logger.error("Error calling API", ex);
                String message = ex instanceof ApiException ? ((ApiException) ex).getError() : ex.getMessage();
                setErrorMessage(message);
            }
        });

        selectMethod(VerificationMethod.of(selectedMethod));

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        inputPanel.add(methodComponent);
        inputPanel.add(Box.createVerticalStrut(SCALE));
        inputPanel.add(instructionsLabel);
        inputPanel.add(Box.createVerticalStrut(SCALE * 3));
        inputPanel.add(codePanel);
        inputPanel.add(Box.createVerticalStrut(SCALE));
        inputPanel.add(errorLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(inputPanel, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);

        okButton.addActionListener(_ -> {
            if (getCode().isEmpty()) {
                errorLabel.setText("Validation code is required");
                return;
            }

            this.token = getToken(method.value(), getCode(), challengeToken);
            if (token != null) {
                confirmed = true;
                dispose();
            }

        });

        cancelButton.addActionListener(_ -> {
            confirmed = false;
            dispose();
        });
    }

    private void selectMethod(VerificationMethod method) {
        this.method = method;
        instructionsLabel.setText(method.instructions());
        resendButton.setVisible(method.resendable());
        resendGap.setVisible(method.resendable());
        if (!method.accepts(codeField.getText())) {
            codeField.setText("");
        }
        errorLabel.setText(" ");
        pack();
    }

    // Rejects changes that make the code invalid for the selected method. For example, letters for TOTP.
    private class CodeFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String next = current.substring(0, offset) + (text == null ? "" : text) + current.substring(offset + length);
            if (method == null || method.accepts(next)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
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
