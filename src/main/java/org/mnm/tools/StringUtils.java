package org.mnm.tools;

public class StringUtils {

    public static boolean isEmpty(final String str) {
        return str == null || str.length() == 0;
    }

    // Hides secret arguments, so that they do not go into logs or error messages
    public static String[] redact(String[] command) {
        String[] redacted = command.clone();
        for (int i = 0; i < redacted.length - 1; i++) {
            if ("--token".equals(redacted[i])) {
                redacted[i + 1] = "***";
            }
        }
        return redacted;
    }

    public static String join(String[] words) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            builder.append(words[i]);
            if (i < words.length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }
}
