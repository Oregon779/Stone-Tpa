# Features

[← zurück zur Übersicht](Home.md)

## TPA-Anfragen (`/tpa`, `/tpahere`)

`/tpa <spieler>` bittet den Zielspieler, zu dir zu kommen; `/tpahere
<spieler>` bittet ihn, dich zu sich zu holen. Vor dem Versand prüft das
Plugin: Ziel existiert und ist nicht man selbst, Ziel akzeptiert überhaupt
Anfragen (siehe `/tptoggle`), und der Absender ist nicht im Cooldown. Pro
Spieler ist immer nur eine ausgehende Anfrage gleichzeitig möglich.
Anfragen laufen nach `request.expire-seconds` (Standard 60 s) automatisch
ab und benachrichtigen dabei beide Seiten.

## Bestätigungs-GUI beim Senden (Send-GUI)

Statt die Anfrage sofort abzuschicken, öffnet `/tpa`/`/tpahere` standardmäßig
ein Inventory-Menü (`send-gui.enabled`, Standard: an) mit dem Kopf des
Zielspielers, seiner Dimension (Overworld/Nether/End) und seinem Ping
(grün/gelb/rot je nach Wert) — beides wird sekündlich live aktualisiert,
solange das Menü offen ist. Der grüne Button sendet die Anfrage, der rote
bricht ab. Ist `send-gui.enabled: false` gesetzt, wird die Anfrage direkt
ohne GUI verschickt.

> Technisch läuft dafür ein einziger gemeinsamer Scheduler-Task, der alle
> gerade offenen Send-GUIs durchläuft (nicht ein Task pro Spieler) — wichtig
> für Server mit vielen gleichzeitigen Spielern, siehe [Changelog](Changelog.md).

## Annehmen / Ablehnen (`/tpaccept`, `/tpdeny`)

Nimmt die einzige offene eingehende Anfrage an oder ab. Gibt es mehrere
gleichzeitig, muss der Absendername als Argument angegeben werden
(Tab-Completion schlägt die Namen der Absender vor). Beim Annehmen wird je
nach Anfrage-Typ entweder der Absender zum Akzeptierenden teleportiert (bei
`/tpa`) oder umgekehrt (bei `/tpahere`).

## Anfrage stornieren (`/tpacancel`)

Bricht die eigene, noch offene ausgehende Anfrage ab und informiert das
Ziel darüber.

## Anfragen an/aus schalten (`/tptoggle`)

Persistenter Schalter pro Spieler (gespeichert in `playerdata.yml`): Ist er
deaktiviert, lehnt das Plugin eingehende Anfragen automatisch mit einer
Hinweismeldung ab — außer der Absender besitzt die Permission
`stonetpa.bypass.toggle`.

## Teleport-Countdown mit Verzögerung

Nach der Annahme läuft standardmäßig ein Countdown von
`teleport.delay-seconds` (Standard: 5 Sekunden), bevor der Sprung ausgeführt
wird. Während dieser Zeit gilt:

- Bewegung bricht den Teleport ab (`teleport.cancel-on-move`, Standard:
  an) — das gilt sowohl für normales Laufen **als auch** für einen Teleport
  durch irgendetwas anderes (Enderperle, `/warp`, ein anderes Plugin), der
  während des Countdowns passiert. Verlässt die Gegenpartei währenddessen
  den Server, wird ebenfalls abgebrochen.
- Optional Blindheit während der Wartezeit (`teleport.blindness-during-delay`,
  Standard: aus).
- Eine Doppelhelix-Partikelspirale (Farbverlauf von unten nach oben) samt
  Ambient-Sound rotiert um den Spieler; am Ende ein Abschiedspartikel und
  -sound. Lässt sich pro Spieler über die Settings-GUI deaktivieren.
- `stonetpa.bypass.delay` überspringt den gesamten Countdown — der Teleport
  passiert sofort nach der Annahme.

## Benachrichtigungen während Countdown & Ankunft

Frei konfigurierbar als Chat, Actionbar, Bossbar oder Title/Subtitle
(`teleport.countdown.notification`, Standard: Actionbar;
`teleport.arrival.notification`, Standard: Title). Zeigt die verbleibenden
Sekunden bzw. eine Ankunftsmeldung mit dem Namen der Gegenpartei an.

## Fallschaden-Schutz

Nach einem Teleport ist der Spieler für 5 Sekunden (100 Ticks) immun gegen
Fallschaden (`teleport.disable-fall-damage`, Standard: an) — verhindert
Schaden durch eine ungünstige Landung.

## Ankunfts-Sound-Effekt (optional)

Standardmäßig deaktiviert (`teleport.arrival.enabled: false`). Wenn
aktiviert, spielt am Zielort ein zusätzlicher Sound ab (Standard:
Totem-Sound).

## Settings-Menü (`/tpasettings`, Aliase `/tpset`, `/tpsettings`)

GUI mit zwei Umschaltern pro Spieler: eingehende Anfragen an/aus (identisch
zu `/tptoggle`) und Teleport-Partikeleffekte an/aus. Einstellungen werden
dauerhaft in `playerdata.yml` gespeichert (siehe [Building](Building.md) für
Details zur Schreib-Robustheit dieser Datei).

## Bedrock-/Floodgate-Erkennung

Erkennt Floodgate per Reflection (keine harte Abhängigkeit nötig) und
schickt Bedrock-Spielern beim Erhalt einer Anfrage einen alternativen
Hinweistext, der auf Befehle statt klickbare Chat-Komponenten setzt, da
Bedrock-Clients diese oft nicht unterstützen.

## Update-Checker

Fragt periodisch (Standard: alle 60 Minuten,
`update-checker.check-interval-minutes`) die Modrinth-API nach neuen
Versionen des Projekts „stone-tpa" ab. Findet er eine neuere Version,
informiert er beim nächsten Login jeden Spieler mit OP-Status oder
`stonetpa.admin` (inklusive Anzahl Versionen Rückstand) und protokolliert es
in der Konsole. Lässt sich per `/stonetpa checkupdate` manuell auslösen und
über `update-checker.enabled: false` komplett abschalten.

> Versionsstrings von Modrinth werden vor der Anzeige escaped, damit ein
> präparierter Versionsstring keine anklickbaren Chat-Komponenten
> einschleusen kann (siehe [Changelog](Changelog.md)).

## Mehrsprachigkeit

Sprachdateien für Deutsch und Englisch (`languages/de|en/messages.yml`),
umschaltbar über den Schlüssel `language` in `config.yml`. Fehlt ein
Text-Schlüssel in der aktiven Sprache komplett, wird automatisch auf
Englisch zurückgefallen — eine bewusst leer gelassene Zeile/Liste bleibt
dabei aber leer und wird nicht ersetzt.

## Automatische Config-Migration

Beim Start bzw. Reload gleicht das Plugin die vorhandene `config.yml` (und
die `messages.yml`-Dateien) gegen die intern gebündelte Standardvorlage ab
und ergänzt fehlende neue Optionen automatisch, ohne bestehende Werte zu
überschreiben — wichtig bei zukünftigen Plugin-Updates.

## Verwaltungsbefehl `/stonetpa` (Alias `/stpa`)

- `reload` — lädt `config.yml` und Sprachdateien neu (nur `stonetpa.admin`)
- `checkupdate` — löst die Update-Prüfung sofort aus (nur `stonetpa.admin`)
- `help` / kein Argument — zeigt Hilfetext, gefiltert nach den Rechten des
  Aufrufers

Vollständige Befehlssyntax in [Commands](Commands.md), alle Permission-Nodes
in [Permissions](Permissions.md).
