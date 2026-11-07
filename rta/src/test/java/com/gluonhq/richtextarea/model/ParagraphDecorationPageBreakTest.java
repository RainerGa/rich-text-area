/*
 * Copyright (c) 2022, 2024, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  See the
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for page break functionality in ParagraphDecoration.
 */
class ParagraphDecorationPageBreakTest {

    @Test
    void testDefaultPageBreakValues() {
        ParagraphDecoration decoration = ParagraphDecoration.builder().presets().build();
        assertFalse(decoration.isPageBreakBefore());
        assertFalse(decoration.isPageBreakAfter());
    }

    @Test
    void testPageBreakBeforeBuilder() {
        ParagraphDecoration decoration = ParagraphDecoration.builder()
                .presets()
                .pageBreakBefore(true)
                .build();
        assertTrue(decoration.isPageBreakBefore());
        assertFalse(decoration.isPageBreakAfter());
    }

    @Test
    void testPageBreakAfterBuilder() {
        ParagraphDecoration decoration = ParagraphDecoration.builder()
                .presets()
                .pageBreakAfter(true)
                .build();
        assertFalse(decoration.isPageBreakBefore());
        assertTrue(decoration.isPageBreakAfter());
    }

    @Test
    void testBothPageBreaksBuilder() {
        ParagraphDecoration decoration = ParagraphDecoration.builder()
                .presets()
                .pageBreakBefore(true)
                .pageBreakAfter(true)
                .build();
        assertTrue(decoration.isPageBreakBefore());
        assertTrue(decoration.isPageBreakAfter());
    }

    @Test
    void testFromDecorationPreservesPageBreaks() {
        ParagraphDecoration original = ParagraphDecoration.builder()
                .presets()
                .pageBreakBefore(true)
                .pageBreakAfter(true)
                .build();

        ParagraphDecoration copy = ParagraphDecoration.builder()
                .fromDecoration(original)
                .build();

        assertTrue(copy.isPageBreakBefore());
        assertTrue(copy.isPageBreakAfter());
    }

    @Test
    void testNormalizeCombinesPageBreaks() {
        ParagraphDecoration decoration1 = ParagraphDecoration.builder()
                .presets()
                .pageBreakBefore(true)
                .build();

        ParagraphDecoration decoration2 = ParagraphDecoration.builder()
                .presets()
                .pageBreakAfter(true)
                .build();

        ParagraphDecoration normalized = decoration1.normalize(decoration2);
        assertTrue(normalized.isPageBreakBefore());
        assertTrue(normalized.isPageBreakAfter());
    }

    @Test
    void testNormalizeWithNull() {
        ParagraphDecoration decoration = ParagraphDecoration.builder()
                .presets()
                .pageBreakBefore(true)
                .pageBreakAfter(true)
                .build();

        ParagraphDecoration normalized = decoration.normalize(null);
        assertTrue(normalized.isPageBreakBefore());
        assertTrue(normalized.isPageBreakAfter());
    }

    @Test
    void testEqualsWithPageBreaks() {
        ParagraphDecoration decoration1 = ParagraphDecoration.builder()
                .presets()
                .pageBreakBefore(true)
                .pageBreakAfter(false)
                .build();

        ParagraphDecoration decoration2 = ParagraphDecoration.builder()
                .presets()
                .pageBreakBefore(true)
                .pageBreakAfter(false)
                .build();

        ParagraphDecoration decoration3 = ParagraphDecoration.builder()
                .presets()
                .pageBreakBefore(false)
                .pageBreakAfter(true)
                .build();

        assertEquals(decoration1, decoration2);
        assertNotEquals(decoration1, decoration3);
        assertEquals(decoration1.hashCode(), decoration2.hashCode());
    }

    @Test
    void testToStringIncludesPageBreaks() {
        ParagraphDecoration decoration = ParagraphDecoration.builder()
                .presets()
                .pageBreakBefore(true)
                .pageBreakAfter(true)
                .build();

        String toString = decoration.toString();
        assertTrue(toString.contains("pbBefore=true"));
        assertTrue(toString.contains("pbAfter=true"));
    }
}