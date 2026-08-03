/*
 * Copyright (c) 2026 ItzUsman (itzusman.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import me.itzusman.velotab.common.IntegrityCheck;
import me.itzusman.velotab.common.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class VeloTabPaperPlugin extends JavaPlugin implements PluginMessageListener {

    private TabCompleteListener tabCompleteListener;
    private ChatFormatListener chatFormatListener;
    private boolean placeholderApiPresent;
    private boolean luckPermsPresent;
    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLang();

        placeholderApiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        luckPermsPresent = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;

        tabCompleteListener = new TabCompleteListener(this);
        chatFormatListener = new ChatFormatListener(this, placeholderApiPresent);

        getServer().getPluginManager().registerEvents(tabCompleteListener, this);
        getServer().getPluginManager().registerEvents(chatFormatListener, this);

        // Registro de canal para sincronizacion
        if (getConfig().getBoolean("network_sync.enable", true)) {
            getServer().getMessenger().registerIncomingPluginChannel(this, "velotab:sync", this);
            getServer().getMessenger().registerOutgoingPluginChannel(this, "velotab:sync");
        }

        // Update Checker
        if (getConfig().getBoolean("update_checker", true)) {
            new UpdateChecker(getDescription().getVersion()).getVersion(latest -> {
                if (new UpdateChecker(getDescription().getVersion()).isNewer(latest)) {
                    getLogger().warning("¡Nueva version disponible: " + latest + "! Descargala en GitHub.");
                }
            });
        }

        if (getCommand("velotab") != null) {
            getCommand("velotab").setExecutor(new VeloTabCommand(this));
        }

        IntegrityCheck.printBranding(getLogger());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("velotab:sync")) return;
        // Aqui se recibiria la config serializada del Proxy para auto-actualizarse
        getLogger().info("Recibida actualizacion de configuracion desde el Proxy.");
        // (Logica de deserializacion y recarga en caliente)
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
