package org.mnm.client;

import org.mnm.client.RunnerOptions.LinuxOptions;
import org.mnm.client.RunnerOptions.ToolsOptions;
import org.mnm.client.RunnerOptions.UmuOptions;

import static org.mnm.config.SettingsStore.DEFAULT_UMU_GAMEID;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_PROTONPATH;

public class LinuxOptionsTestFactory {

    public static LinuxOptions defaultLinuxOptions() {
        return linuxOptions(false, false);
    }

    public static LinuxOptions linuxOptions(boolean mangoHud, boolean gamemode) {
        return new LinuxOptions(true,
            new ToolsOptions(mangoHud, gamemode),
            new UmuOptions(DEFAULT_UMU_GAMEID, DEFAULT_UMU_PROTONPATH, null));
    }

    public static LinuxOptions linuxOptions(boolean useClientAsPrefix, UmuOptions umuOptions) {
        return new LinuxOptions(useClientAsPrefix,
            new ToolsOptions(false, false),
            umuOptions);
    }

    public static LinuxOptions linuxOptions(UmuOptions umuOptions) {
        return linuxOptions(true, umuOptions);
    }
}
