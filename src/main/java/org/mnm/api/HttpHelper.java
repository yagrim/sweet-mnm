package org.mnm.api;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class);

    // Known errors:
    // 200, {"error":"Malformed Request","statusCode":1}
    // 200, {"error":"Incorrect Email/Password","statusCode":4}"
    // 429, {"error":"Too many requests","message":"Rate limit exceeded. Maximum 5 requests per 60 seconds."}
    // 429, {"error":"Too many sign-in attempts for this account. Wait a minute and try again.","message":"Rate limit exceeded. Maximum 5 requests per 60 seconds."}
    // 429, {"error":"Too many attempts. Wait a minute and try again.","code":"two_factor_rate_limited", "status": 6}
    static JsonResponse parseLoginResponse(RestClient.HttpJsonResponse httpResponse) {
        int statusCode = httpResponse.statusCode();
        if (statusCode != 200) {
            throw exception(httpResponse);
        }
        Map<String, Object> body = httpResponse.body();
        logger.debug("Http Response: {} - {}", httpResponse, body);
        return new JsonResponse(body);
    }

    public static JsonResponse parseResponse(RestClient.HttpJsonResponse httpResponse) {
        JsonResponse response = parseLoginResponse(httpResponse);
        Long status = response.getStatus();
        if (status != 0) {
            throw exception(httpResponse);
        }
        return response;
    }

    static RuntimeException exception(RestClient.HttpJsonResponse response) {
        throw new RuntimeException("Response error: " + response.statusCode() + ", " + serializeSortedMap(response.body()));
    }

    public static String serializeSortedMap(Map<String, Object> inputMap) {
        if (inputMap == null) {
            return "{}";
        }
        Map<String, Object> sortedMap = new TreeMap<>(inputMap);
        return sortedMap.toString();
    }

}
