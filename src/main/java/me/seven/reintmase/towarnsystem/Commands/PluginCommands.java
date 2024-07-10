package me.seven.reintmase.towarnsystem.Commands;

import lombok.SneakyThrows;
import me.seven.reintmase.towarnsystem.Main;
import me.seven.reintmase.towarnsystem.System.Colorize;
import me.seven.reintmase.towarnsystem.System.DataBase.PlayerDatabase;
import me.seven.reintmase.towarnsystem.System.WarnSystem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class PluginCommands implements CommandExecutor {

    private final WarnSystem warnSystem;
    private final FileConfiguration config;
    private final PlayerDatabase playerDatabase;

    private final String howToUse;
    private final String noPermissions;

    public PluginCommands() {
        Main plugin = Main.getInstance();
        this.warnSystem = plugin.getWarnSystem();
        this.config = plugin.getConfig();
        this.playerDatabase = plugin.getPlayerDatabase();

        this.howToUse = Colorize.hex(config.getString("messages.how-to-use"));
        this.noPermissions = Colorize.hex(config.getString("messages.no-permissions"));
    }

    @SneakyThrows
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player!");
            return true;
        }

        Player player = (Player) sender;
        String playerUUIDString = player.getUniqueId().toString();

        if (!player.hasPermission("warnsystem.commands")) {
            player.sendMessage(noPermissions);
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(howToUse);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                handleGiveCommand(player, args);
                return true;

            case "take":
                handleTakeCommand(player, args);
                return true;

            case "return":
                handleReturnCommand(player, args);
                return true;

            case "myinfo":
                handleMyInfoCommand(player);
                return true;

            case "help":
                handleHelpCommand(player);
                return true;

            case "info":
                handleInfoCommand(player, args);
                return true;

            default:
                player.sendMessage(howToUse);
                return true;
        }
    }

    private void handleGiveCommand(Player player, String[] args) throws SQLException {
        if (!player.hasPermission("warnsystem.give_warn")) {
            player.sendMessage(noPermissions);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(howToUse);
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage("Player not found or not online.");
            return;
        }

        String reason = Arrays.stream(args, 2, args.length).collect(Collectors.joining(" "));

        String targetUUIDString = targetPlayer.getUniqueId().toString();
        warnSystem.giveWarn(targetUUIDString, player, targetPlayer, reason);

        if (warnSystem.getWarnsCount(targetUUIDString) >= 3) {
            warnSystem.overWarn(targetUUIDString, targetPlayer, player);
        }
    }

    private void handleTakeCommand(Player player, String[] args) {
        if (!player.hasPermission("warnsystem.take_warn")) {
            player.sendMessage(noPermissions);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(howToUse);
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage("Player not found or not online.");
            return;
        }

        String targetUUIDString = targetPlayer.getUniqueId().toString();
        warnSystem.takeWarn(targetUUIDString, player, targetPlayer);
    }

    private void handleReturnCommand(Player player, String[] args) throws SQLException {
        if (!player.hasPermission("warnsystem.return")) {
            player.sendMessage(noPermissions);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(howToUse);
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage("Player not found or not online.");
            return;
        }

        String targetUUIDString = targetPlayer.getUniqueId().toString();
        warnSystem.returnGroups(targetPlayer.getUniqueId(), targetPlayer);
        warnSystem.removePlayerDatabase(targetUUIDString);
        warnSystem.removePlayerUnsetWarns(targetUUIDString, player);
    }

    private void handleMyInfoCommand(Player player) {
        if (!player.hasPermission("warnsystem.my_info")) {
            player.sendMessage(noPermissions);
            return;
        }
        String playerUUIDString = player.getUniqueId().toString();
        warnSystem.getWarns(playerUUIDString, player);
    }

    private void handleHelpCommand(Player player) {
        player.sendMessage("");
        player.sendMessage(Colorize.hex("&a/kwarn help - &fdisplays this message"));
        player.sendMessage(Colorize.hex("&a/kwarn give <nickname> <reason> - &fwarrants"));
        player.sendMessage(Colorize.hex("&a/kwarn return <nickname> - &freturns the privilege to the player"));
        player.sendMessage(Colorize.hex("&a/kwarn take <nickname> - &ftakes away 1 player's warp"));
        player.sendMessage(Colorize.hex("&a/kwarn myinfo - &fto see your warrants"));
        player.sendMessage(Colorize.hex("&a/kwarn info <nickname> - &fplayer data"));
        player.sendMessage("");
    }

    private void handleInfoCommand(Player player, String[] args) throws SQLException {
        if (!player.hasPermission("warnsystem.info")) {
            player.sendMessage(noPermissions);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(howToUse);
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            playerDatabase.getWarnInfo(targetPlayer.getUniqueId().toString(), targetPlayer);
        } else {
            Player offlinePlayer = Bukkit.getOfflinePlayer(args[1]).getPlayer();
            if (offlinePlayer != null) {
                playerDatabase.getWarnInfo(offlinePlayer.getUniqueId().toString(), offlinePlayer);
            } else {
                player.sendMessage("Player not found.");
            }
        }
    }
}
