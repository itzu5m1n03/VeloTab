/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.geysermc.geyser.api.GeyserApi;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.IUser;

import java.util.UUID;

/**
 * Centraliza las comprobaciones de otros plugins.
 */
public class HookManager {

    private final VeloTabPaperPlugin plugin;
    private Essentials essentials;
    private boolean geyserPresent;

    public HookManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        }
        geyserPresent = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null;
    }

    /**
     * Comprueba si un jugador está en modo Vanish.
     * Soporta Essentials, SuperVanish, PremiumVanish y VanishNoPacket.
     */
    public boolean isVanished(Player player) {
        // Bukkit Metadata (Estándar para la mayoría de plugins de Vanish)
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }

        // EssentialsX
        if (essentials != null) {
            IUser user = essentials.getUser(player);
            if (user != null && user.isVanished()) return true;
        }

        return false;
    }

    /**
     * Comprueba si un jugador está AFK (EssentialsX).
     */
    public boolean isAFK(Player player) {
        if (essentials != null) {
            IUser user = essentials.getUser(player);
            return user != null && user.isAfk();
        }
        return false;
    }

    /**
     * Comprueba si un jugador está conectado desde Bedrock Edition.
     */
    public boolean isBedrock(Player player) {
        if (geyserPresent) {
            try {
                return GeyserApi.api().isBedrockPlayer(player.getUniqueId());
            } catch (Exception ignored) {}
        }
        // Fallback: Prefijo común en nombres de Bedrock (configurable)
        return player.getName().startsWith(".");
    }
}
