package org.mnm;

import org.mnm.config.Environment;
import org.mnm.manifest.Manifest;
import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.HashFunctions;
import org.mnm.tools.Zstd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mnm.tools.ProcessUtils.panic;

public class ClientInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ClientInstaller.class);

    public static void main(String[] args) {
        if (args.length != 2) {
            panic("Usage: ClientInstaller <username> <password>");
        }

        ManifestService manifestService = ManifestService.login(args[0], args[1]);
        ManifestHandler manifestHandler = manifestService.getManifestHandler();
        List<Manifest.File> files = manifestHandler.getFiles();

        Set<Integer> sizes = new TreeSet<>();
        System.out.println("Total size: " + files.size());
        files.stream().forEach(f -> sizes.add(f.chunks().size()));

        // Download chunks
        files.forEach(file -> file.getBundlesList().stream()
                .map(Manifest.Bundle::bundleCrc)
                .forEach(manifestService::downloadChunk));
        // TODO validate chunks

        // Validations
        AtomicInteger totalProcessedFiles = new AtomicInteger(0);
        sizes.forEach(i -> {
            ValidationResult result = validatingFiles(files, i);
            logger.info("Total validated files: {}", totalProcessedFiles.addAndGet(result.validated));
        });
        System.out.println("-------------------------------------------------");

    }

    private static ValidationResult validatingFiles(List<Manifest.File> files, Integer chunks) {
//        System.out.println("Checking files with %s chunks".formatted(chunks));
        AtomicInteger count = new AtomicInteger(0);
        files.stream()
                .filter(file -> file.chunks().size() == chunks)
                .forEach(file -> {
                    validateFileAndExtract(file);
                    count.incrementAndGet();
                });

        return new ValidationResult(count.intValue());
    }

    record ValidationResult(int validated) {
    }

    // TODO we could reuse the arrays controlling indices
    // use Zstd.decompressArray( or maybe use ByBuffer with a 45 MB shared array
    // we cal calculate the bigger bundle size, currently 85,621634 MB
    private static void validateFileAndExtract(Manifest.File file) {
        final Path destination = Environment.mnm.resolve(file.path().substring(1));
        destination.getParent().toFile().mkdirs();

        Zstd.Section[] sections = file.getBundlesList()
                .stream()
                .map(bundle -> {
                    Path chunk = Environment.chunks.resolve(bundle.resolveName());
                    return new Zstd.Section(chunk, bundle.fileSectionLength());
                })
                .toArray(Zstd.Section[]::new);
        Zstd.InMemory.decompress(destination, sections);

        String calculatedCrc = HashFunctions.OS.xxh3(destination);
        if (!calculatedCrc.equals(file.fileHash())) {
            panic("Invalid extracted hash: expected %s, found %s".formatted(file.fileHash(), calculatedCrc));
        }
    }

}
