package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.OS;
import org.mnm.config.SettingsStore;
import org.mnm.events.ClientEventHandler;
import org.mnm.events.Refreshable;

import static org.mnm.config.SettingsStore.DEFAULT_UMU_USE_CLIENT_AS_PREFIX;
import static org.mnm.gui.Style.BASE_FONT_SIZE;
import static org.mnm.gui.Style.SCALE;

class ClientPathPanel extends JPanel
    implements Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(ClientPathPanel.class);

    private final SettingsStore settingsStore;
    private final JTextField textArea;

    ClientPathPanel(SettingsStore settingsStore, Color color, float uiScaling) {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(2 * SCALE, SCALE, 0, 0));
        this.settingsStore = settingsStore;

        JLabel jlabel = new JLabel("Settings & logs");
        jlabel.setToolTipText("Settings, UI profiles and logs");
        textArea = new JTextField();
        textArea.setBackground(color);
        textArea.setEditable(false);
        textArea.setMargin(new Insets(0, 2, 0, 2));

        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, textArea.getPreferredSize().height));

        JButton copyToClipboard = new JButton("Copy");
        copyToClipboard.setToolTipText("Copy to clipboard");
        copyToClipboard.addActionListener(_ -> copyToClipboard(textArea));

        GuiComponents.setFontSize(jlabel, BASE_FONT_SIZE * uiScaling);
        GuiComponents.setFontSize(textArea, BASE_FONT_SIZE * uiScaling);

        add(jlabel);
        add(Box.createHorizontalStrut(SCALE));
        add(textArea);
        add(Box.createHorizontalStrut(SCALE));
        add(copyToClipboard);

        ClientEventHandler.getInstance().register(this);
    }

    private void copyToClipboard(JTextComponent textPane) {
        String text = textPane.getText();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
        logger.debug("Copied {} characters to clipboard.", text);
//        openFileExplorer(textArea.getText());
    }

    // experimental
    private void openFileExplorer(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (IOException e) {
            try {
                new ProcessBuilder(OS.isWindows() ? "explorer.exe" : "xdg-open", path).start();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void refresh(ClientStatus client) {
        boolean clientAsWinePrefix = settingsStore.getBoolean(SettingsStore.UMU_USE_CLIENT_AS_PREFIX, DEFAULT_UMU_USE_CLIENT_AS_PREFIX);
        ClientPaths clientPaths = new ClientPaths(client, clientAsWinePrefix);
        String gameSettingsPath = clientPaths.getGameSettingsPath();
        textArea.setText(gameSettingsPath);
        textArea.setCaretPosition(0);
    }

}
