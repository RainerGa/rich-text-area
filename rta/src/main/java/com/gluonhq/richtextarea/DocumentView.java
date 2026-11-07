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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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

import com.gluonhq.richtextarea.action.Action;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.HeaderFooter;
import com.gluonhq.richtextarea.model.ImageDecoration;
import com.gluonhq.richtextarea.model.PageFormat;
import com.gluonhq.richtextarea.model.Paragraph;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.TextBuffer;
import com.gluonhq.richtextarea.model.TextDecoration;
import com.gluonhq.richtextarea.viewmodel.RichTextAreaViewModel;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A real, editable multi-page document view like Microsoft Word or LibreOffice Writer.
 * <p>
 * Architecture:
 * <ul>
 *   <li>A single {@link RichTextArea} (the {@code editor}) is the one and only document
 *       buffer. It stays in the scene (hidden) and keeps the focus so that all keyboard
 *       input (text, Enter, Backspace, Delete, arrow keys, Ctrl+A/C/V/X, undo/redo) is
 *       handled by the existing RichTextArea editing logic.</li>
 *   <li>The document is laid out into real paper pages ({@link PageView}) stacked
 *       vertically inside a scrollable canvas. Paragraphs are measured with the real
 *       TextFlow layout engine (no height estimates) and split at line boundaries so a
 *       single paragraph can flow across page boundaries.</li>
 *   <li>Each page renders its paragraphs with live {@link ParagraphTile}s bound to the
 *       shared view model, so the caret, selection, hit testing and editing all work
 *       directly on the displayed pages.</li>
 * </ol>
 */
public class DocumentView extends Pane {

    private record MeasuredLine(int startOffset, int endOffset, double height) {
    }

    private record CachedParagraphLayout(ParagraphDecoration decoration, int startOffset, int length,
                                         long contentWidth, long zoom, List<MeasuredLine> lines) {
    }

    private static final class ParagraphIdentityKey {
        private final Paragraph paragraph;

        private ParagraphIdentityKey(Paragraph paragraph) {
            this.paragraph = paragraph;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ParagraphIdentityKey key && paragraph == key.paragraph;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(paragraph);
        }
    }

    // ------------------------------------------------------------------ properties

    private final ObjectProperty<Document> documentProperty = new SimpleObjectProperty<>(this, "document");
    private final ObjectProperty<PageFormat> pageFormatProperty = new SimpleObjectProperty<>(this, "pageFormat");
    private final ObjectProperty<java.util.List<HeaderFooter>> headersFootersProperty =
            new SimpleObjectProperty<>(this, "headersFooters", java.util.List.of());
    private final DoubleProperty zoomProperty = new SimpleDoubleProperty(this, "zoom", 1.0);
    private final DoubleProperty pageGapProperty = new SimpleDoubleProperty(this, "pageGap", 20.0);
    private final IntegerProperty totalPagesProperty = new SimpleIntegerProperty(this, "totalPages", 1);

    // ------------------------------------------------------------------ nodes

    private final Rectangle backgroundRect = new Rectangle();
    private final ScrollPane scrollPane = new ScrollPane();
    private final Pane documentCanvas = new Pane();

