# Configuration

[← zurück zur Übersicht](Home.md)

StoneTPA hat zwei Arten von Konfigurationsdateien:

- **`config.yml`** — Einstellungen (Schalter, Zeiten, Farben, Sounds) sowie
  der Text für Countdown- und Ankunftsmeldung. Diese beiden Texte leben
  bewusst hier und nicht in `messages.yml`, damit das gesamte
  "wie sieht/klingt ein Teleport an" an einer einzigen Stelle konfiguriert
  ist.
- **`languages/<sprache>/messages.yml`** — alles andere an Text (Anfragen,
  Hilfe, Fehlermeldungen, Settings-Menü, ...), pro Sprache getrennt.

Beide Dateien werden bei jedem Start/`\/stonetpa reload` automatisch gegen
die im Plugin gebündelte Vorlage abgeglichen: **fehlende neue Schlüssel
werden ergänzt, vorhandene eigene Werte werden nie überschrieben** — auch
nicht in verschachtelten Abschnitten.

## `language`

```yaml
language: en
```

Sprache für die allgemeine Befehls-Rückmeldung. Muss zu einem Ordner unter
`languages/` passen — `en` und `de` sind im Plugin enthalten. Neuinstallationen
starten immer auf Englisch, bis ein Admin das hier ändert.

## `request` — die Anfrage selbst

```yaml
request:
  expire-seconds: 60    # wie lange eine gesendete Anfrage gültig bleibt
  cooldown-seconds: 10  # Cooldown zwischen zwei gesendeten Anfragen
```

## `teleport` — der verzögerte Sprung nach Annahme

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

### `teleport.countdown` — die Spirale während des Wartens

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

### `teleport.arrival` — optionale Ankunftsmeldung

```yaml
teleport:
  arrival:
    enabled: false   # standardmäßig aus - der "Höhepunkt" liegt beim Abflug, nicht bei der Ankunft
    notification: TITLE
    messages: { ... }
    sound: ITEM_TOTEM_USE
    sound-volume: 0.6
```

### `teleport.bossbar` / `teleport.title` — geteilte Optik

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

## `send-gui` — Bestätigungsmenü beim Senden

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

## `gui` — Spieler-Settings-Menü (`/tpasettings`)

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

## `update-checker`

```yaml
update-checker:
  enabled: true
  check-interval-minutes: 60
```

Fragt `https://modrinth.com/project/stone-tpa` ab. `enabled: false`
schaltet die komplette Prüfung ab (auch den einmaligen Check beim Start).

## Materialien, Sounds, Farben, Partikel

Alle `material:`/`sound:`/`particle:`-Werte sind die offiziellen Bukkit/Paper
Enum-Namen (z. B. `LIME_CONCRETE`, `BLOCK_NOTE_BLOCK_PLING`,
`TOTEM_OF_UNDYING`). Ein ungültiger Wert wird beim Laden erkannt, mit einer
Warnung in der Konsole protokolliert und durch einen sinnvollen
Standardwert ersetzt — der Server startet also nie wegen eines Tippfehlers
in `config.yml`.
