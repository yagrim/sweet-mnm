package org.mnm.api;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import org.mnm.tools.JsonParser;

import static org.mnm.tools.UrlBuilder.buildUrl;

public class RestClient {

    private final String baseUrl;

    public RestClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public HttpJsonResponse post(String url, Map<String, Object> body) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(buildUrl(baseUrl, url))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(JsonParser.toJson(body)))
            .build();

        return send(request);
    }

    public HttpJsonResponse get(String url) {
        return get(url, Map.of());
    }

    public HttpJsonResponse get(String url, Map<String, Object> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(buildUrl(baseUrl, url))
            .GET();
        for (Map.Entry<String, Object> stringObjectEntry : headers.entrySet()) {
            builder.header(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
        }

        return send(builder.build());
    }

    private static HttpJsonResponse send(HttpRequest request) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<byte[]> response = httpClient
                .send(request, BodyHandlers.ofByteArray());
            return new HttpJsonResponse(response.statusCode(), toMap(response));
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> toMap(HttpResponse<byte[]> response) {
        return JsonParser.read(response.body());
    }

    record HttpJsonResponse(int statusCode, Map<String, Object> body) {
    }

}
