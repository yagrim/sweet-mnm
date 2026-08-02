package org.mnm.client;

import org.mnm.client.RunnerOptions.LinuxOptions;
import org.mnm.client.RunnerOptions.UmuOptions;

import static org.mnm.config.SettingsStore.DEFAULT_UMU_GAMEID;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_PROTONPATH;

public class LinuxOptionsTestFactory {

    public static LinuxOptions defaultLinuxOptions() {
        return linuxOptions(false);
    }

    public static LinuxOptions linuxOptions(boolean mangoHud) {
        return new LinuxOptions(mangoHud, true,
            new UmuOptions(DEFAULT_UMU_GAMEID, DEFAULT_UMU_PROTONPATH, null));
    }

    public static LinuxOptions linuxOptions(boolean useClientAsPrefix, UmuOptions umuOptions) {
        return new LinuxOptions(false, useClientAsPrefix, umuOptions);
    }

    public static LinuxOptions linuxOptions(UmuOptions umuOptions) {
        return linuxOptions(true, umuOptions);
    }
}
