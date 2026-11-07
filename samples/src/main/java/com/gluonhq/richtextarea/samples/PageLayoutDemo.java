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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.richtextarea.samples;

import com.gluonhq.richtextarea.DocumentView;
import com.gluonhq.richtextarea.RichTextArea;
import com.gluonhq.richtextarea.Selection;
import com.gluonhq.richtextarea.action.Action;
import com.gluonhq.richtextarea.action.DecorateAction;
import com.gluonhq.richtextarea.model.DecorationModel;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.Field;
import com.gluonhq.richtextarea.model.FieldDecoration;
import com.gluonhq.richtextarea.model.Fields;
import com.gluonhq.richtextarea.model.HeaderFooter;
import com.gluonhq.richtextarea.model.PageFormat;
import com.gluonhq.richtextarea.model.Paragraph;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import com.gluonhq.richtextarea.model.Style;
import com.gluonhq.richtextarea.model.StyleRegistry;
import com.gluonhq.richtextarea.model.TextDecoration;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.gluonhq.richtextarea.model.ParagraphDecoration.GraphicType.BULLETED_LIST;

/**
 * This demo showcases the new page layout features of the RichTextArea control,
 * including:
 * - Page Format (size, orientation, margins)
 * - Headers and Footers
 * - Dynamic Fields (page numbers, date, time, etc.)
 * - Styles with inheritance (similar to LibreOffice Writer)
 * - Manual Page Breaks
 *
 * The demo shows a true multi-page document view like Microsoft Word or LibreOffice Writer:
 * - Document consists of multiple pages stacked vertically
 * - Each page has paper background, margins, header, content area, footer
 * - Text flows automatically across pages
 * - Only the document-level scrollbar is visible
 * - Pages have visible gaps between them
 */
public class PageLayoutDemo extends Application {

    private static final String SAMPLE_TEXT = "Page Layout Demo\n\n" +
            "This document demonstrates the new page layout features of the RichTextArea control.\n\n" +
            "Click the buttons below to experiment with different page formats, headers, footers, and fields.\n\n" +
            "Notice how the document adapts to the page settings you choose.\n\n" +
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n\n" +
            "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.\n\n" +
            "At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident, similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio. Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat facere possimus, omnis voluptas assumenda est, omnis dolor repellendus. Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae. Itaque earum rerum hic tenetur a sapiente delectus, ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat.";

    private final RichTextArea editor = new RichTextArea();
    private final ViewModeSwitcher viewModeSwitcher =
            new ViewModeSwitcher(editor, ViewModeSwitcher.ViewMode.PAGES);
    private final DocumentView documentView = viewModeSwitcher.getDocumentView();
    private final StyleRegistry styleRegistry = StyleRegistry.createDefault();

    @Override
    public void start(Stage stage) {
        // Create initial document with page format
        PageFormat initialFormat = PageFormat.builder()
                .pageSize(PageFormat.PageSize.A4)
                .orientation(PageFormat.Orientation.PORTRAIT)
                .margins(new Insets(72, 72, 72, 72))
                .build();
        editor.setPageFormat(initialFormat);

        // Connect editor to document view (editor handles editing, document view handles display)
        // Load the sample text into the shared document buffer.
        documentView.setDocument(new Document(SAMPLE_TEXT));
        documentView.setPageFormat(initialFormat);
        documentView.setHeadersFooters(editor.getHeadersFooters());

        // Create UI for page layout controls
        VBox pageLayoutControls = createPageLayoutControls();
        VBox headerFooterControls = createHeaderFooterControls();
        VBox fieldControls = createFieldControls();
        VBox styleControls = createStyleControls();

        TabPane controls = new TabPane(
                createControlTab("Page Layout", pageLayoutControls),
                createControlTab("Headers & Footers", headerFooterControls),
                createControlTab("Fields", fieldControls),
                createControlTab("Styles", styleControls)
        );
        controls.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        controls.setPrefWidth(370);
        controls.setMinWidth(330);
        controls.setMaxWidth(400);

        // Info panel
        Label infoLabel = new Label("Page Layout Demo - True Multi-Page Document View");
        infoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label featureList = new Label("Click in the document to place the caret and type to edit. Features: Page Format | Headers & Footers | Dynamic Fields | Styles | Page Breaks | Multi-Page View");
        featureList.setWrapText(true);
        featureList.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        VBox infoPanel = new VBox(5, infoLabel, featureList);
        infoPanel.setPadding(new Insets(10, 10, 10, 10));
        infoPanel.setStyle("-fx-background-color: -fx-control-inner-background;");

        BorderPane root = new BorderPane();
        root.setTop(infoPanel);
        root.setCenter(viewModeSwitcher);
        root.setRight(controls);
        BorderPane.setMargin(documentView, new Insets(10));

        Scene scene = new Scene(root, 1200, 850);
        stage.setTitle("RichTextArea - Page Layout Demo (Multi-Page View)");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();

        editor.requestFocus();
    }

