package org.mnm.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.RestClient.HttpJsonResponse;

import static org.mnm.api.HttpHelper.exception;
import static org.mnm.api.HttpHelper.parseLoginResponse;
import static org.mnm.api.HttpHelper.parseResponse;

public class ApiConnector {

    private static final Logger logger = LoggerFactory.getLogger(ApiConnector.class);

    private final RestClient restClient;

    private static final Integer API_VERSION = 21;

    public ApiConnector(RestClient restConnector) {
        this.restClient = restConnector;
    }

    public ApiConnection login(String username, String password, VerificationCodeSupplier verificationCodeSupplier) {
        HttpJsonResponse response = restClient.post("account/login", Map.of(
            "email", username,
            "password", password,
            "version", API_VERSION
        ));
        Map<String, Object> responseMap = parseLoginResponse(response);

        Long status = (Long) responseMap.get("status");
        String token;
        if (status == 6) {
            String code = responseMap.get("code").toString();
            if ("two_factor_required".equals(code)) {
                token = handleTwoFactorAuthentication(responseMap, verificationCodeSupplier);
            } else {
                throw exception(response);
            }
        } else if (status != 0) {
            throw exception(response);
        } else {
            token = (String) responseMap.get("token");
        }

        return new ApiConnection(new ApiSession(token), restClient);
    }

    private String handleTwoFactorAuthentication(Map<String, Object> responseMap, VerificationCodeSupplier verificationCodeSupplier) {
        String method = responseMap.get("method").toString();
        List<String> methods = (List<String>) responseMap.get("methods");
        String challengeToken = responseMap.get("challenge_token").toString();

        String verificationCode = verificationCodeSupplier.getVerificationCode(method, methods, challengeToken);
        return verifyTwoFactorAuthentication(verificationCode, method, challengeToken);
    }

    private String verifyTwoFactorAuthentication(String code, String method, String challengeToken) {
        HttpJsonResponse response = restClient.post("account/login/2fa", Map.of(
            "challenge_token", challengeToken,
            "method", method,
            "code", code
        ));
        return (String) parseResponse(response).get("token");
    }

    public ApiConnection login(String token) {
        return new ApiConnection(new ApiSession(token), restClient);
    }

}
