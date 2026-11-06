/*
 * Copyright (C) 2026 Rainer Garbotz
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gluonhq.richtextarea.viewmodel;

import com.gluonhq.richtextarea.model.Decoration;
import com.gluonhq.richtextarea.model.DecorationModel;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.ImageDecoration;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.TextBuffer;
import com.gluonhq.richtextarea.model.TextDecoration;
import com.gluonhq.richtextarea.Selection;

/** Replaces a document while keeping a single command on the editor undo stack. */
final class ReplaceDocumentCmd extends AbstractEditCmd {
    private final Document document;
    private final Selection resultSelection;
    private final int rangeStart;
    private final int rangeEnd;
    private int insertionPosition;
    private int operations;

    ReplaceDocumentCmd(Document document) { this(document, Selection.UNDEFINED); }

    ReplaceDocumentCmd(Document document, Selection selection) {
        this(document, selection, 0, -1);
    }

    ReplaceDocumentCmd(Document document, Selection selection, int start, int end) {
        this.document = document;
        this.resultSelection = selection;
        this.rangeStart = start;
        this.rangeEnd = end;
    }

    @Override public void doRedo(RichTextAreaViewModel model) {
        TextBuffer buffer = model.getTextBuffer();
        operations = 0;
        model.clearSelection();
        model.setCaretPosition(-1);
        try {
            int start = buffer.getInternalPosition(rangeStart);
            int end = rangeEnd < 0 ? buffer.getTextLength() : buffer.getInternalPosition(rangeEnd);
            insertionPosition = start;
            if (end > start) {
                buffer.delete(start, end - start);
                operations++;
            }
            int cursor = 0;
            for (DecorationModel run : document.getDecorations()) {
                if (cursor < run.getStart()) append(buffer, document.getText().substring(cursor, run.getStart()),
                        TextDecoration.builder().presets().build(), ParagraphDecoration.builder().presets().build());
                if (run.getLength() > 0) {
                    append(buffer, document.getText().substring(run.getStart(), run.getStart() + run.getLength()),
                            run.getDecoration(), run.getParagraphDecoration());
                } else if (run.getParagraphDecoration() != null) {
                    // Empty final paragraphs can still carry structure, for example
                    // a newly inserted one-cell table. Keep its undoable decoration.
                    buffer.decorate(insertionPosition, insertionPosition, run.getParagraphDecoration());
                    operations++;
                }
                cursor = run.getStart() + run.getLength();
            }
            if (cursor < document.getText().length()) append(buffer, document.getText().substring(cursor),
                    TextDecoration.builder().presets().build(), ParagraphDecoration.builder().presets().build());
            int caret = Math.max(0, Math.min(document.getCaretPosition(), buffer.getText().length()));
            model.setCaretPosition(buffer.getInternalPosition(caret));
            model.setSelection(resultSelection.isDefined()
                    ? buffer.getInternalSelection(resultSelection) : Selection.UNDEFINED);
        } catch (RuntimeException failure) {
            while (operations > 0) { buffer.undo(); operations--; }
            restoreContext(model);
            model.attach();
            throw failure;
        }
    }

    private void append(TextBuffer buffer, String text, Decoration decoration, ParagraphDecoration paragraph) {
        int start = insertionPosition;
        int previousLength = buffer.getTextLength();
        if (decoration instanceof ImageDecoration) {
            buffer.decorate(start, start, decoration);
            operations++;
        } else {
            // Insertions inherit the current typing style. Use the fragment's style
            // so a neighboring link cannot spread into the replaced paragraphs.
            if (decoration instanceof TextDecoration textDecoration) {
                buffer.setDecorationAtCaret(textDecoration);
            }
            if (start == buffer.getTextLength()) buffer.append(text);
            else buffer.insert(text, start);
            operations++;
            buffer.decorate(start, start + buffer.getTextLength() - previousLength, decoration);
            operations++;
        }
        if (paragraph != null) {
            buffer.decorate(start, start + buffer.getTextLength() - previousLength, paragraph);
            operations++;
        }
        insertionPosition += buffer.getTextLength() - previousLength;
    }

    @Override public void doUndo(RichTextAreaViewModel model) {
        model.setCaretPosition(-1);
        for (int i = 0; i < operations; i++) model.getTextBuffer().undo();
    }
}
