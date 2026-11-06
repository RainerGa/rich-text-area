package com.gluonhq.richtextarea.model;

/**
 * Represents header or footer content for a document.
 * Can be positioned on all pages, first page only, or left/right pages.
 */
public class HeaderFooter {
    
    public enum Type {
        HEADER,
        FOOTER
    }
    
    public enum Scope {
        ALL_PAGES,
        FIRST_PAGE,
        LEFT_PAGES,
        RIGHT_PAGES
    }
    
    private final Type type;
    private final Scope scope;
    private String content;
    private double height;
    private double distanceFromEdge;
    
    private HeaderFooter(Builder builder) {
        this.type = builder.type;
        this.scope = builder.scope;
        this.content = builder.content;
        this.height = builder.height;
        this.distanceFromEdge = builder.distanceFromEdge;
    }
    
    public Type getType() {
        return type;
    }
    
    public Scope getScope() {
        return scope;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public double getHeight() {
        return height;
    }
    
    public void setHeight(double height) {
        this.height = height;
    }
    
    public double getDistanceFromEdge() {
        return distanceFromEdge;
    }
    
    public void setDistanceFromEdge(double distanceFromEdge) {
        this.distanceFromEdge = distanceFromEdge;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Type type = Type.HEADER;
        private Scope scope = Scope.ALL_PAGES;
        private String content = "";
        private double height = 36; // 0.5 inch
        private double distanceFromEdge = 18; // 0.25 inch
        
        public Builder type(Type type) {
            this.type = type;
            return this;
        }
        
        public Builder scope(Scope scope) {
            this.scope = scope;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder height(double height) {
            this.height = height;
            return this;
        }
        
        public Builder distanceFromEdge(double distanceFromEdge) {
            this.distanceFromEdge = distanceFromEdge;
            return this;
        }
        
        public HeaderFooter build() {
            return new HeaderFooter(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("HeaderFooter[type=%s, scope=%s, content='%s']", 
                type, scope, content);
    }
}
