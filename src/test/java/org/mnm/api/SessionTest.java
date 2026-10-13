package org.mnm.api;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.ApiServerStubs.stubAccountLogin;
import static org.mnm.ApiServerStubs.stubGameVersions;
import static org.mnm.ApiServerStubs.stubGameVersionsForSlugs;
import static org.mnm.ApiServerStubs.versionFor;

@WireMockTest(httpsEnabled = true)
class SessionTest {

    @Test
    void shouldLoginAndRetrieveSession(WireMockRuntimeInfo wiremock) {
        stubAccountLogin();
        stubGameVersions();

        String httpBaseUrl = wiremock.getHttpBaseUrl();
        ApiConnector apiConnector = new ApiConnector(new RestClient(httpBaseUrl));
        TestTokenSupplier tokenSupplier = new TestTokenSupplier();

        Session session = Session.login("username", "password", apiConnector, tokenSupplier);

        assertThat(session).isNotNull();
        assertThat(session.getSlug()).isEqualTo("mnm");
        assertThat(session.getChunksUrl()).isEqualTo("https://clients.domain.com/chunks");
    }

    @Test
    void shouldUseTheOnlySlugWhenNoneIsSelected(WireMockRuntimeInfo wiremock) {
        stubAccountLogin();
        stubGameVersionsForSlugs("https://clients.domain.com", "mnm");

        Session session = Session.login("username", "password", apiConnector(wiremock), new TestTokenSupplier());

        assertThat(session.getSlugs()).containsExactly("mnm");
        assertThat(session.getSlug()).isEqualTo("mnm");
    }

    @Test
    void shouldUseFirstSlugWhenMultipleAreFoundAndNoneIsSelected(WireMockRuntimeInfo wiremock) {
        stubAccountLogin();
        stubGameVersionsForSlugs("https://clients.domain.com", "mnm-test", "mnm");

        Session session = Session.login("username", "password", apiConnector(wiremock), new TestTokenSupplier());

        assertThat(session.getSlugs()).containsExactly("mnm-test", "mnm");
        assertThat(session.getSlug()).isEqualTo("mnm-test");
        assertThat(session.getVersion()).isEqualTo(versionFor("mnm-test"));
    }

    @Test
    void shouldSelectSlug(WireMockRuntimeInfo wiremock) {
        stubAccountLogin();
        stubGameVersionsForSlugs("https://clients.domain.com", "mnm-test", "mnm");

        Session session = Session.login("username", "password", apiConnector(wiremock), new TestTokenSupplier());
        Session selected = session.select("mnm");

        assertThat(selected.getSlug()).isEqualTo("mnm");
        assertThat(selected.getVersion()).isEqualTo(versionFor("mnm"));
        assertThat(selected.getToken()).isEqualTo(session.getToken());
        assertThat(selected.getSlugs()).containsExactly("mnm-test", "mnm");
        // Original session is not modified
        assertThat(session.getSlug()).isEqualTo("mnm-test");
    }

    @Test
    void shouldFailWhenSelectedSlugIsNotFound(WireMockRuntimeInfo wiremock) {
        stubAccountLogin();
        stubGameVersionsForSlugs("https://clients.domain.com", "mnm-test", "mnm");

        Session session = Session.login("username", "password", apiConnector(wiremock), new TestTokenSupplier());

        Throwable t = catchThrowable(() -> session.select("unknown"));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Slug 'unknown' not found, available slugs: [mnm-test, mnm]");
    }

    @Test
    void shouldWorkWhenMnmSlugIsNotPresent(WireMockRuntimeInfo wiremock) {
        stubAccountLogin();
        stubGameVersionsForSlugs("https://clients.domain.com", "alpha", "beta");

        Session session = Session.login("username", "password", apiConnector(wiremock), new TestTokenSupplier());

        assertThat(session.getSlugs()).containsExactly("alpha", "beta");
        assertThat(session.getSlug()).isEqualTo("alpha");
        assertThat(session.select("beta").getVersion()).isEqualTo(versionFor("beta"));
    }

    private static ApiConnector apiConnector(WireMockRuntimeInfo wiremock) {
        return new ApiConnector(new RestClient(wiremock.getHttpBaseUrl()));
    }

    @Test
    void shouldFailWhenCredentialsAreNotValid(WireMockRuntimeInfo wiremock) {
        stubFor(post(urlEqualTo("/account/login"))
            .willReturn(ResponseDefinitionBuilder.responseDefinition()
                .withStatus(200)
                .withBody("""
                    {"status":4, "error": "Incorrect Email/Password"}
                    """)));

        String httpBaseUrl = wiremock.getHttpBaseUrl();
        ApiConnector apiConnector = new ApiConnector(new RestClient(httpBaseUrl));
        TestTokenSupplier tokenSupplier = new TestTokenSupplier();

        Throwable t = catchThrowable(() -> Session.login("username", "password", apiConnector, tokenSupplier));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("API Error: 200 {error=Incorrect Email/Password, status=4}");
    }

    class TestTokenSupplier implements TokenSupplier {

        @Override
        public String getToken(String method, List<String> methods, String challengeToken) {
            return "";
        }
    }
}
