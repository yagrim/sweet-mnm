package org.mnm.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

    public static void panic(String message) {
        throw new PanicException(message);
    }

    public static String runAndWait(Path workingDirectory, ClientCommand command) {
        return runAndWait(workingDirectory, command, null);
    }

    public static Process runDetached(Path workingDirectory, ClientCommand command, Map<String, String> environment, Path logFile) {
        try {
            ProcessBuilder processBuilder = createProcessBuilder(workingDirectory, command, environment);
            if (logFile == null) {
                processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
            } else {
                Files.createDirectories(logFile.toAbsolutePath().getParent());
                processBuilder.redirectErrorStream(true);
                processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
            }
            Process process = processBuilder.start();
            // The game does not read from stdin
            process.getOutputStream().close();
            return process;
        } catch (IOException e) {
            throw startFailure(command, e);
        }
    }

    private static ProcessBuilder createProcessBuilder(Path workingDirectory, ClientCommand command, Map<String, String> environment) {
        ProcessBuilder processBuilder = new ProcessBuilder(command.arguments());
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile());
        }
        if (environment != null && !environment.isEmpty()) {
            processBuilder.environment().putAll(environment);
        }
        return processBuilder;
    }

    private static RuntimeException startFailure(ClientCommand command, IOException e) {
        if (isCommandNotFound(e)) {
            return new CommandNotFound(command.executable(), e);
        }
        return new RuntimeException("Process failed: " + command, e);
    }

    public static String runAndWait(Path workingDirectory, ClientCommand command, Map<String, String> environment) {
        try {
            Process process = createProcessBuilder(workingDirectory, command, environment).start();

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
            throw startFailure(command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Process failed: " + command, e);
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
