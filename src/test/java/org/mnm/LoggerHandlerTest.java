package org.mnm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class LoggerHandlerTest {

    private static final LoggerContext CONTEXT = (LoggerContext) LoggerFactory.getILoggerFactory();

    @AfterEach
    void afterEach() {
        LoggerHandler.setInfo(false);
    }

    @Test
    void shouldDisableFileLogsInTests() {
        assertThat(System.getProperty(LoggerHandler.FILE_LOGS_PROPERTY)).isEqualTo("false");
        assertThat(LoggerHandler.fileLogsEnabled()).isFalse();
    }

    @Test
    void shouldNotAttachFileAppenderWhenFileLogsAreDisabled() {
        LoggerHandler.setInfo(true);

        Logger logger = CONTEXT.getLogger("org.mnm");
        assertThat(logger.getLevel()).isEqualTo(Level.INFO);
        assertThat(logger.iteratorForAppenders())
            .toIterable()
            .noneMatch(RollingFileAppender.class::isInstance);
    }

    @Test
    void shouldWriteLogsToFileInLogDirectory(@TempDir Path tempDir) throws IOException {
        Logger logger = CONTEXT.getLogger("org.mnm.test.file");
        logger.setAdditive(false);
        logger.setLevel(Level.INFO);
        RollingFileAppender<ILoggingEvent> appender = LoggerHandler.createFileAppender(CONTEXT, tempDir);
        logger.addAppender(appender);
        try {
            logger.info("Message to file");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        Path logFile = tempDir.resolve(LoggerHandler.LOG_FILE + ".log");
        assertThat(logFile).exists();
        assertThat(Files.readString(logFile)).contains("INFO  LoggerHandlerTest - Message to file");
    }
}
