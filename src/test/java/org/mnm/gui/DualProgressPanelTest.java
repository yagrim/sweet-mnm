package org.mnm.gui;

import java.awt.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.mnm.events.ClientEventHandler;
import org.mnm.gui.DualProgressPanel.FileCounterBar;
import org.mnm.gui.DualProgressPanel.FileSizeBar;
import org.mnm.gui.DualProgressPanel.ProgressBarPanel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class DualProgressPanelTest {

    private ProgressBarPanel validationBar;
    private ProgressBarPanel downloadBar;
    private ProgressBarPanel assemblyBar;

    private DualProgressPanel panel;

    @BeforeEach
    void setUp() {
        final Color color = new Color(229, 145, 75);
        validationBar = mockFileCounterBar(1);
        downloadBar = mockFileSizeBar(2, 1);
        assemblyBar = mockFileSizeBar(3, 2);
        panel = new DualProgressPanel(validationBar, downloadBar, assemblyBar, color);
        assertThat(panel).isNotNull();

        ClientEventHandler.getInstance().clear();
    }

    @Test
    void shouldProcessFileValidation() {
        panel.validationStart(42);

        verify(validationBar, Mockito.times(1)).setMaximum(Mockito.eq(42));
        verifyNoInteractions(downloadBar, assemblyBar);

        panel.fileValidated();
        verify(validationBar, Mockito.times(1)).increment(1);
        verifyNoInteractions(downloadBar, assemblyBar);
    }

    @Test
    void shouldProcessChunkDownload() {
        panel.dataToDownload(123);

        verify(downloadBar, Mockito.times(1)).setMaximum(Mockito.eq(123L));
        verifyNoInteractions(validationBar, assemblyBar);

        panel.dataDownloaded(123L);
        panel.dataDownloaded(321L);
        verify(downloadBar, Mockito.times(1)).increment(123L);
        verify(downloadBar, Mockito.times(1)).increment(321L);
        verifyNoInteractions(validationBar, assemblyBar);
    }

    @Test
    void shouldProcessFileInstallation() {
        panel.dataToAssemble(4242);

        verify(assemblyBar, Mockito.times(1)).setMaximum(Mockito.eq(4242L));
        verifyNoInteractions(validationBar, downloadBar);

        panel.dataAssembled(42);
        panel.dataAssembled(24);
        panel.dataAssembled(21);
        panel.dataAssembled(12);
        verify(assemblyBar, Mockito.times(1)).increment(42L);
        verify(assemblyBar, Mockito.times(1)).increment(24L);
        verify(assemblyBar, Mockito.times(1)).increment(21L);
        verify(assemblyBar, Mockito.times(1)).increment(12L);
        verifyNoInteractions(validationBar, downloadBar);
    }

    private static ProgressBarPanel mockFileCounterBar(int id) {
        var bar = new FileCounterBar("Bar " + id, Color.GREEN, Color.GREEN, 1f);
        return Mockito.spy(bar);
    }

    private static ProgressBarPanel mockFileSizeBar(int id, int progressMultiplier) {
        var bar = new FileSizeBar("Bar " + id, Color.GREEN, Color.GREEN, 1f, progressMultiplier);
        return Mockito.spy(bar);
    }

    private void verifyNoInteractions(ProgressBarPanel... bars) {
        for (ProgressBarPanel bar : bars) {
            verify(bar, Mockito.times(0)).setMaximum(Mockito.any());
            verify(bar, Mockito.times(0)).increment(Mockito.any());
        }
    }
}
