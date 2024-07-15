package me.seven.reintmase.towarnsystem.System.DataBase;

import me.seven.reintmase.towarnsystem.System.Colorize;
import me.seven.reintmase.towarnsystem.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class PlayerUnsetDatabase {

    private Connection connection;

    private final FileConfiguration config;
    private final String noInDatabase;

    public PlayerUnsetDatabase(File file) throws SQLException {
        String url = "jdbc:sqlite:" + file.getPath();
        this.connection = DriverManager.getConnection(url);
        createTables();

        config = Main.getInstance().getConfig();
        noInDatabase = Colorize.hex(config.getString("messages.no-in-database"));
    }

    public void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS player_unset (\n"
                + "    uuid TEXT NOT NULL,\n"
                + "    unset_time TIMESTAMP NOT NULL,\n"
                + "    previous_groups TEXT NOT NULL,\n"
                + "    PRIMARY KEY (uuid)\n"
                + ");";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    public void addPlayerUnset(String uuid, Set<String> previousGroups) throws SQLException {
        if (isPlayerUnset(uuid)) {
            return;
        }

        if (previousGroups == null) {
            previousGroups = new HashSet<>();
        }

        String previousGroupsStr = String.join(",", previousGroups);
        String sql = "INSERT INTO player_unset (uuid, unset_time, previous_groups) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, previousGroupsStr);
            stmt.executeUpdate();
        }
    }

    public Set<String> getPreviousGroups(String uuid) throws SQLException {
        String sql = "SELECT previous_groups FROM player_unset WHERE uuid = ? AND unset_time >= ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String previousGroupsStr = rs.getString("previous_groups");
                return new HashSet<>(Set.of(previousGroupsStr.split(",")));
            }
        }
        return new HashSet<>();
    }

    public boolean isPlayerUnset(String uuid) throws SQLException {
        String sql = "SELECT 1 FROM player_unset WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public void removePlayer(String uuid, Player player) throws SQLException {
        if(!isPlayerUnset(uuid)){
            player.sendMessage(noInDatabase);
            return;
        }

        String sql = "DELETE FROM player_unset WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
