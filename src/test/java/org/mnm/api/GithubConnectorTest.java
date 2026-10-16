package org.mnm.api;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import org.mnm.api.GithubConnector.Asset;
import org.mnm.api.GithubConnector.Release;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.api.GithubConnector.LATEST_RELEASE_PATH;

@WireMockTest
class GithubConnectorTest {

    @Test
    void shouldReturnLatestRelease(WireMockRuntimeInfo wiremock) {
        stubFor(get(urlEqualTo("/" + LATEST_RELEASE_PATH))
            .willReturn(okJson("""
                {
                  "tag_name": "v1.5.0",
                  "name": "v1.5.0",
                  "draft": false,
                  "prerelease": false,
                  "published_at": "2026-10-01T12:00:00Z",
                  "assets": [
                    {
                      "name": "sweet-mnm-1.5.0.zip",
                      "size": 1024,
                      "browser_download_url": "https://github.com/yagrim/sweet-mnm/releases/download/v1.5.0/sweet-mnm-1.5.0.zip"
                    },
                    {
                      "name": "sweet-mnm-1.5.0.exe",
                      "size": 2048,
                      "browser_download_url": "https://github.com/yagrim/sweet-mnm/releases/download/v1.5.0/sweet-mnm-1.5.0.exe"
                    }
                  ]
                }
                """)));

        Release release = connector(wiremock).getLatestRelease();

        assertThat(release.tagName()).isEqualTo("v1.5.0");
        assertThat(release.publishedAt()).isEqualTo("2026-10-01T12:00:00Z");
        assertThat(release.assets()).containsExactly(
            new Asset("sweet-mnm-1.5.0.zip", "https://github.com/yagrim/sweet-mnm/releases/download/v1.5.0/sweet-mnm-1.5.0.zip"),
            new Asset("sweet-mnm-1.5.0.exe", "https://github.com/yagrim/sweet-mnm/releases/download/v1.5.0/sweet-mnm-1.5.0.exe"));
        verify(getRequestedFor(urlEqualTo("/" + LATEST_RELEASE_PATH))
            .withHeader("User-Agent", equalTo("sweet-mnm"))
            .withHeader("Accept", equalTo("application/vnd.github+json")));
    }

    @Test
    void shouldReturnLatestReleaseWithoutAssets(WireMockRuntimeInfo wiremock) {
        stubFor(get(urlEqualTo("/" + LATEST_RELEASE_PATH))
            .willReturn(okJson("""
                {"tag_name": "v1.5.0", "published_at": "2026-10-01T12:00:00Z", "assets": []}
                """)));

        Release release = connector(wiremock).getLatestRelease();

        assertThat(release.tagName()).isEqualTo("v1.5.0");
        assertThat(release.assets()).isEmpty();
    }

    @Test
    void shouldFailWhenRateLimitIsExceeded(WireMockRuntimeInfo wiremock) {
        stubFor(get(urlEqualTo("/" + LATEST_RELEASE_PATH))
            .willReturn(aResponse()
                .withStatus(403)
                .withBody("""
                    {"message": "API rate limit exceeded"}
                    """)));

        Throwable t = catchThrowable(() -> connector(wiremock).getLatestRelease());

        assertThat(t)
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("403");
    }

    private static GithubConnector connector(WireMockRuntimeInfo wiremock) {
        return new GithubConnector(new RestClient(wiremock.getHttpBaseUrl()));
    }
}
