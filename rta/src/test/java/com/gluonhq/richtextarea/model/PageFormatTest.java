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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the PageFormat class.
 */
class PageFormatTest {

    @Test
    void testDefaultPageFormat() {
        PageFormat format = PageFormat.builder().build();
        assertEquals(PageFormat.PageSize.A4, format.getPageSize());
        assertEquals(PageFormat.Orientation.PORTRAIT, format.getOrientation());
        assertEquals(72, format.getMargins().getTop());
        assertEquals(72, format.getMargins().getRight());
        assertEquals(72, format.getMargins().getBottom());
        assertEquals(72, format.getMargins().getLeft());
    }

    @Test
    void testPageFormatWithCustomSize() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.LETTER)
                .build();
        assertEquals(PageFormat.PageSize.LETTER, format.getPageSize());
        assertEquals(612, format.getEffectiveWidth());
        assertEquals(792, format.getEffectiveHeight());
    }

    @Test
    void testPageFormatWithLandscapeOrientation() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.LANDSCAPE)
                .build();
        assertEquals(PageFormat.Orientation.LANDSCAPE, format.getOrientation());
        assertEquals(842, format.getEffectiveWidth());
        assertEquals(595, format.getEffectiveHeight());
    }

    @Test
    void testPageFormatWithCustomMargins() {
        PageFormat format = PageFormat.builder()
                .margins(36, 36, 36, 36) // 0.5 inch
                .build();
        assertEquals(36, format.getMargins().getTop());
        assertEquals(36, format.getMargins().getRight());
        assertEquals(36, format.getMargins().getBottom());
        assertEquals(36, format.getMargins().getLeft());
    }

    @Test
    void testPrintableArea() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(72, 72, 72, 72)
                .build();

        assertEquals(595 - 144, format.getPrintableWidth()); // 595 - 72 - 72 = 451
        assertEquals(842 - 144, format.getPrintableHeight()); // 842 - 72 - 72 = 698

        var printableArea = format.getPrintableArea();
        assertEquals(72, printableArea.getMinX());
        assertEquals(72, printableArea.getMinY());
        assertEquals(451, printableArea.getWidth());
        assertEquals(698, printableArea.getHeight());
    }

    @Test
    void testPageFormatEquality() {
        PageFormat format1 = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(72, 72, 72, 72)
                .build();

        PageFormat format2 = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(72, 72, 72, 72)
                .build();

        PageFormat format3 = PageFormat.builder()
                .pageSize(PageFormat.PageSize.LETTER)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(72, 72, 72, 72)
                .build();

        assertEquals(format1, format2);
        assertNotEquals(format1, format3);
        assertEquals(format1.hashCode(), format2.hashCode());
    }

    @Test
    void testPageFormatToString() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(72, 72, 72, 72)
                .build();

        String toString = format.toString();
        assertTrue(toString.contains("A4"));
        assertTrue(toString.contains("PORTRAIT"));
        assertTrue(toString.contains("72"));
    }

    @Test
    void testAllPageSizes() {
        for (PageFormat.PageSize size : PageFormat.PageSize.values()) {
            PageFormat format = PageFormat.builder().pageSize(size).build();
            assertEquals(size, format.getPageSize());
            assertTrue(format.getEffectiveWidth() > 0);
            assertTrue(format.getEffectiveHeight() > 0);
        }
    }
}