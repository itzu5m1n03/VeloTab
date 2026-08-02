package com.lobbomax.velotab.paper;

import net.kyori.adventure.text.Component;
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
import org.bukkit.event.Listener;

/**
 * Formatea el chat segun el rango (grupo de LuckPerms) del jugador,
 * usando el formato definido en config.yml -> Chat_Format.
 */
public class ChatFormatListener implements Listener {

    private final VeloTabPaperPlugin plugin;
    private final boolean placeholderApiPresent;

    public ChatFormatListener(VeloTabPaperPlugin plugin, boolean placeholderApiPresent) {
        this.plugin = plugin;
        this.placeholderApiPresent = placeholderApiPresent;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("Chat_Format.Enable", true)) {
            return;
        }

        Player player = event.getPlayer();
        String plainMessage = plainText(event.message());

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
            } catch (IllegalStateException ignored) {
                // LuckPerms no termino de cargar todavia.
            }
        }

        String result = format
                .replace("%luckperms_prefix%", prefix)
                .replace("%luckperms_suffix%", suffix)
                .replace("{player}", player.getName())
                .replace("{message}", plainMessage);

        if (placeholderApiPresent) {
            result = PlaceholderAPI.setPlaceholders(player, result);
        }

        result = translateColors(result);

        Component finalComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(result);
        event.renderer((source, sourceDisplayName, message, viewer) -> finalComponent);
    }

    /**
     * Busca el formato del PRIMER grupo (por peso) del jugador que tenga
     * una entrada en Chat_Format. Si ninguno tiene, usa Default_Format.
     */
    private String resolveFormat(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("Chat_Format");
        if (section == null) {
            return "&7{player}&8: &f{message}";
        }

        if (plugin.isLuckPermsPresent()) {
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String primaryGroup = user.getPrimaryGroup();
                    if (primaryGroup != null && section.contains(primaryGroup)) {
                        return section.getString(primaryGroup);
                    }
                    // Si el grupo primario no tiene formato propio, revisa
                    // tambien todos los grupos heredados por si acaso.
                    for (String inherited : user.getInheritedGroups(user.getQueryOptions())
                            .stream().map(g -> g.getName()).toList()) {
                        if (section.contains(inherited)) {
                            return section.getString(inherited);
                        }
                    }
                }
            } catch (IllegalStateException ignored) {
                // LuckPerms no termino de cargar todavia.
            }
        }

        return section.getString("Default_Format", "&7{player}&8: &f{message}");
    }

    private String translateColors(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    private String plainText(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(component);
    }
}
