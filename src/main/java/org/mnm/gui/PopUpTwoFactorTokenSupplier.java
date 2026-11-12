package org.mnm.gui;

import java.util.List;

import org.mnm.api.ApiConnector;
import org.mnm.api.TokenSupplier;

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
        VerificationDialog dialog = new VerificationDialog(method, methods, challengeToken, apiConnector);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            throw new CancelException("2FA cancelled");
        }

        return dialog.getToken();
    }

}
