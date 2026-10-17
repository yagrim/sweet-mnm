package org.mnm.client;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.mnm.gui.InMemorySettingsStore;
import org.mnm.release.ReleaseChecker;

import static org.assertj.core.api.Assertions.assertThat;

class FactoriesTest {

    @Test
    void shouldCreateReleaseCheckerWithCurrentVersion() {
        ReleaseChecker releaseChecker = Factories.releaseChecker();
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());

        assertThat(releaseChecker.findNewRelease(settings)).isEmpty();
        assertThat(settings.get("release.last-check")).isNull();
    }
}
