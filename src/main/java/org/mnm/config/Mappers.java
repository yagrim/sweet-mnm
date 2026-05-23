package org.mnm.config;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;

class Mappers {

    static Client mapClient(ResultSet rs) {
        try {
            return new Client(
                rs.getString("slug"),
                rs.getString("version"),
                Client.Status.valueOf(rs.getString("status")),
                Path.of(rs.getString("path")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static Session mapSession(ResultSet rs) {
        try {
            return new Session(
                rs.getInt("id"),
                rs.getString("slug"),
                rs.getString("token"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
