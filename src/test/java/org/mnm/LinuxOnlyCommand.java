package org.mnm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mnm.cli.Command;
import org.mnm.launcher.LogoutCommand;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class LinuxOnlyCommand {

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldValidateLinuxAvailability() {
        final Command token = buildCommand();
        boolean available = token.isAvailable();
        assertThat(available).isTrue();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldValidateWindowsAvailability() {
        final Command token = buildCommand();
        boolean available = token.isAvailable();
        assertThat(available).isFalse();
    }


    protected abstract Command buildCommand();

}
