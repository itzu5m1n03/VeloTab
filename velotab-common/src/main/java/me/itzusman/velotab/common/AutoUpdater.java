/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class AutoUpdater {

    public static void downloadUpdate(String downloadUrl, File targetFile, Logger logger, Runnable onSuccess) {
        new Thread(() -> {
            try {
                logger.info("Descargando actualizacion de VeloTab desde GitHub...");
                
                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "VeloTab-AutoUpdater");

                // Seguir redirecciones (GitHub las usa para los assets)
                int status = connection.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    String newUrl = connection.getHeaderField("Location");
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                }

                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(targetFile)) {
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                
                logger.info("¡VeloTab se ha actualizado correctamente! La nueva version se aplicara en el proximo reinicio.");
                if (onSuccess != null) onSuccess.run();
                
            } catch (Exception e) {
                logger.warning("Error al descargar la actualizacion de VeloTab: " + e.getMessage());
            }
        }).start();
    }
}
