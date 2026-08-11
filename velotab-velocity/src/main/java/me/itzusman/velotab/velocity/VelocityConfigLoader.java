/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.velocity;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VelocityConfigLoader {

    private final Path dataDirectory;
    private final Map<String, CommentedConfigurationNode> configs = new HashMap<>();

    public VelocityConfigLoader(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void loadAll() {
        configs.clear();
        loadModule("security", "security.yml");
        loadModule("tablist", "tablist.yml");
    }

    private void loadModule(String folder, String fileName) {
        try {
            File dir = new File(dataDirectory.toFile(), folder);
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, fileName);
            if (!file.exists()) {
                try (InputStream in = getClass().getResourceAsStream("/" + folder + "/" + fileName)) {
                    if (in != null) Files.copy(in, file.toPath());
                }
            }
            
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(file.toPath()).build();
            configs.put(folder + "/" + fileName.replace(".yml", ""), loader.load());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CommentedConfigurationNode get(String path) {
        return configs.get(path);
    }
}
