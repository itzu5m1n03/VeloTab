/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private final String currentVersion;

    public UpdateChecker(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void getLatestInfo(final BiConsumer<String, String> consumer) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/" + Constants.GITHUB_REPO + "/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "VeloTab-Updater");

                if (connection.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        
                        String content = response.toString();
                        String version = extractValue(content, "tag_name");
                        String downloadUrl = extractDownloadUrl(content);

                        if (version != null && !version.isEmpty()) {
                            consumer.accept(version, downloadUrl);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private String extractValue(String json, String key) {
        Pattern r = Pattern.compile("\"" + key + "\":\"(.*?)\"");
        Matcher m = r.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String extractDownloadUrl(String json) {
        Pattern r = Pattern.compile("\"browser_download_url\":\"(.*?\\.jar)\"");
        Matcher m = r.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    public boolean isNewer(String latestVersion) {
        String cleanCurrent = currentVersion.replace("v", "").replace("V", "");
        String cleanLatest = latestVersion.replace("v", "").replace("V", "");
        
        try {
            String[] currentParts = cleanCurrent.split("\\.");
            String[] latestParts = cleanLatest.split("\\.");
            
            for (int i = 0; i < Math.min(currentParts.length, latestParts.length); i++) {
                int current = Integer.parseInt(currentParts[i]);
                int latest = Integer.parseInt(latestParts[i]);
                if (latest > current) return true;
                if (current > latest) return false;
            }
            return latestParts.length > currentParts.length;
        } catch (Exception e) {
            return !cleanCurrent.equalsIgnoreCase(cleanLatest);
        }
    }
}
