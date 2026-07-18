package org.mnm.gui;

import java.util.HashMap;
import java.util.Map;
import java.nio.file.Path;

import javax.swing.JCheckBox;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.config.ConfigDbSettingsStore;
import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;

class OptionsPanelTest {

    @Test
    void shouldRestorePersistedOptions() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(
            OptionsPanel.DEBUG_KEY, "true",
            OptionsPanel.IN_MEMORY_HASHING_KEY, "false",
            OptionsPanel.MANGOHUD_KEY, "true"));

        OptionsPanel panel = new OptionsPanel(settings);

        assertThat(optionAt(panel, 0).isSelected()).isTrue();
        assertThat(optionAt(panel, 1).isSelected()).isFalse();
        assertThat(optionAt(panel, 2).isSelected()).isTrue();
    }

    @Test
    void shouldPersistModifiedDebugAndHashingOptions() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        OptionsPanel panel = new OptionsPanel(settings);

        optionAt(panel, 0).doClick();
        optionAt(panel, 1).doClick();

        assertThat(settings.get(OptionsPanel.DEBUG_KEY)).isEqualTo("true");
        assertThat(settings.get(OptionsPanel.IN_MEMORY_HASHING_KEY)).isEqualTo("false");
    }

    @Test
    void shouldRestoreModifiedOptionsFromConfigDatabase(@TempDir Path tempDir) {
        Path database = tempDir.resolve("config.db");
        SettingsStore settings = new ConfigDbSettingsStore(() -> database);
        OptionsPanel panel = new OptionsPanel(settings);

        optionAt(panel, 0).doClick();
        optionAt(panel, 1).doClick();

        OptionsPanel restoredPanel = new OptionsPanel(settings);
        assertThat(optionAt(restoredPanel, 0).isSelected()).isTrue();
        assertThat(optionAt(restoredPanel, 1).isSelected()).isFalse();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldPersistModifiedMangoHudOption() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        OptionsPanel panel = new OptionsPanel(settings);

        optionAt(panel, 2).doClick();

        assertThat(settings.get(OptionsPanel.MANGOHUD_KEY)).isEqualTo("true");
    }

    private static JCheckBox optionAt(OptionsPanel panel, int index) {
        return (JCheckBox) panel.getComponent(index * 2);
    }

    private static class InMemorySettingsStore implements SettingsStore {

        private final Map<String, String> values;

        InMemorySettingsStore(Map<String, String> values) {
            this.values = new HashMap<>(values);
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public void put(String key, String value) {
            values.put(key, value);
        }
    }
}
