package me.seven.reintmase.towarnsystem.System;

import me.seven.reintmase.towarnsystem.System.DataBase.PlayerDatabase;
import me.seven.reintmase.towarnsystem.Main;
import me.seven.reintmase.towarnsystem.System.DataBase.PlayerUnsetDatabase;
import me.seven.reintmase.towarnsystem.System.Permissions.PermissionsSystem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class WarnSystem implements WSystem {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlayerDatabase playerDatabase;
    private final PlayerUnsetDatabase playerUnsetDatabase;
    private final PermissionsSystem permissionsSystem;

    private final FileConfiguration config;

    private final String youGiveWarnMessage, youHasWarnMessage, broadcastGiveWarnMessage, checkWarns, playerOverWarnsMessage, youHasOverWarnsMessage, returnPrivs, playerAlreadyHasZero, youTakeWarnMessage, takeWarnMessage, broadcastReturnPrivs;

    public WarnSystem() {
        playerDatabase = Main.getInstance().getPlayerDatabase();
        playerUnsetDatabase = Main.getInstance().getPlayerUnsetDatabase();
        permissionsSystem = Main.getInstance().getPermissionsSystem();
        config = Main.getInstance().getConfig();

        youGiveWarnMessage = Colorize.hex(config.getString("messages.you-give-warn"));
        youHasWarnMessage = Colorize.hex(config.getString("messages.you-has-warn"));
        broadcastGiveWarnMessage = Colorize.hex(config.getString("messages.broadcast-give-warn"));
        playerOverWarnsMessage = Colorize.hex(config.getString("messages.broadcast-player-over-warns"));
        youHasOverWarnsMessage = Colorize.hex(config.getString("messages.you-has-over-warns"));
        returnPrivs = Colorize.hex(config.getString("messages.return-privs"));
        broadcastReturnPrivs = Colorize.hex(config.getString("messages.broadcast-return-privs"));
        playerAlreadyHasZero = Colorize.hex(config.getString("messages.player-already-has-zero"));
        youTakeWarnMessage = Colorize.hex(config.getString("messages.you-take-warn"));
        takeWarnMessage = Colorize.hex(config.getString("messages.take-warn"));
        checkWarns = Colorize.hex(config.getString("messages.check-warns"));
    }

    @Override
    public void giveWarn(String targetUUIDString, Player player, Player targetPlayer, String reason) {
        checkPlayerInBase(targetUUIDString);

        try {
            LocalDateTime now = LocalDateTime.now();

            int warnCountTargetPlayer = playerDatabase.getWarnCount(targetUUIDString);
            int alreadyWarnCount = warnCountTargetPlayer + 1;

            playerDatabase.addWarn(targetUUIDString, now, reason);

            String editedYouGiveWarnMessage = youGiveWarnMessage;
            editedYouGiveWarnMessage = editedYouGiveWarnMessage.replace("%player%", targetPlayer.getName());
            editedYouGiveWarnMessage = editedYouGiveWarnMessage.replace("%warn-counts%", alreadyWarnCount + "");

            player.sendMessage(editedYouGiveWarnMessage);

            String editedYouHasWarnMessage = youHasWarnMessage;
            editedYouHasWarnMessage = editedYouHasWarnMessage.replace("%player%", player.getName());
            editedYouHasWarnMessage = editedYouHasWarnMessage.replace("%warn-counts%", alreadyWarnCount + "");

            targetPlayer.sendMessage(editedYouHasWarnMessage);

            String editedBroadCastGiveWarnMessage = broadcastGiveWarnMessage;
            editedBroadCastGiveWarnMessage = editedBroadCastGiveWarnMessage.replace("%target-player%", targetPlayer.getName());
            editedBroadCastGiveWarnMessage = editedBroadCastGiveWarnMessage.replace("%admin-player%", player.getName());
            editedBroadCastGiveWarnMessage = editedBroadCastGiveWarnMessage.replace("%warn-counts%", alreadyWarnCount + "");
            editedBroadCastGiveWarnMessage = editedBroadCastGiveWarnMessage.replace("%reason%", reason);

            Bukkit.broadcastMessage(editedBroadCastGiveWarnMessage);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getWarns(String targetUUIDString, Player player) {
        checkPlayerInBase(targetUUIDString);

        try {
            int warnCountTargetPlayer = playerDatabase.getWarnCount(targetUUIDString);

            String editedCheckWarns = checkWarns;
            editedCheckWarns = editedCheckWarns.replace("%warn-counts%", warnCountTargetPlayer + "");
            player.sendMessage(editedCheckWarns);

        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getWarnsCount(String targetUUIDString) {
        try {
            return playerDatabase.getWarnCount(targetUUIDString);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unsetPlayerWarns(String targetUUIDString, UUID targetPlayerUUID) throws ExecutionException, InterruptedException, SQLException {
        playerUnsetDatabase.addPlayerUnset(targetUUIDString, permissionsSystem.getPlayerGroup(targetPlayerUUID));
    }

    @Override
    public void overWarn(String targetUUIDString, Player targetPlayer, Player player) throws SQLException {
        try {
            unsetPlayerWarns(targetUUIDString, targetPlayer.getUniqueId());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        permissionsSystem.setPlayerToGroup(targetPlayer.getUniqueId());

        String editedPlayerOverWarnsMessage = playerOverWarnsMessage;
        editedPlayerOverWarnsMessage = editedPlayerOverWarnsMessage.replace("%player%", targetPlayer.getName());
        Bukkit.broadcastMessage(editedPlayerOverWarnsMessage);

        targetPlayer.sendMessage(youHasOverWarnsMessage);
    }

    @Override
    public void returnGroups(UUID targetUUID, Player targetPlayer) {
        permissionsSystem.setPlayerToPreviousGroup(targetUUID);
        targetPlayer.sendMessage(returnPrivs);

        String editedBroadcastReturnPrivs = broadcastReturnPrivs;
        editedBroadcastReturnPrivs = editedBroadcastReturnPrivs.replace("%player%", targetPlayer.getName());
        Bukkit.broadcastMessage(editedBroadcastReturnPrivs);
    }

    @Override
    public void removePlayerUnsetWarns(String targetUUIDString, Player player) throws SQLException {
        playerUnsetDatabase.removePlayer(targetUUIDString, player);
    }

    @Override
    public void removePlayerDatabase(String targetUUIDString) throws SQLException {
        playerDatabase.removePlayer(targetUUIDString);
    }

    @Override
    public void takeWarn(String targetUUIDString, Player player, Player targetPlayer) {
        checkPlayerInBase(targetUUIDString);

        try {
            int warnCountTargetPlayer = playerDatabase.getWarnCount(targetUUIDString);

            if(warnCountTargetPlayer <= 0){
                player.sendMessage(playerAlreadyHasZero);
                return;
            }

            playerDatabase.updateWarnCount(targetUUIDString, warnCountTargetPlayer - 1);

            String editedYouTakeWarnMessage = youTakeWarnMessage;
            editedYouTakeWarnMessage = editedYouTakeWarnMessage.replace("%player%", targetPlayer.getName());
            editedYouTakeWarnMessage = editedYouTakeWarnMessage.replace("%warn-counts%", warnCountTargetPlayer + "");
            player.sendMessage(editedYouTakeWarnMessage);

            if(targetPlayer != null){
                String editedTakeWarnMessage = takeWarnMessage;
                editedTakeWarnMessage = editedTakeWarnMessage.replace("%player%", player.getName());
                editedTakeWarnMessage = editedTakeWarnMessage.replace("%warn-counts%", warnCountTargetPlayer + "");
                targetPlayer.sendMessage(editedTakeWarnMessage);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void checkPlayerInBase(String targetUUIDString) {
        try {
            if(!playerDatabase.playerExists(targetUUIDString)){
                playerDatabase.addPlayer(targetUUIDString);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
