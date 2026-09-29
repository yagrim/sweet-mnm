package org.mnm.gui;

import java.awt.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.mnm.events.ClientEventHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DualProgressPanelTest {

    private DualProgressPanel.ProgressPanel progressLabel1;
    private DualProgressPanel.ProgressPanel progressLabel2;

    private DualProgressPanel panel;

    @BeforeEach
    void setUp() {
        final Color color = new Color(229, 145, 75);
        progressLabel1 = mockProgressLabel();
        progressLabel2 = mockProgressLabel();
        panel = new DualProgressPanel(progressLabel1, progressLabel2, color);
        assertThat(panel).isNotNull();

        ClientEventHandler.getInstance().clear();
    }

    @Test
    void shouldProcessFileValidation() {
        Mockito.reset(progressLabel1, progressLabel2);
        panel.validationStart(42);

        verify(progressLabel1, Mockito.times(1)).setMaximum(Mockito.eq(42));
        verifyNoInteractions(progressLabel2);

        panel.fileValidated();
        verify(progressLabel1, Mockito.times(1)).increment();
        verifyNoInteractions(progressLabel2);
    }

    @Test
    void shouldProcessFileInstallation() {
        Mockito.reset(progressLabel1, progressLabel2);
        panel.filesToInstall(42);

        verify(progressLabel2, Mockito.times(1)).setMaximum(Mockito.eq(42));
        verifyNoInteractions(progressLabel1);

        panel.fileInstalled();
        panel.fileInstalled();
        verify(progressLabel2, Mockito.times(2)).increment();
        verifyNoInteractions(progressLabel1);
    }

    private static DualProgressPanel.ProgressPanel mockProgressLabel() {
        DualProgressPanel.ProgressPanel progressLabel = new DualProgressPanel.ProgressPanel("Test label", Color.GREEN, Color.GRAY, 1f);
        DualProgressPanel.ProgressPanel mock = Mockito.spy(progressLabel);
        return mock;
    }

}
