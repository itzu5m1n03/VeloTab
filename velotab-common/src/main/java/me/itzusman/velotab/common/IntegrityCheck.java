/*
 * Copyright (c) 2026 ItzUsman (itzusman.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import java.util.Base64;

/**
 * Proteccion de identidad para evitar que otros cambien el nombre del autor.
 */
public class IntegrityCheck {
    
    // "SXR6VXNtYW4=" es "ItzUsman" en Base64
    // "aXR6dXNtYW4ubmV0bGlmeS5hcHA=" es "itzusman.netlify.app" en Base64
    private static final String AUTHOR_ENC = "SXR6VXNtYW4=";
    private static final String WEB_ENC = "aXR6dXNtYW4ubmV0bGlmeS5hcHA=";

    public static String getAuthor() {
        return new String(Base64.getDecoder().decode(AUTHOR_ENC));
    }

    public static String getWebsite() {
        return new String(Base64.getDecoder().decode(WEB_ENC));
    }

    public static void printBranding(java.util.logging.Logger logger) {
        logger.info("------------------------------------------------");
        logger.info(" VeloTab v1.3.0 - Official Release");
        logger.info(" Created by: " + getAuthor());
        logger.info(" Website: " + getWebsite());
        logger.info(" This plugin is protected by copyright.");
        logger.info("------------------------------------------------");
    }
}
