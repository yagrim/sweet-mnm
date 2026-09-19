package org.mnm.api;

import java.util.Map;
import java.util.TreeMap;

public class ApiException extends RuntimeException {

    private final ApiResponse response;

    public ApiException(ApiResponse response) {
        this.response = response;
    }

    public String getError() {
        return response.getError();
    }

    @Override
    public String getMessage() {
        return "API Error: " + response.statusCode() + " " + serializeSortedMap(response.body());
    }

    private static String serializeSortedMap(Map<String, Object> inputMap) {
        if (inputMap == null) {
            return "{}";
        }
        Map<String, Object> sortedMap = new TreeMap<>(inputMap);
        return sortedMap.toString();
    }
}
