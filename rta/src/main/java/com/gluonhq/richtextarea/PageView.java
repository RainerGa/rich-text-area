/*
 * Copyright (c) 2022, 2025, Gluon
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
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.gluonhq.richtextarea;

import com.gluonhq.richtextarea.model.HeaderFooter;
import com.gluonhq.richtextarea.model.PageFormat;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A visual representation of a single page in a document.
 * <p>
 * The page is drawn at its natural (un-scaled) size in points and scaled
 * visually through {@link #setZoom(double)}. This keeps layout bounds,
 * hit testing and the scroll area consistent with the zoom factor.
 * </p>
 *
 * <p>
 * The page consists of a paper background (with drop shadow), a header area,
 * a content area and a footer area. The {@link DocumentView} is responsible
 * for placing the editable content (paragraphs) inside the content area.
 * </p>
 */
public class PageView extends StackPane {

    // Page properties
    private final ObjectProperty<PageFormat> pageFormatProperty = new SimpleObjectProperty<>(this, "pageFormat");
    private final IntegerProperty pageNumberProperty = new SimpleIntegerProperty(this, "pageNumber", 1);
    private final IntegerProperty totalPagesProperty = new SimpleIntegerProperty(this, "totalPages", 1);
    private final DoubleProperty zoomProperty = new SimpleDoubleProperty(this, "zoom", 1.0);

    // Visual components
    private final StackPane pagePaper = new StackPane();
    private final Pane pageContent = new Pane();
    private final VBox headerArea = new VBox();
    private final Pane contentArea = new Pane();
    private final VBox footerArea = new VBox();
    private final Rectangle pageBackground = new Rectangle();
    private final Label pageNumberLabel = new Label();
    private List<HeaderFooter> renderedHeadersFooters;
    private PageFormat renderedHeaderFooterFormat;
    private double renderedHeaderFooterZoom = Double.NaN;
    private int renderedHeaderFooterPageNumber = -1;

    private static final double PAGE_SHADOW_RADIUS = 20;
    private static final double PAGE_SHADOW_OPACITY = 0.3;
    private static final Color PAGE_BACKGROUND_COLOR = Color.WHITE;
    private static final Color PAGE_SHADOW_COLOR = Color.rgb(0, 0, 0, PAGE_SHADOW_OPACITY);

    public Pane getContentArea() {
        return contentArea;
    }

    public PageView() {
        initialize();
    }

