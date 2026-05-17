package org.mnm.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ArgumentParserTest {

    @Test
    void parsesKeyValuePairs() {
        String[] args = {"--name", "Alice", "--age", "30"};

        Arguments parsed = ArgumentsParser.parse(args);

        assertThat(parsed.get("name")).isEqualTo("Alice");
        assertThat(parsed.getInt("age", 0)).isEqualTo(30);
    }

    @Test
    void parsesBooleanFlag() {
        String[] args = {"--verbose"};

        Arguments parsed = ArgumentsParser.parse(args);

        assertThat(parsed.getBoolean("verbose")).isTrue();
    }

    @Test
    void detectsHelpFlag() {
        String[] args = {"--help"};

        Arguments parsed = ArgumentsParser.parse(args);

        assertThat(parsed.isHelp()).isTrue();
    }

    @Test
    void missingValueDefaultsToTrueFlag() {
        String[] args = {"--debug"};

        Arguments parsed = ArgumentsParser.parse(args);

        assertThat(parsed.getBoolean("debug")).isTrue();
    }

    @Test
    void shouldFailWhenGettingBooleanAsString() {
        String[] args = {"--debug"};

        Arguments parsed = ArgumentsParser.parse(args);
        Throwable t = catchThrowable(() -> parsed.get("debug"));

        assertThat(t)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Boolean flag cannot be read as string");
    }

    @Test
    void returnsDefaultsWhenKeyMissing() {
        String[] args = {};

        Arguments parsed = ArgumentsParser.parse(args);

        assertThat(parsed.getOrDefault("missing", "default")).isEqualTo("default");
        assertThat(parsed.getInt("missingInt", 42)).isEqualTo(42);
        assertThat(parsed.getBoolean("missingBool")).isFalse();
    }

    @Test
    void returnsDefaultStringWhenValueIsBoolean() {
        String[] args = {"--debug"};

        Arguments parsed = ArgumentsParser.parse(args);

        assertThat(parsed.getOrDefault("debug", "default")).isEqualTo("default");
    }

    @Test
    void ignoresNonPrefixedArguments() {
        String[] args = {"name", "Alice", "--age", "25"};

        Arguments parsed = ArgumentsParser.parse(args);

        assertThat(parsed.get("name")).isNull();
        assertThat(parsed.getInt("age", 0)).isEqualTo(25);
    }

    @Test
    void handlesMixedArguments() {
        String[] args = {
                "--name", "Bob",
                "--verbose",
                "--count", "5"
        };

        Arguments parsed = ArgumentsParser.parse(args);

        assertThat(parsed.get("name")).isEqualTo("Bob");
        assertThat(parsed.getBoolean("verbose")).isTrue();
        assertThat(parsed.getInt("count", 0)).isEqualTo(5);
    }

    @Test
    void invalidIntegerFallsBackToDefault() {
        String[] args = {"--age", "notANumber"};

        Arguments parsed = ArgumentsParser.parse(args);

        assertThat(parsed.getInt("age", 99)).isEqualTo(99);
    }
}
