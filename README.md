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
| **API-Version** | `26.2` ([`paper-api` `26.2.build.124-stable`](WIKI.md#building)) |
| **Java** | 25 |
| **Soft-Dependency** | [Floodgate](https://geysermc.org/wiki/floodgate/) (optional, nur für Bedrock-Erkennung) |

## Dokumentation

Die vollständige Dokumentation steht in **[WIKI.md](WIKI.md)** — Features,
Commands, Permissions, `config.yml` Abschnitt für Abschnitt, Build-Anleitung
und Changelog, alles in einer Datei (Ersatz für das GitHub-Wiki, siehe
[WIKI.md](WIKI.md) für den Hintergrund).

## Schnellstart

```bash
mvn clean package
```

Die fertige JAR liegt danach unter `target/StoneTPA-1.0.0.jar` und kann in
den `plugins/`-Ordner eines Paper-26.2-Servers gelegt werden. Details
(insbesondere zu den benötigten Paper-Core-Abhängigkeiten, die nicht über
den öffentlichen papermc-Repository-Proxy auflösbar sein können) stehen in
[WIKI.md](WIKI.md#building).

## Lizenz / Autor

Stone Plugins.
