/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
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
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
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

        String prefixLP = "";
        String suffixLP = "";

        if (plugin.isLuckPermsPresent()) {
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    CachedMetaData metaData = user.getCachedData().getMetaData();
                    prefixLP = metaData.getPrefix() != null ? metaData.getPrefix() : "";
                    suffixLP = metaData.getSuffix() != null ? metaData.getSuffix() : "";
                }
            } catch (Exception ignored) {}
        }

        String baseFormat = format
                .replace("%luckperms_prefix%", prefixLP)
                .replace("%luckperms_suffix%", suffixLP);

        if (placeholderApiPresent) {
            baseFormat = PlaceholderAPI.setPlaceholders(player, baseFormat);
        }

        String[] parts = baseFormat.split("\\{player\\}", 2);
        if (parts.length < 2) return;

        final Component prefixComp = formatComponent(player, parts[0]);
        
        Component nameComp = Component.text(player.getName());
        String hoverText = plugin.getConfig().getString("Chat_Format.Player_Hover", "");
        if (!hoverText.isEmpty()) {
            nameComp = nameComp.hoverEvent(HoverEvent.showText(formatComponent(player, hoverText)));
        }
        final Component playerNameComp = nameComp;

        String afterPlayer = parts[1];
        String[] messageParts = afterPlayer.split("\\{message\\}", 2);
        
        final Component midComp = formatComponent(player, messageParts[0]);
        final Component suffixComp = messageParts.length > 1 ? formatComponent(player, messageParts[1]) : Component.empty();

        event.renderer((source, sourceDisplayName, message, viewer) -> 
            prefixComp.append(playerNameComp).append(midComp).append(message).append(suffixComp)
        );
    }

    private Component formatComponent(Player player, String text) {
        if (placeholderApiPresent) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }
        
        if (text.contains("<") && text.contains(">")) {
            return miniMessage.deserialize(legacyToMiniMessage(text));
        } else {
            // Corregido: Usar '&' para que coincida con legacySerializer (legacyAmpersand)
            text = translateHexToAmpersand(text);
            return legacySerializer.deserialize(text);
        }
    }

    private String legacyToMiniMessage(String text) {
        return text.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                   .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                   .replace("&f", "<white>").replace("&l", "<bold>").replace("&m", "<strikethrough>")
                   .replace("&n", "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>");
    }

    private String translateHexToAmpersand(String message) {
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            // Corregido: Usar '&' en lugar de '§' para ser compatible con legacyAmpersand()
            matcher.appendReplacement(buffer, "&x&" + group.charAt(0) + "&" + group.charAt(1) + "&" + group.charAt(2) + "&" + group.charAt(3) + "&" + group.charAt(4) + "&" + group.charAt(5));
        }
        return matcher.appendTail(buffer).toString();
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
}
