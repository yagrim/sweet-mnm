package org.mnm.client;

import org.mnm.cli.Arguments;
import org.mnm.config.SettingsStore;

import static org.mnm.config.SettingsStore.DEFAULT_UMU_GAMEID;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_USE_CLIENT_AS_PREFIX;
import static org.mnm.config.SettingsStore.MANGOHUD_KEY;
import static org.mnm.config.SettingsStore.UMU_GAMEID;
import static org.mnm.config.SettingsStore.UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.UMU_USE_CLIENT_AS_PREFIX;
import static org.mnm.config.SettingsStore.UMU_WINEPREFIX;
import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

/**
 * Records to pass parameters to {@see Client}.
 */
public record RunnerOptions(String slug, Integer tokenId, boolean skipVersionCheck, LinuxOptions linuxOptions) {

    public record LinuxOptions(boolean enableMangoHud, boolean useClientAsPrefix, UmuOptions umuOptions) {
    }

    public record UmuOptions(String gameId, String protonPath, String winePrefix) {
    }

    public static RunnerOptions parse(Arguments args, SettingsStore settingsStore) {
        boolean useClientAsPrefix = settingsStore.getBoolean(UMU_USE_CLIENT_AS_PREFIX, DEFAULT_UMU_USE_CLIENT_AS_PREFIX);
        boolean mangoHud = getMangoHud(args, settingsStore);

        return new RunnerOptions(
            args.get("slug"),
            parseTokenId(args.get("id")),
            args.getBoolean("skip-version-check"),
            // TODO add cli arg for UMU and useClientAsPrefix

            new LinuxOptions(mangoHud, useClientAsPrefix,
                new UmuOptions(
                    settingsStore.get(UMU_GAMEID, DEFAULT_UMU_GAMEID),
                    settingsStore.get(UMU_PROTONPATH, DEFAULT_UMU_PROTONPATH),
                    getWinePrefix(useClientAsPrefix, settingsStore))));
    }

    private static boolean getMangoHud(Arguments args, SettingsStore settingsStore) {
        if (args.contains("enable-mangohud")) {
            return args.getBoolean("enable-mangohud");
        }
        return settingsStore.getBoolean(MANGOHUD_KEY, false);
    }

    private static String getWinePrefix(boolean useClientAsPrefix, SettingsStore settingsStore) {
        return useClientAsPrefix ? null : settingsStore.get(UMU_WINEPREFIX, null);
    }

    private static Integer parseTokenId(String tokenId) {
        if (isEmpty(tokenId)) {
            return null;
        }

        try {
            return Integer.valueOf(tokenId);
        } catch (NumberFormatException e) {
            panic("Invalid token id: %s".formatted(tokenId));
            return null;
        }
    }

}
