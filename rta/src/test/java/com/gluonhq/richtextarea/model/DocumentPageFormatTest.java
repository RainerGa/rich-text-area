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

import javafx.geometry.Insets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Document with PageFormat.
 */
class DocumentPageFormatTest {

    @Test
    void testDefaultDocumentHasDefaultPageFormat() {
        Document document = new Document();
        assertNotNull(document.getPageFormat());
        assertEquals(PageFormat.PageSize.A4, document.getPageFormat().getPageSize());
        assertEquals(PageFormat.Orientation.PORTRAIT, document.getPageFormat().getOrientation());
    }

    @Test
    void testDocumentWithCustomPageFormat() {
        PageFormat customFormat = PageFormat.builder()
                .pageSize(PageFormat.PageSize.LETTER)
                .orientation(PageFormat.Orientation.LANDSCAPE)
                .margins(new Insets(36, 36, 36, 36))
                .build();

        Document document = new Document("Test", List.of(), 0, customFormat);
        assertEquals(customFormat, document.getPageFormat());
    }

    @Test
    void testDocumentWithNullPageFormatUsesDefault() {
        Document document = new Document("Test", List.of(), 0, null);
        assertNotNull(document.getPageFormat());
        assertEquals(PageFormat.PageSize.A4, document.getPageFormat().getPageSize());
    }

    @Test
    void testDocumentEqualityWithPageFormat() {
        PageFormat format1 = PageFormat.builder().pageSize(PageFormat.PageSize.A4).build();
        PageFormat format2 = PageFormat.builder().pageSize(PageFormat.PageSize.LETTER).build();

        Document doc1 = new Document("Test", List.of(), 0, format1);
        Document doc2 = new Document("Test", List.of(), 0, format1);
        Document doc3 = new Document("Test", List.of(), 0, format2);

        assertEquals(doc1, doc2);
        assertNotEquals(doc1, doc3);
    }

    @Test
    void testDocumentToStringIncludesPageFormat() {
        PageFormat format = PageFormat.builder().pageSize(PageFormat.PageSize.A4).build();
        Document document = new Document("Test", List.of(), 0, format);

        String toString = document.toString();
        assertTrue(toString.contains("pageFormat"));
        assertTrue(toString.contains("A4"));
    }

    @Test
    void testDocumentWithPageFormatCanBeSerializedForClipboard() throws Exception {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.LETTER)
                .orientation(PageFormat.Orientation.LANDSCAPE)
                .margins(new Insets(12, 24, 36, 48))
                .build();
        Document document = new Document("Copied text", List.of(), 0, format);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(document);
        }

        Document copy;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (Document) input.readObject();
        }

        assertEquals(document.getText(), copy.getText());
        assertEquals(PageFormat.PageSize.LETTER, copy.getPageFormat().getPageSize());
        assertEquals(PageFormat.Orientation.LANDSCAPE, copy.getPageFormat().getOrientation());
        assertEquals(12, copy.getPageFormat().getMargins().getTop());
        assertEquals(24, copy.getPageFormat().getMargins().getRight());
        assertEquals(36, copy.getPageFormat().getMargins().getBottom());
        assertEquals(48, copy.getPageFormat().getMargins().getLeft());
    }
}