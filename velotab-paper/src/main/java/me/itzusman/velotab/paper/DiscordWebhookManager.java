/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.Bukkit;
import org.json.simple.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookManager {

    public static void sendWebhook(String webhookUrl, String username, String avatarUrl, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("TU_WEBHOOK_AQUI")) return;

        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("VeloTab"), () -> {
            try {
                JSONObject json = new JSONObject();
                json.put("content", content);
                json.put("username", username);
                json.put("avatar_url", avatarUrl);

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("Content-Type", "application/json");
                connection.addRequestProperty("User-Agent", "Java-DiscordWebhook-BY-ItzUsman");
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");

                try (OutputStream stream = connection.getOutputStream()) {
                    stream.write(json.toJSONString().getBytes(StandardCharsets.UTF_8));
                    stream.flush();
                }

                connection.getInputStream().close();
                connection.disconnect();
            } catch (Exception e) {
                Bukkit.getLogger().warning("[VeloTab] Error al enviar webhook a Discord: " + e.getMessage());
            }
        });
    }
}
