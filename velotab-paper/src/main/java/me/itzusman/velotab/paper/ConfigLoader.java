/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestiona la carga de múltiples archivos de configuración en subcarpetas.
 */
public class ConfigLoader {

    private final VeloTabPaperPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public ConfigLoader(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        configs.clear();
        
        // Configuración Maestra
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        
        // Módulos
        loadModuleConfig("tablist", "tablist.yml");
        loadModuleConfig("tablist", "groups.yml");
        loadModuleConfig("tablist", "separators.yml");
        
        loadModuleConfig("scoreboard", "scoreboard.yml");
        loadModuleConfig("scoreboard", "pages.yml");
        
        loadModuleConfig("chat", "chat.yml");
        loadModuleConfig("chat", "announcements.yml");
        loadModuleConfig("chat", "tags.yml");
        
        loadModuleConfig("security", "security.yml");
        loadModuleConfig("security", "discord_webhooks.yml");
        
        loadModuleConfig("animations", "animations.yml");
        
        // Idiomas
        loadModuleConfig("lang", "es.yml");
        loadModuleConfig("lang", "en.yml");
    }

    private void loadModuleConfig(String folder, String fileName) {
        File dir = new File(plugin.getDataFolder(), folder);
        if (!dir.exists()) dir.mkdirs();
        
        File file = new File(dir, fileName);
        if (!file.exists()) {
            plugin.saveResource(folder + "/" + fileName, false);
        }
        
        String key = folder + "/" + fileName.replace(".yml", "");
        configs.put(key, YamlConfiguration.loadConfiguration(file));
    }

    public FileConfiguration get(String path) {
        return configs.getOrDefault(path, new YamlConfiguration());
    }
    
    public boolean isModuleEnabled(String name) {
        return plugin.getConfig().getBoolean("modules." + name, true);
    }
}
