/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
        if (!plugin.getConfig().getBoolean("ActionBar.Enable", false)) return;

        int interval = plugin.getConfig().getInt("ActionBar.Update_Interval", 20);
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                String text = plugin.getConfig().getString("ActionBar.Text", "&7Ping: &a%player_ping%ms");
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
