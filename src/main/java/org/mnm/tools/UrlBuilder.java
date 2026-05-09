package org.mnm.tools;

import java.net.URI;

public class UrlBuilder {

    public static URI buildUrl(String base, String path) {
        return URI.create(base)
                .resolve(cleanPath(path));
    }

    private static String cleanPath(String path) {
        int start = 0;
        int end = path.length();
        while (start < end && path.charAt(start) == '/') {
            start++;
        }
        while (end > start && path.charAt(end - 1) == '/') {
            end--;
        }
        return path.substring(start, end);
    }

}
