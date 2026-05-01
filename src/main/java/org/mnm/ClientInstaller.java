package org.mnm;

import com.github.luben.zstd.Zstd;
import org.mnm.config.Environment;
import org.mnm.manifest.Manifest;
import org.mnm.manifest.ManifestHandler;
import org.mnm.tools.HashFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mnm.tools.ByteUtils.readAllBytes;
import static org.mnm.tools.FileUtils.humanReadableSize;
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
        List<byte[]> fileSections = new ArrayList<>();

        final Path destination = Environment.mnm.resolve(file.path().substring(1));
        destination.getParent().toFile().mkdirs();

        // TODO Wrap this into proper fileHandler to do try-catch-with-resource, or manual close at least
        OutputStream out;
        try {
            out = Files.newOutputStream(destination, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Manifest.Bundle bundle : file.getBundlesList()) {
            Path chunk = Environment.chunks.resolve(bundle.resolveName());
            byte[] chunkBytes = readAllBytes(chunk);
            logger.debug("Extracted chunk: {} ({})", chunk.getFileName(), humanReadableSize(bundle.fileSectionLength()));
            byte[] bytesUncompressed = new byte[bundle.fileSectionLength()];

            // TODO XXX it's OK to extract in memory, it's 45MB. But then it should create destination file, opening as Append to add contents from each bandle
            Zstd.decompress(bytesUncompressed, chunkBytes);

            try {
                out.write(bytesUncompressed);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            fileSections.add(bytesUncompressed);
        }

//        assemble(fileSections, destination);
//        System.gc();

        String calculatedCrc = HashFunctions.OS.xxh3(destination);

        if (!calculatedCrc.equals(file.fileHash())) {
            panic("Invalid extracted hash: expected %s, found %s".formatted(file.fileHash(), calculatedCrc));
        }
    }

}
