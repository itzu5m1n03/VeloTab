package com.lobbomax.velotab.velocity;

import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtra los comandos que Velocity le manda al cliente para el
 * autocompletado (tab) del proxy. Usa el "requires" real de cada
 * comando (equivalente al permiso real de LuckPerms) para decidir si
 * se muestra o no, sin necesidad de listas manuales por rango.
 */
public class TabFilterListener {

    private final VeloTabConfig config;

    public TabFilterListener(VeloTabConfig config) {
        this.config = config;
    }

    @Subscribe
    public void onCommands(PlayerAvailableCommandsEvent event) {
        if (!config.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        if (player.hasPermission("velotab.bypass")) {
            return;
        }

        CommandNode<CommandSource> root = event.getRootNode();

        // Hay que copiar la lista de hijos antes de iterar, porque
        // removeChild la modifica mientras se recorre.
        List<CommandNode<CommandSource>> children = new ArrayList<>(root.getChildren());

        for (CommandNode<CommandSource> child : children) {
            String name = child.getName().toLowerCase();

            if (config.getAlwaysShow().contains(name)) {
                continue;
            }

            if (config.getForceHide().contains(name)) {
                root.removeChild(child.getName());
                continue;
            }

            // canUse() ejecuta el "requires" real registrado para ese
            // comando (el mismo chequeo de permiso que usa el propio
            // comando al ejecutarse).
            if (!child.canUse(player)) {
                root.removeChild(child.getName());
            }
        }
    }
}
