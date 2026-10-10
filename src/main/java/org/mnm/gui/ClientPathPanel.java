package org.mnm.gui;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.mnm.config.Environment;
import org.mnm.tools.ProcessUtils;

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
        this.setBorder(BorderFactory.createEmptyBorder(3 * SCALE, SCALE, 0, 0));
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

        JButton open = new JButton("Open");
        open.setToolTipText("Open in file explorer");
        open.addActionListener(_ -> openFileExplorer(textArea));

        GuiComponents.setFontSize(jlabel, BASE_FONT_SIZE * uiScaling);
        GuiComponents.setFontSize(textArea, BASE_FONT_SIZE * uiScaling);

        add(jlabel);
        add(Box.createHorizontalStrut(SCALE));
        add(textArea);
        add(Box.createHorizontalStrut(SCALE));
        add(copyToClipboard);
        add(Box.createHorizontalStrut(SCALE));
        add(open);

        ClientEventHandler.getInstance().register(this);
    }

    private void copyToClipboard(JTextComponent textPane) {
        String location = textPane.getText();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(location), null);
        logger.debug("Copied {} characters to clipboard.", location);
    }

    // experimental
    private void openFileExplorer(JTextComponent textPane) {
        String location = textPane.getText();
        logger.debug("Opening 1: {}", location);
        if (OS.isWindows()) {
            location = ClientPaths.protonPath(location).toString();
            logger.debug("Opening 2: {}", location);
        }
        try {
//            location ="C:\\Users\\User\\home";
            File file = new File(location);
            logger.debug("exists: {}", file.exists());
            try {
                Files.list(file.toPath()).forEach(p -> logger.debug("Contains: {}", p.toAbsolutePath()));
            } catch (IOException e) {
                logger.error("", e);
//            throw new RuntimeException(e);
            }
            logger.debug("Opening 3: {}", location);
            if (OS.isWindows()) {
                logger.debug("Calling explorer: {}", location);
                ProcessUtils.run(Environment.getWorkDir(),new String[]{
                    "explorer.exe", location
                });
//                new ProcessBuilder("explorer.exe", location).start();
            } else {
                Desktop.getDesktop().open(new File(location));
            }
        } catch (IOException e) {
            try {
                new ProcessBuilder(OS.isWindows() ? "explorer.exe" : "xdg-open", location).start();
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
        textArea.setToolTipText(gameSettingsPath);
        textArea.setCaretPosition(0);
    }

}
