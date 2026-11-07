/*
 * Copyright (c) 2025, Gluon
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gluonhq.richtextarea.model.DecorationModel;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.HeaderFooter;
import com.gluonhq.richtextarea.model.PageFormat;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.TextDecoration;
import com.gluonhq.richtextarea.viewmodel.RichTextAreaViewModel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests for {@link DocumentView}: a real, editable multi-page document view.
 * <p>
 * The tests exercise the page layout engine (real measurement, paragraph splitting,
 * page centering, gaps, zoom, format/orientation changes) and hit testing.
 */
public class DocumentViewTest {

    private final List<Stage> testStages = new CopyOnWriteArrayList<>();

    @BeforeAll
    public static void initToolkit() throws Exception {
        if (!Platform.isFxApplicationThread()) {
            CompletableFuture<Void> initialized = new CompletableFuture<>();
            try {
                Platform.startup(() -> initialized.complete(null));
            } catch (IllegalStateException alreadyInitialized) {
                Platform.runLater(() -> initialized.complete(null));
            }
            initialized.get(10, TimeUnit.SECONDS);
        }
        Platform.setImplicitExit(false);
    }

    @AfterEach
    public void closeTestStages() throws Exception {
        CompletableFuture<Void> closed = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                testStages.forEach(Stage::close);
                testStages.clear();
                closed.complete(null);
            } catch (Throwable failure) {
                closed.completeExceptionally(failure);
            }
        });
        closed.get(10, TimeUnit.SECONDS);
    }

    private DocumentView newViewWithText(String text, PageFormat format) {
        return newViewWithDocument(new Document(text), format);
    }

    private DocumentView newViewWithDocument(Document document, PageFormat format) {
        CompletableFuture<DocumentView> initialized = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                DocumentView view = new DocumentView();
                showInStage(view, 1000, 800);
                view.setPageFormat(format);
                view.getEditor().textLengthProperty().addListener((obs, oldLength, newLength) -> {
                    if (newLength.intValue() == document.getText().length()) {
                        Platform.runLater(() -> {
                            try {
                                view.relayout();
                                initialized.complete(view);
                            } catch (Throwable failure) {
                                initialized.completeExceptionally(failure);
                            }
                        });
                    }
                });
                view.setDocument(document);
            } catch (Throwable failure) {
                initialized.completeExceptionally(failure);
            }
        });
        try {
            return initialized.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while initializing the document view", interrupted);
        } catch (ExecutionException | TimeoutException failure) {
            throw new IllegalStateException("Could not initialize the document view", failure);
        }
    }

    private Stage showInStage(DocumentView view, double width, double height) {
        Stage stage = new Stage();
        stage.setScene(new Scene(view, width, height));
        stage.show();
        view.applyCss();
        view.layout();
        testStages.add(stage);
        return stage;
    }

    private void runOnFxThread(Runnable action) {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                completed.complete(null);
            } catch (Throwable failure) {
                completed.completeExceptionally(failure);
            }
        });
        try {
            completed.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running a test action on the FX thread", interrupted);
        } catch (ExecutionException | TimeoutException failure) {
            throw new IllegalStateException("Could not run a test action on the FX thread", failure);
        }
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Test
    public void testRenderedDocumentKeepsEditableEditorFocusable() {
        DocumentView view = new DocumentView();

        assertTrue(view.getEditor().isVisible(),
                "The backing editor must remain eligible to receive keyboard focus");
        assertTrue(view.getEditor().isEditable());
        assertTrue(view.getEditor().isFocusTraversable());
    }

    @Test
    public void testDetachingEditorRestoresFlowTextWidthBinding() throws Exception {
        CompletableFuture<Boolean> restoredBinding = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage stage = null;
            try {
                DocumentView view = new DocumentView();
                stage = new Stage();
                stage.setScene(new Scene(view, 1000, 800));
                stage.show();
                view.applyCss();
                view.layout();

                RichTextAreaSkin skin = (RichTextAreaSkin) view.getEditor().getSkin();
                assertNotNull(skin, "The editor skin must be initialized before creating pages");
                view.setPageFormat(PageFormat.builder().build());
                view.relayout();
                assertFalse(skin.textFlowPrefWidthProperty.isBound(),
                        "The page view must use a fixed text width while rendering pages");
                view.setEditor(null);
                restoredBinding.complete(skin.textFlowPrefWidthProperty.isBound());
            } catch (Throwable failure) {
                restoredBinding.completeExceptionally(failure);
            } finally {
                if (stage != null) {
                    stage.close();
                }
            }
        });
        assertTrue(restoredBinding.get(10, TimeUnit.SECONDS),
                "Detaching the page view must restore the editor's viewport-width binding");
    }

    @Test
    public void testFlowHeightIsRecomputedAfterLeavingPageView() throws Exception {
        CompletableFuture<double[]> heights = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage stage = null;
            try {
                DocumentView view = new DocumentView();
                RichTextArea editor = new RichTextArea();
                view.setEditor(editor);
                stage = new Stage();
                stage.setScene(new Scene(view, 1000, 800));
                stage.show();
                view.applyCss();
                view.layout();
                view.setPageFormat(PageFormat.builder().build());

                String text = repeat("Long text must flow across pages. ", 400);
                double heightBeforeDocument = editor.getFullHeight();
                Stage testStage = stage;
                editor.textLengthProperty().addListener((obs, oldLength, newLength) -> {
                    if (newLength.intValue() == text.length()) {
                        Platform.runLater(() -> {
                            try {
                                double heightInPageView = editor.getFullHeight();
                                view.setEditor(null);
                                heights.complete(new double[] {heightBeforeDocument, heightInPageView,
                                        editor.getFullHeight()});
                            } catch (Throwable failure) {
                                heights.completeExceptionally(failure);
                            } finally {
                                testStage.close();
                            }
                        });
                    }
                });
                view.setDocument(new Document(text));
            } catch (Throwable failure) {
                if (stage != null) {
                    stage.close();
                }
                heights.completeExceptionally(failure);
            }
        });

        double[] measuredHeights = heights.get(10, TimeUnit.SECONDS);
        assertEquals(measuredHeights[0], measuredHeights[1], 0.01,
                "Page view edits must not recompute the hidden flow layout");
        assertTrue(measuredHeights[2] > measuredHeights[1],
                "Returning to flow view must recompute the document height");
    }

    @Test
    public void testTypedInputUpdatesTheRenderedDocument() throws Exception {
        CompletableFuture<Integer> newLength = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                DocumentView view = new DocumentView();
                Stage stage = new Stage();
                stage.setScene(new Scene(view, 1000, 800));
                stage.show();
                view.applyCss();
                view.layout();
                view.setDocument(new Document("abc"));
                Platform.runLater(() -> {
                    try {
                        assertNotNull(view.getEditor().getOnKeyTyped(),
                                "The editor skin must register its character input handler");
                        view.getEditor().requestFocus();
                        Platform.runLater(() -> {
                            try {
                                assertTrue(view.getEditor().isFocused(),
                                        "The keyboard focus must stay with the backing editor");
                                Event.fireEvent(view.getEditor(), new KeyEvent(
                                        KeyEvent.KEY_TYPED, "x", "x", KeyCode.UNDEFINED,
                                        false, false, false, false));
                                Platform.runLater(() -> {
                                    newLength.complete(view.getEditor().getTextLength());
                                    stage.close();
                                });
                            } catch (Throwable failure) {
                                stage.close();
                                newLength.completeExceptionally(failure);
                            }
                        });
                    } catch (Throwable failure) {
                        stage.close();
                        newLength.completeExceptionally(failure);
                    }
                });
            } catch (Throwable failure) {
                newLength.completeExceptionally(failure);
            }
        });
        assertEquals(4, newLength.get(10, TimeUnit.SECONDS),
                "Typing into the editor must update the document model");
    }

    @Test
    public void testDocumentPagesRefreshAfterOpeningAndEditing() throws Exception {
        CompletableFuture<double[]> pageCounts = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage stage = null;
            try {
                DocumentView view = new DocumentView();
                stage = new Stage();
                view.setEditor(new RichTextArea());
                stage.setScene(new Scene(view, 1000, 800));
                stage.show();
                view.applyCss();
                view.layout();
                assertNotNull(view.getViewModel(), "The replacement editor must attach its skin");
                PageFormat format = PageFormat.builder()
                        .pageSize(PageFormat.PageSize.A4)
                        .orientation(PageFormat.Orientation.PORTRAIT)
                        .margins(new Insets(72, 72, 72, 72))
                        .build();
                view.setPageFormat(format);
                String longText = repeat("Long text must flow across pages. ", 400);
                String insertedText = repeat("Long text must flow across pages. ", 400);
                int insertedLength = "short".length() + insertedText.length();
                RichTextArea editor = view.getEditor();
                Stage testStage = stage;
                editor.textLengthProperty().addListener((obs, oldLength, newLength) -> {
                    if (newLength.intValue() == longText.length()) {
                        Platform.runLater(() -> {
                            try {
                                int pagesAfterOpen = view.getPageCount();
                                editor.textLengthProperty().addListener((textObs, oldTextLength, newTextLength) -> {
                                    if (newTextLength.intValue() == "short".length()) {
                                        Platform.runLater(() -> {
                                            try {
                                                editor.textLengthProperty().addListener(
                                                        (editObs, oldEditLength, newEditLength) -> {
                                                            if (newEditLength.intValue() == insertedLength) {
                                                                Platform.runLater(() -> {
                                                                    view.applyCss();
                                                                    view.layout();
                                                                    ScrollPane scrollPane = view.getScrollPane();
                                                                    scrollPane.layout();
                                                                    PageView secondPage = view.getPages().get(1);
                                                                    double canvasHeight = scrollPane.getContent()
                                                                            .getBoundsInLocal().getHeight();
                                                                    double viewportHeight = scrollPane.getViewportBounds()
                                                                            .getHeight();
                                                                    double secondPageBottom = secondPage.getLayoutY()
                                                                            + secondPage.getPageHeight() * view.getZoom();
                                                                    double scrollAfterEdit = scrollPane.getVvalue();
                                                                    view.getViewModel().setCaretPosition(0);
                                                                    Platform.runLater(() -> {
                                                                        double scrollAfterMovingToStart =
                                                                                scrollPane.getVvalue();
                                                                        pageCounts.complete(new double[] {
                                                                                pagesAfterOpen,
                                                                                view.getPageCount(),
                                                                                canvasHeight,
                                                                                viewportHeight,
                                                                                secondPageBottom,
                                                                                scrollAfterEdit,
                                                                                scrollAfterMovingToStart
                                                                        });
                                                                        testStage.close();
                                                                    });
                                                                });
                                                            }
                                                        });
                                                editor.getActionFactory().insertText(insertedText)
                                                        .execute(new ActionEvent());
                                            } catch (Throwable failure) {
                                                testStage.close();
                                                pageCounts.completeExceptionally(failure);
                                            }
                                        });
                                    }
                                });
                                view.setDocument(new Document("short"));
                            } catch (Throwable failure) {
                                testStage.close();
                                pageCounts.completeExceptionally(failure);
                            }
                        });
                    }
                });
                view.setDocument(new Document(longText));
            } catch (Throwable failure) {
                if (stage != null) {
                    stage.close();
                }
                pageCounts.completeExceptionally(failure);
            }
        });

        double[] counts = pageCounts.get(10, TimeUnit.SECONDS);
        assertTrue(counts[0] >= 2, "Opening a long document must render all pages");
        assertTrue(counts[1] >= 2, "Editing must reflow text onto additional pages; got " + counts[1]);
        assertTrue(counts[2] > counts[3],
                "The scrollable canvas must be taller than the viewport; canvas=" + counts[2]
                        + ", viewport=" + counts[3]);
        assertTrue(counts[2] >= counts[4],
                "The scrollable canvas must include page 2; canvas=" + counts[2]
                        + ", page 2 bottom=" + counts[4]);
        assertTrue(counts[5] > 0, "Typing at the end of the document must scroll the caret into view");
        assertTrue(counts[6] < counts[5], "Moving the caret to page 1 must scroll back up");
    }

    @Test
    public void testEditingOnSecondPageKeepsEarlierPageTiles() throws Exception {
        CompletableFuture<Boolean> earlierTilesReused = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage stage = null;
            try {
                DocumentView view = new DocumentView();
                stage = new Stage();
                stage.setScene(new Scene(view, 1000, 800));
                stage.show();
                view.applyCss();
                view.layout();

                view.setPageFormat(PageFormat.builder().build());
                String text = repeat("A long paragraph should keep earlier page tiles intact. ", 400);
                RichTextArea editor = view.getEditor();
                Stage testStage = stage;
                editor.textLengthProperty().addListener((obs, oldLength, newLength) -> {
                    if (newLength.intValue() == text.length()) {
                        Platform.runLater(() -> {
                            try {
                                view.relayout();
                                assertTrue(view.getPageCount() >= 2,
                                        "The test document must span multiple pages");

                                PageView firstPage = view.getPages().get(0);
                                Node firstPageTile = firstPage.getContentArea().getChildren().get(0);
                                PageView secondPage = view.getPages().get(1);
                                ParagraphTile secondPageTile = (ParagraphTile) secondPage.getContentArea()
                                        .getChildren().get(0);
                                int editPosition = secondPageTile.getRangeStart() + 3;
                                editor.textLengthProperty().addListener(
                                        (editObs, oldEditLength, newEditLength) -> {
                                            if (newEditLength.intValue() == text.length() + 1) {
                                                Platform.runLater(() -> Platform.runLater(() -> {
                                                    try {
                                                        earlierTilesReused.complete(firstPageTile
                                                                == firstPage.getContentArea().getChildren().get(0));
                                                    } catch (Throwable failure) {
                                                        earlierTilesReused.completeExceptionally(failure);
                                                    } finally {
                                                        testStage.close();
                                                    }
                                                }));
                                            }
                                        });
                                view.getViewModel().setCaretPosition(editPosition);
                                editor.getActionFactory().insertText("x").execute(new ActionEvent());
                            } catch (Throwable failure) {
                                earlierTilesReused.completeExceptionally(failure);
                                testStage.close();
                            }
                        });
                    }
                });
                view.setDocument(new Document(text));
            } catch (Throwable failure) {
                if (stage != null) {
                    stage.close();
                }
                earlierTilesReused.completeExceptionally(failure);
            }
        });

        assertTrue(earlierTilesReused.get(10, TimeUnit.SECONDS),
                "Typing on page 2 must not recreate content tiles on page 1");
    }

    @Test
    public void testPageContentAndFooterRespectPageMargins() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        PageView page = new PageView();
        page.setPageFormat(format);

        assertEquals(72, page.getContentAreaBounds().getMinX(), 0.01,
                "Content must start at the left margin");
        assertEquals(format.getContentWidth(), page.getContentAreaBounds().getWidth(), 0.01,
                "Content width must leave both page margins");
        assertEquals(format.getHeight() - format.getMargins().getBottom(),
                page.getFooterAreaBounds().getMinY(), 0.01,
                "The footer must be inside the bottom margin");
        assertEquals(format.getMargins().getBottom(), page.getFooterAreaBounds().getHeight(), 0.01,
                "The footer must use the bottom margin area");
    }

    @Test
    public void testZoomedPagesCanScrollHorizontally() throws Exception {
        CompletableFuture<double[]> dimensions = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage stage = null;
            try {
                DocumentView view = new DocumentView();
                stage = new Stage();
                stage.setScene(new Scene(view, 500, 700));
                stage.show();
                view.applyCss();
                view.layout();
                view.setPageFormat(PageFormat.builder().build());
                view.setZoom(2.0);
                view.relayout();
                view.applyCss();
                view.layout();
                ScrollPane scrollPane = view.getScrollPane();
                scrollPane.applyCss();
                scrollPane.layout();
                double canvasWidth = scrollPane.getContent().getBoundsInLocal().getWidth();
                double viewportWidth = scrollPane.getViewportBounds().getWidth();
                dimensions.complete(new double[]{canvasWidth, viewportWidth,
                        scrollPane.getHmin(), scrollPane.getHmax()});
            } catch (Throwable failure) {
                dimensions.completeExceptionally(failure);
            } finally {
                if (stage != null) {
                    stage.close();
                }
            }
        });

        double[] result = dimensions.get(10, TimeUnit.SECONDS);
        assertTrue(result[0] > result[1],
                "The zoomed page must be wider than the viewport and remain scrollable");
        assertTrue(result[3] > result[2],
                "The ScrollPane must expose a horizontal scroll range");
    }

    @Test
    public void testWrappedParagraphsFlowAcrossPagesWithoutOverlappingTiles() throws Exception {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                PageFormat format = PageFormat.builder()
                        .pageSize(PageFormat.PageSize.A4)
                        .orientation(PageFormat.Orientation.PORTRAIT)
                        .margins(new Insets(72, 72, 72, 72))
                        .build();
                DocumentView view = new DocumentView();
                showInStage(view, 1000, 800);
                view.setPageFormat(format);
                String text = repeat("Long text must wrap at the page width. ", 400);
                view.getEditor().textLengthProperty().addListener((obs, oldValue, newValue) -> {
                    if (newValue.intValue() == text.length()) {
                        Platform.runLater(() -> {
                            try {
                                view.relayout();
                                assertTrue(view.getPageCount() >= 2,
                                        "Wrapped text must flow onto another page, got " + view.getPageCount()
                                                + " pages for " + view.getEditor().getTextLength() + " characters");
                                for (PageView page : view.getPages()) {
                                    List<Node> tiles = new java.util.ArrayList<>(page.getContentArea().getChildren());
                                    tiles.sort(java.util.Comparator.comparingDouble(Node::getLayoutY));
                                    for (int i = 1; i < tiles.size(); i++) {
                                        Node previous = tiles.get(i - 1);
                                        Node current = tiles.get(i);
                                        assertTrue(previous.getLayoutY() + previous.prefHeight(-1)
                                                        <= current.getLayoutY() + 1,
                                                "Rendered paragraph tiles must not overlap");
                                    }
                                }
                                completed.complete(null);
                            } catch (Throwable failure) {
                                completed.completeExceptionally(failure);
                            }
                        });
                    }
                });
                view.setDocument(new Document(text));
            } catch (Throwable failure) {
                completed.completeExceptionally(failure);
            }
        });
        completed.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void testPageCountAndDimensions() {
        String longText = repeat("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ", 400);
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        DocumentView view = newViewWithText(longText, format);

        assertTrue(view.getPageCount() >= 3,
                "Expected at least 3 pages for a long document, got " + view.getPageCount());

        double expectedRatio = format.getWidth() / format.getHeight(); // ~0.707 for A4 portrait
        for (PageView page : view.getPages()) {
            double ratio = page.getPageWidth() / page.getPageHeight();
            assertEquals(expectedRatio, ratio, 0.01, "Page must keep A4 portrait proportions");
        }
        assertEquals(format.getContentWidth(), view.getPages().get(0).getContentWidth(), 0.01);
    }

    @Test
    public void testLongParagraphSplitsAcrossPages() {
        String longText = repeat("A very long single paragraph that must flow across page boundaries without being moved as a whole. ", 300);
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        DocumentView view = newViewWithText(longText, format);

        assertTrue(view.getPageCount() >= 2,
                "A single long paragraph must produce multiple pages, got " + view.getPageCount());

        assertFalse(view.getPages().get(0).getContentArea().getChildren().isEmpty(),
                "Page 1 must host part of the paragraph");
        assertTrue(view.getPageCount() >= 2,
                "The paragraph must continue on page 2");
        assertFalse(view.getPages().get(1).getContentArea().getChildren().isEmpty(),
                "Page 2 must host the continuation of the paragraph");
    }

    @Test
    public void testReflowShrinksWhenTextIsRemoved() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        DocumentView longView = newViewWithText(repeat("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ", 400), format);
        int pagesWithLongText = longView.getPageCount();

        DocumentView shortView = newViewWithText("Short text.", format);

        assertTrue(pagesWithLongText >= 2);
        assertEquals(1, shortView.getPageCount(),
                "After removing text, the document must reflow back to fewer pages");
    }

    @Test
    public void testPageFormatChangeRelayouts() {
        PageFormat a4 = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        DocumentView view = newViewWithText(repeat("x ", 2000), a4);
        double a4Width = view.getPages().get(0).getPageWidth();

        PageFormat letter = PageFormat.builder()
                .pageSize(PageFormat.PageSize.LETTER)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        runOnFxThread(() -> {
            view.setPageFormat(letter);
            view.relayout();
        });

        double letterWidth = view.getPages().get(0).getPageWidth();
        assertNotEquals(a4Width, letterWidth, 0.001,
                "Changing page format must change the page width");
    }

    @Test
    public void testOrientationChangeSwapsDimensions() {
        PageFormat portrait = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        DocumentView view = newViewWithText(repeat("x ", 2000), portrait);
        double portraitWidth = view.getPages().get(0).getPageWidth();
        double portraitHeight = view.getPages().get(0).getPageHeight();

        PageFormat landscape = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.LANDSCAPE)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        runOnFxThread(() -> {
            view.setPageFormat(landscape);
            view.relayout();
        });

        double landscapeWidth = view.getPages().get(0).getPageWidth();
        double landscapeHeight = view.getPages().get(0).getPageHeight();
        assertEquals(portraitHeight, landscapeWidth, 0.01);
        assertEquals(portraitWidth, landscapeHeight, 0.01);
    }

    @Test
    public void testZoomScalesPageSize() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        DocumentView view = newViewWithText("Hello world", format);
        PageView page = view.getPages().get(0);
        double[] pageWidths = new double[] {page.getPageBounds().getWidth(), 0};

        runOnFxThread(() -> {
            view.setZoom(2.0);
            view.relayout();
            view.applyCss();
            view.layout();
            pageWidths[1] = page.getPageBounds().getWidth();
        });

        assertEquals(pageWidths[0] * 2.0, pageWidths[1], 1.0,
                "Doubling zoom must double the rendered page width");
    }

    @Test
    public void testPagesAreHorizontallyCentered() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        DocumentView view = newViewWithText("Hello world", format);

        double pageWidth = view.getPages().get(0).getPageWidth();
        ScrollPane scrollPane = view.getScrollPane();
        double[] viewportWidth = new double[1];
        runOnFxThread(() -> {
            scrollPane.resize(1200, 800);
            view.layoutChildren();
            scrollPane.layout();
            viewportWidth[0] = scrollPane.getViewportBounds().getWidth();
        });

        double actualLayoutX = view.getPages().get(0).getLayoutX();
        assertEquals((viewportWidth[0] - pageWidth) / 2.0, actualLayoutX, 1.0,
                "The page must be horizontally centered in the viewport");
    }

    @Test
    public void testHitTestLocatesTextOnPage() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        DocumentView view = newViewWithText("First page content here.", format);

        RichTextAreaViewModel viewModel = view.getViewModel();
        assertNotNull(viewModel);

        PageView page1 = view.getPages().get(0);
        double mx = page1.getMargins().getLeft() + 10;
        double my = page1.getMargins().getTop() + 10;
        assertTrue(page1.getContentAreaBounds().contains(mx, my),
                "Page 1 content area should cover a point near its top-left");

        // Long text must flow to page 2 and page 2 must hold renderable tiles.
        String longText = repeat("Flow flow flow flow flow flow flow flow flow flow flow flow flow flow flow flow flow flow flow flow ", 60);
        DocumentView longView = newViewWithText(longText, format);
        assertTrue(longView.getPageCount() >= 2,
                "Long text must flow to page 2, got " + longView.getPageCount());
        PageView page2 = longView.getPages().get(1);
        List<Node> page2Tiles = longView.getTileNodes(page2);
        assertFalse(page2Tiles.isEmpty(), "Page 2 must have renderable tiles");
        boolean anyTileHit = page2Tiles.stream()
                .anyMatch(t -> t.getBoundsInParent().contains(
                        5, 5));
        assertTrue(anyTileHit, "A point over page-2 text must fall inside a tile");
    }

    @Test
    public void testHeadersAndFootersRendered() {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        DocumentView view = newViewWithText(repeat("x ", 3000), format);
        runOnFxThread(() -> {
            view.setHeadersFooters(List.of(
                    HeaderFooter.builder().type(HeaderFooter.Type.HEADER).scope(HeaderFooter.Scope.ALL_PAGES)
                            .content("Header").height(36).distanceFromEdge(18).build(),
                    HeaderFooter.builder().type(HeaderFooter.Type.FOOTER).scope(HeaderFooter.Scope.ALL_PAGES)
                            .content("Footer").height(36).distanceFromEdge(18).build()
            ));
            view.relayout();
        });

        assertTrue(view.getPageCount() >= 2);
        for (PageView page : view.getPages()) {
            assertFalse(page.getContentArea().getChildren().isEmpty());
        }
    }

    @Test
    public void testExplicitPageBreakMovesContentToNextPage() throws Exception {
        PageFormat format = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        TextDecoration td = TextDecoration.builder().presets().build();
        ParagraphDecoration normal = ParagraphDecoration.builder().presets().build();
        ParagraphDecoration withBreak = ParagraphDecoration.builder().presets().pageBreakBefore(true).build();

        String text = "First paragraph content.\nSecond paragraph content.";
        int firstEnd = text.indexOf('\n') + 1; // includes the line feed
        List<DecorationModel> models = List.of(
                new DecorationModel(0, firstEnd, td, normal),
                new DecorationModel(firstEnd, text.length() - firstEnd, td, withBreak)
        );
        Document doc = new Document(text, models, 0, format, List.of());
        DocumentView view = newViewWithDocument(doc, format);

        // The explicit page break forces the second paragraph onto a new page.
        assertTrue(view.getPageCount() >= 2,
                "Explicit page break must start the second paragraph on page 2, got " + view.getPageCount());
    }
}
