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
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  See the
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

import com.gluonhq.richtextarea.Tools;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A Document is the basic model that contains all the information required for the {@link com.gluonhq.richtextarea.RichTextArea}
 * control, in order to render all the rich content, including decorated text, images and other non-text objects.
 *
 * A document is basically a string with the full raw text, and a list of {@link DecorationModel} that contain the text and
 * paragraph decorations for one or more fragments of the text, where a fragment can be defined as the longest
 * substring of the text that shares the same text and paragraph decorations.
 *
 * The document can be serialized/deserialized, and therefore the full raw text should contain all characters that define it,
 * whether these can be rendered by a text editor or not. All the indices used by the Document model (like start/length of
 * decorations or caret position) should refer to their position within the full raw text.
 *
 * The document also contains a {@link PageFormat} that defines the physical page properties for page layout,
 * and {@link HeaderFooter} instances for headers and footers.
 */
public class Document implements Serializable {

    private final String text;
    private final List<DecorationModel> decorationList;
    private final int caretPosition;
    private final PageFormat pageFormat;
    private final java.util.List<HeaderFooter> headersFooters;

    public Document() {
        this("");
    }

    public Document(String text) {
        this(text, 0);
    }

    public Document(String text, int caretPosition) {
        this(text,
                List.of(DecorationModel.createDefaultDecorationModel(text.length())),
                caretPosition,
                PageFormat.builder().build(),
                java.util.List.of());
    }

    public Document(String text, List<DecorationModel> decorationList, int caretPosition) {
        this(text, decorationList, caretPosition, PageFormat.builder().build(), java.util.List.of());
    }

    public Document(String text, List<DecorationModel> decorationList, int caretPosition, PageFormat pageFormat) {
        this(text, decorationList, caretPosition, pageFormat, java.util.List.of());
    }

    public Document(String text, List<DecorationModel> decorationList, int caretPosition, PageFormat pageFormat, java.util.List<HeaderFooter> headersFooters) {
        this.text = text;
        this.decorationList = decorationList;
        this.caretPosition = caretPosition;
        this.pageFormat = pageFormat != null ? pageFormat : PageFormat.builder().build();
        this.headersFooters = headersFooters != null ? headersFooters : java.util.List.of();
    }

    /**
     * Returns the full raw text of the document.
     *
     * This can include unicode characters that may or may not be rendered by an editor
     *
     * @return a string with the full text of the document
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the list of {@link DecorationModel} that define the fragments of text
     * that share the same decorations.
     *
     * The range of each {@link Decoration} is defined based on the full raw text.
     *
     * @return the list of {@link DecorationModel}
     */
    public List<DecorationModel> getDecorations() {
        return decorationList;
    }

    /**
     * Returns the caret position in order to restore the caret when the document
     * is opened, in terms of the full raw text, and not the rendered or visible text.
     *
     * @return the caret position when document was saved
     */
    public int getCaretPosition() {
        return caretPosition;
    }

    /**
     * Returns the page format for this document.
     *
     * @return the page format
     */
    public PageFormat getPageFormat() {
        return pageFormat;
    }

    /**
     * Returns the list of headers and footers for this document.
     *
     * @return the list of headers and footers
     */
    public java.util.List<HeaderFooter> getHeadersFooters() {
        return headersFooters;
    }

    /**
     * Returns the header for the given scope, or null if not found.
     *
     * @param scope the scope
     * @return the header, or null
     */
    public HeaderFooter getHeader(HeaderFooter.Scope scope) {
        return headersFooters.stream()
                .filter(hf -> hf.getType() == HeaderFooter.Type.HEADER && hf.getScope() == scope)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the footer for the given scope, or null if not found.
     *
     * @param scope the scope
     * @return the footer, or null
     */
    public HeaderFooter getFooter(HeaderFooter.Scope scope) {
        return headersFooters.stream()
                .filter(hf -> hf.getType() == HeaderFooter.Type.FOOTER && hf.getScope() == scope)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(text, document.text) &&
                Objects.equals(decorationList, document.decorationList) &&
                Objects.equals(pageFormat, document.pageFormat) &&
                Objects.equals(headersFooters, document.headersFooters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, decorationList, caretPosition, pageFormat, headersFooters);
    }

    @Override
    public String toString() {
        return "Document{" +
                "text='" + Tools.formatTextWithAnchors(text) + '\'' +
                ", decorationList=" + (decorationList == null ? "null" : "{" +
                    decorationList.stream().map(decorationModel -> " - " + decorationModel.toString()).collect(Collectors.joining("\n", "\n", ""))) +
                "\n}, caretPosition=" + caretPosition +
                ", pageFormat=" + pageFormat +
                ", headersFooters=" + headersFooters +
                '}';
    }
}