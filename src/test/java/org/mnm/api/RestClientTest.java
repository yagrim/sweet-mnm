package org.mnm.api;

import java.net.ServerSocket;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class RestClientTest {

    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(300);
    // The first request to WireMock can be slow
    private static final Duration NORMAL_TIMEOUT = Duration.ofSeconds(5);
    private static final int SLOW_RESPONSE_MILLIS = 3000;

    @Test
    void shouldReturnResponseWhenServerAnswersInTime(WireMockRuntimeInfo wiremock) {
        stubFor(get(urlEqualTo("/status")).willReturn(okJson("{\"status\": 1}")));

        ApiResponse response = new RestClient(wiremock.getHttpBaseUrl(), NORMAL_TIMEOUT).get("/status");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.getStatus()).isEqualTo(1L);
    }

    @Test
    void shouldFailGetWhenServerDoesNotAnswerInTime(WireMockRuntimeInfo wiremock) {
        stubFor(get(urlEqualTo("/slow"))
            .willReturn(aResponse().withFixedDelay(SLOW_RESPONSE_MILLIS).withBody("{}")));
        RestClient restClient = new RestClient(wiremock.getHttpBaseUrl(), SHORT_TIMEOUT);

        assertThatThrownBy(() -> restClient.get("/slow"))
            .hasCauseInstanceOf(HttpTimeoutException.class);
    }

    @Test
    void shouldFailPostWhenServerDoesNotAnswerInTime(WireMockRuntimeInfo wiremock) {
        stubFor(post(urlEqualTo("/slow"))
            .willReturn(aResponse().withFixedDelay(SLOW_RESPONSE_MILLIS).withBody("{}")));
        RestClient restClient = new RestClient(wiremock.getHttpBaseUrl(), SHORT_TIMEOUT);

        assertThatThrownBy(() -> restClient.post("/slow", Map.of()))
            .hasCauseInstanceOf(HttpTimeoutException.class);
    }

    @Test
    void shouldNotInterruptThreadWhenConnectionFails() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        RestClient restClient = new RestClient("http://localhost:" + closedPort, SHORT_TIMEOUT);

        try {
            assertThatThrownBy(() -> restClient.get("/status")).isInstanceOf(RuntimeException.class);

            assertThat(Thread.currentThread().isInterrupted()).isFalse();
        } finally {
            // Clear the flag, so that a failure does not affect other tests
            Thread.interrupted();
        }
    }

}
