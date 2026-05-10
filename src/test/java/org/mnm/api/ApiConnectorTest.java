package org.mnm.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.config.Environment.API_BASE_URL;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

class ApiConnectorTest {

    private final boolean mock = true;
//    private final boolean mock = false;

    private ApiConnector apiConnector;
    private RestClient restConnector;

    @BeforeEach
    void setup() {
        restConnector = mock ? Mockito.mock(RestClient.class) : new RestClient(API_BASE_URL);
        apiConnector = new ApiConnector(restConnector);
    }

    @Test
    void should_login() {
        final String username = "username";
        final String password = "password";

        if (mock) {
            Mockito.when(restConnector.post(anyString(), anyMap()))
                    .thenReturn(new RestClient.HttpJsonResponse(200, Map.of("status", 0, "token", "123.456.789")));
        }

        ApiConnection connection = apiConnector.login(username, password);

        assertThat(connection.isActive()).isTrue();
    }

    @Test
    void should_fail_login() {
        final String username = "username";
        final String password = "password";

        if (mock) {
            Mockito.when(restConnector.post(anyString(), anyMap()))
                    .thenThrow(new RuntimeException("Response error: 200, {error=Incorrect Email/Password, status=4}"));
        }

        Throwable t = catchThrowable(() -> apiConnector.login(username, password));

        assertThat(t)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Response error: 200, {error=Incorrect Email/Password, status=4}");
    }

}
