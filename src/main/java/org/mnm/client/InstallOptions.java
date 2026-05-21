package org.mnm.client;

import org.mnm.cli.Arguments;
import org.mnm.tools.StringUtils;

import static org.mnm.tools.ProcessUtils.panic;

record InstallOptions(String username,
                      String password,
                      String slug,
                      FileCheck fileCheck

) {

    public static InstallOptions parse(Arguments args) {
        return new InstallOptions(
                args.get("username"),
                args.get("password"),
                args.getOrDefault("slug", null),
                FileCheck.from(args.get("file-check")));
    }

    public void validate() {
        if (StringUtils.isEmpty(slug)) {
            if (StringUtils.isEmpty(username)) {
                panic("Missing or empty parameter: '--username' or '--slug'");
            }
            if (!StringUtils.isEmpty(username) && StringUtils.isEmpty(password)) {
                panic("Missing or empty parameter: '--password'");
            }
        }
        if (fileCheck == null) {
            panic("Invalid File Check value, use 'in-memory' or 'xxhsum'");
        }
    }

    enum FileCheck {
        xxhsum, inmemory;

        static FileCheck from(String value) {
            if (StringUtils.isEmpty(value)) {
                return xxhsum;
            }
            if ("in-memory".equalsIgnoreCase(value)) {
                return inmemory;
            }
            if ("xxhsum".equalsIgnoreCase(value)) {
                return xxhsum;
            }
            return null;
        }
    }
}
