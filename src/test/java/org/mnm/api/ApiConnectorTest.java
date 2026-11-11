package org.mnm.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import org.mnm.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.config.Environment.API_BASE_URL;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

class ApiConnectorTest {

    private final boolean mock = true;
//    private final boolean mock = false;

    private ApiConnector apiConnector;
    private RestClient restConnector;

    @BeforeEach
    void setup() {
        restConnector = mock ? Mockito.mock(RestClient.class) : new RestClient(API_BASE_URL);
        apiConnector = new ApiConnector(restConnector);
    }

    @Nested
    class SimpleLogin {

        @Test
        void should_login_successfully() {
            final String username = "username";
            final String password = "password";

            if (mock) {
                Mockito.when(restConnector.post(anyString(), anyMap()))
                    .thenReturn(new ApiResponse(200, Map.of("status", 0L, "token", "123.456.789")));
            }

            ApiConnection connection = apiConnector.login(username, password, null);

            assertThat(connection.isActive()).isTrue();
        }

    }

    @Nested
    class TwoFactorAuthentication {

        @Test
        void should_login_successfully() {
            final String username = "username";
            final String password = "password";

            if (mock) {
                Mockito.when(restConnector.post(Mockito.eq("account/login"), anyMap()))
                    .thenReturn(new ApiResponse(200, Map.of(
                        "error", "Two-factor authentication required. Update your launcher if no verification prompt appears.",
                        "code", "two_factor_required",
                        "status", 6L,
                        "methods", List.of("email"),
                        "method", "email",
                        "challenge_token", "my-challenge-token",
                        "expires_in", 300
                    )));
                Mockito.when(restConnector.post(Mockito.eq("account/login/2fa"), anyMap()))
                    .thenReturn(new ApiResponse(200, Map.of("status", 0L, "token", "123.456.789")));
            }

            ApiConnection connection = apiConnector.login(username, password, new TokenSupplier() {
                @Override
                public String getToken(String method, List<String> methods, String challengeToken) {
                    assertThat(method).isEqualTo("email");
                    assertThat(methods).containsExactly("email");
                    assertThat(challengeToken).isEqualTo("my-challenge-token");
                    return TestUtils.validToken();
                }
            });

            assertThat(connection.isActive()).isTrue();
        }

        @Test
        void should_login_successfully_with_totp() {
            final String username = "username";
            final String password = "password";
            final String testToken = TestUtils.validToken();

            Mockito.when(restConnector.post(Mockito.eq("account/login"), anyMap()))
                .thenReturn(new ApiResponse(200, Map.of(
                    "error", "Two-factor authentication required. Update your launcher if no verification prompt appears.",
                    "code", "two_factor_required",
                    "status", 6L,
                    "methods", List.of("email", "totp", "backup"),
                    "method", "totp",
                    "challenge_token", "my-challenge-token",
                    "expires_in", 300L
                )));
            Mockito.when(restConnector.post(Mockito.eq("account/login/2fa"), anyMap()))
                .thenReturn(new ApiResponse(200, Map.of("status", 0L, "token", testToken)));

            ApiConnection connection = apiConnector.login(username, password, (method, methods, challengeToken) -> {
                assertThat(method).isEqualTo("totp");
                assertThat(methods).containsExactly("email", "totp", "backup");
                assertThat(challengeToken).isEqualTo("my-challenge-token");
                return apiConnector.twoFactorAuthentication(method, "123456", challengeToken);
            });

            assertThat(connection.isActive()).isTrue();
            Mockito.verify(restConnector).post("account/login/2fa", Map.of(
                "method", "totp",
                "code", "123456",
                "challenge_token", "my-challenge-token"
            ));
        }

        @ParameterizedTest
        @ValueSource(strings = {"totp", "backup", "unknown"})
        void should_not_resend_code_for_not_resendable_method(String method) {
            Throwable t = catchThrowable(() -> apiConnector.resendVerificationCode("my-challenge-token", method));

            assertThat(t)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot resend verification code for method: " + method);
            Mockito.verifyNoInteractions(restConnector);
        }

