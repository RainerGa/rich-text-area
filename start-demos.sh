#!/usr/bin/env bash
#
# start-demos.sh – Startet die RichTextArea Demo-Anwendungen.
#
# Verwendung:
#   ./start-demos.sh              – Liste aller Demos anzeigen
#   ./start-demos.sh full         – FullFeaturedDemo starten (Standard)
#   ./start-demos.sh actions      – ActionsDemo starten
#   ./start-demos.sh basic        – BasicPromptDemo starten
#   ./start-demos.sh basic-doc    – BasicDocumentDemo starten
#   ./start-demos.sh highlight    – HighlightDemo starten
#   ./start-demos.sh lists        – ListsDemo starten
#   ./start-demos.sh emoji        – EmojiDemo starten
#   ./start-demos.sh emoji-popup  – EmojiPopupDemo starten
#   ./start-demos.sh table        – TableDemo starten
#   ./start-demos.sh page-layout  – PageLayoutDemo starten (NEU: Seitenlayout, Felder, Stile)
#   ./start-demos.sh all          – Alle Demos nacheinander starten
#
# Voraussetzungen:
#   - Java 17+ (empfohlen: Java 21+)
#   - Maven 3.9+
#   - Internetverbindung für erste Ausführung (Abhängigkeiten laden)
#
# Hinweis: Beim ersten Start werden die Maven-Abhängigkeiten heruntergeladen,
#          was einige Minuten dauern kann.

set -euo pipefail

# Projektwurzel dieses Skripts
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== RichTextArea Demos ==="
echo

# ---------- Java prüfen ----------
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    PATH="$JAVA_HOME/bin:$PATH"
    export PATH
fi

if ! command -v java >/dev/null 2>&1; then
    echo "FEHLER: Keine Java-Laufzeit gefunden." >&2
    echo "        Benötigt wird Java 17 oder neuer (empfohlen: Java 21+)." >&2
    echo "        Bei Bedarf JAVA_HOME setzen, z. B.:" >&2
    echo "        JAVA_HOME=/pfad/zu/jdk-21 ./start-demos.sh" >&2
    exit 1
fi

JAVA_VERSION="$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')"
if [ -n "$JAVA_VERSION" ] && [ "$JAVA_VERSION" -lt 17 ]; then
    echo "FEHLER: Java $JAVA_VERSION gefunden – benötigt wird Java 17 oder neuer." >&2
    exit 1
fi
echo "Java: $(java -version 2>&1 | head -1)"
echo

# ---------- Maven prüfen ----------
MVN_BIN="${MVN:-mvn}"
export MAVEN_OPTS="-Dmaven.repo.local=/tmp/kilo/m2repo"
if ! command -v "$MVN_BIN" >/dev/null 2>&1; then
    echo "FEHLER: Maven nicht gefunden (gesucht: '$MVN_BIN')." >&2
    echo "        Maven 3.9+ installieren oder den Pfad über MVN angeben:" >&2
    echo "        MVN=/pfad/zu/mvn ./start-demos.sh" >&2
    exit 1
fi
echo "Maven: $MVN_BIN"
echo

# ---------- Demo-Informationen ----------
DEMO_FULL="FullFeaturedDemo – Kompletter Rich-Text-Editor mit Toolbars und Menüs"
DEMO_ACTIONS="ActionsDemo – Textformatierung via Toggle-Buttons (Fett, Kursiv, Unterstrichen)"
DEMO_BASIC="BasicPromptDemo – Einfache Texteingabe"
DEMO_BASIC_DOC="BasicDocumentDemo – Vordekorierter Text"
DEMO_HIGHLIGHT="HighlightDemo – Suchen und Markieren von Schlüsselwörtern"
DEMO_LISTS="ListsDemo – Aufzählungen und nummerierte Listen"
DEMO_EMOJI="EmojiDemo – Emoji-Unterstützung"
DEMO_EMOJI_POPUP="EmojiPopupDemo – Emoji-Auswahl per Popup"
DEMO_TABLE="TableDemo – Tabellenfunktionen"
DEMO_PAGE_LAYOUT="PageLayoutDemo – Seitenlayout, Kopfzeilen, Felder, Stile (NEU)"

