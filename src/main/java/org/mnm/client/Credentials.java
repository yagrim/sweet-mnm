package org.mnm.client;

import org.mnm.cli.Arguments;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

record Credentials(String username, String password) {

    static Credentials parse(Arguments args) {
        return new Credentials(args.get("username"), args.get("password"));
    }

    void validate() {
        if (isEmpty(username)) {
            panic("Missing or empty parameter: '--username'");
        }
        if (!isEmpty(username) && isEmpty(password)) {
            panic("Missing or empty parameter: '--password'");
        }
    }
}
