package org.mnm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.Environment.getDownloadsLocation;
import static org.mnm.config.Environment.getSweetConfigLocation;

class EnvironmentTest {

    @Test
    void shouldUseCacheFolderInsideConfigLocationForDownloads() {
        assertThat(getDownloadsLocation())
            .isEqualTo(getSweetConfigLocation().resolve("cache"));
    }

    @Test
    void shouldNotUseDownloadsAsFolderName() {
        assertThat(getDownloadsLocation().getFileName()).hasToString("cache");
    }
}
