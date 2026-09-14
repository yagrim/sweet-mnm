package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

import org.mnm.api.JsonResponse;
import org.mnm.api.RestClient;
import org.mnm.api.VerificationCodeSupplier;

import static org.mnm.api.HttpHelper.parseResponse;

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
        JTextField codeField = new JTextField(20);
        JButton okButton = new JButton("Ok");
        JButton cancelButton = new JButton("Cancel");
        okButton.setEnabled(false);
        codeField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateOkButton();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateOkButton();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateOkButton();
            }

            private void updateOkButton() {
                okButton.setEnabled(!codeField.getText().isEmpty());
            }
        });

        JPanel input = new JPanel(new GridLayout(1, 2));
        input.add(new JLabel("Code"));
        input.add(codeField);
        JOptionPane optionPane = new JOptionPane(
            input,
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            null,
            new Object[]{okButton, cancelButton},
            okButton
        );
        JDialog dialog = optionPane.createDialog("Input verification code");
        dialog.setModal(true);
        dialog.setVisible(true);

        if (optionPane.getValue() != okButton) {
            throw new RuntimeException("Login cancelled");
        }

        JsonResponse response = parseResponse(restClient.post("account/login/2fa", Map.of(
            "challenge_token", challengeToken,
            "method", method,
            "code", codeField.getText()
        )));
        return response.get("token");
    }
}
