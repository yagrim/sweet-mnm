package org.mnm;

import org.junit.jupiter.api.extension.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class SystemOutCaptureExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private PrintStream originalOut;
    private ByteArrayOutputStream outputStream;

    @Override
    public void beforeEach(ExtensionContext context) {
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        System.setOut(originalOut);
    }

    public String getOutput() {
        return outputStream.toString();
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