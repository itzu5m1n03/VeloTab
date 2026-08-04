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

    // Serializador Legacy usando '&' como caracter de color (compatible con legacyAmpersand)
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    // MiniMessage para tags como <gradient:...>, <bold>, etc.
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // Patron para &#RRGGBB y #RRGGBB
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})|(?<![&])#([A-Fa-f0-9]{6})");

    private BukkitRunnable updateTask;

    // Prefijo unico para los teams de Name Layering
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

    // -------------------------------------------------------------------------
    // TabList: Header, Footer y Name Layering
    // -------------------------------------------------------------------------

    private void updateTabList(Player player) {
        if (!plugin.getConfig().getBoolean("TabList.Enable", true)) return;

        List<String> headerLines = plugin.getConfig().getStringList("TabList.Header");
        List<String> footerLines = plugin.getConfig().getStringList("TabList.Footer");

        player.sendPlayerListHeaderAndFooter(
                buildComponent(player, headerLines),
                buildComponent(player, footerLines)
        );

        // Aplicar Sorting global y Name Layering a todos los jugadores
        applyGlobalSortingAndNames(player);
    }

    /**
     * Para cada jugador online, aplica el sorting por grupo (LuckPerms) y el
     * Name Layering (UpName / CenterName / DownName) usando Teams de Scoreboard.
     *
     * El Name Layering funciona de la siguiente manera:
     *   - Team.prefix  = UpName   (linea superior, visible en el TabList)
     *   - Team entry   = nombre del jugador (CenterName se aplica como displayName)
     *   - Team.suffix  = DownName (linea inferior, visible en el TabList)
     *
     * Esto es el unico metodo que el protocolo de Minecraft soporta para mostrar
     * texto extra arriba/abajo del nombre en el TabList sin mods del cliente.
     */
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

            // Asegurar que el jugador solo pertenece a un team vt_
            if (!sortTeam.hasEntry(target.getName())) {
                for (Team t : board.getTeams()) {
                    if (t.getName().startsWith("vt_") && t.hasEntry(target.getName())) {
                        t.removeEntry(target.getName());
                    }
                }
                sortTeam.addEntry(target.getName());
            }

            // Aplicar Name Layering usando prefix/suffix del Team
            if (layeringEnabled) {
                applyNameLayeringToTeam(viewer, target, sortTeam);
            } else {
                // Sin layering: limpiar prefix/suffix y restaurar nombre
                sortTeam.prefix(Component.empty());
                sortTeam.suffix(Component.empty());
                target.playerListName(null);
            }
        }
    }

    /**
     * Construye el nombre del team de sorting para un jugador.
     * Formato: vt_{prioridad}{grupo} (max 16 chars)
     */
    private String buildSortingTeamName(LuckPerms lp, Player target) {
        String teamName = NAMETAG_TEAM_PREFIX + "99default";
        if (lp != null) {
            try {
                User user = lp.getUserManager().getUser(target.getUniqueId());
                String group = (user != null) ? user.getPrimaryGroup() : "default";
                String priority = plugin.getConfig().getString("Sorting.Groups." + group, "99");
                teamName = NAMETAG_TEAM_PREFIX + priority + group;
            } catch (Exception ignored) {}
        }
        // Los nombres de team tienen un maximo de 16 caracteres en Minecraft
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);
        return teamName;
    }

    /**
     * Aplica el Name Layering al team del jugador objetivo.
     *
     * El protocolo de Minecraft muestra en el TabList:
     *   [Team.prefix][nombre del jugador][Team.suffix]
     *
     * Para simular 3 lineas usamos:
     *   - prefix = UpName + "\n" + CenterName_coloreado
     *   - entry  = nombre real del jugador (invisible con color negro o vacio)
     *   - suffix = "\n" + DownName
     *
     * NOTA: En versiones modernas de Paper (1.20+) el TabList SI acepta
     * saltos de linea dentro de prefix/suffix de Teams, lo que permite
     * el efecto de 3 lineas. Esto es diferente a playerListName() que
     * no acepta newlines.
     */
    private void applyNameLayeringToTeam(Player viewer, Player target, Team team) {
        String upRaw    = plugin.getConfig().getString("TabList.Name_Layering.UpName", "");
        String centerRaw = plugin.getConfig().getString("TabList.Name_Layering.CenterName", "{player}");
        String downRaw  = plugin.getConfig().getString("TabList.Name_Layering.DownName", "");

        // Reemplazar {player} con el nombre real del jugador
        String centerProcessed = centerRaw.replace("{player}", target.getName());

        // Construir componentes individuales
        Component upComp     = !upRaw.isEmpty()   ? buildComponent(viewer, upRaw)     : Component.empty();
        Component centerComp = buildComponent(viewer, centerProcessed);
        Component downComp   = !downRaw.isEmpty() ? buildComponent(viewer, downRaw)   : Component.empty();

        // --- Estrategia de prefix/suffix para el TabList ---
        // prefix = UpName + newline + CenterName
        // suffix = newline + DownName  (solo si hay DownName)
        // La entrada del team (nombre del jugador) se oculta con un color transparente
        // usando un playerListName vacio o con el nombre real si no hay center distinto.

        Component prefix;
        if (!upRaw.isEmpty()) {
            prefix = upComp.append(Component.newline()).append(centerComp);
        } else {
            prefix = centerComp;
        }

        Component suffix;
        if (!downRaw.isEmpty()) {
            suffix = Component.newline().append(downComp);
        } else {
            suffix = Component.empty();
        }

        team.prefix(prefix);
        team.suffix(suffix);

        // Ocultar el nombre "real" del jugador en el TabList para que no se duplique
        // Usamos un espacio vacio como playerListName para que el nombre del team
        // (prefix + entry + suffix) sea lo unico visible.
        // La entry del team es el nombre real del jugador (target.getName()), que
        // queda "sandwiched" entre prefix y suffix. Para evitar duplicacion,
        // configuramos el playerListName como un componente vacio.
        target.playerListName(Component.empty());
    }

    // -------------------------------------------------------------------------
    // Scoreboard lateral
    // -------------------------------------------------------------------------

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
            // Actualizar titulo dinamicamente
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
                // Entrada unica por linea usando codigos de color invisibles
                String entry = "§" + Integer.toHexString(i / 16) + "§" + Integer.toHexString(i % 16) + "§r";
                team.addEntry(entry);
                obj.getScore(entry).setScore(size - i);
            }

            team.prefix(buildComponent(player, line));
        }
    }

    // -------------------------------------------------------------------------
    // Motor de construccion de componentes (colores arreglados)
    // -------------------------------------------------------------------------

    private Component buildComponent(Player player, List<String> lines) {
        Component finalComp = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            finalComp = finalComp.append(buildComponent(player, lines.get(i)));
            if (i < lines.size() - 1) finalComp = finalComp.append(Component.newline());
        }
        return finalComp;
    }

    /**
     * Construye un Component desde un String, soportando:
     *   - Placeholders de PlaceholderAPI (%placeholder%)
     *   - Colores hex: #RRGGBB y &#RRGGBB
     *   - Colores legacy: &a, &b, &c, &l, &n, etc.
     *   - Tags de MiniMessage: <gradient:...>, <bold>, <color:#RRGGBB>, etc.
     */
    private Component buildComponent(Player player, String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. Resolver Placeholders de PlaceholderAPI
        if (plugin.isPlaceholderApiPresent()) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        // 2. Detectar si el texto contiene tags de MiniMessage (<gradient:...>, <bold>, etc.)
        //    En ese caso, primero convertimos los codigos legacy &x y hex &#RRGGBB a
        //    formato MiniMessage para que todo sea procesado por un solo motor.
        if (containsMiniMessageTags(text)) {
            text = legacyAndHexToMiniMessage(text);
            return miniMessage.deserialize(text);
        }

        // 3. Para texto puramente legacy: convertir &#RRGGBB / #RRGGBB al formato
        //    &x&R&R&G&G&B&B que entiende LegacyComponentSerializer.legacyAmpersand()
        //    CORRECCION: se usa '&' (ampersand) NO '§' (seccion)
        text = translateHexToAmpersand(text);
        return legacySerializer.deserialize(text);
    }

    /**
     * Detecta si el texto contiene tags de MiniMessage como <gradient:...>, <bold>,
     * <color:#...>, <red>, <aqua>, etc.
     */
    private boolean containsMiniMessageTags(String text) {
        return text.contains("<gradient") || text.contains("<color:") || text.contains("<rainbow")
                || text.contains("<bold>") || text.contains("<italic>") || text.contains("<underlined>")
                || text.contains("<strikethrough>") || text.contains("<obfuscated>") || text.contains("<reset>")
                || text.contains("<red>") || text.contains("<green>") || text.contains("<blue>")
                || text.contains("<yellow>") || text.contains("<gold>") || text.contains("<aqua>")
                || text.contains("<white>") || text.contains("<black>") || text.contains("<gray>")
                || text.contains("<dark_");
    }

    /**
     * Convierte codigos legacy (&a, &b, &l, etc.) y hex (&#RRGGBB, #RRGGBB) a
     * formato MiniMessage para ser procesados por MiniMessage.miniMessage().
     */
    private String legacyAndHexToMiniMessage(String text) {
        // Primero convertir hex &#RRGGBB y #RRGGBB a <color:#RRGGBB>
        Matcher hexMatcher = hexPattern.matcher(text);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            String group = hexMatcher.group(1) != null ? hexMatcher.group(1) : hexMatcher.group(2);
            hexMatcher.appendReplacement(hexBuffer, "<color:#" + group + ">");
        }
        text = hexMatcher.appendTail(hexBuffer).toString();

        // Luego convertir codigos legacy & a tags MiniMessage
        return text
                .replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&l", "<bold>").replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>")
                // Mayusculas tambien
                .replace("&A", "<green>").replace("&B", "<aqua>").replace("&C", "<red>")
                .replace("&D", "<light_purple>").replace("&E", "<yellow>").replace("&F", "<white>")
                .replace("&L", "<bold>").replace("&M", "<strikethrough>").replace("&N", "<underlined>")
                .replace("&O", "<italic>").replace("&R", "<reset>");
    }

    /**
     * Convierte &#RRGGBB y #RRGGBB al formato &x&R&R&G&G&B&B que entiende
     * LegacyComponentSerializer.legacyAmpersand().
     *
     * CORRECCION CRITICA: Se usa '&' (ampersand) en lugar de '§' (seccion).
     * El serializador legacyAmpersand() espera '&' como caracter de escape,
     * NO el caracter de seccion §. Usar § causaba que los colores hex no se
     * renderizaran correctamente.
     */
    private String translateHexToAmpersand(String message) {
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            // CORRECTO: usar '&' para compatibilidad con legacyAmpersand()
            matcher.appendReplacement(buffer,
                    "&x&" + group.charAt(0) + "&" + group.charAt(1)
                    + "&" + group.charAt(2) + "&" + group.charAt(3)
                    + "&" + group.charAt(4) + "&" + group.charAt(5));
        }
        return matcher.appendTail(buffer).toString();
    }
}
