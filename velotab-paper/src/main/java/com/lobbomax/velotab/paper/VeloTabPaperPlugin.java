package com.lobbomax.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class VeloTabPaperPlugin extends JavaPlugin {

    private TabCompleteListener tabCompleteListener;
    private ChatFormatListener chatFormatListener;
    private boolean placeholderApiPresent;
    private boolean luckPermsPresent;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        placeholderApiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        luckPermsPresent = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;

        if (!luckPermsPresent) {
            getLogger().warning("No se encontro LuckPerms. El formato de chat por rango "
                    + "y el prefijo/sufijo no van a funcionar hasta que lo instales.");
        }

        tabCompleteListener = new TabCompleteListener(this);
        chatFormatListener = new ChatFormatListener(this, placeholderApiPresent);

        getServer().getPluginManager().registerEvents(tabCompleteListener, this);
        getServer().getPluginManager().registerEvents(chatFormatListener, this);

        if (getCommand("velotab") != null) {
            getCommand("velotab").setExecutor((sender, command, label, args) -> {
                if (!sender.hasPermission("velotab.admin")) {
                    sender.sendMessage("§cNo tienes permiso para ejecutar este comando.");
                    return true;
                }
                reloadConfig();
                sender.sendMessage("§a[VeloTab] Configuracion recargada.");
                return true;
            });
        }

        getLogger().info("VeloTab (Paper) habilitado.");
    }

    public boolean isPlaceholderApiPresent() {
        return placeholderApiPresent;
    }

    public boolean isLuckPermsPresent() {
        return luckPermsPresent;
    }
}
