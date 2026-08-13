/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import me.itzusman.velotab.common.Constants;
import me.itzusman.velotab.common.IntegrityCheck;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "velotab",
        name = "VeloTab",
        version = Constants.VERSION,
        description = "Suite Modular de TabList y Seguridad creada por ItzUsman.",
        authors = {Constants.AUTHOR}
)
public class VeloTabVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private VeloTabConfig config;
    public static final MinecraftChannelIdentifier SYNC_CHANNEL = MinecraftChannelIdentifier.from(Constants.SYNC_CHANNEL);

    @Inject
    public VeloTabVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.config = new VeloTabConfig(dataDirectory, logger);
        this.config.load();

        server.getEventManager().register(this, new TabFilterListener(config));
        server.getChannelRegistrar().register(SYNC_CHANNEL);
        
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("vtreload").build(),
            new VelocityReloadCommand(this)
        );

        IntegrityCheck.printBranding(java.util.logging.Logger.getLogger("VeloTab"));
        logger.info("VeloTab v{} (Velocity) habilitado.", Constants.VERSION);
    }

    @Subscribe
    public void onPluginMessage(com.velocitypowered.api.event.connection.PluginMessageEvent event) {
        if (!event.getIdentifier().equals(SYNC_CHANNEL)) return;
        reload();
    }

    public void reload() {
        config.load();
        byte[] data = new byte[0];
        server.getAllServers().forEach(s -> s.sendPluginMessage(SYNC_CHANNEL, data));
        logger.info("Configuración recargada y señal de sincronización enviada a todos los servidores.");
    }

    public VeloTabConfig getConfig() {
        return config;
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        server.getChannelRegistrar().unregister(SYNC_CHANNEL);
        logger.info("VeloTab se ha deshabilitado correctamente.");
    }
}
