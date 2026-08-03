/*
 * Copyright (c) 2026 ItzUsman (itzusman.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VeloTabCommand implements CommandExecutor {

    private final VeloTabPaperPlugin plugin;

    public VeloTabCommand(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("velotab.admin")) {
            sender.sendMessage(plugin.getLangMessage("no-permission"));
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.loadLang();
                sender.sendMessage(plugin.getLangMessage("reload-success"));
                return true;
            }
            if (args[0].equalsIgnoreCase("info")) {
                sender.sendMessage(plugin.getLangMessage("info-header"));
                sender.sendMessage(plugin.getLangMessage("info-version").replace("{version}", plugin.getDescription().getVersion()));
                sender.sendMessage(plugin.getLangMessage("info-creator").replace("{link}", "itzusman.netlify.app"));
                sender.sendMessage(plugin.getLangMessage("info-luckperms").replace("{status}", plugin.isLuckPermsPresent() ? plugin.getLangMessage("status-enabled") : plugin.getLangMessage("status-disabled")));
                sender.sendMessage(plugin.getLangMessage("info-placeholderapi").replace("{status}", plugin.isPlaceholderApiPresent() ? plugin.getLangMessage("status-enabled") : plugin.getLangMessage("status-disabled")));
                return true;
            }
        }
        
        sender.sendMessage("§e[VeloTab] Uso: /velotab <info|reload>");
        return true;
    }
}
