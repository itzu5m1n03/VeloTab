package me.itzusman.velotab.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
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

/**
 * Formatea el chat conservando eventos de hover/click y soportando PlaceholderAPI.
 */
public class ChatFormatListener implements Listener {

    private final VeloTabPaperPlugin plugin;
    private final boolean placeholderApiPresent;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public ChatFormatListener(VeloTabPaperPlugin plugin, boolean placeholderApiPresent) {
        this.plugin = plugin;
        this.placeholderApiPresent = placeholderApiPresent;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("Chat_Format.Enable", true)) {
            return;
        }

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

        // Reemplazamos los placeholders en el formato (excepto el mensaje)
        String finalFormat = format
                .replace("%luckperms_prefix%", prefix)
                .replace("%luckperms_suffix%", suffix)
                .replace("{player}", player.getName());

        if (placeholderApiPresent) {
            finalFormat = PlaceholderAPI.setPlaceholders(player, finalFormat);
        }

        finalFormat = translateColors(finalFormat);

        // Separamos el formato en prefijo y sufijo respecto al placeholder {message}
        String[] parts = finalFormat.split("\\{message\\}", 2);
        Component prefixComp = serializer.deserialize(parts[0]);
        Component suffixComp = parts.length > 1 ? serializer.deserialize(parts[1]) : Component.empty();

        // Aplicamos el renderer conservando el componente original del mensaje (hover/click)
        event.renderer((source, sourceDisplayName, message, viewer) -> 
            prefixComp.append(message).append(suffixComp)
        );
    }

    private String resolveFormat(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("Chat_Format");
        if (section == null) return "&7{player}&8: &f{message}";

        if (plugin.isLuckPermsPresent()) {
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String primaryGroup = user.getPrimaryGroup();
                    if (primaryGroup != null && section.contains(primaryGroup)) {
                        return section.getString(primaryGroup);
                    }
                    for (String inherited : user.getInheritedGroups(user.getQueryOptions())
                            .stream().map(g -> g.getName()).toList()) {
                        if (section.contains(inherited)) {
                            return section.getString(inherited);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return section.getString("Default_Format", "&7{player}&8: &f{message}");
    }

    private String translateColors(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
