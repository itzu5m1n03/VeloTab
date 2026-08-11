/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import me.itzusman.velotab.common.AnimationManager;
import me.itzusman.velotab.common.AutoUpdater;
import me.itzusman.velotab.common.Constants;
import me.itzusman.velotab.common.IntegrityCheck;
import me.itzusman.velotab.common.PlaceholderCache;
import me.itzusman.velotab.common.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import me.clip.placeholderapi.PlaceholderAPI;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class VeloTabPaperPlugin extends JavaPlugin implements PluginMessageListener {

    private TabCompleteListener tabCompleteListener;
    private ChatFormatListener chatFormatListener;
    private DisplayManager displayManager;
    private BossBarManager bossBarManager;
    private ActionBarManager actionBarManager;
    private HookManager hookManager;
    private AnimationManager animationManager;
    private PlaceholderCache placeholderCache;

    private boolean placeholderApiPresent;
    private boolean luckPermsPresent;
    
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    @Override
    public void onEnable() {
        // Cargar todas las configuraciones
        loadAllConfigs();

        placeholderApiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        luckPermsPresent = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;

        if (placeholderApiPresent) {
            checkPapiExpansions();
        }

        this.animationManager = new AnimationManager();
        this.placeholderCache = new PlaceholderCache();
        this.hookManager = new HookManager(this);
        this.hookManager.init();

        this.displayManager = new DisplayManager(this);
        this.displayManager.start();

        this.bossBarManager = new BossBarManager(this);
        this.bossBarManager.start();

        this.actionBarManager = new ActionBarManager(this);
        this.actionBarManager.start();

        registerEvents();

        if (getCustomConfig("tablist").getBoolean("network_sync.enable", true)) {
            getServer().getMessenger().registerIncomingPluginChannel(this, Constants.SYNC_CHANNEL, this);
            getServer().getMessenger().registerOutgoingPluginChannel(this, Constants.SYNC_CHANNEL);
        }

        checkUpdates();

        VeloTabCommand cmd = new VeloTabCommand(this);
        if (getCommand("velotab") != null) {
            getCommand("velotab").setExecutor(cmd);
            getCommand("velotab").setTabCompleter(cmd);
        }

        IntegrityCheck.printBranding(getLogger());
    }

    @Override
    public void onDisable() {
        if (displayManager != null) displayManager.stop();
        if (bossBarManager != null) bossBarManager.stop();
        if (actionBarManager != null) actionBarManager.stop();
        
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        
        HandlerList.unregisterAll(this);
        getLogger().info("VeloTab se ha deshabilitado correctamente.");
    }

    public void loadAllConfigs() {
        configs.clear();
        String[] configFiles = {"tablist", "scoreboard", "bossbar", "actionbar", "chat", "security", "animations", "lang/es", "lang/en"};
        for (String name : configFiles) {
            loadCustomConfig(name);
        }
        
        // Registrar animaciones si el manager ya existe
        if (animationManager != null) {
            animationManager.clear();
            FileConfiguration animConfig = getCustomConfig("animations");
            if (animConfig.contains("animations")) {
                for (String key : animConfig.getConfigurationSection("animations").getKeys(false)) {
                    int interval = animConfig.getInt("animations." + key + ".interval", 20);
                    java.util.List<String> frames = animConfig.getStringList("animations." + key + ".frames");
                    animationManager.registerAnimation(key, frames, interval);
                }
            }
        }
    }

    private void loadCustomConfig(String name) {
        File file = new File(getDataFolder(), name + ".yml");
        if (!file.exists()) {
            saveResource(name + ".yml", false);
        }
        configs.put(name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name, 
                    YamlConfiguration.loadConfiguration(file));
    }

    public FileConfiguration getCustomConfig(String name) {
        return configs.getOrDefault(name, new YamlConfiguration());
    }

    private void checkUpdates() {
        FileConfiguration tabConfig = getCustomConfig("tablist");
        boolean checkEnabled = tabConfig.getBoolean("update_checker", true);
        boolean autoUpdate = tabConfig.getBoolean("auto_update", false);

        if (checkEnabled) {
            UpdateChecker checker = new UpdateChecker(Constants.VERSION);
            checker.getLatestInfo((latestVersion, downloadUrl) -> {
                if (checker.isNewer(latestVersion)) {
                    getLogger().warning("¡Nueva versión disponible: " + latestVersion + "!");
                    
                    if (autoUpdate && !downloadUrl.isEmpty()) {
                        File updateDir = new File(getDataFolder().getParentFile(), "update");
                        File targetFile = new File(updateDir, "VeloTab.jar");
                        AutoUpdater.downloadUpdate(downloadUrl, targetFile, getLogger(), () -> {
                            Bukkit.getScheduler().runTask(this, () -> {
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    if (p.isOp()) {
                                        p.sendMessage(ChatColor.GREEN + "[VeloTab] Se ha descargado la versión " + latestVersion + ". Reinicia el servidor para aplicarla.");
                                    }
                                }
                            });
                        });
                    } else {
                        getLogger().info("Descarga la nueva versión en: https://github.com/" + Constants.GITHUB_REPO + "/releases");
                    }
                }
            });
        }
    }

    private void checkPapiExpansions() {
        String[] expansions = {"luckperms", "player", "server", "bungee"};
        for (String ext : expansions) {
            if (!PlaceholderAPI.containsPlaceholders("%" + ext + "_")) {
                getLogger().warning("Falta la expansión de PAPI: " + ext + ". Instala con /papi ecloud download " + ext);
            }
        }
    }

    private void registerEvents() {
        tabCompleteListener = new TabCompleteListener(this);
        chatFormatListener = new ChatFormatListener(this, placeholderApiPresent);
        getServer().getPluginManager().registerEvents(tabCompleteListener, this);
        getServer().getPluginManager().registerEvents(chatFormatListener, this);
    }

    public void reloadPlugin() {
        loadAllConfigs();
        
        HandlerList.unregisterAll(this);
        registerEvents();
        
        if (displayManager != null) {
            displayManager.stop();
            displayManager.start();
        }
        if (bossBarManager != null) {
            bossBarManager.stop();
            bossBarManager.start();
        }
        if (actionBarManager != null) {
            actionBarManager.stop();
            actionBarManager.start();
        }
        
        getLogger().info("Plugin recargado exitosamente.");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(Constants.SYNC_CHANNEL)) return;
        reloadPlugin();
    }

    public String getLangMessage(String path) {
        String lang = getCustomConfig("tablist").getString("language", "es").toLowerCase();
        FileConfiguration langConfig = getCustomConfig(lang);
        String msg = langConfig.getString(path, "Message missing: " + path);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public boolean isPlaceholderApiPresent() { return placeholderApiPresent; }
    public boolean isLuckPermsPresent() { return luckPermsPresent; }
    public DisplayManager getDisplayManager() { return displayManager; }
    public HookManager getHookManager() { return hookManager; }
    public AnimationManager getAnimationManager() { return animationManager; }
    public PlaceholderCache getPlaceholderCache() { return placeholderCache; }
}
