package org.mnm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemOutCaptureExtension.class)
class MainClazzTest {

    @Test
    void shouldPrintMessageAndHelpWhenCommandIsUnknown(SystemOutCaptureExtension out) {
        MainClazz mainClazz = new MainClazz();
        mainClazz.main(new String[]{"unknown"});

        assertThat(out.getOutput())
                .isEqualTo("""
                        Unrecognized command: 'unknown'
                        
                        %s""".formatted(expectedAvailableCommands()));
    }

    @Test
    void shouldPrintHelp(SystemOutCaptureExtension out) {
        MainClazz mainClazz = new MainClazz();
        mainClazz.main(new String[]{"help"});

        assertThat(out.getOutput()).isEqualTo(expectedAvailableCommands());
    }

    private static String expectedAvailableCommands() {
        return """
                Available commands:
                  install      Installs MnM client in the current location
                  login        Login with your username and password (can update launcher database)
                  logout       Removes token from the launcher database
                  repair       Checks installation and updates if necessary
                  token        User token utilities
                  token-info   Displays token information
                  version      Displays the version
                  help         Displays available commands
                """;
    }

}
