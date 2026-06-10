package org.mnm.gui;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.client.Installation;
import org.mnm.client.RunnerOptions;
import org.mnm.config.Client;
import org.mnm.config.OS;
import org.mnm.tools.FileUtils;

import static org.mnm.GeneralOptions.toggleDebug;
import static org.mnm.gui.MessageWindow.showErrorMessageDialogSync;

// NOTE: so far options can be grouped as repair or run.
class OptionsPanel extends JPanel implements RepairListener {

    private static final Logger logger = LoggerFactory.getLogger(OptionsPanel.class);

    private final JCheckBox debugOption = new JCheckBox("Enable debug");
    private final JCheckBox inMemoryHashingOption = new JCheckBox("In-memory hashing");
    private final JCheckBox mangoHudOption = new JCheckBox("Enable MangoHud");

    private JButton clearCache;

    OptionsPanel(ClientStatus clientStatus) {
        super();
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

        initializeClearCache(new JButton(), clientStatus);

        this.add(debugOption);
        this.add(Box.createVerticalStrut(8));
        this.add(inMemoryHashingOption);
        this.add(Box.createVerticalStrut(8));
        this.add(mangoHudOption);
        this.add(Box.createVerticalStrut(8));
        this.add(clearCache);

    }

    private void initializeClearCache(JButton button, ClientStatus clientStatus) {
        clearCache = button;
        update(clientStatus.client());
    }

    // TODO disable button while Installing/Repairing
    private void handleClearCache(OptionsPanel parent, JButton clearCache, Path downloadsPath) {
        final int result = showClearCacheConfirmation(parent);
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

    private static Path getDownloadsSize(Client client) {
        return new Installation(client.path(), GUI.DEFAULT_SLUG).getDownloadsPath();
    }

    boolean useInMemoryHashing() {
        return inMemoryHashingOption.isSelected();
    }

    RunnerOptions getRunnerOptions() {
        return new RunnerOptions(null, null, false, mangoHudOption.isSelected());
    }

    public int showClearCacheConfirmation(Container parent) {
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

    public void update(Client client) {
        long folderSize = 0;
        if (client != null) {
            final Path downloadsPath = getDownloadsSize(client);
            folderSize = FileUtils.getFolderSize(downloadsPath);
            clearCache.addActionListener(_ -> handleClearCache(this, clearCache, downloadsPath));
        }
        String size = folderSize == 0 ? "empty" : FileUtils.humanReadableSize(folderSize);
        clearCache.setEnabled(client != null && folderSize > 0);
        clearCache.setText("Clear cache (%s)".formatted(size));
    }

    @Override
    public void repairStart() {
        clearCache.setEnabled(false);
    }

    @Override
    public void repairDone(Client client) {
        update(client);
    }
}
