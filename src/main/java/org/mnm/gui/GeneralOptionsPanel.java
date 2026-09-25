package org.mnm.gui;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.LoggerHandler;
import org.mnm.client.Installation;
import org.mnm.config.Client;
import org.mnm.config.OS;
import org.mnm.config.SettingsStore;
import org.mnm.events.ClientEventHandler;
import org.mnm.events.Refreshable;
import org.mnm.events.RepairListener;
import org.mnm.tools.FileUtils;

import static org.mnm.config.Environment.NATIVE_IMAGE;
import static org.mnm.config.Settings.DEFAULT_FONT_SCALING;
import static org.mnm.config.Settings.MAX_FONT_SCALING;
import static org.mnm.config.Settings.readUiScaling;
import static org.mnm.config.SettingsStore.DEBUG_KEY;

import static org.mnm.config.SettingsStore.HIDE_CREDENTIALS;
import static org.mnm.config.SettingsStore.IN_MEMORY_HASHING_KEY;
import static org.mnm.config.SettingsStore.OPTIONS_FONT_SCALING_KEY;
import static org.mnm.config.SettingsStore.SKIP_UI_WARNINGS;
import static org.mnm.gui.MessageDialog.showErrorMessageDialogSync;
import static org.mnm.gui.Style.SCALE;

public class GeneralOptionsPanel extends BaseOptionsPanel
    implements RepairListener, Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(GeneralOptionsPanel.class);

    private final CheckboxOption debugOption;
    private final JCheckBox inMemoryHashingOption;
    private final JCheckBox hideCredentials;

    private final JButton deleteCredentials = new JButton("Delete login information");
    private final JButton clearCache = new JButton("Clear cache");
    private final JLabel uiScalingLabel = new JLabel("Font Scaling");
    private final JComboBox<Float> uiScalingSelector = new JComboBox<>();
    private final JPanel uiScalingPanel = new JPanel();

    private final CredentialsHandler credentialsHandler;

    private final boolean skipUIWarnings;

    private ClientStatus clientStatus;


    public GeneralOptionsPanel(SettingsStore settingsStore, CredentialsHandler credentialsHandler, Container parent) {
        super("General");
        this.credentialsHandler = credentialsHandler;
        skipUIWarnings = settingsStore.getBoolean(SKIP_UI_WARNINGS, false);

        debugOption = new CheckboxOption("Enable debug", settingsStore, DEBUG_KEY, false);
        if (debugOption.isSelected()) {
            SwingUtilities.invokeLater(() -> {
                LoggerHandler.setDebug(true);
                initConsoleWindow(true);
            });
        }
        debugOption.addActionListener(_ -> {
            boolean selected = debugOption.isSelected();
            initConsoleWindow(selected);
            settingsStore.putBoolean(DEBUG_KEY, selected);
        });

        inMemoryHashingOption = new CheckboxOption("In-memory hashing", settingsStore, IN_MEMORY_HASHING_KEY, true);
        inMemoryHashingOption.setActionCommand("in-memory-hashing");
        inMemoryHashingOption.addActionListener(_ ->
            settingsStore.putBoolean(IN_MEMORY_HASHING_KEY, inMemoryHashingOption.isSelected()));

        clearCache.addActionListener(_ -> handleClearCache(parent, clearCache));

        hideCredentials = new CheckboxOption("Hide credentials", settingsStore, HIDE_CREDENTIALS, false);
        hideCredentials.setToolTipText("aka. Streamer mode");

        deleteCredentials.setEnabled(credentialsHandler.getStoreCredentials());
        deleteCredentials.addActionListener(_ -> handleClearCredentials(parent));

        for (float i = DEFAULT_FONT_SCALING; i <= MAX_FONT_SCALING; i += 0.5) {
            uiScalingSelector.addItem(i);
        }
        uiScalingSelector.setSelectedItem(readUiScaling(settingsStore));
        // triggers only when value changes
        uiScalingSelector.addItemListener(evt -> {
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                Float scale = (Float) uiScalingSelector.getSelectedItem();
                settingsStore.putFloat(OPTIONS_FONT_SCALING_KEY, scale);
                if (!skipUIWarnings) {
                    MessageDialog.showInfoMessageDialogSync("Close and relaunch Sweet to update UI", scale);
                }
            }
        });

        uiScalingPanel.add(uiScalingLabel);
        uiScalingPanel.add(uiScalingSelector);
        // must go after adding the label and selector:
        uiScalingPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        uiScalingPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        this.add(debugOption);
        this.add(inMemoryHashingOption);
        this.add(clearCache);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(hideCredentials);
        this.add(deleteCredentials);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(uiScalingPanel);
        this.add(Box.createVerticalStrut(SCALE));

        // post-init
        ClientEventHandler.getInstance().register(this);
    }

    private static void initConsoleWindow(boolean selected) {
        if (selected && NATIVE_IMAGE && OS.isWindows()) {
            ConsoleAllocator.allocConsole();
        }
        LoggerHandler.setDebug(selected);
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

    boolean isInMemoryHashing() {
        return inMemoryHashingOption.isSelected();
    }

    private static Path getDownloadsPath(Client client) {
        return new Installation(client.path(), MainTabs.DEFAULT_SLUG).getDownloadsPath();
    }

    // This is quick enough, we don't bother running async and disabling button in the meantime
    private void handleClearCache(Container parent, JButton clearCache) {
        final int result = ConfirmationDialog.showConfirmationDialog(parent, "Clear download cache", "Delete all temporal downloads cache?");
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

    private void handleClearCredentials(Container parent) {
        final int result = ConfirmationDialog.showConfirmationDialog(parent, "Delete login information", "Delete stored email and password?");
        if (result == JOptionPane.OK_OPTION) {
            credentialsHandler.clearCredentials();
            ClientEventHandler.getInstance().refresh(clientStatus);
        }
    }

}
