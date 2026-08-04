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

    // Serializador Legacy configurado para soportar Hex y codigos &
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat() // Soporta &x&r&r&g&g&b&b
            .build();

    // MiniMessage para tags avanzados
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // Patron para detectar colores hexadecimales (&#RRGGBB o #RRGGBB)
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})|(?<![#A-Fa-f0-9])#([A-Fa-f0-9]{6})");

    private BukkitRunnable updateTask;
    private static final String NAMETAG_TEAM_PREFIX = "vt_nl_";

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

        applyGlobalSortingAndNames(player);
    }

    private void applyGlobalSortingAndNames(Player viewer) {
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

        boolean layeringEnabled = plugin.getConfig().getBoolean("TabList.Name_Layering.Enable", true);

        for (Player target : Bukkit.getOnlinePlayers()) {
            String sortingTeamName = buildSortingTeamName(lp, target);
            Team sortTeam = board.getTeam(sortingTeamName);
            if (sortTeam == null) {
                sortTeam = board.registerNewTeam(sortingTeamName);
            }

            if (!sortTeam.hasEntry(target.getName())) {
                for (Team t : board.getTeams()) {
                    if (t.getName().startsWith(NAMETAG_TEAM_PREFIX) && t.hasEntry(target.getName())) {
                        t.removeEntry(target.getName());
                    }
                }
                sortTeam.addEntry(target.getName());
            }

            if (layeringEnabled) {
                applyNameLayeringToTeam(viewer, target, sortTeam);
            } else {
                sortTeam.prefix(Component.empty());
                sortTeam.suffix(Component.empty());
                target.playerListName(null);
            }
        }
    }

    private String buildSortingTeamName(LuckPerms lp, Player target) {
        String priority = "99";
        String group = "default";
        if (lp != null) {
            try {
                User user = lp.getUserManager().getUser(target.getUniqueId());
                group = (user != null) ? user.getPrimaryGroup() : "default";
                priority = plugin.getConfig().getString("Sorting.Groups." + group, "99");
            } catch (Exception ignored) {}
        }
        String teamName = NAMETAG_TEAM_PREFIX + priority + group;
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);
        return teamName;
    }

    private void applyNameLayeringToTeam(Player viewer, Player target, Team team) {
        String upRaw = plugin.getConfig().getString("TabList.Name_Layering.UpName", "");
        String centerRaw = plugin.getConfig().getString("TabList.Name_Layering.CenterName", "{player}");
        String downRaw = plugin.getConfig().getString("TabList.Name_Layering.DownName", "");

        String centerProcessed = centerRaw.replace("{player}", target.getName());

        Component upComp = !upRaw.isEmpty() ? buildComponent(viewer, upRaw) : Component.empty();
        Component centerComp = buildComponent(viewer, centerProcessed);
        Component downComp = !downRaw.isEmpty() ? buildComponent(viewer, downRaw) : Component.empty();

        Component prefix = upRaw.isEmpty() ? centerComp : upComp.append(Component.newline()).append(centerComp);
        Component suffix = downRaw.isEmpty() ? Component.empty() : Component.newline().append(downComp);

        team.prefix(prefix);
        team.suffix(suffix);
        
        // Esencial para que el prefix/suffix del team se vea en el Tab
        target.playerListName(Component.empty());
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

        // 1. Resolver Placeholders
        if (plugin.isPlaceholderApiPresent()) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        // 2. Si contiene tags de MiniMessage, lo procesamos con MiniMessage
        if (text.contains("<") && text.contains(">")) {
            // Antes de darselo a MiniMessage, convertimos los & legacy a tags de MiniMessage
            // para permitir mezcla de formatos, pero SIN tocar los hex dentro de tags.
            return miniMessage.deserialize(legacyToMiniMessage(text));
        }

        // 3. Si no tiene tags, usamos el serializador Legacy (que ahora SI tiene Hex activado)
        // Primero normalizamos los #RRGGBB y &#RRGGBB al formato &x... para el serializador
        text = translateHexToLegacyFormat(text);
        return legacySerializer.deserialize(text);
    }

    private String legacyToMiniMessage(String text) {
        // Convertir codigos legacy & a tags MiniMessage
        return text
                .replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&l", "<bold>").replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>")
                .replace("&A", "<green>").replace("&B", "<aqua>").replace("&C", "<red>")
                .replace("&D", "<light_purple>").replace("&E", "<yellow>").replace("&F", "<white>")
                .replace("&L", "<bold>").replace("&M", "<strikethrough>").replace("&N", "<underlined>")
                .replace("&O", "<italic>").replace("&R", "<reset>");
    }

    private String translateHexToLegacyFormat(String message) {
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            // Convertir a formato &x&R&R&G&G&B&B
            matcher.appendReplacement(buffer,
                    "&x&" + group.charAt(0) + "&" + group.charAt(1)
                    + "&" + group.charAt(2) + "&" + group.charAt(3)
                    + "&" + group.charAt(4) + "&" + group.charAt(5));
        }
        return matcher.appendTail(buffer).toString();
    }
}
