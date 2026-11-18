package org.mnm.tools;

import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class DownloaderTest {

    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(300);
    // The first request to WireMock can be slow
    private static final Duration NORMAL_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void shouldDownloadFile(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) throws Exception {
        stubFor(get(urlEqualTo("/file.bin")).willReturn(aResponse().withBody("content")));
        Path destination = tempDir.resolve("downloads").resolve("file.bin");

        Downloader.downloadFile(wiremock.getHttpBaseUrl() + "/file.bin", destination, NORMAL_TIMEOUT);

        assertThat(Files.readString(destination)).isEqualTo("content");
    }

    @Test
    void shouldFailWhenServerDoesNotAnswerInTime(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        stubFor(get(urlEqualTo("/slow.bin"))
            .willReturn(aResponse().withFixedDelay(3000).withBody("content")));
        Path destination = tempDir.resolve("slow.bin");

        assertThatThrownBy(() -> Downloader.downloadFile(wiremock.getHttpBaseUrl() + "/slow.bin", destination, SHORT_TIMEOUT))
            .hasCauseInstanceOf(SocketTimeoutException.class);
        assertThat(destination).doesNotExist();
    }

}
