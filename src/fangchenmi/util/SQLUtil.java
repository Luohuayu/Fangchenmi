package fangchenmi.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.UUID;

public class SQLUtil {
    private static Connection connection;
    private static String table;

    public static void connection(String host, int port, String user, String password, String database, String table) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            SQLUtil.connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true", host, port, database), user, password);
            SQLUtil.table = table;
            createTableIfNotExists();
        } catch (Exception e) {
            throw new RuntimeException("MySQL连接初始化失败!", e);
        }
    }

    private static void createTableIfNotExists() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format("CREATE TABLE IF NOT EXISTS `%s` (`uuid` varchar(38) COLLATE utf8mb4_unicode_ci NOT NULL, `birth` date NOT NULL, PRIMARY KEY (`uuid`)) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;", table));
        }
    }

    private static PreparedStatement prepareStatement(String sql) throws SQLException {
        int i = 0;
        while (true){
            try {
                return connection.prepareStatement(sql);
            } catch (SQLException e) {
                e.printStackTrace();
                if (!(e.getCause() instanceof IOException) || i++ > 3) throw e;
            }
        }
    }

    public static boolean setPlayerBirth(UUID uuid, Date birth) throws SQLException {
        try (PreparedStatement ps = prepareStatement(String.format("INSERT INTO %s (`uuid`, `birth`) VALUES (?, ?)", table))) {
            ps.setString(1, uuid.toString());
            ps.setDate(2, new java.sql.Date(birth.getTime()));
            return ps.executeUpdate() > 0;
        }
    }

    public static Date queryPlayerBirth(UUID uuid) throws SQLException {
        try (PreparedStatement ps = prepareStatement(String.format("SELECT * FROM %s WHERE `uuid`=?", table))) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs != null && rs.next()) {
                    return new Date(rs.getDate("birth").getTime());
                } else {
                    return null;
                }
            }
        }
    }
}
