package org.mnm;

import org.mnm.config.Environment;
import org.mnm.manifest.Manifest;
import org.mnm.manifest.ManifestHandler;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ManifestExplorer {


    public static void main(String[] args) {

        final Path location = Environment.downloads.resolve("manifest.json");
        final ManifestHandler manifestHandler = new ManifestHandler(location);

        Set<Integer> sizes = new HashSet<>();
        List<Manifest.File> files = manifestHandler.getFiles();

        files.forEach(file -> {
            file.chunks().forEach(chunk -> {
                int length = chunk.crc().length();
                sizes.add(length);
//                if (chunk.crc().length() < 16) {
//                    System.out.println(chunk.crc());
//                }
            });
        });

        System.out.println("Files with more than 1 bundle:");
        files.stream()
                .forEach(file -> {
                    List<String> crcs = file.bundleCrcs();
                    if (crcs.size() > 1) {
                        System.out.println("File: " + file.path() + ", bundles: " + crcs.size());
                    }
                });

        System.out.println();
        System.out.println("Bundles that belong to more than one file:");

        Map<String, Set<String>> bundleIndex = manifestHandler.buildBundleIndex();
        bundleIndex.entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(e -> {
                    System.out.println(e.getKey() + " " + e.getValue());
                    List<Manifest.File> list = e.getValue().stream().map(path -> manifestHandler.findByFilePath(path)).toList();
                    System.out.println("");
                });

    }
}
