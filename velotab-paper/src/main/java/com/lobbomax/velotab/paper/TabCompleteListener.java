package com.lobbomax.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Filtra la lista de comandos que el servidor envia al cliente para el
 * autocompletado (tab), usando el permiso REAL registrado en cada
 * comando (el mismo que revisa LuckPerms).
 */
public class TabCompleteListener implements Listener {

    private final VeloTabPaperPlugin plugin;

    public TabCompleteListener(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("Tab_Hide.enable", true);
    }

    private boolean shouldHidePrefixed() {
        return plugin.getConfig().getBoolean("Tab_Hide.hide_prefixed_commands", true);
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

        boolean isAdmin = player.hasPermission("velotab.admin");
        Set<String> forceHide = forceHide();
        Set<String> alwaysShow = alwaysShow();
        CommandMap commandMap = Bukkit.getCommandMap();

        Iterator<String> iterator = event.getCommands().iterator();
        while (iterator.hasNext()) {
            String rawCommand = iterator.next();
            String baseCommand = stripPluginPrefix(rawCommand).toLowerCase();

            // 1. Siempre mostrar
            if (alwaysShow.contains(baseCommand)) {
                continue;
            }

            // 2. Forzar ocultar
            if (forceHide.contains(baseCommand)) {
                iterator.remove();
                continue;
            }

            // 3. Ocultar comandos con prefijo (plugin:comando) para no-admins
            // Esto limpia el tab de "chatmanager:staffchat", etc.
            if (shouldHidePrefixed() && !isAdmin && rawCommand.contains(":")) {
                iterator.remove();
                continue;
            }

            // 4. Filtrar por permiso real
            if (commandMap != null) {
                Command command = commandMap.getCommand(rawCommand);
                if (command == null) {
                    command = commandMap.getCommand(baseCommand);
                }

                if (command != null) {
                    if (!command.testPermissionSilent(player)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private String stripPluginPrefix(String command) {
        int colonIndex = command.indexOf(':');
        return colonIndex >= 0 ? command.substring(colonIndex + 1) : command;
    }
}
