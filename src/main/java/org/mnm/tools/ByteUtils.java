package org.mnm.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class ByteUtils {

    private ByteUtils() {
    }

    public static byte[] copyRange(byte[] source, int start, int length) {
        if (source == null) {
            throw new IllegalArgumentException("Source array is null");
        }
        if (start < 0 || length < 0 || start + length > source.length) {
            throw new IndexOutOfBoundsException("Invalid start or length");
        }

        return Arrays.copyOfRange(source, start, start + length);
    }

    public static byte[] readAllBytes(Path destination) {
        try {
            return Files.readAllBytes(destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeAllBytes(Path destination, byte[] bytes) {
        try {
            Files.write(destination, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
