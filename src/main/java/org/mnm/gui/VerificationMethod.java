package org.mnm.gui;

import java.util.List;
import java.util.stream.Stream;

import org.mnm.config.Messages;

record VerificationMethod(String value, String label, String instructions, boolean resendable, boolean digitsOnly) {

    private static final int TOTP_CODE_LENGTH = 6;

    static VerificationMethod of(String method) {
        String instructions = Messages.TwoFactorAuthentication.instructions(method);
        return switch (method) {
            case "email" -> new VerificationMethod(method, "Email", instructions, true, false);
            case "totp" -> new VerificationMethod(method, "Authenticator app", instructions, false, true);
            case "backup" -> new VerificationMethod(method, "Backup code", instructions, false, false);
            // ApiConnector refuses unsupported methods, thus this cannot occur.
            case null, default -> throw new IllegalStateException("Unexpected authentication method: " + method);
        };
    }

    // Keeps the order of the server. Adds the selected method if the server does not include it in the list.
    static List<VerificationMethod> of(String selected, List<String> methods) {
        List<String> values = methods == null ? List.of() : methods;
        Stream<String> all = values.contains(selected)
                ? values.stream()
                : Stream.concat(Stream.of(selected), values.stream());
        return all.map(VerificationMethod::of).toList();
    }

    boolean accepts(String code) {
        return !digitsOnly || code.matches("\\d{0," + TOTP_CODE_LENGTH + "}");
    }

    @Override
    public String toString() {
        return label;
    }
}
