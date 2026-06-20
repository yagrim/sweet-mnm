package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;

import org.mnm.events.ClientEventHandler;
import org.mnm.events.RepairListener;

/**
 * A modal JDialog that hosts a DualProgressPanel.
 * Shown by calling show(parent); hidden by the built-in close button.
 */
public class ProgressBarWindow extends JDialog
    implements RepairListener {

    private static final Color backgroundColor = new Color(220, 220, 220);

    private final DualProgressPanel panel;
    private final JButton closeBtn;

    public ProgressBarWindow(Frame owner, String label1, String label2) {
        super(owner, "Progress", true); // true = modal
        panel = new DualProgressPanel(label1, label2, backgroundColor);

        closeBtn = new JButton("Close");
        closeBtn.setEnabled(false);
        closeBtn.addActionListener(e -> close());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        footer.setBackground(backgroundColor);
        footer.add(new JSeparator(), BorderLayout.NORTH);
        footer.add(closeBtn);

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        add(panel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        pack();
        setSize((int) (owner.getWidth() * 0.9), getHeight());
        setLocationRelativeTo(owner);

        ClientEventHandler.getInstance().register(this);
    }

    public void resetProgress() {
        panel.resetProgress();
    }

    public void close() {
        SwingUtilities.invokeLater(this::dispose);
    }

    @Override
    public void repairStart() {
    }

    @Override
    public void repairDone(ClientStatus client) {
        closeBtn.setEnabled(true);
    }
}
