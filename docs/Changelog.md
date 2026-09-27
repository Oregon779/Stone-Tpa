# Changelog

[← zurück zur Übersicht](Home.md)

Diese Seite fasst die zwei großen Arbeitsschritte zusammen, die dieses
Repo bisher durchlaufen hat: die Portierung auf eine neue Zielversion und
die anschließende systematische Bug-Hunt- und Performance-Runde.

## Portierung auf Paper API 26.2 / Java 25

Ausgangslage war Paper `1.21.4-R0.1-SNAPSHOT` / Java 21 / `api-version: '1.21'`.
Geändert wurden:

- **`pom.xml`**: `paper-api`-Version auf die gepinnte stabile Build
  `26.2.build.124-stable` angehoben (statt eines Versionsbereichs — siehe
  [Building](Building.md#kern-abhängigkeit) für den Grund); `maven.compiler.source/target`
  und die Compiler-Plugin-`release` auf `25`.
- **`plugin.yml`**: `api-version` auf `'26.2'`.

**Keine Compile-Fixes waren nötig** — der komplette Code kompilierte
unverändert gegen die neue Paper-API. Die Codebasis nutzt ausschließlich
`org.bukkit.*`, `net.kyori.adventure.*` und `com.google.gson.*`, alles APIs,
die zwischen den beiden Versionen stabil geblieben sind.

## Bugfixes

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

## Performance (300 gleichzeitige Spieler)

- **Send-GUI:** ein gemeinsamer Scheduler-Task statt eines Tasks pro
  offenem Bestätigungsmenü — reduziert bis zu ~300 Scheduler-Einträge auf
  höchstens 1.
- Der Persistenz-Fix (dedizierter Writer-Thread) wirkt zusätzlich als
  Entlastung des geteilten Async-Pools bei vielen gleichzeitigen
  Settings-Änderungen.

## Was schon vorher gut war

- Hochfrequenz-Event-Handler (`PlayerMoveEvent`, `EntityDamageEvent`)
  brechen ganz am Anfang ab, bevor teure Arbeit passiert.
- Partikel/Sounds/Titel/Boss-Bars laufen durchgängig über die
  Player-scoped APIs, nicht über Welt-weites Broadcasting.
- Sauberes Aufräumen pro Spieler beim `PlayerQuitEvent` war für die
  Anfragen- und GUI-Verwaltung bereits vorher korrekt umgesetzt.
- Teleports laufen über `teleportAsync` (kein blockierender Chunk-Load).

## Tests

40 neue Unit-Tests (JUnit 5 + Mockito) decken die Anfragen-Zustandsmaschine,
die Config-Migration und die Message-Formatierung/-Injection-Absicherung
ab. Details in [Building](Building.md#tests).
