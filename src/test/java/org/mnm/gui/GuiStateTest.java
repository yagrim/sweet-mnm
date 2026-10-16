package org.mnm.gui;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.mnm.config.Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.Client.Status.UPDATED;

class GuiStateTest {

    @Test
    void shouldBuildWithAllValues() {
        final ClientStatus clientStatus = new ClientStatus(new Client("mnm", "1.2.3", UPDATED, Path.of("client")), true, null);

        GuiState guiState = GuiState.builder()
            .slugs(List.of("mnm", "mnm-test"))
            .clientStatus(clientStatus)
            .newRelease("1.5.0")
            .build();

        assertThat(guiState.slugs()).containsExactly("mnm", "mnm-test");
        assertThat(guiState.clientStatus()).isEqualTo(clientStatus);
        assertThat(guiState.selectedSlug()).isEqualTo("mnm");
        assertThat(guiState.newRelease()).isEqualTo("1.5.0");
    }

    @Test
    void shouldBuildWithoutNewRelease() {
        GuiState guiState = GuiState.builder()
            .slugs(List.of())
            .clientStatus(new ClientStatus(null, false, null))
            .build();

        assertThat(guiState.newRelease()).isNull();
        assertThat(guiState.selectedSlug()).isNull();
    }
}
