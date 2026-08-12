# 🚀 VeloTab: La Suite Visual Definitiva para Minecraft

**VeloTab** es un plugin modular de alto rendimiento diseñado para transformar la experiencia visual y administrativa de tu servidor. Desde un TabList dinámico hasta un sistema de seguridad de chat avanzado, VeloTab ofrece funcionalidades de nivel premium de forma totalmente gratuita y optimizada.

---

## 🌟 Características Principales

### 📊 TabList & Nametags
*   **Modular & Grupos:** Configura cabeceras y pies de página únicos basados en rangos o permisos.
*   **Nametags Pro:** Añade líneas físicas arriba y abajo del nombre del jugador mediante paquetes de red (vía ProtocolLib).
*   **Ordenación Inteligente:** Integración nativa con **LuckPerms** para mantener la lista organizada por jerarquía.
*   **Separadores Visuales:** Crea categorías como `--- STAFF ---` o `--- JUGADORES ---` en la lista.
*   **Objetivos en Vivo:** Muestra la salud o el ping de los jugadores en tiempo real.

### 📋 Scoreboard Avanzado
*   **Sistema de Páginas:** Crea múltiples tableros que rotan automáticamente con transiciones fluidas.
*   **Privacidad:** Los jugadores pueden ocultar su propio tablero con `/velotab toggle`.
*   **Cero Parpadeo:** Diseñado para actualizarse sin molestar a la vista del jugador.

### 🛡️ Seguridad & Moderación
*   **Ocultación de Comandos:** Esconde del autocompletado (TAB) cualquier comando para el que el jugador no tenga permiso.
*   **Discord Webhooks:** Sincroniza el chat del juego con Discord y recibe alertas de seguridad al instante.
*   **Protección de Chat:** Suite completa con AntiSpam, AntiSwear (filtro de palabras), Caps Blocker y Repeat Blocker.

### 💬 Chat Interactivo
*   **Menciones Reales:** Notificaciones visuales (ActionBar) y sonoras al mencionar a alguien con `@Jugador`.
*   **Tags de Jugador:** Permite que los usuarios elijan títulos personalizados mediante permisos.
*   **Anuncios Automáticos:** Sistema de mensajes globales periódicos con soporte para animaciones.

### 🌈 Visuales & Animaciones
*   **Motor de Animaciones:** Crea textos dinámicos con frames personalizados.
*   **Efectos Especiales:** Soporte para efectos de texto deslizante (**Scroller**) y arcoíris dinámico (**Rainbow**).
*   **Colores Modernos:** Compatibilidad total con colores clásicos (`&`) y Hexadecimales (`&#RRGGBB`).

---

## ⚙️ Infraestructura Profesional
*   **Arquitectura Modular:** Configuración organizada en carpetas independientes para cada módulo.
*   **Soporte MySQL/MariaDB:** Sincronización de datos y estados entre todos los servidores de tu red.
*   **Expansión PAPI:** VeloTab ofrece sus propios placeholders para ser usados en otros plugins.
*   **Máximo Rendimiento:** Tareas asíncronas y caché de placeholders para garantizar 20 TPS constantes.

---

## 🛠️ Compatibilidad Universal
VeloTab detecta automáticamente tu entorno y activa las funciones correspondientes. Un solo archivo funciona en:
- **Servidores:** Paper, Spigot, Purpur, Folia (1.20.x - 1.21+).
- **Proxies:** Velocity, BungeeCord, Waterfall, FlameCord.

---

## 📦 Dependencias
*   **Requerido:** [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi)
*   **Opcional (Recomendado):** 
    *   [LuckPerms](https://modrinth.com/plugin/luckperms): Gestión de rangos y ordenación.
    *   [ProtocolLib](https://modrinth.com/plugin/protocollib): Para la función de Nametags Pro.
    *   [EssentialsX](https://modrinth.com/plugin/essentialsx): Para la detección de AFK.
    *   [Geyser](https://modrinth.com/plugin/geyser): Para la detección de jugadores de Bedrock.

---

## 🛡️ Autoría y Soporte
Este proyecto es propiedad exclusiva de **ItzUsman**.
- **Sitio Web:** [https://itzusm.netlify.app/](https://itzusm.netlify.app/)
- **GitHub:** [VeloTab Repository](https://github.com/itzu5m1n03/VeloTab)

---

## 🔒 Seguridad y Transparencia
VeloTab ha sido diseñado con la seguridad y el rendimiento como prioridades:
- **Sin Conexiones Ocultas:** Las únicas conexiones externas son los Webhooks de Discord (configurables por el usuario) y las actualizaciones de PlaceholderAPI.
- **Uso de ProtocolLib:** Se utiliza exclusivamente para la manipulación segura de paquetes de Nametags, garantizando compatibilidad sin interferir con otros plugins.
- **Código Abierto:** Todo el código fuente está disponible para auditoría en este repositorio.
- **Sin Ofuscación:** El plugin no utiliza técnicas de ofuscación para garantizar la máxima transparencia ante la comunidad y los moderadores.
