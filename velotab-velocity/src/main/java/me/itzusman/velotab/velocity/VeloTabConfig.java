package me.itzusman.velotab.velocity;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Carga config.yml para el modulo de Velocity. Si no existe, copia el
 * que viene dentro del jar como plantilla por defecto.
 */
public class VeloTabConfig {

    private final Path dataDirectory;
    private final Logger logger;

    private boolean enabled = true;
    private final Set<String> forceHide = new HashSet<>();
    private final Set<String> alwaysShow = new HashSet<>();
    private boolean hidePrefixed = true;

    public VeloTabConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void load() {
        try {
            Files.createDirectories(dataDirectory);
            Path configPath = dataDirectory.resolve("config.yml");

            if (!Files.exists(configPath)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    }
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();

            CommentedConfigurationNode root = loader.load();

            enabled = root.node("Tab_Hide", "enable").getBoolean(true);

            forceHide.clear();
            for (String s : root.node("Tab_Hide", "force_hide").getList(String.class, List.of())) {
                forceHide.add(s.toLowerCase());
            }

            alwaysShow.clear();
            for (String s : root.node("Tab_Hide", "always_show").getList(String.class, List.of())) {
                alwaysShow.add(s.toLowerCase());
            }

            hidePrefixed = root.node("Tab_Hide", "hide_prefixed_commands").getBoolean(true);

        } catch (IOException e) {
            logger.warn("No se pudo cargar config.yml, usando valores por defecto.", e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<String> getForceHide() {
        return forceHide;
    }

    public Set<String> getAlwaysShow() {
        return alwaysShow;
    }

    public boolean isHidePrefixed() {
        return hidePrefixed;
    }
}
