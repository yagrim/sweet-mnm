package org.mnm.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.mnm.gui.InMemorySettingsStore;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsStoreTest {

    private static final String OPTION1_KEY = "option-1";
    private static final String OPTION2_KEY = "option-2";

    private static final String OPTION1_VALUE = "value-1";
    private static final String OPTION2_VALUE = "value-2";

    @Test
    void shouldGetStringValueOrDefault() {
        Map<String, String> values = Map.of(OPTION1_KEY, OPTION1_VALUE, OPTION2_KEY, OPTION2_VALUE);
        SettingsStore settingsStore = new InMemorySettingsStore(values);

        assertThat(settingsStore.get(OPTION1_KEY)).isEqualTo(OPTION1_VALUE);
        assertThat(settingsStore.get(OPTION1_KEY, "something")).isEqualTo(OPTION1_VALUE);
        assertThat(settingsStore.get("not-present", "something")).isEqualTo("something");
    }

    @Test
    void shouldGetBooleanValueOrDefault() {
        Map<String, String> values = Map.of(OPTION1_KEY, "true", OPTION2_KEY, "false");
        SettingsStore settingsStore = new InMemorySettingsStore(values);

        assertThat(settingsStore.getBoolean(OPTION1_KEY, false)).isTrue();
        assertThat(settingsStore.getBoolean(OPTION2_KEY, true)).isFalse();
        assertThat(settingsStore.getBoolean("not-present", true)).isTrue();
        assertThat(settingsStore.getBoolean("not-present", false)).isFalse();
    }
}
