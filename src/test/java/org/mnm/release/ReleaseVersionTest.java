package org.mnm.release;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseVersionTest {

    @ParameterizedTest
    @ValueSource(strings = {"1.5.0", "v1.5.0", " v1.5.0\n"})
    void shouldParseVersion(String value) {
        assertThat(ReleaseVersion.parse(value)).contains(new ReleaseVersion(1, 5, 0));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "0.0.1-SNAPSHOT", "1.5", "v1.5.0-rc1", "release"})
    void shouldNotParseInvalidVersion(String value) {
        assertThat(ReleaseVersion.parse(value)).isEmpty();
    }

    @Test
    void shouldCompareNumbersNotText() {
        assertThat(new ReleaseVersion(1, 10, 0)).isGreaterThan(new ReleaseVersion(1, 9, 0));
        assertThat(new ReleaseVersion(2, 0, 0)).isGreaterThan(new ReleaseVersion(1, 99, 99));
        assertThat(new ReleaseVersion(1, 5, 1)).isGreaterThan(new ReleaseVersion(1, 5, 0));
        assertThat(new ReleaseVersion(1, 5, 0)).isEqualByComparingTo(new ReleaseVersion(1, 5, 0));
    }

    @Test
    void shouldFormatWithoutPrefix() {
        assertThat(new ReleaseVersion(1, 5, 0)).hasToString("1.5.0");
    }
}
