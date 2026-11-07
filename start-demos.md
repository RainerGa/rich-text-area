# RichTextArea Demos starten

Dieses Skript vereinfacht das Starten der verschiedenen Demo-Anwendungen aus dem `samples`-Modul.

## 🚀 Schnellstart

### Alle Demos anzeigen
```bash
./start-demos.sh
```

### Standard-Demo (FullFeaturedDemo) starten
```bash
./start-demos.sh
```
oder explizit:
```bash
./start-demos.sh full
```

### Spezifische Demo starten
```bash
# ActionsDemo (Fett, Kursiv, Unterstrichen)
./start-demos.sh actions

# BasicPromptDemo (einfache Texteingabe)
./start-demos.sh basic

# BasicDocumentDemo (vordekorierter Text)
./start-demos.sh basic-doc

# HighlightDemo (Schlüsselwörter suchen und markieren)
./start-demos.sh highlight

# ListsDemo (Aufzählungen und nummerierte Listen)
./start-demos.sh lists

# EmojiDemo (Emoji-Unterstützung)
./start-demos.sh emoji

# EmojiPopupDemo (Emoji-Auswahl per Popup)
./start-demos.sh emoji-popup

# TableDemo (Tabellenfunktionen)
./start-demos.sh table

# PageLayoutDemo (NEU: Seitenlayout, Kopfzeilen, Felder, Stile)
./start-demos.sh page-layout
```

### Alle Demos nacheinander starten
```bash
./start-demos.sh all
```

## ⚙️ Voraussetzungen

- **Java 17+** (empfohlen: Java 21+)
- **Maven 3.9+**
- **Internetverbindung** (für erste Ausführung, um Abhängigkeiten zu laden)

## 💡 Tipps

### Java-Version festlegen
```bash
JAVA_HOME=/pfad/zu/jdk-21 ./start-demos.sh full
```

### Maven-Binary festlegen
```bash
MVN=/pfad/zu/mvn ./start-demos.sh full
```

### Leiser Modus (weniger Maven-Output)
```bash
./start-demos.sh full 2>/dev/null
```

## 📚 Demo-Beschreibungen

| Demo | Beschreibung |
|------|--------------|
| **FullFeaturedDemo** | Kompletter Rich-Text-Editor mit Toolbars, Menüs und **neuen Funktionen** (Seitenumbrüche, Page Format Dialog). Ideal, um alle Möglichkeiten zu erkunden. |
| **ActionsDemo** | Zeigt, wie Textformatierungen (Fett, Kursiv, Unterstrichen) via Toggle-Buttons angewendet werden. |
| **BasicPromptDemo** | Einfache Eingabe mit RichTextArea-Control. Perfekt für erste Tests. |
| **BasicDocumentDemo** | Erweitert BasicPromptDemo um vordekorierten Text. |
| **HighlightDemo** | Demonstriert das Suchen und Markieren von Schlüsselwörtern im Text. |
| **ListsDemo** | Zeigt Aufzählungen (Bullet Lists) und nummerierte Listen mit benutzerdefinierten Dekorationen. |
| **EmojiDemo** | Demonstriert die Unterstützung für Emoji-Zeichen in der RichTextArea. |
| **EmojiPopupDemo** | Wie EmojiDemo, aber mit interaktivem Popup zur Emoji-Auswahl. |
| **TableDemo** | Zeigt, wie Tabellen in der RichTextArea erstellt und bearbeitet werden. |
| **PageLayoutDemo** ⭐ **NEU** | **Demonstriert die neuen Seitenlayout-Funktionen:** Seitenformat (A4, Letter, Hoch-/Querformat, Ränder), Kopf- und Fußzeilen, dynamische Felder (Seitenzahlen, Datum, etc.), Stil-System mit Vererbung, manuelle Seitenumbrüche. |

## 🎯 Empfohlene Demo-Reihenfolge

1. **PageLayoutDemo** ⭐ **NEU** – Alle neuen Seitenlayout-Funktionen
2. **FullFeaturedDemo** – Vollständiger Überblick über alle Funktionen (inkl. neue Page Layout Features)
3. **ActionsDemo** – Grundlegende Textformatierung
4. **ListsDemo** – Listenfunktionen
5. **TableDemo** – Tabellenfunktionen
6. **EmojiPopupDemo** – Emoji-Unterstützung
7. **HighlightDemo** – Suchfunktionen

