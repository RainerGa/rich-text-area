/*
 * Copyright (c) 2024, Gluon
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.richtextarea.ui;

import com.gluonhq.richtextarea.RichTextArea;
import com.gluonhq.richtextarea.RichTextAreaSkin;
import com.gluonhq.richtextarea.action.Action;
import com.gluonhq.richtextarea.model.DecorationModel;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.TableDecoration;
import com.gluonhq.richtextarea.model.TextBuffer;
import com.gluonhq.richtextarea.model.TextDecoration;
import com.gluonhq.richtextarea.viewmodel.RichTextAreaViewModel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Init;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Verifies the <em>selection highlight</em> of a table cell: the highlight of a selected range
 * has to match the text layout of the cell. The "Table Selection Highlight" bug showed a
 * highlight that was calculated from a text flow without width: every character of the range
 * ended up on its own line, so only the first character was highlighted in the cell and the
 * remaining characters were drawn as separate marks over the rows below.
 *
 * <p>The bug appears when the cell is created while the selection is already active, e.g. when
 * the table is scrolled into view after the selection was made. The test reproduces exactly that:
 * the table is placed below the visible area, the selection is set, and only then the paragraph
 * is scrolled into view so that the cell is created.</p>
 *
 * <p>All measurements are taken by this test, the production code stays silent.</p>
 */
@ExtendWith(ApplicationExtension.class)
public class TableSelectionHighlightTest {

    /** Tolerance for the comparison of two shape bounds. */
    private static final double TOLERANCE_PX = 1.0;
    /** Number of cells of the table used in this test. */
    private static final int TABLE_ROWS = 5;
    private static final int TABLE_COLUMNS = 3;
    /** The cell that is selected, and the text range inside it. */
    private static final String SELECTED_CELL_TEXT = "Headings";
    private static final String SELECTED_RANGE = "Heading";

    private RichTextArea richTextArea;
    private Scene scene;

    @Init
    public void init() {
        richTextArea = new RichTextArea();
        // Small window: the table is below the visible area and is only rendered when it is
        // scrolled into view.
        richTextArea.setPrefSize(600, 400);
    }

    @Start
    public void start(Stage stage) {
        VBox root = new VBox(richTextArea);
        root.setStyle("-fx-padding: 10px; -fx-background-color: white;");
        scene = new Scene(root, 620, 420);
        stage.setScene(scene);
        stage.setTitle("Table Selection Highlight Test");
        stage.show();
    }

    @Test
    @DisplayName("Table selection: the highlight matches the text layout of the cell")
    public void selectionHighlightMatchesTheCellLayout(FxRobot robot) {
        int cellStart = openTableDocument();
        assertTrue(cellStart >= 0, "The document must contain the table paragraph");

        // The selection is set before the table is rendered, so the cell is created while the
        // selection is already active.
        selectRange(cellStart, cellStart + SELECTED_RANGE.length());
        waitForFxEvents();

        // Scroll away and back: the VirtualFlow recycles the cell, so the cell of the table is
        // created again while the selection is already active. This is the situation of the bug:
        // the cell is created, but not laid out yet.
        scrollToParagraph(0);
        waitForFxEvents();
        scrollToTable(cellStart);
        waitForFxEvents();

        HighlightSnapshot snapshot = snapshotHighlight();
        assertTrue(snapshot.rendered(), "The selection of the table cell must be rendered: " + snapshot);

        // Only the cell that contains the selection may be marked.
        assertEquals(1, snapshot.markedCells(), "Exactly one table cell must be marked: " + snapshot);
        assertTrue(snapshot.cellText().startsWith(SELECTED_CELL_TEXT),
                "The marked cell must be the cell '" + SELECTED_CELL_TEXT + "', but was '"
                        + snapshot.cellText() + "': " + snapshot);

        // The drawn highlight has to be the shape of the selected range of the laid out text
        // flow: one rectangle that covers exactly the selected word and stays in the row.
        PathElement[] expected = snapshot.textFlow().rangeShape(0, SELECTED_RANGE.length());
        assertTrue(expected.length > 0, "The text flow must provide a shape for the range: " + snapshot);
        Bounds expectedBounds = boundsOf(expected);
        Bounds actualBounds = snapshot.selectionPath().getBoundsInLocal();

        assertEquals(expectedBounds.getWidth(), actualBounds.getWidth(), TOLERANCE_PX,
                "The highlight must cover exactly '" + SELECTED_RANGE + "': " + snapshot);
        assertEquals(expectedBounds.getHeight(), actualBounds.getHeight(), TOLERANCE_PX,
                "The highlight must stay on a single line: " + snapshot);
        // The shape includes the leading of the line, so it is slightly higher than the text
        // flow. It must however stay within one line - the bug produced one rectangle per
        // character, i.e. a shape that was seven lines high.
        assertTrue(actualBounds.getHeight() <= snapshot.textFlow().getLayoutBounds().getHeight() + 3.0,
                "The highlight must stay inside the table row: " + snapshot);
    }

    // ==================== MEASUREMENT ====================

    /**
     * The measured state of the selection highlight of the table cells: the path that is drawn,
     * the text flow it belongs to and the text of the marked cell.
     */
    private record HighlightSnapshot(boolean rendered, Path selectionPath, TextFlow textFlow,
                                     String cellText, int markedCells) {

        @Override
        public String toString() {
            return "marked cells: " + markedCells
                    + ", cell text: '" + cellText + "'"
                    + (rendered
                            ? ", highlight bounds: " + selectionPath.getBoundsInLocal()
                              + ", path elements: " + selectionPath.getElements().size()
                              + ", text flow: " + textFlow.getLayoutBounds()
                            : ", no highlight rendered");
        }
    }

