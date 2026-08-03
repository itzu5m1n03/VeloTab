package me.itzusman.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class VeloTabPaperPlugin extends JavaPlugin {

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

        if (getCommand("velotab") != null) {
            getCommand("velotab").setExecutor((sender, command, label, args) -> {
                if (!sender.hasPermission("velotab.admin")) {
                    sender.sendMessage(getLangMessage("no-permission"));
                    return true;
                }

                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("reload")) {
                        reloadConfig();
                        loadLang();
                        sender.sendMessage(getLangMessage("reload-success"));
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("info")) {
                        sender.sendMessage(getLangMessage("info-header"));
                        sender.sendMessage(getLangMessage("info-version").replace("{version}", getDescription().getVersion()));
                        sender.sendMessage(getLangMessage("info-creator").replace("{link}", "itzusman.netlify.app"));
                        sender.sendMessage(getLangMessage("info-luckperms").replace("{status}", luckPermsPresent ? getLangMessage("status-enabled") : getLangMessage("status-disabled")));
                        sender.sendMessage(getLangMessage("info-placeholderapi").replace("{status}", placeholderApiPresent ? getLangMessage("status-enabled") : getLangMessage("status-disabled")));
                        return true;
                    }
                }
                
                sender.sendMessage("§e[VeloTab] Uso: /velotab <info|reload>");
                return true;
            });
        }

        getLogger().info("VeloTab v" + getDescription().getVersion() + " por ItzUsman habilitado.");
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

    public boolean isPlaceholderApiPresent() {
        return placeholderApiPresent;
    }

    public boolean isLuckPermsPresent() {
        return luckPermsPresent;
    }
}
