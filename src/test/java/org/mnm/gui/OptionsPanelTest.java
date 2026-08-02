package org.mnm.gui;

import javax.swing.JCheckBox;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.config.ConfigDbSettingsStore;
import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.SettingsStore.DEBUG_KEY;
import static org.mnm.config.SettingsStore.IN_MEMORY_HASHING_KEY;
import static org.mnm.config.SettingsStore.MANGOHUD_KEY;

class OptionsPanelTest {

    @Test
    void shouldRestorePersistedOptions() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            DEBUG_KEY, "true",
            IN_MEMORY_HASHING_KEY, "false",
            MANGOHUD_KEY, "true"));

        OptionsPanel panel = new OptionsPanel(settings, new CredentialsHandler(settings));

        assertThat(option(panel, "debugOption").isSelected()).isTrue();
        assertThat(option(panel, "inMemoryHashingOption").isSelected()).isFalse();
        assertThat(linuxOption(panel, "mangoHudOption").isSelected()).isTrue();
    }

    @Test
    void shouldPersistModifiedDebugAndHashingOptions() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        OptionsPanel panel = new OptionsPanel(settings, new CredentialsHandler(settings));

        option(panel, "debugOption").doClick();
        option(panel, "inMemoryHashingOption").doClick();

        assertThat(settings.get(DEBUG_KEY)).isEqualTo("true");
        assertThat(settings.get(IN_MEMORY_HASHING_KEY)).isEqualTo("false");
    }

    @Test
    void shouldRestoreModifiedOptionsFromConfigDatabase(@TempDir Path tempDir) {
        Path database = tempDir.resolve("config.db");
        SettingsStore settings = new ConfigDbSettingsStore(() -> database);
        OptionsPanel panel = new OptionsPanel(settings, new CredentialsHandler(settings));

        option(panel, "debugOption").doClick();
        option(panel, "inMemoryHashingOption").doClick();

        OptionsPanel restoredPanel = new OptionsPanel(settings, new CredentialsHandler(settings));
        assertThat(option(restoredPanel, "debugOption").isSelected()).isTrue();
        assertThat(option(restoredPanel, "inMemoryHashingOption").isSelected()).isFalse();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldPersistModifiedMangoHudOption() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        OptionsPanel panel = new OptionsPanel(settings, new CredentialsHandler(settings));

        linuxOption(panel, "mangoHudOption").doClick();

        assertThat(settings.get(MANGOHUD_KEY)).isEqualTo("true");
    }

    private static JCheckBox option(OptionsPanel panel, String fieldName) {
        return (JCheckBox) ReflectionTestTools.get(panel, fieldName);
    }

    private static JCheckBox linuxOption(OptionsPanel panel, String fieldName) {
        LinuxOptionsPanel linuxPanel = (LinuxOptionsPanel) ReflectionTestTools.get(panel, "linuxPanel");
        return (JCheckBox) ReflectionTestTools.get(linuxPanel, fieldName);
    }

}
