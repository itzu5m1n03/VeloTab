/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {

    private final VeloTabPaperPlugin plugin;
    private final Map<UUID, BossBar> activeBars = new HashMap<>();
    private BukkitRunnable updateTask;

    public BossBarManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        FileConfiguration config = plugin.getCustomConfig("bossbar");
        if (!config.getBoolean("Enable", false)) return;

        int interval = config.getInt("Update_Interval", 40);
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateForPlayer(player);
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeForPlayer(player);
        }
        activeBars.clear();
    }

    private void updateForPlayer(Player player) {
        FileConfiguration config = plugin.getCustomConfig("bossbar");
        String title = config.getString("Title", "&bBienvenido a VeloTab");
        float progress = (float) config.getDouble("Progress", 1.0);
        String colorStr = config.getString("Color", "BLUE");
        String overlayStr = config.getString("Overlay", "PROGRESS");

        BossBar.Color color = parseColor(colorStr);
        BossBar.Overlay overlay = parseOverlay(overlayStr);
        Component titleComp = plugin.getDisplayManager().buildComponent(player, title);

        BossBar bar = activeBars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(titleComp, progress, color, overlay);
            player.showBossBar(bar);
            activeBars.put(player.getUniqueId(), bar);
        } else {
            bar.name(titleComp);
            bar.progress(progress);
            bar.color(color);
            bar.overlay(overlay);
        }
    }

    public void removeForPlayer(Player player) {
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private BossBar.Color parseColor(String s) {
        try { return BossBar.Color.valueOf(s.toUpperCase()); }
        catch (Exception e) { return BossBar.Color.BLUE; }
    }

    private BossBar.Overlay parseOverlay(String s) {
        try { return BossBar.Overlay.valueOf(s.toUpperCase()); }
        catch (Exception e) { return BossBar.Overlay.PROGRESS; }
    }
}
