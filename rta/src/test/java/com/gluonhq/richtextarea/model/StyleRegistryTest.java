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
 * Tests for the StyleRegistry class.
 */
class StyleRegistryTest {

    @Test
    void testCreateDefault() {
        StyleRegistry registry = StyleRegistry.createDefault();
        assertFalse(registry.getAllStyles().isEmpty());
        assertTrue(registry.containsStyle("Default Character Style"));
        assertTrue(registry.containsStyle("Default Paragraph Style"));
        assertTrue(registry.containsStyle("Heading 1"));
        assertTrue(registry.containsStyle("Heading 2"));
        assertTrue(registry.containsStyle("Heading 3"));
        assertTrue(registry.containsStyle("Title"));
        assertTrue(registry.containsStyle("Emphasis"));
        assertTrue(registry.containsStyle("Strong"));
        assertTrue(registry.containsStyle("Code"));
    }

    @Test
    void testAddAndRetrieveStyle() {
        StyleRegistry registry = new StyleRegistry();
        Style style = Style.characterStyle("MyStyle")
                .foreground("red")
                .build();

        registry.addStyle(style);
        assertEquals(style, registry.getStyle("MyStyle"));
        assertTrue(registry.containsStyle("MyStyle"));
    }

    @Test
    void testGetNonExistentStyle() {
        StyleRegistry registry = new StyleRegistry();
        assertNull(registry.getStyle("NonExistent"));
        assertFalse(registry.containsStyle("NonExistent"));
    }

    @Test
    void testRemoveStyle() {
        StyleRegistry registry = new StyleRegistry();
        Style style = Style.characterStyle("MyStyle").build();
        registry.addStyle(style);

        Style removed = registry.removeStyle("MyStyle");
        assertEquals(style, removed);
        assertFalse(registry.containsStyle("MyStyle"));
    }

    @Test
    void testGetStylesByType() {
        StyleRegistry registry = StyleRegistry.createDefault();

        var characterStyles = registry.getStylesByType(Style.Type.CHARACTER);
        var paragraphStyles = registry.getStylesByType(Style.Type.PARAGRAPH);

        assertTrue(characterStyles.stream().allMatch(s -> s.getType() == Style.Type.CHARACTER));
        assertTrue(paragraphStyles.stream().allMatch(s -> s.getType() == Style.Type.PARAGRAPH));
        assertFalse(characterStyles.isEmpty());
        assertFalse(paragraphStyles.isEmpty());
    }

    @Test
    void testResolveInheritance() {
        StyleRegistry registry = new StyleRegistry();

        // Create parent style
        Style parent = Style.paragraphStyle("Parent")
                .foreground("black")
                .fontFamily("Arial")
                .fontSize(12)
                .alignment(TextAlignment.LEFT)
                .spacing(0)
                .build();

        // Create child style that inherits from parent
        Style child = Style.paragraphStyle("Child")
                .parent("Parent")
                .fontSize(14) // Override parent's font size
                .fontWeight(FontWeight.BOLD) // New property
                .build();

        registry.addStyle(parent);
        registry.addStyle(child);

        // Resolve child style
        Style resolved = registry.resolve(child);

        // Properties from parent should be inherited
        assertEquals("black", resolved.getForeground());
        assertEquals("Arial", resolved.getFontFamily());
        assertEquals(TextAlignment.LEFT, resolved.getAlignment());
        assertEquals(0.0, resolved.getSpacing());

        // Properties overridden in child should take precedence
        assertEquals(14.0, resolved.getFontSize());
        assertEquals(FontWeight.BOLD, resolved.getFontWeight());
    }

    @Test
    void testResolveByName() {
        StyleRegistry registry = StyleRegistry.createDefault();

        Style resolved = registry.resolveByName("Heading 1");
        assertNotNull(resolved);
        assertEquals("Heading 1", resolved.getName());
        // Should inherit from Default Paragraph Style
        assertEquals(TextAlignment.LEFT, resolved.getAlignment());
        // Should have its own properties
        assertEquals(16.0, resolved.getFontSize());
        assertEquals(FontWeight.BOLD, resolved.getFontWeight());
    }

