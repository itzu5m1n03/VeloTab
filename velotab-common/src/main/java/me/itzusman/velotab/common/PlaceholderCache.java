/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de caché para placeholders que no cambian en cada tick.
 */
public class PlaceholderCache {

    // Caché por jugador: UUID -> (Placeholder -> Valor)
    private final Map<UUID, Map<String, CachedValue>> playerCache = new ConcurrentHashMap<>();
    // Caché global: Placeholder -> Valor
    private final Map<String, CachedValue> globalCache = new ConcurrentHashMap<>();

    public void set(UUID uuid, String placeholder, String value, long ttlMillis) {
        playerCache.computeIfAbsent(uuid, k -> new HashMap<>())
                   .put(placeholder, new CachedValue(value, ttlMillis));
    }

    public void setGlobal(String placeholder, String value, long ttlMillis) {
        globalCache.put(placeholder, new CachedValue(value, ttlMillis));
    }

    public String get(UUID uuid, String placeholder) {
        Map<String, CachedValue> cache = playerCache.get(uuid);
        if (cache == null) return null;
        CachedValue val = cache.get(placeholder);
        if (val == null || val.isExpired()) return null;
        return val.value;
    }

    public String getGlobal(String placeholder) {
        CachedValue val = globalCache.get(placeholder);
        if (val == null || val.isExpired()) return null;
        return val.value;
    }

    public void clear(UUID uuid) {
        playerCache.remove(uuid);
    }

    public void clearAll() {
        playerCache.clear();
        globalCache.clear();
    }

    private static class CachedValue {
        final String value;
        final long expiry;

        CachedValue(String value, long ttl) {
            this.value = value;
            this.expiry = System.currentTimeMillis() + ttl;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiry;
        }
    }
}
