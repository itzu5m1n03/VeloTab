/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class VelocityReloadCommand implements SimpleCommand {

    private final VeloTabVelocityPlugin plugin;

    public VelocityReloadCommand(VeloTabVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("velotab.admin")) {
            invocation.source().sendMessage(Component.text("No tienes permiso para esto.", NamedTextColor.RED));
            return;
        }

        plugin.reload();
        invocation.source().sendMessage(Component.text("VeloTab recargado correctamente en toda la red.", NamedTextColor.GREEN));
    }
}
