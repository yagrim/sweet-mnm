package org.mnm.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mnm.gui.InMemorySettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.gui.InMemorySettingsStore.CURSOR_SIZE_KEY;

class CursorSizeTest {

    @Test
    void shouldUseDefaultWhenNotSet() {
        assertThat(CursorSize.read(new InMemorySettingsStore(Map.of()))).isEqualTo(CursorSize.DEFAULT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "SCALE_125", "SCALE_175", "200%", ""})
    void shouldUseDefaultWhenValueIsInvalid(String value) {
        SettingsStore settings = new InMemorySettingsStore(Map.of(CURSOR_SIZE_KEY, value));

        assertThat(CursorSize.read(settings)).isEqualTo(CursorSize.DEFAULT);
    }

    @ParameterizedTest
    @EnumSource(CursorSize.class)
    void shouldSaveAndReadValue(CursorSize cursorSize) {
        SettingsStore settings = new InMemorySettingsStore(Map.of());

        CursorSize.save(settings, cursorSize);

        assertThat(settings.get(CURSOR_SIZE_KEY)).isEqualTo(cursorSize.name());
        assertThat(CursorSize.read(settings)).isEqualTo(cursorSize);
    }

    @Test
    void shouldMapScaleToLogPixels() {
        assertThat(CursorSize.DEFAULT.logPixels()).isNull();
        assertThat(CursorSize.SCALE_100.logPixels()).isEqualTo(96);
        assertThat(CursorSize.SCALE_150.logPixels()).isEqualTo(144);
        assertThat(CursorSize.SCALE_200.logPixels()).isEqualTo(0xc0);
    }

    @Test
    void shouldShowLabel() {
        assertThat(CursorSize.DEFAULT).hasToString("Default size");
        assertThat(CursorSize.SCALE_100).hasToString("100%");
        assertThat(CursorSize.SCALE_150).hasToString("150%");
        assertThat(CursorSize.SCALE_200).hasToString("200%");
    }
}
