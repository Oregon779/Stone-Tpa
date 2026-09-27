# StoneTPA

Vollständiges Teleport-Request-System (TPA) für Paper-Server: `/tpa` und
`/tpahere` mit Bestätigungs-GUI, Countdown mit Partikel-Spirale und
konfigurierbaren Benachrichtigungen (Chat/Actionbar/Bossbar/Title), einem
Spieler-Settings-Menü, Bedrock-/Floodgate-Erkennung und einem
Modrinth-Update-Checker.

## Anforderungen

| | |
|---|---|
| **Server** | Paper (kein Spigot/Bukkit-only, kein Fabric/Forge/Velocity/BungeeCord) |
| **API-Version** | `26.2` ([`paper-api` `26.2.build.124-stable`](docs/Building.md)) |
| **Java** | 25 |
| **Soft-Dependency** | [Floodgate](https://geysermc.org/wiki/floodgate/) (optional, nur für Bedrock-Erkennung) |

## Dokumentation

Die vollständige Dokumentation liegt im [`docs/`](docs/Home.md)-Ordner:

- **[Home / Übersicht](docs/Home.md)** — Einstiegspunkt mit Links auf alles
- **[Features](docs/Features.md)** — genaue Beschreibung jedes Features
- **[Commands](docs/Commands.md)** — Befehlsreferenz
- **[Permissions](docs/Permissions.md)** — alle Permission-Nodes und ihre Wirkung
- **[Configuration](docs/Configuration.md)** — `config.yml` Abschnitt für Abschnitt erklärt
- **[Building](docs/Building.md)** — Build aus dem Quellcode, Abhängigkeiten, Tests
- **[Changelog](docs/Changelog.md)** — Portierungs- und Bugfix-Historie

## Schnellstart

```bash
mvn clean package
```

Die fertige JAR liegt danach unter `target/StoneTPA-1.0.0.jar` und kann in
den `plugins/`-Ordner eines Paper-26.2-Servers gelegt werden. Details
(insbesondere zu den benötigten Paper-Core-Abhängigkeiten, die nicht über
den öffentlichen papermc-Repository-Proxy auflösbar sein können) stehen in
[docs/Building.md](docs/Building.md).

## Lizenz / Autor

Stone Plugins.
