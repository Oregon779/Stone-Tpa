# Building

[← zurück zur Übersicht](Home.md)

## Voraussetzungen

- **JDK 25** (`java -version` sollte `25.x` zeigen)
- **Maven 3.9+**
- Netzwerkzugriff auf:
  - `repo.papermc.io` (für `paper-api` selbst und seine Pflicht-Transitiven:
    `brigadier`, `bungeecord-chat`)
  - `repo.maven.apache.org` / Maven Central (für `net.kyori:adventure-*`,
    die über `paper-api`s eigene `adventure-bom`-Importe aufgelöst werden,
    sowie für die Test-Abhängigkeiten)

## Bauen

```bash
mvn clean package
```

Ergebnis: `target/StoneTPA-1.0.0.jar`, fertig zum Kopieren in den
`plugins/`-Ordner eines Paper-26.2-Servers.

`mvn clean package` führt dabei automatisch auch die komplette Testsuite
aus (siehe [Tests](#tests) unten) — schlägt ein Test fehl, bricht der Build
ab, bevor eine JAR entsteht.

## Kern-Abhängigkeit

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

## Tests

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

## Hinweis zu `repo.papermc.io`

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
