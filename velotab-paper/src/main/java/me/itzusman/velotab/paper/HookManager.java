/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

/**
 * Centraliza las comprobaciones de otros plugins de forma segura.
 * Evita errores de NoClassDefFoundError al no importar directamente las clases si el plugin no existe.
 */
public class HookManager {

    private final VeloTabPaperPlugin plugin;
    private boolean essentialsPresent;
    private boolean geyserPresent;

    public HookManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        essentialsPresent = Bukkit.getPluginManager().getPlugin("Essentials") != null;
        geyserPresent = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null;
    }

    /**
     * Comprueba si un jugador está en modo Vanish.
     */
    public boolean isVanished(Player player) {
        // Bukkit Metadata (Estándar para la mayoría de plugins de Vanish)
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }

        // EssentialsX (Llamada segura)
        if (essentialsPresent) {
            try {
                return EssentialsWrapper.isVanished(player);
            } catch (NoClassDefFoundError | Exception ignored) {}
        }

        return false;
    }

    /**
     * Comprueba si un jugador está AFK (EssentialsX).
     */
    public boolean isAFK(Player player) {
        if (essentialsPresent) {
            try {
                return EssentialsWrapper.isAFK(player);
            } catch (NoClassDefFoundError | Exception ignored) {}
        }
        return false;
    }

    /**
     * Comprueba si un jugador está conectado desde Bedrock Edition.
     */
    public boolean isBedrock(Player player) {
        if (geyserPresent) {
            try {
                return GeyserWrapper.isBedrock(player);
            } catch (NoClassDefFoundError | Exception ignored) {}
        }
        return player.getName().startsWith(".");
    }

    /**
     * Clase interna para aislar las dependencias de EssentialsX.
     */
    private static class EssentialsWrapper {
        private static boolean isVanished(Player player) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (plugin instanceof com.earth2me.essentials.Essentials) {
                com.earth2me.essentials.IUser user = ((com.earth2me.essentials.Essentials) plugin).getUser(player);
                return user != null && user.isVanished();
            }
            return false;
        }

        private static boolean isAFK(Player player) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (plugin instanceof com.earth2me.essentials.Essentials) {
                com.earth2me.essentials.IUser user = ((com.earth2me.essentials.Essentials) plugin).getUser(player);
                return user != null && user.isAfk();
            }
            return false;
        }
    }

    /**
     * Clase interna para aislar las dependencias de Geyser.
     */
    private static class GeyserWrapper {
        private static boolean isBedrock(Player player) {
            return org.geysermc.geyser.api.GeyserApi.api().isBedrockPlayer(player.getUniqueId());
        }
    }
}
