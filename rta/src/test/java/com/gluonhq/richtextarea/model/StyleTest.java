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

import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Style class.
 */
class StyleTest {

    @Test
    void testCharacterStyleCreation() {
        Style style = Style.characterStyle("MyStyle")
                .foreground("red")
                .fontWeight(FontWeight.BOLD)
                .build();

        assertEquals("MyStyle", style.getName());
        assertEquals(Style.Type.CHARACTER, style.getType());
        assertEquals("red", style.getForeground());
        assertEquals(FontWeight.BOLD, style.getFontWeight());
        assertNull(style.getParentName());
    }

    @Test
    void testParagraphStyleCreation() {
        Style style = Style.paragraphStyle("MyParagraph")
                .alignment(TextAlignment.CENTER)
                .spacing(10)
                .build();

        assertEquals("MyParagraph", style.getName());
        assertEquals(Style.Type.PARAGRAPH, style.getType());
        assertEquals(TextAlignment.CENTER, style.getAlignment());
        assertEquals(10.0, style.getSpacing());
    }

    @Test
    void testStyleWithParent() {
        Style style = Style.paragraphStyle("Child")
                .parent("Parent")
                .fontSize(14)
                .build();

        assertEquals("Child", style.getName());
        assertEquals("Parent", style.getParentName());
        assertEquals(14.0, style.getFontSize());
    }

    @Test
    void testStyleEquality() {
        Style style1 = Style.characterStyle("Test")
                .foreground("blue")
                .build();

        Style style2 = Style.characterStyle("Test")
                .foreground("blue")
                .build();

        Style style3 = Style.characterStyle("Test")
                .foreground("red")
                .build();

        assertEquals(style1, style2);
        assertNotEquals(style1, style3);
        assertEquals(style1.hashCode(), style2.hashCode());
    }

    @Test
    void testStyleToString() {
        Style style = Style.paragraphStyle("Heading1")
                .parent("Default")
                .build();

        String toString = style.toString();
        assertTrue(toString.contains("Heading1"));
        assertTrue(toString.contains("PARAGRAPH"));
        assertTrue(toString.contains("Default"));
    }

    @Test
    void testStyleWithAllProperties() {
        Style style = Style.paragraphStyle("FullStyle")
                .parent("Parent")
                .foreground("black")
                .background("white")
                .fontFamily("Arial")
                .fontSize(12)
                .fontPosture(FontPosture.ITALIC)
                .fontWeight(FontWeight.BOLD)
                .strikethrough(true)
                .underline(false)
                .url("http://example.com")
                .spacing(5)
                .alignment(TextAlignment.JUSTIFY)
                .insets(10, 20, 10, 20)
                .indentationLevel(2)
                .graphicType(ParagraphDecoration.GraphicType.NUMBERED_LIST)
                .startNumber(3)
                .pageBreakBefore(true)
                .pageBreakAfter(false)
                .build();

        assertEquals("FullStyle", style.getName());
        assertEquals("Parent", style.getParentName());
        assertEquals("black", style.getForeground());
        assertEquals("white", style.getBackground());
        assertEquals("Arial", style.getFontFamily());
        assertEquals(12.0, style.getFontSize());
        assertEquals(FontPosture.ITALIC, style.getFontPosture());
        assertEquals(FontWeight.BOLD, style.getFontWeight());
        assertTrue(style.getStrikethrough());
        assertFalse(style.getUnderline());
        assertEquals("http://example.com", style.getUrl());
        assertEquals(5.0, style.getSpacing());
        assertEquals(TextAlignment.JUSTIFY, style.getAlignment());
        assertEquals(10.0, style.getTopInset());
        assertEquals(20.0, style.getRightInset());
        assertEquals(10.0, style.getBottomInset());
        assertEquals(20.0, style.getLeftInset());
        assertEquals(2, style.getIndentationLevel());
        assertEquals(ParagraphDecoration.GraphicType.NUMBERED_LIST, style.getGraphicType());
        assertEquals(3, style.getStartNumber());
        assertTrue(style.getPageBreakBefore());
        assertFalse(style.getPageBreakAfter());
    }

    @Test
    void testToTextDecoration() {
        StyleRegistry registry = StyleRegistry.createDefault();
        Style style = Style.characterStyle("Test")
                .foreground("red")
                .fontWeight(FontWeight.BOLD)
                .build();

        TextDecoration td = style.toTextDecoration(registry);
        assertEquals("red", td.getForeground());
        assertEquals(FontWeight.BOLD, td.getFontWeight());
    }

    @Test
    void testToParagraphDecoration() {
        StyleRegistry registry = StyleRegistry.createDefault();
        Style style = Style.paragraphStyle("Test")
                .alignment(TextAlignment.CENTER)
                .spacing(10)
                .build();

        ParagraphDecoration pd = style.toParagraphDecoration(registry);
        assertEquals(TextAlignment.CENTER, pd.getAlignment());
        assertEquals(10.0, pd.getSpacing());
    }
}
