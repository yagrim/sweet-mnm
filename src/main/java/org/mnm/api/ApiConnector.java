package org.mnm.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mnm.api.ApiHelper.exception;
import static org.mnm.api.ApiHelper.parseLoginResponse;
import static org.mnm.api.ApiHelper.parseResponse;

public class ApiConnector {

    private static final Logger logger = LoggerFactory.getLogger(ApiConnector.class);

    private final RestClient restClient;

    private static final Integer API_VERSION = 21;

    public ApiConnector(RestClient restConnector) {
        this.restClient = restConnector;
    }

    public ApiConnection login(String username, String password, VerificationCodeSupplier verificationCodeSupplier) {
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

    private String handleTwoFactorAuthentication(ApiResponse response, VerificationCodeSupplier verificationCodeSupplier) {
        String method = response.get("method");
        List<String> methods = response.getList("methods");
        String challengeToken = response.get("challenge_token").toString();

        String verificationCode = verificationCodeSupplier.getVerificationCode(method, methods, challengeToken);
        return verifyTwoFactorAuthentication(verificationCode, method, challengeToken);
    }

    private String verifyTwoFactorAuthentication(String code, String method, String challengeToken) {
        ApiResponse httpResponse = restClient.post("account/login/2fa", Map.of(
            "challenge_token", challengeToken,
            "method", method,
            "code", code
        ));
        // TODO custom handling of errors for better UX
        return parseResponse(httpResponse).get("token");
    }

    public ApiConnection login(String token) {
        return new ApiConnection(new ApiSession(token), restClient);
    }

}
