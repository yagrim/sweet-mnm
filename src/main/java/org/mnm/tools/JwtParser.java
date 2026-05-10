package org.mnm.tools;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * WARNING: This does not validate the signature since MnM public keys are not available.
 */
public class JwtParser {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JwtParser() {
    }

    public static JwtClaims parse(String jwt) {

        final String[] parts = jwt.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT");
        }

        return new JwtClaims(parseJson(parts[1]));
    }

    private static JsonNode parseJson(String base64Url) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(base64Url);
            return MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT content", e);
        }
    }

    // Ignored claims:
    // - jti: UUID jwt unique identifier
    // - jti: UUID jwt unique identifier
    // - nbf (not before time): is emission time
    // - purpose: (integer 0) ???
    // - type: (string "access") ???
    public record JwtClaims(JsonNode payload) {

        public String audience() {
            return getString("aud");
        }

        /**
         * MnM account email.
         */
        public String email() {
            return getString("email");
        }

        /**
         * MnM internal user id?
         */
        public String subject() {
            return getString("sub");
        }

        public String issuer() {
            return getString("iss");
        }

        public long issuedAt() {
            return getLong("iat");
        }

        public long expiration() {
            return getLong("exp");
        }

        /**
         * MnM API version.
         */
        public long version() {
            return getInt("version");
        }

        public boolean isExpired() {
            long exp = expiration();
            return exp > 0 && (System.currentTimeMillis() / 1000) >= exp;
        }

        private String getString(String key) {
            return payload.path(key).asString(null);
        }

        private long getLong(String key) {
            return payload.path(key).asLong(0);
        }

        private int getInt(String key) {
            return payload.path(key).asInt(0);
        }

    }
}
