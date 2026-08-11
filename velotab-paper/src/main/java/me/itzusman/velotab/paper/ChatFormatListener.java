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
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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

    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, String> lastMessageContent = new HashMap<>();

    public ChatFormatListener(VeloTabPaperPlugin plugin, boolean placeholderApiPresent) {
        this.plugin = plugin;
        this.placeholderApiPresent = placeholderApiPresent;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        FileConfiguration chatConfig = plugin.getConfigLoader().get("chat/chat");
        if (!chatConfig.getBoolean("Enable", true)) return;

        Player player = event.getPlayer();
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        // 1. Protección de Chat
        if (handleChatProtection(player, rawMessage, event)) return;

        // 2. Menciones
        handleMentions(player, rawMessage);

        // 3. Discord Webhook Sync
        handleDiscordSync(player, rawMessage);

        // 4. Formato de Chat con Soporte para Tags
        String format = chatConfig.getString("Default_Format", "&8[%luckperms_prefix%&8] &7{player} &8» &7{message}");
        
        String prefixLP = "";
        String suffixLP = "";
        String playerTag = getPlayerTag(player);

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
                .replace("%luckperms_suffix%", suffixLP)
                .replace("{tag}", playerTag);

        if (placeholderApiPresent) {
            baseFormat = PlaceholderAPI.setPlaceholders(player, baseFormat);
        }

        String[] parts = baseFormat.split("\\{player\\}", 2);
        if (parts.length < 2) return;

        final Component prefixComp = formatComponent(player, parts[0]);
        
        Component nameComp = Component.text(player.getName());
        String hoverText = chatConfig.getString("Format.Player_Hover", "");
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

    private void handleMentions(Player sender, String message) {
        FileConfiguration config = plugin.getConfigLoader().get("chat/chat");
        if (!config.getBoolean("Mentions.Enable", true)) return;

        String mentionColor = config.getString("Mentions.Color", "&e&l");
        String soundStr = config.getString("Mentions.Sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        String notify = config.getString("Mentions.Actionbar_Notify", "&f¡&e%player_name% &fte ha mencionado!");

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (message.contains("@" + target.getName())) {
                try {
                    target.playSound(target.getLocation(), Sound.valueOf(soundStr), 1.0f, 1.0f);
                    target.sendActionBar(plugin.getDisplayManager().buildComponent(sender, notify));
                } catch (Exception ignored) {}
            }
        }
    }

    private void handleDiscordSync(Player player, String message) {
        FileConfiguration config = plugin.getConfigLoader().get("security/discord_webhooks");
        if (config.getBoolean("chat_sync.enable", false)) {
            String url = config.getString("chat_sync.webhook_url");
            String username = config.getString("chat_sync.username", "%player_name%");
            String avatar = config.getString("chat_sync.avatar_url", "https://minotar.net/avatar/%player_name%/128.png");
            
            username = PlaceholderAPI.setPlaceholders(player, username);
            avatar = PlaceholderAPI.setPlaceholders(player, avatar);
            
            DiscordWebhookManager.sendWebhook(url, username, avatar, message);
        }
    }

    private String getPlayerTag(Player player) {
        FileConfiguration config = plugin.getConfigLoader().get("chat/tags");
        if (!config.getBoolean("enable", false)) return "";

        ConfigurationSection tags = config.getConfigurationSection("tags");
        if (tags != null) {
            for (String key : tags.getKeys(false)) {
                if (player.hasPermission("velotab.tag." + key)) {
                    return tags.getString(key + ".display", "");
                }
            }
        }
        return "";
    }

    private boolean handleChatProtection(Player player, String message, AsyncChatEvent event) {
        FileConfiguration config = plugin.getConfigLoader().get("chat/chat");
        UUID uuid = player.getUniqueId();

        // AntiSpam
        if (config.getBoolean("Protection.AntiSpam.Enable", true) && !player.hasPermission("velotab.chat.bypass.spam")) {
            long now = System.currentTimeMillis();
            long last = lastMessageTime.getOrDefault(uuid, 0L);
            int cooldown = config.getInt("Protection.AntiSpam_Cooldown", 3) * 1000;
            if (now - last < cooldown) {
                double remaining = (cooldown - (now - last)) / 1000.0;
                player.sendMessage(ColorUtil.colorize("&c¡Espera " + String.format("%.1f", remaining) + "s!"));
                event.setCancelled(true);
                return true;
            }
            lastMessageTime.put(uuid, now);
        }

        // AntiSwear
        for (String word : config.getStringList("Protection.Blocked_Words")) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                player.sendMessage(ColorUtil.colorize("&c¡Mensaje bloqueado por lenguaje inapropiado!"));
                event.setCancelled(true);
                return true;
            }
        }

        return false;
    }

    private Component formatComponent(Player player, String text) {
        return plugin.getDisplayManager().buildComponent(player, text);
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
