/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class BungeeReloadCommand extends Command {

    private final VeloTabBungeePlugin plugin;

    public BungeeReloadCommand(VeloTabBungeePlugin plugin) {
        super("vtreload", "velotab.admin");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reload();
        sender.sendMessage(new TextComponent("§aVeloTab recargado correctamente en toda la red."));
    }
}
