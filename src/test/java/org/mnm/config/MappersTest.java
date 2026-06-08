package org.mnm.config;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MappersTest {

    @Nested
    class MapClientTests {

        private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        private PrintStream originalErr;

        @BeforeEach
        void redirectStderr() {
            originalErr = System.err;
            System.setErr(new PrintStream(errContent));
        }

        @AfterEach
        void restoreStderr() {
            System.setErr(originalErr);
        }

        @Test
        void shouldMapAllFields() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("slug")).thenReturn("my-client");
            when(rs.getString("version")).thenReturn("1.2.3");
            when(rs.getString("status")).thenReturn("UPDATED");
            when(rs.getString("path")).thenReturn("/opt/clients/my-client");

            Client client = Mappers.mapClient(rs);

            assertThat(client.slug()).isEqualTo("my-client");
            assertThat(client.version()).isEqualTo("1.2.3");
            assertThat(client.status()).isEqualTo(Client.Status.UPDATED);
            assertThat(client.path()).isEqualTo(Path.of("/opt/clients/my-client"));
        }

        @Test
        void shouldMapEveryStatus() throws SQLException {
            for (Client.Status status : Client.Status.values()) {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getString("slug")).thenReturn("slug");
                when(rs.getString("version")).thenReturn("1.0");
                when(rs.getString("status")).thenReturn(status.name());
                when(rs.getString("path")).thenReturn("/tmp");

                Client client = Mappers.mapClient(rs);

                assertThat(client.status()).isEqualTo(status);
            }
        }

        @Test
        void shouldWrapSQLExceptionInRuntimeException() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString(anyString())).thenThrow(new SQLException("db error"));

            assertThatThrownBy(() -> Mappers.mapClient(rs))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(SQLException.class)
                .hasRootCauseMessage("db error");
        }

        @Test
        void shouldDefaultToNotInstalled() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("slug")).thenReturn("slug");
            when(rs.getString("version")).thenReturn("1.0");
            when(rs.getString("status")).thenReturn("NOT_A_REAL_STATUS");
            when(rs.getString("path")).thenReturn("/tmp");

            Client client = Mappers.mapClient(rs);

            assertThat(client.status()).isEqualTo(Client.Status.NOT_INSTALLED);
        }

        @Test
        void shouldConvertPathStringToPathObject() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("slug")).thenReturn("s");
            when(rs.getString("version")).thenReturn("0");
            when(rs.getString("status")).thenReturn("NOT_INSTALLED");
            when(rs.getString("path")).thenReturn("/a/b/c");

            Client client = Mappers.mapClient(rs);

            assertThat(client.path()).isEqualTo(Path.of("/a/b/c"));
        }
    }

    @Nested
    class MapTokenTests {

        @Test
        void shouldMapAllFields() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getInt("id")).thenReturn(42);
            when(rs.getString("slug")).thenReturn("my-token");
            when(rs.getString("token")).thenReturn("abc123");

            Token token = Mappers.mapToken(rs);

            assertThat(token.id()).isEqualTo(42);
            assertThat(token.slug()).isEqualTo("my-token");
            assertThat(token.token()).isEqualTo("abc123");
        }

        @Test
        void shouldMapZeroId() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getInt("id")).thenReturn(0);
            when(rs.getString("slug")).thenReturn("slug");
            when(rs.getString("token")).thenReturn("tok");

            Token token = Mappers.mapToken(rs);

            assertThat(token.id()).isZero();
        }

        @Test
        void shouldMapNegativeIdWithoutThrowing() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getInt("id")).thenReturn(-1);
            when(rs.getString("slug")).thenReturn("slug");
            when(rs.getString("token")).thenReturn("tok");

            assertThatNoException().isThrownBy(() -> Mappers.mapToken(rs));
        }

        @Test
        void shouldWrapSQLExceptionInRuntimeException() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getInt(anyString())).thenThrow(new SQLException("db error"));

            assertThatThrownBy(() -> Mappers.mapToken(rs))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(SQLException.class);
        }

        @Test
        void shouldPreserveNullSlugsAndTokenStrings() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getInt("id")).thenReturn(1);
            when(rs.getString("slug")).thenReturn(null);
            when(rs.getString("token")).thenReturn(null);

            Token token = Mappers.mapToken(rs);

            assertThat(token.slug()).isNull();
            assertThat(token.token()).isNull();
        }
    }
}
