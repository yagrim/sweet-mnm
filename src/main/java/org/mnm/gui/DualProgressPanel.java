package org.mnm.gui;

import org.mnm.events.ClientEventHandler;
import org.mnm.events.FilesValidationListener;
import org.mnm.events.RepairFilesListener;
import org.mnm.events.RepairListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 * A reusable JPanel containing two labeled progress bars stacked vertically.
 * Labels are left-aligned above each bar.
 * <p>
 * Usage:
 * DualProgressPanel panel = new DualProgressPanel("Downloading...", "Installing...");
 * panel.setProgress1(45);
 * panel.setProgress2(72);
 */
public class DualProgressPanel extends JPanel
    implements FilesValidationListener, RepairFilesListener {

    private final JLabel label1;
    private final JLabel label2;
    private final JProgressBar progressBar1;
    private final JProgressBar progressBar2;

    public DualProgressPanel(String labelText1, String labelText2) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(new Color(245, 245, 248));

        label1 = createLabel(labelText1);
        progressBar1 = createProgressBar(new Color(70, 130, 220));

        label2 = createLabel(labelText2);
        progressBar2 = createProgressBar(new Color(70, 190, 140));

        add(label1);
        add(Box.createVerticalStrut(6));
        add(progressBar1);
        add(Box.createVerticalStrut(20));
        add(label2);
        add(Box.createVerticalStrut(6));
        add(progressBar2);

        ClientEventHandler.getInstance().register(this);
    }

    // --- Factories ---

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setForeground(new Color(50, 50, 60));
        return label;
    }

    private JProgressBar createProgressBar(Color fill) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(true);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        bar.setForeground(fill);
        bar.setBackground(new Color(220, 225, 235));
        return bar;
    }

    // --- Public API ---

    public void setProgress1(int value) {
        progressBar1.setValue(value);
    }

    public void setProgress2(int value) {
        progressBar2.setValue(value);
    }

    public void setLabel1Text(String text) {
        label1.setText(text);
    }

    public void setLabel2Text(String text) {
        label2.setText(text);
    }

    public int getProgress1() {
        return progressBar1.getValue();
    }

    public int getProgress2() {
        return progressBar2.getValue();
    }

    @Override
    public void validationStart(int filesCount) {
        progressBar1.setMaximum(filesCount);
    }

    @Override
    public void fileValidated() {
        int value = progressBar1.getValue();
        progressBar1.setValue(value + 1);
    }

    @Override
    public void filesToInstall(int filesCount) {
        progressBar2.setMaximum(filesCount);
        if (filesCount == 0) {
            progressBar2.setMaximum(100);
            progressBar2.setValue(100);
        }
    }

    @Override
    public void fileInstalled() {
        int value = progressBar2.getValue();
        progressBar2.setValue(value + 1);
    }

}
