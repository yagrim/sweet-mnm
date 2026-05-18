package org.mnm.tools;

import net.openhft.hashing.LongTupleHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mnm.tools.ByteUtils.readAllBytes;

public class HashFunctions {

    private static final Logger logger = LoggerFactory.getLogger(HashFunctions.class);

    public final class InMemory {

        public static String crc64(Path file) {
            return Crc64Redis.calculateHex(readAllBytes(file));
        }

        public static String crc64(byte[] bytes) {
            return Crc64Redis.calculateHex(bytes);
        }

        public static String xxh3(Path path) {
            return xxh3(readAllBytes(path));
        }

        /**
         * Note: the alternative hashByteBuffer does nto work:
         * - Fails with illegal access to sun.nio.ch.DirectBuffer.
         * - Still loads file into memory, outside the process, but still does.
         */
        public static String xxh3(byte[] bytes) {
            return timed(() -> {
                final var hashFunction = LongTupleHashFunction.xx128();
                long[] values = hashFunction.hashBytes(ByteBuffer.wrap(bytes));
                return String.format("%016x%016x", values[1], values[0]);
            }, "XXH3 hash calculated");
        }
    }

    public final class OS {

        /**
         * Slightly slower but far more memory efficient than {@link #xxh3(Path)}.
         */
        public static String xxh3(Path path) {
            return timed(() -> {
                final String[] command = {"xxhsum", "-H2", path.toAbsolutePath().toString()};
                return firstToken(ProcessUtils.run(null, command));
            }, "XXH3 hash calculated");
        }
    }

    private static String firstToken(String output) {
        int newlineIndex = output.indexOf(System.lineSeparator());
        String firstLine = newlineIndex >= 0 ? output.substring(0, newlineIndex) : output;
        int separatorIndex = firstLine.indexOf(" ");
        if (separatorIndex < 0) {
            return firstLine;
        }
        return firstLine.substring(0, separatorIndex);
    }

    private static <T> T timed(Supplier<T> task, String message) {
        long init = System.currentTimeMillis();
        T result = task.get();
        logger.debug("{} ({} ms)", message, System.currentTimeMillis() - init);
        return result;
    }

}
