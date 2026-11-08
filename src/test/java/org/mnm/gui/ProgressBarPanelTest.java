package org.mnm.gui;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mnm.gui.DualProgressPanel.FileCounterBar;
import org.mnm.gui.DualProgressPanel.FileSizeBar;
import org.mnm.gui.DualProgressPanel.ProgressBarPanel;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressBarPanelTest {

    private JLabel label;
    private JProgressBar bar;

    private ProgressBarPanel progressBar;


    abstract class BaseClass {

        abstract ProgressBarPanel createPanel(Color color);

        @BeforeEach
        void setUp() {
            final Color color = new Color(229, 145, 75);
            progressBar = createPanel(color);
            label = ReflectionTestTools.getField(progressBar, "label", JLabel.class);
            bar = ReflectionTestTools.getField(progressBar, "bar", JProgressBar.class);
        }

        @Test
        void shouldInitialize() {
            assertThat(progressBar).isNotNull();
            assertThat(label.getText()).isEqualTo("Test label");
            assertProgressValues(0, 100, 0);
        }

    }

    @Nested
    class FileCounterBarTest extends BaseClass {

        @Override
        ProgressBarPanel createPanel(Color color) {
            return new FileCounterBar("Test label", color, color, 1f);
        }

        @Test
        void shouldSetMaximum() {
            progressBar.setMaximum(300);
            assertThat(label.getText()).isEqualTo("Test label");
        }

        @Test
        void shouldSetDefaultsWhenZero() {
            progressBar.setMaximum(0);

            assertProgressValues(0, 100, 100);
        }

        @Test
        void shouldCountFiles() {
            progressBar.setMaximum(42);
            assertThat(label.getText()).isEqualTo("Test label");

            progressBar.increment(1);
            assertProgressValues(0, 42, 1);
            assertThat(label.getText()).isEqualTo("Test label... 1 of 42 files");

            progressBar.increment(2);
            assertProgressValues(0, 42, 3);
            assertThat(label.getText()).isEqualTo("Test label... 3 of 42 files");
        }


        @Test
        void shouldResetProgressButNotLabel() {
            progressBar.increment(2);

            assertProgressValues(0, 100, 2);
            assertThat(label.getText()).isEqualTo("Test label... 2 of 100 files");

            progressBar.reset();

            assertProgressValues(0, 100, 0);
            assertThat(label.getText()).isEqualTo("Test label... 2 of 100 files");
        }

    }

    @Nested
    class FileSizeBarTest extends BaseClass {

        int KB = 1024;
        int FACTOR = 1024 * KB;

        @Override
        ProgressBarPanel createPanel(Color color) {
            return new FileSizeBar("Test label", color, color, 1f, 1);
        }

        @Test
        void shouldSetMaximumInKilobytes() {
            long bytes = 17343956L;
            progressBar.setMaximum(bytes);
            assertProgressValues(0, 16938, 0);
            assertThat(label.getText()).isEqualTo("Test label");
        }

        @Test
        void shouldSetDefaultsWhenZero() {
            progressBar.setMaximum(0L);

            assertProgressValues(0, 100, 100);
        }

        @Test
        void shouldCountFilesSizeInMegabytes() {
            long bytes = 17343956L;
            progressBar.setMaximum(bytes);
            assertThat(label.getText()).isEqualTo("Test label");

            progressBar.increment(Long.valueOf(FACTOR * 8));
            assertProgressValues(0, 16938, 8 * KB);
            assertThat(label.getText()).isEqualTo("Test label... 8 of 16 MB");

            progressBar.increment(Long.valueOf(FACTOR * 8));
            assertProgressValues(0, 16938, 16 * KB);
            assertThat(label.getText()).isEqualTo("Test label... 16 of 16 MB");

            progressBar.increment(bytes - FACTOR * 16);
            assertProgressValues(0, 16938, 16938);
            assertThat(label.getText()).isEqualTo("Test label... 16 of 16 MB");
        }

        @Test
        void shouldShowProgressForFileSmallerThanOneMegabyte() {
            long bytes = 300 * KB + 5;
            progressBar.setMaximum(bytes);
            assertProgressValues(0, 301, 0);

            progressBar.increment(100L * KB);
            assertProgressValues(0, 301, 100);

            progressBar.increment(bytes - 100L * KB);
            assertProgressValues(0, 301, 301);
            assertThat(label.getText()).isEqualTo("Test label... 0 of 0 MB");
        }

        @Test
        void shouldShowProgressForFileSmallerThanOneKilobyte() {
            progressBar.setMaximum(42L);
            assertProgressValues(0, 1, 0);

            progressBar.increment(42L);
            assertProgressValues(0, 1, 1);
        }

        @Test
        void shouldShowMultiStepProgressForFileSmallerThanOneMegabyte() {
            progressBar = new FileSizeBar("Test label", Color.GREEN, Color.GREEN, 1f, 2);
            bar = ReflectionTestTools.getField(progressBar, "bar", JProgressBar.class);

            long bytes = 500;
            progressBar.setMaximum(bytes);
            assertProgressValues(0, 1, 0);

            progressBar.increment(bytes);
            progressBar.increment(bytes);
            assertProgressValues(0, 1, 1);
        }

        @Test
        void shouldSupportLargeSizes() {
            long bytes = 500L * 1024 * FACTOR;
            progressBar.setMaximum(bytes);
            assertProgressValues(0, 500 * 1024 * KB, 0);

            progressBar.increment(bytes);
            assertProgressValues(0, 500 * 1024 * KB, 500 * 1024 * KB);
            assertThat(label.getText()).isEqualTo("Test label... 512000 of 512000 MB");
        }

        @Test
        void shouldSupportMultiStepProgress() {
            progressBar = new FileSizeBar("Test label", Color.GREEN, Color.GREEN, 1f, 2);
            label = ReflectionTestTools.getField(progressBar, "label", JLabel.class);
            bar = ReflectionTestTools.getField(progressBar, "bar", JProgressBar.class);

            progressBar.setMaximum(Long.valueOf(FACTOR * 16));
            progressBar.increment(Long.valueOf(FACTOR * 8));

            assertProgressValues(0, 32 * KB, 8 * KB);
            assertThat(label.getText()).isEqualTo("Test label... 4 of 16 MB");
        }

        // better to be inaccurate than to crash
        @Test
        void shouldOverflowAndNotFail() {
            long bytes = 17343956L;
            progressBar.setMaximum(bytes);

            progressBar.increment(Long.valueOf(FACTOR * 8));
            progressBar.increment(Long.valueOf(FACTOR * 8));
            progressBar.increment(Long.valueOf(FACTOR * 2));
            assertProgressValues(0, 16938, 16938);
            assertThat(label.getText()).isEqualTo("Test label... 18 of 16 MB");
        }

        @Test
        void shouldResetProgressButNotLabel() {
            assertProgressValues(0, 100, 0);
            progressBar.increment(Long.valueOf(FACTOR * 1));
            assertThat(label.getText()).isEqualTo("Test label... 1 of 100 MB");

            progressBar.reset();

            progressBar.increment(Long.valueOf(FACTOR * 3));
            assertThat(label.getText()).isEqualTo("Test label... 3 of 100 MB");
        }

    }

    private void assertProgressValues(int min, int max, int value) {
        assertThat(bar.getMinimum()).isEqualTo(min);
        assertThat(bar.getMaximum()).isEqualTo(max);
        assertThat(bar.getValue()).isEqualTo(value);
    }
}
