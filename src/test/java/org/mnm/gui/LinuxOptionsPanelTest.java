package org.mnm.gui;

import javax.swing.JCheckBox;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.config.CursorSize;
import org.mnm.config.OS;
import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_GAMEID;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.MANGOHUD_KEY;
import static org.mnm.config.SettingsStore.PROTON_ENABLE_WAYLAND;
import static org.mnm.config.SettingsStore.PROTON_USE_WINED3D;
import static org.mnm.config.SettingsStore.UMU_GAMEID;
import static org.mnm.config.SettingsStore.UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.UMU_USE_CLIENT_AS_PREFIX;
import static org.mnm.config.SettingsStore.UMU_WINEPREFIX;
import static org.mnm.gui.InMemorySettingsStore.CURSOR_SIZE_KEY;

class LinuxOptionsPanelTest {

    private static final Supplier<ProtonOptionsWindow> NO_OP = () -> null;

    @Test
    void shouldRestorePersistedLinuxOptions() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of(
            MANGOHUD_KEY, "true",
            UMU_GAMEID, "custom-game",
            UMU_PROTONPATH, "/opt/proton",
            UMU_USE_CLIENT_AS_PREFIX, "false",
            UMU_WINEPREFIX, "/home/user/prefix",
            PROTON_USE_WINED3D, "true",
            PROTON_ENABLE_WAYLAND, "true",
            CURSOR_SIZE_KEY, "SCALE_200"));
        LinuxOptionsPanel panel = new LinuxOptionsPanel(settingsStore, NO_OP);

        assertThat(panel.isMangoHudEnabled()).isTrue();
        assertThat(panel.getUmuGameId()).isEqualTo("custom-game");
        assertThat(panel.getUmuProtonPath()).isEqualTo("/opt/proton");
        assertThat(panel.isUseClientAsPrefix()).isFalse();
        assertThat(panel.getUmuWinePrefix()).isEqualTo("/home/user/prefix");
        assertThat(panel.getCursorSize()).isEqualTo(CursorSize.SCALE_200);
        // default values
        assertThat(panel.isProtonUseWined3dEnabled()).isFalse();
        assertThat(panel.isProtonWaylandEnabled()).isFalse();

        assertThat(textOption(panel, "umuWinePrefix").isEnabled()).isEqualTo(!OS.isWindows());
    }

    @Test
    void shouldUseLinuxOptionDefaults() {
        SettingsStore settingsStore = new InMemorySettingsStore(Map.of());
        LinuxOptionsPanel panel = new LinuxOptionsPanel(settingsStore, NO_OP);

        assertThat(panel.isMangoHudEnabled()).isFalse();
        assertThat(panel.getUmuGameId()).isEqualTo(DEFAULT_UMU_GAMEID);
        assertThat(panel.getUmuProtonPath()).isEqualTo(DEFAULT_UMU_PROTONPATH);
        assertThat(panel.isUseClientAsPrefix()).isTrue();
        assertThat(panel.getUmuWinePrefix()).isEmpty();
        assertThat(panel.getCursorSize()).isEqualTo(CursorSize.DEFAULT);
        assertThat(panel.isProtonUseWined3dEnabled()).isFalse();
        assertThat(panel.isProtonWaylandEnabled()).isFalse();

        assertThat(textOption(panel, "umuWinePrefix").isEnabled()).isFalse();
    }

    @Test
    @EnabledOnOs(LINUX)
    void shouldPersistMangoHudCheckboxOptions() {
        SettingsStore settings = new InMemorySettingsStore(Map.of());
        LinuxOptionsPanel panel = new LinuxOptionsPanel(settings, NO_OP);

        assertThat(settings.get(MANGOHUD_KEY)).isNull();
        checkbox(panel, "mangoHudOption").doClick();
        assertThat(settings.get(MANGOHUD_KEY)).isEqualTo("true");
    }

    @Test
    @EnabledOnOs(LINUX)
    void shouldPersistUseClientAsPrefixCheckboxOption() {
        SettingsStore settings = new InMemorySettingsStore(Map.of());
        LinuxOptionsPanel panel = new LinuxOptionsPanel(settings, NO_OP);

        assertThat(settings.get(UMU_USE_CLIENT_AS_PREFIX)).isNull();
        checkbox(panel, "useClientAsPrefix").doClick();
        assertThat(settings.get(UMU_USE_CLIENT_AS_PREFIX)).isEqualTo("false");

        assertThat(textOption(panel, "umuWinePrefix").isEnabled()).isTrue();
    }

    @Test
    void shouldPersistCursorSizeOption() {
        SettingsStore settings = new InMemorySettingsStore(Map.of());
        LinuxOptionsPanel panel = new LinuxOptionsPanel(settings, NO_OP);

        assertThat(settings.get(CURSOR_SIZE_KEY)).isNull();
        cursorSize(panel).getComboBox().setSelectedItem(CursorSize.SCALE_150);
        assertThat(settings.get(CURSOR_SIZE_KEY)).isEqualTo("SCALE_150");
        assertThat(panel.getCursorSize()).isEqualTo(CursorSize.SCALE_150);
    }

    @Test
    void shouldDisableAllLinuxControlsOnWindows() {
        try (MockedStatic<OS> os = Mockito.mockStatic(OS.class)) {
            os.when(OS::isWindows).thenReturn(true);

            LinuxOptionsPanel panel = new LinuxOptionsPanel(new InMemorySettingsStore(Map.of()), NO_OP);

            assertThat(checkbox(panel, "mangoHudOption").isEnabled()).isFalse();
            assertThat(textOption(panel, "umuGameId").isEnabled()).isFalse();
            assertThat(textOption(panel, "umuProtonPath").isEnabled()).isFalse();
            assertThat(checkbox(panel, "useClientAsPrefix").isEnabled()).isFalse();
            assertThat(textOption(panel, "umuWinePrefix").isEnabled()).isFalse();
            assertThat(button(panel, "protonVariables").isEnabled()).isFalse();
            assertThat(cursorSize(panel).isEnabled()).isFalse();
        }
    }

    private static JCheckBox checkbox(LinuxOptionsPanel panel, String fieldName) {
        return (JCheckBox) ReflectionTestTools.get(panel, fieldName);
    }

    @SuppressWarnings("unchecked")
    private static ComboBoxOption<CursorSize> cursorSize(LinuxOptionsPanel panel) {
        return (ComboBoxOption<CursorSize>) ReflectionTestTools.get(panel, "cursorSizeSelector");
    }

    private static TextOption textOption(LinuxOptionsPanel panel, String fieldName) {
        return (TextOption) ReflectionTestTools.get(panel, fieldName);
    }

    private static javax.swing.JButton button(LinuxOptionsPanel panel, String fieldName) {
        return (javax.swing.JButton) ReflectionTestTools.get(panel, fieldName);
    }
}
