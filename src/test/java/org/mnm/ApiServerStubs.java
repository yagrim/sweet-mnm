package org.mnm;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mnm.tools.FileUtils.readFromClasspathAsArray;

public class ApiServerStubs {

    public static final String TEST_SLUG = "mnm";
    public static final String TEST_VERSION = "publish-0.24.1.0-65ab2c20cf879a5a25ea4212df4f6774ef96774e";

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

    public static void stubGameVersions(String baseUrl) {
        final String version = "publish-0.24.1.0-65ab2c20cf879a5a25ea4212df4f6774ef96774e";
        stubFor(get(urlPathEqualTo("/game/versions"))
            .withQueryParam("token", matching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""
                    { "versions": [{
                        "version": "VERSION",
                        "slug": "mnm",
                        "chunks_url": "BASE_URL/chunks",
                        "manifest_url": "BASE_URL/manifests/65ab2c20cf879a5a25ea4212df4f6774ef96774e.manifest"
                    }]}
                    """
                    .replace("VERSION", version)
                    .replaceAll("BASE_URL", baseUrl))));
    }

    public static void stubManifestDownload() {
        final byte[] manifest = readFromClasspathAsArray("installation/test-manifest.json");
        stubFor(get(urlPathEqualTo("/manifests/65ab2c20cf879a5a25ea4212df4f6774ef96774e.manifest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(manifest)));
    }

    public static void stubEmptyManifestDownload() {
        stubFor(get(urlPathEqualTo("/manifests/65ab2c20cf879a5a25ea4212df4f6774ef96774e.manifest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""
                    { "manifest": [] }""")));
    }

    public static void stubChunkDownload(String bundleCrc) {
        byte[] chunk;
        try {
            chunk = readFromClasspathAsArray("installation/%s.zst".formatted(bundleCrc));
        } catch (NullPointerException e) {
            throw new RuntimeException("Classpath resource not found: " + bundleCrc);
        }
        stubFor(get(urlEqualTo("/chunks/%s.bin".formatted(bundleCrc)))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(chunk)));
    }

}
