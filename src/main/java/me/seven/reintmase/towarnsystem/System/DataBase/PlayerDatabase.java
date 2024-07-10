package me.seven.reintmase.towarnsystem.System.DataBase;

import me.seven.reintmase.towarnsystem.System.Colorize;
import me.seven.reintmase.towarnsystem.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlayerDatabase {

    private Connection connection;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String noInDatabase;

    public PlayerDatabase(File file) throws SQLException {
        String url = "jdbc:sqlite:" + file.getPath();
        this.connection = DriverManager.getConnection(url);

        noInDatabase = Colorize.hex(Main.getInstance().getConfig().getString("messages.no-in-database"));

    }

    public void createTables() throws SQLException {
        String sqlPlayers = "CREATE TABLE IF NOT EXISTS players (\n" +
                "    uuid TEXT NOT NULL UNIQUE,\n" +
                "    warncount INTEGER DEFAULT 0,\n" +
                "    reason TEXT,\n" +
                "    warn_date_1 TEXT,\n" +
                "    warn_date_2 TEXT,\n" +
                "    warn_date_3 TEXT,\n" +
                "    warn_reason_1 TEXT,\n" +
                "    warn_reason_2 TEXT,\n" +
                "    warn_reason_3 TEXT\n" +
                ");";
        try (PreparedStatement stmt = connection.prepareStatement(sqlPlayers)) {
            stmt.execute();
        }
    }

    public void removePlayer(String uuid) throws SQLException {
        String sql = "DELETE FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.executeUpdate();
        }
    }

    public boolean playerExists(String uuid) throws SQLException {
        String sql = "SELECT 1 FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void addPlayer(String uuid) throws SQLException {
        String sqlPlayers = "INSERT INTO players (uuid) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sqlPlayers)) {
            stmt.setString(1, uuid);
            stmt.execute();
        }
    }

    public int getWarnCount(String uuid) throws SQLException {
        String sql = "SELECT warncount FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("warncount");
                } else {
                    return 0;
                }
            }
        }
    }

    public String getWarnReason(String uuid, int warnNumber) throws SQLException {
        String columnName = "warn_reason_" + warnNumber;
        String sql = "SELECT " + columnName + " FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(columnName);
                } else {
                    return null;
                }
            }
        }
    }

    public void updateWarnCount(String uuid, int newWarnCount) throws SQLException {
        String sql = "UPDATE players SET warncount = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, newWarnCount);
            stmt.setString(2, uuid);
            stmt.executeUpdate();
        }
    }

    public void addWarn(String uuid, LocalDateTime warnDate, String reason) throws SQLException {
        String sql = "SELECT warncount, warn_date_1, warn_date_2, warn_date_3 FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int warncount = rs.getInt("warncount");
                    if (warncount < 3) {
                        warncount++;
                        String dateColumn = "warn_date_" + warncount;
                        String reasonColumn = "warn_reason_" + warncount;
                        String updateSql = "UPDATE players SET warncount = ?, " + dateColumn + " = ?, " + reasonColumn + " = ? WHERE uuid = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, warncount);
                            updateStmt.setString(2, warnDate.format(formatter));
                            updateStmt.setString(3, reason);
                            updateStmt.setString(4, uuid);
                            updateStmt.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    public void reduceWarnsIfNecessary() throws SQLException {
        String sql = "SELECT uuid, warncount, warn_date_1, warn_date_2, warn_date_3 FROM players WHERE warncount > 0";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            LocalDateTime now = LocalDateTime.now();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                int warncount = rs.getInt("warncount");

                for (int i = 1; i <= 3; i++) {
                    String warnDateStr = rs.getString("warn_date_" + i);
                    if (warnDateStr != null) {
                        LocalDateTime warnDateTime = LocalDateTime.parse(warnDateStr, formatter);
                        if (java.time.Duration.between(warnDateTime, now).toDays() >= 30) {
                            warncount--;
                            updateWarnDate(uuid, i);
                        }
                    }
                }

                updateWarnCount(uuid, warncount);
            }
        } catch (SQLException e) {
            System.out.println("Ошибка");
            Bukkit.getLogger().severe("Error while reducing warns: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public void getWarnInfo(String uuid, Player player) throws SQLException {
        String sql = "SELECT warn_date_1, warn_date_2, warn_date_3 FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime warnDate1 = getLocalDateTimeOrNull(rs.getString("warn_date_1"));
                    LocalDateTime warnDate2 = getLocalDateTimeOrNull(rs.getString("warn_date_2"));
                    LocalDateTime warnDate3 = getLocalDateTimeOrNull(rs.getString("warn_date_3"));

                    player.sendMessage(Colorize.hex("&f● Player's nickname: &a" + player.getName()));
                    player.sendMessage(Colorize.hex(""));
                    player.sendMessage(Colorize.hex("&a▶ Warnings: "));
                    if (warnDate1 != null) {
                        player.sendMessage("");
                        player.sendMessage(Colorize.hex("&f▶ First warning:"));
                        player.sendMessage("");
                        player.sendMessage(Colorize.hex("&f ● Date: &a" + warnDate1.format(formatter)));
                        player.sendMessage(Colorize.hex("&f ● Reason: &a" + getWarnReason(uuid, 1)));
                        player.sendMessage("");
                    }
                    if (warnDate2 != null) {
                        player.sendMessage(Colorize.hex("&f▶ Second warning:"));
                        player.sendMessage("");
                        player.sendMessage(Colorize.hex("&f ● Date: &a" + warnDate2.format(formatter)));
                        player.sendMessage(Colorize.hex("&f ● Reason: &a" + getWarnReason(uuid, 2)));
                        player.sendMessage("");
                    }
                    if (warnDate3 != null) {
                        player.sendMessage(Colorize.hex("&f▶ Third warning:"));
                        player.sendMessage("");
                        player.sendMessage(Colorize.hex("&f ● Date: &a" + warnDate3.format(formatter)));
                        player.sendMessage(Colorize.hex("&f ● Reason: &a" + getWarnReason(uuid, 3)));
                        player.sendMessage("");
                    }
                }
            }
        }
    }

    private LocalDateTime getLocalDateTimeOrNull(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, formatter);
    }

    private void updateWarnDate(String uuid, int warnNumber) throws SQLException {
        String updateSql = "UPDATE players SET warn_date_" + warnNumber + " = NULL WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, uuid);
            stmt.executeUpdate();
        }
    }

    public void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
