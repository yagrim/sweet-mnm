package org.mnm.gui;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.mnm.LoggerHandler;
import org.mnm.client.CacheHandler;
import org.mnm.client.Installation;
import org.mnm.config.Client;
import org.mnm.config.OS;
import org.mnm.config.SettingsStore;
import org.mnm.events.CacheListener;
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
import static org.mnm.gui.Style.SCALE;

public class GeneralOptionsPanel extends BaseOptionsPanel
    implements RepairListener, Refreshable, CacheListener {

    private final CheckboxOption debugOption;
    private final JCheckBox inMemoryHashingOption;
    private final JCheckBox hideCredentials;
    private final CheckboxOption autoClearCacheOption;

    private final JButton deleteCredentials = new JButton("Delete login information");
    private final JButton clearCache = new JButton("Clear cache");
    private final ComboBoxOption<Float> uiScalingSelector;
    private final ComboBoxOption<Integer> cacheRetentionSelector;

    private final CredentialsHandler credentialsHandler;

    private final boolean skipUIWarnings;

    private ClientStatus clientStatus;


    public GeneralOptionsPanel(SettingsStore settingsStore, CredentialsHandler credentialsHandler,
                               CacheHandler cacheHandler, Container parent) {
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

        autoClearCacheOption = new CheckboxOption("Auto-clear cache", cacheHandler.isAutoClear(), cacheHandler::saveAutoClear);
        autoClearCacheOption.setToolTipText("Delete old cached files on startup");

        cacheRetentionSelector = new ComboBoxOption<>("Delete cached files after (days)",
            CacheHandler.RETENTION_DAYS,
            cacheHandler.getRetentionDays(),
            cacheHandler::saveRetentionDays);
        cacheRetentionSelector.setEnabled(autoClearCacheOption.isSelected());
        autoClearCacheOption.addActionListener(_ -> cacheRetentionSelector.setEnabled(autoClearCacheOption.isSelected()));

        hideCredentials = new CheckboxOption("Hide credentials", settingsStore, HIDE_CREDENTIALS, false);
        hideCredentials.setToolTipText("aka. Streamer mode");

        deleteCredentials.setEnabled(credentialsHandler.isStoreCredentials());
        deleteCredentials.addActionListener(_ -> handleClearCredentials(parent));

        uiScalingSelector = new ComboBoxOption<>("Font Scaling",
            uiScalingAvailableValues(),
            readUiScaling(settingsStore),
            scale -> {
                settingsStore.putFloat(OPTIONS_FONT_SCALING_KEY, scale);
                if (!skipUIWarnings) {
                    MessageDialog.showInfoMessageDialogSync("Close and relaunch Sweet to update UI", scale);
                }
            });

        this.add(debugOption);
        this.add(inMemoryHashingOption);
        this.add(hideCredentials);
        this.add(deleteCredentials);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(uiScalingSelector);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(clearCache);
        this.add(autoClearCacheOption);
        this.add(cacheRetentionSelector);
        this.add(Box.createVerticalStrut(SCALE));

        // post-init
        ClientEventHandler.getInstance().register(this);
    }

    private static List<Float> uiScalingAvailableValues() {
        List<Float> uiScalingValues = new ArrayList<>();
        for (float i = DEFAULT_FONT_SCALING; i <= MAX_FONT_SCALING; i += 0.5f) {
            uiScalingValues.add(i);
        }
        return uiScalingValues;
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
    public void cacheCleared() {
        refreshClearCache();
    }

    @Override
    public void refresh(ClientStatus clientStatus) {
        this.clientStatus = clientStatus;
        refreshClearCache();
        deleteCredentials.setEnabled(credentialsHandler.isStoreCredentials());
    }

    private void refreshClearCache() {
        long folderSize = 0;
        if (clientStatus != null && clientStatus.client() != null) {
            final Path downloadsPath = getDownloadsPath(clientStatus.client());
            folderSize = FileUtils.getFolderSize(downloadsPath);
        }
        String size = folderSize == 0 ? "empty" : FileUtils.humanReadableSize(folderSize);
        clearCache.setEnabled(clientStatus != null && folderSize > 0);
        clearCache.setText("Clear cache (%s)".formatted(size));
    }

    boolean isInMemoryHashing() {
        return inMemoryHashingOption.isSelected();
    }

    private static Path getDownloadsPath(Client client) {
        return new Installation(client.path(), client.slug()).getDownloadsPath();
    }

    // This is quick enough, we don't bother running async and disabling button in the meantime
    private void handleClearCache(Container parent, JButton clearCache) {
        final int result = ConfirmationDialog.showConfirmationDialog(parent, "Clear download cache", "Delete all temporal downloads cache?");
        if (result == JOptionPane.OK_OPTION) {
            try {
                final Path downloadsPath = getDownloadsPath(clientStatus.client());
                FileUtils.deleteFolder(downloadsPath);
                clearCache.setText("Clear cache (empty)");
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            } finally {
                refresh(clientStatus);
            }
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
