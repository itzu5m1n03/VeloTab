/*
 * Copyright (c) 2026 ItzUsman (itzusman.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "velotab",
        name = "VeloTab",
        version = "1.0.0",
        description = "Oculta comandos del tab del proxy segun el permiso real.",
        authors = {"ItzUsman"}
)
public class VeloTabVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private VeloTabConfig config;

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

        me.itzusman.velotab.common.IntegrityCheck.printBranding(java.util.logging.Logger.getLogger("VeloTab"));
    }
}
