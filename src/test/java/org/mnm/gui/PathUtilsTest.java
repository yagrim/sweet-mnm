package org.mnm.gui;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PathUtilsTest {

    @Test
    void shouldReplaceDrive() {
        Path path = PathUtils.replaceDrive(Path.of("c:/some/two"), 'd');

        assertThat(path).isEqualTo(Path.of("d:/some/two"));
    }

    @Test
    void shouldRejectUnixPath() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> PathUtils.replaceDrive(Path.of("/some/two"), 'd'));
    }
}
