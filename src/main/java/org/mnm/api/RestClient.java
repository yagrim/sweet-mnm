package org.mnm.api;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import static org.mnm.tools.UrlBuilder.buildUrl;

public class RestClient {

    private static final JsonMapper mapper = new JsonMapper();

    private final String baseUrl;

    public RestClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public HttpJsonResponse post(String url, Map<String, Object> body) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUrl(baseUrl, url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
                .build();

        return send(request);
    }

    private String toJson(Map<String, Object> values) {
        return mapper.writeValueAsString(values);
    }

    private static Map toMap(HttpResponse<String> response) {
        return mapper.readValue(response.body(), Map.class);
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
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, BodyHandlers.ofString());
            return new HttpJsonResponse(response.statusCode(), toMap(response));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    record HttpJsonResponse(int statusCode, Map<String, Object> body) {
    }

}
