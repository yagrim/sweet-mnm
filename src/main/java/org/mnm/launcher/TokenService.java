package org.mnm.launcher;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.Instant;

import org.mnm.tools.JwtParser;

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

    String formatToColumns(String token) {
        JwtParser.JwtClaims claims = JwtParser.parse(token);

        StringBuilder sb = new StringBuilder();
        sb.append("%-7s : %s%n".formatted("issuer", claims.issuer()));
        sb.append("%-7s : %s%n".formatted("created", toInstant(claims.issuedAt())));
        sb.append("%-7s : %s%n".formatted("expires", toInstant(claims.expiration())));
        sb.append("%-7s : %s%n".formatted("email", claims.email()));
        return sb.toString();
    }

    private static Instant toInstant(long value) {
        return value > 0 ? Instant.ofEpochSecond(value) : null;
    }

}
