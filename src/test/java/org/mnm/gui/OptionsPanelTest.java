package org.mnm.gui;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.mnm.client.CacheHandler;
import org.mnm.client.RunnerOptions;
import org.mnm.config.CursorSize;
import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_GAMEID;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_PROTONPATH;

class OptionsPanelTest {

    @Test
    void shouldReturnDefaultRunnerOptions() {
        SettingsStore settings = new InMemorySettingsStore(Map.of());
        OptionsPanel panel = new OptionsPanel(null, settings, new CredentialsHandler(settings), new CacheHandler(settings));

        RunnerOptions options = panel.getRunnerOptions("other-game");

        var expectedUmuOptions = new RunnerOptions.UmuOptions(DEFAULT_UMU_GAMEID, DEFAULT_UMU_PROTONPATH, "");
        var expectedProtonOptions = new RunnerOptions.ProtonOptions(false, false);
        var expectedLinuxOptions = new RunnerOptions.LinuxOptions(false, true, expectedUmuOptions, expectedProtonOptions, new RunnerOptions.WineOptions(CursorSize.DEFAULT));
        assertThat(options).isEqualTo(new RunnerOptions("other-game", null, true, expectedLinuxOptions));
    }

    @Test
    void shouldReturnCursorSizeInRunnerOptions() {
        SettingsStore settings = new InMemorySettingsStore(Map.of(InMemorySettingsStore.CURSOR_SIZE_KEY, "SCALE_150"));
        OptionsPanel panel = new OptionsPanel(null, settings, new CredentialsHandler(settings), new CacheHandler(settings));

        RunnerOptions options = panel.getRunnerOptions("mnm");

        assertThat(options.linuxOptions().wineOptions().cursorSize()).isEqualTo(CursorSize.SCALE_150);
    }
}
