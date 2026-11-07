package com.gluonhq.richtextarea.model;

import javafx.geometry.Insets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Represents the page format settings for a document.
 * Includes page size, orientation, and margins.
 */
public class PageFormat implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum PageSize {
        A0(841 * 72 / 25.4, 1189 * 72 / 25.4),    // mm to points
        A1(594 * 72 / 25.4, 841 * 72 / 25.4),
        A2(420 * 72 / 25.4, 594 * 72 / 25.4),
        A3(297 * 72 / 25.4, 420 * 72 / 25.4),
        A4(210 * 72 / 25.4, 297 * 72 / 25.4),
        A5(148 * 72 / 25.4, 210 * 72 / 25.4),
        A6(105 * 72 / 25.4, 148 * 72 / 25.4),
        LETTER(612, 792),      // 8.5 x 11 inches
        LEGAL(612, 1008),      // 8.5 x 14 inches
        EXECUTIVE(540, 720),   // 7.25 x 10.5 inches
        CUSTOM(0, 0);
        
        private final double width;
        private final double height;
        
        PageSize(double width, double height) {
            this.width = width;
            this.height = height;
        }
        
        public double getWidth() {
            return width;
        }
        
        public double getHeight() {
            return height;
        }
    }
    
    public enum Orientation {
        PORTRAIT,
        LANDSCAPE
    }
    
    private PageSize pageSize;
    private Orientation orientation;
    private transient Insets margins;
    
    private PageFormat(Builder builder) {
        this.pageSize = builder.pageSize;
        this.orientation = builder.orientation;
        this.margins = builder.margins;
    }
    
    public PageSize getPageSize() {
        return pageSize;
    }
    
    public Orientation getOrientation() {
        return orientation;
    }
    
    public Insets getMargins() {
        return margins;
    }
    
    public double getWidth() {
        double w = pageSize.getWidth();
        return orientation == Orientation.LANDSCAPE ? pageSize.getHeight() : w;
    }
    
    public double getHeight() {
        double h = pageSize.getHeight();
        return orientation == Orientation.LANDSCAPE ? pageSize.getWidth() : h;
    }
    
    public double getContentWidth() {
        return getWidth() - margins.getLeft() - margins.getRight();
    }
    
    public double getContentHeight() {
        return getHeight() - margins.getTop() - margins.getBottom();
    }
    
    public double getEffectiveHeight() {
        return getHeight();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private PageSize pageSize = PageSize.A4;
        private Orientation orientation = Orientation.PORTRAIT;
        private Insets margins = new Insets(72, 72, 72, 72); // 1 inch
        
        public Builder pageSize(PageSize pageSize) {
            this.pageSize = pageSize;
            return this;
        }
        
        public Builder orientation(Orientation orientation) {
            this.orientation = orientation;
            return this;
        }
        
        public Builder margins(Insets margins) {
            this.margins = margins;
            return this;
        }
        
        public PageFormat build() {
            return new PageFormat(this);
        }
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeDouble(margins.getTop());
        out.writeDouble(margins.getRight());
        out.writeDouble(margins.getBottom());
        out.writeDouble(margins.getLeft());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        double top = in.readDouble();
        double right = in.readDouble();
        double bottom = in.readDouble();
        double left = in.readDouble();
        margins = new Insets(top, right, bottom, left);
    }

    @Override
    public String toString() {
        return String.format("PageFormat[size=%s, orientation=%s, margins=%s]", 
                pageSize, orientation, margins);
    }
}
