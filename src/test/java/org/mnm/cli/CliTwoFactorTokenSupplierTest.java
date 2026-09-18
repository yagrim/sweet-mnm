package org.mnm.cli;

import java.util.function.Consumer;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.mnm.ApiServerStubs;
import org.mnm.TestUtils;
import org.mnm.api.ApiConnector;
import org.mnm.api.ApiException;
import org.mnm.api.RestClient;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mnm.ApiServerStubs.stubInvalidCode;
import static org.mnm.ApiServerStubs.stubTooManyRequests;

@WireMockTest(extensionScanningEnabled = true)
class CliTwoFactorTokenSupplierTest {

    private static final String METHOD = "email";


    @Test
    void shouldValidateCodeAndReturnToken(WireMockRuntimeInfo wiremock) {
        final String challengeToken = "1234567890";
        final String code = "122333";
        final String expectedToken = TestUtils.validToken();
        ApiServerStubs.stubTwoFactorAuthentication(METHOD, challengeToken, code, expectedToken);
        LineReader mockLineReader = Mockito.mock(LineReader.class);
        Mockito.when(mockLineReader.readLine()).thenReturn(code);

        final ApiConnector apiConnector = new ApiConnector(new RestClient(wiremock.getHttpBaseUrl()));
        final CliTwoFactorTokenSupplier tokenSupplier = new CliTwoFactorTokenSupplier(apiConnector, mockLineReader);

        String actualToken = tokenSupplier.getToken(METHOD, emptyList(), challengeToken);

        assertThat(actualToken).isEqualTo(expectedToken);
    }

    @Test
    void shouldFailWithUnsupportedMethod() {
        final CliTwoFactorTokenSupplier tokenSupplier = new CliTwoFactorTokenSupplier(null, null);

        assertThatThrownBy(() -> tokenSupplier.getToken("other", emptyList(), ""))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Unsupported authentication method: other");
    }

    @Test
    void shouldFailWithTooManyRequests(WireMockRuntimeInfo wiremock) {
        shouldFailAndAssertApiException(wiremock,
            (challengeToken, code, _) -> {
                stubTooManyRequests(METHOD, challengeToken, code);
            },
            e -> {

                assertThat(e)
                    .isInstanceOf(ApiException.class)
                    .hasMessage("API Error: 429 {code=two_factor_rate_limited, error=Too many attempts. Wait a minute and try again., status=6}");
                assertThat(((ApiException) e).getError()).isEqualTo("Too many attempts. Wait a minute and try again.");
            });
    }

    @Test
    void shouldFailWithInvalidCode(WireMockRuntimeInfo wiremock) {
        shouldFailAndAssertApiException(wiremock,
            (challengeToken, code, _) -> stubInvalidCode(METHOD, challengeToken, code),
            e -> {
                assertThat(e)
                    .isInstanceOf(ApiException.class)
                    .hasMessage("API Error: 200 {code=two_factor_rate_limited, error=Invalid verification code. Please try again., status=6}");
                assertThat(((ApiException) e).getError()).isEqualTo("Invalid verification code. Please try again.");
            });
    }

    private static void shouldFailAndAssertApiException(WireMockRuntimeInfo wiremock, Stubber stubber, ExceptionAsserter asserter) {
        final String challengeToken = "1234567890";
        final String code = "122333";
        stubber.run(challengeToken, code, null);
        LineReader mockLineReader = Mockito.mock(LineReader.class);
        Mockito.when(mockLineReader.readLine()).thenReturn("122333");

        final ApiConnector apiConnector = new ApiConnector(new RestClient(wiremock.getHttpBaseUrl()));
        final CliTwoFactorTokenSupplier tokenSupplier = new CliTwoFactorTokenSupplier(apiConnector, mockLineReader);

        Exception e = catchException(() -> tokenSupplier.getToken(METHOD, emptyList(), challengeToken));

        asserter.accept(e);
    }

    interface Stubber {
        void run(String challengeToken, String code, String expectedToken);
    }

    interface ExceptionAsserter extends Consumer<Exception> {
    }
}
