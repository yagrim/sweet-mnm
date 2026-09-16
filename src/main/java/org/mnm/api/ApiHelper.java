package org.mnm.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ApiHelper {

    private static final Logger logger = LoggerFactory.getLogger(ApiHelper.class);

    // Known errors:
    // 200, {"error":"Malformed Request","statusCode":1}
    // 200, {"error":"Incorrect Email/Password","statusCode":4}"
    // 429, {"error":"Too many requests","message":"Rate limit exceeded. Maximum 5 requests per 60 seconds."}
    // 429, {"error":"Too many sign-in attempts for this account. Wait a minute and try again.","message":"Rate limit exceeded. Maximum 5 requests per 60 seconds."}
    // 429, {"error":"Too many attempts. Wait a minute and try again.","code":"two_factor_rate_limited", "status": 6}

    /**
     * Processes response throwing an exception based on HTTP status code only.
     */
    static ApiResponse parseLoginResponse(ApiResponse apiResponse) {
        int statusCode = apiResponse.statusCode();
        if (statusCode != 200) {
            throw exception(apiResponse);
        }
        logger.debug("Http Response: {} - {}", statusCode, apiResponse.body());
        return apiResponse;
    }

    /**
     * Processes response throwing an exception based on HTTP and API status codes.
     */
    static ApiResponse parseResponse(ApiResponse apiResponse) {
        ApiResponse response = parseLoginResponse(apiResponse);
        Long status = response.getStatus();
        if (status != 0) {
            throw exception(apiResponse);
        }
        return response;
    }

    static RuntimeException exception(ApiResponse response) {
        throw new ApiException(response);
    }

}
