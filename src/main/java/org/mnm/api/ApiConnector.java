package org.mnm.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.RestClient.HttpJsonResponse;

import static org.mnm.api.HttpHelper.parseResponse;

public class ApiConnector {

    private static final Logger logger = LoggerFactory.getLogger(ApiConnector.class);

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
        return new ApiConnection(new ApiSession((String) parseResponse(response).get("token")), restClient);
    }

    public ApiConnection login(String token) {
        return new ApiConnection(new ApiSession(token), restClient);
    }

}