## 🆕 Neue Funktionen in den Demos

Ab dieser Version enthält die RichTextArea-Bibliothek **neue Seitenlayout-Funktionen**, die in den Demos gezeigt werden:

### PageLayoutDemo
Die **PageLayoutDemo** ist eine neue, umfassende Demo, die speziell die folgenden neuen Funktionen demonstriert:

| Funktion | Beschreibung |
|----------|--------------|
| **Seitenformat** | Seitengrößen (A0-A6, Letter, Legal, etc.), Hoch-/Querformat, Ränder |
| **Kopf- und Fußzeilen** | Header/Footer für alle Seiten, erste Seite, linke/rechte Seiten |
| **Dynamische Felder** | Seitenzahlen, Seitenanzahl, Datum, Uhrzeit, Dokumenttitel, Autor, Variablen, etc. |
| **Stil-System** | Vererbbare Text- und Absatzstile (ähnlich LibreOffice Writer) |
| **Manuelle Seitenumbrüche** | Seitenumbrüche vor/nach Absätzen einfügen |

### FullFeaturedDemo (erweitert)
Die FullFeaturedDemo wurde um folgende Funktionen erweitert:
- **Seitenumbruch-Button** – Manuelle Seitenumbrüche einfügen
- **Feld einfügen** – Dynamische Felder einfügen
- **Seitenumbruch vor/nach** – Toggle-Buttons für Absatz-Page-Breaks
- **Seitenformat-Konfiguration** – Page Size, Orientation, Margins

### Code-Beispiele

#### Seitenformat setzen
```java
PageFormat format = PageFormat.builder()
    .pageSize(PageFormat.PageSize.A4)
    .orientation(PageFormat.Orientation.PORTRAIT)
    .margins(new Insets(72, 72, 72, 72)) // 1 Zoll
    .build();
richTextArea.setPageFormat(format);
```

#### Header/Footer erstellen
```java
HeaderFooter header = HeaderFooter.builder()
    .type(HeaderFooter.Type.HEADER)
    .scope(HeaderFooter.Scope.ALL_PAGES)
    .content("Mein Dokument")
    .height(36) // 0.5 Zoll
    .distanceFromEdge(18) // 0.25 Zoll
    .build();
```

#### Dynamische Felder einfügen
```java
// Seitenzahl-Feld
editor.getActionFactory().insertField(new Fields.PageNumberField("Page #")).execute();

// Datum-Feld
editor.getActionFactory().insertField(new Fields.DateField("dd/MM/yyyy")).execute();
```

#### Seitenumbruch einfügen
```java
editor.getActionFactory().insertPageBreak().execute();
```

#### Stil mit Vererbung erstellen
```java
StyleRegistry registry = StyleRegistry.createDefault();
registry.addStyle(Style.paragraphStyle("Mein Stil")
    .parent("Default Paragraph Style")
    .fontSize(14)
    .fontWeight(FontWeight.BOLD)
    .build());
```

## ❓ Häufige Fragen

### Warum wird beim ersten Start so lange geladen?
Beim ersten Start lädt Maven alle Abhängigkeiten (JavaFX, etc.) herunter. Das kann mehrere Minuten dauern. Nach dem ersten Start sind die Abhängigkeiten gecacht und der Start geht schneller.

### Kann ich die Demos ohne Maven starten?
Ja, nach dem ersten Start mit Maven können Sie die JAR-Dateien direkt ausführen:
```bash
cd rich-text-area/samples/target
java -cp "classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':')" \
     com.gluonhq.richtextarea.samples.FullFeaturedDemo
```
(Hinweis: Dies ist komplexer und wird nicht empfohlen.)

### Wo finde ich den Quellcode der Demos?
Im Verzeichnis `rich-text-area/samples/src/main/java/com/gluonhq/richtextarea/samples/`.

### Wie kann ich die Demos modifizieren?
Einfach den Quellcode bearbeiten und mit `./start-demos.sh` neu starten. Maven kompiliert die Änderungen automatisch.

---

**Hinweis:** Dieses Skript ist eine Ergänzung zur offiziellen [samples/README.md](samples/README.md) im RichTextArea-Projekt.
