package com.lobbomax.velotab.paper;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
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
 * Filtra comandos y ARGUMENTOS basandose en permisos de LuckPerms.
 */
public class TabCompleteListener implements Listener {

    private final VeloTabPaperPlugin plugin;

    public TabCompleteListener(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (!plugin.getConfig().getBoolean("Tab_Hide.enable", true)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("velotab.bypass")) return;

        Set<String> forceHide = getSet("Tab_Hide.force_hide");
        Set<String> alwaysShow = getSet("Tab_Hide.always_show");
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

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        Player player = (Player) event.getSender();
        if (player.hasPermission("velotab.bypass")) return;

        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) return;

        String[] args = buffer.substring(1).split(" ");
        if (args.length == 0) return;

        String raw = args[0];
        String base = stripPrefix(raw).toLowerCase();

        Command cmd = Bukkit.getCommandMap().getCommand(raw);
        if (cmd == null) cmd = Bukkit.getCommandMap().getCommand(base);

        // Si el jugador no tiene permiso para el comando base, cancelamos todas las sugerencias de argumentos
        if (!hasPermission(player, cmd, raw, base)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommandPreProcess(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("Tab_Hide.enable", true)) return;
        
        Player player = event.getPlayer();
        if (player.hasPermission("velotab.bypass")) return;

        String message = event.getMessage().substring(1);
        String raw = message.split(" ")[0];
        String base = stripPrefix(raw).toLowerCase();

        Set<String> forceHide = getSet("Tab_Hide.force_hide");
        if (forceHide.contains(base)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLangMessage("command-blocked"));
            return;
        }

        Command cmd = Bukkit.getCommandMap().getCommand(raw);
        if (cmd == null) cmd = Bukkit.getCommandMap().getCommand(base);

        if (!hasPermission(player, cmd, raw, base)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLangMessage("no-permission"));
        }
    }

    private boolean hasPermission(Player player, Command cmd, String raw, String base) {
        if (cmd != null) {
            String perm = cmd.getPermission();
            if (perm != null && !perm.isEmpty()) {
                return player.hasPermission(perm);
            }
            
            // Si no tiene permiso oficial, buscamos por plugin
            if (cmd instanceof PluginCommand) {
                String pluginName = ((PluginCommand) cmd).getPlugin().getName().toLowerCase();
                return player.hasPermission(pluginName + "." + base) || player.hasPermission("velotab.admin");
            }
        }
        
        // Comandos con prefijo sin dueño claro
        if (raw.contains(":")) {
            return player.hasPermission(raw.replace(":", ".")) || player.hasPermission("velotab.admin");
        }
        
        return true; // Por defecto permitimos si no hay forma de identificarlo (evita falsos positivos)
    }

    private Set<String> getSet(String path) {
        Set<String> set = new HashSet<>();
        for (String s : plugin.getConfig().getStringList(path)) set.add(s.toLowerCase());
        return set;
    }

    private String stripPrefix(String cmd) {
        int i = cmd.indexOf(':');
        return i >= 0 ? cmd.substring(i + 1) : cmd;
    }
}
