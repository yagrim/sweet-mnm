package org.mnm.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WineRegistry {

    private static final Logger logger = LoggerFactory.getLogger(WineRegistry.class);

    static final String SYSTEM_REG = "system.reg";
    static final String SYSTEM_REG_BACKUP = "system.reg.sweet-backup";

    private static final String LOG_PIXELS = "LogPixels";

    private WineRegistry() {
    }

    // Sets the cursor size. Wine scales the cursor with the "LogPixels" (DPI) value.
    static boolean setLogPixels(Path winePrefix, int logPixels) {
        return setIntValue(winePrefix, LOG_PIXELS, logPixels);
    }

    // Sets all the dword values with this key in the "system.reg" file of the prefix.
    // Before the first change, makes a backup of the original file. Later changes keep that backup.
    // Returns false when the file or the key does not exist. For example, before the first run,
    // Wine did not create the prefix yet.
    private static boolean setIntValue(Path winePrefix, String key, int value) {
        Path systemReg = winePrefix.resolve(SYSTEM_REG);
        if (!Files.isRegularFile(systemReg)) {
            logger.info("Cannot set {}: {} not found. Run the client one time and try again", key, systemReg);
            return false;
        }

        try {
            // ISO-8859-1 keeps all bytes as they are
            String content = Files.readString(systemReg, StandardCharsets.ISO_8859_1);
            Matcher matcher = dwordPattern(key).matcher(content);
            if (!matcher.find()) {
                logger.info("Cannot set {}: key not found in {}", key, systemReg);
                return false;
            }
            String line = "\"%s\"=dword:%08x".formatted(key, value);
            String updated = matcher.replaceAll(Matcher.quoteReplacement(line));
            if (!updated.equals(content)) {
                backupOnce(systemReg, winePrefix.resolve(SYSTEM_REG_BACKUP));
                Files.writeString(systemReg, updated, StandardCharsets.ISO_8859_1);
                logger.info("Set {} in {}", line, systemReg);
            }
            return true;
        } catch (IOException e) {
            logger.warn("Cannot set {} in {}: {}", key, systemReg, e.getMessage());
            return false;
        }
    }

    private static Pattern dwordPattern(String key) {
        return Pattern.compile("^" + Pattern.quote("\"" + key + "\"") + "=dword:[0-9a-fA-F]{8}$", Pattern.MULTILINE);
    }

    // Replaces "system.reg" with the backup of the original file, and removes the backup.
    // Returns false when there is no backup.
    static boolean restore(Path winePrefix) {
        Path backup = winePrefix.resolve(SYSTEM_REG_BACKUP);
        if (!Files.isRegularFile(backup)) {
            return false;
        }

        Path systemReg = winePrefix.resolve(SYSTEM_REG);
        try {
            Files.move(backup, systemReg, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Restored original {}", systemReg);
            return true;
        } catch (IOException e) {
            logger.warn("Cannot restore {} from {}: {}", systemReg, backup, e.getMessage());
            return false;
        }
    }

    private static void backupOnce(Path systemReg, Path backup) throws IOException {
        if (Files.exists(backup)) {
            return;
        }
        Files.copy(systemReg, backup, StandardCopyOption.COPY_ATTRIBUTES);
        logger.info("Made a backup of {} in {}", systemReg, backup);
    }
}
