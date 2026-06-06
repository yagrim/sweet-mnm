package org.mnm.tools;

import java.time.Instant;
import java.util.Base64;

import com.google.gson.JsonObject;

public class JwtParser {

    private JwtParser() {
    }

    public static JwtClaims parse(String jwt) {

        final String[] parts = jwt.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT");
        }

        return new JwtClaims(parseJson(parts[1]));
    }

    private static JsonObject parseJson(String base64Url) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(base64Url);
            return JsonParser.read(decoded, JsonObject.class);
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
    public record JwtClaims(JsonObject payload) {

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

        public Instant issuedAtTime() {
            return toInstant(issuedAt());
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

        public Instant expirationTime() {
            return toInstant(expiration());
        }

        private static Instant toInstant(long value) {
            return value > 0 ? Instant.ofEpochSecond(value) : null;
        }

        public boolean isExpired() {
            long exp = expiration();
            return exp > 0 && (System.currentTimeMillis() / 1000) >= exp;
        }

        private String getString(String key) {
            return payload.has(key) && !payload.get(key).isJsonNull()
                ? payload.get(key).getAsString() : null;
        }

        private long getLong(String key) {
            return payload.has(key) && !payload.get(key).isJsonNull()
                ? payload.get(key).getAsLong() : 0;
        }

        private int getInt(String key) {
            return payload.has(key) && !payload.get(key).isJsonNull()
                ? payload.get(key).getAsInt() : 0;
        }

    }
}
