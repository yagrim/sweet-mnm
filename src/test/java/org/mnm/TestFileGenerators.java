package org.mnm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestFileGenerators {

    public static void letters(int size, Path path) throws IOException {
        if (size < 0) {
            throw new IllegalArgumentException();
        }

        byte[] bytes = new byte[size];
        int column = 0;
        for (int i = 0; i < size; i++) {
            if (column == 26) {
                bytes[i] = '\n';
                column = 0;
            } else {
                bytes[i] = (byte) ('a' + column);
                column++;
            }
        }
        Files.write(path, bytes);
    }

    public static void numbers(int size, Path path) throws IOException {
        if (size < 0) {
            throw new IllegalArgumentException();
        }

        byte[] digits = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
        byte[] bytes = new byte[size];
        int column = 0;
        for (int i = 0; i < size; i++) {
            if (column == digits.length) {
                bytes[i] = '\n';
                column = 0;
            } else {
                bytes[i] = digits[column];
                column++;
            }
        }
        Files.write(path, bytes);
    }

}
