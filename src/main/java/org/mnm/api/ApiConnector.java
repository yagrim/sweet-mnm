package org.mnm.api;

import java.util.List;
import java.util.Map;

import static org.mnm.api.ApiHelper.exception;
import static org.mnm.api.ApiHelper.parseLoginResponse;
import static org.mnm.api.ApiHelper.parseResponse;

public class ApiConnector {

    private final RestClient restClient;

    private static final Integer API_VERSION = 21;

    public ApiConnector(RestClient restConnector) {
        this.restClient = restConnector;
    }

    public ApiConnection login(String username, String password, TokenSupplier verificationCodeSupplier) {
        ApiResponse httpResponse = restClient.post("account/login", Map.of(
            "email", username,
            "password", password,
            "version", API_VERSION
        ));
        var response = parseLoginResponse(httpResponse);

        Long status = response.getStatus();
        String token;
        if (status == 6) {
            String code = response.getCode();
            if ("two_factor_required".equals(code)) {
                token = handleTwoFactorAuthentication(response, verificationCodeSupplier);
            } else {
                throw exception(httpResponse);
            }
        } else if (status != 0) {
            throw exception(httpResponse);
        } else {
            token = response.get("token");
        }

        return new ApiConnection(new ApiSession(token), restClient);
    }

    private String handleTwoFactorAuthentication(ApiResponse response, TokenSupplier tokenSupplier) {
        String method = response.get("method");
        List<String> methods = response.getList("methods");
        String challengeToken = response.get("challenge_token").toString();
        return tokenSupplier.getToken(method, methods, challengeToken);
    }

    public ApiConnection login(String token) {
        return new ApiConnection(new ApiSession(token), restClient);
    }

    public void resendVerificationCode(String challengeToken, String method) {
        ApiResponse response = restClient.post("account/login/2fa/" + method, Map.of("challenge_token", challengeToken));
        // Beware! Success is reported as status=6
        parseLoginResponse(response);
    }

    public String twoFactorAuthentication(String method, String code, String challengeToken) {
        ApiResponse response = restClient.post("account/login/2fa", Map.of(
            "method", method,
            "code", code,
            "challenge_token", challengeToken
        ));

        return parseResponse(response).get("token");
    }
}
