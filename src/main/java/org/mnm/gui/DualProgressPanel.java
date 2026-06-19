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

    private final JLabel label1;
    private final JLabel label2;
    private final JProgressBar progressBar1;
    private final JProgressBar progressBar2;

    public DualProgressPanel(String labelText1, String labelText2,
                             Color backgroundColor) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(backgroundColor);

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

    public void resetProgress() {
        this.progressBar1.setValue(0);
        this.progressBar2.setValue(0);
    }

    private String original1;
    private String original2;

    @Override
    public void validationStart(int filesCount) {
        progressBar1.setMaximum(filesCount);
        original1 = label1.getText();
    }

    @Override
    public void fileValidated() {
        int value = progressBar1.getValue() + 1;
        progressBar1.setValue(value);
        label1.setText("%s... %s of %s".formatted(original1, value, progressBar1.getMaximum()));
    }

    @Override
    public void filesToInstall(int filesCount) {
        progressBar2.setMaximum(filesCount);
        if (filesCount == 0) {
            progressBar2.setMaximum(100);
            progressBar2.setValue(100);
        }
        original2 = label2.getText();
    }

    @Override
    public void fileInstalled() {
        int value = progressBar2.getValue() + 1;
        progressBar2.setValue(value);
        label2.setText("%s... %s of %s".formatted(original2, value, progressBar2.getMaximum()));
    }

    // --- Factories ---
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JProgressBar createProgressBar(Color fill) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(true);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        bar.setForeground(fill);
        return bar;
    }

}
