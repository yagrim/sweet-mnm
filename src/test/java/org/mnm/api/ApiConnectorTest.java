package org.mnm.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

            ApiConnection connection = apiConnector.login(username, password, new VerificationCodeSupplier() {
                @Override
                public String getVerificationCode(String method, List<String> methods, String challengeToken) {
                    assertThat(method).isEqualTo("email");
                    assertThat(methods).containsExactly("email");
                    assertThat(challengeToken).isEqualTo("my-challenge-token");
                    return TestUtils.validToken();
                }
            });

            assertThat(connection.isActive()).isTrue();
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
