package org.mnm.api;

import org.mnm.api.RestClient.HttpJsonResponse;

import java.util.Map;

import static org.mnm.api.HttpHelper.parseResponse;

public class ApiConnector {

    private final RestClient restClient;

    private static final Integer API_VERSION = 21;
    private static final String BASE_URL = "https://account.monstersandmemories.com/api/";

    public ApiConnector(RestClient restConnector) {
        this.restClient = restConnector;
    }

    public ApiConnection getConnection(String username, String password) {

        HttpJsonResponse response = restClient.post(url("account/login"), Map.of(
                "email", username,
                "password", password,
                "version", API_VERSION
        ));
        Map<String, Object> responseBody = parseResponse(response);

        return new ApiConnection(new ApiSession((String) responseBody.get("token")), restClient);
    }

    static String url(String path) {
        return BASE_URL + path;
    }
}
