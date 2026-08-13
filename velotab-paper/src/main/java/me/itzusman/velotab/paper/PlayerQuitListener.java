/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Escucha eventos de desconexión para limpiar datos en memoria y prevenir fugas.
 */
public class PlayerQuitListener implements Listener {

    private final VeloTabPaperPlugin plugin;

    public PlayerQuitListener(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().removePlayer(event.getPlayer());
        }
        
        if (plugin.getBossBarManager() != null) {
            plugin.getBossBarManager().removeForPlayer(event.getPlayer());
        }
        
        if (plugin.getChatFormatListener() != null) {
            plugin.getChatFormatListener().removePlayer(event.getPlayer());
        }
    }
}
