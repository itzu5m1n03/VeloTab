/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.BiConsumer;

public class UpdateChecker {

    private final String currentVersion;
    private final String githubRepo = "itzu5m1n03/VeloTab";

    public UpdateChecker(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    /**
     * Obtiene la version y la URL de descarga del JAR.
     */
    public void getLatestInfo(final BiConsumer<String, String> consumer) {
        new Thread(() -> {
            try (InputStream inputStream = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest").openStream();
                 Scanner scanner = new Scanner(inputStream)) {
                
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.next());
                }
                String content = response.toString();
                
                String version = "";
                String downloadUrl = "";

                // Extraer tag_name
                if (content.contains("tag_name")) {
                    int start = content.indexOf("tag_name") + 11;
                    int end = content.indexOf("\"", start);
                    version = content.substring(start, end);
                }

                // Extraer browser_download_url para VeloTab.jar
                if (content.contains("browser_download_url")) {
                    int start = content.indexOf("browser_download_url") + 23;
                    int end = content.indexOf("\"", start);
                    downloadUrl = content.substring(start, end);
                }

                if (!version.isEmpty()) {
                    consumer.accept(version, downloadUrl);
                }
            } catch (IOException ignored) {}
        }).start();
    }

    public boolean isNewer(String latestVersion) {
        String cleanCurrent = currentVersion.replace("v", "");
        String cleanLatest = latestVersion.replace("v", "");
        return !cleanCurrent.equalsIgnoreCase(cleanLatest);
    }
}
