/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.itzusman.velotab.common.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ChatFormatListener implements Listener {

    private final VeloTabPaperPlugin plugin;
    private final boolean placeholderApiPresent;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Cache para AntiSpam y Repeat Blocker
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, String> lastMessageContent = new HashMap<>();

    public ChatFormatListener(VeloTabPaperPlugin plugin, boolean placeholderApiPresent) {
        this.plugin = plugin;
        this.placeholderApiPresent = placeholderApiPresent;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        FileConfiguration config = plugin.getCustomConfig("chat");
        if (!config.getBoolean("Enable", true)) return;

        Player player = event.getPlayer();
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        // --- PROTECCIÓN DE CHAT ---
        if (handleChatProtection(player, rawMessage, event)) return;

        // --- FORMATO DE CHAT ---
        String format = resolveFormat(player, config);
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
        String hoverText = config.getString("Format.Player_Hover", "");
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

    private boolean handleChatProtection(Player player, String message, AsyncChatEvent event) {
        FileConfiguration config = plugin.getCustomConfig("chat");
        UUID uuid = player.getUniqueId();

        // 1. AntiSpam
        if (config.getBoolean("Protection.AntiSpam.Enable", true) && !player.hasPermission(config.getString("Protection.AntiSpam.Bypass_Permission"))) {
            long now = System.currentTimeMillis();
            long last = lastMessageTime.getOrDefault(uuid, 0L);
            int cooldown = config.getInt("Protection.AntiSpam.Cooldown", 3) * 1000;
            if (now - last < cooldown) {
                double remaining = (cooldown - (now - last)) / 1000.0;
                player.sendMessage(ColorUtil.colorize(config.getString("Protection.AntiSpam.Message").replace("{time}", String.format("%.1f", remaining))));
                event.setCancelled(true);
                return true;
            }
            lastMessageTime.put(uuid, now);
        }

        // 2. Repeat Blocker
        if (config.getBoolean("Protection.Repeat_Blocker.Enable", true) && !player.hasPermission(config.getString("Protection.Repeat_Blocker.Bypass_Permission"))) {
            String lastMsg = lastMessageContent.get(uuid);
            if (message.equalsIgnoreCase(lastMsg)) {
                player.sendMessage(ColorUtil.colorize(config.getString("Protection.Repeat_Blocker.Message")));
                event.setCancelled(true);
                return true;
            }
            lastMessageContent.put(uuid, message);
        }

        // 3. AntiSwear
        if (config.getBoolean("Protection.AntiSwear.Enable", true) && !player.hasPermission(config.getString("Protection.AntiSwear.Bypass_Permission"))) {
            for (String word : config.getStringList("Protection.AntiSwear.Blocked_Words")) {
                if (Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b").matcher(message).find()) {
                    player.sendMessage(ColorUtil.colorize(config.getString("Protection.AntiSwear.Message")));
                    event.setCancelled(true);
                    return true;
                }
            }
        }

        // 4. Caps Blocker
        if (config.getBoolean("Protection.Caps_Blocker.Enable", true) && !player.hasPermission(config.getString("Protection.Caps_Blocker.Bypass_Permission"))) {
            int minLength = config.getInt("Protection.Caps_Blocker.Min_Length", 5);
            if (message.length() >= minLength) {
                int caps = 0;
                for (char c : message.toCharArray()) {
                    if (Character.isUpperCase(c)) caps++;
                }
                int percentage = (caps * 100) / message.length();
                if (percentage > config.getInt("Protection.Caps_Blocker.Max_Percentage", 70)) {
                    player.sendMessage(ColorUtil.colorize(config.getString("Protection.Caps_Blocker.Message")));
                    event.setCancelled(true);
                    return true;
                }
            }
        }

        return false;
    }

    private Component formatComponent(Player player, String text) {
        if (placeholderApiPresent) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }
        String coloredText = ColorUtil.colorize(text);
        return legacySerializer.deserialize(coloredText);
    }

    private String resolveFormat(Player player, FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("Format.Groups");
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
        return config.getString("Format.Default_Format", "&8[%luckperms_prefix%&8] &7{player} &8» &7{message}");
    }
}
