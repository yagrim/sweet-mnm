package org.mnm;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mnm.config.OS;

public class SystemOutCaptureExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;

    @Override
    public void beforeEach(ExtensionContext context) {
        originalOut = System.out;
        originalErr = System.err;
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream, false, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errorStream, false, StandardCharsets.UTF_8));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    public String getOutput() {
        return format(outputStream.toString(StandardCharsets.UTF_8));
    }

    public String getErrorOutput() {
        return format(errorStream.toString(StandardCharsets.UTF_8));
    }

    private static String format(String value) {
        return OS.isWindows() ? value.replaceAll("\r\n", "\n") : value;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(SystemOutCaptureExtension.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) {
        return this;
    }
}
