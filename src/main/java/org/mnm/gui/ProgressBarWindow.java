package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;

/**
 * A modal JDialog that hosts a DualProgressPanel.
 * Shown by calling show(parent); hidden by the built-in close button.
 */
public class ProgressBarWindow extends JDialog {

    private final DualProgressPanel panel;

    public ProgressBarWindow(Frame owner) {
        this(owner, "Preparing files...", "Installing components...");
    }

    public ProgressBarWindow(Frame owner, String label1, String label2) {
        super(owner, "Progress", true); // true = modal
        panel = new DualProgressPanel(label1, label2);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> close());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        footer.add(closeBtn);

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        add(panel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        pack();
        setSize((int) (owner.getWidth() * 0.9), getHeight());
        setLocationRelativeTo(owner);
    }

    /**
     * Delegate accessors
     */
    public void setProgress1(int value) {
        panel.setProgress1(value);
    }

    public void setProgress2(int value) {
        panel.setProgress2(value);
    }

    public void setLabel1Text(String text) {
        panel.setLabel1Text(text);
    }

    public void setLabel2Text(String text) {
        panel.setLabel2Text(text);
    }

    public DualProgressPanel getPanel() {
        return panel;
    }

    public void close() {
        SwingUtilities.invokeLater(this::dispose);
    }

}
