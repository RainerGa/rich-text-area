/*
 * Copyright (c) 2022, 2024, Gluon
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
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.richtextarea.samples;

import com.gluonhq.richtextarea.DocumentView;
import com.gluonhq.richtextarea.RichTextArea;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * Adds a shared switch between the regular flowing editor and the paginated view.
 */
public final class ViewModeSwitcher extends BorderPane {

    public enum ViewMode {
        FLOW,
        PAGES
    }

    private final RichTextArea editor;
    private final DocumentView documentView = new DocumentView();
    private final StackPane viewport = new StackPane();
    private final EditorPresentation originalPresentation;
    private final ToggleButton flowButton = new ToggleButton("Flow view");
    private final ToggleButton pagesButton = new ToggleButton("Page view");
    private final EventHandler<KeyEvent> flowTypedFilter = this::forwardFlowTypedInput;
    private Scene flowFilterScene;
    private boolean showingPages;

    public ViewModeSwitcher(RichTextArea editor) {
        this(editor, ViewMode.FLOW);
    }

    public ViewModeSwitcher(RichTextArea editor, ViewMode initialMode) {
        this.editor = editor;
        this.originalPresentation = EditorPresentation.capture(editor);
        documentView.setPageFormat(editor.getPageFormat());
        documentView.setHeadersFooters(editor.getHeadersFooters());
        editor.pageFormatProperty().addListener(
                (obs, oldFormat, newFormat) -> documentView.setPageFormat(newFormat));
        editor.headersFootersProperty().addListener(
                (obs, oldHeadersFooters, newHeadersFooters) ->
                        documentView.setHeadersFooters(newHeadersFooters));

        ToggleGroup viewModes = new ToggleGroup();
        flowButton.setToggleGroup(viewModes);
        pagesButton.setToggleGroup(viewModes);
        flowButton.setOnAction(event -> showMode(ViewMode.FLOW));
        pagesButton.setOnAction(event -> showMode(ViewMode.PAGES));
        HBox modeControls = new HBox(6, flowButton, pagesButton);
        modeControls.setAlignment(Pos.CENTER_RIGHT);
        modeControls.setPadding(new Insets(4, 8, 4, 8));
        modeControls.getStyleClass().add("view-mode-controls");

        viewport.getStyleClass().add("view-mode-viewport");
        setTop(modeControls);
        setCenter(viewport);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (flowFilterScene != null) {
                flowFilterScene.removeEventFilter(KeyEvent.KEY_TYPED, flowTypedFilter);
                flowFilterScene = null;
            }
            if (newScene != null && !showingPages) {
                flowFilterScene = newScene;
                flowFilterScene.addEventFilter(KeyEvent.KEY_TYPED, flowTypedFilter);
            }
        });
        showMode(initialMode);
    }

    public DocumentView getDocumentView() {
        return documentView;
    }

    public void showMode(ViewMode mode) {
        if (mode == ViewMode.PAGES) {
            if (!showingPages) {
                detachFlowTypedFilter();
                viewport.getChildren().remove(editor);
                documentView.setEditor(editor);
                viewport.getChildren().setAll(documentView);
                showingPages = true;
            }
            pagesButton.setSelected(true);
        } else {
            if (showingPages) {
                viewport.getChildren().clear();
                documentView.setEditor(null);
                originalPresentation.restore(editor);
                showingPages = false;
            }
            viewport.getChildren().setAll(editor);
            attachFlowTypedFilter();
            flowButton.setSelected(true);
        }
        editor.requestFocus();
    }

    private void attachFlowTypedFilter() {
        Scene scene = getScene();
        if (scene != null && flowFilterScene == null) {
            flowFilterScene = scene;
            flowFilterScene.addEventFilter(KeyEvent.KEY_TYPED, flowTypedFilter);
        }
    }

    private void detachFlowTypedFilter() {
        if (flowFilterScene != null) {
            flowFilterScene.removeEventFilter(KeyEvent.KEY_TYPED, flowTypedFilter);
            flowFilterScene = null;
        }
    }

    private void forwardFlowTypedInput(KeyEvent event) {
        if (showingPages || !editor.isFocused() || !editor.isEditable()
                || event.isControlDown() || event.isMetaDown()) {
            return;
        }
        String character = event.getCharacter();
        if (character == null || character.isEmpty() || KeyEvent.CHAR_UNDEFINED.equals(character)) {
            return;
        }
        for (int i = 0; i < character.length(); i++) {
            if (Character.isISOControl(character.charAt(i))) {
                return;
            }
        }
        editor.getActionFactory().insertText(character).execute(new ActionEvent());
        event.consume();
    }

    private record EditorPresentation(boolean managed, boolean visible, double opacity,
                                      boolean mouseTransparent, boolean focusTraversable,
                                      double minWidth, double minHeight,
                                      double prefWidth, double prefHeight,
                                      double maxWidth, double maxHeight) {

        static EditorPresentation capture(RichTextArea editor) {
            return new EditorPresentation(editor.isManaged(), editor.isVisible(), editor.getOpacity(),
                    editor.isMouseTransparent(), editor.isFocusTraversable(),
                    editor.getMinWidth(), editor.getMinHeight(),
                    editor.getPrefWidth(), editor.getPrefHeight(),
                    editor.getMaxWidth(), editor.getMaxHeight());
        }

        void restore(RichTextArea editor) {
            editor.setManaged(managed);
            editor.setVisible(visible);
            editor.setOpacity(opacity);
            editor.setMouseTransparent(mouseTransparent);
            editor.setFocusTraversable(focusTraversable);
            editor.setMinSize(minWidth, minHeight);
            editor.setPrefSize(prefWidth, prefHeight);
            editor.setMaxSize(maxWidth, maxHeight);
        }
    }
}
