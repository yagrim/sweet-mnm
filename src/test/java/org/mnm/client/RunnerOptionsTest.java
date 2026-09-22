package org.mnm.client;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.mnm.cli.Arguments;
import org.mnm.gui.InMemorySettingsStore;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.client.LinuxOptionsTestFactory.defaultLinuxOptions;
import static org.mnm.client.LinuxOptionsTestFactory.linuxOptions;
import static org.mnm.client.LinuxOptionsTestFactory.umuOptions;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_GAMEID;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.MANGOHUD_KEY;
import static org.mnm.config.SettingsStore.UMU_GAMEID;
import static org.mnm.config.SettingsStore.UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.UMU_USE_CLIENT_AS_PREFIX;
import static org.mnm.config.SettingsStore.UMU_WINEPREFIX;

class RunnerOptionsTest {

    @Nested
    class Parse {

        @Test
        void shouldReturnDefaults() {
            Arguments arguments = Arguments.parse();
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of());

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options)
                .isEqualTo(new RunnerOptions(null, null, false, defaultLinuxOptions()));
        }

        @Test
        void shouldParseAllArgumentOptions() {
            Arguments arguments = Arguments.parse(
                "--slug", "mnm",
                "--id", "42",
                "--skip-version-check",
                "--enable-mangohud");
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of());

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options)
                .isEqualTo(new RunnerOptions("mnm", 42, true, linuxOptions(true)));
        }

        @Test
        void shouldGetUmuGameIdFromSettingsStore() {
            Arguments arguments = Arguments.parse();
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of(UMU_GAMEID, "my_id"));

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options)
                .isEqualTo(new RunnerOptions(null, null, false,
                    linuxOptions(new RunnerOptions.UmuOptions("my_id", DEFAULT_UMU_PROTONPATH, null))));
        }

        @Test
        void shouldGetUmuProtonPathFromSettingsStore() {
            Arguments arguments = Arguments.parse();
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of(UMU_PROTONPATH, "GE-Proton-XX"));

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options)
                .isEqualTo(new RunnerOptions(null, null, false,
                    linuxOptions(new RunnerOptions.UmuOptions(DEFAULT_UMU_GAMEID, "GE-Proton-XX", null))));
        }

        @Test
        void shouldGetUseClientAsPrefixFromSettingsStore() {
            Arguments arguments = Arguments.parse();
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of(UMU_USE_CLIENT_AS_PREFIX, "false"));

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options)
                .isEqualTo(new RunnerOptions(null, null, false, linuxOptions(false, umuOptions())));
        }

        @Test
        void shouldGetDefaultUmuWinePrefixFromSettingsStore() {
            Arguments arguments = Arguments.parse();
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of(UMU_WINEPREFIX, "/home/user/games/mnm"));

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options)
                .isEqualTo(new RunnerOptions(null, null, false,
                    linuxOptions(new RunnerOptions.UmuOptions(DEFAULT_UMU_GAMEID, DEFAULT_UMU_PROTONPATH, null)
                    )));
        }

        @Test
        void shouldGetUmuWinePrefixFromSettingsStore() {
            Arguments arguments = Arguments.parse();
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of(
                UMU_USE_CLIENT_AS_PREFIX, "false",
                UMU_WINEPREFIX, "/home/user/games/mnm"
            ));

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options)
                .isEqualTo(new RunnerOptions(null, null, false,
                    linuxOptions(false, new RunnerOptions.UmuOptions(DEFAULT_UMU_GAMEID, DEFAULT_UMU_PROTONPATH, "/home/user/games/mnm"))));
        }

        @Test
        void shouldGetUmuOptionsFromSettingsStore() {
            Arguments arguments = Arguments.parse(
                "--slug", "mnm",
                "--id", "42",
                "--skip-version-check");
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of(
                UMU_GAMEID, "my_id",
                UMU_PROTONPATH, "GE-Proton-XX"
            ));

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options)
                .isEqualTo(new RunnerOptions("mnm", 42, true,
                    linuxOptions(new RunnerOptions.UmuOptions("my_id", "GE-Proton-XX", null))));
        }

        @Test
        void shouldPanicWhenTokenIdIsInvalid() {
            Arguments arguments = Arguments.parse("--id", "not-a-number");
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of());

            assertThatThrownBy(() -> RunnerOptions.parse(arguments, settingsStore))
                .isInstanceOf(PanicException.class)
                .hasMessage("Invalid token id: not-a-number");
        }

        @Test
        void shouldReadMangoHudFromArgs() {
            Arguments arguments = Arguments.parse("--enable-mangohud", "true");
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of());

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options.linuxOptions().enableMangoHud()).isTrue();
        }

        @Test
        void shouldReadMangoHudFromSettings() {
            Arguments arguments = Arguments.parse("--something-else", "true");
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of(MANGOHUD_KEY, "true"));

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options.linuxOptions().enableMangoHud()).isTrue();
        }

        @Test
        void shouldOverrideMangoHudFromArgs() {
            Arguments arguments = Arguments.parse("--enable-mangohud", "true");
            InMemorySettingsStore settingsStore = new InMemorySettingsStore(Map.of(MANGOHUD_KEY, "false"));

            RunnerOptions options = RunnerOptions.parse(arguments, settingsStore);

            assertThat(options.linuxOptions().enableMangoHud()).isTrue();
        }
    }

}
