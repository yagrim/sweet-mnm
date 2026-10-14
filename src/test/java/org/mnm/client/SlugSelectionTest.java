package org.mnm.client;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugSelectionTest {

    @Test
    void shouldSelectPreferredSlug() {
        assertThat(SlugSelection.select(List.of("a", "mnm", "b"), "b")).isEqualTo("b");
    }

    @Test
    void shouldSelectMnmWhenPreferredSlugIsNotFound() {
        assertThat(SlugSelection.select(List.of("a", "mnm"), "b")).isEqualTo("mnm");
        assertThat(SlugSelection.select(List.of("a", "mnm"), null)).isEqualTo("mnm");
    }

    @Test
    void shouldSelectFirstSlugWhenMnmIsNotFound() {
        assertThat(SlugSelection.select(List.of("a", "b"), "c")).isEqualTo("a");
    }

    @Test
    void shouldSelectNothingWhenThereAreNoSlugs() {
        assertThat(SlugSelection.select(List.of(), "mnm")).isNull();
    }
}