    private Tab createControlTab(String title, javafx.scene.Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setMinWidth(0);

        Tab tab = new Tab(title, scrollPane);
        tab.setClosable(false);
        return tab;
    }

    private VBox createPageLayoutControls() {
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10, 10, 10, 10));

        // Page Size
        Label pageSizeLabel = new Label("Page Size:");
        pageSizeLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<PageFormat.PageSize> pageSizeCombo = new ComboBox<>();
        pageSizeCombo.getItems().addAll(PageFormat.PageSize.values());
        pageSizeCombo.setValue(PageFormat.PageSize.A4);
        pageSizeCombo.setTooltip(new Tooltip("Select page size (A0-A6, Letter, Legal, etc.)"));

        // Orientation
        Label orientationLabel = new Label("Orientation:");
        orientationLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<PageFormat.Orientation> orientationCombo = new ComboBox<>();
        orientationCombo.getItems().addAll(PageFormat.Orientation.values());
        orientationCombo.setValue(PageFormat.Orientation.PORTRAIT);
        orientationCombo.setTooltip(new Tooltip("Select page orientation (Portrait or Landscape)"));

        // Margins
        Label marginsLabel = new Label("Margins (points):");
        marginsLabel.setStyle("-fx-font-weight: bold;");

        GridPane marginsGrid = new GridPane();
        marginsGrid.setHgap(10);
        marginsGrid.setVgap(5);

        Label topLabel = new Label("Top:");
        Spinner<Double> topSpinner = createMarginSpinner(72.0);
        Label rightLabel = new Label("Right:");
        Spinner<Double> rightSpinner = createMarginSpinner(72.0);
        Label bottomLabel = new Label("Bottom:");
        Spinner<Double> bottomSpinner = createMarginSpinner(72.0);
        Label leftLabel = new Label("Left:");
        Spinner<Double> leftSpinner = createMarginSpinner(72.0);

        marginsGrid.addRow(0, topLabel, topSpinner, rightLabel, rightSpinner);
        marginsGrid.addRow(1, bottomLabel, bottomSpinner, leftLabel, leftSpinner);

        // Apply Page Format button
        Button applyPageFormat = new Button("Apply Page Format");
        applyPageFormat.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        applyPageFormat.setOnAction(e -> {
            PageFormat format = PageFormat.builder()
                    .pageSize(pageSizeCombo.getValue())
                    .orientation(orientationCombo.getValue())
                    .margins(new Insets(
                            topSpinner.getValue(),
                            rightSpinner.getValue(),
                            bottomSpinner.getValue(),
                            leftSpinner.getValue()
                    ))
                    .build();
            editor.setPageFormat(format);
            documentView.setPageFormat(format);
        });

        // Page Break button
        Button insertPageBreak = new Button("Insert Page Break");
        insertPageBreak.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        insertPageBreak.setTooltip(new Tooltip("Insert a manual page break at the current caret position"));
        insertPageBreak.setOnAction(e -> {
            editor.getActionFactory().insertPageBreak().execute(new ActionEvent());
            // Trigger relayout after page break insertion
            Platform.runLater(() -> documentView.relayout());
        });

        // Zoom controls
        Label zoomLabel = new Label("Zoom:");
        zoomLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<Double> zoomCombo = new ComboBox<>();
        zoomCombo.getItems().addAll(0.5, 0.75, 1.0, 1.25, 1.5, 2.0);
        zoomCombo.setValue(1.0);
        zoomCombo.setOnAction(e -> {
            documentView.setZoom(zoomCombo.getValue());
        });
        zoomCombo.setTooltip(new Tooltip("Document zoom level"));

        // Page gap control
        Label pageGapLabel = new Label("Page Gap (px):");
        pageGapLabel.setStyle("-fx-font-weight: bold;");

        Spinner<Double> pageGapSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 20, 5));
        pageGapSpinner.setPrefWidth(80);
        pageGapSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            documentView.setPageGap(newVal);
        });
        pageGapSpinner.setTooltip(new Tooltip("Gap between pages"));

        // Show current page format info
        Label currentFormatLabel = new Label("Current Page Format:");
        currentFormatLabel.setStyle("-fx-font-weight: bold;");

        Label formatInfo = new Label();
        formatInfo.setWrapText(true);
        formatInfo.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        // Update format info when page format changes
        editor.pageFormatProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                formatInfo.setText(String.format(
                        "Size: %s, Orientation: %s, Margins: %.0f, %.0f, %.0f, %.0f pt",
                        newVal.getPageSize(),
                        newVal.getOrientation(),
                        newVal.getMargins().getTop(),
                        newVal.getMargins().getRight(),
                        newVal.getMargins().getBottom(),
                        newVal.getMargins().getLeft()
                ));
            }
        });

        // Initial update
        formatInfo.setText(String.format(
                "Size: %s, Orientation: %s, Margins: %.0f, %.0f, %.0f, %.0f pt",
                editor.getPageFormat().getPageSize(),
                editor.getPageFormat().getOrientation(),
                editor.getPageFormat().getMargins().getTop(),
                editor.getPageFormat().getMargins().getRight(),
                editor.getPageFormat().getMargins().getBottom(),
                editor.getPageFormat().getMargins().getLeft()
        ));

        // Page count info
        Label pageCountLabel = new Label("Pages: 1");
        pageCountLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-text-fill: #666;");

        // Update page count when document view relayouts
        documentView.totalPagesProperty().addListener((obs, oldVal, newVal) -> {
            pageCountLabel.setText("Pages: " + newVal);
        });

        controls.getChildren().addAll(
                pageSizeLabel, pageSizeCombo,
                orientationLabel, orientationCombo,
                marginsLabel, marginsGrid,
                new Separator(),
                applyPageFormat,
                new Separator(),
                insertPageBreak,
                new Separator(),
                zoomLabel, zoomCombo,
                pageGapLabel, pageGapSpinner,
                new Separator(),
                currentFormatLabel, formatInfo,
                pageCountLabel
        );

        return controls;
    }

    private VBox createHeaderFooterControls() {
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10, 10, 10, 10));

        Label title = new Label("Headers and Footers");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Header Footer Type
        Label typeLabel = new Label("Type:");
        typeLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<HeaderFooter.Type> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(HeaderFooter.Type.values());
        typeCombo.setValue(HeaderFooter.Type.HEADER);

        // Header Footer Scope
        Label scopeLabel = new Label("Scope:");
        scopeLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<HeaderFooter.Scope> scopeCombo = new ComboBox<>();
        scopeCombo.getItems().addAll(HeaderFooter.Scope.values());
        scopeCombo.setValue(HeaderFooter.Scope.ALL_PAGES);

        // Content
        Label contentLabel = new Label("Content:");
        contentLabel.setStyle("-fx-font-weight: bold;");

        TextField contentField = new TextField();
        contentField.setPromptText("Enter header/footer text (e.g., 'My Document')");

        // Height
        Label heightLabel = new Label("Height (points):");
        heightLabel.setStyle("-fx-font-weight: bold;");

        Spinner<Double> heightSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 144, 36, 1));
        heightSpinner.setTooltip(new Tooltip("Height of the header/footer area"));

        // Distance from edge
        Label distanceLabel = new Label("Distance from Edge (points):");
        distanceLabel.setStyle("-fx-font-weight: bold;");

        Spinner<Double> distanceSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 72, 18, 1));
        distanceSpinner.setTooltip(new Tooltip("Distance from the page edge"));

        // Apply button
        Button applyHeaderFooter = new Button("Apply Header/Footer");
        applyHeaderFooter.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        applyHeaderFooter.setOnAction(e -> {
            HeaderFooter hf = HeaderFooter.builder()
                    .type(typeCombo.getValue())
                    .scope(scopeCombo.getValue())
                    .content(contentField.getText())
                    .height(heightSpinner.getValue())
                    .distanceFromEdge(distanceSpinner.getValue())
                    .build();

            // Create a new document with the header/footer
            Document doc = editor.getDocument();
            java.util.List<HeaderFooter> headersFooters = new java.util.ArrayList<>(doc.getHeadersFooters());
            headersFooters.add(hf);

            Document newDoc = new Document(
                    doc.getText(),
                    doc.getDecorations(),
                    doc.getCaretPosition(),
                    doc.getPageFormat(),
                    headersFooters
            );

            editor.getActionFactory().open(newDoc).execute(new ActionEvent());
            documentView.setHeadersFooters(headersFooters);
            documentView.relayout();
        });

        // Clear button
        Button clearHeadersFooters = new Button("Clear All Headers/Footers");
        clearHeadersFooters.setStyle("-fx-base: #f44336; -fx-text-fill: white;");
        clearHeadersFooters.setOnAction(e -> {
            Document doc = editor.getDocument();
            Document newDoc = new Document(
                    doc.getText(),
                    doc.getDecorations(),
                    doc.getCaretPosition(),
                    doc.getPageFormat(),
                    java.util.List.of()
            );
            editor.getActionFactory().open(newDoc).execute(new ActionEvent());
            documentView.setHeadersFooters(java.util.List.of());
            documentView.relayout();
        });

        // Show current headers/footers
        Label currentLabel = new Label("Current Headers/Footers:");
        currentLabel.setStyle("-fx-font-weight: bold;");

        Label headersFootersInfo = new Label();
        headersFootersInfo.setWrapText(true);
        headersFootersInfo.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        // Update info when document changes
        editor.documentProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                java.util.List<HeaderFooter> hfs = newVal.getHeadersFooters();
                if (hfs.isEmpty()) {
                    headersFootersInfo.setText("No headers or footers defined");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (HeaderFooter hf : hfs) {
                        sb.append(String.format(
                                "%s (%s): '%s'\n",
                                hf.getType(),
                                hf.getScope(),
                                hf.getContent()
                        ));
                    }
                    headersFootersInfo.setText(sb.toString());
                }
            }
        });

        controls.getChildren().addAll(
                title,
                typeLabel, typeCombo,
                scopeLabel, scopeCombo,
                contentLabel, contentField,
                heightLabel, heightSpinner,
                distanceLabel, distanceSpinner,
                new Separator(),
                applyHeaderFooter,
                clearHeadersFooters,
                new Separator(),
                currentLabel, headersFootersInfo
        );

        return controls;
    }

    private VBox createFieldControls() {
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10, 10, 10, 10));

        Label title = new Label("Dynamic Fields");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label description = new Label("Insert dynamic fields that update automatically. Fields are similar to LibreOffice Writer fields.");
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Field type selection
        Label fieldTypeLabel = new Label("Field Type:");
        fieldTypeLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<FieldTypeOption> fieldTypeCombo = new ComboBox<>();
        fieldTypeCombo.getItems().addAll(FieldTypeOption.values());
        fieldTypeCombo.setValue(FieldTypeOption.PAGE_NUMBER);

        // Format options for some field types
        Label formatLabel = new Label("Format (optional):");
        formatLabel.setStyle("-fx-font-weight: bold;");

        TextField formatField = new TextField();
        formatField.setPromptText("e.g., 'Page #' for page number, 'dd/MM/yyyy' for date");

        // Insert Field button
        Button insertField = new Button("Insert Field");
        insertField.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        insertField.setTooltip(new Tooltip("Insert the selected field at the current caret position"));
        insertField.setOnAction(e -> {
            Field field = createFieldFromSelection(fieldTypeCombo.getValue(), formatField.getText());
            if (field != null) {
                editor.getActionFactory().insertField(field).execute(new ActionEvent());
            }
        });

        // Field info
        Label fieldInfo = new Label("Available Fields:");
        fieldInfo.setStyle("-fx-font-weight: bold;");

        Label availableFields = new Label("PAGE_NUMBER, PAGE_COUNT, DATE, TIME, DOCUMENT_TITLE, DOCUMENT_AUTHOR, VARIABLE, FILENAME, USER_NAME, CUSTOM");
        availableFields.setWrapText(true);
        availableFields.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        // Common fields quick insert buttons
        FlowPane quickInsert = new FlowPane(6, 6);
        quickInsert.setAlignment(Pos.TOP_LEFT);

        Button pageNumberBtn = new Button("Page #");
        pageNumberBtn.setOnAction(e -> {
            editor.getActionFactory().insertField(new Fields.PageNumberField("Page #")).execute(new ActionEvent());
        });

        Button pageCountBtn = new Button("of ##");
        pageCountBtn.setOnAction(e -> {
            editor.getActionFactory().insertField(new Fields.PageCountField(" of ##")).execute(new ActionEvent());
        });

        Button dateBtn = new Button("Date");
        dateBtn.setOnAction(e -> {
            editor.getActionFactory().insertField(new Fields.DateField("dd/MM/yyyy")).execute(new ActionEvent());
        });

        Button timeBtn = new Button("Time");
        timeBtn.setOnAction(e -> {
            editor.getActionFactory().insertField(new Fields.TimeField("HH:mm:ss")).execute(new ActionEvent());
        });

        quickInsert.getChildren().addAll(
                new Label("Quick Insert:"),
                pageNumberBtn, pageCountBtn, dateBtn, timeBtn
        );

        controls.getChildren().addAll(
                title,
                description,
                new Separator(),
                fieldTypeLabel, fieldTypeCombo,
                formatLabel, formatField,
                new Separator(),
                insertField,
                new Separator(),
                fieldInfo, availableFields,
                new Separator(),
                quickInsert
        );

        return controls;
    }

    private VBox createStyleControls() {
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10, 10, 10, 10));

        Label title = new Label("Styles (LibreOffice Writer-like)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label description = new Label("Create and apply named styles with inheritance. Styles can inherit from other styles.");
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Style name
        Label styleNameLabel = new Label("Style Name:");
        styleNameLabel.setStyle("-fx-font-weight: bold;");

        TextField styleNameField = new TextField();
        styleNameField.setPromptText("Enter style name (e.g., 'My Heading')");

        // Style type
        Label styleTypeLabel = new Label("Style Type:");
        styleTypeLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<Style.StyleType> styleTypeCombo = new ComboBox<>();
        styleTypeCombo.getItems().addAll(Style.StyleType.values());
        styleTypeCombo.setValue(Style.StyleType.PARAGRAPH);

        // Parent style
        Label parentStyleLabel = new Label("Parent Style:");
        parentStyleLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<String> parentStyleCombo = new ComboBox<>();
        parentStyleCombo.setPromptText("None (root style)");
        parentStyleCombo.setTooltip(new Tooltip("Select parent style for inheritance"));

        // Update parent style list from registry
        parentStyleCombo.getItems().addAll(styleRegistry.getStyles().stream().map(Style::getName).toList());
        parentStyleCombo.getItems().add("Default Paragraph Style");
        parentStyleCombo.getItems().add("Default Character Style");

        // Font size
        Label fontSizeLabel = new Label("Font Size:");
        fontSizeLabel.setStyle("-fx-font-weight: bold;");

        Spinner<Double> fontSizeSpinner = new Spinner<>(1, 72, 12, 1);

        // Font weight
        Label fontWeightLabel = new Label("Font Weight:");
        fontWeightLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<FontWeight> fontWeightCombo = new ComboBox<>();
        fontWeightCombo.getItems().addAll(FontWeight.values());
        fontWeightCombo.setValue(FontWeight.NORMAL);

        // Alignment
        Label alignmentLabel = new Label("Alignment:");
        alignmentLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<TextAlignment> alignmentCombo = new ComboBox<>();
        alignmentCombo.getItems().addAll(TextAlignment.values());
        alignmentCombo.setValue(TextAlignment.LEFT);

        // Page break options
        CheckBox pageBreakBeforeCheck = new CheckBox("Page Break Before");
        CheckBox pageBreakAfterCheck = new CheckBox("Page Break After");

        HBox pageBreakBox = new HBox(10, pageBreakBeforeCheck, pageBreakAfterCheck);

        // Create Style button
        Button createStyle = new Button("Create Style");
        createStyle.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        createStyle.setOnAction(e -> {
            String styleName = styleNameField.getText();
            if (styleName == null || styleName.trim().isEmpty()) {
                return;
            }

            Style.Builder builder;
            if (styleTypeCombo.getValue() == Style.StyleType.TEXT) {
                builder = Style.textStyle(styleName);
            } else {
                builder = Style.paragraphStyle(styleName);
            }

            String parent = parentStyleCombo.getValue();
            if (parent != null && !parent.trim().isEmpty()) {
                builder.parent(parent);
            }

            builder.fontSize(fontSizeSpinner.getValue())
                    .fontWeight(fontWeightCombo.getValue())
                    .alignment(alignmentCombo.getValue().toString());

            Style style = builder.build();
            styleRegistry.addStyle(style);

            // Update parent style combo
            parentStyleCombo.getItems().add(styleName);

            styleNameField.clear();
        });

        // Apply Style button
        Button applyStyle = new Button("Apply Style to Selection");
        applyStyle.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        applyStyle.setOnAction(e -> {
            String styleName = styleNameField.getText();
            if (styleName == null || styleName.trim().isEmpty()) {
                return;
            }
            Style style = styleRegistry.getStyle(styleName);

            if (style != null) {
                if (style.getType() == Style.StyleType.TEXT) {
                    TextDecoration textDec = TextDecoration.builder()
                            .presets()
                            .fontFamily(style.getFontFamily())
                            .fontSize(style.getFontSize())
                            .fontWeight(style.getFontWeight())
                            .fontPosture(style.getFontPosture())
                            .build();
                    editor.getActionFactory().decorate(textDec).execute(new ActionEvent());
                } else {
                    ParagraphDecoration paraDec = ParagraphDecoration.builder().build();
                    editor.getActionFactory().decorate(paraDec).execute(new ActionEvent());
                }
            }
        });

        // Show available styles
        Label availableStylesLabel = new Label("Available Styles:");
        availableStylesLabel.setStyle("-fx-font-weight: bold;");

        Label stylesInfo = new Label();
        stylesInfo.setWrapText(true);
        stylesInfo.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        // Update styles info
        stylesInfo.setText(String.join(", ", styleRegistry.getStyles().stream().map(Style::getName).toList()));

        controls.getChildren().addAll(
                title,
                description,
                new Separator(),
                styleNameLabel, styleNameField,
                styleTypeLabel, styleTypeCombo,
                parentStyleLabel, parentStyleCombo,
                new Separator(),
                fontSizeLabel, fontSizeSpinner,
                fontWeightLabel, fontWeightCombo,
                alignmentLabel, alignmentCombo,
                new Separator(),
                pageBreakBox,
                new Separator(),
                createStyle,
                applyStyle,
                new Separator(),
                availableStylesLabel, stylesInfo
        );

        return controls;
    }

    private Field createFieldFromSelection(FieldTypeOption option, String format) {
        switch (option) {
            case PAGE_NUMBER:
                return new Fields.PageNumberField("PageNumber");
            case PAGE_COUNT:
                return new Fields.PageCountField("PageCount");
            case DATE:
                return new Fields.DateField(format);
            case TIME:
                return new Fields.TimeField(format);
            case DOCUMENT_TITLE:
                return new Fields.DocumentTitleField("DocumentTitle");
            case DOCUMENT_AUTHOR:
                return new Fields.DocumentAuthorField("DocumentAuthor");
            case FILENAME:
                return new Fields.FileNameField();
            case USER_NAME:
                return new Fields.UserNameField();
            case VARIABLE:
                return new Fields.VariableField("MyVariable");
            default:
                return null;
        }
    }

    private Spinner<Double> createMarginSpinner(double initialValue) {
        SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 144, initialValue, 1);
        Spinner<Double> spinner = new Spinner<>(factory);
        spinner.setPrefWidth(80);
        spinner.setEditable(true);
        return spinner;
    }

    private enum FieldTypeOption {
        PAGE_NUMBER("Page Number"),
        PAGE_COUNT("Page Count"),
        DATE("Date"),
        TIME("Time"),
        DOCUMENT_TITLE("Document Title"),
        DOCUMENT_AUTHOR("Document Author"),
        FILENAME("File Name"),
        USER_NAME("User Name"),
        VARIABLE("Variable");

        private final String displayName;

        FieldTypeOption(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}