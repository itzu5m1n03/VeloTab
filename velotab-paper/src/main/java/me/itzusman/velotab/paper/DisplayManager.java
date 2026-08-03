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

        if (plugin.getConfig().getBoolean("Sorting.Enable", true)) {
            applyGlobalSorting(player);
        }
    }

    private void applyGlobalSorting(Player viewer) {
        if (!plugin.isLuckPermsPresent()) return;

        Scoreboard board = viewer.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            viewer.setScoreboard(board);
        }

        try {
            LuckPerms lp = LuckPermsProvider.get();
            for (Player target : Bukkit.getOnlinePlayers()) {
                User user = lp.getUserManager().getUser(target.getUniqueId());
                String group = (user != null) ? user.getPrimaryGroup() : "default";
                String priority = plugin.getConfig().getString("Sorting.Groups." + group, "99");
                
                String teamName = "vt_" + priority + group;
                if (teamName.length() > 16) teamName = teamName.substring(0, 16);

                Team team = board.getTeam(teamName);
                if (team == null) team = board.registerNewTeam(teamName);
                
                if (!team.hasEntry(target.getName())) {
                    for (Team oldTeam : board.getTeams()) {
                        if (oldTeam.getName().startsWith("vt_") && oldTeam.hasEntry(target.getName())) {
                            oldTeam.removeEntry(target.getName());
                        }
                    }
                    team.addEntry(target.getName());
                }
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

        Objective obj = board.getObjective("velotab_sb");
        if (obj == null) {
            String title = plugin.getConfig().getString("Scoreboard.Title", "&bVeloTab");
            obj = board.registerNewObjective("velotab_sb", "dummy", buildComponent(player, title));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        List<String> lines = plugin.getConfig().getStringList("Scoreboard.Lines");
        int size = lines.size();
        
        for (int i = 0; i < size; i++) {
            String line = lines.get(i);
            String teamName = "sb_line_" + i;
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                String entry = "§" + Integer.toHexString(i / 16) + "§" + Integer.toHexString(i % 16) + "§r";
                team.addEntry(entry);
                obj.getScore(entry).setScore(size - i);
            }

            team.prefix(buildComponent(player, line));
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
        if (plugin.isPlaceholderApiPresent()) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        if (text.contains("<") && text.contains(">")) {
            String mmText = legacyToMiniMessage(text);
            return miniMessage.deserialize(mmText);
        } else {
            // Corregido: Usar '&' para que coincida con legacySerializer (legacyAmpersand)
            text = translateHexToAmpersand(text);
            return legacySerializer.deserialize(text);
        }
    }

    private String legacyToMiniMessage(String text) {
        return text.replace("&0", "<black>")
                   .replace("&1", "<dark_blue>")
                   .replace("&2", "<dark_green>")
                   .replace("&3", "<dark_aqua>")
                   .replace("&4", "<dark_red>")
                   .replace("&5", "<dark_purple>")
                   .replace("&6", "<gold>")
                   .replace("&7", "<gray>")
                   .replace("&8", "<dark_gray>")
                   .replace("&9", "<blue>")
                   .replace("&a", "<green>")
                   .replace("&b", "<aqua>")
                   .replace("&c", "<red>")
                   .replace("&d", "<light_purple>")
                   .replace("&e", "<yellow>")
                   .replace("&f", "<white>")
                   .replace("&l", "<bold>")
                   .replace("&m", "<strikethrough>")
                   .replace("&n", "<underlined>")
                   .replace("&o", "<italic>")
                   .replace("&r", "<reset>");
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
}
