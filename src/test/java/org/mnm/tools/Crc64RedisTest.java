package org.mnm.tools;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Crc64RedisTest {

    @Test
    void shouldCalculateHex() {
        byte[] bytes = "1234567890".getBytes(StandardCharsets.UTF_8);

        String crc = Crc64Redis.calculateHex(bytes);

        assertThat(crc).isEqualTo("93be8325535c0007");
    }

    @Test
    void shouldCalculateHexForEmpty() {
        byte[] bytes = "".getBytes(StandardCharsets.UTF_8);

        String crc = Crc64Redis.calculateHex(bytes);

        assertThat(crc).isEqualTo("0000000000000000");
    }

    @Test
    void shouldFailWhenNull() {
        assertThatThrownBy(() -> Crc64Redis.calculateHex(null))
            .isInstanceOf(NullPointerException.class);
    }

}
