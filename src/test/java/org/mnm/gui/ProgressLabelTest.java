package org.mnm.gui;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mnm.gui.DualProgressPanel.ProgressLabel;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressLabelTest {

    private JLabel label;
    private JProgressBar bar;

    private ProgressLabel progressLabel;

    @BeforeEach
    void setUp() {
        final Color color = new Color(229, 145, 75);
        progressLabel = new ProgressLabel("Test label", color);
        label = ReflectionTestTools.getLabel(progressLabel, "label");
        bar = ReflectionTestTools.getProgressBar(progressLabel, "bar");
    }

    @Test
    void shouldInitialize() {
        assertThat(progressLabel).isNotNull();
        assertThat(label.getText()).isEqualTo("Test label");
        assertProgressValues(0, 100, 0);
    }

    @Test
    void shouldIncrementProgress() {
        progressLabel.increment();

        assertProgressValues(0, 100, 1);

        progressLabel.increment();
        progressLabel.increment();

        assertProgressValues(0, 100, 3);
    }

    @Test
    void shouldSetMaximum() {
        progressLabel.setMaximum(42);

        assertProgressValues(0, 42, 0);
    }

    @Test
    void shouldFakeMaximumWhenZero() {
        progressLabel.setMaximum(0);

        assertProgressValues(0, 100, 100);
    }

    @Test
    void shouldResetProgressButNotLabel() {
        progressLabel.increment();
        progressLabel.increment();

        assertProgressValues(0, 100, 2);
        assertThat(label.getText()).isEqualTo("Test label... 2 of 100");

        progressLabel.reset();

        assertProgressValues(0, 100, 0);
        assertThat(label.getText()).isEqualTo("Test label... 2 of 100");
    }

    private void assertProgressValues(int min, int max, int value) {
        assertThat(bar.getMinimum()).isEqualTo(min);
        assertThat(bar.getMaximum()).isEqualTo(max);
        assertThat(bar.getValue()).isEqualTo(value);
    }
}
