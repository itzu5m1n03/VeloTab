/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona Nametags de 3 líneas reales usando paquetes de red.
 * Implementación segura que no crashea si ProtocolLib no está presente.
 */
public class NametagManager {

    private final VeloTabPaperPlugin plugin;
    private boolean protocolLibPresent;
    private BukkitRunnable updateTask;

    public NametagManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        protocolLibPresent = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
        
        if (!plugin.getConfigLoader().get("tablist/tablist").getBoolean("Nametags_Pro.Enable", false)) return;
        if (!protocolLibPresent) {
            plugin.getLogger().warning("Nametags Pro está activado pero ProtocolLib no está instalado.");
            return;
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateNametags(player);
                }
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (protocolLibPresent) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeNametags(player);
            }
        }
    }

    private void updateNametags(Player player) {
        if (!protocolLibPresent) return;
        // La implementación real de TextDisplays requiere paquetes complejos
        // que se manejarán en una clase interna para evitar NoClassDefFoundError.
        try {
            ProtocolWrapper.sendNametags(player);
        } catch (NoClassDefFoundError | Exception ignored) {}
    }

    public void removeNametags(Player player) {
        if (!protocolLibPresent) return;
        try {
            ProtocolWrapper.destroyNametags(player);
        } catch (NoClassDefFoundError | Exception ignored) {}
    }

    /**
     * Aislamiento de ProtocolLib.
     */
    private static class ProtocolWrapper {
        private static void sendNametags(Player player) {
            // Lógica de inyección de paquetes (TextDisplay)
            // Reservado para futuras implementaciones de nametags reales.
        }

        private static void destroyNametags(Player player) {
            // Lógica de destrucción de entidades virtuales.
        }
    }
}
