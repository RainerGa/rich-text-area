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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits a text into plain text and emojis.
 *
 * <p>The emojis of the {@link EmojiData} database are the only source of truth here: a sequence
 * of code points is turned into an {@link Emoji} if and only if the database contains an emoji
 * for exactly that sequence. Every other character is kept as plain text.
 *
 * <p>This differs from {@link com.gluonhq.emoji.util.TextUtils#convertToStringAndEmojiObjects(String)},
 * which guesses the emoji code points with a coarse range check (any code point between U+200D and
 * U+3299 is treated as an emoji) and silently drops the collected characters when the database
 * does not contain a matching emoji. Characters like the en dash (U+2013), the em dash (U+2014),
 * the horizontal ellipsis (U+2026), the euro sign (U+20AC) or box drawing characters were
 * therefore lost when a document was loaded.
 */
final class EmojiTextSplitter {

    /**
     * The variant selector U+FE0F, that turns a code point into its fully qualified emoji form.
     */
    private static final String VARIANT_SELECTOR = "FE0F";

    /**
     * All known emojis, indexed by their unified representation, e.g. "1F44B" or "1F44B-1F3FB".
     */
    private static final Map<String, Emoji> EMOJI_BY_UNIFIED = new HashMap<>();

    /**
     * The unified representations of all emojis, grouped by their first code point and sorted by
     * the length of the sequence, longest sequence first.
     */
    private static final Map<String, List<String>> UNIFIED_BY_FIRST_CODE_POINT = new HashMap<>();

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    static {
        for (Emoji emoji : EmojiData.getEmojiCollection()) {
            String unified = emoji.getUnified();
            if (unified != null && !unified.isEmpty()) {
                EMOJI_BY_UNIFIED.putIfAbsent(unified, emoji);
            }
        }
        EMOJI_BY_UNIFIED.keySet().forEach(unified -> {
            String firstCodePoint = unified.substring(0, separatorIndex(unified));
            UNIFIED_BY_FIRST_CODE_POINT
                    .computeIfAbsent(firstCodePoint, key -> new ArrayList<>())
                    .add(unified);
        });
        UNIFIED_BY_FIRST_CODE_POINT.values().forEach(unifieds ->
                unifieds.sort(Comparator.comparingInt(EmojiTextSplitter::codePointCount).reversed()));
    }

    private EmojiTextSplitter() {
    }

    /**
     * Parses a text string and returns a list of objects: all emojis found are returned as
     * {@link Emoji} objects, the rest of the text is returned as {@link String} objects.
     *
     * <p>Unlike the original implementation, no character of the text is ever lost: a code point
     * that is not part of a known emoji is always part of the returned plain text.
     *
     * @param text a string that might contain emoji unicode characters
     * @return a list of objects
     */
    static List<Object> split(String text) {
        List<Object> objects = new ArrayList<>();
        StringBuilder plainText = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            EmojiMatch match = matchEmoji(text, index);
            if (match == null) {
                int codePoint = text.codePointAt(index);
                plainText.appendCodePoint(codePoint);
                index += Character.charCount(codePoint);
            } else {
                if (plainText.length() > 0) {
                    objects.add(plainText.toString());
                    plainText.setLength(0);
                }
                objects.add(match.emoji);
                index += match.length;
            }
        }
        if (plainText.length() > 0) {
            objects.add(plainText.toString());
        }
        return objects;
    }

    /**
     * Looks for an emoji at the given position of the text.
     *
     * @param text the text that is parsed
     * @param index the position to start at
     * @return the matching emoji and the number of characters it consumes, or null if the text
     *         does not start with a known emoji at that position
     */
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

    /**
     * Checks if the text contains the code points of the given unified representation at the
     * given position.
     *
     * @param text the text that is parsed
     * @param index the position to start at
     * @param unified the unified representation, e.g. "1F44B-1F3FB"
     * @return the number of characters the sequence consumes, or -1 if the text does not start
     *         with that sequence
     */
    private static int matchLength(String text, int index, String unified) {
        int position = index;
        int start = 0;
        while (start >= 0) {
            int separator = unified.indexOf('-', start);
            String segment = separator < 0 ? unified.substring(start) : unified.substring(start, separator);
            int codePoint = Integer.parseInt(segment, 16);
            if (position >= text.length() || text.codePointAt(position) != codePoint) {
                return -1;
            }
            position += Character.charCount(codePoint);
            if (separator < 0) {
                return position - index;
            }
            start = separator + 1;
        }
        return -1;
    }

    /**
     * Checks if a single code point can be the beginning of an emoji, using the same ranges as
     * the original implementation. This is only used to complete a code point with the variant
     * selector, e.g. "2764" is completed to "2764-FE0F".
     *
     * @param codePoint the code point to check
     * @return true if the code point can be the beginning of an emoji
     */
    private static boolean isEmojiCodePoint(int codePoint) {
        // valid ranges for emojis:
        // 127744 <= ch && ch <= 129784 0x1F300-0x1FAF8, excluding the skin tone
        // 126980 <= ch && ch <= 127569 0x1F004-0x1F251
        // 8205 < ch && ch <= 12953 0x200D < ch && ch <= 0x3299
        return (0x1F300 <= codePoint && codePoint <= 0x1FAF8 && !isSkinTone(codePoint)) ||
                (0x1F004 <= codePoint && codePoint <= 0x1F251) ||
                (0x200D < codePoint && codePoint <= 0x3299);
    }

    private static boolean isSkinTone(int codePoint) {
        return 0x1F3FB <= codePoint && codePoint <= 0x1F3FF;
    }

    /**
     * Converts a code point into its unified representation, e.g. 0x1F44B into "1F44B" and 0x23
     * into "0023".
     *
     * @param codePoint the code point to convert
     * @return the unified representation of the code point
     */
    private static String toUnified(int codePoint) {
        int digits = codePoint > 0xFFFF ? 5 : 4;
        char[] chars = new char[digits];
        for (int i = digits - 1; i >= 0; i--) {
            chars[i] = HEX_DIGITS[codePoint & 0xF];
            codePoint >>>= 4;
        }
        return new String(chars);
    }

    private static int separatorIndex(String unified) {
        int separator = unified.indexOf('-');
        return separator < 0 ? unified.length() : separator;
    }

    private static int codePointCount(String unified) {
        int count = 1;
        for (int i = 0; i < unified.length(); i++) {
            if (unified.charAt(i) == '-') {
                count++;
            }
        }
        return count;
    }

    /**
     * An emoji that was found in a text, together with the number of characters it consumes.
     */
    private static final class EmojiMatch {

        private final Emoji emoji;
        private final int length;

        private EmojiMatch(Emoji emoji, int length) {
            this.emoji = emoji;
            this.length = length;
        }
    }
}
