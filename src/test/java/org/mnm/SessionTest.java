package org.mnm;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

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

        Session session = Session.login("username", "password", wiremock.getHttpBaseUrl());

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

        Throwable t = catchThrowable(() -> Session.login("username", "password", wiremock.getHttpBaseUrl()));

        assertThat(t)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Response error: 200, {status=4, error=Incorrect Email/Password}");
    }
}
