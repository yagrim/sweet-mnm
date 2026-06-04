package org.mnm.experimental;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LogPanel extends JPanel {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final JTextArea area = new JTextArea();
    private final SwingAppender appender = new SwingAppender();
    private volatile Level minLevel = Level.TRACE;

    public LogPanel() {
        super(new BorderLayout());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void install() {
        ILoggerFactory f = LoggerFactory.getILoggerFactory();
        if (!(f instanceof LoggerContext ctx)) {
            area.setText("ERROR: logback-classic not found.\nGot: " + f.getClass().getName());
            return;
        }
        appender.setContext(ctx);
        appender.start();
        ch.qos.logback.classic.Logger root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.TRACE);
        root.addAppender(appender);
    }

    public void uninstall() {
        ILoggerFactory f = LoggerFactory.getILoggerFactory();
        if (f instanceof LoggerContext ctx)
            ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).detachAppender(appender);
    }

    private void onEvent(ILoggingEvent e) {
        if (!e.getLevel().isGreaterOrEqual(minLevel)) return;
        String line = FMT.format(Instant.ofEpochMilli(e.getTimeStamp()))
            + " " + String.format("%-5s", e.getLevel())
            + " " + e.getLoggerName()
            + " - " + e.getFormattedMessage() + "\n";
        SwingUtilities.invokeLater(() -> {
            area.append(line);
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    private class SwingAppender extends AppenderBase<ILoggingEvent> {
        @Override protected void append(ILoggingEvent e) { onEvent(e); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LogPanel panel = new LogPanel();
            panel.install();

            org.slf4j.Logger log = LoggerFactory.getLogger(LogPanel.class);

            JButton btn = new JButton("Fire logs");
            btn.addActionListener(e -> new Thread(() -> {
                log.trace("trace message");
                log.debug("debug message");
                log.info("info message");
                log.warn("warn message");
                log.error("error message", new RuntimeException("boom"));
            }).start());

            JFrame frame = new JFrame("LogPanel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(btn,   BorderLayout.NORTH);
            frame.getContentPane().add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setSize(800, 500);
            frame.setVisible(true);
        });
    }
}
