package org.mnm.config;

public record StoredSession(
    Integer id,
    String slug,
    String token
) {

    public StoredSession(String slug, String token) {
        this(null, slug, token);
    }
}
