package org.mnm.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class ProcessUtils {

    public static void panic(String message) {
        throw new PanicException(message);
    }

    static String run(Path workingDirectory, String[] command) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();

            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                line = reader.readLine();
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || line == null || line.isEmpty()) {
                throw new RuntimeException("Process failed: exitCode=" + exitCode + ", output=" + line);
            }
            return line.substring(0, line.indexOf(" "));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("XXH3 failed for: " + workingDirectory, e);
        }
    }

}
