package com.gluonhq.richtextarea.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for managing styles in a document.
 * Supports style inheritance and lookups.
 */
public class StyleRegistry {
    
    private final Map<String, Style> styles = new HashMap<>();
    private Style defaultTextStyle;
    private Style defaultParagraphStyle;
    
    private StyleRegistry() {
    }
    
    /**
     * Create a default style registry with common styles.
     */
    public static StyleRegistry createDefault() {
        StyleRegistry registry = new StyleRegistry();
        
        // Create default text style
        registry.defaultTextStyle = Style.textStyle("Default Text Style")
                .fontFamily("System")
                .fontSize(12)
                .textFill(javafx.scene.paint.Color.BLACK)
                .build();
        registry.addStyle(registry.defaultTextStyle);
        
        // Create default paragraph style
        registry.defaultParagraphStyle = Style.paragraphStyle("Default Paragraph Style")
                .parent(null)
                .spacingAfter(12)
                .alignment("LEFT")
                .build();
        registry.addStyle(registry.defaultParagraphStyle);
        
        // Add common text styles
        registry.addStyle(Style.textStyle("Heading 1")
                .fontSize(24)
                .fontWeight(javafx.scene.text.FontWeight.BOLD)
                .spacingAfter(18)
                .build());
        
        registry.addStyle(Style.textStyle("Heading 2")
                .fontSize(20)
                .fontWeight(javafx.scene.text.FontWeight.BOLD)
                .spacingAfter(14)
                .build());
        
        registry.addStyle(Style.textStyle("Heading 3")
                .fontSize(16)
                .fontWeight(javafx.scene.text.FontWeight.BOLD)
                .spacingAfter(12)
                .build());
        
        registry.addStyle(Style.textStyle("Bold")
                .bold(true)
                .build());
        
        registry.addStyle(Style.textStyle("Italic")
                .italic(true)
                .build());
        
        registry.addStyle(Style.textStyle("Underline")
                .underline(true)
                .build());
        
        return registry;
    }
    
    /**
     * Add a style to the registry.
     */
    public void addStyle(Style style) {
        styles.put(style.getName(), style);
    }
    
    /**
     * Remove a style from the registry.
     */
    public void removeStyle(String name) {
        styles.remove(name);
    }
    
    /**
     * Get a style by name.
     */
    public Style getStyle(String name) {
        return styles.get(name);
    }
    
    /**
     * Get a style by name with inheritance resolution.
     * This will resolve all parent styles and merge their properties.
     */
    public Style getResolvedStyle(String name) {
        Style style = styles.get(name);
        if (style == null) {
            return null;
        }
        
        // TODO: Implement inheritance resolution
        return style;
    }
    
    /**
     * Get all style names.
     */
    public List<String> getStyleNames() {
        return new ArrayList<>(styles.keySet());
    }
    
    /**
     * Get all styles.
     */
    public List<Style> getStyles() {
        return new ArrayList<>(styles.values());
    }
    
    /**
     * Get all text styles.
     */
    public List<Style> getTextStyles() {
        List<Style> result = new ArrayList<>();
        for (Style style : styles.values()) {
            if (style.getType() == Style.StyleType.TEXT) {
                result.add(style);
            }
        }
        return result;
    }
    
    /**
     * Get all paragraph styles.
     */
    public List<Style> getParagraphStyles() {
        List<Style> result = new ArrayList<>();
        for (Style style : styles.values()) {
            if (style.getType() == Style.StyleType.PARAGRAPH) {
                result.add(style);
            }
        }
        return result;
    }
    
    public Style getDefaultTextStyle() {
        return defaultTextStyle;
    }
    
    public Style getDefaultParagraphStyle() {
        return defaultParagraphStyle;
    }
    
    public void setDefaultTextStyle(Style style) {
        this.defaultTextStyle = style;
    }
    
    public void setDefaultParagraphStyle(Style style) {
        this.defaultParagraphStyle = style;
    }
    
    /**
     * Clear all styles from the registry.
     */
    public void clear() {
        styles.clear();
    }
    
    @Override
    public String toString() {
        return String.format("StyleRegistry[styles=%d]", styles.size());
    }
}
