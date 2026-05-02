package org.mnm.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArgumentParserTest {

    @Test
    void parsesKeyValuePairs() {
        String[] args = {"--name", "Alice", "--age", "30"};

        ParsedArguments parsed = ArgumentParser.parse(args);

        assertThat(parsed.get("name")).isEqualTo("Alice");
        assertThat(parsed.getInt("age", 0)).isEqualTo(30);
    }

    @Test
    void parsesBooleanFlag() {
        String[] args = {"--verbose"};

        ParsedArguments parsed = ArgumentParser.parse(args);

        assertThat(parsed.getBoolean("verbose")).isTrue();
    }

    @Test
    void missingValueDefaultsToTrueFlag() {
        String[] args = {"--debug"};

        ParsedArguments parsed = ArgumentParser.parse(args);

        assertThat(parsed.get("debug")).isEqualTo("true");
    }

    @Test
    void returnsDefaultsWhenKeyMissing() {
        String[] args = {};

        ParsedArguments parsed = ArgumentParser.parse(args);

        assertThat(parsed.getOrDefault("missing", "default")).isEqualTo("default");
        assertThat(parsed.getInt("missingInt", 42)).isEqualTo(42);
        assertThat(parsed.getBoolean("missingBool")).isFalse();
    }

    @Test
    void ignoresNonPrefixedArguments() {
        String[] args = {"name", "Alice", "--age", "25"};

        ParsedArguments parsed = ArgumentParser.parse(args);

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

        ParsedArguments parsed = ArgumentParser.parse(args);

        assertThat(parsed.get("name")).isEqualTo("Bob");
        assertThat(parsed.getBoolean("verbose")).isTrue();
        assertThat(parsed.getInt("count", 0)).isEqualTo(5);
    }

    @Test
    void invalidIntegerFallsBackToDefault() {
        String[] args = {"--age", "notANumber"};

        ParsedArguments parsed = ArgumentParser.parse(args);

        assertThat(parsed.getInt("age", 99)).isEqualTo(99);
    }
}