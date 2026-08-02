package org.mnm.gui;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;

class CheckboxOptionTest {

    private static final String SETTING_KEY = "test.option";
    private static final String TEST_TEXT = "Enable feature";

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldUseDefaultValueWhenSettingIsMissing(boolean defaultValue) {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of());
        CheckboxOption option = new CheckboxOption(TEST_TEXT, settingsStore, SETTING_KEY, defaultValue);

        assertThat(option.getText()).isEqualTo(TEST_TEXT);
        assertThat(option.isSelected()).isEqualTo(defaultValue);
    }

    @Test
    void shouldRestorePersistedValueInsteadOfDefault() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of(SETTING_KEY, "true"));
        CheckboxOption option = new CheckboxOption(TEST_TEXT, settingsStore, SETTING_KEY, true);

        assertThat(option.isSelected()).isTrue();
    }

    @Test
    void shouldDefaultToFalseWhenStoreValueIsInvalid() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of(SETTING_KEY, "not-a-boolean"));
        CheckboxOption option = new CheckboxOption(TEST_TEXT, settingsStore, SETTING_KEY, true);

        assertThat(option.isSelected()).isFalse();
    }

    @Test
    void shouldPersistSelectionWhenClicked() {
        SettingsStore settings = new InMemorySettingsStore(Map.of());
        CheckboxOption option = new CheckboxOption(TEST_TEXT, settings, SETTING_KEY, false);

        option.doClick();
        assertThat(settings.get(SETTING_KEY)).isEqualTo("true");

        option.doClick();
        assertThat(settings.get(SETTING_KEY)).isEqualTo("false");
    }

}
