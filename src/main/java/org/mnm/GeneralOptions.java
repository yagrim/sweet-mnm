package org.mnm;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

public class GeneralOptions {

    public static void setInfo(boolean value) {
        setLevel(value, Level.INFO);
    }

    public static void setDebug(boolean value) {
        setLevel(value, Level.DEBUG);
    }

    private static void setLevel(boolean value, Level info) {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        final Logger myLogger = context.getLogger("org.mnm");
        myLogger.setLevel(value ? info : Level.OFF);
    }

}
