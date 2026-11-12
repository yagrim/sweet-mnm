package org.mnm.config;

public class Messages {

    public class TwoFactorAuthentication {
        public static final String EMAIL_INSTRUCTIONS = "Enter the six-digit code sent to your email. Check spam if it hasn't arrived.";
        public static final String TOTP_INSTRUCTIONS = "Enter the six-digit code from your authenticator app.";
        public static final String BACKUP_INSTRUCTIONS = "Enter one of your backup codes. You can use each backup code only one time.";

        // Returns null for unknown methods.
        public static String instructions(String method) {
            return switch (method) {
                case "email" -> EMAIL_INSTRUCTIONS;
                case "totp" -> TOTP_INSTRUCTIONS;
                case "backup" -> BACKUP_INSTRUCTIONS;
                case null, default -> null;
            };
        }
    }
}
