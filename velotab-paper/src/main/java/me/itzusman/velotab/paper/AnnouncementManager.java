/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnnouncementManager {

    private final VeloTabPaperPlugin plugin;
    private BukkitRunnable task;
    private final Random random = new Random();
    private int currentIndex = 0;

    public AnnouncementManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        FileConfiguration config = plugin.getConfigLoader().get("chat/announcements");
        if (!config.getBoolean("enable", true)) return;

        int interval = config.getInt("interval", 6000);
        task = new BukkitRunnable() {
            @Override
            public void run() {
                sendAnnouncement();
            }
        };
        task.runTaskTimer(plugin, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void sendAnnouncement() {
        FileConfiguration config = plugin.getConfigLoader().get("chat/announcements");
        ConfigurationSection section = config.getConfigurationSection("messages");
        if (section == null) return;

        List<String> keys = new ArrayList<>(section.getKeys(false));
        if (keys.isEmpty()) return;

        String key;
        if (config.getBoolean("random", true)) {
            key = keys.get(random.nextInt(keys.size()));
        } else {
            if (currentIndex >= keys.size()) currentIndex = 0;
            key = keys.get(currentIndex++);
        }

        List<String> lines = section.getStringList(key);
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String line : lines) {
                player.sendMessage(plugin.getDisplayManager().buildComponent(player, line));
            }
        }
    }
}
