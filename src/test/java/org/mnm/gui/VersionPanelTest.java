package org.mnm.gui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionPanelTest {

    @Test
    void test() {
        PlayPanel.VersionPanel panel = new PlayPanel.VersionPanel(1f);

        assertThat(panel.getText()).matches("^Sweet v0\\.0\\.1-SNAPSHOT \\(.{7}\\)$");
    }
}
