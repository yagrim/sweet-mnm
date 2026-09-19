package org.mnm.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.api.ApiHelper.parseLoginResponse;
import static org.mnm.api.ApiHelper.parseResponse;

class HttpHelperTest {

    @Test
    void shouldParseSuccess() {
        ApiResponse apiResponse = new ApiResponse(200, Map.of(
            "status", 0L,
            "token", "1234567890"
        ));

        ApiResponse actual = parseResponse(apiResponse);

        assertThat(actual).isEqualTo(apiResponse);
    }

    // We use parseLoginResponse to avoid retorting an exception even when API status=6
    @Test
    void shouldParseTwoFactorRequiredResponse() {
        ApiResponse apiResponse = new ApiResponse(200, Map.of(
            "error", "Two-factor authentication required. Update your launcher if no verification prompt appears.",
            "code", "two_factor_required",
            "status", 6L,
            "methods", List.of("email"),
            "method", "email",
            "challenge_token", "1234567890",
            "expires_in", 300L
        ));

        ApiResponse actual = parseLoginResponse(apiResponse);

        assertThat(actual).isEqualTo(apiResponse);
    }

    @Test
    void shouldSerializeHttpErrorWithEmptyBody() {
        ApiResponse apiResponse = new ApiResponse(400, Map.of());

        Throwable t = catchThrowable(() -> parseResponse(apiResponse));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("API Error: 400 {}");
    }

    @Test
    void shouldSerializeHttpErrorWithBody() {
        ApiResponse apiResponse = new ApiResponse(429, Map.of(
            "error", "Too many attempts. Wait a minute and try again.",
            "code", "two_factor_rate_limited",
            "status", 6L
        ));

        Throwable t = catchThrowable(() -> parseResponse(apiResponse));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("API Error: 429 {code=two_factor_rate_limited, error=Too many attempts. Wait a minute and try again., status=6}");
    }

    @Test
    void shouldSerializeApiErrorWithBody() {
        ApiResponse apiResponse = new ApiResponse(200, Map.of(
            "error", "Invalid verification code. Please try again.",
            "code", "two_factor_invalid",
            "status", 6L
        ));

        Throwable t = catchThrowable(() -> parseResponse(apiResponse));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("API Error: 200 {code=two_factor_invalid, error=Invalid verification code. Please try again., status=6}");
    }

}
