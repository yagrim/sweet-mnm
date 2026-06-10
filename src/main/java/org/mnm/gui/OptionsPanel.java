package org.mnm.gui;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.client.Installation;
import org.mnm.client.RunnerOptions;
import org.mnm.config.OS;
import org.mnm.tools.FileUtils;

import static org.mnm.GeneralOptions.toggleDebug;
import static org.mnm.gui.MessageWindow.showErrorMessageDialogSync;

// NOTE: so far options can be grouped as repair or run.
class OptionsPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(OptionsPanel.class);

    private final JCheckBox debugOption = new JCheckBox("Enable debug");
    private final JCheckBox inMemoryHashingOption = new JCheckBox("In-memory hashing");
    private final JCheckBox mangoHudOption = new JCheckBox("Enable MangoHud");

    OptionsPanel() {
        super();
    }

    JPanel initialize(ClientStatus clientStatus) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));

        debugOption.setActionCommand("debug");
        debugOption.addActionListener(_ -> toggleDebug(debugOption.isSelected()));

        inMemoryHashingOption.setActionCommand("in-memory-hashing");
        inMemoryHashingOption.setSelected(true);

        mangoHudOption.setActionCommand("mangohud");
        if (OS.isWindows()) {
            mangoHudOption.setEnabled(false);
            mangoHudOption.setText("Enable MangoHud (Linux only)");
        }

        this.add(debugOption);
        this.add(Box.createVerticalStrut(8));
        this.add(inMemoryHashingOption);
        this.add(Box.createVerticalStrut(8));
        this.add(mangoHudOption);
        this.add(Box.createVerticalStrut(8));
        this.add(createClearCacheButton(clientStatus));

        return this;
    }

    private JButton createClearCacheButton(ClientStatus clientStatus) {
        final JButton clearCache = new JButton();
        long folderSize = 0;

        if (clientStatus.client() != null) {
            final Path downloadsPath = getDownloadsPath(clientStatus);
            folderSize = FileUtils.getFolderSize(downloadsPath);
            clearCache.addActionListener(_ -> handleClearCache(this, clearCache, downloadsPath));
        }
        String size = folderSize == 0 ? "empty" : FileUtils.humanReadableSize(folderSize);
        clearCache.setEnabled(clientStatus.client() != null && folderSize > 0);
        clearCache.setText("Clear cache (%s)".formatted(size));
        return clearCache;
    }

    // TODO disable button while Installing/Repairing
    private void handleClearCache(OptionsPanel parent, JButton clearCache, Path downloadsPath) {
        final int result = show(parent);
        if (result == JOptionPane.OK_OPTION) {
            try {
                FileUtils.deleteFolder(downloadsPath);
                clearCache.setEnabled(false);
                clearCache.setText("Clear cache (empty)");
            } catch (Exception e) {
                logger.error("", e);
                showErrorMessageDialogSync("Error: " + e.getMessage());
            }
        }
    }

    private static Path getDownloadsPath(ClientStatus clientStatus) {
        return new Installation(clientStatus.client().path(), GUI.DEFAULT_SLUG).getDownloadsPath();
    }

    boolean useInMemoryHashing() {
        return inMemoryHashingOption.isSelected();
    }

    RunnerOptions getRunnerOptions() {
        return new RunnerOptions(null, null, false, mangoHudOption.isSelected());
    }

    public int show(Container parent) {
        final JPanel panel = new JPanel(new GridLayout(1, 1, 8, 8));

        panel.add(new JLabel("Delete all temporal downloads cache?"));

        return JOptionPane.showConfirmDialog(
            parent,
            panel,
            "Clear download cache",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
    }

}
