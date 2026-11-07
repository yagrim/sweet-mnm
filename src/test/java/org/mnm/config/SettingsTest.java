package org.mnm.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.mnm.gui.InMemorySettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.SettingsStore.CACHE_AUTO_CLEAR_KEY;
import static org.mnm.config.SettingsStore.CACHE_RETENTION_DAYS_KEY;

class SettingsTest {

    @Test
    void shouldInitCacheAutoClearDefaultsWhenOptionsAreNotSet() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());

        assertThat(Settings.initCacheAutoClearDefaults(settings)).isTrue();

        assertThat(settings.get(CACHE_AUTO_CLEAR_KEY)).isEqualTo("true");
        assertThat(settings.get(CACHE_RETENTION_DAYS_KEY)).isEqualTo("10");
    }

    @Test
    void shouldKeepRetentionDaysWhenOnlyAutoClearIsNotSet() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(CACHE_RETENTION_DAYS_KEY, "30"));

        assertThat(Settings.initCacheAutoClearDefaults(settings)).isTrue();

        assertThat(settings.get(CACHE_AUTO_CLEAR_KEY)).isEqualTo("true");
        assertThat(settings.get(CACHE_RETENTION_DAYS_KEY)).isEqualTo("30");
    }

    @Test
    void shouldNotInitCacheAutoClearDefaultsWhenOptionsAreSet() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(CACHE_AUTO_CLEAR_KEY, "false"));

        assertThat(Settings.initCacheAutoClearDefaults(settings)).isFalse();

        assertThat(settings.get(CACHE_AUTO_CLEAR_KEY)).isEqualTo("false");
        assertThat(settings.get(CACHE_RETENTION_DAYS_KEY)).isNull();
    }
}
