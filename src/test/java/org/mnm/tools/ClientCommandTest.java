package org.mnm.tools;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientCommandTest {

    @Test
    void shouldRedactTokenInToString() {
        ClientCommand command = ClientCommand.of("mnm.exe", "--token", "secret-token");

        assertThat(command.toString()).isEqualTo("mnm.exe --token ***");
    }

    @Test
    void shouldKeepTokenInArray() {
        ClientCommand command = ClientCommand.of("mnm.exe", "--token", "secret-token");

        assertThat(command.toArray()).containsExactly("mnm.exe", "--token", "secret-token");
    }

    @Test
    void shouldKeepCommandWithoutToken() {
        ClientCommand command = ClientCommand.of("xxhsum", "-H2", "file");

        assertThat(command.toString()).isEqualTo("xxhsum -H2 file");
    }

    @Test
    void shouldKeepTokenOptionWithoutValue() {
        ClientCommand command = ClientCommand.of("mnm.exe", "--token");

        assertThat(command.toString()).isEqualTo("mnm.exe --token");
    }

    @Test
    void shouldReturnExecutable() {
        assertThat(ClientCommand.of("umu-run", "mnm.exe").executable()).isEqualTo("umu-run");
    }

    @Test
    void shouldRejectEmptyCommand() {
        assertThatThrownBy(ClientCommand::of).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClientCommand(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotChangeWhenSourceListChanges() {
        List<String> arguments = new ArrayList<>(List.of("mnm.exe", "--token", "secret-token"));
        ClientCommand command = new ClientCommand(arguments);

        arguments.set(2, "other-token");

        assertThat(command.toArray()).containsExactly("mnm.exe", "--token", "secret-token");
    }

}
