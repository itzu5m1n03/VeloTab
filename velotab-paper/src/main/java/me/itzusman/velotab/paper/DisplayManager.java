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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
    private final Pattern scrollerPattern = Pattern.compile("\\{scroller:([^,]+):(\\d+):(\\d+)\\}");
    private final Pattern rainbowPattern = Pattern.compile("\\{rainbow:([^}]+)\\}");
    
    private final ScoreboardPageManager pageManager;

    public DisplayManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
        this.pageManager = new ScoreboardPageManager(plugin);
        this.pageManager.load();
    }

    public void start() {
        stop();
        FileConfiguration tabConfig = plugin.getConfigLoader().get("tablist/tablist");
        int tabInterval = tabConfig.getInt("Update_Interval", 20);
        
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                currentTicks++;
                
                // Animaciones y efectos se procesan cada tick si es necesario,
                // pero las actualizaciones pesadas se distribuyen.
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Scoreboard cada 2 ticks (suficiente para la mayoría de animaciones)
                    if (currentTicks % 2 == 0) {
                        updateScoreboard(player);
                    }
                    
                    // TabList según configuración (default 20 ticks)
                    if (currentTicks % tabInterval == 0) {
                        updateTabList(player);
                        updateTabObjectives(player);
                    }
                }
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 1L);
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
        if (current) player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
    }

    public void removePlayer(Player player) {
        scoreboardVisibility.remove(player.getUniqueId());
    }

    private void updateTabList(Player player) {
        FileConfiguration tabConfig = plugin.getConfigLoader().get("tablist/tablist");
        if (!tabConfig.getBoolean("Enable", true)) return;

        ConfigurationSection groups = plugin.getConfigLoader().get("tablist/groups").getConfigurationSection("groups");
        String headerKey = "Default.Header";
        String footerKey = "Default.Footer";
        
        if (groups != null) {
            for (String group : groups.getKeys(false)) {
                if (player.hasPermission("velotab.tablist.group." + group)) {
                    headerKey = "groups." + group + ".Header";
                    footerKey = "groups." + group + ".Footer";
                    break;
                }
            }
        }

        List<String> headerLines = plugin.getConfigLoader().get("tablist/groups").getStringList(headerKey);
        if (headerLines.isEmpty()) headerLines = tabConfig.getStringList("Default.Header");
        
        List<String> footerLines = plugin.getConfigLoader().get("tablist/groups").getStringList(footerKey);
        if (footerLines.isEmpty()) footerLines = tabConfig.getStringList("Default.Footer");

        player.sendPlayerListHeaderAndFooter(
                buildComponent(player, headerLines),
                buildComponent(player, footerLines)
        );

        applyGlobalSorting(player);
    }

    private void applyGlobalSorting(Player viewer) {
        FileConfiguration tabConfig = plugin.getConfigLoader().get("tablist/tablist");
        FileConfiguration groupsConfig = plugin.getConfigLoader().get("tablist/groups");
        
        Scoreboard board = viewer.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            viewer.setScoreboard(board);
        }

        LuckPerms lp = null;
        if (plugin.isLuckPermsPresent()) {
            try { lp = LuckPermsProvider.get(); } catch (Exception ignored) {}
        }

        boolean layeringEnabled = tabConfig.getBoolean("Name_Layering.Enable", true);

        for (Player target : Bukkit.getOnlinePlayers()) {
            // Manejo de Vanish
            if (plugin.getHookManager().isVanished(target) && !viewer.isOp()) {
                viewer.hidePlayer(plugin, target);
                continue;
            }

            String priority = "99";
            String group = "default";
            if (lp != null) {
                try {
                    User user = lp.getUserManager().getUser(target.getUniqueId());
                    group = (user != null) ? user.getPrimaryGroup() : "default";
                    priority = groupsConfig.getString("sorting." + group, "99");
                } catch (Exception ignored) {}
            }

            // TEAM UNICO por jugador para evitar el bug de Name_Layering compartido.
            // Usamos el hash del UUID para garantizar unicidad pero mantener consistencia.
            String uniqueId = Integer.toHexString(target.getUniqueId().hashCode());
            String teamName = SORTING_TEAM_PREFIX + priority + "_" + uniqueId;
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);

            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);

            if (!team.hasEntry(target.getName())) {
                // Limpiar al jugador de otros equipos de sorting anteriores
                for (Team t : board.getTeams()) {
                    if (t.getName().startsWith(SORTING_TEAM_PREFIX) && t.hasEntry(target.getName()) && !t.getName().equals(teamName)) {
                        t.removeEntry(target.getName());
                    }
                }
                team.addEntry(target.getName());
            }

            // Aplicar Name_Layering (Prefix/Suffix en el nombre del jugador)
            if (layeringEnabled) {
                String up = tabConfig.getString("Name_Layering.UpName", "");
                String down = tabConfig.getString("Name_Layering.DownName", "");
                String center = tabConfig.getString("Name_Layering.CenterName", "{player}");

                // Prefix (UpName)
                if (!up.isEmpty()) {
                    team.prefix(buildComponent(target, up + " "));
                } else {
                    team.prefix(Component.empty());
                }

                // Suffix (DownName)
                if (!down.isEmpty()) {
                    team.suffix(buildComponent(target, " " + down));
                } else {
                    team.suffix(Component.empty());
                }
                
                // CenterName (Nombre en el Tab)
                String centerName = center.replace("{player}", target.getName());
                if (plugin.getHookManager().isAFK(target)) {
                    centerName = tabConfig.getString("AFK_Format", "&7[AFK] ") + centerName;
                }
                target.playerListName(buildComponent(target, centerName));
            } else {
                team.prefix(Component.empty());
                team.suffix(Component.empty());
                target.playerListName(null); // Reset to default
            }
        }
    }

    private void updateTabObjectives(Player player) {
        FileConfiguration tabConfig = plugin.getConfigLoader().get("tablist/tablist");
        String type = tabConfig.getString("Objective.Type", "NONE").toUpperCase();
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
            if (type.equals("HEALTH")) value = (int) target.getHealth();
            else if (type.equals("PING")) value = target.getPing();
            obj.getScore(target.getName()).setScore(value);
        }
    }

    private void updateScoreboard(Player player) {
        FileConfiguration sbConfig = plugin.getConfigLoader().get("scoreboard/scoreboard");
        if (!sbConfig.getBoolean("Enable", true)) return;
        if (!scoreboardVisibility.getOrDefault(player.getUniqueId(), true)) return;

        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        ScoreboardPageManager.ScoreboardPage page = pageManager.getCurrentPage(currentTicks);

        Objective obj = board.getObjective("velotab_sb");
        if (obj == null) {
            obj = board.registerNewObjective("velotab_sb", "dummy", buildComponent(player, page.getTitle()));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.displayName(buildComponent(player, page.getTitle()));
        }

        List<String> lines = page.getLines();
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
        
        for (int i = size; i < 15; i++) {
            String teamName = "sb_line_" + i;
            Team team = board.getTeam(teamName);
            if (team != null) {
                for (String entry : team.getEntries()) board.resetScores(entry);
                team.unregister();
            }
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
        
        // 1. Efectos Avanzados (Scroller y Rainbow)
        text = processAdvancedEffects(text);
        
        // 2. Animaciones Base
        text = processAnimations(text);
        
        // 3. Placeholders
        if (plugin.isPlaceholderApiPresent()) text = PlaceholderAPI.setPlaceholders(player, text);
        
        // 4. Colores
        String coloredText = ColorUtil.colorize(text);
        return legacySerializer.deserialize(coloredText);
    }

    private String processAdvancedEffects(String text) {
        // Scroller: {scroller:TEXTO:WIDTH:SPACE}
        Matcher sm = scrollerPattern.matcher(text);
        while (sm.find()) {
            String content = sm.group(1);
            int width = Integer.parseInt(sm.group(2));
            int space = Integer.parseInt(sm.group(3));
            text = text.replace(sm.group(0), plugin.getAnimationManager().getScroller(content, width, space, currentTicks / 2));
        }
        
        // Rainbow: {rainbow:TEXTO}
        Matcher rm = rainbowPattern.matcher(text);
        while (rm.find()) {
            String content = rm.group(1);
            text = text.replace(rm.group(0), plugin.getAnimationManager().getRainbow(content, currentTicks / 2));
        }
        
        return text;
    }

    private String processAnimations(String text) {
        Matcher matcher = animPattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String animName = matcher.group(1);
            String frame = plugin.getAnimationManager().getCurrentFrame(animName, currentTicks / 5);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(frame));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
