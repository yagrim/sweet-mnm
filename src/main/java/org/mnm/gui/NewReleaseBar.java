package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

import static org.mnm.gui.GuiComponents.setFontSize;
import static org.mnm.gui.Style.BASE_FONT_SIZE;
import static org.mnm.gui.Style.SCALE;

class NewReleaseBar extends JPanel {

    private static final Color BACKGROUND = new Color(255, 236, 179);
    private static final Color BORDER = new Color(255, 193, 7);
    private static final Color FOREGROUND = new Color(102, 60, 0);

    private final JLabel label;

    NewReleaseBar(float uiScaling) {
        super(new BorderLayout());
        // The outer panel is transparent: its border is the margin around the colored box
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(SCALE, SCALE, SCALE, SCALE));
        this.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JPanel box = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        box.setBackground(BACKGROUND);
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(SCALE, SCALE, SCALE, SCALE)));

        this.label = new JLabel();
        label.setForeground(FOREGROUND);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        setFontSize(label, BASE_FONT_SIZE * uiScaling);
        box.add(label);
        this.add(box, BorderLayout.CENTER);

        this.setVisible(false);
    }

    String getText() {
        return label.getText();
    }

    void showRelease(String newVersion) {
        label.setText("New version of Sweet v%s available!".formatted(newVersion));
        this.setVisible(true);

        // The window is not resizable and is packed before the bar is visible.
        // Do not use pack(): other components can be wider now and change the width.
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.setSize(window.getWidth(), window.getHeight() + getPreferredSize().height);
            window.validate();
        }
    }

    // Prevent BoxLayout from making the bar taller than its content
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
