# Table Column Loss on Replace - Documentation

## Bug Description

When text inside a table cell is replaced (search & replace, dialog or programmatic), a whole table column can disappear.

**Example:** In a document with a table containing a cell "Via pipe syntax", replacing the space character with another character causes the cell "Yes" together with its separator to be deleted:

```
Vorher: | Feature | Supported | Notes |        | Bold | Yes | Via pipe syntax |
                                    ^^^                                          <-- dieses "|" und "Yes" verschwinden
Nachher:| Feature | Supported | Notes |        | Bold | Yes | Via pipe syntax |
                                                       <-- die letzte Spalte ist weg / verschoben
```

Concretely: the number of table cells drops from `rows * columns` to `rows * columns - 1`. The table is no longer recognized as a table (the cell separator count no longer matches `rows * columns - 1`), so the last column is not rendered and the written markdown loses a column.

## Root Cause

The library converts editor/text positions into internal piece-table positions in
`PieceTable.getInternalSelection()` and `PieceTable.getInternalPosition()`.

That conversion accumulates the length of every text piece in a `StringBuilder sb` and compares it against the requested position. For each piece it used `unit.getText().length()`:

```java
int sbMax = sbMin + unit.getText().length();
...
sb.append(unit.getText());
```

**`unit.getText()` returns the text of the whole unit**, not the text of the piece. A piece, however, covers only part of its unit whenever the text was edited - which is exactly what happens inside a table cell: the cell's text unit is split into "before", "replacement", "after" pieces, and each piece covers only part of the original unit.

**Incorrect flow (before the fix):**
1. The user replaces text inside a table cell.
2. The cell's text unit is split into several pieces; every piece still reports the *whole* unit text via `unit.getText().length()`.
3. `getInternalSelection()` therefore accumulates too many characters for every split piece.
4. All subsequent positions are shifted. The range that is deleted for the replacement no longer covers the match, but the cell separator *and* part of the neighbouring cell.
5. The cell separator (`U+200B`) is deleted - the table loses exactly one separator, i.e. `rows * columns - 2` instead of `rows * columns - 1`.
6. `isTableParagraph()` (MarkdownWriter) requires exactly `rows * columns - 1` separators. The check fails, the paragraph is no longer treated as a table - a whole column disappears from the rendered table and from the saved markdown.

## Fix

**Files:**
- `rta/src/main/java/com/gluonhq/richtextarea/model/PieceTable.java`

The conversion must use the *piece's own* text length, not the whole unit's text length. A new helper `exportableText(Piece, Unit)` returns the text that the piece actually covers:

* For a `TextUnit` the exportable text equals the piece's internal text (`piece.getInternalText()`), whose length is `piece.length()`.
* For all other units (emoji, image, block) the exportable text is the unit's text itself (e.g. an emoji renders as several characters but occupies one internal position).

```java
private static String exportableText(Piece piece, Unit unit) {
    return unit instanceof TextUnit ? piece.getInternalText() : unit.getText();
}
```

Both `getInternalPosition(int position)` and `getInternalSelection(Selection selection)` now use `exportableText(p, unit)` for the position accumulation instead of `unit.getText()`.

### Before (buggy):

```java
int sbMax = sbMin + unit.getText().length();
...
sb.append(unit.getText());
```

### After (fixed):

```java
String text = exportableText(p, unit);
int sbMax = sbMin + text.length();
...
sb.append(text);
```

## Unit Test

**Files:**
- `rta/src/test/java/com/gluonhq/richtextarea/model/PieceTableInternalSelectionTests.java` (new)
- `editor/src/test/java/de/richmarkdown/findreplace/TableReplaceTest.java` (new)

The library test `internalSelectionWithSplitPiece` reproduces the bug directly: after an insertion splits a text unit, `getInternalSelection` must return the exact internal positions. Without the fix it returns positions shifted by the length difference of the split unit (e.g. `1` instead of `6`, or `-1` because the end position is no longer found).

The editor test `replaceAllTwiceInTheSameCellKeepsAllColumns` reproduces the reported bug end-to-end: a table cell contains the search term twice ("Via pipe syntax" contains two spaces). Replacing all spaces shifts the positions of the second replacement so far that the cell separator and the neighbouring cell are deleted - the table loses a cell (15 instead of 16 cells) before the fix.

```bash
# Library test
cd rta && mvn -B -ntp -Ptest -DskipTests=false -Dtest=PieceTableInternalSelectionTests test -f rta

# Editor test
cd editor && mvn -B -ntp -pl editor -am -Duser.home=/tmp/kilo/home -Dtest=TableReplaceTest test
```

## Test Results

### Before the Fix (bug):

```
Library:  TableReplaceTest.replaceAllTwiceInTheSameCellKeepsAllColumns
          Die Anzahl der Tabellenzellen hat sich geaendert:
          [...] -> [Feature, Supported, Notes, Bold, Yes, Via_toolbar, Italic, Yes,
          Via_toolbar, Headings, Yes, _ through_H6, Tables, _ pipe_syntax]
          ==> expected: <15> but was: <14>   ❌

Editor:   TableReplaceTest.replaceAllTwiceInTheSameCellKeepsAllColumns
          Zelle 5 hat einen unerwarteten Text: 'Durch Werkzeugleistebar'
          (erwartet 'Durch Werkzeugleiste')   ❌
```

### After the Fix (correct):

```
Library:  Tests run: 5, Failures: 0, Errors: 0  -> BUILD SUCCESS   ✅
Editor:   TableReplaceTest: Tests run: 6, Failures: 0, Errors: 0  -> BUILD SUCCESS   ✅
```

Full suites (with the fix):
| Suite | Result |
|-------|--------|
| Library (`rta`) | Tests run: 222, Failures: 0, Errors: 0 |
| Editor (`editor`) | Tests run: 575, Failures: 1 (pre-existing), Errors: 0 |

The single editor failure (`FindReplaceDialogTest.dialog_replaceAllSkipsParagraphBoundary`) is pre-existing in this environment and fails identically without the fix.

## Summary

| Aspect | Details |
|--------|---------|
| **Bug** | Replacing text inside a table cell deletes the cell separator, so a whole column disappears |
| **Root Cause** | `PieceTable.getInternalSelection()`/`getInternalPosition()` use `unit.getText().length()` (the whole unit) instead of the piece's own length; after an edit splits a unit into pieces, every piece still reports the whole unit length, so positions shift and the wrong range is deleted |
| **Fix** | Helper `exportableText(Piece, Unit)` returns the piece's actual text; both conversion methods use `p.length` for text units |
| **Test** | `PieceTableInternalSelectionTests` (library), `TableReplaceTest` (editor) |
| **Status** | ✅ Fixed and verified |
