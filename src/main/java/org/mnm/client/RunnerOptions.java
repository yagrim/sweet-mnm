package org.mnm.client;

import org.mnm.cli.Arguments;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

record RunnerOptions(String slug, Integer tokenId, boolean skipVersionCheck) {

    static RunnerOptions parse(Arguments args) {
        return new RunnerOptions(
            args.get("slug"),
            parseTokenId(args.get("id")),
            args.getBoolean("skip-version-check")
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
