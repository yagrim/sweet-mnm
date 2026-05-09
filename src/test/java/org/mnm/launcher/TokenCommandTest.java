package org.mnm.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mnm.SystemOutCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.TestUtils.classpathFile;

@ExtendWith(SystemOutCaptureExtension.class)
class TokenCommandTest {

    @Test
    void shouldReturnToken(SystemOutCaptureExtension out) {
        TokenCommand token = new TokenCommand(() -> classpathFile("launcher_test_db.db"));

        token.run(null);

        assertThat(out.getOutput()).isEqualTo("""
                123.456.789
                """);
    }

}
