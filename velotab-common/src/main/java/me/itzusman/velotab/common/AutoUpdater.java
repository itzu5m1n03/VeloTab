/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class AutoUpdater {

    /**
     * Descarga la actualizacion desde GitHub de forma segura.
     *
     * @param downloadUrl URL de descarga directa del JAR.
     * @param targetFile  Archivo destino (normalmente en la carpeta update).
     * @param logger      Logger de la plataforma.
     * @param onSuccess   Tarea a ejecutar tras una descarga exitosa.
     */
    public static void downloadUpdate(String downloadUrl, File targetFile, Logger logger, Runnable onSuccess) {
        new Thread(() -> {
            try {
                logger.info("Iniciando descarga de VeloTab desde GitHub...");
                
                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "VeloTab-AutoUpdater");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                // Manejo manual de redirecciones si InstanceFollowRedirects falla
                int status = connection.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    String newUrl = connection.getHeaderField("Location");
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestProperty("User-Agent", "VeloTab-AutoUpdater");
                }

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    if (targetFile.exists()) targetFile.delete();
                    if (!targetFile.getParentFile().exists()) targetFile.getParentFile().mkdirs();

                    try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                         FileOutputStream out = new FileOutputStream(targetFile)) {
                        
                        byte[] dataBuffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(dataBuffer, 0, 4096)) != -1) {
                            out.write(dataBuffer, 0, bytesRead);
                        }
                    }

                    if (targetFile.length() > 0) {
                        logger.info("¡VeloTab descargado con éxito! Se aplicará en el próximo reinicio.");
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        logger.warning("Fallo en la descarga: El archivo está vacío.");
                        targetFile.delete();
                    }
                } else {
                    logger.warning("Error al descargar la actualización. HTTP: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                logger.warning("Error crítico al descargar VeloTab: " + e.getMessage());
            }
        }).start();
    }
}
