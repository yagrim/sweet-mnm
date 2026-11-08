package org.mnm.client;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mnm.gui.InMemorySettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.gui.InMemorySettingsStore.CACHE_AUTO_CLEAR_KEY;
import static org.mnm.gui.InMemorySettingsStore.CACHE_RETENTION_DAYS_KEY;

class CacheHandlerTest {

    @Test
    void shouldInitDefaultsWhenOptionsAreNotSet() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());

        assertThat(new CacheHandler(settings).initDefaults()).isTrue();

        assertThat(settings.get(CACHE_AUTO_CLEAR_KEY)).isEqualTo("true");
        assertThat(settings.get(CACHE_RETENTION_DAYS_KEY)).isEqualTo("10");
    }

    @Test
    void shouldKeepRetentionDaysWhenOnlyAutoClearIsNotSet() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(CACHE_RETENTION_DAYS_KEY, "30"));

        assertThat(new CacheHandler(settings).initDefaults()).isTrue();

        assertThat(settings.get(CACHE_AUTO_CLEAR_KEY)).isEqualTo("true");
        assertThat(settings.get(CACHE_RETENTION_DAYS_KEY)).isEqualTo("30");
    }

    @Test
    void shouldNotInitDefaultsWhenOptionsAreSet() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of(CACHE_AUTO_CLEAR_KEY, "false"));

        assertThat(new CacheHandler(settings).initDefaults()).isFalse();

        assertThat(settings.get(CACHE_AUTO_CLEAR_KEY)).isEqualTo("false");
        assertThat(settings.get(CACHE_RETENTION_DAYS_KEY)).isNull();
    }

    @Test
    void shouldEnableAutoClearWhenOptionIsNotSet() {
        CacheHandler cacheHandler = new CacheHandler(new InMemorySettingsStore(Map.of()));

        assertThat(cacheHandler.isAutoClear()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReadAutoClear(boolean value) {
        CacheHandler cacheHandler = new CacheHandler(new InMemorySettingsStore(Map.of(CACHE_AUTO_CLEAR_KEY, Boolean.toString(value))));

        assertThat(cacheHandler.isAutoClear()).isEqualTo(value);
    }

    @Test
    void shouldSaveAutoClear() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        CacheHandler cacheHandler = new CacheHandler(settings);

        cacheHandler.saveAutoClear(false);
        assertThat(settings.get(CACHE_AUTO_CLEAR_KEY)).isEqualTo("false");
        assertThat(cacheHandler.isAutoClear()).isFalse();

        cacheHandler.saveAutoClear(true);
        assertThat(settings.get(CACHE_AUTO_CLEAR_KEY)).isEqualTo("true");
        assertThat(cacheHandler.isAutoClear()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 15, 30})
    void shouldReadValidRetentionDays(int days) {
        CacheHandler cacheHandler = new CacheHandler(new InMemorySettingsStore(Map.of(CACHE_RETENTION_DAYS_KEY, Integer.toString(days))));

        assertThat(cacheHandler.getRetentionDays()).isEqualTo(days);
    }

    @ParameterizedTest
    @ValueSource(strings = {"7", "0", "-5", "not-a-number", ""})
    void shouldUseDefaultRetentionDaysWhenValueIsInvalid(String value) {
        CacheHandler cacheHandler = new CacheHandler(new InMemorySettingsStore(Map.of(CACHE_RETENTION_DAYS_KEY, value)));

        assertThat(cacheHandler.getRetentionDays()).isEqualTo(10);
    }

    @Test
    void shouldUseDefaultRetentionDaysWhenOptionIsNotSet() {
        CacheHandler cacheHandler = new CacheHandler(new InMemorySettingsStore(Map.of()));

        assertThat(cacheHandler.getRetentionDays()).isEqualTo(10);
    }

    @Test
    void shouldSaveRetentionDays() {
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());
        CacheHandler cacheHandler = new CacheHandler(settings);

        cacheHandler.saveRetentionDays(15);

        assertThat(settings.get(CACHE_RETENTION_DAYS_KEY)).isEqualTo("15");
        assertThat(cacheHandler.getRetentionDays()).isEqualTo(15);
    }
}
