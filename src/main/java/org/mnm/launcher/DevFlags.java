package org.mnm.launcher;

import org.mnm.cli.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mnm.tools.StringUtils.isEmpty;

record DevFlags(boolean enabled, String apiEndpoint) {

    private static final Logger logger = LoggerFactory.getLogger(DevFlags.class);

    public static DevFlags parse(Arguments args) {
        final String apiEndpoint = args.get("api-endpoint");
        if (args.getBoolean("dev-options") && !isEmpty(apiEndpoint)) {
            logger.info("DEVELOPER OPTIONS ENABLED!");
            logger.info("If you see this line, proceed at your own risk");
            return new DevFlags(true, apiEndpoint);
        }
        return new DevFlags(false, null);
    }
}
