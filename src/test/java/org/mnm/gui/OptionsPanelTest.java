package org.mnm.gui;

import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.mnm.cache.CacheHandler;
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

    @Test
    void shouldShowSweetLogsBelowGeneralAndLinuxPanels() {
        SettingsStore settings = new InMemorySettingsStore(Map.of());
        OptionsPanel panel = new OptionsPanel(null, settings, new CredentialsHandler(settings), new CacheHandler(settings));
        panel.setSize(900, 700);
        panel.doLayout();
        panel.getComponent(0).doLayout();

        Container columns = (Container) panel.getComponent(0);
        Component sweetLogs = Arrays.stream(panel.getComponents())
            .filter(SweetLogsPanel.class::isInstance)
            .findFirst()
            .orElseThrow();

        assertThat(columns.getComponentCount()).isEqualTo(2);
        assertThat(sweetLogs.getY()).isGreaterThanOrEqualTo(columns.getY() + columns.getHeight());
    }
}
