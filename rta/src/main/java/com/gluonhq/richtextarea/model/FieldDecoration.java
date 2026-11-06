package com.gluonhq.richtextarea.model;

/**
 * Decoration for field content in the document.
 * Contains the field and its visual styling.
 */
public class FieldDecoration extends TextDecoration {
    
    private final Field field;
    
    public FieldDecoration(Field field) {
        super();
        this.field = field;
    }
    
    public Field getField() {
        return field;
    }
    
    public FieldDecoration copy() {
        FieldDecoration copy = new FieldDecoration(field);
        copy.setStrikethrough(isStrikethrough());
        copy.setUnderline(isUnderline());
        copy.setFontWeight(getFontWeight());
        copy.setFontPosture(getFontPosture());
        copy.setBackground(getBackground());
        copy.setForeground(getForeground());
        copy.setFontFamily(getFontFamily());
        copy.setFontSize(getFontSize());
        copy.setURL(getURL());
        return copy;
    }
    
    @Override
    public String toString() {
        return "FieldDecoration[field=" + field + "]";
    }
}
