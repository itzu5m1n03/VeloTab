/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
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

        Component header = buildComponent(player, headerLines);
        Component footer = buildComponent(player, footerLines);

        player.sendPlayerListHeaderAndFooter(header, footer);
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
            obj = board.registerNewObjective("velotab", "dummy", serializer.deserialize(format(player, title)));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        List<String> lines = plugin.getConfig().getStringList("Scoreboard.Lines");
        int score = lines.size();
        for (String line : lines) {
            String formatted = format(player, line);
            if (formatted.length() > 40) formatted = formatted.substring(0, 40);
            obj.getScore(formatted).setScore(score--);
        }
    }

    private Component buildComponent(Player player, List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            builder.append(lines.get(i));
            if (i < lines.size() - 1) builder.append("\n");
        }
        return serializer.deserialize(format(player, builder.toString()));
    }

    private String format(Player player, String text) {
        if (plugin.isPlaceholderApiPresent()) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }
        text = translateHexColorCodes(text);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
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
