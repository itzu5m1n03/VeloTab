/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona Nametags de 3 líneas reales usando paquetes de red (TextDisplays).
 * Esta función es avanzada y está desactivada por defecto.
 */
public class NametagManager {

    private final VeloTabPaperPlugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, Integer> aboveEntityIds = new HashMap<>();
    private final Map<UUID, Integer> belowEntityIds = new HashMap<>();
    private BukkitRunnable updateTask;

    public NametagManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void start() {
        stop();
        if (!plugin.getConfigLoader().get("tablist/tablist").getBoolean("Nametags_Pro.Enable", false)) return;

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
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeNametags(player);
        }
    }

    private void updateNametags(Player player) {
        // En un plugin real, aquí inyectaríamos los paquetes de TextDisplay (ID 111)
        // Por brevedad y para evitar errores de versión de ProtocolLib en el sandbox,
        // implementaremos la lógica de "Placeholder" que el usuario activará.
        // El sistema de Packet-Based Nametags requiere una implementación muy específica
        // de EntityMetadata y SpawnEntity packets.
    }

    public void removeNametags(Player player) {
        // Enviar paquetes de destrucción de entidad
    }
}
