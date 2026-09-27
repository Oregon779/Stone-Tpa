# Permissions

[← zurück zur Übersicht](Home.md)

Alle Permission-Nodes sind in `plugin.yml` definiert. Der Standardwert
bestimmt, wer den Node automatisch besitzt, wenn ein Server keine eigene
Permission-Verwaltung (z. B. LuckPerms) einsetzt.

| Node | Standard | Wirkung |
|---|---|---|
| `stonetpa.use` | `true` (jeder) | Grundvoraussetzung für **alle** Spielerbefehle (`/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`, `/tptoggle`, `/tpasettings`) sowie die Settings-GUI. Ohne dieses Recht bricht jeder dieser Befehle sofort mit einer lokalisierten „keine Berechtigung"-Meldung ab. |
| `stonetpa.admin` | `op` | Schaltet `/stonetpa reload` und `/stonetpa checkupdate` frei (und blendet sie in der Hilfe ein); Spieler mit diesem Recht erhalten außerdem die Update-Benachrichtigung beim Login. Enthält automatisch `stonetpa.bypass.toggle` als Kind-Permission. |
| `stonetpa.bypass.cooldown` | `op` | Ignoriert den Cooldown zwischen zwei ausgehenden Anfragen (`request.cooldown-seconds`, Standard 10 s). |
| `stonetpa.bypass.delay` | `op` | Überspringt den kompletten Teleport-Countdown (Partikel, Sound, Verzögerung) — der Teleport passiert sofort nach der Annahme. |
| `stonetpa.bypass.toggle` | `op` (auch via `stonetpa.admin`) | Erlaubt das Senden einer Anfrage an Spieler, die eingehende Anfragen über `/tptoggle` deaktiviert haben — das Ziel bekommt die Anfrage trotzdem. |

## Hinweis zur Durchsetzung

Die Permissions werden bewusst **im Code** geprüft (z. B.
`player.hasPermission("stonetpa.use")` in jedem Command-Handler), nicht
über das `permission:`-Feld in `plugin.yml`. Der Grund: Bukkit würde einen
Befehl sonst schon vor dem eigenen `onCommand()` blockieren, und mit einer
leeren `permission-message` dabei komplett stillschweigend ohne jede
Rückmeldung — das war ursprünglich ein Bug in diesem Plugin, siehe
[Changelog](Changelog.md#bugfixes). Für Serveradmins ändert sich dadurch
nichts an der Konfiguration selbst: Alle fünf Nodes lassen sich ganz normal
über LuckPerms & Co. vergeben.
