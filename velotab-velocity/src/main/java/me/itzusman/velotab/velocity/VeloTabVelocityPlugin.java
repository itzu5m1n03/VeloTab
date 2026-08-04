/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
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
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import me.itzusman.velotab.common.AutoUpdater;
import me.itzusman.velotab.common.IntegrityCheck;
import me.itzusman.velotab.common.UpdateChecker;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Plugin(
        id = "velotab",
        name = "VeloTab",
        version = "1.6.1",
        description = "Suite completa de TabList y Seguridad creada por ItzUsman.",
        authors = {"ItzUsman"}
)
public class VeloTabVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private VeloTabConfig config;
    public static final MinecraftChannelIdentifier SYNC_CHANNEL = MinecraftChannelIdentifier.from("velotab:sync");
    private static final String VERSION = "1.6.1";

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

        // Update Checker & Auto Updater
        checkUpdates();

        IntegrityCheck.printBranding(java.util.logging.Logger.getLogger("VeloTab"));
    }

    private void checkUpdates() {
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("VeloTab");
        
        UpdateChecker checker = new UpdateChecker(VERSION);
        checker.getLatestInfo((latestVersion, downloadUrl) -> {
            if (checker.isNewer(latestVersion)) {
                logger.warn("¡Nueva version de VeloTab disponible: {}!", latestVersion);
                
                if (!downloadUrl.isEmpty()) {
                    File targetFile = new File("plugins/VeloTab.jar");
                    AutoUpdater.downloadUpdate(downloadUrl, targetFile, julLogger, () -> {
                        logger.info("La actualizacion de Velocity se aplicara en el proximo reinicio.");
                    });
                }
            }
        });
    }
}
