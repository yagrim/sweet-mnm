package org.mnm.gui;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.gui.ReflectionTestTools.getField;

class SlugSelectorPanelTest {

    private final List<String> selections = new ArrayList<>();
    private SlugSelectorPanel panel;
    private JComboBox<?> slugs;

    @BeforeEach
    void setUp() {
        panel = new SlugSelectorPanel(selections::add, 1f);
        slugs = getField(panel, "slugs", JComboBox.class);
    }

    @Test
    void shouldBeEmptyAndEnabledByDefault() {
        assertThat(slugs.getItemCount()).isZero();
        assertThat(slugs.isEnabled()).isTrue();
        assertThat(panel.getSelectedSlug()).isNull();
    }

    @Test
    void shouldSetItemsWithoutNotifyingSelection() {
        panel.setItems(List.of("mnm-test", "mnm"), "mnm");

        assertThat(slugs.getItemCount()).isEqualTo(2);
        assertThat(panel.getSelectedSlug()).isEqualTo("mnm");
        assertThat(selections).isEmpty();
    }

    @Test
    void shouldNotifySelectionOnce() {
        panel.setItems(List.of("mnm-test", "mnm"), "mnm");

        slugs.setSelectedItem("mnm-test");

        assertThat(selections).containsExactly("mnm-test");
        assertThat(panel.getSelectedSlug()).isEqualTo("mnm-test");
    }

    @Test
    void shouldBeEnabledWithOneSlug() {
        panel.setItems(List.of("mnm"), "mnm");

        assertThat(slugs.isEnabled()).isTrue();
        assertThat(panel.getSelectedSlug()).isEqualTo("mnm");
    }

    @Test
    void shouldBeEnabledWithMultipleSlugs() {
        panel.setItems(List.of("mnm-test", "mnm"), "mnm");

        assertThat(slugs.isEnabled()).isTrue();
    }

    @Test
    void shouldFitWidthToLongestSlug() {
        panel.setItems(List.of("mnm", "mnm-test"), "mnm");

        assertThat(slugs.getPrototypeDisplayValue()).isEqualTo("mnm-test");
        assertThat(slugs.getMaximumSize()).isEqualTo(slugs.getPreferredSize());
    }

    @Test
    void shouldLimitWidthToMaxVisibleChars() {
        panel.setItems(List.of("mnm", "a-very-long-slug"), "a-very-long-slug");

        assertThat(slugs.getPrototypeDisplayValue()).isEqualTo("a-very-l");
        assertThat(slugs.getToolTipText()).isEqualTo("a-very-long-slug");
        assertThat(panel.getSelectedSlug()).isEqualTo("a-very-long-slug");
    }

    @Test
    void shouldNotStretchHorizontally() {
        panel.setItems(List.of("mnm"), "mnm");
        final int shortWidth = slugs.getMaximumSize().width;

        panel.setItems(List.of("mnm", "a-very-long-slug"), "mnm");
        final int maxWidth = slugs.getMaximumSize().width;

        assertThat(shortWidth).isLessThan(maxWidth);
        assertThat(maxWidth).isLessThan(Integer.MAX_VALUE);
    }
}
