# Commands

[← zurück zur Übersicht](Home.md)

Alle Befehle unterhalb von `stonetpa` benötigen die Permission
`stonetpa.use` (Standard: jeder Spieler hat sie). Details zu jedem
Permission-Node stehen in [Permissions](Permissions.md).

| Befehl | Aliase | Syntax | Beschreibung |
|---|---|---|---|
| `/tpa` | — | `/tpa <spieler>` | Teleport-Anfrage an `<spieler>` senden (du kommst zu ihm) |
| `/tpahere` | — | `/tpahere <spieler>` | `<spieler>` bitten, zu dir zu kommen |
| `/tpaccept` | `/tpaaccept` | `/tpaccept [spieler]` | Eingehende Anfrage annehmen; `[spieler]` nötig, wenn mehrere offen sind |
| `/tpdeny` | `/tpadeny` | `/tpdeny [spieler]` | Eingehende Anfrage ablehnen; `[spieler]` nötig, wenn mehrere offen sind |
| `/tpacancel` | — | `/tpacancel` | Eigene ausgehende Anfrage stornieren |
| `/tptoggle` | — | `/tptoggle` | Eingehende Anfragen komplett an-/ausschalten |
| `/tpasettings` | `/tpset`, `/tpsettings` | `/tpasettings` | Settings-Menü öffnen (Anfragen + Teleport-Effekte) |
| `/stonetpa` | `/stpa` | `/stonetpa <reload\|help\|checkupdate>` | Verwaltung (siehe unten) |

## `/stonetpa`-Unterbefehle

| Unterbefehl | Permission | Beschreibung |
|---|---|---|
| *(kein Argument)* / `help` | `stonetpa.use` | Zeigt Hilfetext, gefiltert nach den Rechten des Aufrufers |
| `reload` | `stonetpa.admin` | Lädt `config.yml` und alle `messages.yml`-Dateien neu |
| `checkupdate` | `stonetpa.admin` | Löst die Modrinth-Update-Prüfung sofort aus |

`/stonetpa` selbst trägt bewusst **keine** eigene Permission in
`plugin.yml` — der Befehl bleibt für jeden sichtbar und ausführbar, filtert
aber intern anhand von `stonetpa.use`/`stonetpa.admin`, welche
Unterbefehle in der Hilfe erscheinen bzw. tatsächlich ausgeführt werden
können. Alle anderen Befehle prüfen `stonetpa.use` ebenfalls **im Code**,
nicht über das `permission:`-Feld in `plugin.yml` — der Hintergrund dazu
steht in [Changelog](Changelog.md#bugfixes).

## Tab-Completion

- `/tpa`, `/tpahere`: schlägt Namen aller online Spieler vor (außer dir
  selbst)
- `/tpaccept`, `/tpdeny`: schlägt nur die Namen der Spieler vor, die dir
  gerade tatsächlich eine Anfrage geschickt haben
- `/stonetpa`: schlägt `reload`, `help`, `checkupdate` vor
