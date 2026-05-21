package org.mnm.tools;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mnm.tools.JwtParser.JwtClaims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.TestUtils.testToken;

class JwtParserTest {

    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJtbm0iLCJlbWFpbCI6ImEtdXNlcm5hbWVAc29tZS1lbWFpbC5jb20iLCJleHAiOjE3ODA3OTAyNTcsImlhdCI6MTc3ODM3MTA1NywiaXNzIjoibW5tIiwianRpIjoiYzg2MThkMmEtYTExNy00YmQ4LWJiZmUtZDQwMjJkNWI4MThjIiwibmJmIjoxNzc4MzcxMDU2LCJwdXJwb3NlIjowLCJzdWIiOiI0MjQyNDIiLCJ0eXAiOiJhY2Nlc3MiLCJ2ZXJzaW9uIjoyMX0.8_TEQWuqz4abx3YoXawWRGlnPBVFgm9MigBA4nHt9eA";

    @Test
    void shouldParseClaims() {

        JwtClaims claims = JwtParser.parse(TEST_TOKEN);

        assertThat(claims.audience()).isEqualTo("mnm");
        assertThat(claims.subject()).isEqualTo("424242");
        assertThat(claims.issuer()).isEqualTo("mnm");

        assertThat(claims.expiration()).isEqualTo(1780790257L);
        assertThat(claims.issuedAt()).isEqualTo(1778371057L);

        assertThat(claims.email()).isEqualTo("a-username@some-email.com");
        assertThat(claims.version()).isEqualTo(21);
    }

    @Test
    void shouldDetectExpiredToken() {
        final String expiredToken = testToken(Instant.ofEpochSecond(1000));

        JwtClaims claims = JwtParser.parse(expiredToken);

        assertThat(claims.expiration()).isEqualTo(1000L);
        assertThat(claims.isExpired()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "  ",
        "invalid-token",
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9eyJzdWIiOiJtZSIsImlhdCI6MTUxNjIzOTAyMn0",
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtZSIsImlhdCI6MTUxNjIzOTAyMn0",
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtZSIsImlhdCI6MTUxNjIzOTAyMn0.",
        ".eyJzdWIiOiJtZSIsImlhdCI6MTUxNjIzOTAyMn0.",
        "eyJzdWIiOiJtZSIsImlhdCI6MTUxNjIzOTAyMn0"
    })
    void shouldFailWhenTokenIsNotValid(String token) {
        Throwable t = catchThrowable(() -> JwtParser.parse("invalid-token"));

        assertThat(t)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid JWT");
    }

    @Test
    void shouldReturnNullForMissingStringClaims() {
        String jwt = "eyJhbGciOiJub25lIn0.e30.123";

        JwtClaims claims = JwtParser.parse(jwt);

        assertThat(claims.subject()).isNull();
        assertThat(claims.issuer()).isNull();
    }

    @Test
    void shouldReturnZeroForMissingLongClaims() {
        String jwt = "eyJhbGciOiJub25lIn0.e30.123";

        JwtClaims claims = JwtParser.parse(jwt);

        assertThat(claims.expiration()).isZero();
        assertThat(claims.issuedAt()).isZero();
    }
}
