package org.mnm;

import org.junit.jupiter.api.extension.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

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
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    public String getErrorOutput() {
        return errorStream.toString(StandardCharsets.UTF_8);
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
