package me.seven.reintmase.towarnsystem;

import lombok.Getter;
import me.seven.reintmase.towarnsystem.Commands.PluginCommands;
import me.seven.reintmase.towarnsystem.System.DataBase.PlayerDatabase;
import me.seven.reintmase.towarnsystem.System.DataBase.PlayerUnsetDatabase;
import me.seven.reintmase.towarnsystem.System.Permissions.PermissionsSystem;
import me.seven.reintmase.towarnsystem.System.WarnSystem;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

@Getter
public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;

    private PlayerDatabase playerDatabase;
    private PlayerUnsetDatabase playerUnsetDatabase;
    private WarnSystem warnSystem;
    private LuckPerms luckPerms;
    private PermissionsSystem permissionsSystem;
    private PluginCommands pluginCommands;

    private final String connectionUrl = "jdbc:sqlite:" + getDataFolder().getAbsolutePath() + File.separator + "database.db";
    private final String connectionUrlUnset = "jdbc:sqlite:" + getDataFolder().getAbsolutePath() + File.separator + "unsetDatabase.db";
    private final File dbFile = new File(getDataFolder(), "database.db");
    private final File unsetDbFile = new File(getDataFolder(), "unsetDatabase.db");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;

        connectBase();
        connectUnsetBase();

        luckPerms = getServer().getServicesManager().load(LuckPerms.class);
        permissionsSystem = new PermissionsSystem();
        warnSystem = new WarnSystem();
        pluginCommands = new PluginCommands();
        getCommand("kwarn").setExecutor(pluginCommands);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                playerDatabase.reduceWarnsIfNecessary();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, 0L, 60 * 20 * 60);
    }

    @Override
    public void onDisable() {
        try {
            if (playerDatabase != null) {
                playerDatabase.closeConnection();
            }
            if (playerUnsetDatabase != null) {
                playerUnsetDatabase.closeConnection();
            }
        } catch (SQLException e) {
            getLogger().severe("Failed to close database connection: " + e.getMessage());
            e.printStackTrace();
        }

        getLogger().info("Plugin disabled successfully!");
    }

    private void connectBase() {
        try {
            playerDatabase = new PlayerDatabase(dbFile);
            playerDatabase.createTables();
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to player database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void connectUnsetBase() {
        try {
            playerUnsetDatabase = new PlayerUnsetDatabase(unsetDbFile);
            playerUnsetDatabase.createTables();
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to unset database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
