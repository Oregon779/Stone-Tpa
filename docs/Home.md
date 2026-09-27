# StoneTPA — Dokumentation

Dieser Ordner ersetzt ein GitHub-Wiki: Das Wiki-Feature ist für dieses
private Repo aktuell nicht aktiviert, und weder das Aktivieren noch das
Wiki-Git-Repo selbst (`Stone-Tpa.wiki.git`, ein von diesem Repo getrennter
Git-Namespace) sind über den aktuellen GitHub-Zugriff dieser Session
erreichbar. Die Seiten hier sind deshalb bewusst genauso aufgebaut wie
einzelne Wiki-Seiten — jede Datei deckt ein Thema komplett ab.

## Seiten

| Seite | Inhalt |
|---|---|
| **[Features](Features.md)** | Genaue Beschreibung jedes Features: TPA-Anfragen, Send-GUI, Teleport-Countdown, Benachrichtigungen, Settings-Menü, Bedrock-Erkennung, Update-Checker, Mehrsprachigkeit, Config-Migration |
| **[Commands](Commands.md)** | Alle Befehle mit Syntax, Aliassen und benötigter Permission |
| **[Permissions](Permissions.md)** | Alle Permission-Nodes, ihr Standardwert und was sie genau freischalten |
| **[Configuration](Configuration.md)** | `config.yml` Abschnitt für Abschnitt erklärt (inkl. Sprache/`messages.yml`) |
| **[Building](Building.md)** | Voraussetzungen, Build-Befehl, Test-Suite, Hinweise zu den Paper-Kernabhängigkeiten |
| **[Changelog](Changelog.md)** | Portierung auf Paper 26.2/Java 25 sowie die anschließende Bug-Hunt- und Performance-Runde |

## Kurzüberblick

StoneTPA ist ein Teleport-Request-Plugin (TPA) für Paper-Server:

- `/tpa <spieler>` / `/tpahere <spieler>` — Anfrage senden (mit
  Bestätigungs-GUI dazwischen)
- `/tpaccept` / `/tpdeny` / `/tpacancel` — Anfrage annehmen/ablehnen/stornieren
- `/tptoggle` / `/tpasettings` — eingehende Anfragen und Teleport-Effekte
  pro Spieler an-/ausschalten
- `/stonetpa reload|checkupdate|help` — Verwaltung

Details zu jedem einzelnen Punkt in [Features](Features.md).
