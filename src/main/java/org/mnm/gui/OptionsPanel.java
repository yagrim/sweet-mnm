package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
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
import static org.mnm.config.SettingsStore.DEBUG_KEY;
import static org.mnm.config.SettingsStore.IN_MEMORY_HASHING_KEY;
import static org.mnm.gui.ClientPanel.SCALE;
import static org.mnm.gui.MainTabs.DEFAULT_SLUG;
import static org.mnm.gui.MessageWindow.showErrorMessageDialogSync;

class OptionsPanel extends JPanel
    implements RepairListener, Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(OptionsPanel.class);

    private final JCheckBox debugOption = new JCheckBox("Enable debug");
    private final JCheckBox inMemoryHashingOption = new JCheckBox("In-memory hashing");

    private final JButton deleteCredentials = new JButton("Delete login information");
    private final JButton clearCache = new JButton("Clear cache");

    private final LinuxOptionsPanel linuxPanel;

    private final SettingsStore settingsStore;
    private final CredentialsHandler credentialsHandler;

    private ClientStatus clientStatus;

    OptionsPanel(SettingsStore settingsStore, CredentialsHandler credentialsHandler) {
        super();
        this.settingsStore = settingsStore;
        this.credentialsHandler = credentialsHandler;
        loadSettings();
        this.setLayout(new GridLayout(1, 2, 10, 0));
        this.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));

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

        clearCache.addActionListener(_ -> handleClearCache(this, clearCache));

        final JPanel left = new GeneralOptionsPanel();
        left.add(debugOption);
        left.add(Box.createVerticalStrut(SCALE));
        left.add(inMemoryHashingOption);
        left.add(Box.createVerticalStrut(SCALE));
        left.add(clearCache);
        left.add(Box.createVerticalStrut(SCALE));

        deleteCredentials.setEnabled(false);
        deleteCredentials.addActionListener(_ -> handleClearCredentials(this));
        left.add(deleteCredentials);

        linuxPanel = new LinuxOptionsPanel(settingsStore);

        this.add(left);
        this.add(linuxPanel);

        // post-init
        ClientEventHandler.getInstance().register(this);
    }

    private void loadSettings() {
        boolean debugEnabled = settingsStore.getBoolean(DEBUG_KEY, false);
        if (debugEnabled) {
            debugOption.setSelected(true);
            GeneralOptions.setDebug(true);
        }
        inMemoryHashingOption.setSelected(settingsStore.getBoolean(IN_MEMORY_HASHING_KEY, true));

        deleteCredentials.setEnabled(credentialsHandler.getStoreCredentials());
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
        deleteCredentials.setEnabled(credentialsHandler.getStoreCredentials());
    }

    private static Path getDownloadsPath(Client client) {
        return new Installation(client.path(), MainTabs.DEFAULT_SLUG).getDownloadsPath();
    }

    // This is quick enough, we don't bother running async and disabling button in the meantime
    private void handleClearCache(OptionsPanel parent, JButton clearCache) {
        final int result = showConfirmationWindow(parent, "Clear download cache", "Delete all temporal downloads cache?");
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

    private void handleClearCredentials(OptionsPanel parent) {
        final int result = showConfirmationWindow(parent, "Delete login information", "Delete stored email and password?");
        if (result == JOptionPane.OK_OPTION) {
            credentialsHandler.clearCredentials();
            ClientEventHandler.getInstance().refresh(clientStatus);
        }
    }

    boolean useInMemoryHashing() {
        return inMemoryHashingOption.isSelected();
    }

    RunnerOptions getRunnerOptions() {
        return new RunnerOptions(DEFAULT_SLUG, null, false,
            new RunnerOptions.LinuxOptions(linuxPanel.isMangoHudEnabled(), linuxPanel.isUseClientAsPrefix(),
                new RunnerOptions.UmuOptions(linuxPanel.getUmuGameId(), linuxPanel.getUmuProtonPath(), linuxPanel.getUmuWinePrefix()))
        );
    }

    public static int showConfirmationWindow(Container parent, String title, String message) {
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
