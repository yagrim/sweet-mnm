package org.mnm.gui;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mnm.LoggerHandler;

class SweetLogsPanel extends JPanel {

    private static final String NO_LOGS_ERROR = "Error: no log folder found";

    private final PathPanel pathPanel;

    SweetLogsPanel(float uiScaling) {
        super(new BorderLayout());
        pathPanel = new PathPanel("Sweet logs", "Folder with the Sweet log files", getBackground(), uiScaling);
        add(pathPanel, BorderLayout.CENTER);

        setMaximumSize(pathPanel.getMaximumSize());
        showLogDirectory();
    }

    private void showLogDirectory() {
        Path logDirectory = LoggerHandler.getLogDirectory();
        boolean exists = Files.isDirectory(logDirectory);
        pathPanel.setPath(exists ? logDirectory.toString() : NO_LOGS_ERROR, exists);
    }

}
