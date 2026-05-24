package org.mnm.config;

public record Token(
    Integer id,
    String slug,
    String token
) {

    public Token(String slug, String token) {
        this(null, slug, token);
    }
}
