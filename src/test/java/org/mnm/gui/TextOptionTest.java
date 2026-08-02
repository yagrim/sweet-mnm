package org.mnm.gui;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;

class TextOptionTest {

    private static final String SETTING_KEY = "test.text-option";
    private final String GAME_ID = "Game ID";


    @Test
    void shouldSetLabel() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of());
        TextOption option = new TextOption(GAME_ID, settingsStore, SETTING_KEY, "my-mnm");

        assertThat(((JLabel) option.getComponent(0)).getText()).isEqualTo(GAME_ID);
    }

    @Test
    void shouldUseDefaultValueWhenSettingIsMissing() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of());
        TextOption option = new TextOption(GAME_ID, settingsStore, SETTING_KEY, "my-mnm");

        assertThat(option.getText()).isEqualTo("my-mnm");
    }

    @Test
    void shouldRestorePersistedValueInsteadOfDefault() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of(SETTING_KEY, "custom-id"));
        TextOption option = new TextOption(GAME_ID, settingsStore, SETTING_KEY, "mnm");

        assertThat(option.getText()).isEqualTo("custom-id");
    }

    @Test
    void shouldUseEmptyTextWhenNoDefaultValueIsProvided() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of());
        TextOption option = new TextOption(GAME_ID, settingsStore, SETTING_KEY);

        assertThat(option.getText()).isEmpty();
    }

    @Test
    void shouldPersistEditedTextAfterWaitTime() throws InterruptedException {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        TextOption option = new TextOption(GAME_ID, settings, SETTING_KEY);

        textField(option).setText("custom-id");

        assertThat(settings.get(SETTING_KEY)).isNull();
        waitForSetting(settings, "custom-id");
        assertThat(settings.get(SETTING_KEY)).isEqualTo("custom-id");
    }

    @Test
    void shouldEnableAndDisableItsTextField() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of());
        TextOption option = new TextOption(GAME_ID, settingsStore, SETTING_KEY);

        option.setEnabled(false);
        assertThat(textField(option).isEnabled()).isFalse();

        option.setEnabled(true);
        assertThat(textField(option).isEnabled()).isTrue();
    }

    private static JTextField textField(TextOption option) {
        return (JTextField) option.getComponent(1);
    }

    private static void waitForSetting(InMemorySettingsStore settings, String expectedValue) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000_000L;
        while (!expectedValue.equals(settings.get(SETTING_KEY)) && System.currentTimeMillis() < deadline) {
            Thread.sleep(150);
        }
    }

}
