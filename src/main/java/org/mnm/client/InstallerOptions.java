package org.mnm.client;

import org.mnm.cli.Arguments;
import org.mnm.tools.StringUtils;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public record InstallerOptions(String username,
                               String password,
                               String slug,
                               FileCheck fileCheck
) {

    public static InstallerOptions forRepair(String slug, boolean inMemoryHashing) {
        return new InstallerOptions(null, null, slug, inMemoryHashing ? FileCheck.inmemory : FileCheck.xxhsum);
    }

    public static InstallerOptions parse(Arguments args) {
        return new InstallerOptions(
            args.get("username"),
            args.get("password"),
            args.getOrDefault("slug", null),
            FileCheck.from(args.get("file-check")));
    }

    public void validateInstall() {
        if (isEmpty(username)) {
            panic("Missing or empty parameter: '--username'");
        }
        if (!isEmpty(username) && isEmpty(password)) {
            panic("Missing or empty parameter: '--password'");
        }
        validateFileCheck();
    }

    public void validateRepair() {
        if (StringUtils.isEmpty(slug)) {
            panic("Missing or empty parameter: '--slug'");
        }
        validateFileCheck();
    }

    private void validateFileCheck() {
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
