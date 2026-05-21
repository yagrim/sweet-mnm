package org.mnm.manifest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManifestParserTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseManifestFile() throws Exception {

        String json = """
            {
              "manifest": [
                {
                  "path": "data/file1.bin",
                  "file_hash": "abc123",
                  "chunks": [
                    {
                      "offset": 0,
                      "length": 512,
                      "crc": "chunkcrc1",
                      "bundle_offset": 0,
                      "length_in_bundle": 1024,
                      "bundle_crc": "bundleA"
                    },
                    {
                      "offset": 512,
                      "length": 256,
                      "crc": "chunkcrc2",
                      "bundle_offset": 1024,
                      "length_in_bundle": 512,
                      "bundle_crc": "bundleA"
                    }
                  ]
                }
              ]
            }
            """;

        Path manifestFile = tempDir.resolve("manifest.json");

        Files.writeString(manifestFile, json);

        ManifestParser parser = new ManifestParser();

        List<Manifest.File> files = parser.parse(manifestFile);

        assertThat(files)
            .isNotNull()
            .hasSize(1);

        Manifest.File file = files.getFirst();

        assertThat(file)
            .extracting(
                Manifest.File::path,
                Manifest.File::fileHash,
                Manifest.File::totalSize
            )
            .containsExactly(
                "data/file1.bin",
                "abc123",
                768
            );

        assertThat(file.bundleCrcs())
            .containsExactly("bundleA");

        assertThat(file.chunks())
            .hasSize(2);

        assertThat(file.chunks().getFirst())
            .extracting(
                Manifest.Chunk::offset,
                Manifest.Chunk::length,
                Manifest.Chunk::crc,
                Manifest.Chunk::bundleOffset,
                Manifest.Chunk::lengthInBundle,
                Manifest.Chunk::bundleCrc
            )
            .containsExactly(
                0,
                512,
                "chunkcrc1",
                0,
                1024,
                "bundleA"
            );
    }

    @Test
    void shouldThrowExceptionForInvalidJson() throws Exception {

        String invalidJson = """
            {
              "manifest": [
            """;

        Path manifestFile = tempDir.resolve("invalid.json");

        Files.writeString(manifestFile, invalidJson);

        ManifestParser parser = new ManifestParser();

        assertThatThrownBy(() -> parser.parse(manifestFile))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to parse manifest");
    }
}
