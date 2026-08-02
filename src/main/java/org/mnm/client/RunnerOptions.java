package org.mnm.client;

import org.mnm.cli.Arguments;
import org.mnm.config.SettingsStore;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

/**
 * Records to pass paramaters to {@see Client}
 */
public record RunnerOptions(String slug, Integer tokenId, boolean skipVersionCheck, LinuxOptions linuxOptions) {

    public record LinuxOptions(boolean enableMangoHud, boolean useClientAsPrefix, UmuOptions umuOptions) {
    }

    public record UmuOptions(String gameId, String protonPath, String winePrefix) {
    }

    public static RunnerOptions parse(Arguments args) {
        return new RunnerOptions(
            args.get("slug"),
            parseTokenId(args.get("id")),
            args.getBoolean("skip-version-check"),
            // TODO add cli arg for useClientAsPrefix
            new LinuxOptions(args.getBoolean("enable-mangohud"), true,
                new UmuOptions(SettingsStore.DEFAULT_UMU_GAMEID, SettingsStore.DEFAULT_UMU_PROTONPATH, null))
        );
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
