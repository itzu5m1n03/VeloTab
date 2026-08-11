/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarManager {

    private final VeloTabPaperPlugin plugin;
    private BukkitRunnable updateTask;

    public ActionBarManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        FileConfiguration config = plugin.getCustomConfig("actionbar");
        if (!config.getBoolean("Enable", false)) return;

        int interval = config.getInt("Update_Interval", 20);
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                String text = config.getString("Text", "&7Ping: &a%player_ping%ms");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Component comp = plugin.getDisplayManager().buildComponent(player, text);
                    player.sendActionBar(comp);
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
}
