/*
 * Copyright (c) 2023, Gluon
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
package com.gluonhq.richtextarea.model;

import com.gluonhq.richtextarea.Selection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests fuer die Umrechnung von Positionen des exportierbaren Textes in die Positionen des
 * internen Textes.
 *
 * <p>Die Umrechnung ist wichtig, wenn ein Textbereich ersetzt wird: die Auswahl des Editors
 * wird in die internen Positionen umgerechnet, damit der richtige Bereich geloescht wird.
 */
public class PieceTableInternalSelectionTests {

    @Test
    @DisplayName("Internal selection of a single piece")
    public void internalSelectionOfSinglePiece() {
        PieceTable pt = new PieceTable(new Document("Hello brave new world"));
        Selection selection = pt.getInternalSelection(new Selection(6, 11));
        Assertions.assertEquals(6, selection.getStart());
        Assertions.assertEquals(11, selection.getEnd());
    }

    @Test
    @DisplayName("Internal selection after the piece was split by an insertion")
    public void internalSelectionWithSplitPiece() {
        String text = "Hello brave new world";
        PieceTable pt = new PieceTable(new Document(text));
        // Die Einfuegung teilt das Textstueck in drei Teile: der Rest des Originals,
        // der eingefuegte Text und der Rest des Originals.
        pt.insert("XX", 11);
        Assertions.assertEquals("Hello braveXX new world", pt.getText());

        // "brave" liegt vor der Einfuegestelle, "new" danach.
        Selection before = pt.getInternalSelection(new Selection(6, 11));
        Assertions.assertEquals(6, before.getStart());
        Assertions.assertEquals(11, before.getEnd());

        Selection after = pt.getInternalSelection(new Selection(15, 18));
        Assertions.assertEquals(15, after.getStart());
        Assertions.assertEquals(18, after.getEnd());
    }

    @Test
    @DisplayName("Internal selection after a deletion split the piece")
    public void internalSelectionWithDeletedPiece() {
        String text = "Hello brave new world";
        PieceTable pt = new PieceTable(new Document(text));
        pt.delete(6, 6); // loescht "brave "
        Assertions.assertEquals("Hello new world", pt.getText());

        Selection selection = pt.getInternalSelection(new Selection(6, 9));
        Assertions.assertEquals(6, selection.getStart());
        Assertions.assertEquals(9, selection.getEnd());
    }

    @Test
    @DisplayName("Internal selection with several decorations")
    public void internalSelectionWithSeveralDecorations() {
        // Ein Dokument mit mehreren Dekorationen, wie es eine Tabelle erzeugt:
        // jede Zelle ist eine eigene Dekoration.
        String text = "Zelle1\u200bZelle2\u200bZelle3";
        Document document = new Document(text, List.of(
                new DecorationModel(0, 6, TextDecoration.builder().presets().build(), ParagraphDecoration.builder().presets().build()),
                new DecorationModel(6, 1, TextDecoration.builder().presets().build(), ParagraphDecoration.builder().presets().build()),
                new DecorationModel(7, 6, TextDecoration.builder().presets().build(), ParagraphDecoration.builder().presets().build()),
                new DecorationModel(13, 1, TextDecoration.builder().presets().build(), ParagraphDecoration.builder().presets().build()),
                new DecorationModel(14, 6, TextDecoration.builder().presets().build(), ParagraphDecoration.builder().presets().build())), 0);

        PieceTable pt = new PieceTable(document);

        // Ein Wort in der zweiten Zelle ersetzen: das Textstueck wird geteilt.
        pt.delete(7, 6); // loescht "Zelle2"
        pt.insert("Zwei", 7);
        Assertions.assertEquals("Zelle1\u200bZwei\u200bZelle3", pt.getText());

        // Die Auswahl "Zelle1" darf nicht in die zweite Zelle hineinreichen.
        Selection selection = pt.getInternalSelection(new Selection(0, 6));
        Assertions.assertEquals(0, selection.getStart());
        Assertions.assertEquals(6, selection.getEnd());

        // Die Auswahl "Zelle3" beginnt hinter dem Trenner.
        Selection last = pt.getInternalSelection(new Selection(12, 18));
        Assertions.assertEquals(12, last.getStart());
        Assertions.assertEquals(18, last.getEnd());
    }

    @Test
    @DisplayName("Internal position after the piece was split")
    public void internalPositionWithSplitPiece() {
        PieceTable pt = new PieceTable(new Document("Hello brave new world"));
        pt.insert("XX", 11);
        // Ein Textstück darf die genaue Position nicht auf sein Ende verschieben.
        Assertions.assertEquals(6, pt.getInternalPosition(6));
        Assertions.assertEquals(13, pt.getInternalPosition(13));
    }
    @Test
    void exactPositionsRemainStableAcrossTextPieceSplitsAndDeletions() {
        PieceTable pt = new PieceTable(new Document("Hello brave world"));
        for (int i = 0; i <= pt.getTextLength(); i++) Assertions.assertEquals(i, pt.getInternalPosition(i));
        pt.insert("XX", 6); pt.delete(10, 2);
        for (int i = 0; i <= pt.getTextLength(); i++) Assertions.assertEquals(i, pt.getInternalPosition(i));
        Assertions.assertEquals(-1, pt.getInternalPosition(-1));
        Assertions.assertEquals(-1, pt.getInternalPosition(pt.getTextLength() + 1));
        Assertions.assertEquals(0, new PieceTable(new Document("")).getInternalPosition(0));
    }

    @Test
    void emojiPositionsAreAtomicAndFollowingTextHasExactOffsets() {
        PieceTable pt = new PieceTable(new Document("😀 tail"));
        Assertions.assertEquals(0, pt.getInternalPosition(0));
        Assertions.assertEquals(0, pt.getInternalPosition(1));
        Assertions.assertEquals(1, pt.getInternalPosition(2));
        Assertions.assertEquals(3, pt.getInternalPosition(4));
        Assertions.assertEquals(6, pt.getInternalPosition(7));
    }

    @Test
    void imageUrlPositionsAreAtomicAndFollowingTextHasExactOffsets() {
        String url = "local-image.png";
        PieceTable pt = new PieceTable(new Document(" tail"));
        pt.decorate(0, 0, new ImageDecoration(url));
        Assertions.assertEquals(url + " tail", pt.getText());
        Assertions.assertEquals(0, pt.getInternalPosition(0));
        Assertions.assertEquals(0, pt.getInternalPosition(url.length() / 2));
        Assertions.assertEquals(1, pt.getInternalPosition(url.length()));
        Assertions.assertEquals(3, pt.getInternalPosition(url.length() + 2));
        Assertions.assertEquals(6, pt.getInternalPosition(url.length() + 5));
    }

}
