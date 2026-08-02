package com.lobbomax.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Filtra la lista de comandos que el servidor envia al cliente para el
 * autocompletado (tab), usando el permiso REAL registrado en cada
 * comando (el mismo que revisa LuckPerms). No hace falta mantener una
 * lista manual por rango.
 */
public class TabCompleteListener implements Listener {

    private final VeloTabPaperPlugin plugin;
    private CommandMap commandMap;

    public TabCompleteListener(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
        this.commandMap = resolveCommandMap();
    }

    private CommandMap resolveCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo acceder al CommandMap por reflexion. "
                    + "El filtrado por permiso real quedara desactivado. Error: " + e.getMessage());
            return null;
        }
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

        Iterator<String> iterator = event.getCommands().iterator();
        while (iterator.hasNext()) {
            String rawCommand = iterator.next();
            String baseCommand = stripPluginPrefix(rawCommand).toLowerCase();

            if (alwaysShow.contains(baseCommand)) {
                continue;
            }

            if (forceHide.contains(baseCommand)) {
                iterator.remove();
                continue;
            }

            if (commandMap != null) {
                Command command = commandMap.getCommand(rawCommand);
                if (command == null) {
                    command = commandMap.getCommand(baseCommand);
                }
                if (command != null) {
                    String permission = command.getPermission();
                    if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
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
