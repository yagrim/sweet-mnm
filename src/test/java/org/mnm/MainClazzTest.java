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
                        
                        Available commands:
                          install   Installs MnM client in the current location.
                          repair    Checks installation and updates if necessary
                          help      Displays available commands
                        """);
    }

    @Test
    void shouldPrintHelp(SystemOutCaptureExtension out) {
        MainClazz mainClazz = new MainClazz();
        mainClazz.main(new String[]{"help"});

        assertThat(out.getOutput()).isEqualTo("""
                Available commands:
                  install   Installs MnM client in the current location.
                  repair    Checks installation and updates if necessary
                  help      Displays available commands
                """);
    }

}
