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
                        
                        %s""".formatted(expectedHelp()));
    }

    @Test
    void shouldPrintHelp(SystemOutCaptureExtension out) {
        MainClazz mainClazz = new MainClazz();
        mainClazz.main(new String[]{"help"});

        assertThat(out.getOutput()).isEqualTo(expectedHelp());
    }

    private static String expectedHelp() {
        return """
                (The unofficial and...) Sweet tool to manage Monsters & Memories clients
                
                Usage:
                  sweet <command> [--option [value]] ...

                Available commands:
                  install      Installs MnM client in the current location
                  login        Login with your username and password (can update launcher database)
                  logout       Removes token from the launcher database
                  repair       Checks installation and updates if necessary
                  token        Shows official launcher current token
                  token-info   Shows official launcher token information
                  version      Shows the version
                  help         Shows available commands
                """;
    }

}
