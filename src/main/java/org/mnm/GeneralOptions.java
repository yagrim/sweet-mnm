package org.mnm;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

public class GeneralOptions {

    public static void toggleDebug(boolean value) {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        final Logger myLogger = context.getLogger("org.mnm");
        myLogger.setLevel(value ? Level.DEBUG : Level.INFO);
    }

}
