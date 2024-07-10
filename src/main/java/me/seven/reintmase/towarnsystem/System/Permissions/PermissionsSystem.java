package me.seven.reintmase.towarnsystem.System.Permissions;

import me.seven.reintmase.towarnsystem.Main;
import me.seven.reintmase.towarnsystem.System.DataBase.PlayerUnsetDatabase;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PermissionsSystem {

    private final LuckPerms luckPerms;
    private final FileConfiguration config;
    private final PlayerUnsetDatabase playerUnsetDatabase;

    public PermissionsSystem() {
        luckPerms = Main.getInstance().getLuckPerms();
        config = Main.getInstance().getConfig();
        playerUnsetDatabase = Main.getInstance().getPlayerUnsetDatabase();
    }

    public void setPlayerToGroup(UUID playerUUID) {
        if (luckPerms == null) {
            Bukkit.getLogger().severe("LuckPerms API is not loaded!");
            return;
        }

        luckPerms.getUserManager().loadUser(playerUUID).thenAccept(user -> {
            if (user != null) {
                user.data().clear(NodeType.INHERITANCE::matches);
                String groupName = config.getString("group.name", "default");

                Node node = InheritanceNode.builder(groupName).build();
                user.data().add(node);

                luckPerms.getUserManager().saveUser(user).join();
            } else {
                Bukkit.getLogger().severe("User with UUID " + playerUUID + " not found!");
            }
        });
    }

    public void setPlayerToPreviousGroup(UUID playerUUID) {
        if (luckPerms == null) {
            Bukkit.getLogger().severe("LuckPerms API is not loaded!");
            return;
        }

        luckPerms.getUserManager().loadUser(playerUUID).thenAccept(user -> {
            if (user != null) {
                user.data().clear(NodeType.INHERITANCE::matches);
                try {
                    Set<String> groups = playerUnsetDatabase.getPreviousGroups(playerUUID.toString());

                    for (String group : groups) {
                        Node node = InheritanceNode.builder(group).build();
                        user.data().add(node);
                        luckPerms.getUserManager().saveUser(user).join();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Bukkit.getLogger().severe("User with UUID " + playerUUID + " not found!");
            }
        });
    }

    public Set<String> getPlayerGroup(UUID playerUUID) throws ExecutionException, InterruptedException {
        if (luckPerms == null) {
            Bukkit.getLogger().severe("LuckPerms API is not loaded!");
            return null;
        }

        User user = luckPerms.getUserManager().loadUser(playerUUID).get();
        Set<String> groups = user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());

        for(String group : groups) {
            return Collections.singleton(group);
        }

        return null;
    }
}