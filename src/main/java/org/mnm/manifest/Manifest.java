package org.mnm.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public record Manifest(List<File> manifest) {

    public record File(String path, @JsonProperty("file_hash") String fileHash, List<Chunk> chunks) {

        /**
         * Size of final file.
         */
        public int totalSize() {
            int size = 0;
            for (Manifest.Chunk chunk : chunks) {
                size += chunk.length();
            }
            return size;
        }

        // IMPORTANT: preserves order from manifest
        public List<String> bundleCrcs() {
            final Set<String> found = new HashSet<>();
            final List<String> result = new ArrayList<>();
            for (Chunk chunk : chunks) {
                String bundleCrc = chunk.bundleCrc;
                if (!found.contains(bundleCrc)) {
                    found.add(bundleCrc);
                    result.add(bundleCrc);
                }
            }
            return Collections.unmodifiableList(result);
        }

        // Note: max number of chunks is 600
        // We need to keep order for assembly
        public List<Bundle> getBundlesList() {
            List<Bundle> indexedChunks = new ArrayList<>();
            for (String crc : bundleCrcs()) {
                List<Manifest.Chunk> list = chunks().stream()
                        .filter(c -> c.bundleCrc().equals(crc))
                        .toList();

                indexedChunks.add(new Bundle(list));
            }

            return indexedChunks;
        }

    }


    public record Chunk(Integer offset, Integer length, String crc, @JsonProperty("bundle_offset") Integer bundleOffset,
                        @JsonProperty("length_in_bundle") Integer lengthInBundle,
                        @JsonProperty("bundle_crc") String bundleCrc) {

        String normalize() {
            return String.format("%16s", crc).replace(' ', '0');
        }
    }

    public record Bundle(List<Manifest.Chunk> chunks) {

        public String bundleCrc() {
            return chunks.get(0).bundleCrc();
        }

        /**
         * Size of the extracted bundle.
         */
        public int fileSectionLength() {
            int size = 0;
            for (Manifest.Chunk chunk : chunks) {
                size += chunk.length();
            }
            return size;
        }

        /**
         * Size uncompressed.
         */
        public int bundleLength() {
            int size = 0;
            for (Manifest.Chunk chunk : chunks) {
                size += chunk.lengthInBundle();
            }
            return size;
        }

        public String resolveName() {
            return chunks.get(0).bundleCrc() + ".bin";
        }
    }
}
