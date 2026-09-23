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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mnm.tools.StringUtils.join;

public class ProcessUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

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

                if (exitCode != 0) {
                    throw new RuntimeException("Process failed: exitCode=" + exitCode
                        + ", stdout=" + stdout
                        + ", stderr=" + stderr);
                }
                if (logger.isDebugEnabled()) {
                    if (!stdout.isEmpty()) {
                        logger.debug("stdout={}", stdout);
                    }
                    if (!stderr.isEmpty()) {
                        logger.debug("stderr={}", stderr);
                    }
                }

                return stdout;
            }
        } catch (IOException e) {
            if (isCommandNotFound(e)) {
                throw new CommandNotFound(command[0], e);
            }
            throw new RuntimeException("Process failed: " + join(command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Process failed: " + join(command), e);
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

    private static boolean isCommandNotFound(IOException e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (isLinux(m) || isWindows(m)) return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isWindows(String m) {
        return m.contains("createprocess error=2")
            || m.contains("createprocess error=3")
            || m.contains("cannot find the file specified");
    }

    private static boolean isLinux(String m) {
        return m.contains("error=2")
            || m.contains("no such file or directory");
    }
}
