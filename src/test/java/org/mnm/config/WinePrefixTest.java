package org.mnm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WinePrefixTest {

    @Test
    void shouldAddSuffixToSlug() {
        assertThat(WinePrefix.name("mnm")).isEqualTo("mnm_prefix");
    }

    @Test
    void shouldReplaceSpacesInSlug() {
        assertThat(WinePrefix.name("other game")).isEqualTo("other_game_prefix");
        assertThat(WinePrefix.name(" a  b ")).isEqualTo("_a__b__prefix");
    }
}
