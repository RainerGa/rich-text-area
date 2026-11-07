# Character Loss on Document Load - Documentation

## Bug Description

When a document is loaded into the `RichTextArea`, characters are silently dropped from the
text. Every character of the "General Punctuation" and "Miscellaneous Symbols" area that has
no emoji in the emoji database is lost.

**Example:** The text

```
a–b—c…d€ee←f─g
```

(14 characters) ends up as

```
abcdeefg
```

(8 characters) in the text buffer – the en dash (U+2013), the em dash (U+2014), the horizontal
ellipsis (U+2026), the euro sign (U+20AC), the left arrow (U+2190) and the box drawing
character (U+2500) are gone.

In a real document this means that a markdown file with typographic punctuation is modified
when it is opened and saved again: the dashes and ellipses of the original text disappear.

## Root Cause

The text of a document is split into text and emoji in
`UnitBuffer.convertTextToUnits()`, which uses
`TextUtils.convertToStringAndEmojiObjects()` of the `com.gluonhq.emoji` library.

That method guesses the emoji code points with a coarse range check:

```java
private static boolean isEmoji(int ch) {
    // valid ranges for emojis:
    // 127744 <= ch && ch <= 129784 0x1F300-0x1FAF8, excluding the skin tone
    // 126980 <= ch && ch <= 127569 0x1F004-0x1F251
    // 8205 < ch && ch <= 12953 0x200D < ch && ch <= 0x3299
    return  (0x1F300 <= ch && ch <= 0x1FAF8 && !isSkinTone(ch)) ||
            (0x1F004 <= ch && ch <= 0x1F251) ||
            (0x200D < ch && ch <= 0x3299);
}
```

The third range covers everything between U+200E and U+3299, which includes the whole
"General Punctuation" block (U+2000-U+206F) and the "Letterlike Symbols" and "Arrows" blocks.
All of these characters are collected as a candidate emoji.

**Incorrect flow:**
1. The document is opened, the text of a paragraph is passed to `UnitBuffer.convertTextToUnits()`
2. `TextUtils.convertToStringAndEmojiObjects()` collects the code points of the text
3. A character like U+2013 passes the range check of `isEmoji()` and is collected as a candidate
   emoji ("2013")
4. The candidate is looked up in the emoji database with `EmojiData.emojiFromCodepoints()`
5. The database does not contain an emoji for "2013" – the candidate is discarded
6. The character is **not** added to the plain text either, because it was never part of it
7. The character is lost – the same happens for every character in the range that has no emoji

The emoji database is the only place that knows which sequences are emojis. A character that is
not in the database is plain text and has to be kept as such.

## Fix

**Files:**
- `rta/src/main/java/com/gluonhq/richtextarea/model/EmojiTextSplitter.java` (new)
- `rta/src/main/java/com/gluonhq/richtextarea/model/UnitBuffer.java`

The splitting is no longer based on a range check, but on the emoji database itself: a sequence
of code points is only turned into an emoji if the database contains an emoji for exactly that
sequence. Every other character stays plain text and can therefore not be lost.

The new `EmojiTextSplitter` builds an index of all emojis of the database once, grouped by
their first code point, and then matches the text against that index, longest sequence first:

```java
private static EmojiMatch matchEmoji(String text, int index) {
    int codePoint = text.codePointAt(index);
    String unified = toUnified(codePoint);
    List<String> candidates = UNIFIED_BY_FIRST_CODE_POINT.get(unified);
    if (candidates != null) {
        for (String candidate : candidates) {
            int length = matchLength(text, index, candidate);
            if (length > 0) {
                return new EmojiMatch(EMOJI_BY_UNIFIED.get(candidate), length);
            }
        }
    }
    // A code point that is not followed by the variant selector can still be an emoji, e.g.
    // "2764" is the non-qualified form of the emoji "2764-FE0F".
    if (isEmojiCodePoint(codePoint)) {
        Emoji emoji = EMOJI_BY_UNIFIED.get(unified + "-" + VARIANT_SELECTOR);
        if (emoji != null) {
            return new EmojiMatch(emoji, Character.charCount(codePoint));
        }
    }
    return null;
}
```

