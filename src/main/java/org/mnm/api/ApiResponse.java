package org.mnm.api;

import java.util.List;
import java.util.Map;

/**
 * Simple and convenient HTTP JSON response wrapper with utility methods for M&M API.
 */
record ApiResponse(int statusCode, Map<String, Object> body) {

    Long getStatus() {
        return (Long) body.get("status");
    }

    String getCode() {
        return body.get("code").toString();
    }

    public String get(String key) {
        return body.get(key).toString();
    }

    public List<String> getList(String key) {
        return (List<String>) body.get(key);
    }

    public Map<String, Object> getObject(String key) {
        return (Map<String, Object>) body.get(key);
    }

}
