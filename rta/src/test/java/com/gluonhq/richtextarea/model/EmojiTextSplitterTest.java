/*
 * Copyright (c) 2023, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.richtextarea.model;

import com.gluonhq.emoji.Emoji;
import com.gluonhq.emoji.EmojiData;
import com.gluonhq.emoji.util.TextUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmojiTextSplitterTest {

    /**
     * The characters that were dropped by the original implementation, because it treats every
     * code point between U+200D and U+3299 as an emoji, but the emoji database does not contain
     * any emoji for them.
     */
    private static final String DROPPED_CHARACTERS = "–—…€←─⇒±≤≥≠≈∞";

    @Test
    public void keepsCharactersThatAreNoEmoji() {
        String text = "Ein Gedankenstrich " + DROPPED_CHARACTERS + " am Ende.";
        UnitBuffer buffer = UnitBuffer.convertTextToUnits(text);
        assertEquals(text, buffer.getText());
    }

    @Test
    public void keepsTheCharactersOfADocumentWithMarkdownSyntax() {
        String text = "# Überschrift\n\n"
                + "Text mit Gedankenstrich – Pfeil ← und Auslassungspunkt …\n\n"
                + "| Spalte | Wert |\n"
                + "|---|---|\n"
                + "| Preis | 10 € |\n";
        UnitBuffer buffer = UnitBuffer.convertTextToUnits(text);
        assertEquals(text, buffer.getText());
    }

    @Test
    public void keepsEverySingleCodePoint() {
        for (int codePoint = 0x20; codePoint <= 0x2FFFF; codePoint++) {
            if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                continue;
            }
            String character = new String(Character.toChars(codePoint));
            List<Object> objects = EmojiTextSplitter.split(character);
            assertFalse(objects.isEmpty(), "U+" + Integer.toHexString(codePoint) + " was dropped");
            StringBuilder restored = new StringBuilder();
            objects.forEach(object -> restored.append(object instanceof Emoji
                    ? ((Emoji) object).character()
                    : (String) object));
            assertTrue(restored.toString().startsWith(character),
                    "U+" + Integer.toHexString(codePoint) + " was not restored: " + restored);
        }
    }

    @Test
    public void detectsEveryKnownEmoji() {
        for (Emoji emoji : EmojiData.getEmojiCollection()) {
            String unified = emoji.getUnified();
            List<Object> objects = EmojiTextSplitter.split(emoji.character());
            assertEquals(1, objects.size(), "Emoji " + unified + " was not detected: " + objects);
            assertTrue(objects.get(0) instanceof Emoji, "Emoji " + unified + " was not detected: " + objects);
            assertEquals(unified, ((Emoji) objects.get(0)).getUnified(),
                    "Wrong emoji detected for " + unified);
        }
    }

    @Test
    public void detectsEmojiSequences() {
        assertSingleEmoji("1F468-200D-1F469-200D-1F467", "family");
        assertSingleEmoji("1F1E9-1F1EA", "flag");
        assertSingleEmoji("1F44B-1F3FC", "wave with skin tone");
        assertSingleEmoji("0023-FE0F-20E3", "keycap");
        assertSingleEmoji("1F3F3-FE0F-200D-1F308", "rainbow flag");
        assertSingleEmoji("1F9D1-200D-1F680", "person with profession");
    }

    @Test
    public void keepsTheTextAroundAnEmoji() {
        List<Object> objects = EmojiTextSplitter.split("vor 😀 nach");
        assertEquals(3, objects.size(), objects.toString());
        assertEquals("vor ", objects.get(0));
        assertEquals("1F600", ((Emoji) objects.get(1)).getUnified());
        assertEquals(" nach", objects.get(2));
    }

    @Test
    public void findsAllEmojisOfTheOriginalImplementation() {
        List<String> corpus = new ArrayList<>();
        corpus.add("Hello world");
        corpus.add("Hello 👋 world");
        corpus.add("Grüße – mit Gedankenstrich");
        corpus.add("😀😀😀");
        corpus.add("Family: 👨‍👩‍👧 end");
        corpus.add("Flag: 🇩🇪 end");
        corpus.add("Skin tone: 👍🏽 end");
        corpus.add("Keycap: 1️⃣ end");
        corpus.add("Mixed: a–b😀c…d");
        corpus.add("Copyright © and trademark ™");
        corpus.add("Loved ❤ heart");
        StringBuilder allEmojis = new StringBuilder();
        for (Emoji emoji : EmojiData.getEmojiCollection()) {
            allEmojis.append(emoji.character()).append(' ');
        }
        corpus.add(allEmojis.toString());

        corpus.forEach(text -> {
            List<String> expected = unifiedOf(TextUtils.convertToStringAndEmojiObjects(text));
            List<String> actual = unifiedOf(EmojiTextSplitter.split(text));
            assertEquals(expected, actual,
                    "The detected emojis must be the same as with the original implementation for text " + text);
        });
    }

    @Test
    public void keepsTheTextThatTheOriginalImplementationDropped() {
        String text = "a–b";
        List<Object> original = TextUtils.convertToStringAndEmojiObjects(text);
        StringBuilder originalText = new StringBuilder();
        original.forEach(object -> originalText.append(object instanceof Emoji
                ? ((Emoji) object).character()
                : (String) object));
        assertEquals("ab", originalText.toString(), "The original implementation is expected to drop the dash");

        StringBuilder restored = new StringBuilder();
        EmojiTextSplitter.split(text).forEach(object -> restored.append(object instanceof Emoji
                ? ((Emoji) object).character()
                : (String) object));
        assertEquals(text, restored.toString());
    }

    private void assertSingleEmoji(String unified, String description) {
        Emoji expected = EmojiData.emojiFromCodepoints(unified).orElse(null);
        assertNotNull(expected, "Unknown emoji " + unified);
        String character = expected.character();
        List<Object> objects = EmojiTextSplitter.split(character);
        assertEquals(1, objects.size(), "Emoji " + description + " (" + unified + ") was not detected: " + objects);
        assertTrue(objects.get(0) instanceof Emoji, "Emoji " + description + " was not detected: " + objects);
        assertEquals(unified, ((Emoji) objects.get(0)).getUnified(),
                "Wrong emoji detected for " + description);
    }

    private List<String> unifiedOf(List<Object> objects) {
        List<String> unified = new ArrayList<>();
        objects.forEach(object -> {
            if (object instanceof Emoji) {
                unified.add(((Emoji) object).getUnified());
            }
        });
        return unified;
    }
}
