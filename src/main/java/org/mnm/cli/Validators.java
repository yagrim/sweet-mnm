package org.mnm.cli;

import org.mnm.tools.StringUtils;

import static org.mnm.tools.ProcessUtils.panic;

class Validators {

    static void validateArguments(Arguments args) {
        if (StringUtils.isEmpty(args.get("username"))) {
            panic("Missing parameter: '--username'");
        }

        if (StringUtils.isEmpty(args.get("password"))) {
            panic("Missing parameter: '--password'");
        }
    }
}
