package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import org.mnm.events.ClientEventHandler;
import org.mnm.events.FilesValidationListener;
import org.mnm.events.RepairFilesListener;

public class DualProgressPanel extends JPanel
    implements FilesValidationListener, RepairFilesListener {

    private final ProgressLabel progressLabel1;
    private final ProgressLabel progressLabel2;

    public DualProgressPanel(String labelText1, String labelText2, Color backgroundColor) {
        this(new ProgressLabel(labelText1, new Color(70, 130, 220)),
            new ProgressLabel(labelText2, new Color(70, 190, 140)),
            backgroundColor);
    }

    DualProgressPanel(ProgressLabel progressLabel1, ProgressLabel progressLabel2, Color backgroundColor) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(backgroundColor);

        this.progressLabel1 = progressLabel1;
        this.progressLabel2 = progressLabel2;

        add(progressLabel1.getLabel());
        add(Box.createVerticalStrut(6));
        add(progressLabel1.getBar());

        add(Box.createVerticalStrut(20));

        add(progressLabel2.getLabel());
        add(Box.createVerticalStrut(6));
        add(progressLabel2.getBar());

        ClientEventHandler.getInstance().register(this);
    }

    public void resetProgress() {
        progressLabel1.reset();
        progressLabel2.reset();
    }

    @Override
    public void validationStart(int filesCount) {
        progressLabel1.setMaximum(filesCount);
    }

    @Override
    public void fileValidated() {
        progressLabel1.increment();
    }

    @Override
    public void filesToInstall(int filesCount) {
        progressLabel2.setMaximum(filesCount);
    }

    @Override
    public void fileInstalled() {
        progressLabel2.increment();
    }

    static final class ProgressLabel {

        private final JLabel label;
        private final JProgressBar bar;
        private final String labelText;

        ProgressLabel(String labelText, Color barColor) {
            this.labelText = labelText;
            this.bar = createProgressBar(barColor);
            this.label = createLabel(labelText);
        }

        private static JProgressBar createProgressBar(Color fill) {
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(0);
            bar.setStringPainted(true);
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            bar.setForeground(fill);
            return bar;
        }

        private static JLabel createLabel(String text) {
            JLabel label = new JLabel(text);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            return label;
        }

        public void reset() {
            bar.setValue(0);
        }

        public void setMaximum(int value) {
            bar.setMaximum(value);
            // Fakes full progress when there's nothing to do
            if (value == 0) {
                bar.setMaximum(100);
                bar.setValue(100);
            }
        }

        public void increment() {
            int value;
            synchronized (bar) {
                value = bar.getValue() + 1;
                bar.setValue(value);
            }
            label.setText("%s... %s of %s".formatted(labelText, value, bar.getMaximum()));
        }

        public JProgressBar getBar() {
            return bar;
        }

        public JLabel getLabel() {
            return label;
        }
    }

}
