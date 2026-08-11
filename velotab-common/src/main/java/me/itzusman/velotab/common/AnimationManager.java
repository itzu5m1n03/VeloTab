/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestiona las animaciones de texto para todas las plataformas.
 */
public class AnimationManager {

    private final Map<String, Animation> animations = new HashMap<>();

    /**
     * Registra una nueva animación.
     * @param name Nombre identificador (ej: "server_name").
     * @param frames Lista de textos que componen la animación.
     * @param interval Intervalo en ticks entre frames.
     */
    public void registerAnimation(String name, List<String> frames, int interval) {
        animations.put(name, new Animation(frames, interval));
    }

    /**
     * Obtiene el frame actual de una animación basándose en el tiempo actual del servidor.
     * @param name Nombre de la animación.
     * @param currentTicks Ticks totales transcurridos en el servidor.
     * @return El texto del frame actual coloreado.
     */
    public String getCurrentFrame(String name, long currentTicks) {
        Animation anim = animations.get(name);
        if (anim == null) return "";
        return anim.getFrame(currentTicks);
    }

    public void clear() {
        animations.clear();
    }

    private static class Animation {
        private final List<String> frames;
        private final int interval;

        public Animation(List<String> frames, int interval) {
            this.frames = new ArrayList<>();
            for (String f : frames) {
                this.frames.add(ColorUtil.colorize(f));
            }
            this.interval = Math.max(1, interval);
        }

        public String getFrame(long currentTicks) {
            if (frames.isEmpty()) return "";
            int index = (int) ((currentTicks / interval) % frames.size());
            return frames.get(index);
        }
    }
}
