/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import me.itzusman.velotab.common.AutoUpdater;
import me.itzusman.velotab.common.IntegrityCheck;
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

public final class VeloTabPaperPlugin extends JavaPlugin implements PluginMessageListener {

    private TabCompleteListener tabCompleteListener;
    private ChatFormatListener chatFormatListener;
    private DisplayManager displayManager;
    private boolean placeholderApiPresent;
    private boolean luckPermsPresent;
    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLang();

        placeholderApiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        luckPermsPresent = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;

        if (placeholderApiPresent) {
            checkPapiExpansions();
        }

        displayManager = new DisplayManager(this);
        displayManager.start();

        registerEvents();

        if (getConfig().getBoolean("network_sync.enable", true)) {
            getServer().getMessenger().registerIncomingPluginChannel(this, "velotab:sync", this);
            getServer().getMessenger().registerOutgoingPluginChannel(this, "velotab:sync");
        }

        // Sistema de Actualizacion Automatica
        checkUpdates();

        VeloTabCommand cmd = new VeloTabCommand(this);
        if (getCommand("velotab") != null) {
            getCommand("velotab").setExecutor(cmd);
            getCommand("velotab").setTabCompleter(cmd);
        }

        IntegrityCheck.printBranding(getLogger());
    }

    private void checkUpdates() {
        boolean checkEnabled = getConfig().getBoolean("update_checker", true);
        boolean autoUpdate = getConfig().getBoolean("auto_update", false);

        if (checkEnabled) {
            UpdateChecker checker = new UpdateChecker(getDescription().getVersion());
            checker.getLatestInfo((latestVersion, downloadUrl) -> {
                if (checker.isNewer(latestVersion)) {
                    getLogger().warning("¡Nueva version disponible: " + latestVersion + "!");
                    
                    if (autoUpdate && !downloadUrl.isEmpty()) {
                        // Usar la carpeta 'update' de Bukkit para un reemplazo limpio al reiniciar
                        File updateDir = new File(getDataFolder().getParentFile(), "update");
                        if (!updateDir.exists()) updateDir.mkdirs();
                        
                        File targetFile = new File(updateDir, "VeloTab.jar");
                        AutoUpdater.downloadUpdate(downloadUrl, targetFile, getLogger(), () -> {
                            // Opcional: Avisar a OPs online
                            Bukkit.getScheduler().runTask(this, () -> {
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    if (p.isOp()) {
                                        p.sendMessage(ChatColor.GREEN + "[VeloTab] Se ha descargado una nueva version (" + latestVersion + "). Reinicia el servidor para aplicarla.");
                                    }
                                }
                            });
                        });
                    } else {
                        getLogger().info("Descarga la nueva version en: https://github.com/itzu5m1n03/VeloTab/releases");
                    }
                }
            });
        }
    }

    private void checkPapiExpansions() {
        String[] expansions = {"luckperms", "player", "server", "bungee"};
        for (String ext : expansions) {
            if (!PlaceholderAPI.containsPlaceholders("%" + ext + "_")) {
                getLogger().warning("Falta la expansion de PAPI: " + ext + ". Instala con /papi ecloud download " + ext);
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
        reloadConfig();
        loadLang();
        
        HandlerList.unregisterAll(this);
        registerEvents();
        
        displayManager.stop();
        displayManager.start();
        
        getLogger().info("Plugin recargado exitosamente.");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("velotab:sync")) return;
        reloadPlugin();
    }

    public void loadLang() {
        String lang = getConfig().getString("language", "es").toLowerCase();
        File langDir = new File(getDataFolder(), "lang");
        if (!langDir.exists()) langDir.mkdirs();
        
        File langFile = new File(langDir, lang + ".yml");
        if (!langFile.exists()) {
            saveResource("lang/es.yml", false);
            saveResource("lang/en.yml", false);
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        InputStream defLangStream = getResource("lang/" + lang + ".yml");
        if (defLangStream != null) {
            langConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8)));
        }
    }

    public String getLangMessage(String path) {
        String msg = langConfig.getString(path, "Message missing: " + path);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public boolean isPlaceholderApiPresent() { return placeholderApiPresent; }
    public boolean isLuckPermsPresent() { return luckPermsPresent; }
}
