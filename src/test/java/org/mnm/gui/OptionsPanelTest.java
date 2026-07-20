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

        assertThat(optionAt(panel, 0).isSelected()).isTrue();
        assertThat(optionAt(panel, 1).isSelected()).isFalse();
        assertThat(optionAt(panel, 2).isSelected()).isTrue();
    }

    @Test
    void shouldPersistModifiedDebugAndHashingOptions() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        OptionsPanel panel = new OptionsPanel(settings, new CredentialsHandler(settings));

        optionAt(panel, 0).doClick();
        optionAt(panel, 1).doClick();

        assertThat(settings.get(DEBUG_KEY)).isEqualTo("true");
        assertThat(settings.get(IN_MEMORY_HASHING_KEY)).isEqualTo("false");
    }

    @Test
    void shouldRestoreModifiedOptionsFromConfigDatabase(@TempDir Path tempDir) {
        Path database = tempDir.resolve("config.db");
        SettingsStore settings = new ConfigDbSettingsStore(() -> database);
        OptionsPanel panel = new OptionsPanel(settings, new CredentialsHandler(settings));

        optionAt(panel, 0).doClick();
        optionAt(panel, 1).doClick();

        OptionsPanel restoredPanel = new OptionsPanel(settings, new CredentialsHandler(settings));
        assertThat(optionAt(restoredPanel, 0).isSelected()).isTrue();
        assertThat(optionAt(restoredPanel, 1).isSelected()).isFalse();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldPersistModifiedMangoHudOption() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        OptionsPanel panel = new OptionsPanel(settings, new CredentialsHandler(settings));

        optionAt(panel, 2).doClick();

        assertThat(settings.get(MANGOHUD_KEY)).isEqualTo("true");
    }

    private static JCheckBox optionAt(OptionsPanel panel, int index) {
        return (JCheckBox) panel.getComponent(index * 2);
    }

}
