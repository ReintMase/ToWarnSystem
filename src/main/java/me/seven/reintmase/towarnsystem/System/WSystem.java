package me.seven.reintmase.towarnsystem.System;

import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface WSystem {

    void giveWarn(String targetUUIDString, Player player, Player targetPlayer, String reason);

    void getWarns(String targetUUIDString, Player player);

    int getWarnsCount(String targetUUIDString);

    void unsetPlayerWarns(String targetUUIDString ,UUID targetUUID) throws ExecutionException, InterruptedException, SQLException;

    void overWarn(String targetUUIDString, Player targetPlayer, Player player) throws SQLException;

    void returnGroups(UUID targetUUID, Player player);

    void removePlayerUnsetWarns(String targetUUIDString, Player player) throws SQLException;

    void removePlayerDatabase(String targetUUIDString) throws SQLException;

    void takeWarn(String targetUUIDString, Player player, Player targetPlayer);

    void checkPlayerInBase(String targetUUIDString);
}