    private void initialize() {
        getStyleClass().add("page-view");

        // Page paper (white background with shadow), sized at natural size.
        pagePaper.setBackground(new Background(new BackgroundFill(PAGE_BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        pagePaper.setEffect(new DropShadow(PAGE_SHADOW_RADIUS, PAGE_SHADOW_COLOR));

        // Page background rectangle (clips content to the paper size)
        pageBackground.setFill(PAGE_BACKGROUND_COLOR);
        pagePaper.getChildren().add(pageBackground);

        pageContent.setBackground(Background.EMPTY);
        pagePaper.getChildren().add(pageContent);

        headerArea.setAlignment(Pos.CENTER);
        headerArea.setFillWidth(true);
        headerArea.setManaged(false);

        contentArea.setBackground(Background.EMPTY);
        contentArea.setManaged(false);

        footerArea.setAlignment(Pos.CENTER);
        footerArea.setFillWidth(true);
        footerArea.setManaged(false);

        // Page number label
        pageNumberLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        pageNumberLabel.setAlignment(Pos.CENTER);
        pageNumberLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pageNumberLabel, Priority.ALWAYS);
        pageNumberLabel.textProperty().bind(
                pageNumberProperty.asString("Page %d").concat(" / ").concat(totalPagesProperty.asString())
        );

        HBox footerContent = new HBox(pageNumberLabel);
        footerContent.setAlignment(Pos.CENTER);
        footerContent.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pageNumberLabel, Priority.ALWAYS);
        footerArea.getChildren().add(footerContent);

        pageContent.getChildren().addAll(headerArea, contentArea, footerArea);
        getChildren().add(pagePaper);

        zoomProperty.addListener((obs, oldVal, newVal) -> applyZoom(newVal.doubleValue()));
        pageFormatProperty.addListener((obs, oldVal, newVal) -> updatePageSize());
        pageNumberProperty.addListener((obs, oldVal, newVal) -> updatePageNumberLabel());
    }

    public void setPageFormat(PageFormat format) {
        pageFormatProperty.set(format);
    }

    public PageFormat getPageFormat() {
        return pageFormatProperty.get();
    }

    public ObjectProperty<PageFormat> pageFormatProperty() {
        return pageFormatProperty;
    }

    public void setPageNumber(int pageNumber) {
        pageNumberProperty.set(pageNumber);
    }

    public int getPageNumber() {
        return pageNumberProperty.get();
    }

    public IntegerProperty pageNumberProperty() {
        return pageNumberProperty;
    }

    public void setTotalPages(int totalPages) {
        totalPagesProperty.set(totalPages);
    }

    public int getTotalPages() {
        return totalPagesProperty.get();
    }

    public IntegerProperty totalPagesProperty() {
        return totalPagesProperty;
    }

    public void setZoom(double zoom) {
        zoomProperty.set(Math.max(0.1, Math.min(5.0, zoom)));
    }

    public double getZoom() {
        return zoomProperty.get();
    }

    public DoubleProperty zoomProperty() {
        return zoomProperty;
    }

    /**
     * Returns the un-scaled page width in points.
     */
    public double getPageWidth() {
        PageFormat format = getPageFormat();
        return format != null ? format.getWidth() : 0;
    }

    /**
     * Returns the un-scaled page height in points.
     */
    public double getPageHeight() {
        PageFormat format = getPageFormat();
        return format != null ? format.getHeight() : 0;
    }

    /**
     * Returns the un-scaled content width (page width minus left/right margins).
     */
    public double getContentWidth() {
        PageFormat format = getPageFormat();
        return format != null ? format.getContentWidth() : 0;
    }

    /**
     * Returns the un-scaled content height (page height minus top/bottom margins).
     */
    public double getContentHeight() {
        PageFormat format = getPageFormat();
        return format != null ? format.getContentHeight() : 0;
    }

    /**
     * Returns the page margins.
     */
    public Insets getMargins() {
        PageFormat format = getPageFormat();
        return format != null ? format.getMargins() : new Insets(0);
    }

    /**
     * Applies zoom by recomputing all sizes at the zoomed (visual) size.
     */
    private void applyZoom(double zoom) {
        updatePageSize();
    }

    /**
     * Updates the page size at the zoomed (visual) size.
     */
    private void updatePageSize() {
        PageFormat format = getPageFormat();
        if (format == null) {
            return;
        }

        double zoom = getZoom();
        double pageWidth = format.getWidth() * zoom;
        double pageHeight = format.getHeight() * zoom;
        Insets margins = format.getMargins();
        double marginLeft = margins.getLeft() * zoom;
        double marginTop = margins.getTop() * zoom;
        double marginBottom = margins.getBottom() * zoom;

        // Page paper size at visual (zoomed) size
        pagePaper.setPrefSize(pageWidth, pageHeight);
        pagePaper.setMinSize(pageWidth, pageHeight);
        pagePaper.setMaxSize(pageWidth, pageHeight);
        pageBackground.setWidth(pageWidth);
        pageBackground.setHeight(pageHeight);

        double contentWidth = format.getContentWidth() * zoom;
        double contentHeight = format.getContentHeight() * zoom;

        pageContent.setPrefSize(pageWidth, pageHeight);
        pageContent.setMinSize(pageWidth, pageHeight);
        pageContent.setMaxSize(pageWidth, pageHeight);
        pageContent.resizeRelocate(0, 0, pageWidth, pageHeight);

        headerArea.resizeRelocate(marginLeft, 0, contentWidth, marginTop);
        contentArea.setPrefSize(contentWidth, contentHeight);
        contentArea.setMinSize(contentWidth, contentHeight);
        contentArea.setMaxSize(contentWidth, contentHeight);
        contentArea.resizeRelocate(marginLeft, marginTop, contentWidth, contentHeight);
        footerArea.resizeRelocate(marginLeft, pageHeight - marginBottom, contentWidth, marginBottom);
    }

    private void updatePageNumberLabel() {
        // Bound via property binding
    }

    /**
     * Renders the headers and footers for this page.
     */
    public void renderHeadersFooters(List<HeaderFooter> headersFooters, double zoom) {
        List<HeaderFooter> currentHeadersFooters = headersFooters == null
                ? List.of() : List.copyOf(headersFooters);
        if (Objects.equals(renderedHeadersFooters, currentHeadersFooters)
                && Objects.equals(renderedHeaderFooterFormat, getPageFormat())
                && Double.compare(renderedHeaderFooterZoom, zoom) == 0
                && renderedHeaderFooterPageNumber == getPageNumber()) {
            return;
        }
        renderedHeadersFooters = currentHeadersFooters;
        renderedHeaderFooterFormat = getPageFormat();
        renderedHeaderFooterZoom = zoom;
        renderedHeaderFooterPageNumber = getPageNumber();

        headerArea.getChildren().clear();
        footerArea.getChildren().clear();

        double contentWidth = getContentWidth() * zoom;

        // Re-add the page number label to the footer first
        HBox footerContent = new HBox(pageNumberLabel);
        footerContent.setAlignment(Pos.CENTER);
        footerContent.setFillHeight(true);
        footerContent.setMaxWidth(Double.MAX_VALUE);
        footerContent.setPrefWidth(contentWidth);
        HBox.setHgrow(pageNumberLabel, Priority.ALWAYS);
        footerArea.getChildren().add(footerContent);

        if (currentHeadersFooters.isEmpty()) {
            return;
        }

        int pageNum = getPageNumber();
        int totalPages = getTotalPages();

        for (HeaderFooter hf : currentHeadersFooters) {
            HeaderFooter.Scope scope = hf.getScope();
            boolean show = false;
            switch (scope) {
                case ALL_PAGES:
                    show = true;
                    break;
                case FIRST_PAGE:
                    show = (pageNum == 1);
                    break;
                case LEFT_PAGES:
                    show = (pageNum % 2 == 0);
                    break;
                case RIGHT_PAGES:
                    show = (pageNum % 2 == 1);
                    break;
                default:
                    show = true;
            }
            if (!show || hf.getType() == null) {
                continue;
            }

            TextFlow textFlow = new TextFlow();
            textFlow.setPrefWidth(contentWidth);
            textFlow.setTextAlignment(TextAlignment.CENTER);
            textFlow.setLineSpacing(0);

            Text text = new Text(hf.getContent());
            text.setFont(Font.font(10 * zoom));
            text.setFill(Color.GRAY);
            textFlow.getChildren().add(text);

            double hfHeight = hf.getHeight() * zoom;
            textFlow.setMaxHeight(hfHeight);
            textFlow.setPrefHeight(hfHeight);

            VBox container = new VBox(textFlow);
            container.setAlignment(Pos.CENTER);
            container.setPrefWidth(contentWidth);
            container.setMinWidth(contentWidth);
            container.setMaxWidth(contentWidth);

            if (hf.getType() == HeaderFooter.Type.HEADER) {
                headerArea.getChildren().add(container);
            } else if (hf.getType() == HeaderFooter.Type.FOOTER) {
                footerArea.getChildren().add(container);
            }
        }
    }

    /**
     * Gets the paper bounds in the parent coordinate space, excluding the drop shadow.
     */
    public Bounds getPageBounds() {
        return pagePaper.localToParent(pagePaper.getLayoutBounds());
    }

    /**
     * Checks if a point (in parent coordinate space) is within this page's paper.
     */
    public boolean containsPoint(Point2D point) {
        return getPageBounds().contains(point);
    }

    /**
     * Gets the content area bounds in the parent coordinate space.
     */
    public Bounds getContentAreaBounds() {
        return contentArea.getBoundsInParent();
    }

    Bounds getFooterAreaBounds() {
        return footerArea.getBoundsInParent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageView)) return false;
        PageView that = (PageView) o;
        return Objects.equals(getPageFormat(), that.getPageFormat())
                && getPageNumber() == that.getPageNumber();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPageFormat(), getPageNumber());
    }
}
