package com.gluonhq.richtextarea.model;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/** Regression contracts for the public API actually shipped by this fork. */
class ForkFeaturesTest {
    @Test void landscapeMarginsSurviveSerialization() throws Exception {
        PageFormat format = PageFormat.builder().pageSize(PageFormat.PageSize.LETTER)
                .orientation(PageFormat.Orientation.LANDSCAPE).margins(new Insets(12, 24, 36, 48)).build();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(format); }
        PageFormat restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (PageFormat) input.readObject();
        }
        assertEquals(792, restored.getWidth());
        assertEquals(612, restored.getHeight());
        assertEquals(720, restored.getContentWidth());
        assertEquals(564, restored.getContentHeight());
        assertEquals(format.getMargins(), restored.getMargins());
    }

    @Test void documentFieldsRetainUserValuesAndTypes() {
        Fields.DocumentTitleField title = new Fields.DocumentTitleField("Title");
        title.setTitle("Release 😀");
        assertEquals("Release 😀", title.getValue());
        assertEquals(Field.FieldType.DOCUMENT_TITLE, title.getType());
        Fields.VariableField variable = new Fields.VariableField("Customer");
        variable.setValue("Müller – 10 €");
        assertEquals("Müller – 10 €", variable.getValue());
        assertEquals("Customer", variable.getName());
        assertEquals(Field.FieldType.VARIABLE, variable.getType());
    }

    @Test void registryCanReplaceAndRemoveNamedStyles() {
        StyleRegistry registry = StyleRegistry.createDefault();
        Style first = Style.textStyle("Custom").fontSize(14).textFill(Color.RED).build();
        registry.addStyle(first);
        assertSame(first, registry.getStyle("Custom"));
        Style replacement = Style.textStyle("Custom").fontSize(18).textFill(Color.BLUE).build();
        registry.addStyle(replacement);
        assertSame(replacement, registry.getStyle("Custom"));
        assertEquals(1, registry.getStyleNames().stream().filter("Custom"::equals).count());
        assertEquals(Color.BLUE, registry.getStyle("Custom").getTextFill());
        registry.removeStyle("Custom");
        assertNull(registry.getStyle("Custom"));
        assertNotNull(registry.getDefaultParagraphStyle());
        assertNotNull(registry.getDefaultTextStyle());
    }
}
