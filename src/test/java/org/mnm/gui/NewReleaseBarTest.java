package org.mnm.gui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewReleaseBarTest {

    @Test
    void shouldBeHiddenByDefault() {
        NewReleaseBar bar = new NewReleaseBar(1f);

        assertThat(bar.isVisible()).isFalse();
    }

    @Test
    void shouldShowRelease() {
        NewReleaseBar bar = new NewReleaseBar(1f);

        bar.showRelease("1.5.0");

        assertThat(bar.isVisible()).isTrue();
        assertThat(bar.getText()).isEqualTo("New version of Sweet v1.5.0 available!");
    }

    @Test
    void shouldHaveSameMarginsOnAllSides() {
        NewReleaseBar bar = new NewReleaseBar(1f);

        var insets = bar.getInsets();

        assertThat(bar.isOpaque()).isFalse();
        assertThat(insets.top).isPositive();
        assertThat(insets.left).isEqualTo(insets.top);
        assertThat(insets.right).isEqualTo(insets.top);
        assertThat(insets.bottom).isEqualTo(insets.top);
    }

    @Test
    void shouldResizeWindowWhenShown() {
        NewReleaseBar bar = new NewReleaseBar(1f);
        JFrame frame = new JFrame();
        try {
            frame.add(bar);
            frame.setResizable(false);
            frame.pack();
            int initialHeight = frame.getHeight();

            bar.showRelease("1.5.0");

            assertThat(frame.getHeight()).isGreaterThan(initialHeight);
        } finally {
            frame.dispose();
        }
    }

    @Test
    void shouldNotChangeWindowWidthWhenShown() {
        NewReleaseBar bar = new NewReleaseBar(1f);
        JFrame frame = new JFrame();
        try {
            JLabel otherContent = new JLabel("short");
            frame.add(otherContent, BorderLayout.NORTH);
            frame.add(bar, BorderLayout.CENTER);
            frame.setResizable(false);
            frame.pack();
            int initialWidth = frame.getWidth();
            otherContent.setText("a much longer text that makes the preferred width larger than before");

            bar.showRelease("1.5.0");

            assertThat(frame.getWidth()).isEqualTo(initialWidth);
        } finally {
            frame.dispose();
        }
    }
}
