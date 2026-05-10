package org.mnm.api;

import org.mnm.api.RestClient.HttpJsonResponse;

import java.util.Map;

import static org.mnm.api.HttpHelper.parseResponse;

public class ApiConnector {

    private final RestClient restClient;

    private static final Integer API_VERSION = 21;

    public ApiConnector(RestClient restConnector) {
        this.restClient = restConnector;
    }

    public ApiConnection login(String username, String password) {

        HttpJsonResponse response = restClient.post("account/login", Map.of(
                "email", username,
                "password", password,
                "version", API_VERSION
        ));
        Map<String, Object> responseBody = parseResponse(response);

        return new ApiConnection(new ApiSession((String) responseBody.get("token")), restClient);
    }

}
