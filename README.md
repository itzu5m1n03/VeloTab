# VeloTab

Plugin de LobboMax que:
1. **Oculta comandos del autocompletado (tab) automáticamente** según el
   permiso real de cada comando — sin listas manuales por rango. Funciona
   tanto en el **Proxy (Velocity)** como en los **servidores normales
   (Paper)**: Lobby, LandBox, RedesBox.
2. **Formatea el chat por rango de LuckPerms** (solo en Paper), 100%
   personalizable por grupo en `config.yml`.

Compatible con **1.20.1 en adelante**.

## ⚠️ No pude compilarlo yo mismo

Escribí y revisé todo el código a mano, pero en este entorno no tengo
acceso a Maven Central, al repo de PaperMC, al de Velocity ni al de
LuckPerms para bajar las dependencias y compilar los `.jar` de verdad.
**Necesitas compilarlo tú** (o cualquiera con Java 17 + Maven) antes de
usarlo. Trátalo como código fuente listo para compilar, no como un
plugin ya probado en un servidor real — revísalo/pruébalo en un entorno
de test antes de subirlo a producción.

## Estructura del proyecto

```
VeloTab/
├── pom.xml                  <- proyecto padre (compila los 2 modulos juntos)
├── velotab-paper/            <- para Lobby, LandBox, RedesBox
└── velotab-velocity/         <- para el Proxy
```

## Cómo compilarlo

1. Necesitas [Java 17+](https://adoptium.net/) y [Maven](https://maven.apache.org/download.cgi).
2. Revisa las versiones de API en cada `pom.xml` según tu versión exacta
   de Minecraft/Velocity/LuckPerms si usas algo distinto a lo que dejé
   por defecto (Paper 1.20.4, Velocity 3.3.0, LuckPerms 5.4).
3. En la carpeta raíz `VeloTab/`, corre:
   ```
   mvn clean package
   ```
4. Te quedan dos jars:
   - `velotab-paper/target/VeloTab-Paper.jar`
   - `velotab-velocity/target/VeloTab-Velocity.jar`

## Instalación

- **`VeloTab-Paper.jar`** → carpeta `plugins/` de Lobby, LandBox y RedesBox.
  Requiere **LuckPerms** instalado (para el formato de chat y prefijos).
  **PlaceholderAPI** es opcional, solo se usa si lo tienes.
- **`VeloTab-Velocity.jar`** → carpeta `plugins/` del Proxy.

Reinicia cada servidor para que se generen los `config.yml` por defecto,
y dale el permiso `velotab.bypass` a los rangos que deban ver todos los
comandos sin filtrar (developer, fundador, etc.).

## Formato de chat (solo Paper)

En `velotab-paper`'s `config.yml`, bajo `Chat_Format`:

```yaml
Chat_Format:
  Enable: true
  Default_Format: '&8[%luckperms_prefix%&8] &7{player} &8» &7{message}'
  fundador: '&8[&4&lFUNDADOR&8] %luckperms_prefix%&7{player} &8» &f{message}'
  admin: '&8[&cADMIN&8] %luckperms_prefix%&7{player} &8» &f{message}'
  vip: '&8[&aVIP&8] %luckperms_prefix%&7{player} &8» &f{message}'
  # ...agrega/edita el rango que quieras, la clave debe ser
  # exactamente el nombre del grupo en LuckPerms
```

- `{player}` → nombre del jugador.
- `{message}` → el mensaje que escribió.
- `%luckperms_prefix%` / `%luckperms_suffix%` → prefijo/sufijo de LuckPerms.
- Cualquier otro `%placeholder%` se resuelve con PlaceholderAPI si lo tienes.
- Un rango que no tenga entrada propia usa `Default_Format`.

Ya te dejé precargados los 28 rangos de tu red con colores de ejemplo —
cámbialos como quieras, son solo un punto de partida.

## Por qué el chat NO se formatea en el Proxy

Desde Minecraft 1.19 el chat viaja "firmado" (chat signing) por
seguridad. Modificar el texto del chat directamente en Velocity rompe
esa firma o requiere configuraciones poco confiables. Formatearlo en
cada servidor Paper (como ya hace tu ChatManager actual) es el método
estándar y seguro — por eso el Proxy en VeloTab solo filtra el tab, no
toca el chat.

## Limitaciones

- El filtro de tab solo oculta el **nombre del comando** (`/rg`, `/co`,
  etc.), no los argumentos dentro de un comando ya escrito.
- Si un plugin no declaró permiso para su comando, no se puede filtrar
  automáticamente — usa `force_hide` en el config para esos casos.
- El renderer de chat toma el mensaje como texto plano (no conserva
  hover/click events que el cliente pudiera meter en el mensaje
  original); es la misma limitación que tienen la mayoría de plugins de
  formato de chat.
