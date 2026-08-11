/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.itzusman.velotab.common.Constants;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VeloTabExpansion extends PlaceholderExpansion {

    private final VeloTabPaperPlugin plugin;

    public VeloTabExpansion(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "velotab";
    }

    @Override
    public @NotNull String getAuthor() {
        return Constants.AUTHOR;
    }

    @Override
    public @NotNull String getVersion() {
        return Constants.VERSION;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("version")) {
            return Constants.VERSION;
        }

        if (params.equalsIgnoreCase("is_afk")) {
            return String.valueOf(plugin.getHookManager().isAFK(player));
        }

        if (params.equalsIgnoreCase("is_bedrock")) {
            return String.valueOf(plugin.getHookManager().isBedrock(player));
        }

        return null;
    }
}
