package org.mnm;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@WireMockTest(httpsEnabled = true)
public class SessionTest {

    @Test
    void shouldLoginAndRetrieveSession(WireMockRuntimeInfo wiremock) {
        stubFor(post(urlEqualTo("/account/login"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withBody("""
                                {"status": 0, "token": "123.456.789"}
                                """)));

        stubFor(get(urlPathEqualTo("/game/versions"))
                .withQueryParam("token", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("""
                                { "versions": [{
                                    "version": "publish-0.24.1.0-65ab2c20cf879a5a25ea4212df4f6774ef96774e",
                                    "slug": "mnm",
                                    "chunks_url": "https://clients.domain.com/chunks",
                                    "manifest_url": "http://clients.domain.com/manifests/65ab2c20cf879a5a25ea4212df4f6774ef96774e.manifest"
                                }]}
                                """)));

        Session session = Session.login("username", "password", wiremock.getHttpBaseUrl());

        assertThat(session).isNotNull();
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
