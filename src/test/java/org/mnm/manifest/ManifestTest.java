package org.mnm.manifest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestTest {


    @Test
    void shouldMatch16CharsCrc() {
        Manifest.Chunk chunk = new Manifest.Chunk(0, 10, "56b2a21c1dc0b793", 0, 12, "f492f3ad9464a58d");

        String normalizedCrc = chunk.normalize();

        assertThat(normalizedCrc)
            .isEqualTo("56b2a21c1dc0b793")
            .hasSize(16);
    }

    @Test
    void shouldMatchLessThan16CharsCrc() {
        Manifest.Chunk chunk = new Manifest.Chunk(0, 10, "adb0b1594f912a", 0, 12, "f492f3ad9464a58d");

        String normalizedCrc = chunk.normalize();

        assertThat(normalizedCrc)
            .isEqualTo("00adb0b1594f912a")
            .hasSize(16);
    }
}
