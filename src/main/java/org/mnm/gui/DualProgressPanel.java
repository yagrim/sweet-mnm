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

    public DualProgressPanel(String labelText1, String labelText2,
                             Color backgroundColor) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(backgroundColor);

        progressLabel1 = new ProgressLabel(labelText1, new Color(70, 130, 220));
        progressLabel2 = new ProgressLabel(labelText2, new Color(70, 190, 140));

        add(progressLabel1.label);
        add(Box.createVerticalStrut(6));
        add(progressLabel1.bar);
        add(Box.createVerticalStrut(20));
        add(progressLabel2.bar);
        add(Box.createVerticalStrut(6));
        add(progressLabel2.bar);

        ClientEventHandler.getInstance().register(this);
    }

    public void resetProgress() {
        progressLabel1.setValue(0);
        progressLabel2.setValue(0);
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

    private final class ProgressLabel {

        private final JLabel label;
        private final JProgressBar bar;
        private final String labelText;

        private ProgressLabel(String labelText, Color barColor) {
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

        public void setValue(int value) {
            bar.setValue(value);
        }

        public void setMaximum(int value) {
            bar.setMaximum(value);
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
    }

}
