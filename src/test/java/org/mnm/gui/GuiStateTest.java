package org.mnm.gui;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuiStateTest {

    @Test
    void shouldSelectPreferredSlug() {
        assertThat(GuiState.selectSlug(List.of("a", "mnm", "b"), "b")).isEqualTo("b");
    }

    @Test
    void shouldSelectMnmWhenPreferredSlugIsNotFound() {
        assertThat(GuiState.selectSlug(List.of("a", "mnm"), "b")).isEqualTo("mnm");
        assertThat(GuiState.selectSlug(List.of("a", "mnm"), null)).isEqualTo("mnm");
    }

    @Test
    void shouldSelectFirstSlugWhenMnmIsNotFound() {
        assertThat(GuiState.selectSlug(List.of("a", "b"), "c")).isEqualTo("a");
    }

    @Test
    void shouldSelectNothingWhenThereAreNoSlugs() {
        assertThat(GuiState.selectSlug(List.of(), "mnm")).isNull();
    }
}
