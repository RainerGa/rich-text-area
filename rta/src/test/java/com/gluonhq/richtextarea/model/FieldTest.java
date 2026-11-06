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

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Field implementations and DefaultFieldContext.
 */
class FieldTest {

    @Test
    void testPageNumberField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setCurrentPageNumber(5);

        Fields.PageNumberField field = new Fields.PageNumberField();
        assertEquals("5", field.getValue(context));
        assertEquals(Field.FieldType.PAGE_NUMBER, field.getType());
        assertEquals("Page Number", field.getDisplayName());
    }

    @Test
    void testPageNumberFieldWithFormat() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setCurrentPageNumber(3);

        Fields.PageNumberField field = new Fields.PageNumberField("Page #");
        assertEquals("Page 3", field.getValue(context));
    }

    @Test
    void testPageCountField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setPageCount(10);

        Fields.PageCountField field = new Fields.PageCountField();
        assertEquals("10", field.getValue(context));
        assertEquals(Field.FieldType.PAGE_COUNT, field.getType());
        assertEquals("Page Count", field.getDisplayName());
    }

    @Test
    void testPageCountFieldWithFormat() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setPageCount(15);

        Fields.PageCountField field = new Fields.PageCountField("Page ## of ##");
        assertEquals("Page ## of 15", field.getValue(context));
    }

    @Test
    void testDateField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setCurrentDate(LocalDate.of(2024, 1, 15));

        Fields.DateField field = new Fields.DateField();
        assertEquals("2024-01-15", field.getValue(context));
        assertEquals(Field.FieldType.DATE, field.getType());
        assertEquals("Date", field.getDisplayName());
    }

    @Test
    void testDateFieldWithPattern() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setCurrentDate(LocalDate.of(2024, 1, 15));

        Fields.DateField field = new Fields.DateField("MM/dd/yyyy");
        assertEquals("01/15/2024", field.getValue(context));
    }

    @Test
    void testDateFieldWithLocale() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setCurrentDate(LocalDate.of(2024, 1, 15));

        Fields.DateField field = new Fields.DateField("dd MMMM yyyy", Locale.ENGLISH);
        assertEquals("15 January 2024", field.getValue(context));
    }

    @Test
    void testTimeField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setCurrentTime(LocalTime.of(14, 30, 45));

        Fields.TimeField field = new Fields.TimeField();
        assertEquals("14:30:45", field.getValue(context));
        assertEquals(Field.FieldType.TIME, field.getType());
        assertEquals("Time", field.getDisplayName());
    }

    @Test
    void testTimeFieldWithPattern() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setCurrentTime(LocalTime.of(14, 30, 45));

        Fields.TimeField field = new Fields.TimeField("hh:mm a");
        assertEquals("02:30 PM", field.getValue(context));
    }

    @Test
    void testDocumentTitleField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setDocumentTitle("My Document");

        Fields.DocumentTitleField field = new Fields.DocumentTitleField();
        assertEquals("My Document", field.getValue(context));
        assertEquals(Field.FieldType.DOCUMENT_TITLE, field.getType());
        assertEquals("Document Title", field.getDisplayName());
    }

    @Test
    void testDocumentSubjectField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setDocumentSubject("Quarterly Report");

        Fields.DocumentSubjectField field = new Fields.DocumentSubjectField();
        assertEquals("Quarterly Report", field.getValue(context));
        assertEquals(Field.FieldType.DOCUMENT_SUBJECT, field.getType());
    }

    @Test
    void testDocumentAuthorField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setDocumentAuthor("John Doe");

        Fields.DocumentAuthorField field = new Fields.DocumentAuthorField();
        assertEquals("John Doe", field.getValue(context));
        assertEquals(Field.FieldType.DOCUMENT_AUTHOR, field.getType());
    }

    @Test
    void testVariableField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setVariable("company", "Acme Corp");

        Fields.VariableField field = new Fields.VariableField("company");
        assertEquals("Acme Corp", field.getValue(context));
        assertEquals(Field.FieldType.VARIABLE, field.getType());
        assertEquals("Variable: company", field.getDisplayName());
    }

    @Test
    void testVariableFieldNotFound() {
        DefaultFieldContext context = new DefaultFieldContext();

        Fields.VariableField field = new Fields.VariableField("nonexistent");
        assertEquals("", field.getValue(context));
    }

    @Test
    void testFileNameField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setFileName("document.rtf");

        Fields.FileNameField field = new Fields.FileNameField();
        assertEquals("document.rtf", field.getValue(context));
        assertEquals(Field.FieldType.FILENAME, field.getType());
    }

    @Test
    void testUserNameField() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setUserName("TestUser");

        Fields.UserNameField field = new Fields.UserNameField();
        assertEquals("TestUser", field.getValue(context));
        assertEquals(Field.FieldType.USER_NAME, field.getType());
    }

    @Test
    void testCustomField() {
        DefaultFieldContext context = new DefaultFieldContext();

        Fields.CustomField field = new Fields.CustomField("Custom", ctx -> "Custom Value");
        assertEquals("Custom Value", field.getValue(context));
        assertEquals(Field.FieldType.CUSTOM, field.getType());
        assertEquals("Custom", field.getDisplayName());
    }

    @Test
    void testFieldDecoration() {
        Fields.PageNumberField field = new Fields.PageNumberField();
        FieldDecoration decoration = new FieldDecoration(field);

        assertEquals(field, decoration.getField());
        assertEquals("Page Number", decoration.toString());
    }

    @Test
    void testFieldDecorationEquality() {
        Fields.PageNumberField field1 = new Fields.PageNumberField();
        Fields.PageNumberField field2 = new Fields.PageNumberField();

        FieldDecoration dec1 = new FieldDecoration(field1);
        FieldDecoration dec2 = new FieldDecoration(field2);

        assertEquals(dec1, dec2);
        assertEquals(dec1.hashCode(), dec2.hashCode());
    }

    @Test
    void testDefaultFieldContextDefaults() {
        DefaultFieldContext context = new DefaultFieldContext();

        assertEquals(1, context.getCurrentPageNumber());
        assertEquals(1, context.getPageCount());
        assertEquals("", context.getDocumentTitle());
        assertEquals("", context.getDocumentSubject());
        assertEquals("", context.getDocumentAuthor());
        assertEquals("", context.getFileName());
        assertEquals("", context.getTemplateName());
        assertNotNull(context.getCurrentDate());
        assertNotNull(context.getCurrentTime());
    }

    @Test
    void testDefaultFieldContextEquality() {
        DefaultFieldContext context1 = new DefaultFieldContext()
                .setCurrentPageNumber(3)
                .setPageCount(10)
                .setDocumentTitle("Test");

        DefaultFieldContext context2 = new DefaultFieldContext()
                .setCurrentPageNumber(3)
                .setPageCount(10)
                .setDocumentTitle("Test");

        assertEquals(context1, context2);
        assertEquals(context1.hashCode(), context2.hashCode());
    }

    @Test
    void testDefaultFieldContextVariables() {
        DefaultFieldContext context = new DefaultFieldContext()
                .setVariable("var1", "value1")
                .setVariable("var2", "value2");

        assertEquals("value1", context.getVariable("var1"));
        assertEquals("value2", context.getVariable("var2"));
        assertNull(context.getVariable("var3"));

        context.removeVariable("var1");
        assertNull(context.getVariable("var1"));
    }
}
