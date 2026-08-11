/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad centralizada para el manejo de colores en todas las plataformas.
 * Soporta colores Legacy (&) y Hexadecimal (&#RRGGBB o #RRGGBB).
 */
public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})|#([A-Fa-f0-9]{6})");

    /**
     * Traduce los colores Hexadecimales al formato legacy de Minecraft (&x&r&r&g&g&b&b).
     * Tambien limpia cualquier tag de MiniMessage residual.
     *
     * @param message El mensaje a traducir.
     * @return El mensaje con colores traducidos.
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";

        // 1. Eliminar tags de MiniMessage/HTML para evitar inyecciones visuales
        message = message.replaceAll("<[^>]*>", "");

        // 2. Traducir Hexadecimales
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(buffer, "&x&" + group.charAt(0) + "&" + group.charAt(1)
                    + "&" + group.charAt(2) + "&" + group.charAt(3)
                    + "&" + group.charAt(4) + "&" + group.charAt(5));
        }
        matcher.appendTail(buffer);
        
        return buffer.toString();
    }
}
