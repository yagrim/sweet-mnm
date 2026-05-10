package org.mnm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.mnm.TestUtils.classpathFile;

public class LauncherTestDatabase {

    private static final String LAUNCHER_TEST_DB = "launcher_test_db.db";

    public static final String INITIAL_TOKEN = "123.456.789";

    public static Path fromClasspath() {
        return classpathFile(LAUNCHER_TEST_DB);
    }

    public static Path copyTestDb(Path tempDir) {
        Path dbCopy = tempDir.resolve(LAUNCHER_TEST_DB);
        try {
            Files.copy(classpathFile(LAUNCHER_TEST_DB), dbCopy);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dbCopy;
    }

    public static Path initTestDatabase(Path tempDir) {
        Path dbFile = tempDir.resolve("%s.db".formatted(UUID.randomUUID()));
        try {
            dbFile.toFile().createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
             var st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE settings (variable text primary key,value text)");
            st.executeUpdate("CREATE TABLE game_versions (slug text primary key,version text)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dbFile;
    }

    public static void clearTestDatabaseToken(Path dbFile) {
        updateTestDatabaseToken(dbFile, "");
    }

    public static void updateTestDatabaseToken(Path dbFile, String token) {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
             var st = connection.prepareStatement("update settings set value = ? where variable = ?;")) {
            st.setString(1, token);
            st.setString(2, "token");
            st.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
