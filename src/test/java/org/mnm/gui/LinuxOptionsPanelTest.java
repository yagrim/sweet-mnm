package org.mnm.gui;

import javax.swing.JCheckBox;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.config.OS;
import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_GAMEID;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.MANGOHUD_KEY;
import static org.mnm.config.SettingsStore.UMU_GAMEID;
import static org.mnm.config.SettingsStore.UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.UMU_USE_CLIENT_AS_PREFIX;
import static org.mnm.config.SettingsStore.UMU_WINEPREFIX;

class LinuxOptionsPanelTest {

    @Test
    void shouldRestorePersistedLinuxOptions() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of(
            MANGOHUD_KEY, "true",
            UMU_GAMEID, "custom-game",
            UMU_PROTONPATH, "/opt/proton",
            UMU_USE_CLIENT_AS_PREFIX, "false",
            UMU_WINEPREFIX, "/home/user/prefix"));
        LinuxOptionsPanel panel = new LinuxOptionsPanel(settingsStore);

        assertThat(panel.isMangoHudEnabled()).isTrue();
        assertThat(panel.getUmuGameId()).isEqualTo("custom-game");
        assertThat(panel.getUmuProtonPath()).isEqualTo("/opt/proton");
        assertThat(panel.isUseClientAsPrefix()).isFalse();
        assertThat(panel.getUmuWinePrefix()).isEqualTo("/home/user/prefix");

        assertThat(textOption(panel, "umuWinePrefix").isEnabled()).isTrue();
    }

    @Test
    void shouldUseLinuxOptionDefaults() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of());
        LinuxOptionsPanel panel = new LinuxOptionsPanel(settingsStore);

        assertThat(panel.isMangoHudEnabled()).isFalse();
        assertThat(panel.getUmuGameId()).isEqualTo(DEFAULT_UMU_GAMEID);
        assertThat(panel.getUmuProtonPath()).isEqualTo(DEFAULT_UMU_PROTONPATH);
        assertThat(panel.isUseClientAsPrefix()).isTrue();
        assertThat(panel.getUmuWinePrefix()).isEmpty();

        assertThat(textOption(panel, "umuWinePrefix").isEnabled()).isFalse();
    }

    @Test
    void shouldPersistMangoHudCheckboxOptions() {
        SettingsStore settings = new InMemorySettingsStore(Map.of());
        LinuxOptionsPanel panel = new LinuxOptionsPanel(settings);

        assertThat(settings.get(MANGOHUD_KEY)).isNull();
        checkbox(panel, "mangoHudOption").doClick();
        assertThat(settings.get(MANGOHUD_KEY)).isEqualTo("true");
    }

    @Test
    void shouldPersistUseClientAsPrefixCheckboxOption() {
        SettingsStore settings = new InMemorySettingsStore(Map.of());
        LinuxOptionsPanel panel = new LinuxOptionsPanel(settings);

        assertThat(settings.get(UMU_USE_CLIENT_AS_PREFIX)).isNull();
        checkbox(panel, "useClientAsPrefix").doClick();
        assertThat(settings.get(UMU_USE_CLIENT_AS_PREFIX)).isEqualTo("false");

        assertThat(textOption(panel, "umuWinePrefix").isEnabled()).isTrue();
    }

    @Test
    void shouldDisableAllLinuxControlsOnWindows() {
        try (MockedStatic<OS> os = Mockito.mockStatic(OS.class)) {
            os.when(OS::isWindows).thenReturn(true);

            LinuxOptionsPanel panel = new LinuxOptionsPanel(new InMemorySettingsStore(Map.of()));

            assertThat(checkbox(panel, "mangoHudOption").isEnabled()).isFalse();
            assertThat(textOption(panel, "umuGameId").isEnabled()).isFalse();
            assertThat(textOption(panel, "umuProtonPath").isEnabled()).isFalse();
            assertThat(checkbox(panel, "useClientAsPrefix").isEnabled()).isFalse();
            assertThat(textOption(panel, "umuWinePrefix").isEnabled()).isFalse();
        }
    }

    private static JCheckBox checkbox(LinuxOptionsPanel panel, String fieldName) {
        return (JCheckBox) ReflectionTestTools.get(panel, fieldName);
    }

    private static TextOption textOption(LinuxOptionsPanel panel, String fieldName) {
        return (TextOption) ReflectionTestTools.get(panel, fieldName);
    }
}
