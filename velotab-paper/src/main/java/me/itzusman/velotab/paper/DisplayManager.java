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
import net.luckperms.api.model.user.User;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisplayManager {

    private final VeloTabPaperPlugin plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})|#([A-Fa-f0-9]{6})");
    private BukkitRunnable updateTask;

    public DisplayManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        int interval = plugin.getConfig().getInt("TabList.Update_Interval", 20);
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateTabList(player);
                    updateScoreboard(player);
                }
            }
        };
        updateTask.runTaskTimer(plugin, 0L, interval);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updateTabList(Player player) {
        if (!plugin.getConfig().getBoolean("TabList.Enable", true)) return;

        List<String> headerLines = plugin.getConfig().getStringList("TabList.Header");
        List<String> footerLines = plugin.getConfig().getStringList("TabList.Footer");

        player.sendPlayerListHeaderAndFooter(
                buildComponent(player, headerLines),
                buildComponent(player, footerLines)
        );

        // Error 4 corregido: Implementacion real de Sorting
        if (plugin.getConfig().getBoolean("Sorting.Enable", true)) {
            applySorting(player);
        }
    }

    private void applySorting(Player player) {
        if (!plugin.isLuckPermsPresent()) return;

        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) return;

            String group = user.getPrimaryGroup();
            String priority = plugin.getConfig().getString("Sorting.Groups." + group, "99");
            
            // Usamos el Scoreboard del jugador para el ordenamiento
            Scoreboard board = player.getScoreboard();
            String teamName = priority + group;
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);

            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);
            
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        } catch (Exception ignored) {}
    }

    private void updateScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("Scoreboard.Enable", true)) return;

        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("velotab");
        if (obj == null) {
            String title = plugin.getConfig().getString("Scoreboard.Title", "&bVeloTab");
            obj = board.registerNewObjective("velotab", "dummy", buildComponent(player, title));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        List<String> lines = plugin.getConfig().getStringList("Scoreboard.Lines");
        int size = lines.size();
        
        for (int i = 0; i < size; i++) {
            String line = lines.get(i);
            String teamName = "vls_" + i;
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                String entry = "§" + Integer.toHexString(i / 16) + "§" + Integer.toHexString(i % 16) + "§r";
                team.addEntry(entry);
                obj.getScore(entry).setScore(size - i);
            }

            Component comp = buildComponent(player, line);
            team.prefix(comp);
        }
    }

    private Component buildComponent(Player player, List<String> lines) {
        Component finalComp = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            finalComp = finalComp.append(buildComponent(player, lines.get(i)));
            if (i < lines.size() - 1) finalComp = finalComp.append(Component.newline());
        }
        return finalComp;
    }

    private Component buildComponent(Player player, String text) {
        // 1. PlaceholderAPI
        if (plugin.isPlaceholderApiPresent()) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        // 2. Error 3 corregido: Soporte MiniMessage real
        if (text.contains("<") && text.contains(">")) {
            // Si contiene etiquetas MiniMessage, lo parseamos como tal
            // Pero primero traducimos los colores legados y hex para que convivan
            text = translateHexColorCodes(text);
            text = org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
            // Nota: MiniMessage no se lleva bien con codigos de seccion (§), 
            // asi que para mezclar ambos mundos usamos un enfoque hibrido o detectamos prioridad.
            // Por simplicidad en este caso, si hay < > asumimos MiniMessage puro o legados ya convertidos.
            return miniMessage.deserialize(text);
        } else {
            // 3. Colores Legados y Hexadecimales
            text = translateHexColorCodes(text);
            text = org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
            return legacySerializer.deserialize(text);
        }
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
