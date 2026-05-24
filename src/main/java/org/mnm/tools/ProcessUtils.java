package org.mnm.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ProcessUtils {

    public static void panic(String message) {
        throw new PanicException(message);
    }

    public static String run(Path workingDirectory, String[] command) {
        return run(workingDirectory, command, null);
    }

    public static String run(Path workingDirectory, String[] command, Map<String, String> environment) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory.toFile());
            }
            if (environment != null && !environment.isEmpty()) {
                processBuilder.environment().putAll(environment);
            }
            Process process = processBuilder.start();

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<String> standardOutput = executor.submit(() -> read(process.getInputStream()));
                Future<String> standardError = executor.submit(() -> read(process.getErrorStream()));

                int exitCode = process.waitFor();
                String stdout = getFuture(standardOutput);
                String stderr = getFuture(standardError);

                if (exitCode != 0 || stdout.isEmpty()) {
                    throw new RuntimeException("Process failed: exitCode=" + exitCode
                        + ", stdout=" + stdout
                        + ", stderr=" + stderr);
                }
                return stdout;
            }
        } catch (IOException e) {
            throw new RuntimeException("Process failed for: " + workingDirectory, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Process failed for: " + workingDirectory, e);
        }
    }

    private static String read(InputStream inputStream) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append(System.lineSeparator());
                }
                output.append(line);
            }
            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read process stream", e);
        }
    }

    private static String getFuture(Future<String> future) throws InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

}
