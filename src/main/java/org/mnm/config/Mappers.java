package org.mnm.config;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Mappers {

    private static final Logger logger = LoggerFactory.getLogger(Mappers.class);

    static Client mapClient(ResultSet rs) {
        try {
            String rawStatus = rs.getString("status");
            return new Client(
                rs.getString("slug"),
                rs.getString("version"),
                mapStatus(rawStatus),
                Path.of(rs.getString("path")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Client.Status mapStatus(String rawStatus) {
        try {
            return Client.Status.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            logger.warn("WARNING: unknown client status '{}', defaulting to NOT_INSTALLED", rawStatus);
            return Client.Status.NOT_INSTALLED;
        }
    }

    static Token mapToken(ResultSet rs) {
        try {
            return new Token(
                rs.getInt("id"),
                rs.getString("slug"),
                rs.getString("token"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
