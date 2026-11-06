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
package com.gluonhq.richtextarea.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A default implementation of {@link Field.FieldContext} that provides
 * field evaluation context for document fields like page numbers, dates,
 * and document properties.
 *
 * <p>This implementation allows setting all context values and is used
 * by the RichTextArea to evaluate fields in headers, footers, and the
 * document body.</p>
 */
public class DefaultFieldContext implements Field.FieldContext, Serializable {

    private int currentPageNumber = 1;
    private int pageCount = 1;
    private LocalDate currentDate = LocalDate.now();
    private LocalTime currentTime = LocalTime.now();
    private String documentTitle = "";
    private String documentSubject = "";
    private String documentAuthor = "";
    private String fileName = "";
    private String templateName = "";
    private String userName = System.getProperty("user.name", "");
    private final Map<String, String> variables = new HashMap<>();

    /**
     * Creates a new DefaultFieldContext with default values.
     */
    public DefaultFieldContext() {
    }

    /**
     * Creates a new DefaultFieldContext with the given page count.
     *
     * @param pageCount the total page count
     */
    public DefaultFieldContext(int pageCount) {
        this.pageCount = pageCount;
    }

    @Override
    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

    /**
     * Sets the current page number.
     *
     * @param currentPageNumber the current page number (1-based)
     * @return this for chaining
     */
    public DefaultFieldContext setCurrentPageNumber(int currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
        return this;
    }

    @Override
    public int getPageCount() {
        return pageCount;
    }

    /**
     * Sets the total page count.
     *
     * @param pageCount the total page count
     * @return this for chaining
     */
    public DefaultFieldContext setPageCount(int pageCount) {
        this.pageCount = pageCount;
        return this;
    }

    @Override
    public LocalDate getCurrentDate() {
        return currentDate;
    }

    /**
     * Sets the current date.
     *
     * @param currentDate the current date
     * @return this for chaining
     */
    public DefaultFieldContext setCurrentDate(LocalDate currentDate) {
        this.currentDate = Objects.requireNonNull(currentDate);
        return this;
    }

    @Override
    public LocalTime getCurrentTime() {
        return currentTime;
    }

    /**
     * Sets the current time.
     *
     * @param currentTime the current time
     * @return this for chaining
     */
    public DefaultFieldContext setCurrentTime(LocalTime currentTime) {
        this.currentTime = Objects.requireNonNull(currentTime);
        return this;
    }

    @Override
    public String getDocumentTitle() {
        return documentTitle;
    }

    /**
     * Sets the document title.
     *
     * @param documentTitle the document title
     * @return this for chaining
     */
    public DefaultFieldContext setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle != null ? documentTitle : "";
        return this;
    }

    @Override
    public String getDocumentSubject() {
        return documentSubject;
    }

    /**
     * Sets the document subject.
     *
     * @param documentSubject the document subject
     * @return this for chaining
     */
    public DefaultFieldContext setDocumentSubject(String documentSubject) {
        this.documentSubject = documentSubject != null ? documentSubject : "";
        return this;
    }

    @Override
    public String getDocumentAuthor() {
        return documentAuthor;
    }

    /**
     * Sets the document author.
     *
     * @param documentAuthor the document author
     * @return this for chaining
     */
    public DefaultFieldContext setDocumentAuthor(String documentAuthor) {
        this.documentAuthor = documentAuthor != null ? documentAuthor : "";
        return this;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the file name.
     *
     * @param fileName the file name
     * @return this for chaining
     */
    public DefaultFieldContext setFileName(String fileName) {
        this.fileName = fileName != null ? fileName : "";
        return this;
    }

    @Override
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Sets the template name.
     *
     * @param templateName the template name
     * @return this for chaining
     */
    public DefaultFieldContext setTemplateName(String templateName) {
        this.templateName = templateName != null ? templateName : "";
        return this;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user name.
     *
     * @param userName the user name
     * @return this for chaining
     */
    public DefaultFieldContext setUserName(String userName) {
        this.userName = userName != null ? userName : "";
        return this;
    }

    /**
     * Sets a document variable.
     *
     * @param name the variable name
     * @param value the variable value
     * @return this for chaining
     */
    public DefaultFieldContext setVariable(String name, String value) {
        variables.put(name, value);
        return this;
    }

    /**
     * Removes a document variable.
     *
     * @param name the variable name
     * @return this for chaining
     */
    public DefaultFieldContext removeVariable(String name) {
        variables.remove(name);
        return this;
    }

    @Override
    public String getVariable(String name) {
        return variables.get(name);
    }

    /**
     * Returns all variables.
     *
     * @return a map of variable names to values
     */
    public Map<String, String> getVariables() {
        return new HashMap<>(variables);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultFieldContext that = (DefaultFieldContext) o;
        return currentPageNumber == that.currentPageNumber &&
                pageCount == that.pageCount &&
                Objects.equals(currentDate, that.currentDate) &&
                Objects.equals(currentTime, that.currentTime) &&
                Objects.equals(documentTitle, that.documentTitle) &&
                Objects.equals(documentSubject, that.documentSubject) &&
                Objects.equals(documentAuthor, that.documentAuthor) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(templateName, that.templateName) &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentPageNumber, pageCount, currentDate, currentTime,
                documentTitle, documentSubject, documentAuthor, fileName,
                templateName, userName, variables);
    }
}