# ---------- Hauptprogramm ----------
case "${1:-}" in
    "")
        echo "Verfügbare Demos:"
        echo
        echo "  full         – $DEMO_FULL"
        echo "  actions      – $DEMO_ACTIONS"
        echo "  basic        – $DEMO_BASIC"
        echo "  basic-doc    – $DEMO_BASIC_DOC"
        echo "  highlight    – $DEMO_HIGHLIGHT"
        echo "  lists        – $DEMO_LISTS"
        echo "  emoji        – $DEMO_EMOJI"
        echo "  emoji-popup  – $DEMO_EMOJI_POPUP"
        echo "  table        – $DEMO_TABLE"
        echo "  page-layout  – $DEMO_PAGE_LAYOUT"
        echo "  all          – Alle Demos nacheinander"
        echo
        echo "Verwendung:"
        echo "  ./start-demos.sh [demo-name]"
        echo
        echo "Beispiel:"
        echo "  ./start-demos.sh full"
        echo
        echo "Standarddemo (ohne Argument): FullFeaturedDemo"
        exit 0
        ;;
    "full")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.FullFeaturedDemo"
        DEMO_NAME="FullFeaturedDemo"
        ;;
    "actions")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.ActionsDemo"
        DEMO_NAME="ActionsDemo"
        ;;
    "basic")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.BasicPromptDemo"
        DEMO_NAME="BasicPromptDemo"
        ;;
    "basic-doc")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.BasicDocumentDemo"
        DEMO_NAME="BasicDocumentDemo"
        ;;
    "highlight")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.HighlightDemo"
        DEMO_NAME="HighlightDemo"
        ;;
    "lists")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.ListsDemo"
        DEMO_NAME="ListsDemo"
        ;;
    "emoji")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.EmojiDemo"
        DEMO_NAME="EmojiDemo"
        ;;
    "emoji-popup")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.EmojiPopupDemo"
        DEMO_NAME="EmojiPopupDemo"
        ;;
    "table")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.TableDemo"
        DEMO_NAME="TableDemo"
        ;;
    "page-layout")
        DEMO_CLASS="com.gluonhq.richtextarea.samples.PageLayoutDemo"
        DEMO_NAME="PageLayoutDemo"
        ;;
    "all")
        echo "Starte alle Demos nacheinander..."
        echo "(Drücken Sie Strg+C zum Beenden)"
        echo

        # Kompilieren aller Samples
        "$MVN_BIN" -f "$SCRIPT_DIR/pom.xml" -pl samples -am compile -q

        # JavaFX Module Path für Java 27+ (nutzt die modularen -linux.jar Varianten)
        JFX_MP="$HOME/.m2/repository/org/openjfx/javafx-base/25/javafx-base-25-linux.jar"
        JFX_MP="$JFX_MP:$HOME/.m2/repository/org/openjfx/javafx-controls/25/javafx-controls-25-linux.jar"
        JFX_MP="$JFX_MP:$HOME/.m2/repository/org/openjfx/javafx-graphics/25/javafx-graphics-25-linux.jar"
        JFX_MP="$JFX_MP:$HOME/.m2/repository/org/openjfx/javafx-fxml/25/javafx-fxml-25-linux.jar"

        # Klassenpfad (ohne JavaFX, das über Module Path geladen wird)
        CP="$SCRIPT_DIR/samples/target/classes"
        CP="$CP:$SCRIPT_DIR/rta/target/classes"
        CP="$CP:$HOME/.m2/repository/com/gluonhq/emoji/emoji/1.1.0/emoji-1.1.0.jar"
        CP="$CP:$HOME/.m2/repository/com/gluonhq/emoji/offline/1.1.0/offline-1.1.0.jar"
        CP="$CP:$HOME/.m2/repository/org/kordamp/ikonli/ikonli-core/12.3.1/ikonli-core-12.3.1.jar"
        CP="$CP:$HOME/.m2/repository/org/kordamp/ikonli/ikonli-javafx/12.3.1/ikonli-javafx-12.3.1.jar"
        CP="$CP:$HOME/.m2/repository/org/kordamp/ikonli/ikonli-lineawesome-pack/12.3.1/ikonli-lineawesome-pack-12.3.1.jar"

        for demo in full actions basic basic-doc highlight lists emoji emoji-popup table page-layout; do
            DEMO_CLASS_NAME="com.gluonhq.richtextarea.samples.${demo^}"
            echo "=== Starte $demo ==="
            java --module-path "$JFX_MP" --add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.base \
                -cp "$CP" "$DEMO_CLASS_NAME"
            echo
            read -p "Weiter mit Enter, oder 'q' zum Beenden... " -r
            if [[ $REPLY == "q" ]]; then
                exit 0
            fi
        done
        exit 0
        ;;
    *)
        echo "FEHLER: Unbekannte Demo '$1'" >&2
        echo
        ./start-demos.sh
        exit 1
        ;;
