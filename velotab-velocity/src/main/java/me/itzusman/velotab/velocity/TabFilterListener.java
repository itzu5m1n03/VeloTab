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
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TabFilterListener {

    private final VelocityConfigLoader configLoader;

    public TabFilterListener(VelocityConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onCommands(PlayerAvailableCommandsEvent event) {
        CommentedConfigurationNode config = configLoader.get("security/security");
        if (config == null || !config.node("Command_Hiding", "Enable").getBoolean(true)) return;

        Player player = event.getPlayer();
        if (player.hasPermission(config.node("Command_Hiding", "Bypass_Permission").getString("velotab.bypass"))) return;

        RootCommandNode<CommandSource> root = (RootCommandNode<CommandSource>) event.getRootNode();
        List<CommandNode<CommandSource>> children = new ArrayList<>(root.getChildren());

        try {
            List<String> forceHide = config.node("Command_Hiding", "Force_Hide").getList(String.class);
            List<String> alwaysShow = config.node("Command_Hiding", "Always_Show").getList(String.class);

            for (CommandNode<CommandSource> child : children) {
                String name = child.getName().toLowerCase();

                if (alwaysShow != null && alwaysShow.contains(name)) continue;
                if (forceHide != null && forceHide.contains(name)) {
                    removeChild(root, child.getName());
                    continue;
                }

                if (!child.canUse(player)) {
                    removeChild(root, child.getName());
                }
            }
        } catch (Exception ignored) {}
    }

    private void removeChild(RootCommandNode<CommandSource> root, String name) {
        try {
            Field childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            Map<String, CommandNode<?>> children = (Map<String, CommandNode<?>>) childrenField.get(root);
            children.remove(name);

            Field literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);
            Map<String, CommandNode<?>> literals = (Map<String, CommandNode<?>>) literalsField.get(root);
            literals.remove(name);
        } catch (Exception ignored) {}
    }
}