    /** Reads the highlight of the table cells on the FX thread. */
    private HighlightSnapshot snapshotHighlight() {
        HighlightSnapshot[] holder = new HighlightSnapshot[1];
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            List<Node> layers = scene.getRoot().lookupAll(".table .layer").stream().toList();
            List<Node> marked = layers.stream()
                    .filter(layer -> {
                        Path selection = (Path) layer.lookup(".selection");
                        return selection != null && !selection.getElements().isEmpty();
                    })
                    .toList();
            if (marked.isEmpty()) {
                holder[0] = new HighlightSnapshot(false, null, null, "", 0);
            } else {
                Node layer = marked.get(0);
                Path selection = (Path) layer.lookup(".selection");
                TextFlow textFlow = (TextFlow) layer.lookup(".text-flow");
                String cellText = textFlow.getChildren().stream()
                        .filter(Text.class::isInstance)
                        .map(Text.class::cast)
                        .map(Text::getText)
                        .collect(Collectors.joining());
                holder[0] = new HighlightSnapshot(true, selection, textFlow, cellText, marked.size());
            }
            latch.countDown();
        });
        awaitFxThread(latch, "the highlight snapshot");
        return holder[0];
    }

    private static Bounds boundsOf(PathElement[] elements) {
        Path path = new Path();
        path.getElements().setAll(elements);
        return path.getBoundsInLocal();
    }

    // ==================== SCENE SETUP ====================

    /**
     * Opens a document with filler paragraphs and a table with text in its cells and returns the
     * start of the table paragraph.
     */
    private int openTableDocument() {
        StringBuilder cells = new StringBuilder();
        for (int row = 0; row < TABLE_ROWS; row++) {
            for (int column = 0; column < TABLE_COLUMNS; column++) {
                if (cells.length() > 0) {
                    cells.append(TextBuffer.ZERO_WIDTH_TABLE_SEPARATOR);
                }
                cells.append(row == 2 && column == 0 ? SELECTED_CELL_TEXT : "Cell " + row + column);
            }
        }
        // The filler paragraphs keep the table outside of the visible area, the cell is only
        // created when the table is scrolled into view.
        String filler = "Fuelltext\n".repeat(60);
        ParagraphDecoration paragraphDecoration = ParagraphDecoration.builder()
                .presets()
                .tableDecoration(new TableDecoration(TABLE_ROWS, TABLE_COLUMNS))
                .build();
        DecorationModel tableModel = new DecorationModel(filler.length(), cells.length(),
                TextDecoration.builder().presets().build(), paragraphDecoration);
        DecorationModel fillerModel = new DecorationModel(0, filler.length(),
                TextDecoration.builder().presets().build(),
                ParagraphDecoration.builder().presets().build());
        int[] tableStart = {-1};
        int cellOffset = cells.indexOf(SELECTED_CELL_TEXT);
        runOnFxThread(() -> {
            Action openAction = richTextArea.getActionFactory().open(
                    new Document(filler + cells, List.of(fillerModel, tableModel), 0));
            openAction.execute(new ActionEvent());
        });
        waitForFxEvents();
        runOnFxThread(() -> {
            RichTextAreaViewModel viewModel = viewModel();
            viewModel.getParagraphList().stream()
                    .filter(paragraph -> paragraph.getDecoration() instanceof ParagraphDecoration decoration
                            && decoration.hasTableDecoration())
                    .findFirst()
                    .ifPresent(paragraph -> tableStart[0] = paragraph.getStart() + cellOffset);
        });
        waitForFxEvents();
        return tableStart[0];
    }

    private void selectRange(int start, int end) {
        runOnFxThread(() -> viewModel().setSelection(
                new com.gluonhq.richtextarea.Selection(start, end)));
    }

    /** Scrolls the paragraph with the given position into the visible area. */
    private void scrollToTable(int position) {
        scrollToParagraph(position);
    }

    /** Scrolls the paragraph with the given position into the visible area. */
    private void scrollToParagraph(int position) {
        runOnFxThread(() -> {
            RichTextAreaViewModel viewModel = viewModel();
            int index = 0;
            int target = 0;
            for (var paragraph : viewModel.getParagraphList()) {
                if (paragraph.getStart() <= position && position < paragraph.getEnd()) {
                    target = index;
                }
                index++;
            }
            Set<Node> flows = richTextArea.lookupAll(".virtual-flow");
            for (Node node : flows) {
                if (node instanceof javafx.scene.control.skin.VirtualFlow<?> flow) {
                    flow.scrollTo(target);
                }
            }
        });
        waitForFxEvents();
    }

    private RichTextAreaViewModel viewModel() {
        return ((RichTextAreaSkin) richTextArea.getSkin()).getViewModel();
    }

    // ==================== THREAD HELPERS ====================

    private void runOnFxThread(Runnable runnable) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        });
        awaitFxThread(latch, "the requested action");
    }

    private void awaitFxThread(CountDownLatch latch, String context) {
        try {
            assertTrue(latch.await(3, TimeUnit.SECONDS), "The FX thread did not execute " + context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for the FX thread: " + e.getMessage());
        }
    }
}
