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
 * autocompletado (tab) del proxy, basandose estrictamente en permisos.
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
        List<CommandNode<CommandSource>> children = new ArrayList<>(root.getChildren());

        for (CommandNode<CommandSource> child : children) {
            String name = child.getName().toLowerCase();

            // 1. Whitelist / Blacklist manual
            if (config.getAlwaysShow().contains(name)) continue;
            if (config.getForceHide().contains(name)) {
                removeChild(root, child.getName());
                continue;
            }

            // 2. Comprobacion nativa de Brigadier
            if (!child.canUse(player)) {
                removeChild(root, child.getName());
                continue;
            }

            // 3. Filtrado Estricto para comandos con prefijo (ej: litebans:checkwarn)
            // Si el comando tiene ":" y el jugador no tiene el permiso explicito, lo quitamos.
            if (name.contains(":")) {
                String guessedPermission = name.replace(":", ".");
                if (!player.hasPermission(guessedPermission) && !player.hasPermission(name) && !player.hasPermission("velotab.admin")) {
                    removeChild(root, child.getName());
                }
            }
            
            // 4. Caso especial: comandos de plugins conocidos que a veces se saltan el canUse
            if (name.equals("listwarn") || name.equals("listwarnings") || name.equals("checkwarn")) {
                if (!player.hasPermission("litebans." + name) && !player.hasPermission("velotab.admin")) {
                    removeChild(root, child.getName());
                }
            }
        }
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

            Field argumentsField = CommandNode.class.getDeclaredField("arguments");
            argumentsField.setAccessible(true);
            Map<String, CommandNode<?>> arguments = (Map<String, CommandNode<?>>) argumentsField.get(root);
            arguments.remove(name);
        } catch (Exception ignored) {}
    }
}
