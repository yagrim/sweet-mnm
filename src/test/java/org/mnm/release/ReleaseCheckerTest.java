package org.mnm.release;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mnm.LoggerHandler;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.api.GithubConnector;
import org.mnm.api.RestClient;
import org.mnm.gui.InMemorySettingsStore;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.release.ReleaseChecker.CHECK_INTERVAL;
import static org.mnm.release.ReleaseChecker.LAST_CHECK_KEY;
import static org.mnm.release.ReleaseChecker.LATEST_VERSION_KEY;

@WireMockTest
@ExtendWith(SystemOutCaptureExtension.class)
class ReleaseCheckerTest {

    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    @Test
    void shouldFindNewRelease(WireMockRuntimeInfo wiremock) {
        stubLatestRelease("v1.5.0");

        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        assertThat(checker(wiremock, "1.4.0").findNewRelease(settings)).contains("1.5.0");

        assertThat(settings.get(LAST_CHECK_KEY)).isEqualTo(NOW.toString());
        assertThat(settings.get(LATEST_VERSION_KEY)).isEqualTo("v1.5.0");
    }

    @Test
    void shouldNotFindNewReleaseWhenVersionIsTheLatest(WireMockRuntimeInfo wiremock) {
        stubLatestRelease("v1.5.0");

        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        assertThat(checker(wiremock, "1.5.0").findNewRelease(settings)).isEmpty();
    }

    @Test
    void shouldNotFindNewReleaseWhenVersionIsNewer(WireMockRuntimeInfo wiremock) {
        stubLatestRelease("v1.5.0");

        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        assertThat(checker(wiremock, "1.10.0").findNewRelease(settings)).isEmpty();
    }

    @Test
    void shouldSkipCheckWhenVersionIsNotARelease(WireMockRuntimeInfo wiremock) {
        stubLatestRelease("v1.5.0");

        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        assertThat(checker(wiremock, "0.0.1-SNAPSHOT").findNewRelease(settings)).isEmpty();

        verify(0, anyRequestedFor(anyUrl()));
        assertThat(settings.get(LAST_CHECK_KEY)).isNull();
    }

    @Test
    void shouldUseLastCheckWhenItIsNewerThanInterval(WireMockRuntimeInfo wiremock) {
        stubLatestRelease("v2.0.0");

        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        settings.put(LAST_CHECK_KEY, NOW.minus(CHECK_INTERVAL).plusSeconds(1).toString());
        settings.put(LATEST_VERSION_KEY, "v1.5.0");

        assertThat(checker(wiremock, "1.4.0").findNewRelease(settings)).contains("1.5.0");

        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void shouldCheckAgainWhenLastCheckIsOlderThanInterval(WireMockRuntimeInfo wiremock) {
        stubLatestRelease("v2.0.0");

        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        settings.put(LAST_CHECK_KEY, NOW.minus(CHECK_INTERVAL).toString());
        settings.put(LATEST_VERSION_KEY, "v1.5.0");

        assertThat(checker(wiremock, "1.4.0").findNewRelease(settings)).contains("2.0.0");

        verify(1, anyRequestedFor(anyUrl()));
        assertThat(settings.get(LAST_CHECK_KEY)).isEqualTo(NOW.toString());
        assertThat(settings.get(LATEST_VERSION_KEY)).isEqualTo("v2.0.0");
    }

    @Test
    void shouldCheckAgainAfter12Hours(WireMockRuntimeInfo wiremock) {
        stubLatestRelease("v2.0.0");

        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        settings.put(LAST_CHECK_KEY, NOW.minus(Duration.ofHours(12)).toString());
        settings.put(LATEST_VERSION_KEY, "v1.5.0");

        assertThat(checker(wiremock, "1.4.0").findNewRelease(settings)).contains("2.0.0");

        verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void shouldNotFailWhenCheckFails(WireMockRuntimeInfo wiremock, SystemOutCaptureExtension out) {
        LoggerHandler.setInfo(true);
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500).withBody("{}")));

        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        assertThat(checker(wiremock, "1.4.0").findNewRelease(settings)).isEmpty();

        assertThat(out.getOutput()).contains("INFO  ReleaseChecker - Could not check for new releases: API Error: 500 {}");
        // Next start tries again
        assertThat(settings.get(LAST_CHECK_KEY)).isNull();
    }

    @Test
    void shouldNotFindNewReleaseWhenTagIsNotAVersion(WireMockRuntimeInfo wiremock) {
        stubLatestRelease("nightly");

        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        assertThat(checker(wiremock, "1.4.0").findNewRelease(settings)).isEmpty();
    }

    private static ReleaseChecker checker(WireMockRuntimeInfo wiremock, String currentVersion) {
        return new ReleaseChecker(
            new GithubConnector(new RestClient(wiremock.getHttpBaseUrl())),
            currentVersion,
            Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static void stubLatestRelease(String tag) {
        stubFor(get(anyUrl()).willReturn(okJson("""
            {"tag_name": "%s", "published_at": "2026-10-01T12:00:00Z", "assets": []}
            """.formatted(tag))));
    }
}
