package org.mnm.tools;

public class StringUtils {

    public static boolean isEmpty(final String str) {
        return str == null || str.length() == 0;
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
