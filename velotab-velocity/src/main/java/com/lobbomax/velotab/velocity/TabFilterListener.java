package com.lobbomax.velotab.velocity;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.proxy.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        RootCommandNode<CommandSource> root = (RootCommandNode<CommandSource>) event.getRootNode();

        // Hay que copiar la lista de hijos antes de iterar, porque
        // removeChild la modifica mientras se recorre.
        List<CommandNode<CommandSource>> children = new ArrayList<>(root.getChildren());

        for (CommandNode<CommandSource> child : children) {
            String name = child.getName().toLowerCase();

            if (config.getAlwaysShow().contains(name)) {
                continue;
            }

            if (config.getForceHide().contains(name)) {
                removeChild(root, child.getName());
                continue;
            }

            // canUse() ejecuta el "requires" real registrado para ese
            // comando (el mismo chequeo de permiso que usa el propio
            // comando al ejecutarse).
            if (!child.canUse(player)) {
                removeChild(root, child.getName());
            }
        }
    }

    private void removeChild(RootCommandNode<CommandSource> root, String name) {
        try {
            Field childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            Map<String, CommandNode<CommandSource>> children = (Map<String, CommandNode<CommandSource>>) childrenField.get(root);
            children.remove(name);

            Field literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);
            Map<String, CommandNode<CommandSource>> literals = (Map<String, CommandNode<CommandSource>>) literalsField.get(root);
            literals.remove(name);

            Field argumentsField = CommandNode.class.getDeclaredField("arguments");
            argumentsField.setAccessible(true);
            Map<String, CommandNode<CommandSource>> arguments = (Map<String, CommandNode<CommandSource>>) argumentsField.get(root);
            arguments.remove(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
