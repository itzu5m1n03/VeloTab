/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import me.itzusman.velotab.common.Constants;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VeloTabCommand implements CommandExecutor, TabCompleter {

    private final VeloTabPaperPlugin plugin;

    public VeloTabCommand(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(plugin.getLangMessage("info-header"));
            sender.sendMessage(plugin.getLangMessage("info-version").replace("{version}", Constants.VERSION));
            sender.sendMessage(plugin.getLangMessage("info-creator").replace("{link}", Constants.WEBSITE));
            sender.sendMessage(plugin.getLangMessage("info-luckperms").replace("{status}", plugin.isLuckPermsPresent() ? "§aHabilitado" : "§cDeshabilitado"));
            sender.sendMessage(plugin.getLangMessage("info-placeholderapi").replace("{status}", plugin.isPlaceholderApiPresent() ? "§aHabilitado" : "§cDeshabilitado"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("velotab.admin")) {
                sender.sendMessage(plugin.getLangMessage("no-permission"));
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getLangMessage("reload-success"));
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cSolo jugadores pueden usar este comando.");
                return true;
            }
            plugin.getDisplayManager().toggleScoreboard((Player) sender);
            sender.sendMessage("§aHas cambiado la visibilidad de tu Scoreboard.");
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§b§lVeloTab §8- §7Ayuda");
        sender.sendMessage("§8» §b/velotab info §8- §7Muestra información del plugin.");
        sender.sendMessage("§8» §b/velotab toggle §8- §7Muestra/Oculta tu Scoreboard.");
        if (sender.hasPermission("velotab.admin")) {
            sender.sendMessage("§8» §b/velotab reload §8- §7Recarga toda la configuración.");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>(Arrays.asList("info", "help", "toggle"));
            if (sender.hasPermission("velotab.admin")) sub.add("reload");
            return sub.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