esac

# Standard: FullFeaturedDemo
if [ -z "${DEMO_CLASS:-}" ]; then
    DEMO_CLASS="com.gluonhq.richtextarea.samples.FullFeaturedDemo"
    DEMO_NAME="FullFeaturedDemo"
fi

echo "Starte Demo: $DEMO_NAME"
echo "(Drücken Sie Strg+C zum Beenden)"
echo

# Demo starten
# JavaFX erfordert spezielle VM-Argumente für Module (Java 27+: --module-path / --add-modules)
# Kompilieren und dann direkt mit java ausführen

# Kompilieren
"$MVN_BIN" -f "$SCRIPT_DIR/pom.xml" -pl samples -am compile -q

# JavaFX Module Path für Java 27+ (nutzt die modularen -linux.jar Varianten)
JFX_MP="$HOME/.m2/repository/org/openjfx/javafx-base/25/javafx-base-25-linux.jar"
JFX_MP="$JFX_MP:$HOME/.m2/repository/org/openjfx/javafx-controls/25/javafx-controls-25-linux.jar"
JFX_MP="$JFX_MP:$HOME/.m2/repository/org/openjfx/javafx-graphics/25/javafx-graphics-25-linux.jar"
JFX_MP="$JFX_MP:$HOME/.m2/repository/org/openjfx/javafx-fxml/25/javafx-fxml-25-linux.jar"

# Klassenpfad (ohne JavaFX, das über Module Path geladen wird)
CP="$SCRIPT_DIR/samples/target/classes"
        CP="$CP:$SCRIPT_DIR/rta/target/classes"
        CP="$CP:$HOME/.m2/repository/com/gluonhq/emoji/emoji/1.1.0/emoji-1.1.0.jar"
CP="$CP:$HOME/.m2/repository/com/gluonhq/emoji/offline/1.1.0/offline-1.1.0.jar"
CP="$CP:$HOME/.m2/repository/org/kordamp/ikonli/ikonli-core/12.3.1/ikonli-core-12.3.1.jar"
CP="$CP:$HOME/.m2/repository/org/kordamp/ikonli/ikonli-javafx/12.3.1/ikonli-javafx-12.3.1.jar"
CP="$CP:$HOME/.m2/repository/org/kordamp/ikonli/ikonli-lineawesome-pack/12.3.1/ikonli-lineawesome-pack-12.3.1.jar"

# Starte mit java direkt (korrekte JavaFX-Modul-Auflösung für Java 27+)
java --module-path "$JFX_MP" --add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.base \
    -cp "$CP" "$DEMO_CLASS"
