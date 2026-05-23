package org.mnm.launcher;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import static org.mnm.tools.StringUtils.isEmpty;

class TokenService {

    String retrieveToken(Path dbFile) {
        try (LauncherDb launcherDb = new LauncherDb(dbFile)) {
            String token = launcherDb.getSettings().get("token");
            return isEmpty(token) ? null : token;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
