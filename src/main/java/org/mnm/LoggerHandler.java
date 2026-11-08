package org.mnm;

import java.io.File;
import java.nio.file.Path;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;

import static org.mnm.config.Environment.getSweetConfigLocation;

public class LoggerHandler {

    private static final String FILE_APPENDER_NAME = "MNM_FILE";
    public static final String LOG_FILE = "sweet-messages";
    // Set to "false" to not write logs to a file (for example, in tests)
    static final String FILE_LOGS_PROPERTY = "sweet_log_to_file";

    private static final String MAX_FILE_SIZE = "10MB";
    private static final int MAX_ARCHIVES = 1;

    static {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = context.getLogger("org.mnm");
        attachFileAppender(context, logger);
    }

    public static void setInfo(boolean value) {
        setLevel(value, Level.INFO);
    }

    public static void setDebug(boolean value) {
        setLevel(value, Level.DEBUG);
    }

    private static void setLevel(boolean value, Level info) {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = context.getLogger("org.mnm");

        logger.setLevel(value ? info : Level.OFF);

        attachFileAppender(context, logger);
    }

    static boolean fileLogsEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(FILE_LOGS_PROPERTY));
    }

    private static void attachFileAppender(LoggerContext context, Logger logger) {
        if (!fileLogsEnabled() || logger.getAppender(FILE_APPENDER_NAME) != null) {
            return;
        }

        logger.addAppender(createFileAppender(context, getSweetConfigLocation().resolve("logs")));
    }

    static RollingFileAppender<ILoggingEvent> createFileAppender(LoggerContext context, Path logPath) {
        File logDir = logPath.toFile();
        String file = new File(logDir, LOG_FILE + ".log").getPath();
        String archivePattern = new File(logDir, LOG_FILE + ".%i.log").getPath();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d [%thread] %-5level %class{0} - %msg%n");
        encoder.start();

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName(FILE_APPENDER_NAME);
        appender.setFile(file);
        appender.setAppend(true);
        appender.setEncoder(encoder);

        FixedWindowRollingPolicy rolling = new FixedWindowRollingPolicy();
        rolling.setContext(context);
        rolling.setParent(appender);
        rolling.setFileNamePattern(archivePattern);
        rolling.setMinIndex(1);
        rolling.setMaxIndex(MAX_ARCHIVES);
        rolling.start();

        SizeBasedTriggeringPolicy<ILoggingEvent> trigger = new SizeBasedTriggeringPolicy<>();
        trigger.setContext(context);
        trigger.setMaxFileSize(FileSize.valueOf(MAX_FILE_SIZE));
        trigger.start();

        appender.setRollingPolicy(rolling);
        appender.setTriggeringPolicy(trigger);
        appender.start();

        return appender;
    }

}
