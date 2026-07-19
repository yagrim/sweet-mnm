package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Container;
import java.awt.GridLayout;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.GeneralOptions;
import org.mnm.client.Installation;
import org.mnm.client.RunnerOptions;
import org.mnm.config.Client;
import org.mnm.config.OS;
import org.mnm.config.SettingsStore;
import org.mnm.events.ClientEventHandler;
import org.mnm.events.Refreshable;
import org.mnm.events.RepairListener;
import org.mnm.tools.FileUtils;

import static org.mnm.config.Environment.NATIVE_IMAGE;
import static org.mnm.gui.ClientPanel.SCALE;
import static org.mnm.gui.MainTabs.DEFAULT_SLUG;
import static org.mnm.gui.MessageWindow.showErrorMessageDialogSync;

// NOTE: so far options can be grouped as repair or run.
class OptionsPanel extends JPanel
    implements RepairListener, Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(OptionsPanel.class);

    static final String DEBUG_KEY = "debug";
    static final String IN_MEMORY_HASHING_KEY = "in-memory-hashing";
    static final String MANGOHUD_KEY = "linux.mangohud";

    private final JCheckBox debugOption = new JCheckBox("Enable debug");
    private final JCheckBox inMemoryHashingOption = new JCheckBox("In-memory hashing");
    private final JCheckBox mangoHudOption = new JCheckBox("Enable MangoHud");

    private final JButton clearCache;

    private ClientStatus clientStatus;

    OptionsPanel(SettingsStore settingsStore) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));

        loadSettings(settingsStore);

        debugOption.setActionCommand("debug");
        debugOption.addActionListener(_ -> {
            boolean selected = debugOption.isSelected();
            if (selected && NATIVE_IMAGE && OS.isWindows()) {
                ConsoleAllocator.allocConsole();
            }
            GeneralOptions.setDebug(selected);
            settingsStore.putBoolean(DEBUG_KEY, selected);
        });

        inMemoryHashingOption.setActionCommand("in-memory-hashing");
        inMemoryHashingOption.addActionListener(_ ->
            settingsStore.putBoolean(IN_MEMORY_HASHING_KEY, inMemoryHashingOption.isSelected()));

        mangoHudOption.setActionCommand("mangohud");
        mangoHudOption.addActionListener(_ ->
            settingsStore.putBoolean(MANGOHUD_KEY, mangoHudOption.isSelected()));
        if (OS.isWindows()) {
            mangoHudOption.setEnabled(false);
            mangoHudOption.setText("Enable MangoHud (Linux only)");
        }

        clearCache = new JButton("Clear cache");
        clearCache.addActionListener(_ -> handleClearCache(this, clearCache));

        this.add(debugOption);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(inMemoryHashingOption);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(mangoHudOption);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(clearCache);

        ClientEventHandler.getInstance().register(this);
    }

    private void loadSettings(SettingsStore settingsStore) {
        boolean debugEnabled = settingsStore.getBoolean(DEBUG_KEY, false);
        if (debugEnabled) {
            debugOption.setSelected(true);
            GeneralOptions.setDebug(true);
        }
        inMemoryHashingOption.setSelected(settingsStore.getBoolean(IN_MEMORY_HASHING_KEY, true));
        mangoHudOption.setSelected(settingsStore.getBoolean(MANGOHUD_KEY, false));
    }

    @Override
    public void repairStart() {
        clearCache.setEnabled(false);
    }

    @Override
    public void repairDone(ClientStatus client) {
        refresh(client);
    }

    @Override
    public void refresh(ClientStatus clientStatus) {
        this.clientStatus = clientStatus;

        long folderSize = 0;
        if (clientStatus != null && clientStatus.client() != null) {
            final Path downloadsPath = getDownloadsPath(clientStatus.client());
            folderSize = FileUtils.getFolderSize(downloadsPath);
        }
        String size = folderSize == 0 ? "empty" : FileUtils.humanReadableSize(folderSize);
        clearCache.setEnabled(clientStatus != null && folderSize > 0);
        clearCache.setText("Clear cache (%s)".formatted(size));
    }

    private static Path getDownloadsPath(Client client) {
        return new Installation(client.path(), MainTabs.DEFAULT_SLUG).getDownloadsPath();
    }

    // This is quick enough, we don't bother running async and disabling button in the meantime
    private void handleClearCache(OptionsPanel parent, JButton clearCache) {
        final int result = showClearCacheConfirmation(parent);
        if (result == JOptionPane.OK_OPTION) {
            try {
                final Path downloadsPath = getDownloadsPath(clientStatus.client());
                FileUtils.deleteFolder(downloadsPath);
                clearCache.setText("Clear cache (empty)");
            } catch (Exception e) {
                logger.error("", e);
                showErrorMessageDialogSync("Error: " + e.getMessage());
            }
            refresh(clientStatus);
        }
    }

    boolean useInMemoryHashing() {
        return inMemoryHashingOption.isSelected();
    }

    RunnerOptions getRunnerOptions() {
        return new RunnerOptions(DEFAULT_SLUG, null, false, mangoHudOption.isSelected());
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
}
