package org.mnm.gui;

import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.config.ConfigDbSettingsStore;
import org.mnm.config.Client;
import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.SettingsStore.DEBUG_KEY;
import static org.mnm.config.SettingsStore.IN_MEMORY_HASHING_KEY;

class GeneralOptionsPanelTest {

    @Test
    void shouldRestorePersistedOptions() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            DEBUG_KEY, "true",
            IN_MEMORY_HASHING_KEY, "false"));

        GeneralOptionsPanel panel = panel(settings);

        assertThat(option(panel, "debugOption").isSelected()).isTrue();
        assertThat(option(panel, "inMemoryHashingOption").isSelected()).isFalse();
    }

    @Test
    void shouldPersistModifiedDebugAndHashingOptions() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        GeneralOptionsPanel panel = panel(settings);

        option(panel, "debugOption").doClick();
        option(panel, "inMemoryHashingOption").doClick();

        assertThat(settings.get(DEBUG_KEY)).isEqualTo("true");
        assertThat(settings.get(IN_MEMORY_HASHING_KEY)).isEqualTo("false");
    }

    @Test
    void shouldRestoreModifiedOptionsFromConfigDatabase(@TempDir Path tempDir) {
        Path database = tempDir.resolve("config.db");
        SettingsStore settings = new ConfigDbSettingsStore(() -> database);
        GeneralOptionsPanel panel = panel(settings);

        option(panel, "debugOption").doClick();
        option(panel, "inMemoryHashingOption").doClick();

        GeneralOptionsPanel restoredPanel = panel(settings);
        assertThat(option(restoredPanel, "debugOption").isSelected()).isTrue();
        assertThat(option(restoredPanel, "inMemoryHashingOption").isSelected()).isFalse();
    }

    @Test
    void shouldDisableClearCacheWhenNoClientIsAvailable() {
        GeneralOptionsPanel panel = panel(new InMemorySettingsStore(Map.of()));

        panel.refresh(null);

        JButton clearCacheButton = button(panel, "clearCache");
        assertThat(clearCacheButton.isEnabled()).isFalse();
        assertThat(clearCacheButton.getText()).isEqualTo("Clear cache (empty)");
    }

    @Test
    void shouldEnableClearCacheAndShowDownloadSizeWhenCacheContainsFiles(@TempDir Path tempDir) throws IOException {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        CredentialsHandler credentialsHandler = new CredentialsHandler(settings);
        credentialsHandler.saveStoreCredentials(true);
        GeneralOptionsPanel panel = new GeneralOptionsPanel(settings, credentialsHandler, new JPanel());
        Path downloadsPath = Files.createDirectories(tempDir.resolve("downloads"));
        Files.writeString(downloadsPath.resolve("cache"), "cache");
        Client client = new Client("test", "1.0.0", Client.Status.UPDATED, tempDir);

        panel.refresh(new ClientStatus(client, false, null));

        JButton clearCacheButton = button(panel, "clearCache");
        assertThat(clearCacheButton.isEnabled()).isTrue();
        assertThat(clearCacheButton.getText()).isEqualTo("Clear cache (5 B)");
        assertThat(button(panel, "deleteCredentials").isEnabled()).isTrue();
    }

    private static GeneralOptionsPanel panel(SettingsStore settings) {
        return new GeneralOptionsPanel(settings, new CredentialsHandler(settings), new JPanel());
    }

    private static JCheckBox option(GeneralOptionsPanel panel, String fieldName) {
        return (JCheckBox) ReflectionTestTools.get(panel, fieldName);
    }

    private static JButton button(GeneralOptionsPanel panel, String fieldName) {
        return (JButton) ReflectionTestTools.get(panel, fieldName);
    }

}
