package org.mnm.release;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record ReleaseVersion(int major, int minor, int patch) implements Comparable<ReleaseVersion> {

    // Release tags have a 'v' prefix, versions in version.txt do not
    private static final Pattern PATTERN = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)$");

    private static final Comparator<ReleaseVersion> COMPARATOR = Comparator
        .comparingInt(ReleaseVersion::major)
        .thenComparingInt(ReleaseVersion::minor)
        .thenComparingInt(ReleaseVersion::patch);

    static Optional<ReleaseVersion> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher matcher = PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new ReleaseVersion(
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3))));
    }

    @Override
    public int compareTo(ReleaseVersion other) {
        return COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return "%d.%d.%d".formatted(major, minor, patch);
    }
}
