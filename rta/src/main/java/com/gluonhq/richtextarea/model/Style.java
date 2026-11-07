package com.gluonhq.richtextarea.model;

import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;

/**
 * Represents a style that can be applied to text or paragraphs.
 * Supports inheritance from parent styles.
 */
public class Style {
    
    public enum StyleType {
        TEXT,
        PARAGRAPH
    }
    
    private final String name;
    private final StyleType type;
    private final String parentName;
    private String fontFamily;
    private double fontSize;
    private FontWeight fontWeight;
    private FontPosture fontPosture;
    private Color textFill;
    private Color backgroundFill;
    private Boolean bold;
    private Boolean italic;
    private Boolean underline;
    private Boolean strikethrough;
    // Paragraph-specific properties
    private Double spacingBefore;
    private Double spacingAfter;
    private Double leftIndent;
    private Double rightIndent;
    private Double firstLineIndent;
    private String alignment;
    
    private Style(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.parentName = builder.parentName;
        this.fontFamily = builder.fontFamily;
        this.fontSize = builder.fontSize;
        this.fontWeight = builder.fontWeight;
        this.fontPosture = builder.fontPosture;
        this.textFill = builder.textFill;
        this.backgroundFill = builder.backgroundFill;
        this.bold = builder.bold;
        this.italic = builder.italic;
        this.underline = builder.underline;
        this.strikethrough = builder.strikethrough;
        this.spacingBefore = builder.spacingBefore;
        this.spacingAfter = builder.spacingAfter;
        this.leftIndent = builder.leftIndent;
        this.rightIndent = builder.rightIndent;
        this.firstLineIndent = builder.firstLineIndent;
        this.alignment = builder.alignment;
    }
    
    public String getName() {
        return name;
    }
    
    public StyleType getType() {
        return type;
    }
    
    public String getParentName() {
        return parentName;
    }
    
    public String getFontFamily() {
        return fontFamily;
    }
    
    public double getFontSize() {
        return fontSize;
    }
    
    public FontWeight getFontWeight() {
        return fontWeight;
    }
    
    public FontPosture getFontPosture() {
        return fontPosture;
    }
    
    public Color getTextFill() {
        return textFill;
    }
    
    public Color getBackgroundFill() {
        return backgroundFill;
    }
    
    public Boolean isBold() {
        return bold;
    }
    
    public Boolean isItalic() {
        return italic;
    }
    
    public Boolean isUnderline() {
        return underline;
    }
    
    public Boolean isStrikethrough() {
        return strikethrough;
    }
    
    public Double getSpacingBefore() {
        return spacingBefore;
    }
    
    public Double getSpacingAfter() {
        return spacingAfter;
    }
    
    public Double getLeftIndent() {
        return leftIndent;
    }
    
    public Double getRightIndent() {
        return rightIndent;
    }
    
    public Double getFirstLineIndent() {
        return firstLineIndent;
    }
    
    public String getAlignment() {
        return alignment;
    }
    
    public static Builder textStyle(String name) {
        return new Builder(name, StyleType.TEXT);
    }
    
    public static Builder paragraphStyle(String name) {
        return new Builder(name, StyleType.PARAGRAPH);
    }
    
    public static class Builder {
        private final String name;
        private final StyleType type;
        private String parentName = null;
        private String fontFamily = null;
        private double fontSize = -1;
        private FontWeight fontWeight = null;
        private FontPosture fontPosture = null;
        private Color textFill = null;
        private Color backgroundFill = null;
        private Boolean bold = null;
        private Boolean italic = null;
        private Boolean underline = null;
        private Boolean strikethrough = null;
        private Double spacingBefore = null;
        private Double spacingAfter = null;
        private Double leftIndent = null;
        private Double rightIndent = null;
        private Double firstLineIndent = null;
        private String alignment = null;
        
        private Builder(String name, StyleType type) {
            this.name = name;
            this.type = type;
        }
        
        public Builder parent(String parentName) {
            this.parentName = parentName;
            return this;
        }
        
        public Builder fontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
            return this;
        }
        
        public Builder fontSize(double fontSize) {
            this.fontSize = fontSize;
            return this;
        }
        
        public Builder fontWeight(FontWeight fontWeight) {
            this.fontWeight = fontWeight;
            return this;
        }
        
        public Builder fontPosture(FontPosture fontPosture) {
            this.fontPosture = fontPosture;
            return this;
        }
        
        public Builder textFill(Color textFill) {
            this.textFill = textFill;
            return this;
        }
        
        public Builder backgroundFill(Color backgroundFill) {
            this.backgroundFill = backgroundFill;
            return this;
        }
        
        public Builder bold(Boolean bold) {
            this.bold = bold;
            return this;
        }
        
        public Builder italic(Boolean italic) {
            this.italic = italic;
            return this;
        }
        
        public Builder underline(Boolean underline) {
            this.underline = underline;
            return this;
        }
        
        public Builder strikethrough(Boolean strikethrough) {
            this.strikethrough = strikethrough;
            return this;
        }
        
        public Builder spacingBefore(double spacingBefore) {
            this.spacingBefore = spacingBefore;
            return this;
        }
        
        public Builder spacingAfter(double spacingAfter) {
            this.spacingAfter = spacingAfter;
            return this;
        }
        
        public Builder leftIndent(double leftIndent) {
            this.leftIndent = leftIndent;
            return this;
        }
        
        public Builder rightIndent(double rightIndent) {
            this.rightIndent = rightIndent;
            return this;
        }
        
        public Builder firstLineIndent(double firstLineIndent) {
            this.firstLineIndent = firstLineIndent;
            return this;
        }
        
        public Builder alignment(String alignment) {
            this.alignment = alignment;
            return this;
        }
        
        public Style build() {
            return new Style(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("Style[name='%s', type=%s, parent=%s]", name, type, parentName);
    }
}