    @Test
    void testResolveByNameNonExistent() {
        StyleRegistry registry = new StyleRegistry();
        assertNull(registry.resolveByName("NonExistent"));
    }

    @Test
    void testResolveNullStyle() {
        StyleRegistry registry = new StyleRegistry();
        assertNull(registry.resolve(null));
    }

    @Test
    void testMultiLevelInheritance() {
        StyleRegistry registry = new StyleRegistry();

        // Grandparent
        Style grandparent = Style.paragraphStyle("Grandparent")
                .foreground("black")
                .fontFamily("Arial")
                .fontSize(10)
                .alignment(TextAlignment.LEFT)
                .build();

        // Parent
        Style parent = Style.paragraphStyle("Parent")
                .parent("Grandparent")
                .fontSize(12)
                .fontWeight(FontWeight.BOLD)
                .build();

        // Child
        Style child = Style.paragraphStyle("Child")
                .parent("Parent")
                .fontPosture(FontPosture.ITALIC)
                .build();

        registry.addStyle(grandparent);
        registry.addStyle(parent);
        registry.addStyle(child);

        Style resolved = registry.resolve(child);

        // From grandparent
        assertEquals("black", resolved.getForeground());
        assertEquals("Arial", resolved.getFontFamily());
        assertEquals(TextAlignment.LEFT, resolved.getAlignment());

        // From parent (overrides grandparent)
        assertEquals(12.0, resolved.getFontSize());
        assertEquals(FontWeight.BOLD, resolved.getFontWeight());

        // From child
        assertEquals(FontPosture.ITALIC, resolved.getFontPosture());
    }

    @Test
    void testToTextDecorationWithInheritance() {
        StyleRegistry registry = StyleRegistry.createDefault();

        Style style = Style.characterStyle("MyEmphasis")
                .parent("Default Character Style")
                .fontPosture(FontPosture.ITALIC)
                .build();

        registry.addStyle(style);

        TextDecoration td = style.toTextDecoration(registry);
        assertEquals(FontPosture.ITALIC, td.getFontPosture());
        // Should inherit from parent
        assertEquals("black", td.getForeground());
        assertEquals("System", td.getFontFamily());
    }

    @Test
    void testToParagraphDecorationWithInheritance() {
        StyleRegistry registry = StyleRegistry.createDefault();

        Style style = Style.paragraphStyle("MyHeading")
                .parent("Default Paragraph Style")
                .fontSize(18)
                .fontWeight(FontWeight.BOLD)
                .alignment(TextAlignment.CENTER)
                .build();

        registry.addStyle(style);

        ParagraphDecoration pd = style.toParagraphDecoration(registry);
        assertEquals(0.0, pd.getSpacing()); // inherited from parent
        assertEquals(TextAlignment.CENTER, pd.getAlignment()); // overridden
    }

    @Test
    void testSizeAndClear() {
        StyleRegistry registry = new StyleRegistry();
        assertEquals(0, registry.size());

        registry.addStyle(Style.characterStyle("Style1").build());
        registry.addStyle(Style.characterStyle("Style2").build());
        assertEquals(2, registry.size());

        registry.clear();
        assertEquals(0, registry.size());
    }

    @Test
    void testReplaceStyle() {
        StyleRegistry registry = new StyleRegistry();
        Style style1 = Style.characterStyle("MyStyle").foreground("red").build();
        Style style2 = Style.characterStyle("MyStyle").foreground("blue").build();

        registry.addStyle(style1);
        assertEquals("red", registry.getStyle("MyStyle").getForeground());

        registry.addStyle(style2);
        assertEquals("blue", registry.getStyle("MyStyle").getForeground());
        assertEquals(1, registry.size());
    }
}