        @Test
        void should_resend_code_for_email() {
            Mockito.when(restConnector.post(Mockito.eq("account/login/2fa/email"), anyMap()))
                .thenReturn(new ApiResponse(200, Map.of("status", 6L)));

            apiConnector.resendVerificationCode("my-challenge-token", "email");

            Mockito.verify(restConnector).post("account/login/2fa/email", Map.of("challenge_token", "my-challenge-token"));
        }

        @Test
        void should_handle_2fa_error() {
            final String username = "username";
            final String password = "password";

            if (mock) {
                Mockito.when(restConnector.post(Mockito.eq("account/login"), anyMap()))
                    .thenReturn(new ApiResponse(200, Map.of(
                        "code", "something_else",
                        "status", 6L
                    )));
            }

            Throwable t = catchThrowable(() -> apiConnector.login(username, password, null));

            assertThat(t)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("API Error: 200 {code=something_else, status=6}");
        }

        @Test
        void should_get_2fa_token() {
            final String method = "email";
            final String code = "123456";
            final String challengeToken = "12345678";
            final String testToken = TestUtils.validToken();

            if (mock) {
                Mockito.when(restConnector.post(Mockito.eq("account/login/2fa"), anyMap()))
                    .thenReturn(new ApiResponse(200, Map.of(
                        "status", 0L,
                        "token", testToken
                    )));
            }

            String token = apiConnector.twoFactorAuthentication(method, code, challengeToken);

            assertThat(token).isEqualTo(testToken);
        }

        @Test
        void should_handle_2fa_http_status_error() {
            final String method = "email";
            final String code = "123456";
            final String challengeToken = "12345678";

            if (mock) {
                Mockito.when(restConnector.post(Mockito.eq("account/login/2fa"), anyMap()))
                    .thenReturn(new ApiResponse(429, Map.of(
                        "error", "Too many sign-in attempts",
                        "message", "Rate limit exceeded."
                    )));
            }

            Throwable t = catchThrowable(() -> apiConnector.twoFactorAuthentication(method, code, challengeToken));

            assertThat(t)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("API Error: 429 {error=Too many sign-in attempts, message=Rate limit exceeded.}");
        }

        @Test
        void should_handle_2fa_api_status_error() {
            final String method = "email";
            final String code = "123456";
            final String challengeToken = "12345678";

            if (mock) {
                Mockito.when(restConnector.post(Mockito.eq("account/login/2fa"), anyMap()))
                    .thenReturn(new ApiResponse(200, Map.of(
                        "error", "Invalid verification code. Please try again.",
                        "code", "two_factor_invalid",
                        "status", 6L
                    )));
            }

            Throwable t = catchThrowable(() -> apiConnector.twoFactorAuthentication(method, code, challengeToken));

            assertThat(t)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("API Error: 200 {code=two_factor_invalid, error=Invalid verification code. Please try again., status=6}");
        }

    }

    @Test
    void should_handle_error_invalid_credentials() {
        final String username = "username";
        final String password = "password";

        if (mock) {
            Mockito.when(restConnector.post(Mockito.eq("account/login"), anyMap()))
                .thenReturn(new ApiResponse(200, Map.of(
                    "status", 4L,
                    "error", "Incorrect Email/Password")));
        }

        Throwable t = catchThrowable(() -> apiConnector.login(username, password, null));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("API Error: 200 {error=Incorrect Email/Password, status=4}");
    }

    @Test
    void should_handle_error_too_many_requests() {
        final String username = "username";
        final String password = "password";

        if (mock) {
            Mockito.when(restConnector.post(Mockito.eq("account/login"), anyMap()))
                .thenReturn(new ApiResponse(429, Map.of(
                    "error", "Too many sign-in attempts for this account. Wait a minute and try again.",
                    "message", "Rate limit exceeded. Maximum 5 requests per 60 seconds.")));
        }

        Throwable t = catchThrowable(() -> apiConnector.login(username, password, null));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("API Error: 429 {error=Too many sign-in attempts for this account. Wait a minute and try again., message=Rate limit exceeded. Maximum 5 requests per 60 seconds.}");
    }

}
