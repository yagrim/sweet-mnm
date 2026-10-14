package org.mnm.gui;

import javax.swing.JTextPane;
import java.awt.Color;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.mnm.config.Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.Client.Status.INSTALLING;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.ReflectionTestTools.getField;

class InfoPanelTest {

    private static final String VERSION = "publish-0.24.1.0-65ab2c20cf879a5a25ea4212df4f6774ef96774e";

    private final InfoPanel panel = new InfoPanel(100, 50, Color.WHITE, 1f);
    private final JTextPane text = getField(panel, "textArea", JTextPane.class);

    @Test
    void shouldClearTextWhenThereIsNoClient() {
        panel.refresh(new ClientStatus(null, false, null));

        assertThat(text.getText()).isEmpty();
    }

    @Test
    void shouldAskForLoginWhenClientHasNoValidToken() {
        panel.refresh(new ClientStatus(client(UPDATED), false, null));

        assertThat(text.getText()).isEqualTo("No valid token: run Login");
    }

    @Test
    void shouldReplacePreviousTextWhenClientHasNoValidToken() {
        panel.refresh(new ClientStatus(client(INSTALLING), true, null));
        panel.refresh(new ClientStatus(client(UPDATED), false, null));

        assertThat(text.getText()).isEqualTo("No valid token: run Login");
    }

    @Test
    void shouldReportInterruptedOperation() {
        panel.refresh(new ClientStatus(client(INSTALLING), true, null));

        assertThat(text.getText()).startsWith("Last operation was interrupted: Re-run Install");
    }

    @Test
    void shouldReportUpToDateClient() {
        panel.refresh(new ClientStatus(client(UPDATED), true, null));

        assertThat(text.getText()).startsWith("Client is up-to-date");
    }

    private static Client client(Client.Status status) {
        return new Client("mnm", VERSION, status, Path.of("."));
    }
}
