package org.mnm.config;

public record Session(
    Integer id,
    String slug,
    String token
) {

    public Session(String slug, String token) {
        this(null, slug, token);
    }
}
