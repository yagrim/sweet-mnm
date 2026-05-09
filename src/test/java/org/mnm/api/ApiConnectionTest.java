package org.mnm.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.Environment.API_BASE_URL;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

class ApiConnectionTest {

    private final boolean mock = true;
//    private final boolean mock = false;

    private ApiConnector apiConnector;
    private RestClient restConnector;
    private ApiConnection connection;

    @BeforeEach
    void setup() {
        restConnector = mock ? Mockito.mock(RestClient.class) : new RestClient(API_BASE_URL);
        apiConnector = new ApiConnector(restConnector);

        if (mock) {
            connection = new ApiConnection(new ApiSession("123.456.789"), restConnector);
        } else {
            final String username = "";
            final String password = "";
            connection = apiConnector.getConnection(username, password);
        }
    }

    @Test
    void should_validateToken() {
        if (mock) {
            Mockito.when(restConnector.get(anyString(), anyMap()))
                    .thenReturn(new RestClient.HttpJsonResponse(200, Map.of("status", 0)));
        }

        connection.isTokenValid();

        assertThat(connection.isActive()).isTrue();
    }

    @Test
    void should_getGamesInfo() {
        if (mock) {
            Mockito.when(restConnector.get(anyString(), anyMap()))
                    .thenReturn(new RestClient.HttpJsonResponse(200, Map.of("status", 0,
                            "games", Map.of(
                                    "mnm", Map.of(
                                            "dir", "mnm",
                                            "name", "Monsters & Memories",
                                            "uri_base", "https://client-r3.monstersandmemories.com",
                                            "uri_key", "release"
                                    )
                            )
                    )));
        }

        List<ApiConnection.GameInfo> games = connection.getGamesInfo();

        assertThat(games)
                .containsExactly(new ApiConnection.GameInfo(
                        "mnm",
                        "Monsters & Memories",
                        "mnm",
                        "https://client-r3.monstersandmemories.com",
                        "release"
                ));
    }

    @Test
    void should_getGamesVersions() {
        if (mock) {
            Mockito.when(restConnector.get(anyString()))
                    .thenReturn(new RestClient.HttpJsonResponse(200, Map.of(
                            "versions", List.of(Map.of(
                                    "slug", "mnm",
                                    "version", "publish-0.21.2.0-95f37c2aba9b89a27bd3ac54ddb52b58970beb54",
                                    "chunks_url", "http://clients.monstersandmemories.com/chunks",
                                    "manifest_url", "http://clients.monstersandmemories.com/manifests/95f37c2aba9b89a27bd3ac54ddb52b58970beb54.manifest"
                            ))
                    )));
        }

        List<ApiConnection.GameVersion> games = connection.getGamesVersions();

        assertThat(games)
                .containsExactly(new ApiConnection.GameVersion(
                        "mnm",
                        "publish-0.21.2.0-95f37c2aba9b89a27bd3ac54ddb52b58970beb54",
                        "http://clients.monstersandmemories.com/chunks",
                        "http://clients.monstersandmemories.com/manifests/95f37c2aba9b89a27bd3ac54ddb52b58970beb54.manifest"
                ));
    }

}
