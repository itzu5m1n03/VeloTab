/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la rotación de páginas del Scoreboard.
 */
public class ScoreboardPageManager {

    private final VeloTabPaperPlugin plugin;
    private final List<ScoreboardPage> pages = new ArrayList<>();
    private int currentPageIndex = 0;
    private long lastRotationTime = 0;

    public ScoreboardPageManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        pages.clear();
        FileConfiguration config = plugin.getConfigLoader().get("scoreboard/pages");
        ConfigurationSection section = config.getConfigurationSection("pages");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String title = section.getString(key + ".title");
                List<String> lines = section.getStringList(key + ".lines");
                pages.add(new ScoreboardPage(title, lines));
            }
        }
        if (pages.isEmpty()) {
            pages.add(new ScoreboardPage("&bVeloTab", List.of("&7Configura tus páginas en", "&fscoreboard/pages.yml")));
        }
    }

    public ScoreboardPage getCurrentPage(long currentTicks) {
        FileConfiguration config = plugin.getConfigLoader().get("scoreboard/scoreboard");
        if (config.getBoolean("Rotate_Pages", true) && pages.size() > 1) {
            int interval = config.getInt("Rotation_Interval", 200);
            if (System.currentTimeMillis() - lastRotationTime > (interval * 50L)) {
                currentPageIndex = (currentPageIndex + 1) % pages.size();
                lastRotationTime = System.currentTimeMillis();
            }
        }
        return pages.get(currentPageIndex);
    }

    public static class ScoreboardPage {
        private final String title;
        private final List<String> lines;

        public ScoreboardPage(String title, List<String> lines) {
            this.title = title;
            this.lines = lines;
        }

        public String getTitle() { return title; }
        public List<String> getLines() { return lines; }
    }
}
