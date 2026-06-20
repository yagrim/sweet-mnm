package org.mnm.gui;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.mnm.events.ClientEventHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DualProgressPanelTest {

    private DualProgressPanel.ProgressLabel progressLabel1;
    private DualProgressPanel.ProgressLabel progressLabel2;

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

    private static DualProgressPanel.ProgressLabel mockProgressLabel() {
        DualProgressPanel.ProgressLabel mock = mock(DualProgressPanel.ProgressLabel.class);
        Mockito.when(mock.getLabel()).thenReturn(new JLabel("Test label"));
        Mockito.when(mock.getBar()).thenReturn(new JProgressBar());
        return mock;
    }

}
