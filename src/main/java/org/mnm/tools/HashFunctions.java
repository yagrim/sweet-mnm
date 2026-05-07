package org.mnm.tools;

import net.openhft.hashing.LongTupleHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.file.Path;

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
    }

    public final class OS {

        /**
         * Slightly slower but far more memory efficient than {@link #xxh3(Path)}.
         */
        public static String xxh3(Path path) {
            long init = System.currentTimeMillis();
            final String[] command = {"xxhsum", "-H2", path.getFileName().toString()};
            String output = ProcessUtils.run(path.getParent(), command);
            logTime(init);
            return output;
        }
    }

    private static void logTime(long init) {
//        logger.debug("XXH3 hash calculated ({} ms)", System.currentTimeMillis() - init);
    }
}
