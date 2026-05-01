package org.mnm;

import com.github.luben.zstd.Zstd;
import org.mnm.config.Environment;
import org.mnm.manifest.Manifest;
import org.mnm.manifest.ManifestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mnm.tools.ByteUtils.readAllBytes;
import static org.mnm.tools.FileUtils.assemble;
import static org.mnm.tools.FileUtils.humanReadableSize;
import static org.mnm.tools.HashFunctions.xxh3cli;
import static org.mnm.tools.Proc.panic;

public class ClientInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ClientInstaller.class);


    public static void main(String[] args) {
        ManifestHandler manifestHandler = new ManifestHandler(Environment.downloads.resolve("manifest.json"));
        List<Manifest.File> files = manifestHandler.getFiles();

        Set<Integer> sizes = new TreeSet<>();
        System.out.println("Total size: " + files.size());
        files.stream().forEach(f -> sizes.add(f.chunks().size()));

        AtomicInteger totalProcessedFiles = new AtomicInteger(0);
        AtomicInteger totalSkippedFiles = new AtomicInteger(0);
        Map<Integer, List<Manifest.File>> skippedFiles = new TreeMap<>();
        sizes.forEach(i -> {
            ValidationResult result = validatingFiles(files, i);
            if (!result.skipped.isEmpty()) {
                skippedFiles.put(i, result.skipped);
            }
            System.out.println("Total validated files: %s. Skipped: %s".formatted(
                    totalProcessedFiles.addAndGet(result.validated),
                    totalSkippedFiles.addAndGet(result.skipped.size())));
        });
        System.out.println("-------------------------------------------------");

        skippedFiles.forEach((k, v) -> {
            System.out.println("Skipped files with chunks: " + k);
            v.stream()
                    .forEach(f -> {
                        System.out.println("\t" + f.path());
                        f.getBundlesList().forEach(e -> {
                            System.out.println("\t\t" + e.bundleCrc() + ": " + e.chunks().size());
                        });
                        System.out.println("");
                    });
        });

        System.out.println("End");
    }

    private static ValidationResult validatingFiles(List<Manifest.File> files, Integer chunks) {
//        System.out.println("Checking files with %s chunks".formatted(chunks));
        AtomicInteger count = new AtomicInteger(0);
        List<Manifest.File> skipped = new ArrayList<>();
        files.stream()
                .filter(file -> file.chunks().size() == chunks)
                .forEach(file -> {
                    validateFileAndExtract(file);
                    count.incrementAndGet();
                });

        return new ValidationResult(skipped, count.intValue());
    }

    record ValidationResult(List<Manifest.File> skipped, int validated) {
    }

    // TODO we could reuse the arrays controlling indices
    // use Zstd.decompressArray(
    private static void validateFileAndExtract(Manifest.File file) {
        List<byte[]> fileSections = new ArrayList<>();
        for (Manifest.Bundle bundle : file.getBundlesList()) {
            Path chunk = Environment.chunks.resolve(bundle.resolveName());
            byte[] chunkBytes = readAllBytes(chunk);
            logger.debug("Extracted chunk: {}", humanReadableSize(bundle.fileSectionLength()));
            byte[] bytesUncompressed = new byte[bundle.fileSectionLength()];
            Zstd.decompress(bytesUncompressed, chunkBytes);
            fileSections.add(bytesUncompressed);
        }

        Path destination = Environment.mnm.resolve(file.path().substring(1));
        destination.getParent().toFile().mkdirs();
        assemble(fileSections, destination);
//        System.gc();

        String calculatedCrc = xxh3cli(destination);

        if (!calculatedCrc.equals(file.fileHash())) {
            panic("Invalid extracted hash: expected %s, found %s".formatted(file.fileHash(), calculatedCrc));
        }
    }

}
