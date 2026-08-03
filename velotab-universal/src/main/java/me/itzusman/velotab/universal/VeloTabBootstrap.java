/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.universal;

import java.util.logging.Logger;

/**
 * Esta clase actua como el punto de entrada universal que detecta la plataforma.
 * Nota: En la practica, cada plataforma requiere un archivo de metadatos diferente 
 * (plugin.yml, bungee.yml, velocity-plugin.json), por lo que el JAR contendra todos
 * y cada plataforma cargara su clase correspondiente.
 */
public class VeloTabBootstrap {
    
    public enum PlatformType {
        PAPER, SPIGOT, VELOCITY, BUNGEECORD, UNKNOWN
    }

    public static PlatformType detectPlatform() {
        try {
            Class.forName("io.papermc.paper.event.server.AsyncTabCompleteEvent");
            return PlatformType.PAPER;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("org.bukkit.Bukkit");
                return PlatformType.SPIGOT;
            } catch (ClassNotFoundException e2) {
                try {
                    Class.forName("com.velocitypowered.api.proxy.ProxyServer");
                    return PlatformType.VELOCITY;
                } catch (ClassNotFoundException e3) {
                    try {
                        Class.forName("net.md_5.bungee.api.ProxyServer");
                        return PlatformType.BUNGEECORD;
                    } catch (ClassNotFoundException e4) {
                        return PlatformType.UNKNOWN;
                    }
                }
            }
        }
    }
}
