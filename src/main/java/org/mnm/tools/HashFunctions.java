package org.mnm.tools;

import net.openhft.hashing.LongTupleHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static org.mnm.tools.ByteUtils.readAllBytes;

public class HashFunctions {

    private static final Logger logger = LoggerFactory.getLogger(HashFunctions.class);

    public static String crc64(Path file) {
        return Crc64Redis.calculateHex(readAllBytes(file));
    }

    public static String crc64(byte[] bytes) {
        return Crc64Redis.calculateHex(bytes);
    }

    public static String xxh3(Path file) {
        return xxh3(readAllBytes(file));
    }

    /**
     * Note: the alternative hashByteBuffer does nto work:
     * - Fails with illegal access to sun.nio.ch.DirectBuffer.
     * - Still loads file into memory, outside the process, but still does.
     */
    public static String xxh3(byte[] bytes) {
        long init = System.currentTimeMillis();
        final var hashFunction = LongTupleHashFunction.xx128();
        long[] values = hashFunction.hashBytes(ByteBuffer.wrap(bytes));
        String format = String.format("%016x%016x", values[1], values[0]);
        logTime(init);
        return format;
    }

    /**
     * Slightly slower but far more memory efficient than {@link #xxh3cli(Path)}.
     */
    public static String xxh3cli(Path path) {

        try {
            long init = System.currentTimeMillis();
            // TODO consider using relative to avoid leaking things in logs
            ProcessBuilder pb = new ProcessBuilder("xxhsum", "-H2", path.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                line = reader.readLine();
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || line == null || line.isEmpty()) {
                throw new RuntimeException("Process failed for file: " + path + " (exitCode=" + exitCode + ", output=" + line + ")");
            }
            logTime(init);
            return line.substring(0, line.indexOf(" "));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("XXH3 failed for: " + path, e);
        }
    }

    private static void logTime(long init) {
        logger.info("XXH3 hash calculated {}", System.currentTimeMillis() - init);
    }
}
