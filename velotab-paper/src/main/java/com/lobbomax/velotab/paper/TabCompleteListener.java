package com.lobbomax.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Filtra la lista de comandos que el servidor envia al cliente para el
 * autocompletado (tab), basandose estrictamente en los permisos de LuckPerms.
 */
public class TabCompleteListener implements Listener {

    private final VeloTabPaperPlugin plugin;

    public TabCompleteListener(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("Tab_Hide.enable", true);
    }

    private Set<String> forceHide() {
        Set<String> set = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("Tab_Hide.force_hide")) {
            set.add(s.toLowerCase());
        }
        return set;
    }

    private Set<String> alwaysShow() {
        Set<String> set = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("Tab_Hide.always_show")) {
            set.add(s.toLowerCase());
        }
        return set;
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("velotab.bypass")) {
            return;
        }

        Set<String> forceHide = forceHide();
        Set<String> alwaysShow = alwaysShow();
        CommandMap commandMap = Bukkit.getCommandMap();

        Iterator<String> iterator = event.getCommands().iterator();
        while (iterator.hasNext()) {
            String rawCommand = iterator.next();
            String baseCommand = stripPluginPrefix(rawCommand).toLowerCase();

            // 1. Whitelist
            if (alwaysShow.contains(baseCommand)) {
                continue;
            }

            // 2. Blacklist
            if (forceHide.contains(baseCommand)) {
                iterator.remove();
                continue;
            }

            // 3. Filtrado por Propiedad y Permiso
            Command command = commandMap.getCommand(rawCommand);
            if (command == null) {
                command = commandMap.getCommand(baseCommand);
            }

            if (command != null) {
                // Intentamos sacar el permiso oficial
                String permission = command.getPermission();
                
                // Intentamos identificar al plugin dueño del comando
                String ownerPlugin = null;
                if (command instanceof PluginCommand) {
                    ownerPlugin = ((PluginCommand) command).getPlugin().getName().toLowerCase();
                }

                // A) Si tiene permiso oficial, es la prioridad
                if (permission != null && !permission.isEmpty()) {
                    if (!player.hasPermission(permission)) {
                        iterator.remove();
                        continue;
                    }
                } 
                
                // B) Si no tiene permiso oficial pero sabemos de que plugin es,
                // forzamos un chequeo de permiso "plugin.comando"
                if (ownerPlugin != null) {
                    String guessed = ownerPlugin + "." + baseCommand;
                    if (!player.hasPermission(guessed) && !player.hasPermission("velotab.admin")) {
                        iterator.remove();
                        continue;
                    }
                }
                
                // C) Si es un comando con prefijo y no hemos pasado los filtros anteriores, fuera.
                if (rawCommand.contains(":") && !player.hasPermission("velotab.admin")) {
                    String guessed = rawCommand.replace(":", ".");
                    if (!player.hasPermission(guessed)) {
                        iterator.remove();
                    }
                }
            } else if (rawCommand.contains(":")) {
                // Comandos huerfanos con prefijo
                iterator.remove();
            }
        }
    }

    private String stripPluginPrefix(String command) {
        int colonIndex = command.indexOf(':');
        return colonIndex >= 0 ? command.substring(colonIndex + 1) : command;
    }
}
