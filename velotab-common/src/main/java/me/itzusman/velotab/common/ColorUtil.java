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
 * Soporta colores Legacy (&) y Hexadecimal (&#RRGGBB).
 */
public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final char COLOR_CHAR = '\u00A7'; // §

    /**
     * Traduce los colores Hexadecimales y Legacy al formato interno de Minecraft.
     *
     * @param message El mensaje a traducir.
     * @return El mensaje con colores traducidos.
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";

        // 1. Traducir Hexadecimales (&#RRGGBB -> §x§R§R§G§G§B§B)
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            String group = matcher.group(1);
            String replacement = COLOR_CHAR + "x" 
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5);
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        message = buffer.toString();

        // 2. Traducir Legacy (& -> §)
        return translateAlternateColorCodes('&', message);
    }

    /**
     * Versión propia de translateAlternateColorCodes para evitar dependencia de Bukkit en Common.
     */
    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = COLOR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    /**
     * Limpia todos los códigos de color de un mensaje.
     */
    public static String stripColor(String message) {
        if (message == null) return null;
        return Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-ORX]").matcher(message).replaceAll("");
    }
}
