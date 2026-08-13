/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.velocity;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.proxy.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Filtra los comandos disponibles en el autocompletado de Velocity.
 */
public class TabFilterListener {

    private final VeloTabConfig config;

    public TabFilterListener(VeloTabConfig config) {
        this.config = config;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onCommands(PlayerAvailableCommandsEvent event) {
        if (!config.isEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("velotab.bypass")) return;

        RootCommandNode<CommandSource> root = (RootCommandNode<CommandSource>) event.getRootNode();
        List<CommandNode<CommandSource>> children = new ArrayList<>(root.getChildren());

        for (CommandNode<CommandSource> child : children) {
            String name = child.getName().toLowerCase();

            if (config.getAlwaysShow().contains(name)) continue;
            if (config.getForceHide().contains(name)) {
                removeChild(root, child.getName());
                continue;
            }

            // Si el jugador no puede usar el comando, lo ocultamos del autocompletado.
            if (!child.canUse(player)) {
                removeChild(root, child.getName());
            }
            
            // Ocultar comandos con prefijo (ej: minecraft:tp) si está habilitado
            if (config.isHidePrefixed() && name.contains(":")) {
                removeChild(root, child.getName());
            }
        }
    }

    private void removeChild(RootCommandNode<CommandSource> root, String name) {
        try {
            Field childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            Map<String, CommandNode<?>> childrenMap = (Map<String, CommandNode<?>>) childrenField.get(root);
            childrenMap.remove(name);

            Field literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);
            Map<String, CommandNode<?>> literalsMap = (Map<String, CommandNode<?>>) literalsField.get(root);
            literalsMap.remove(name);
        } catch (Exception ignored) {}
    }
}
