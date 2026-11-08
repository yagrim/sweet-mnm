package org.mnm.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mnm.gui.InMemorySettingsStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.SettingsStore.CACHE_AUTO_CLEAR_KEY;
import static org.mnm.config.SettingsStore.CACHE_RETENTION_DAYS_KEY;

class CacheCleanerTest {

    // A fixed time makes the results the same on each test run.
    // Test files dates are created from it.
    private static final Instant FIXED_NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void shouldUseDefaultOptionsWhenOptionsAreNotSet(@TempDir Path tempDir) throws IOException {
        Path keptFile = cacheFile(tempDir, "kept.bin", 9);
        Path deletedFile = cacheFile(tempDir, "bundles/deleted.bin", "12345", 11);
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());

        CacheCleaner.Result result = new CacheCleaner(settings, CLOCK).clean(List.of(tempDir));

        assertThat(result).isEqualTo(new CacheCleaner.Result(1, 5));
        assertThat(keptFile).exists();
        assertThat(deletedFile).doesNotExist();
    }

    @Test
    void shouldNotWriteSettingsWhenOptionsAreNotSet(@TempDir Path tempDir) throws IOException {
        cacheFile(tempDir, "bundles/deleted.bin", 11);
        InMemorySettingsStore settings = new InMemorySettingsStore(Map.of());

        new CacheCleaner(settings, CLOCK).clean(List.of(tempDir));

        assertThat(settings.values()).isEmpty();
    }

    @Test
    void shouldNotDeleteFilesWhenAutoClearIsDisabled(@TempDir Path tempDir) throws IOException {
        Path oldFile = cacheFile(tempDir, "bundles/old.bin", 100);
        InMemorySettingsStore settings = settings("false", 10);

        CacheCleaner.Result result = new CacheCleaner(settings, CLOCK).clean(List.of(tempDir));

        assertThat(result).isEqualTo(new CacheCleaner.Result(0, 0));
        assertThat(oldFile).exists();
    }

    @Test
    void shouldDeleteOnlyFilesOlderThanRetentionPeriod(@TempDir Path tempDir) throws IOException {
        Path oldBundle = cacheFile(tempDir, "bundles/old.bin", "12345", 11);
        Path oldJson = cacheFile(tempDir, "manifest.json", "123", 20);
        Path newBundle = cacheFile(tempDir, "bundles/new.bin", "1234567", 9);
        InMemorySettingsStore settings = settings("true", 10);

        CacheCleaner.Result result = new CacheCleaner(settings, CLOCK).clean(List.of(tempDir));

        assertThat(result).isEqualTo(new CacheCleaner.Result(2, 8));
        assertThat(oldBundle).doesNotExist();
        assertThat(oldJson).doesNotExist();
        assertThat(newBundle).exists();
        assertThat(tempDir.resolve("downloads").resolve("bundles")).isDirectory();
    }

    @Test
    void shouldNotDeleteManifestFiles(@TempDir Path tempDir) throws IOException {
        Path oldManifest = cacheFile(tempDir, "65ab2c20cf879a5a25ea4212df4f6774ef96774e.manifest", "123", 100);
        Path oldBundle = cacheFile(tempDir, "bundles/old.bin", "12345", 100);
        InMemorySettingsStore settings = settings("true", 10);

        CacheCleaner.Result result = new CacheCleaner(settings, CLOCK).clean(List.of(tempDir));

        assertThat(result).isEqualTo(new CacheCleaner.Result(1, 5));
        assertThat(oldManifest).exists();
        assertThat(oldBundle).doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 15, 30})
    void shouldUseConfiguredRetentionPeriod(int retentionDays, @TempDir Path tempDir) throws IOException {
        Path keptFile = cacheFile(tempDir, "kept.bin", "1", retentionDays - 1);
        Path deletedFile = cacheFile(tempDir, "deleted.bin", "12", retentionDays + 1);
        InMemorySettingsStore settings = settings("true", retentionDays);

        CacheCleaner.Result result = new CacheCleaner(settings, CLOCK).clean(List.of(tempDir));

        assertThat(result).isEqualTo(new CacheCleaner.Result(1, 2));
        assertThat(keptFile).exists();
        assertThat(deletedFile).doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3", "7", "invalid"})
    void shouldUseDefaultRetentionPeriodWhenValueIsNotValid(String retentionDays, @TempDir Path tempDir) throws IOException {
        Path keptFile = cacheFile(tempDir, "kept.bin", 9);
        Path deletedFile = cacheFile(tempDir, "deleted.bin", 11);
        InMemorySettingsStore settings = settings("true", retentionDays);

        new CacheCleaner(settings, CLOCK).clean(List.of(tempDir));

        assertThat(keptFile).exists();
        assertThat(deletedFile).doesNotExist();
    }

    @Test
    void shouldCleanEachDownloadsFolderOnlyOnce(@TempDir Path tempDir) throws IOException {
        Path sharedPath = tempDir.resolve("shared");
        Path otherPath = tempDir.resolve("other");
        cacheFile(sharedPath, "a.bin", "123", 11);
        cacheFile(otherPath, "b.bin", "12", 11);
        InMemorySettingsStore settings = settings("true", 10);

        CacheCleaner.Result result = new CacheCleaner(settings, CLOCK)
            .clean(List.of(sharedPath, otherPath, sharedPath));

        assertThat(result).isEqualTo(new CacheCleaner.Result(2, 5));
    }

    @Test
    void shouldIgnoreMissingDownloadsFolder(@TempDir Path tempDir) {
        InMemorySettingsStore settings = settings("true", 10);

        CacheCleaner.Result result = new CacheCleaner(settings, CLOCK).clean(List.of(tempDir.resolve("missing")));

        assertThat(result).isEqualTo(new CacheCleaner.Result(0, 0));
    }

    private static InMemorySettingsStore settings(String autoClear, int retentionDays) {
        return settings(autoClear, Integer.toString(retentionDays));
    }

    private static InMemorySettingsStore settings(String autoClear, String retentionDays) {
        return new InMemorySettingsStore(Map.of(
            CACHE_AUTO_CLEAR_KEY, autoClear,
            CACHE_RETENTION_DAYS_KEY, retentionDays));
    }

    private static Path cacheFile(Path clientPath, String name, int ageInDays) throws IOException {
        return cacheFile(clientPath, name, "content", ageInDays);
    }

    private static Path cacheFile(Path clientPath, String name, String content, int ageInDays) throws IOException {
        Path file = clientPath.resolve("downloads").resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        Files.setLastModifiedTime(file, FileTime.from(FIXED_NOW.minus(Duration.ofDays(ageInDays))));
        return file;
    }
}
