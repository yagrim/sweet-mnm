package org.mnm.client;

import org.mnm.tools.StringUtils;

import static org.mnm.tools.ProcessUtils.panic;

record InstallOptions(String username, String password, String slug) {

    public void validate() {
        if (StringUtils.isEmpty(slug)) {
            if (StringUtils.isEmpty(username)) {
                panic("Missing or empty parameter: '--username' or '--slug'");
            }
            if (!StringUtils.isEmpty(username) && StringUtils.isEmpty(password)) {
                panic("Missing or empty parameter: '--password'");
            }
        }
    }
}
