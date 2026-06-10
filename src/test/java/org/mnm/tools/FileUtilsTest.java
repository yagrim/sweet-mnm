package org.mnm.tools;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.tools.FileUtils.humanReadableSize;

class FileUtilsTest {

    @Nested
    class HumanReadableSize {

        @Test
        void shouldReturnBytes() {
            assertThat(humanReadableSize(0)).isEqualTo("0 B");
            assertThat(humanReadableSize(1)).isEqualTo("1 B");
            assertThat(humanReadableSize(1023)).isEqualTo("1023 B");
        }

        @Test
        void shouldReturnKilobytes() {
            assertThat(humanReadableSize(1024)).isEqualTo("1.00 KB");
            assertThat(humanReadableSize(1536)).isEqualTo("1.50 KB");
            assertThat(humanReadableSize(1024 * 1024 - 1)).isEqualTo("1024.00 KB");
        }

        @Test
        void shouldReturnMegabytes() {
            assertThat(humanReadableSize(1024 * 1024)).isEqualTo("1.00 MB");
            assertThat(humanReadableSize((long) (1.5 * 1024 * 1024))).isEqualTo("1.50 MB");
            assertThat(humanReadableSize(1024L * 1024 * 1024 - 1)).isEqualTo("1024.00 MB");
        }

        @Test
        void shouldReturnGigabytes() {
            assertThat(humanReadableSize(1024L * 1024 * 1024)).isEqualTo("1.00 GB");
            assertThat(humanReadableSize((long) (1.5 * 1024 * 1024 * 1024))).isEqualTo("1.50 GB");
            assertThat(humanReadableSize(10L * 1024 * 1024 * 1024)).isEqualTo("10.00 GB");
        }

        @Test
        void shouldHandleBoundaries() {
            assertThat(humanReadableSize(1023)).isEqualTo("1023 B");
            assertThat(humanReadableSize(1024)).isEqualTo("1.00 KB");
            assertThat(humanReadableSize(1024 * 1024 - 1)).isEqualTo("1024.00 KB");
            assertThat(humanReadableSize(1024 * 1024)).isEqualTo("1.00 MB");
            assertThat(humanReadableSize(1024L * 1024 * 1024 - 1)).isEqualTo("1024.00 MB");
            assertThat(humanReadableSize(1024L * 1024 * 1024)).isEqualTo("1.00 GB");
        }
    }

    @Nested
    class GetFolderSize {

        @Test
        void shouldReturn0WhenPathDoesNotExist() {
            final Path missingPath = Path.of(UUID.randomUUID().toString());
            long folderSize = FileUtils.getFolderSize(missingPath);

            assertThat(folderSize).isEqualTo(0);
        }

        @Test
        void shouldReturnSizeWhenPathIsValid() {
            final Path missingPath = Path.of(".");
            long folderSize = FileUtils.getFolderSize(missingPath);

            assertThat(folderSize).isGreaterThan(110000L);
        }
    }

}