    // The single, shared document buffer / editor. It stays in the scene (hidden) and
    // owns the focus; everything the user sees (caret, selection, text) is rendered by
    // the live ParagraphTiles on the pages.
    private RichTextArea editor;
    private RichTextAreaSkin skin;
    private RichTextAreaViewModel viewModel;
    private TextBuffer observedTextBuffer;
    private static final int MAX_CACHED_PARAGRAPH_LAYOUTS = 1024;
    private final Map<ParagraphIdentityKey, CachedParagraphLayout> paragraphLayoutCache =
            new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<ParagraphIdentityKey, CachedParagraphLayout> eldest) {
                    return size() > MAX_CACHED_PARAGRAPH_LAYOUTS;
                }
            };
    private final ChangeListener<Scene> editorSceneListener = (obs, oldScene, newScene) -> {
        if (newScene != null && skin == null && editor != null) {
            editor.applyCss();
            editor.layout();
            attachSkin();
        }
    };

    private final Consumer<TextBuffer.Event> textBufferChangeListener =
            event -> scheduleRelayout(getEventStart(event));
    private final ChangeListener<TextBuffer> textBufferListener = (obs, oldBuffer, newBuffer) -> {
        if (oldBuffer != null) {
            oldBuffer.removeChangeListener(textBufferChangeListener);
        }
        observedTextBuffer = newBuffer;
        if (newBuffer != null) {
            newBuffer.addChangeListener(textBufferChangeListener);
        }
        paragraphLayoutCache.clear();
        scheduleRelayout();
    };
    private final ChangeListener<Document> viewModelDocumentListener = (obs, ov, nv) -> {
        if (nv != null) {
            paragraphLayoutCache.clear();
            scheduleRelayout();
        }
    };
    private final ChangeListener<Number> caretPositionListener =
            (obs, oldPosition, newPosition) -> Platform.runLater(this::scrollCaretIntoView);
    private final EventHandler<KeyEvent> keyPressedFilter = event -> forwardKeyEvent(event, true);
    private final EventHandler<KeyEvent> keyTypedFilter = event -> forwardKeyEvent(event, false);

    private final List<PageView> pages = new ArrayList<>();
    private final AtomicBoolean layoutInProgress = new AtomicBoolean(false);
    private final AtomicBoolean relayoutScheduled = new AtomicBoolean(false);
    private final AtomicBoolean relayoutContentChanged = new AtomicBoolean(false);
    private final AtomicInteger relayoutDirtyStart = new AtomicInteger(Integer.MAX_VALUE);

    public DocumentView() {
        initialize();
        // Default to an internal editor so the control is usable stand-alone.
        setEditor(new RichTextArea());
    }

    // ---------------------------------------------------------------- public API

    public void setEditor(RichTextArea editor) {
        if (this.editor == editor) {
            return;
        }
        if (this.editor != null) {
            this.editor.sceneProperty().removeListener(editorSceneListener);
            getChildren().remove(this.editor);
        }
        if (skin != null) {
            skin.restoreTextFlowPrefWidthBinding();
            skin.setPageViewMode(false);
        }
        if (viewModel != null) {
            viewModel.textBufferProperty().removeListener(textBufferListener);
            viewModel.documentProperty().removeListener(viewModelDocumentListener);
            viewModel.caretPositionProperty().removeListener(caretPositionListener);
            if (observedTextBuffer != null) {
                observedTextBuffer.removeChangeListener(textBufferChangeListener);
            }
        }
        skin = null;
        viewModel = null;
        observedTextBuffer = null;
        paragraphLayoutCache.clear();
        this.editor = editor;
        if (this.editor != null) {
            this.editor.sceneProperty().addListener(editorSceneListener);
            this.editor.setManaged(false);
            this.editor.setOpacity(0);
            this.editor.setMouseTransparent(true);
            this.editor.setFocusTraversable(true);
            getChildren().add(this.editor);
            // Force the RichTextArea to instantiate its skin. A hidden editor never
            // receives a CSS/layout pass, so the skin is created lazily and must be
            // triggered explicitly before we can access the view model.
            editor.applyCss();
            editor.layout();
            attachSkin();
            // The editor is the focus owner so that all keyboard input reaches the
            // RichTextArea editing logic. The visible caret is rendered by the tiles.
            editor.requestFocus();
        }
        relayout();
    }

    public RichTextArea getEditor() {
        return editor;
    }

    public void setDocument(Document document) {
        documentProperty.set(document);
        if (editor != null) {
            Action open = editor.getActionFactory().open(document);
            open.execute(new ActionEvent());
            // open() applies the document asynchronously on the FX thread. Layout once it has
            // been applied so the skin and paragraph list are already populated.
            Platform.runLater(this::relayout);
        }
    }

    public Document getDocument() {
        return documentProperty.get();
    }

    public ObjectProperty<Document> documentProperty() {
        return documentProperty;
    }

    public void setPageFormat(PageFormat format) {
        pageFormatProperty.set(format);
        if (editor != null) {
            editor.setPageFormat(format);
        }
    }

    public PageFormat getPageFormat() {
        return pageFormatProperty.get();
    }

    public ObjectProperty<PageFormat> pageFormatProperty() {
        return pageFormatProperty;
    }

    public void setHeadersFooters(java.util.List<HeaderFooter> headersFooters) {
        headersFootersProperty.set(headersFooters != null ? headersFooters : java.util.List.of());
    }

    public java.util.List<HeaderFooter> getHeadersFooters() {
        return headersFootersProperty.get();
    }

    public ObjectProperty<java.util.List<HeaderFooter>> headersFootersProperty() {
        return headersFootersProperty;
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

    public void setPageGap(double pageGap) {
        pageGapProperty.set(Math.max(0, pageGap));
    }

    public double getPageGap() {
        return pageGapProperty.get();
    }

    public DoubleProperty pageGapProperty() {
        return pageGapProperty;
    }

    public int getPageCount() {
        return totalPagesProperty.get();
    }

    public IntegerProperty totalPagesProperty() {
        return totalPagesProperty;
    }

    RichTextAreaViewModel getViewModel() {
        return viewModel;
    }

    List<PageView> getPages() {
        return pages;
    }

    ScrollPane getScrollPane() {
        return scrollPane;
    }

    List<Node> getTileNodes(PageView page) {
        return page.getContentArea().getChildren();
    }

    /**
     * Forces a full re-layout of the pages from the current document state.
     */
    public void relayout() {
        relayoutFrom(0, false);
    }

    private void relayoutFrom(int dirtyStart, boolean contentChanged) {
        if (layoutInProgress.getAndSet(true)) {
            return;
        }
        try {
            ensureEditor();
            doRelayout(dirtyStart, contentChanged);
        } finally {
            layoutInProgress.set(false);
        }
    }

    // ---------------------------------------------------------------- initialization

    private void initialize() {
        getStyleClass().add("document-view");

        backgroundRect.setFill(Color.rgb(191, 191, 191));
        backgroundRect.setManaged(false);
        getChildren().add(backgroundRect);

        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(documentCanvas);
        getChildren().add(scrollPane);

        documentCanvas.setManaged(false);
        documentCanvas.setBackground(Background.EMPTY);

        // Clicks on the canvas background (margins, gaps, empty page area) place the
        // caret at the end of the document and keep the editor focused.
        documentCanvas.setOnMousePressed(e -> {
            Node target = (Node) e.getTarget();
            boolean overEditorContent = false;
            Node n = target;
            while (n != null && n != documentCanvas) {
                if (n instanceof ParagraphTile) {
                    overEditorContent = true;
                    break;
                }
                n = n.getParent();
            }
            if (!overEditorContent && viewModel != null) {
                viewModel.setCaretPosition(viewModel.getTextLength());
                editor.requestFocus();
                e.consume();
            }
        });

        scrollPane.viewportBoundsProperty().addListener((obs, ov, nv) -> repositionPages());

        zoomProperty.addListener((obs, ov, nv) -> scheduleRelayout());
        pageFormatProperty.addListener((obs, ov, nv) -> {
            paragraphLayoutCache.clear();
            scheduleRelayout();
        });
        headersFootersProperty.addListener((obs, ov, nv) -> scheduleRelayout());
        pageGapProperty.addListener((obs, ov, nv) -> scheduleRelayout());
        sceneProperty().addListener((obs, oldScene, newScene) -> attachKeyEventFilters(oldScene, newScene));
    }

    private void scheduleRelayout() {
        scheduleRelayout(0, false);
    }

    private void scheduleRelayout(int dirtyStart) {
        scheduleRelayout(dirtyStart, true);
    }

    private void scheduleRelayout(int dirtyStart, boolean contentChanged) {
        relayoutDirtyStart.accumulateAndGet(Math.max(0, dirtyStart), Math::min);
        if (contentChanged) {
            relayoutContentChanged.set(true);
        }
        if (relayoutScheduled.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                int earliestDirtyStart = relayoutDirtyStart.getAndSet(Integer.MAX_VALUE);
                boolean layoutContentChanged = relayoutContentChanged.getAndSet(false);
                relayoutScheduled.set(false);
                relayoutFrom(earliestDirtyStart == Integer.MAX_VALUE ? 0 : earliestDirtyStart,
                        layoutContentChanged);
            });
        }
    }

    private int getEventStart(TextBuffer.Event event) {
        if (event instanceof TextBuffer.InsertEvent insertEvent) {
            return insertEvent.getPosition();
        }
        if (event instanceof TextBuffer.DeleteEvent deleteEvent) {
            return deleteEvent.getPosition();
        }
        if (event instanceof TextBuffer.DecorateEvent decorateEvent) {
            return decorateEvent.getStart();
        }
        return 0;
    }

    private void attachKeyEventFilters(Scene oldScene, Scene newScene) {
        if (oldScene != null) {
            oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedFilter);
            oldScene.removeEventFilter(KeyEvent.KEY_TYPED, keyTypedFilter);
        }
        if (newScene != null) {
            newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedFilter);
            newScene.addEventFilter(KeyEvent.KEY_TYPED, keyTypedFilter);
        }
    }

    private void forwardKeyEvent(KeyEvent event, boolean pressed) {
        if (editor == null || !editor.isFocused()) {
            return;
        }
        if (!pressed && isPrintableCharacter(event)) {
            editor.getActionFactory().insertText(event.getCharacter()).execute(new ActionEvent());
            event.consume();
            return;
        }
        EventHandler<? super KeyEvent> handler = pressed ? editor.getOnKeyPressed() : editor.getOnKeyTyped();
        if (handler != null) {
            handler.handle(event);
            event.consume();
        }
    }

    private boolean isPrintableCharacter(KeyEvent event) {
        String character = event.getCharacter();
        if (character.isEmpty() || KeyEvent.CHAR_UNDEFINED.equals(character)
                || event.isControlDown() || event.isMetaDown()) {
            return false;
        }
        char first = character.charAt(0);
        Character.UnicodeBlock block = Character.UnicodeBlock.of(first);
        return (first == '\n' || first == '\t' || !Character.isISOControl(first))
                && block != null && block != Character.UnicodeBlock.SPECIALS;
    }

    private void ensureEditor() {
        if (editor == null) {
            setEditor(new RichTextArea());
        }
        if (skin == null && editor != null) {
            attachSkin();
        }
    }

    private void attachSkin() {
        if (editor == null) {
            return;
        }
        RichTextAreaSkin newSkin = (RichTextAreaSkin) editor.getSkin();
        if (newSkin == null) {
            // Skin is created lazily; retry once the control has been laid out.
            return;
        }
        RichTextAreaViewModel newViewModel = newSkin.getViewModel();
        if (viewModel != newViewModel) {
            if (viewModel != null) {
                viewModel.textBufferProperty().removeListener(textBufferListener);
                viewModel.documentProperty().removeListener(viewModelDocumentListener);
                viewModel.caretPositionProperty().removeListener(caretPositionListener);
                if (observedTextBuffer != null) {
                    observedTextBuffer.removeChangeListener(textBufferChangeListener);
                }
            }
            viewModel = newViewModel;
            observedTextBuffer = viewModel.getTextBuffer();
            observedTextBuffer.addChangeListener(textBufferChangeListener);
            viewModel.textBufferProperty().addListener(textBufferListener);
            viewModel.documentProperty().addListener(viewModelDocumentListener);
            viewModel.caretPositionProperty().addListener(caretPositionListener);
        }
        skin = newSkin;
        skin.setPageViewMode(true);
    }

    // ---------------------------------------------------------------- layout

    private void doRelayout(int dirtyStart, boolean contentChanged) {
        PageFormat format = getPageFormat();
        if (format == null || viewModel == null || skin == null) {
            clearPages();
            setTotalPages(0);
            repositionPages();
            return;
        }

        double zoom = getZoom();
        double contentWidth = format.getContentWidth() * zoom;
        double contentHeight = format.getContentHeight() * zoom;

        // Paragraphs sorted by document position (real measurement, no estimates)
        List<Paragraph> paragraphs = skin.getParagraphs();
        List<List<PageFragment>> pagesData = computePages(paragraphs, contentWidth, contentHeight,
                dirtyStart, contentChanged);

        renderPages(pagesData, contentWidth, contentHeight, zoom, dirtyStart);
        setTotalPages(pagesData.size());
        repositionPages();
        Platform.runLater(this::scrollCaretIntoView);
    }

    /**
     * Computes how the paragraphs are distributed across pages.
     * <p>
     * Each paragraph is measured line by line with the real TextFlow engine. Lines are
     * packed into pages; when a line no longer fits, a new page is started. This lets a
     * single paragraph flow across page boundaries at line granularity instead of moving
     * the whole paragraph to the next page.
     */
    private List<List<PageFragment>> computePages(List<Paragraph> paragraphs,
                                                  double contentWidth, double contentHeight,
                                                  int dirtyStart, boolean contentChanged) {
        List<List<PageFragment>> pages = new ArrayList<>();
        List<PageFragment> currentPage = new ArrayList<>();
        double currentY = 0;

        Paragraph openPara = null;
        int openStart = -1;
        int openEnd = -1;
        double openY = 0;

        for (Paragraph p : paragraphs) {
            ParagraphDecoration decoration = p.getDecoration();
            if (decoration != null && decoration.isPageBreakBefore() && !currentPage.isEmpty()) {
                    closeOpenFragment(currentPage, openPara, openStart, openEnd, openY, currentY);
                    openPara = null;
                pages.add(currentPage);
                currentPage = new ArrayList<>();
                currentY = 0;
            }

            List<Line> lines = measureLines(p, contentWidth, dirtyStart, contentChanged);
            for (Line line : lines) {
                if ((!currentPage.isEmpty() || openPara != null)
                        && currentY + line.height > contentHeight) {
                    closeOpenFragment(currentPage, openPara, openStart, openEnd, openY, currentY);
                    openPara = null;
                    pages.add(currentPage);
                    currentPage = new ArrayList<>();
                    currentY = 0;
                }
                if (openPara != p) {
                    closeOpenFragment(currentPage, openPara, openStart, openEnd, openY, currentY);
                    openPara = null;
                    openPara = p;
                    openStart = line.start;
                    openEnd = line.end;
                    openY = currentY;
                } else {
                    openEnd = line.end;
                }
                currentY += line.height;
            }
            // end of paragraph: close the fragment on the current page
            closeOpenFragment(currentPage, openPara, openStart, openEnd, openY, currentY);
            openPara = null;
        }
        closeOpenFragment(currentPage, openPara, openStart, openEnd, openY, currentY);
        openPara = null;

        if (!currentPage.isEmpty()) {
            pages.add(currentPage);
        }
        if (pages.isEmpty()) {
            pages.add(new ArrayList<>()); // always at least one page
        }
        return pages;
    }

    private void closeOpenFragment(List<PageFragment> page, Paragraph openPara, int openStart,
                                   int openEnd, double openY, double currentY) {
        if (openPara != null) {
            page.add(new PageFragment(openPara, openStart, openEnd, openY, currentY - openY));
        }
    }

    /**
     * Measures the rendered lines of a paragraph at the given content width, using the
     * real TextFlow layout engine. Returns one entry per visual line with the document
     * offset range it covers and its height.
     */
    private List<Line> measureLines(Paragraph paragraph, double contentWidth, int dirtyStart,
                                    boolean contentChanged) {
        if (paragraph == null || paragraph.getDecoration() == null) {
            return List.of();
        }
        int paragraphStart = paragraph.getStart();
        int paragraphEnd = paragraph.getEnd();
        ParagraphDecoration decoration = paragraph.getDecoration();
        long contentWidthBits = Double.doubleToLongBits(contentWidth);
        long zoomBits = Double.doubleToLongBits(getZoom());
        ParagraphIdentityKey cacheKey = new ParagraphIdentityKey(paragraph);
        CachedParagraphLayout cached = paragraphLayoutCache.get(cacheKey);
        boolean compatibleCache = cached != null
                && Objects.equals(cached.decoration, decoration)
                && cached.contentWidth == contentWidthBits
                && cached.zoom == zoomBits;
        boolean sameParagraphLength = compatibleCache
                && cached.length == paragraphEnd - paragraphStart;

        List<MeasuredLine> unchangedPrefix = List.of();
        int measurementStart = paragraphStart;
        boolean paragraphChanged = contentChanged
                && dirtyStart >= paragraphStart && dirtyStart <= paragraphEnd;
        if (compatibleCache && sameParagraphLength && !paragraphChanged) {
            return toAbsoluteLines(cached.lines, paragraphStart);
        }
        if (compatibleCache && paragraphChanged && cached.startOffset == paragraphStart) {
            int dirtyParagraphOffset = dirtyStart - paragraphStart;
            int prefixEnd = 0;
            while (prefixEnd < cached.lines.size()
                    && cached.lines.get(prefixEnd).endOffset < dirtyParagraphOffset) {
                prefixEnd++;
            }
            if (prefixEnd > 0) {
                unchangedPrefix = cached.lines.subList(0, prefixEnd);
                measurementStart = paragraphStart + cached.lines.get(prefixEnd).startOffset;
            }
        }

        List<Line> lines = new ArrayList<>(toAbsoluteLines(unchangedPrefix, paragraphStart));
        lines.addAll(measureLineRange(paragraph, contentWidth, measurementStart));
        cacheMeasuredLines(cacheKey, paragraph, contentWidthBits, zoomBits, lines);
        return lines;
    }

    private List<Line> measureLineRange(Paragraph paragraph, double contentWidth, int measurementStart) {
        List<Line> lines = new ArrayList<>();
        ParagraphDecoration decoration = paragraph.getDecoration();
        double lineSpacing = decoration.getSpacing();
        double topInset = measurementStart == paragraph.getStart() ? decoration.getTopInset() : 0;
        double bottomInset = decoration.getBottomInset();

        TextFlow textFlow = new TextFlow();
        textFlow.setPrefWidth(contentWidth);
        textFlow.setTextAlignment(decoration.getAlignment());
        textFlow.setLineSpacing(lineSpacing);
        textFlow.setPadding(new Insets(topInset, 0, bottomInset, 0));

        List<FragmentInfo> infos = new ArrayList<>();
        AtomicInteger pos = new AtomicInteger(measurementStart);
        viewModel.walkFragments((unit, dec) -> {
            if (dec instanceof TextDecoration && !unit.isEmpty()) {
                String text = unitText(unit);
                if (text != null && !text.isEmpty()) {
                    Text t = buildText(text, (TextDecoration) dec);
                    int fStart = pos.get();
                    pos.addAndGet(unit.length());
                    infos.add(new FragmentInfo(t, fStart, pos.get()));
                    textFlow.getChildren().add(t);
                }
            } else if (dec instanceof ImageDecoration) {
                ImageView imageView = buildImage((ImageDecoration) dec);
                int fStart = pos.get();
                pos.addAndGet(1);
                infos.add(new FragmentInfo(imageView, fStart, pos.get()));
                textFlow.getChildren().add(imageView);
            }
        }, measurementStart, paragraph.getEnd());

        // Empty paragraph: render a zero-width line so it still occupies one line.
        if (infos.isEmpty()) {
            Text zero = new Text(TextBuffer.ZERO_WIDTH_TEXT);
            zero.setFont(Font.font(Font.getDefault().getFamily(), Font.getDefault().getSize()));
            infos.add(new FragmentInfo(zero, measurementStart, measurementStart));
            textFlow.getChildren().add(zero);
        }

        textFlow.setMinWidth(contentWidth);
        textFlow.setMaxWidth(contentWidth);
        textFlow.resize(contentWidth, Math.max(1, textFlow.prefHeight(contentWidth)));
        textFlow.applyCss();
        textFlow.layout();

        List<Integer> documentOffsets = new ArrayList<>();
        for (FragmentInfo info : infos) {
            if (info.end <= info.start) {
                continue;
            }
            if (info.node instanceof Text) {
                String text = ((Text) info.node).getText();
                for (int i = 0; i < text.length(); i++) {
                    documentOffsets.add(Math.min(info.start + i, Math.max(info.start, info.end - 1)));
                }
            } else {
                documentOffsets.add(info.start);
            }
        }

        if (documentOffsets.isEmpty()) {
            lines.add(new Line(measurementStart, paragraph.getEnd(), Math.max(1, textFlow.getHeight())));
            return lines;
        }

        double[] caretTops = new double[documentOffsets.size()];
        for (int i = 0; i < documentOffsets.size(); i++) {
            caretTops[i] = caretBounds(textFlow, i).getMinY();
        }

        int lineStart = 0;
        while (lineStart < documentOffsets.size()) {
            double lineTop = caretTops[lineStart];
            int lineEnd = lineStart + 1;
            while (lineEnd < documentOffsets.size() && caretTops[lineEnd] <= lineTop + 0.5) {
                lineEnd++;
            }
            double nextTop = lineEnd < documentOffsets.size()
                    ? caretTops[lineEnd]
                    : textFlow.getHeight();
            double segmentStart = lineStart == 0 ? 0 : lineTop;
            double lineHeight = Math.max(1, nextTop - segmentStart);
            int documentStart = documentOffsets.get(lineStart);
            int documentEnd = lineEnd == documentOffsets.size()
                    ? paragraph.getEnd()
                    : documentOffsets.get(lineEnd - 1) + 1;
            lines.add(new Line(documentStart, documentEnd, lineHeight));
            lineStart = lineEnd;
        }
        return lines;
    }

    private List<Line> toAbsoluteLines(List<MeasuredLine> measuredLines, int paragraphStart) {
        return measuredLines.stream()
                .map(line -> new Line(paragraphStart + line.startOffset,
                        paragraphStart + line.endOffset, line.height))
                .toList();
    }

    private void cacheMeasuredLines(ParagraphIdentityKey key, Paragraph paragraph,
                                    long contentWidth, long zoom, List<Line> lines) {
        int paragraphStart = paragraph.getStart();
        List<MeasuredLine> relativeLines = lines.stream()
                .map(line -> new MeasuredLine(line.start - paragraphStart,
                        line.end - paragraphStart, line.height))
                .toList();
        paragraphLayoutCache.put(key, new CachedParagraphLayout(paragraph.getDecoration(), paragraphStart,
                paragraph.getEnd() - paragraphStart, contentWidth, zoom, relativeLines));
    }

    @SuppressWarnings("deprecation")
    private Bounds caretBounds(TextFlow textFlow, int offset) {
        return new javafx.scene.shape.Path(textFlow.caretShape(offset, true)).getLayoutBounds();
    }

    /**
     * Renders the computed pages into live {@link ParagraphTile}s bound to the shared
     * view model.
     */
    private void renderPages(List<List<PageFragment>> pagesData, double contentWidth,
                             double contentHeight, double zoom, int dirtyStart) {
        // Make paragraph tiles wrap at the zoomed page content width with scaled fonts.
        // The property is normally bound to the editor's content area width; we
        // temporarily unbind it for the page layout and rebind afterwards.
        DoubleProperty textFlowWidthProp = skin.textFlowPrefWidthProperty;
        textFlowWidthProp.unbind();
        textFlowWidthProp.set(contentWidth);
        try {
            int target = pagesData.size();
            int existingPageCount = pages.size();
            while (pages.size() > target) {
                PageView removed = pages.remove(pages.size() - 1);
                documentCanvas.getChildren().remove(removed);
            }
            while (pages.size() < target) {
                PageView page = new PageView();
                page.setPageFormat(getPageFormat());
                page.setZoom(getZoom());
                page.setPageNumber(pages.size() + 1);
                page.setTotalPages(target);
                page.renderHeadersFooters(getHeadersFooters(), getZoom());
                pages.add(page);
                documentCanvas.getChildren().add(page);
            }

            for (int i = 0; i < pages.size(); i++) {
                PageView page = pages.get(i);
                page.setPageNumber(i + 1);
                page.setTotalPages(target);
                page.setPageFormat(getPageFormat());
                page.setZoom(getZoom());
                page.renderHeadersFooters(getHeadersFooters(), getZoom());

                boolean pageIsUnchanged = dirtyStart > 0 && i < existingPageCount
                        && pagesData.get(i).stream().allMatch(fragment -> fragment.end < dirtyStart);
                if (pageIsUnchanged) {
                    continue;
                }

                Pane contentArea = page.getContentArea();
                contentArea.getChildren().clear();

                for (PageFragment fragment : pagesData.get(i)) {
                    List<Node> fragments = new ArrayList<>();
                    List<Integer> positions = new ArrayList<>();
                    collectFragments(fragment.paragraph, fragment.start, fragment.end,
                            fragments, positions);
                    ParagraphTile tile = new ParagraphTile(skin);
                    tile.setParagraphFragment(fragment.paragraph, fragments, positions,
                            new ArrayList<>(), fragment.start, fragment.end,
                            fragment.start <= fragment.paragraph.getStart(),
                            fragment.end >= fragment.paragraph.getEnd());
                    tile.setLayoutX(0);
                    tile.setLayoutY(fragment.y);
                    tile.setPrefWidth(contentWidth);
                    tile.setMinWidth(contentWidth);
                    tile.setMaxWidth(contentWidth);
                    tile.setPrefHeight(fragment.height);
                    tile.setMinHeight(fragment.height);
                    tile.setMaxHeight(fragment.height);
                    // Wire the tile so clicks and drag selection resolve against the shared
                    // view model (caret, selection, hit testing) even though the tiles are
                    // not hosted in a RichListCell.
                    tile.setOnMousePressed(ev -> tile.mousePressedListener(ev));
                    tile.setOnMouseDragged(ev -> tile.mouseDraggedListener(ev));
                    contentArea.getChildren().add(tile);
                }
            }
        } finally {
            skin.textFlowPrefWidthProperty.unbind();
            skin.textFlowPrefWidthProperty.set(contentWidth);
        }
    }

    /**
     * Collects the rendered nodes for {@code paragraph} limited to the document range
     * {@code [startOffset, endOffset)}.
     */
    private void collectFragments(Paragraph paragraph, int startOffset, int endOffset,
                                  List<Node> fragments, List<Integer> positions) {
        boolean tableParagraph = paragraph.getDecoration().hasTableDecoration();
        if (tableParagraph) {
            positions.add(paragraph.getStart());
        }
        AtomicInteger pos = new AtomicInteger(paragraph.getStart());
        viewModel.walkFragments((unit, dec) -> {
            int fStart = pos.get();
            int fEnd = fStart + unit.length();
            if (tableParagraph) {
                if (dec instanceof TextDecoration && !unit.isEmpty()) {
                    String text = unitText(unit);
                    if (text != null && !text.isEmpty()) {
                        fragments.add(buildText(text, (TextDecoration) dec));
                    }
                } else if (dec instanceof ImageDecoration) {
                    fragments.add(buildImage((ImageDecoration) dec));
                }
                pos.addAndGet(unit.length());
                positions.add(pos.get());
                return;
            }
            if (fEnd <= startOffset || fStart >= endOffset) {
                pos.addAndGet(unit.length());
                return;
            }
            if (dec instanceof TextDecoration && !unit.isEmpty()) {
                String text = unitText(unit);
                if (text != null && !text.isEmpty()) {
                    int clippedStart = Math.max(startOffset, fStart);
                    int clippedEnd = Math.min(endOffset, fEnd);
                    int textStart = Math.min(clippedStart - fStart, text.length());
                    int textEnd = Math.min(clippedEnd - fStart, text.length());
                    if (textEnd > textStart) {
                        fragments.add(buildText(text.substring(textStart, textEnd), (TextDecoration) dec));
                        positions.add(clippedStart);
                        positions.add(clippedEnd);
                    }
                }
            } else if (dec instanceof ImageDecoration) {
                fragments.add(buildImage((ImageDecoration) dec));
                positions.add(fStart);
                positions.add(fEnd);
            }
            pos.addAndGet(unit.length());
        }, paragraph.getStart(), paragraph.getEnd());
    }

    private String unitText(com.gluonhq.richtextarea.model.Unit unit) {
        if (unit instanceof com.gluonhq.richtextarea.model.TextUnit) {
            return ((com.gluonhq.richtextarea.model.TextUnit) unit).getText().replace("\n", "");
        } else if (unit instanceof com.gluonhq.richtextarea.model.BlockUnit) {
            return ((com.gluonhq.richtextarea.model.BlockUnit) unit).getBlock().getContent();
        }
        return null;
    }

    private Text buildText(String content, TextDecoration decoration) {
        Text text = new Text(content);
        try {
            text.setFill(Color.web(decoration.getForeground()));
        } catch (Exception e) {
            text.setFill(Color.BLACK);
        }
        text.setStrikethrough(decoration.isStrikethrough());
        text.setUnderline(decoration.isUnderline());
        int hash = Objects.hash(decoration.getFontFamily(), decoration.getFontWeight(),
                decoration.getFontPosture(), decoration.getFontSize() * getZoom());
        Font font = skin.getFontCache().computeIfAbsent(hash,
                h -> Font.font(decoration.getFontFamily(), decoration.getFontWeight(),
                        decoration.getFontPosture(), decoration.getFontSize() * getZoom()));
        text.setFont(font);
        return text;
    }

    private javafx.scene.image.ImageView buildImage(com.gluonhq.richtextarea.model.ImageDecoration imageDecoration) {
        javafx.scene.image.Image image = skin.getImageCache().computeIfAbsent(imageDecoration.getUrl(), javafx.scene.image.Image::new);
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
        double contentWidth = skin.textFlowPrefWidthProperty.get();
        if (imageDecoration.getWidth() > -1 && imageDecoration.getHeight() > -1) {
            imageView.setFitWidth(imageDecoration.getWidth() * getZoom());
            imageView.setFitHeight(imageDecoration.getHeight() * getZoom());
        } else {
            double width = Math.min(image.getWidth(), contentWidth - 10);
            imageView.setFitWidth(width);
            imageView.setPreserveRatio(true);
        }
        return imageView;
    }

    private void clearPages() {
        for (PageView page : pages) {
            documentCanvas.getChildren().remove(page);
        }
        pages.clear();
    }

    private void setTotalPages(int totalPages) {
        totalPagesProperty.set(totalPages);
    }

    /**
     * Positions the pages inside the document canvas: horizontally centered, stacked
     * vertically with a gap, and sizes the canvas scroll area.
     */
    private void repositionPages() {
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0) {
            return;
        }
        double zoom = getZoom();
        double pageGap = getPageGap() * zoom;
        double currentY = 0;
        double canvasWidth = viewportWidth;
        for (PageView page : pages) {
            double pageWidth = page.getPageWidth() * zoom;
            canvasWidth = Math.max(canvasWidth, pageWidth);
            double layoutX = (canvasWidth - pageWidth) / 2.0;
            page.setLayoutX(layoutX);
            page.setLayoutY(currentY);
            currentY += page.getPageHeight() * zoom + pageGap;
        }
        double canvasHeight = Math.max(currentY, scrollPane.getViewportBounds().getHeight());
        documentCanvas.setMinSize(canvasWidth, canvasHeight);
        documentCanvas.setPrefSize(canvasWidth, canvasHeight);
        documentCanvas.resize(canvasWidth, canvasHeight);
        documentCanvas.requestLayout();
        scrollPane.requestLayout();
    }

    private void scrollCaretIntoView() {
        if (viewModel == null || pages.isEmpty() || getScene() == null) {
            return;
        }
        int caretPosition = viewModel.getCaretPosition();
        Bounds caretBounds = null;
        for (PageView page : pages) {
            for (Node node : page.getContentArea().getChildren()) {
                if (node instanceof ParagraphTile) {
                    caretBounds = ((ParagraphTile) node).getCaretBounds(documentCanvas, caretPosition);
                    if (caretBounds != null) {
                        break;
                    }
                }
            }
            if (caretBounds != null) {
                break;
            }
        }
        if (caretBounds == null) {
            return;
        }

        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double contentHeight = documentCanvas.getBoundsInLocal().getHeight();
        double maxScroll = contentHeight - viewportHeight;
        if (viewportHeight <= 0 || maxScroll <= 0) {
            return;
        }

        double currentScroll = (scrollPane.getVvalue() - scrollPane.getVmin())
                / (scrollPane.getVmax() - scrollPane.getVmin()) * maxScroll;
        double margin = 24 * getZoom();
        double targetScroll = currentScroll;
        if (caretBounds.getMinY() < currentScroll + margin) {
            targetScroll = caretBounds.getMinY() - margin;
        } else if (caretBounds.getMaxY() > currentScroll + viewportHeight - margin) {
            targetScroll = caretBounds.getMaxY() - viewportHeight + margin;
        }
        if (targetScroll != currentScroll) {
            double normalized = Math.max(0, Math.min(1, targetScroll / maxScroll));
            scrollPane.setVvalue(scrollPane.getVmin()
                    + normalized * (scrollPane.getVmax() - scrollPane.getVmin()));
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = getWidth();
        double h = getHeight();
        backgroundRect.setWidth(w);
        backgroundRect.setHeight(h);
        scrollPane.resize(w, h);
        scrollPane.setLayoutX(0);
        scrollPane.setLayoutY(0);
        repositionPages();
    }

    // ------------------------------------------------------------------ hit testing

    /**
     * Returns the page that contains the given point (in document canvas coordinates),
     * or {@code null} if the point falls in a gap or outside all pages.
     */
    public PageView getPageAtPoint(Point2D point) {
        for (PageView page : pages) {
            if (page.getBoundsInParent().contains(point)) {
                return page;
            }
        }
        return null;
    }

    /**
     * Converts a point in document canvas coordinates to the document position (caret
     * index) at that location, by hit testing the live paragraph tiles.
     *
     * @return the document position, or -1 if the point is not over any text
     */
    public int hitTest(Point2D point) {
        Point2D screenPt = documentCanvas.localToScreen(point);
        for (PageView page : pages) {
            Pane contentArea = page.getContentArea();
            Point2D contentLocal = contentArea.screenToLocal(screenPt);
            for (Node child : contentArea.getChildren()) {
                if (child instanceof ParagraphTile && child.getBoundsInParent().contains(contentLocal)) {
                    // Let the tile resolve the exact caret position from the click.
                    return hitTestTile((ParagraphTile) child, point);
                }
            }
        }
        return -1;
    }

    private int hitTestTile(ParagraphTile tile, Point2D canvasPoint) {
        // The tile's layers perform hit testing on their text flows. Ask the tile to
        // resolve the caret position for the given screen point.
        Point2D screenPt = documentCanvas.localToScreen(canvasPoint);
        MouseEvent synthetic = new MouseEvent(tile, tile, MouseEvent.MOUSE_PRESSED,
                screenPt.getX(), screenPt.getY(),
                screenPt.getX(), screenPt.getY(),
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false,
                true, false, false,
                false, false, false, null);
        // The tile listener sets the caret in the shared view model; we read it back.
        int before = viewModel.getCaretPosition();
        tile.mousePressedListener(synthetic);
        int after = viewModel.getCaretPosition();
        if (after != before) {
            return after;
        }
        // Fallback: map to the start of the tile's range if caret did not move.
        return tileRangeStart(tile);
    }

    private int tileRangeStart(ParagraphTile tile) {
        return -1;
    }

    // ------------------------------------------------------------------ inner classes

    /**
     * A fragment of a paragraph rendered on a single page.
     */
    private static final class PageFragment {
        final Paragraph paragraph;
        final int start;
        final int end;
        final double y;      // un-scaled top offset within the page content area
        final double height; // un-scaled height

        PageFragment(Paragraph paragraph, int start, int end, double y, double height) {
            this.paragraph = paragraph;
            this.start = start;
            this.end = end;
            this.y = y;
            this.height = height;
        }
    }

    /**
     * A single rendered line of a paragraph, with the document offset range it covers.
     */
    private static final class Line {
        final int start;
        final int end;
        final double height;

        Line(int start, int end, double height) {
            this.start = start;
            this.end = end;
            this.height = height;
        }
    }

    /**
     * A rendered fragment node together with its document start offset.
     */
    private static final class FragmentInfo {
        final Node node;
        final int start;
        final int end;

        FragmentInfo(Node node, int start, int end) {
            this.node = node;
            this.start = start;
            this.end = end;
        }
    }
}
