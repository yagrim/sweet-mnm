package org.mnm.api;

import java.util.Map;

public class HttpHelper {

    // Known errors:
    // 200, {"error":"Malformed Request","statusCode":1}
    // 200, {"error":"Incorrect Email/Password","statusCode":4}"
    // 429, {"error":"Too many requests","message":"Rate limit exceeded. Maximum 5 requests per 60 seconds."}
    static Map<String, Object> parseResponse(RestClient.HttpJsonResponse response) {
        int statusCode = response.statusCode();
        if (statusCode != 200) {
            throw exception(response);
        }
        Map<String, Object> body = response.body();
        Integer status = (Integer) body.get("status");
        if (status != 0) {
            throw exception(response);
        }
        return body;
    }

    static RuntimeException exception(RestClient.HttpJsonResponse response) {
        throw new RuntimeException("Response error: " + response.statusCode() + ", " + response.body());
    }
}
