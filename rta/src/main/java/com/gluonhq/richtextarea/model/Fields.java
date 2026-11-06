package com.gluonhq.richtextarea.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Factory class for creating different types of fields.
 */
public class Fields {
    
    /**
     * A field that displays the current page number.
     */
    public static class PageNumberField extends Field {
        private static int lastPageCount = 0;
        
        public PageNumberField(String name) {
            super(name, FieldType.PAGE_NUMBER);
        }
        
        @Override
        public String getValue() {
            // In a real implementation, this would get the actual page number
            // For now, return a placeholder
            return "1";
        }
        
        public void setPageCount(int pageCount) {
            lastPageCount = pageCount;
        }
    }
    
    /**
     * A field that displays the total number of pages.
     */
    public static class PageCountField extends Field {
        public PageCountField(String name) {
            super(name, FieldType.PAGE_COUNT);
        }
        
        @Override
        public String getValue() {
            // In a real implementation, this would get the actual page count
            return "99";
        }
    }
    
    /**
     * A field that displays the current date.
     */
    public static class DateField extends Field {
        private final DateTimeFormatter formatter;
        
        public DateField(String name, String pattern) {
            super(name, FieldType.DATE);
            this.formatter = DateTimeFormatter.ofPattern(pattern);
        }
        
        public DateField(String name) {
            this(name, "dd/MM/yyyy");
        }
        
        @Override
        public String getValue() {
            return LocalDate.now().format(formatter);
        }
        
        @Override
        public void update() {
            // Date changes every day, so we need to update
        }
    }
    
    /**
     * A field that displays the current time.
     */
    public static class TimeField extends Field {
        private final DateTimeFormatter formatter;
        
        public TimeField(String name, String pattern) {
            super(name, FieldType.TIME);
            this.formatter = DateTimeFormatter.ofPattern(pattern);
        }
        
        public TimeField(String name) {
            this(name, "HH:mm:ss");
        }
        
        @Override
        public String getValue() {
            return LocalTime.now().format(formatter);
        }
        
        @Override
        public void update() {
            // Time changes frequently
        }
    }
    
    /**
     * A field that displays the current date and time.
     */
    public static class DateTimeField extends Field {
        private final DateTimeFormatter formatter;
        
        public DateTimeField(String name, String pattern) {
            super(name, FieldType.DATE_TIME);
            this.formatter = DateTimeFormatter.ofPattern(pattern);
        }
        
        public DateTimeField(String name) {
            this(name, "dd/MM/yyyy HH:mm:ss");
        }
        
        @Override
        public String getValue() {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " " + 
                   LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        
        @Override
        public void update() {
            // Date and time change frequently
        }
    }
    
    /**
     * A field that displays the document title.
     */
    public static class DocumentTitleField extends Field {
        private String title;
        
        public DocumentTitleField(String name) {
            super(name, FieldType.DOCUMENT_TITLE);
            this.title = "Untitled";
        }
        
        @Override
        public String getValue() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
    }
    
    /**
     * A field that displays the document author.
     */
    public static class DocumentAuthorField extends Field {
        private String author;
        
        public DocumentAuthorField(String name) {
            super(name, FieldType.DOCUMENT_AUTHOR);
            this.author = "Unknown";
        }
        
        @Override
        public String getValue() {
            return author;
        }
        
        public void setAuthor(String author) {
            this.author = author;
        }
    }
    
    /**
     * A field that displays the document subject.
     */
    public static class DocumentSubjectField extends Field {
        private String subject;
        
        public DocumentSubjectField(String name) {
            super(name, FieldType.DOCUMENT_SUBJECT);
            this.subject = "";
        }
        
        @Override
        public String getValue() {
            return subject;
        }
        
        public void setSubject(String subject) {
            this.subject = subject;
        }
    }
    
    /**
     * A variable field with a custom value.
     */
    public static class VariableField extends Field {
        private String value;
        
        public VariableField(String name) {
            super(name, FieldType.VARIABLE);
            this.value = "";
        }
        
        public VariableField(String name, String initialValue) {
            super(name, FieldType.VARIABLE);
            this.value = initialValue;
        }
        
        @Override
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
    
    /**
     * A field that displays the filename.
     */
    public static class FileNameField extends Field {
        public FileNameField() {
            super("Filename", FieldType.FILENAME);
        }
        
        @Override
        public String getValue() {
            return "Document";
        }
    }
    
    /**
     * A field that displays the user name.
     */
    public static class UserNameField extends Field {
        public UserNameField() {
            super("User", FieldType.USER_NAME);
        }
        
        @Override
        public String getValue() {
            return System.getProperty("user.name", "Unknown");
        }
    }
}
