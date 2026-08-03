package me.itzusman.velotab.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class VeloTabBungeePlugin extends Plugin {

    private Configuration config;
    private final Set<String> forceHide = new HashSet<>();
    private final Set<String> alwaysShow = new HashSet<>();

    @Override
    public void onEnable() {
        loadConfig();
        getProxy().getPluginManager().registerListener(this, new TabFilterBungee(this));
        getLogger().info("VeloTab (Bungee/Waterfall) habilitado.");
    }

    public void loadConfig() {
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            forceHide.clear();
            for (String s : config.getStringList("Tab_Hide.force_hide")) forceHide.add(s.toLowerCase());
            alwaysShow.clear();
            for (String s : config.getStringList("Tab_Hide.always_show")) alwaysShow.add(s.toLowerCase());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Configuration getConfig() {
        return config;
    }

    public Set<String> getForceHide() {
        return forceHide;
    }

    public Set<String> getAlwaysShow() {
        return alwaysShow;
    }
}
