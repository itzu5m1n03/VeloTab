/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormatListener implements Listener {

    private final VeloTabPaperPlugin plugin;
    private final boolean placeholderApiPresent;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})|#([A-Fa-f0-9]{6})");

    public ChatFormatListener(VeloTabPaperPlugin plugin, boolean placeholderApiPresent) {
        this.plugin = plugin;
        this.placeholderApiPresent = placeholderApiPresent;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("Chat_Format.Enable", true)) return;

        Player player = event.getPlayer();
        String format = resolveFormat(player);

        String prefix = "";
        String suffix = "";

        if (plugin.isLuckPermsPresent()) {
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    CachedMetaData metaData = user.getCachedData().getMetaData();
                    prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
                    suffix = metaData.getSuffix() != null ? metaData.getSuffix() : "";
                }
            } catch (Exception ignored) {}
        }

        // Aplicar PAPI primero
        String finalFormat = format
                .replace("%luckperms_prefix%", prefix)
                .replace("%luckperms_suffix%", suffix)
                .replace("{player}", player.getName());

        if (placeholderApiPresent) {
            finalFormat = PlaceholderAPI.setPlaceholders(player, finalFormat);
        }

        // Traducir colores HEX y Legados
        finalFormat = translateHexColorCodes(finalFormat);
        finalFormat = org.bukkit.ChatColor.translateAlternateColorCodes('&', finalFormat);

        // Separar por {message}
        String[] parts = finalFormat.split("\\{message\\}", 2);
        
        // Usar MiniMessage para permitir Hover/Click en el formato si se desea
        // Pero para compatibilidad con colores legacy usamos el serializer
        Component prefixComp = serializer.deserialize(parts[0]);
        Component suffixComp = parts.length > 1 ? serializer.deserialize(parts[1]) : Component.empty();

        // Aplicar Hover al nombre si esta configurado
        String hoverText = plugin.getConfig().getString("Chat_Format.Player_Hover", "");
        if (!hoverText.isEmpty()) {
            if (placeholderApiPresent) hoverText = PlaceholderAPI.setPlaceholders(player, hoverText);
            hoverText = translateHexColorCodes(hoverText);
            hoverText = org.bukkit.ChatColor.translateAlternateColorCodes('&', hoverText);
            
            // Reemplazar el nombre del jugador en el prefijo con un componente con hover
            // Esto es mas complejo, por ahora aplicamos el renderer directamente
        }

        event.renderer((source, sourceDisplayName, message, viewer) -> 
            prefixComp.append(message).append(suffixComp)
        );
    }

    private String resolveFormat(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("Chat_Format.Groups");
        if (section != null && plugin.isLuckPermsPresent()) {
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String primaryGroup = user.getPrimaryGroup();
                    if (primaryGroup != null && section.contains(primaryGroup)) {
                        return section.getString(primaryGroup);
                    }
                }
            } catch (Exception ignored) {}
        }
        return plugin.getConfig().getString("Chat_Format.Default_Format", "&8[%luckperms_prefix%&8] &7{player} &8» &7{message}");
    }

    private String translateHexColorCodes(String message) {
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(buffer, "§x§" + group.charAt(0) + "§" + group.charAt(1) + "§" + group.charAt(2) + "§" + group.charAt(3) + "§" + group.charAt(4) + "§" + group.charAt(5));
        }
        return matcher.appendTail(buffer).toString();
    }
}
