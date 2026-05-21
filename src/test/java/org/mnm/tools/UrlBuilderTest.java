package org.mnm.tools;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlBuilderTest {

    @Test
    void shouldBuildUrl() {
        String base = "https://clients.monstersandmemories.com/chunks";
        String path = "b81e5afbb805ae34.bin";

        URI uri = UrlBuilder.buildUrl(base, path);

        assertThat(base + "/" + path).isEqualTo(uri.toString());
    }

    @Test
    void shouldBuildUrlWithTrailingSlash() {
        String base = "https://clients.monstersandmemories.com/chunks/";
        String path = "b81e5afbb805ae34.bin";

        URI uri = UrlBuilder.buildUrl(base, path);

        assertThat(base + path).isEqualTo(uri.toString());
    }
}
