package org.mnm.gui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class InfoPanel extends JPanel {

    private final JTextPane textArea;

    public InfoPanel(int width, int height, Color color) {
        super(new FlowLayout(FlowLayout.CENTER, 0, 0));
        textArea = new JTextPane();
        textArea.setEditable(false);
        textArea.setBackground(color);
        textArea.setBorder(new LineBorder(Color.GRAY, 1));
        textArea.setPreferredSize(new Dimension(width, height));

        StyledDocument doc = textArea.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        GuiComponents.setFontSize(textArea, 15);
        this.add(textArea);
    }

    public void setText(String text) {
        textArea.setText(text);
    }
}
