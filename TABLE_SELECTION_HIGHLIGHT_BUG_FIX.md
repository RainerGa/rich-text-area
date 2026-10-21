# Table Selection Highlight Bug - Documentation

## Bug Description

When a text range is selected in a table cell, the selection highlight is drawn in the wrong
place. Only the first character of the selected range is highlighted in its own cell, the
remaining characters of the range appear as separate single-character marks over the table rows
below the cell.

**Example:** A 15x3 table with the cell "Headings" and the selection "Heading" (7 characters):

- Expected: one rectangle over "Heading" in the "Headings" cell ✅
- Actual: the "H" is highlighted in the cell, the remaining six characters are drawn as single
  marks over the six rows below ("Blockquote", "Bulleted List", "Numbered List", "Code Block",
  "Horizontal Rule", "Links") ❌

The bug appears whenever the cell is created while the selection is already active, e.g. when the
table is scrolled into view after the selection was made - which is the normal case in an
application that searches in a document.

## Root Cause

The highlight of a layer is calculated in
`ParagraphTile.Layer.updateSelection()` with `TextFlow.rangeShape(...)`. The result depends on
the text layout of the `TextFlow` of the layer.

When a table cell is created (`ParagraphTile.createGridBox()`), the cell is not laid out yet, so
its `TextFlow` has no width. The line break then puts every single character of the cell on its
own line, and `rangeShape()` returns one rectangle per character.

**Incorrect flow:**
1. The user selects a range in a table cell (e.g. via a search dialog)
2. The table is scrolled into view, the VirtualFlow creates the cell
3. `RichListCell.updateItem()` -> `ParagraphTile.updateLayout()` -> `Layer.updateLayout()`
4. `Layer.updateSelection()` calls `textFlow.rangeShape(...)`
5. The `TextFlow` still has no width -> every character is on its own line
6. `rangeShape()` returns one rectangle per character (7 characters -> 35 path elements)
7. The cell is laid out with its final width afterwards, so the text is displayed correctly
8. The highlight is only recalculated when the selection itself changes - the stale shape stays
   and is drawn across the following table rows

The caret does not suffer from this, because `Layer.updateCaretPosition()` forces a layout before
it queries the caret shape:

```java
// Force full layout (CSS + layout) before querying caret shape to ensure
// textFlow.caretShape() calculates positions based on current text content
textFlow.applyCss();
textFlow.layout();
```

## Fix

**File:** `rta/src/main/java/com/gluonhq/richtextarea/ParagraphTile.java`

The highlight is recalculated whenever the layout of the `TextFlow` changes, so it always matches
the final geometry of the cell:

```java
// Die Auswahl-Hervorhebung wird aus dem Text-Layout berechnet. Beim
// Anlegen einer Tabellenzelle hat der TextFlow noch keine Breite: der
// Umbruch legt dann jedes Zeichen in eine eigene Zeile. Die so
// berechnete Form passt nicht mehr zur Zelle und ragt ueber die
// folgenden Zeilen hinaus. Deshalb wird die Form nach jeder
// Layoutaenderung des TextFlows neu berechnet.
textFlow.layoutBoundsProperty().addListener((obs, old, nv) -> updateSelection(viewModel.getSelection()));
```

The listener is added in the constructor of the inner class `Layer`, i.e. for every layer of every
paragraph - the same mechanism that is already used for the background color paths of a paragraph.

## Unit Test

**File:** `rta/src/test/java/com/gluonhq/richtextarea/ui/TableSelectionHighlightTest.java`

The test `selectionHighlightMatchesTheCellLayout` reproduces the bug automatically. It builds the
situation of the bug in three steps:

1. a document with filler paragraphs and a table with text in its cells is opened,
2. the selection is set inside a cell of the table,
3. the table is scrolled into view, so the cell is created while the selection is already active.

The test measures the highlight of the marked cell and compares it with the shape that the laid
out text flow reports for the selected range:

```java
PathElement[] expected = snapshot.textFlow().rangeShape(0, SELECTED_RANGE.length());
Bounds expectedBounds = boundsOf(expected);
Bounds actualBounds = snapshot.selectionPath().getBoundsInLocal();

assertEquals(expectedBounds.getWidth(), actualBounds.getWidth(), TOLERANCE_PX,
        "The highlight must cover exactly '" + SELECTED_RANGE + "': " + snapshot);
assertEquals(expectedBounds.getHeight(), actualBounds.getHeight(), TOLERANCE_PX,
        "The highlight must stay on a single line: " + snapshot);
```

It also asserts that only the cell with the selection is marked and that the highlight stays
inside the table row.

## Running the Tests

```bash
# The test suite of the library is not part of the project build, it needs the test profile
./mvnw -B -ntp -Ptest -DskipTests=false -Dtest=TableSelectionHighlightTest test -f rta
```

## Test Results

### Before the Fix (bug):
```
layers=14
highlight bounds: BoundingBox [minX:-1.0, minY:-1.0, width:12.374, height:135.476]
path elements: 35
text flow: BoundingBox [minX:0.0, minY:0.0, width:191.667, height:19.068]
```

The highlight is 12.4px wide (one character) but 135.5px high (seven lines) - one rectangle per
character of the selected range, drawn over the rows below the cell.

### After the Fix (correct):
```
layers=14
highlight bounds: BoundingBox [minX:-1.0, minY:-1.0, width:57.608, height:21.068]
path elements: 5
text flow: BoundingBox [minX:0.0, minY:0.0, width:191.667, height:19.068]
```

The highlight is one rectangle over the whole range ("Heading"), 57.6px wide and 21.1px high,
i.e. it stays in the line of the cell.

## Summary

| Aspect | Details |
|--------|---------|
| **Bug** | The selection highlight of a table cell is drawn as one mark per character over the following table rows |
| **Root Cause** | The highlight is calculated from the text layout of a `TextFlow` that has no width yet, because the cell was just created |
| **Fix** | Recalculate the highlight when the layout of the `TextFlow` changes |
| **Test** | `TableSelectionHighlightTest.selectionHighlightMatchesTheCellLayout` |
| **Status** | ✅ Fixed and verified |
