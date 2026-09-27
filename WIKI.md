# StoneTPA — Wiki

Vollständige Dokumentation für das StoneTPA-Plugin. Liegt als einzelne
Datei im Repo-Root statt im GitHub-Wiki-Feature: Das Wiki-Feature ist für
dieses private Repo nicht aktiviert, und weder das Aktivieren noch das
Wiki-Git-Repo selbst (`Stone-Tpa.wiki.git`, ein von diesem Repo getrennter
Git-Namespace) sind über den aktuellen GitHub-Zugriff dieser Session
erreichbar — diese Datei ist der gleichwertige Ersatz.

## Inhaltsverzeichnis

- [Überblick](#überblick)
- [Features](#features)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Building](#building)
- [Changelog](#changelog)

## Überblick

StoneTPA ist ein vollständiges Teleport-Request-System (TPA) für
Paper-Server: `/tpa` und `/tpahere` mit Bestätigungs-GUI, Countdown mit
Partikel-Spirale und konfigurierbaren Benachrichtigungen
(Chat/Actionbar/Bossbar/Title), einem Spieler-Settings-Menü,
Bedrock-/Floodgate-Erkennung und einem Modrinth-Update-Checker.

| | |
|---|---|
| **Server** | Paper (kein Spigot/Bukkit-only, kein Fabric/Forge/Velocity/BungeeCord) |
| **API-Version** | `26.2` (`paper-api` `26.2.build.124-stable`) |
| **Java** | 25 |
| **Soft-Dependency** | [Floodgate](https://geysermc.org/wiki/floodgate/) (optional, nur für Bedrock-Erkennung) |

Kurzüberblick über die Befehle:

- `/tpa <spieler>` / `/tpahere <spieler>` — Anfrage senden (mit
  Bestätigungs-GUI dazwischen)
- `/tpaccept` / `/tpdeny` / `/tpacancel` — Anfrage annehmen/ablehnen/stornieren
- `/tptoggle` / `/tpasettings` — eingehende Anfragen und Teleport-Effekte
  pro Spieler an-/ausschalten
- `/stonetpa reload|checkupdate|help` — Verwaltung

Details zu jedem einzelnen Punkt unten in [Features](#features).

## Features

### TPA-Anfragen (`/tpa`, `/tpahere`)

`/tpa <spieler>` bittet den Zielspieler, zu dir zu kommen; `/tpahere
<spieler>` bittet ihn, dich zu sich zu holen. Vor dem Versand prüft das
Plugin: Ziel existiert und ist nicht man selbst, Ziel akzeptiert überhaupt
Anfragen (siehe `/tptoggle`), und der Absender ist nicht im Cooldown. Pro
Spieler ist immer nur eine ausgehende Anfrage gleichzeitig möglich.
Anfragen laufen nach `request.expire-seconds` (Standard 60 s) automatisch
ab und benachrichtigen dabei beide Seiten.

### Bestätigungs-GUI beim Senden (Send-GUI)

Statt die Anfrage sofort abzuschicken, öffnet `/tpa`/`/tpahere` standardmäßig
ein Inventory-Menü (`send-gui.enabled`, Standard: an) mit dem Kopf des
Zielspielers, seiner Dimension (Overworld/Nether/End) und seinem Ping
(grün/gelb/rot je nach Wert) — beides wird sekündlich live aktualisiert,
solange das Menü offen ist. Der grüne Button sendet die Anfrage, der rote
bricht ab. Ist `send-gui.enabled: false` gesetzt, wird die Anfrage direkt
ohne GUI verschickt.

> Technisch läuft dafür ein einziger gemeinsamer Scheduler-Task, der alle
> gerade offenen Send-GUIs durchläuft (nicht ein Task pro Spieler) — wichtig
> für Server mit vielen gleichzeitigen Spielern, siehe [Changelog](#changelog).

### Annehmen / Ablehnen (`/tpaccept`, `/tpdeny`)

Nimmt die einzige offene eingehende Anfrage an oder ab. Gibt es mehrere
gleichzeitig, muss der Absendername als Argument angegeben werden
(Tab-Completion schlägt die Namen der Absender vor). Beim Annehmen wird je
nach Anfrage-Typ entweder der Absender zum Akzeptierenden teleportiert (bei
`/tpa`) oder umgekehrt (bei `/tpahere`).

### Anfrage stornieren (`/tpacancel`)

Bricht die eigene, noch offene ausgehende Anfrage ab und informiert das
Ziel darüber.

### Anfragen an/aus schalten (`/tptoggle`)

Persistenter Schalter pro Spieler (gespeichert in `playerdata.yml`): Ist er
deaktiviert, lehnt das Plugin eingehende Anfragen automatisch mit einer
Hinweismeldung ab — außer der Absender besitzt die Permission
`stonetpa.bypass.toggle`.

### Teleport-Countdown mit Verzögerung

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

### Benachrichtigungen während Countdown & Ankunft

Frei konfigurierbar als Chat, Actionbar, Bossbar oder Title/Subtitle
(`teleport.countdown.notification`, Standard: Actionbar;
`teleport.arrival.notification`, Standard: Title). Zeigt die verbleibenden
Sekunden bzw. eine Ankunftsmeldung mit dem Namen der Gegenpartei an.

### Fallschaden-Schutz

Nach einem Teleport ist der Spieler für 5 Sekunden (100 Ticks) immun gegen
Fallschaden (`teleport.disable-fall-damage`, Standard: an) — verhindert
Schaden durch eine ungünstige Landung.

### Ankunfts-Sound-Effekt (optional)

Standardmäßig deaktiviert (`teleport.arrival.enabled: false`). Wenn
aktiviert, spielt am Zielort ein zusätzlicher Sound ab (Standard:
Totem-Sound).

### Settings-Menü (`/tpasettings`, Aliase `/tpset`, `/tpsettings`)

GUI mit zwei Umschaltern pro Spieler: eingehende Anfragen an/aus (identisch
zu `/tptoggle`) und Teleport-Partikeleffekte an/aus. Einstellungen werden
dauerhaft in `playerdata.yml` gespeichert (siehe [Building](#building) für
Details zur Schreib-Robustheit dieser Datei).

### Bedrock-/Floodgate-Erkennung

Erkennt Floodgate per Reflection (keine harte Abhängigkeit nötig) und
schickt Bedrock-Spielern beim Erhalt einer Anfrage einen alternativen
Hinweistext, der auf Befehle statt klickbare Chat-Komponenten setzt, da
Bedrock-Clients diese oft nicht unterstützen.

### Update-Checker

Fragt periodisch (Standard: alle 60 Minuten,
`update-checker.check-interval-minutes`) die Modrinth-API nach neuen
Versionen des Projekts „stone-tpa" ab. Findet er eine neuere Version,
informiert er beim nächsten Login jeden Spieler mit OP-Status oder
`stonetpa.admin` (inklusive Anzahl Versionen Rückstand) und protokolliert es
in der Konsole. Lässt sich per `/stonetpa checkupdate` manuell auslösen und
über `update-checker.enabled: false` komplett abschalten.

> Versionsstrings von Modrinth werden vor der Anzeige escaped, damit ein
> präparierter Versionsstring keine anklickbaren Chat-Komponenten
> einschleusen kann (siehe [Changelog](#changelog)).

### Mehrsprachigkeit

Sprachdateien für Deutsch und Englisch (`languages/de|en/messages.yml`),
umschaltbar über den Schlüssel `language` in `config.yml`. Fehlt ein
Text-Schlüssel in der aktiven Sprache komplett, wird automatisch auf
Englisch zurückgefallen — eine bewusst leer gelassene Zeile/Liste bleibt
dabei aber leer und wird nicht ersetzt.

### Automatische Config-Migration

Beim Start bzw. Reload gleicht das Plugin die vorhandene `config.yml` (und
die `messages.yml`-Dateien) gegen die intern gebündelte Standardvorlage ab
und ergänzt fehlende neue Optionen automatisch, ohne bestehende Werte zu
überschreiben — wichtig bei zukünftigen Plugin-Updates.

### Verwaltungsbefehl `/stonetpa` (Alias `/stpa`)

- `reload` — lädt `config.yml` und Sprachdateien neu (nur `stonetpa.admin`)
- `checkupdate` — löst die Update-Prüfung sofort aus (nur `stonetpa.admin`)
- `help` / kein Argument — zeigt Hilfetext, gefiltert nach den Rechten des
  Aufrufers

Vollständige Befehlssyntax in [Commands](#commands), alle Permission-Nodes
in [Permissions](#permissions).

## Commands

Alle Befehle unterhalb von `stonetpa` benötigen die Permission
`stonetpa.use` (Standard: jeder Spieler hat sie). Details zu jedem
Permission-Node stehen in [Permissions](#permissions).

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

### `/stonetpa`-Unterbefehle

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
steht in [Changelog](#bugfixes).

### Tab-Completion

- `/tpa`, `/tpahere`: schlägt Namen aller online Spieler vor (außer dir
  selbst)
- `/tpaccept`, `/tpdeny`: schlägt nur die Namen der Spieler vor, die dir
  gerade tatsächlich eine Anfrage geschickt haben
- `/stonetpa`: schlägt `reload`, `help`, `checkupdate` vor

## Permissions

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

### Hinweis zur Durchsetzung

Die Permissions werden bewusst **im Code** geprüft (z. B.
`player.hasPermission("stonetpa.use")` in jedem Command-Handler), nicht
über das `permission:`-Feld in `plugin.yml`. Der Grund: Bukkit würde einen
Befehl sonst schon vor dem eigenen `onCommand()` blockieren, und mit einer
leeren `permission-message` dabei komplett stillschweigend ohne jede
Rückmeldung — das war ursprünglich ein Bug in diesem Plugin, siehe
[Changelog](#bugfixes). Für Serveradmins ändert sich dadurch nichts an der
Konfiguration selbst: Alle fünf Nodes lassen sich ganz normal über
LuckPerms & Co. vergeben.

## Configuration

StoneTPA hat zwei Arten von Konfigurationsdateien:

- **`config.yml`** — Einstellungen (Schalter, Zeiten, Farben, Sounds) sowie
  der Text für Countdown- und Ankunftsmeldung. Diese beiden Texte leben
  bewusst hier und nicht in `messages.yml`, damit das gesamte
  "wie sieht/klingt ein Teleport an" an einer einzigen Stelle konfiguriert
  ist.
- **`languages/<sprache>/messages.yml`** — alles andere an Text (Anfragen,
  Hilfe, Fehlermeldungen, Settings-Menü, ...), pro Sprache getrennt.

Beide Dateien werden bei jedem Start/`/stonetpa reload` automatisch gegen
die im Plugin gebündelte Vorlage abgeglichen: **fehlende neue Schlüssel
werden ergänzt, vorhandene eigene Werte werden nie überschrieben** — auch
nicht in verschachtelten Abschnitten.

### `language`

```yaml
language: en
```

Sprache für die allgemeine Befehls-Rückmeldung. Muss zu einem Ordner unter
`languages/` passen — `en` und `de` sind im Plugin enthalten. Neuinstallationen
starten immer auf Englisch, bis ein Admin das hier ändert.

### `request` — die Anfrage selbst

```yaml
request:
  expire-seconds: 60    # wie lange eine gesendete Anfrage gültig bleibt
  cooldown-seconds: 10  # Cooldown zwischen zwei gesendeten Anfragen
```

### `teleport` — der verzögerte Sprung nach Annahme

```yaml
teleport:
  delay-seconds: 5              # 0 = sofortiger Teleport ohne Countdown
  cancel-on-move: true          # bricht auch bei Fremd-Teleports ab (Enderperle, /warp, ...)
  blindness-during-delay: false
  disable-fall-damage: true
```

Der Standardwert `delay-seconds: 5` ist bewusst gewählt: Die
Countdown-Spirale unten ist so gebaut, dass ihre fünf sichtbaren
"Wachstumsstufen" exakt auf ganze Sekunden fallen. Andere Werte
funktionieren trotzdem, nur eben nicht mehr ganz so exakt getaktet.

#### `teleport.countdown` — die Spirale während des Wartens

```yaml
teleport:
  countdown:
    notification: ACTIONBAR   # CHAT, ACTIONBAR, BOSSBAR oder TITLE
    messages:
      text: "..."       # gilt für CHAT und ACTIONBAR
      bossbar: "..."
      title: "..."
      subtitle: "..."
    spiral:
      color-bottom: "#00B4D8"
      color-top: "#7B2CBF"
      size: 1.1
      radius: 0.5
      height: 2.2
      cycle-ticks: 30
      rotation-speed-degrees: 45.0
      sound: BLOCK_AMETHYST_BLOCK_CHIME
      pitch-start: 0.8
      pitch-end: 1.6
      sound-volume: 0.6
      tick-sound: BLOCK_NOTE_BLOCK_PLING
      tick-volume: 0.5
    departure:
      sound: ITEM_TOTEM_USE
      particle: TOTEM_OF_UNDYING
      count: 30
```

`{seconds}` und `{player}` sind in den Texten als Platzhalter nutzbar. Nur
der unter `notification` gewählte Kanal wird tatsächlich angezeigt — die
Texte für die anderen drei Kanäle bleiben einfach ungenutzt vorbereitet, so
kann man später umschalten, ohne Text neu schreiben zu müssen.

#### `teleport.arrival` — optionale Ankunftsmeldung

```yaml
teleport:
  arrival:
    enabled: false   # standardmäßig aus - der "Höhepunkt" liegt beim Abflug, nicht bei der Ankunft
    notification: TITLE
    messages: { ... }
    sound: ITEM_TOTEM_USE
    sound-volume: 0.6
```

#### `teleport.bossbar` / `teleport.title` — geteilte Optik

```yaml
teleport:
  bossbar:
    color: PURPLE               # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
                                 # (Aliase GOLD/ORANGE→YELLOW, MAGENTA→PURPLE, CYAN→BLUE, GRAY/GREY→WHITE)
    style: PROGRESS              # PROGRESS, NOTCHED_6/10/12/20
                                 # (Aliase SOLID/BAR/FULL/NONE→PROGRESS, SEGMENTED/SEGMENTS→NOTCHED_10)
    duration-seconds: 5
  title:
    fade-in-ticks: 5
    stay-ticks: 30
    fade-out-ticks: 5
```

Gilt jeweils, wann immer `notification: BOSSBAR` bzw. `notification: TITLE`
irgendwo oben ausgewählt ist (Countdown und/oder Ankunft).

### `send-gui` — Bestätigungsmenü beim Senden

```yaml
send-gui:
  enabled: true
  size: 27              # Vielfaches von 9, 9-54
  filler: { enabled: true, material: BLACK_STAINED_GLASS_PANE, accent-material: PURPLE_STAINED_GLASS_PANE }
  target-head: { slot: 13 }
  dimension-info: { slot: 14, material: COMPASS }
  ping-info: { slot: 12, material: CLOCK }
  send: { slot: 16, material: LIME_CONCRETE }
  cancel: { slot: 10, material: RED_CONCRETE }
  click-sound: { enabled: true, sound: UI_BUTTON_CLICK, volume: 1.0, pitch: 1.0 }
```

Slots sind links nach rechts, oben nach unten nummeriert, beginnend bei 0.
`enabled: false` überspringt das Menü komplett — die Anfrage wird dann
direkt verschickt.

### `gui` — Spieler-Settings-Menü (`/tpasettings`)

```yaml
gui:
  size: 27
  filler: { enabled: true, material: BLACK_STAINED_GLASS_PANE, accent-material: PURPLE_STAINED_GLASS_PANE }
  player-head: { enabled: true, slot: 4 }
  requests-toggle: { slot: 11, material-on: LIME_DYE, material-off: GRAY_DYE }
  effects-toggle: { slot: 15, material-on: LIME_DYE, material-off: GRAY_DYE }
  close-button: { enabled: true, slot: 22, material: BARRIER }
  click-sound: { enabled: true, sound: UI_BUTTON_CLICK, volume: 1.0, pitch: 1.0 }
```

### `update-checker`

```yaml
update-checker:
  enabled: true
  check-interval-minutes: 60
```

Fragt `https://modrinth.com/project/stone-tpa` ab. `enabled: false`
schaltet die komplette Prüfung ab (auch den einmaligen Check beim Start).

### Materialien, Sounds, Farben, Partikel

Alle `material:`/`sound:`/`particle:`-Werte sind die offiziellen Bukkit/Paper
Enum-Namen (z. B. `LIME_CONCRETE`, `BLOCK_NOTE_BLOCK_PLING`,
`TOTEM_OF_UNDYING`). Ein ungültiger Wert wird beim Laden erkannt, mit einer
Warnung in der Konsole protokolliert und durch einen sinnvollen
Standardwert ersetzt — der Server startet also nie wegen eines Tippfehlers
in `config.yml`.

## Building

### Voraussetzungen

- **JDK 25** (`java -version` sollte `25.x` zeigen)
- **Maven 3.9+**
- Netzwerkzugriff auf:
  - `repo.papermc.io` (für `paper-api` selbst und seine Pflicht-Transitiven:
    `brigadier`, `bungeecord-chat`)
  - `repo.maven.apache.org` / Maven Central (für `net.kyori:adventure-*`,
    die über `paper-api`s eigene `adventure-bom`-Importe aufgelöst werden,
    sowie für die Test-Abhängigkeiten)

### Bauen

```bash
mvn clean package
```

Ergebnis: `target/StoneTPA-1.0.0.jar`, fertig zum Kopieren in den
`plugins/`-Ordner eines Paper-26.2-Servers.

`mvn clean package` führt dabei automatisch auch die komplette Testsuite
aus (siehe [Tests](#tests) unten) — schlägt ein Test fehl, bricht der Build
ab, bevor eine JAR entsteht.

### Kern-Abhängigkeit

```xml
<dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>26.2.build.124-stable</version>
    <scope>provided</scope>
</dependency>
```

Bewusst eine **fest gepinnte stabile Build-Nummer**, kein Versionsbereich
(`[26.2.build,)`) — ein offener Bereich zwingt Maven, bei *jedem* Build
`maven-metadata.xml` von `repo.papermc.io` neu abzufragen, um die aktuell
neueste Version zu ermitteln; das schlägt fehl, sobald dieser Host gerade
nicht erreichbar ist (z. B. in einer Sandbox oder hinter einer
Firewall/einem Proxy ohne Zugriff darauf), obwohl die exakte Version längst
lokal im `~/.m2`-Cache läge. Ein fest gepinnter Build hat dieses Problem
nicht.

### Tests

```bash
mvn test
```

JUnit 5 + Mockito (beide nur `test`-Scope, landen nicht in der finalen
JAR). Es kommt **kein** [MockBukkit](https://github.com/MockBukkit/MockBukkit)
zum Einsatz — der Grund steht direkt als Kommentar in `pom.xml`: die
verfügbaren MockBukkit-Artefakte hängen transitiv an einem Paper-**SNAPSHOT**-
Build (nur auf `repo.papermc.io` veröffentlicht, nie auf Maven Central
gespiegelt), was in der Sandbox, in der dieses Projekt zuletzt bearbeitet
wurde, nicht erreichbar war. Player-/Bukkit-Interaktionen werden stattdessen
mit reinem Mockito nachgebaut (`mockStatic(Bukkit.class)`,
`mock(Player.class)`, ...). Wer lieber mit echtem MockBukkit testen möchte
und Zugriff auf `repo.papermc.io` hat, kann `MockBukkit-v1.21` als weitere
Test-Abhängigkeit ergänzen — die bestehenden Tests bleiben davon unberührt.

Getestet wird gezielt die fehleranfälligste Logik, nicht 100 % Coverage:

- `RequestManagerTest` — die komplette Anfragen-Zustandsmaschine
  (Selbst-Ziel, Toggle + Bypass, Cooldown + Bypass, "schon eine Anfrage
  offen"-Sperre, beide Annahme-Richtungen, mehrere gleichzeitige Anfragen,
  Ablauf, Aufräumen beim Verlassen des Servers)
- `ConfigUpdaterTest` — die rekursive Merge-Logik der Config-Migration
- `MessageManagerFormatTest` / `MessageManagerFallbackTest` — Legacy-Farbcode-
  Konvertierung, Escaping gegen MiniMessage-Injection, Sprachfallback-Logik
- `CooldownManagerTest` — Cooldown-Ablauf und Aufräum-Verhalten

### Hinweis zu `repo.papermc.io`

Falls `mvn clean package` mit einem Netzwerkfehler beim Auflösen von
`paper-api`, `brigadier` oder `bungeecord-chat` abbricht: Diese drei
Artefakte liegen ausschließlich auf `repo.papermc.io` (nicht auf Maven
Central) — der Host muss für einen normalen Build erreichbar sein. In einer
Umgebung, in der dieser Host blockiert ist, müssen die passenden `.jar`/
`.pom`-Dateien einmalig manuell besorgt und per
`mvn install:install-file -Dfile=... -DpomFile=...` in den lokalen
`~/.m2`-Cache installiert werden, bevor `mvn clean package` offline
funktioniert. Das ist der Grund, warum genau diese drei Artefakte (plus
VaultAPI/PlaceholderAPI, die vom Plugin aktuell aber gar nicht genutzt
werden) in der Versionsgeschichte dieses Projekts einmal manuell installiert
wurden — für einen Rechner mit normalem Internetzugang ist das nicht nötig.

## Changelog

Diese Seite fasst die zwei großen Arbeitsschritte zusammen, die dieses
Repo bisher durchlaufen hat: die Portierung auf eine neue Zielversion und
die anschließende systematische Bug-Hunt- und Performance-Runde.

### Portierung auf Paper API 26.2 / Java 25

Ausgangslage war Paper `1.21.4-R0.1-SNAPSHOT` / Java 21 / `api-version: '1.21'`.
Geändert wurden:

- **`pom.xml`**: `paper-api`-Version auf die gepinnte stabile Build
  `26.2.build.124-stable` angehoben (statt eines Versionsbereichs — siehe
  [Kern-Abhängigkeit](#kern-abhängigkeit) für den Grund); `maven.compiler.source/target`
  und die Compiler-Plugin-`release` auf `25`.
- **`plugin.yml`**: `api-version` auf `'26.2'`.

**Keine Compile-Fixes waren nötig** — der komplette Code kompilierte
unverändert gegen die neue Paper-API. Die Codebasis nutzt ausschließlich
`org.bukkit.*`, `net.kyori.adventure.*` und `com.google.gson.*`, alles APIs,
die zwischen den beiden Versionen stabil geblieben sind.

### Bugfixes

Im Anschluss an die Portierung wurde das Plugin systematisch auf Bugs
geprüft und für ~300 gleichzeitige Spieler optimiert. Gefunden und behoben:

| Schweregrad | Bug | Fix |
|---|---|---|
| **Kritisch** | `plugin.yml` gatete `/tpa` & Co. zusätzlich über `permission:`/`permission-message:""` — Bukkit blockierte damit **vor** dem eigenen `onCommand()`, und zwar völlig lautlos (leere `permission-message` unterdrückt auch Bukkits Standardmeldung). Der bereits vorhandene, sauber lokalisierte In-Code-Check war dadurch toter Code. | `permission:`/`permission-message:` aus `plugin.yml` entfernt; der In-Code-Check ist jetzt die einzige Instanz (gleiches Muster wie schon vorher bei `/stonetpa`). |
| **Hoch** | `playerdata.yml`-Schreibvorgänge liefen über den geteilten Bukkit-Async-Pool ohne Reihenfolge-Garantie und schrieben direkt in die Live-Datei — zwei schnelle Settings-Änderungen konnten in falscher Reihenfolge landen und eine neuere Änderung zurückrollen; ein Absturz mitten im Schreiben konnte die Datei für **alle** Spieler korrumpieren. | Dedizierter Single-Thread-Writer (garantierte Reihenfolge) + Schreiben über Temp-Datei mit atomarem Rename; sauberes Draining beim Disable. |
| **Mittel-Hoch** | Platzhalter-Werte wurden roh in MiniMessage-Text gespleißt. Der Update-Checker zeigt jedem OP einen von der Modrinth-API geholten Versionsstring an — ein präparierter String hätte klickbare/hover-fähige Chat-Komponenten einschleusen können. | `<` in Platzhalter-**Werten** (nicht im Template) wird vor dem Parsen escaped. |
| **Mittel** | Boss-Bars und offene Plugin-GUIs werden von Bukkits automatischem Scheduler-/Listener-Cleanup beim Disable **nicht** erfasst — ein roher `/reload` mitten im Countdown konnte eine für immer eingefrorene Boss-Bar hinterlassen bzw. ein offenes GUI ohne Klick-Schutz. | `onDisable()` versteckt aktive Boss-Bars und schließt offene Send-/Settings-GUIs. |
| **Niedrig-Mittel** | `cancel-on-move` reagierte nur auf `PlayerMoveEvent` — ein Teleport durch etwas anderes (Enderperle, `/warp`, ein anderes Plugin) während des Countdowns wurde nicht erkannt. | Zusätzlicher `PlayerTeleportEvent`-Handler, sicher gegen Selbst-Abbruch. |
| **Niedrig** | `getRawList` behandelte eine absichtlich leer konfigurierte Liste wie eine fehlende und ersetzte sie durch die englische Fallback-Liste. | Prüfung auf „ist der Schlüssel gesetzt" statt „ist die Liste leer". |
| **Niedrig** | `CooldownManager`-Einträge einmaliger Besucher blieben für immer im Speicher. | Eintrag wird bei `PlayerQuitEvent` sofort entfernt. |

### Performance (300 gleichzeitige Spieler)

- **Send-GUI:** ein gemeinsamer Scheduler-Task statt eines Tasks pro
  offenem Bestätigungsmenü — reduziert bis zu ~300 Scheduler-Einträge auf
  höchstens 1.
- Der Persistenz-Fix (dedizierter Writer-Thread) wirkt zusätzlich als
  Entlastung des geteilten Async-Pools bei vielen gleichzeitigen
  Settings-Änderungen.

### Was schon vorher gut war

- Hochfrequenz-Event-Handler (`PlayerMoveEvent`, `EntityDamageEvent`)
  brechen ganz am Anfang ab, bevor teure Arbeit passiert.
- Partikel/Sounds/Titel/Boss-Bars laufen durchgängig über die
  Player-scoped APIs, nicht über Welt-weites Broadcasting.
- Sauberes Aufräumen pro Spieler beim `PlayerQuitEvent` war für die
  Anfragen- und GUI-Verwaltung bereits vorher korrekt umgesetzt.
- Teleports laufen über `teleportAsync` (kein blockierender Chunk-Load).

### Testabdeckung

40 neue Unit-Tests (JUnit 5 + Mockito) decken die Anfragen-Zustandsmaschine,
die Config-Migration und die Message-Formatierung/-Injection-Absicherung
ab. Details in [Tests](#tests).
