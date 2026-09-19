package org.mnm.api;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.ApiServerStubs.stubAccountLogin;
import static org.mnm.ApiServerStubs.stubGameVersions;

@WireMockTest(httpsEnabled = true)
class SessionTest {

    @Test
    void shouldLoginAndRetrieveSession(WireMockRuntimeInfo wiremock) {
        stubAccountLogin();
        stubGameVersions();

        String httpBaseUrl = wiremock.getHttpBaseUrl();
        ApiConnector apiConnector = new ApiConnector(new RestClient(httpBaseUrl));
        TestTokenSupplier tokenSupplier = new TestTokenSupplier();

        Session session = Session.login("username", "password", apiConnector, tokenSupplier);

        assertThat(session).isNotNull();
        assertThat(session.getSlug()).isEqualTo("mnm");
        assertThat(session.getChunksUrl()).isEqualTo("https://clients.domain.com/chunks");
    }

    @Test
    void shouldFailWhenCredentialsAreNotValid(WireMockRuntimeInfo wiremock) {
        stubFor(post(urlEqualTo("/account/login"))
            .willReturn(ResponseDefinitionBuilder.responseDefinition()
                .withStatus(200)
                .withBody("""
                    {"status":4, "error": "Incorrect Email/Password"}
                    """)));

        String httpBaseUrl = wiremock.getHttpBaseUrl();
        ApiConnector apiConnector = new ApiConnector(new RestClient(httpBaseUrl));
        TestTokenSupplier tokenSupplier = new TestTokenSupplier();

        Throwable t = catchThrowable(() -> Session.login("username", "password", apiConnector, tokenSupplier));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("API Error: 200 {error=Incorrect Email/Password, status=4}");
    }

    class TestTokenSupplier implements TokenSupplier {

        @Override
        public String getToken(String method, List<String> methods, String challengeToken) {
            return "";
        }
    }
}
