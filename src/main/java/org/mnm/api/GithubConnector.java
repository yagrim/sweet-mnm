package org.mnm.api;

import java.util.List;
import java.util.Map;

import static org.mnm.api.ApiHelper.parseLoginResponse;

public class GithubConnector {

    static final String LATEST_RELEASE_PATH = "repos/yagrim/sweet-mnm/releases/latest";

    // GitHub rejects requests without User-Agent
    private static final Map<String, Object> HEADERS = Map.of(
        "Accept", "application/vnd.github+json",
        "User-Agent", "sweet-mnm"
    );

    private final RestClient restClient;

    public GithubConnector(RestClient restClient) {
        this.restClient = restClient;
    }

    // The latest release does not include drafts and pre-releases
    public Release getLatestRelease() {
        ApiResponse response = parseLoginResponse(restClient.get(LATEST_RELEASE_PATH, HEADERS));

        List<Map<String, Object>> assets = (List<Map<String, Object>>) response.body().get("assets");

        return new Release(
            response.get("tag_name"),
            response.get("published_at"),
            assets.stream()
                .map(value -> {
                    String name = (String) value.get("name");
                    String browserDownloadUrl = (String) value.get("browser_download_url");

                    return new Asset(name, browserDownloadUrl);
                })
                .toList());
    }

    public record Release(String tagName, String publishedAt, List<Asset> assets) {
    }

    public record Asset(String name, String downloadUrl) {
    }
}
