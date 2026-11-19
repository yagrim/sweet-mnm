package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.OS;

import static org.mnm.gui.Style.BASE_FONT_SIZE;
import static org.mnm.gui.Style.SCALE;

class PathPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(PathPanel.class);

    private final JTextField textArea;
    private final JButton copyToClipboard;
    private final JButton open;

    PathPanel(String label, String labelTooltip, Color color, float uiScaling) {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(3 * SCALE, SCALE, 0, 0));

        JLabel jlabel = new JLabel(label);
        jlabel.setToolTipText(labelTooltip);
        textArea = new JTextField();
        textArea.setBackground(color);
        textArea.setEditable(false);
        textArea.setMargin(new Insets(0, 2, 0, 2));

        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, textArea.getPreferredSize().height));

        copyToClipboard = new JButton("Copy");
        copyToClipboard.setEnabled(false);
        copyToClipboard.setToolTipText("Copy to clipboard");
        copyToClipboard.addActionListener(_ -> copyToClipboard());

        open = new JButton("Open");
        open.setEnabled(false);
        open.setToolTipText("Open in file explorer");
        open.addActionListener(_ -> openFileExplorer());

        GuiComponents.setFontSize(jlabel, BASE_FONT_SIZE * uiScaling);
        GuiComponents.setFontSize(textArea, BASE_FONT_SIZE * uiScaling);

        add(jlabel);
        add(Box.createHorizontalStrut(SCALE));
        add(textArea);
        add(Box.createHorizontalStrut(SCALE));
        add(copyToClipboard);
        // Opening file explorer from Proton fails:
        // - Desktop.getDesktop() -> wine: Call from 00006FFFFFF5D040 to unimplemented function ADVAPI32.dll.SaferiIsExecutableFileType, aborting
        // - calling explorer.exe doesn't do anything
        if (!OS.isWindows()) {
            add(Box.createHorizontalStrut(SCALE));
            add(open);
        }
    }

    void setPath(String path, boolean valid) {
        textArea.setText(path);
        textArea.setToolTipText(path);
        textArea.setCaretPosition(0);
        copyToClipboard.setEnabled(valid);
        open.setEnabled(valid);
    }

    private void copyToClipboard() {
        String location = textArea.getText();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(location), null);
        logger.debug("Copied {} characters to clipboard.", location);
    }

    // Running from proton is not working. Left in case of further investigation.
    private void openFileExplorer() {
        String location = textArea.getText();
        try {
            Desktop.getDesktop().open(new File(location));
        } catch (IOException e) {
            try {
                new ProcessBuilder(OS.isWindows() ? "explorer.exe" : "xdg-open", location).start();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex.getMessage(), ex);
            }
        }
    }

}
