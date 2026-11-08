package org.mnm.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class WineRegistryTest {

    private static final String SYSTEM_REG = """
        WINE REGISTRY Version 2
        ;; All keys relative to \\\\Machine

        [Software\\\\Fonts] 1700000000
        #time=1d9c0a1b2c3d4e5
        "LogPixels"=dword:00000060

        [System\\\\CurrentControlSet\\\\Hardware Profiles\\\\Current\\\\Software\\\\Fonts] 1700000000
        #time=1d9c0a1b2c3d4e5
        "FIXEDFON.FON"="vgafix.fon"
        "LogPixels"=dword:00000078
        "OtherLogPixels"=dword:00000060
        """;

    @Test
    void shouldReplaceAllLogPixelsValues(@TempDir Path prefix) throws IOException {
        Path systemReg = writeSystemReg(prefix, SYSTEM_REG);

        WineRegistry.setLogPixels(prefix, 192);

        assertThat(Files.readString(systemReg)).isEqualTo(SYSTEM_REG
            .replace("\"LogPixels\"=dword:00000060", "\"LogPixels\"=dword:000000c0")
            .replace("\"LogPixels\"=dword:00000078", "\"LogPixels\"=dword:000000c0"));
    }

    @Test
    void shouldNotChangeOtherLines(@TempDir Path prefix) throws IOException {
        Path systemReg = writeSystemReg(prefix, SYSTEM_REG);

        WineRegistry.setLogPixels(prefix, 144);

        assertThat(Files.readString(systemReg))
            .contains("\"OtherLogPixels\"=dword:00000060")
            .contains("\"FIXEDFON.FON\"=\"vgafix.fon\"")
            .doesNotContain("\"LogPixels\"=dword:00000060")
            .contains("\"LogPixels\"=dword:00000090");
    }

    @Test
    void shouldKeepNonAsciiBytes(@TempDir Path prefix) throws IOException {
        byte[] content = (SYSTEM_REG + "\"Name\"=\"café\"\n").getBytes(StandardCharsets.UTF_8);
        Path systemReg = prefix.resolve(WineRegistry.SYSTEM_REG);
        Files.write(systemReg, content);

        WineRegistry.setLogPixels(prefix, 96);

        assertThat(Files.readString(systemReg, StandardCharsets.UTF_8)).endsWith("\"Name\"=\"café\"\n");
    }

    @Test
    void shouldNotCreateSystemRegWhenItDoesNotExist(@TempDir Path prefix) {
        WineRegistry.setLogPixels(prefix, 192);

        assertThat(prefix.resolve(WineRegistry.SYSTEM_REG)).doesNotExist();
        assertThat(prefix.resolve(WineRegistry.SYSTEM_REG_BACKUP)).doesNotExist();
    }

    @Test
    void shouldNotChangeFileWhenLogPixelsDoesNotExist(@TempDir Path prefix) throws IOException {
        String content = "WINE REGISTRY Version 2\n\"OtherLogPixels\"=dword:00000060\n";
        Path systemReg = writeSystemReg(prefix, content);

        WineRegistry.setLogPixels(prefix, 192);

        assertThat(Files.readString(systemReg)).isEqualTo(content);
    }

    @Test
    void shouldBackupOriginalFileOnFirstChange(@TempDir Path prefix) throws IOException {
        writeSystemReg(prefix, SYSTEM_REG);

        WineRegistry.setLogPixels(prefix, 192);

        assertThat(Files.readString(prefix.resolve(WineRegistry.SYSTEM_REG_BACKUP))).isEqualTo(SYSTEM_REG);
    }

    @Test
    void shouldKeepFirstBackupOnNextChanges(@TempDir Path prefix) throws IOException {
        Path systemReg = writeSystemReg(prefix, SYSTEM_REG);

        WineRegistry.setLogPixels(prefix, 192);
        WineRegistry.setLogPixels(prefix, 144);

        assertThat(Files.readString(prefix.resolve(WineRegistry.SYSTEM_REG_BACKUP))).isEqualTo(SYSTEM_REG);
        assertThat(Files.readString(systemReg)).contains("\"LogPixels\"=dword:00000090");
    }

    @Test
    void shouldNotBackupWhenFileDoesNotChange(@TempDir Path prefix) throws IOException {
        String content = "WINE REGISTRY Version 2\n\"LogPixels\"=dword:000000c0\n";
        Path systemReg = writeSystemReg(prefix, content);

        WineRegistry.setLogPixels(prefix, 192);

        assertThat(Files.readString(systemReg)).isEqualTo(content);
        assertThat(prefix.resolve(WineRegistry.SYSTEM_REG_BACKUP)).doesNotExist();
    }

    @Test
    void shouldNotBackupWhenLogPixelsDoesNotExist(@TempDir Path prefix) throws IOException {
        writeSystemReg(prefix, "WINE REGISTRY Version 2\n");

        WineRegistry.setLogPixels(prefix, 192);

        assertThat(prefix.resolve(WineRegistry.SYSTEM_REG_BACKUP)).doesNotExist();
    }

    @Test
    void shouldRestoreOriginalFileAndRemoveBackup(@TempDir Path prefix) throws IOException {
        Path systemReg = writeSystemReg(prefix, SYSTEM_REG);
        WineRegistry.setLogPixels(prefix, 192);
        WineRegistry.setLogPixels(prefix, 144);

        WineRegistry.restore(prefix);

        assertThat(Files.readString(systemReg)).isEqualTo(SYSTEM_REG);
        assertThat(prefix.resolve(WineRegistry.SYSTEM_REG_BACKUP)).doesNotExist();
    }

    @Test
    void shouldBackupAgainAfterRestore(@TempDir Path prefix) throws IOException {
        writeSystemReg(prefix, SYSTEM_REG);
        WineRegistry.setLogPixels(prefix, 192);
        WineRegistry.restore(prefix);

        WineRegistry.setLogPixels(prefix, 120);

        assertThat(Files.readString(prefix.resolve(WineRegistry.SYSTEM_REG_BACKUP))).isEqualTo(SYSTEM_REG);
    }

    @Test
    void shouldNotChangeFileWhenRestoreHasNoBackup(@TempDir Path prefix) throws IOException {
        Path systemReg = writeSystemReg(prefix, SYSTEM_REG);

        WineRegistry.restore(prefix);

        assertThat(Files.readString(systemReg)).isEqualTo(SYSTEM_REG);
    }

    @Test
    void shouldRestoreBackupWhenSystemRegDoesNotExist(@TempDir Path prefix) throws IOException {
        Path backup = Files.writeString(prefix.resolve(WineRegistry.SYSTEM_REG_BACKUP), SYSTEM_REG);

        WineRegistry.restore(prefix);

        assertThat(Files.readString(prefix.resolve(WineRegistry.SYSTEM_REG))).isEqualTo(SYSTEM_REG);
        assertThat(backup).doesNotExist();
    }

    @Test
    void shouldDoNothingWhenRestoreHasNoPrefix(@TempDir Path tempDir) {
        Path prefix = tempDir.resolve("missing");

        WineRegistry.restore(prefix);

        assertThat(prefix).doesNotExist();
    }

    private static Path writeSystemReg(Path prefix, String content) throws IOException {
        return Files.writeString(prefix.resolve(WineRegistry.SYSTEM_REG), content);
    }
}
