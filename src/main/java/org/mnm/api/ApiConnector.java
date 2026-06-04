package org.mnm.api;

import java.util.Map;

import org.mnm.api.RestClient.HttpJsonResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mnm.api.HttpHelper.parseResponse;

public class ApiConnector {

    private static final Logger logger = LoggerFactory.getLogger(ApiConnector.class);

    private final RestClient restClient;

    private static final Integer API_VERSION = 21;

    public ApiConnector(RestClient restConnector) {
        this.restClient = restConnector;
    }

    public ApiConnection login(String username, String password) {
        logger.debug("pre-POST");
        logger.debug("restClient: {}", restClient);
        HttpJsonResponse response = restClient.post("account/login", Map.of(
            "email", username,
            "password", password,
            "version", API_VERSION
        ));
        Map<String, Object> responseBody = parseResponse(response);
        logger.debug("post-POST");

        return new ApiConnection(new ApiSession((String) responseBody.get("token")), restClient);
    }

    public ApiConnection login(String token) {
        return new ApiConnection(new ApiSession(token), restClient);
    }

}
