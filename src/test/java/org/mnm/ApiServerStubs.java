package org.mnm;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ApiServerStubs {

    public static void stubAccountLogin() {
        stubAccountLogin("123.45678.90");
    }

    public static void stubAccountLogin(String token) {
        stubFor(post(urlEqualTo("/account/login"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withBody("""
                                {"status": 0, "token": "%s"}
                                """.formatted(token))));
    }

    public static void stubGameVersions() {
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
    }

}
