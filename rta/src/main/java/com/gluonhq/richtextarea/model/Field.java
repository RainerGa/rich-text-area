package com.gluonhq.richtextarea.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Base class for all field types in a document.
 * Fields are dynamic content that can be updated automatically.
 */
public abstract class Field {
    
    private final String name;
    private final FieldType type;
    
    protected Field(String name, FieldType type) {
        this.name = name;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public FieldType getType() {
        return type;
    }
    
    /**
     * Get the current value of the field as a string.
     */
    public abstract String getValue();
    
    /**
     * Update the field value if it's dynamic.
     */
    public void update() {
        // Default implementation does nothing
        // Override for dynamic fields like date/time
    }
    
    @Override
    public String toString() {
        return String.format("Field[name='%s', type=%s, value='%s']", name, type, getValue());
    }
    
    // Static field types
    public enum FieldType {
        PAGE_NUMBER,
        PAGE_COUNT,
        DATE,
        TIME,
        DATE_TIME,
        DOCUMENT_TITLE,
        DOCUMENT_AUTHOR,
        DOCUMENT_SUBJECT,
        FILENAME,
        USER_NAME,
        VARIABLE
    }

    /**
     * Context interface for field evaluation, providing document properties
     * used by dynamic fields like page numbers, dates, and document metadata.
     */
    public interface FieldContext {
        int getCurrentPageNumber();
        int getPageCount();
        LocalDate getCurrentDate();
        LocalTime getCurrentTime();
        String getDocumentTitle();
        String getDocumentSubject();
        String getDocumentAuthor();
        String getFileName();
        String getTemplateName();
        String getUserName();
        String getVariable(String name);
    }
}
