/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import net.kyori.adventure.text.Component;
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

    // Serializador Legacy robusto para codigos & y Hex nativo de Minecraft
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Patron para detectar colores hexadecimales (&#RRGGBB o #RRGGBB)
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})|#([A-Fa-f0-9]{6})");

    private BukkitRunnable updateTask;
    private static final String SORTING_TEAM_PREFIX = "vt_s_";

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

        applyGlobalSorting(player);
    }

    private void applyGlobalSorting(Player viewer) {
        Scoreboard board = viewer.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            viewer.setScoreboard(board);
        }

        LuckPerms lp = null;
        if (plugin.isLuckPermsPresent()) {
            try {
                lp = LuckPermsProvider.get();
            } catch (Exception ignored) {}
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            String priority = "99";
            String group = "default";
            if (lp != null) {
                try {
                    User user = lp.getUserManager().getUser(target.getUniqueId());
                    group = (user != null) ? user.getPrimaryGroup() : "default";
                    priority = plugin.getConfig().getString("Sorting.Groups." + group, "99");
                } catch (Exception ignored) {}
            }

            // Team de ordenación basado en prioridad
            String teamName = SORTING_TEAM_PREFIX + priority + group;
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }

            if (!team.hasEntry(target.getName())) {
                for (Team t : board.getTeams()) {
                    if (t.getName().startsWith(SORTING_TEAM_PREFIX) && t.hasEntry(target.getName())) {
                        t.removeEntry(target.getName());
                    }
                }
                team.addEntry(target.getName());
            }

            // Name_Layering DESACTIVADO por ahora para evitar bugs de visualización
            target.playerListName(null);
            team.prefix(Component.empty());
            team.suffix(Component.empty());
        }
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
        } else {
            String title = plugin.getConfig().getString("Scoreboard.Title", "&bVeloTab");
            obj.displayName(buildComponent(player, title));
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

    public Component buildComponent(Player player, String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. Resolver Placeholders (PAPI)
        if (plugin.isPlaceholderApiPresent()) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        // 2. Limpiar CUALQUIER tag de MiniMessage o HTML que el usuario haya puesto por error
        // Solo queremos & y &#RRGGBB
        text = text.replaceAll("<[^>]*>", "");

        // 3. Traducir Hexadecimales al formato legacy que Adventure entiende nativamente
        String coloredText = translateHexToLegacy(text);

        // 4. Deserializar usando el formato Legacy (&)
        return legacySerializer.deserialize(coloredText);
    }

    private String translateHexToLegacy(String message) {
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            // Formato estándar de Minecraft para Hex: &x&r&r&g&g&b&b
            matcher.appendReplacement(buffer, "&x&" + group.charAt(0) + "&" + group.charAt(1)
                    + "&" + group.charAt(2) + "&" + group.charAt(3)
                    + "&" + group.charAt(4) + "&" + group.charAt(5));
        }
        return matcher.appendTail(buffer).toString();
    }
}
