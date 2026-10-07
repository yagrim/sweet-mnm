package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import org.mnm.events.AssemblyListener;
import org.mnm.events.ClientEventHandler;
import org.mnm.events.DownloadListener;
import org.mnm.events.FilesValidationListener;
import org.mnm.events.RepairFilesListener;

import static org.mnm.gui.Style.SCALE;

public class DualProgressPanel extends JPanel
    implements
    FilesValidationListener,
    DownloadListener,
    AssemblyListener,
    RepairFilesListener {

    private final ProgressBarPanel manifest;
    private final ProgressBarPanel download;
    private final ProgressBarPanel assembly;

    public DualProgressPanel(String labelText1, String labelText2, String labelText3, Color backgroundColor, float uiScaling) {
        this(
            new FileCounterBar(labelText1, new Color(70, 130, 220), backgroundColor, uiScaling),
            new FileSizeBar(labelText2, new Color(130, 105, 200), backgroundColor, uiScaling),
            new FileSizeBar(labelText3, new Color(70, 190, 140), backgroundColor, uiScaling),
            backgroundColor);
    }

    DualProgressPanel(ProgressBarPanel manifest, ProgressBarPanel download, ProgressBarPanel assembly, Color backgroundColor) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(backgroundColor);

        this.manifest = manifest;
        this.download = download;
        this.assembly = assembly;

        add(manifest);
        add(download);
        add(assembly);

        ClientEventHandler.getInstance().register(this);
    }

    public void resetProgress() {
        manifest.reset();
        download.reset();
        assembly.reset();
    }

    // FilesValidationListener
    @Override
    public void validationStart(int filesCount) {
        manifest.setMaximum(filesCount);
    }

    @Override
    public void fileValidated() {
        manifest.increment(1);
    }

    // DownloadListener
    @Override
    public void dataToDownload(long bytes) {
        download.setMaximum(bytes);
    }

    @Override
    public void dataDownloaded(long bytes) {
        download.increment(bytes);
    }

    // RepairFilesListener
    @Override
    public void filesToInstall(int filesCount) {
        download.setMaximum(Long.valueOf(filesCount));
    }

    @Override
    public void fileInstalled() {
        download.increment(1L);
    }

    // AssemblyListener
    @Override
    public void dataToAssemble(long bytes) {
        assembly.setMaximum(bytes);
    }

    @Override
    public void dataAssembled(long bytes) {
        assembly.increment(bytes);
    }

    static abstract class ProgressBarPanel<T> extends JPanel {

        protected final JLabel label;
        protected final JProgressBar bar;
        protected final String labelTitle;

        ProgressBarPanel(String labelText, Color barColor, Color backgroundColor, float uiScaling) {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setBackground(backgroundColor);
            this.setBorder(BorderFactory.createEmptyBorder(SCALE, SCALE, SCALE, SCALE));

            this.labelTitle = labelText;
            this.bar = createProgressBar(barColor, uiScaling);
            this.label = createLabel(labelText);

            this.add(label);
            this.add(bar);
        }

        private static JProgressBar createProgressBar(Color fill, float uiScaling) {
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(0);
            bar.setStringPainted(true);
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.round(22 * uiScaling)));
            bar.setForeground(fill);
            return bar;
        }

        private static JLabel createLabel(String text) {
            JLabel label = new JLabel(text);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            return label;
        }

        public void reset() {
            bar.setValue(0);
        }

        abstract void setMaximum(T value);

        abstract void increment(T amount);

        public JProgressBar getBar() {
            return bar;
        }

        public JLabel getLabel() {
            return label;
        }
    }

    static class FileCounterBar extends ProgressBarPanel<Integer> {

        FileCounterBar(String labelText, Color barColor, Color backgroundColor, float uiScaling) {
            super(labelText, barColor, backgroundColor, uiScaling);
        }

        @Override
        void setMaximum(Integer value) {
            if (value == 0) {
                bar.setMaximum(100);
                bar.setValue(100);
            } else {
                bar.setMaximum(value);
                bar.setValue(0);
            }
        }

        @Override
        void increment(Integer amount) {
            int value;
            synchronized (bar) {
                value = bar.getValue() + amount;
                bar.setValue(value);
            }
            label.setText("%s... %s of %s files".formatted(labelTitle, value, bar.getMaximum()));
        }

    }

    static class FileSizeBar extends ProgressBarPanel<Long> {

        long value;

        FileSizeBar(String labelText, Color barColor, Color backgroundColor, float uiScaling) {
            super(labelText, barColor, backgroundColor, uiScaling);
        }

        @Override
        void setMaximum(Long value) {
            if (value == 0) {
                bar.setMaximum(100);
                bar.setValue(100);
            } else {
                bar.setMaximum(toMegaBytes(value));
                bar.setValue(0);
            }
        }

        @Override
        void increment(Long amount) {
            int megaBytes;
            synchronized (bar) {
                value += amount;
                megaBytes = toMegaBytes(value);
//                float gigaBytes = megaBytes / 1024;
                bar.setValue(megaBytes);
            }
            label.setText("%s... %s of %s %s".formatted(labelTitle, megaBytes, bar.getMaximum(), "MB"));
        }

        private int toMegaBytes(long bytes) {
            return Math.toIntExact(bytes / (1024 * 1024));
        }

        public void reset() {
            super.reset();
            this.value = 0;
        }

    }

}
