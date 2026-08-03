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
import java.util.function.Consumer;

public class UpdateChecker {

    private final String currentVersion;
    private final String githubRepo = "itzu5m1n03/VeloTab";

    public UpdateChecker(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void getVersion(final Consumer<String> consumer) {
        try (InputStream inputStream = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest").openStream();
             Scanner scanner = new Scanner(inputStream)) {
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.next());
            }
            // Simplificado para este entorno: buscamos "tag_name":"vX.X.X"
            String content = response.toString();
            if (content.contains("tag_name")) {
                int start = content.indexOf("tag_name") + 11;
                int end = content.indexOf("\"", start);
                consumer.accept(content.substring(start, end));
            }
        } catch (IOException exception) {
            // Silencioso si falla el internet
        }
    }

    public boolean isNewer(String latestVersion) {
        return !currentVersion.equalsIgnoreCase(latestVersion.replace("v", ""));
    }
}
