/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Filtra comandos del autocompletado basándose estrictamente en permisos y configuración de seguridad.
 */
public class TabCompleteListener implements Listener {

    private final VeloTabPaperPlugin plugin;

    public TabCompleteListener(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        FileConfiguration config = plugin.getCustomConfig("security");
        if (!config.getBoolean("Command_Hiding.Enable", true)) return;

        Player player = event.getPlayer();
        if (player.hasPermission(config.getString("Command_Hiding.Bypass_Permission", "velotab.bypass"))) return;

        Set<String> forceHide = getSet("Command_Hiding.Force_Hide");
        Set<String> alwaysShow = getSet("Command_Hiding.Always_Show");
        CommandMap commandMap = Bukkit.getCommandMap();

        Iterator<String> iterator = event.getCommands().iterator();
        while (iterator.hasNext()) {
            String raw = iterator.next();
            String base = stripPrefix(raw).toLowerCase();

            if (alwaysShow.contains(base)) continue;
            if (forceHide.contains(base)) {
                iterator.remove();
                continue;
            }

            Command cmd = commandMap.getCommand(raw);
            if (cmd == null) cmd = commandMap.getCommand(base);

            if (!hasPermission(player, cmd, raw, base)) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        Player player = (Player) event.getSender();
        
        FileConfiguration config = plugin.getCustomConfig("security");
        if (!config.getBoolean("Command_Hiding.Enable", true)) return;
        if (player.hasPermission(config.getString("Command_Hiding.Bypass_Permission", "velotab.bypass"))) return;

        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) return;

        String[] args = buffer.substring(1).split(" ");
        if (args.length == 0) return;

        String raw = args[0];
        String base = stripPrefix(raw).toLowerCase();

        Command cmd = Bukkit.getCommandMap().getCommand(raw);
        if (cmd == null) cmd = Bukkit.getCommandMap().getCommand(base);

        if (!hasPermission(player, cmd, raw, base)) {
            event.setCancelled(true);
        }
    }

    private boolean hasPermission(Player player, Command cmd, String raw, String base) {
        if (cmd != null) {
            String perm = cmd.getPermission();
            if (perm != null && !perm.isEmpty()) {
                return player.hasPermission(perm);
            }
            
            if (cmd instanceof PluginCommand) {
                String pluginName = ((PluginCommand) cmd).getPlugin().getName().toLowerCase();
                return player.hasPermission(pluginName + ".command." + base) || 
                       player.hasPermission(pluginName + "." + base);
            }
        }
        
        if (raw.contains(":")) {
            return player.hasPermission(raw.replace(":", "."));
        }
        
        return player.isOp(); 
    }

    private Set<String> getSet(String path) {
        Set<String> set = new HashSet<>();
        for (String s : plugin.getCustomConfig("security").getStringList(path)) set.add(s.toLowerCase());
        return set;
    }

    private String stripPrefix(String cmd) {
        int i = cmd.indexOf(':');
        return i >= 0 ? cmd.substring(i + 1) : cmd;
    }
}
