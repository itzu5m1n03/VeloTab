/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import java.util.logging.Logger;

public class IntegrityCheck {

    public static void printBranding(Logger logger) {
        logger.info("------------------------------------------------");
        logger.info(" VeloTab v" + Constants.VERSION + " - Official Release");
        logger.info(" Created by: " + Constants.AUTHOR);
        logger.info(" Website: " + Constants.WEBSITE);
        logger.info(" This plugin is protected by copyright.");
        logger.info("------------------------------------------------");
    }
}
