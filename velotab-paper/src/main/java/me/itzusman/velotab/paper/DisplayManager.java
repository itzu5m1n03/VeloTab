/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import me.itzusman.velotab.common.ColorUtil;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisplayManager {

    private final VeloTabPaperPlugin plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private BukkitRunnable updateTask;
    private long currentTicks = 0;
    private static final String SORTING_TEAM_PREFIX = "vt_s_";
    private final Map<UUID, Boolean> scoreboardVisibility = new HashMap<>();
    private final Pattern animPattern = Pattern.compile("\\{anim:([^}]+)\\}");

    public DisplayManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        int interval = plugin.getConfig().getInt("TabList.Update_Interval", 20);
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                currentTicks += interval;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateTabList(player);
                    updateScoreboard(player);
                    updateTabObjectives(player);
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

    public void toggleScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = scoreboardVisibility.getOrDefault(uuid, true);
        scoreboardVisibility.put(uuid, !current);
        
        if (current) {
            player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
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
            try { lp = LuckPermsProvider.get(); } catch (Exception ignored) {}
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            // 1. Manejo de Vanish: Si el objetivo esta en vanish y el espectador no es OP, ocultar.
            if (plugin.getHookManager().isVanished(target) && !viewer.isOp()) {
                viewer.hidePlayer(plugin, target);
                continue;
            } else if (!target.canSee(viewer)) {
                // Asegurar que si ya no esta en vanish, se vea.
                viewer.showPlayer(plugin, target);
            }

            String priority = "99";
            String group = "default";
            if (lp != null) {
                try {
                    User user = lp.getUserManager().getUser(target.getUniqueId());
                    group = (user != null) ? user.getPrimaryGroup() : "default";
                    priority = plugin.getConfig().getString("Sorting.Groups." + group, "99");
                } catch (Exception ignored) {}
            }

            // Team de ordenacion basado en prioridad
            String teamName = SORTING_TEAM_PREFIX + priority + group;
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);

            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);

            if (!team.hasEntry(target.getName())) {
                for (Team t : board.getTeams()) {
                    if (t.getName().startsWith(SORTING_TEAM_PREFIX) && t.hasEntry(target.getName())) {
                        t.removeEntry(target.getName());
                    }
                }
                team.addEntry(target.getName());
            }

            // 2. Manejo de AFK en el nombre del Tab
            String displayName = target.getName();
            if (plugin.getHookManager().isAFK(target)) {
                displayName = plugin.getConfig().getString("TabList.AFK_Format", "&7[AFK] ") + displayName;
            }
            
            // 3. Deteccion de Bedrock
            if (plugin.getHookManager().isBedrock(target)) {
                displayName = plugin.getConfig().getString("TabList.Bedrock_Prefix", "&7[BE] ") + displayName;
            }

            target.playerListName(buildComponent(target, displayName));
        }
    }

    private void updateTabObjectives(Player player) {
        String type = plugin.getConfig().getString("TabList.Objective.Type", "NONE").toUpperCase();
        if (type.equals("NONE")) return;

        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("vt_tab_obj");
        
        if (obj == null) {
            String title = type.equals("HEALTH") ? "§c❤" : "ms";
            obj = board.registerNewObjective("vt_tab_obj", "dummy", title);
            obj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            int value = 0;
            if (type.equals("HEALTH")) {
                value = (int) target.getHealth();
            } else if (type.equals("PING")) {
                value = target.getPing();
            }
            obj.getScore(target.getName()).setScore(value);
        }
    }

    private void updateScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("Scoreboard.Enable", true)) return;
        if (!scoreboardVisibility.getOrDefault(player.getUniqueId(), true)) return;

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

        // 1. Procesar Animaciones {anim:name}
        text = processAnimations(text);

        // 2. Resolver Placeholders (con cache opcional)
        if (plugin.isPlaceholderApiPresent()) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        // 3. Colorear (Legacy + Hex)
        String coloredText = ColorUtil.colorize(text);

        return legacySerializer.deserialize(coloredText);
    }

    private String processAnimations(String text) {
        Matcher matcher = animPattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String animName = matcher.group(1);
            String frame = plugin.getAnimationManager().getCurrentFrame(animName, currentTicks);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(frame));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
