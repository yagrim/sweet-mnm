package org.mnm.api;

import org.mnm.api.RestClient.HttpJsonResponse;

import java.util.List;
import java.util.Map;

import static org.mnm.api.HttpHelper.exception;
import static org.mnm.api.HttpHelper.parseResponse;

public class ApiConnection {

    private final ApiSession session;
    private final RestClient restClient;

    public ApiConnection(ApiSession session, RestClient restClient) {
        this.session = session;
        this.restClient = restClient;
    }

    public boolean isActive() {
        return !session.token().isBlank();
    }

    void isTokenValid() {
        Map<String, Object> headers = Map.of("Authorization", session.token());
        HttpJsonResponse response = restClient.get("account/me", headers);
        parseResponse(response);
    }

    public List<GameInfo> getGamesInfo() {
        Map<String, Object> headers = Map.of("Authorization", session.token());
        HttpJsonResponse response = restClient.get("account/games", headers);
        Map<String, Object> responseMap = parseResponse(response);

        Map<String, Object> games = (Map<String, Object>) responseMap.get("games");

        return games.entrySet().stream()
                .map(entry -> {
                    String id = entry.getKey();
                    Map<String, Object> value = (Map<String, Object>) entry.getValue();

                    String dir = (String) value.get("dir");
                    String name = (String) value.get("name");
                    String uriBase = (String) value.get("uri_base");
                    String uriKey = (String) value.get("uri_key");

                    return new GameInfo(id, name, dir, uriBase, uriKey);
                })
                .toList();
    }

    public List<GameVersion> getGamesVersions() {
        HttpJsonResponse response = restClient.get("game/versions?token=" + session.token());
        if (response.statusCode() != 200) {
            exception(response);
        }

        List<Map> versions = (List<Map>) response.body().get("versions");

        return versions.stream()
                .map(value -> {
                    String slug = (String) value.get("slug");
                    String version = (String) value.get("version");
                    String chunksUrl = (String) value.get("chunks_url");
                    String manifestUrl = (String) value.get("manifest_url");

                    return new GameVersion(slug, version, chunksUrl, manifestUrl);
                })
                .toList();
    }

    public String getToken() {
        return session.token();
    }

    public record GameInfo(String id, String name, String dir, String uriBase, String uriKey) {
    }

    public record GameVersion(String slug, String version, String chunksUrl, String manifest_url) {
    }
}
