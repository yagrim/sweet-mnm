package org.mnm.api;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;

import org.mnm.tools.JsonParser;

import static org.mnm.tools.UrlBuilder.buildUrl;

public class RestClient {

    // A server that accepts the connection but does not respond must not block Sweet forever
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final String baseUrl;
    private final Duration timeout;

    public RestClient(String baseUrl) {
        this(baseUrl, DEFAULT_TIMEOUT);
    }

    RestClient(String baseUrl, Duration timeout) {
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }

    public ApiResponse post(String url, Map<String, Object> body) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(buildUrl(baseUrl, url))
            .timeout(timeout)
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(JsonParser.toJson(body)))
            .build();

        return send(request);
    }

    public ApiResponse get(String url) {
        return get(url, Map.of());
    }

    public ApiResponse get(String url, Map<String, Object> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(buildUrl(baseUrl, url))
            .timeout(timeout)
            .GET();
        for (Map.Entry<String, Object> stringObjectEntry : headers.entrySet()) {
            builder.header(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
        }

        return send(builder.build());
    }

    private ApiResponse send(HttpRequest request) {
        try (HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build()) {
            HttpResponse<byte[]> response = httpClient
                .send(request, BodyHandlers.ofByteArray());
            return new ApiResponse(response.statusCode(), toMap(response));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> toMap(HttpResponse<byte[]> response) {
        return JsonParser.read(response.body());
    }

}
