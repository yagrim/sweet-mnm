package org.mnm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitVersionTest {

    private static final String PREFIX = "publish";
    private static final String SEMVER = "0.25.0.0";
    private static final String SHA = "a6afacc94b4fcb1bcf4bf250cebf05e4b9af6183";
    private static final String FULL_VERSION = PREFIX + "-" + SEMVER + "-" + SHA;


    @Test
    void shouldSplitInConstructor() {
        SplitVersion version = new SplitVersion(FULL_VERSION);

        assertThat(version.getPrefix()).isEqualTo(PREFIX);
        assertThat(version.getSemver()).isEqualTo(SEMVER);
        assertThat(version.getSha()).isEqualTo(SHA);
        assertThat(version.getShortSha()).isEqualTo("a6afacc");
    }

    @Test
    void shouldTreatThirdSegmentAsShaEvenIfItContainsDashes() {
        String versionWithDashInSha = "myapp-1.2.3-abc1234-extra";
        SplitVersion version = new SplitVersion(versionWithDashInSha);

        assertThat(version.getPrefix()).isEqualTo("myapp");
        assertThat(version.getSemver()).isEqualTo("1.2.3");
        assertThat(version.getSha()).isEqualTo("abc1234-extra");
    }


    @Test
    void shouldReturnShortShaWithExactlySevenCharacters() {
        SplitVersion version = new SplitVersion(FULL_VERSION);

        assertThat(version.getShortSha()).hasSize(7);
    }

    @Test
    void shouldReturnShortShaWhenShaIsExactlySevenChars() {
        SplitVersion version = new SplitVersion(PREFIX + "-" + SEMVER + "-abc1234");

        assertThat(version.getShortSha()).isEqualTo("abc1234");
    }

    @Test
    void shouldFailWhenFewerThanThreeSegments() {
        assertThatThrownBy(() -> new SplitVersion("only-two"))
            .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-"})
    void shouldFailWhenMalformed(String bad) {
        assertThatThrownBy(() -> new SplitVersion(bad))
            .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--"})
    void shouldFailWhenEmpty(String bad) {
        assertThatThrownBy(() -> new SplitVersion(bad))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailWhenShaIsInvalid() {
        SplitVersion version = new SplitVersion(PREFIX + "-" + SEMVER + "-abc");

        assertThatThrownBy(version::getShortSha)
            .isInstanceOf(StringIndexOutOfBoundsException.class);
    }

}