`UnitBuffer.convertTextToUnits()` now uses the new splitter:

```java
public static UnitBuffer convertTextToUnits(String text) {
    List<Unit> units = new ArrayList<>();
    EmojiTextSplitter.split(text).stream()
            .map(o -> {
                if (o instanceof Emoji) {
                    return List.of(new EmojiUnit((Emoji) o));
                } else {
                    return createTextAndBlockUnits((String) o);
                }
            })
            .forEach(units::addAll);
    return new UnitBuffer(units);
}
```

The detected emojis are exactly the same as before – the only difference is that the characters
that are not part of an emoji are kept instead of being dropped.

## Unit Test

**File:** `rta/src/test/java/com/gluonhq/richtextarea/model/EmojiTextSplitterTest.java`

The tests reproduce the bug automatically:

```java
@Test
public void keepsCharactersThatAreNoEmoji() {
    String text = "Ein Gedankenstrich " + DROPPED_CHARACTERS + " am Ende.";
    UnitBuffer buffer = UnitBuffer.convertTextToUnits(text);
    assertEquals(text, buffer.getText());
}
```

The test suite covers:

| Test | Purpose |
|------|---------|
| `keepsCharactersThatAreNoEmoji` | the reported characters survive the conversion |
| `keepsTheCharactersOfADocumentWithMarkdownSyntax` | a markdown document keeps all its characters |
| `keepsEverySingleCodePoint` | no code point of U+0020 - U+2FFFF is ever dropped |
| `detectsEveryKnownEmoji` | every emoji of the database is detected as exactly one emoji |
| `detectsEmojiSequences` | ZWJ sequences, flags, skin tones, keycaps and tag flags are detected |
| `keepsTheTextAroundAnEmoji` | the text before and after an emoji is not merged or lost |
| `findsAllEmojisOfTheOriginalImplementation` | the detected emojis are exactly the same as with the original implementation |
| `keepsTheTextThatTheOriginalImplementationDropped` | documents the difference to the original implementation |

## Running the Tests

```bash
# The test suite of the library is not part of the project build, it needs the test profile
./mvnw -B -ntp -Ptest -DskipTests=false -Dtest=EmojiTextSplitterTest test -f rta
```

## Test Results

### Before the Fix (bug):

```
input  (14): a–b—c…d€ee←f─g
output (8): abcdeefg
```

6 of 14 characters were lost.

In the demo document of the editor (`demo.md`, 3170 characters) the two en dashes (U+2013) and
the em dash (U+2014) were lost:

```
Das Zeichen U+2013 ging beim Laden von demo.md verloren ==> expected: <2> but was: <0>
```

### After the Fix (correct):

```
input  (14): a–b—c…d€ee←f─g
output (14): a–b—c…d€ee←f─g
```

No character is lost, the emojis are still detected:

```
Library test suite:  Tests run: 217, Failures: 0, Errors: 0, Skipped: 0  → BUILD SUCCESS
Editor test suite:  Tests run: 553, Failures: 0, Errors: 0, Skipped: 0  → BUILD SUCCESS
```

The editor regression test is in
`editor/src/test/java/de/richmarkdown/editor/SpecialCharacterPreservationTest.java` and covers
the complete path from the markdown file to the document in the editor.

## Summary

| Aspect | Details |
|--------|---------|
| **Bug** | Characters without an emoji are dropped when a document is loaded |
| **Root Cause** | The emoji library treats every code point between U+200D and U+3299 as an emoji and discards the collected characters when the emoji database does not contain a matching emoji |
| **Fix** | The text is split with the emoji database as the only source of truth, characters that are not part of an emoji stay plain text |
| **Test** | `EmojiTextSplitterTest` (library), `SpecialCharacterPreservationTest` (editor) |
| **Status** | ✅ Fixed and verified |
