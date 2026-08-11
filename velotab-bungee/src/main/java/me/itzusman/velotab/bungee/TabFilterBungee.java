/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.TabCompleteResponseEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Iterator;

public class TabFilterBungee implements Listener {

    private final VeloTabBungeePlugin plugin;

    public TabFilterBungee(VeloTabBungeePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabResponse(TabCompleteResponseEvent event) {
        if (!plugin.getConfig().getBoolean("Tab_Hide.enable", true)) return;
        if (!(event.getReceiver() instanceof ProxiedPlayer)) return;

        ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
        if (player.hasPermission("velotab.bypass")) return;

        Iterator<String> iterator = event.getSuggestions().iterator();
        while (iterator.hasNext()) {
            String suggestion = iterator.next().toLowerCase();
            String base = suggestion.contains(":") ? suggestion.substring(suggestion.indexOf(":") + 1) : suggestion;

            if (plugin.getAlwaysShow().contains(base)) continue;
            if (plugin.getForceHide().contains(base)) {
                iterator.remove();
                continue;
            }

            // Ocultación basada en permisos
            if (suggestion.contains(":")) {
                String guessed = suggestion.replace(":", ".");
                if (!player.hasPermission(guessed) && !player.hasPermission(suggestion)) {
                    iterator.remove();
                }
            } else {
                // Comprobamos si el jugador tiene permiso para el comando base
                if (!player.hasPermission("bungeecord.command." + base) && 
                    !player.hasPermission(base) && 
                    !player.hasPermission("proxy." + base)) {
                    iterator.remove();
                }
            }
        }
    }
}
