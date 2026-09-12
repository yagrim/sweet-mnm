package org.mnm.gui;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Container;
import java.awt.GridLayout;

class ConfirmationDialog {

    static int showConfirmationDialog(Container parent, String title, String message) {
        final JPanel panel = new JPanel(new GridLayout(1, 1, 8, 8));
        panel.add(new JLabel(message));
        return JOptionPane.showConfirmDialog(
            parent,
            panel,
            title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
    }
}
