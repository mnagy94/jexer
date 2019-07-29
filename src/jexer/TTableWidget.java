/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2019 Kevin Lamonte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * @author Kevin Lamonte [kevin.lamonte@gmail.com]
 * @version 1
 */
package jexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jexer.bits.CellAttributes;
import jexer.event.TKeypressEvent;
import jexer.event.TMenuEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.menu.TMenu;
import static jexer.TKeypress.*;

/**
 * TTableWidget is used to display and edit regular two-dimensional tables of
 * cells.
 *
 * This class was inspired by a TTable implementation originally developed by
 * David "Niki" ROULET [niki@nikiroo.be], made available under MIT at
 * https://github.com/nikiroo/jexer/tree/ttable_pull.
 */
public class TTableWidget extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Available borders for cells.
     */
    public enum Border {
        /**
         * No border.
         */
        NONE,

        /**
         * Single bar: \u2502 (vertical) and \u2500 (horizontal).
         */
        SINGLE,

        /**
         * Double bar: \u2551 (vertical) and \u2550 (horizontal).
         */
        DOUBLE,

        /**
         * Thick bar: \u258C (vertical, left half block) and \u2580
         * (horizontal, upper block).
         */
        THICK,
    }

    /**
     * Row label width.
     */
    private static final int ROW_LABEL_WIDTH = 8;

    /**
     * Column label height.
     */
    private static final int COLUMN_LABEL_HEIGHT = 1;

    /**
     * Column default width.
     */
    private static final int COLUMN_DEFAULT_WIDTH = 8;

    /**
     * Extra rows to add.
     */
    private static final int EXTRA_ROWS = 10;

    /**
     * Extra columns to add.
     */
    private static final int EXTRA_COLUMNS = 10 * (8 + 1);

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The underlying data, organized as columns.
     */
    private ArrayList<Column> columns = new ArrayList<Column>();

    /**
     * The underlying data, organized as rows.
     */
    private ArrayList<Row> rows = new ArrayList<Row>();

    /**
     * The row in model corresponding to the top-left visible cell.
     */
    private int top = 0;

    /**
     * The column in model corresponding to the top-left visible cell.
     */
    private int left = 0;

    /**
     * The row in model corresponding to the currently selected cell.
     */
    private int selectedRow = 0;

    /**
     * The column in model corresponding to the currently selected cell.
     */
    private int selectedColumn = 0;

    /**
     * If true, highlight the entire row of the currently-selected cell.
     */
    private boolean highlightRow = false;

    /**
     * If true, highlight the entire column of the currently-selected cell.
     */
    private boolean highlightColumn = false;

    /**
     * If true, show the row labels as the first column.
     */
    private boolean showRowLabels = true;

    /**
     * If true, show the column labels as the first row.
     */
    private boolean showColumnLabels = true;

    /**
     * The top border for the first row.
     */
    private Border topBorder = Border.NONE;

    /**
     * The left border for the first column.
     */
    private Border leftBorder = Border.NONE;

    /**
     * Column represents a column of cells.
     */
    public class Column {

        /**
         * X position of this column.
         */
        private int x = 0;

        /**
         * Width of column.
         */
        private int width = COLUMN_DEFAULT_WIDTH;

        /**
         * The cells of this column.
         */
        private ArrayList<Cell> cells = new ArrayList<Cell>();

        /**
         * Column label.
         */
        private String label = "";

        /**
         * The right border for this column.
         */
        private Border rightBorder = Border.NONE;

        /**
         * Constructor sets label to lettered column.
         *
         * @param col column number to use for this column.  Column 0 will be
         * "A", column 1 will be "B", column 26 will be "AA", and so on.
         */
        Column(int col) {
            StringBuilder sb = new StringBuilder();
            for (;;) {
                sb.append((char) ('A' + (col % 26)));
                if (col < 26) {
                    break;
                }
                col /= 26;
            }
            label = sb.reverse().toString();
        }

        /**
         * Add an entry to this column.
         *
         * @param cell the cell to add
         */
        public void add(final Cell cell) {
            cells.add(cell);
        }

        /**
         * Get an entry from this column.
         *
         * @param row the entry index to get
         * @return the cell at row
         */
        public Cell get(final int row) {
            return cells.get(row);
        }

        /**
         * Get the X position of the cells in this column.
         *
         * @return the position
         */
        public int getX() {
            return x;
        }

        /**
         * Set the X position of the cells in this column.
         *
         * @param x the position
         */
        public void setX(final int x) {
            for (Cell cell: cells) {
                cell.setX(x);
            }
            this.x = x;
        }

    }

    /**
     * Row represents a row of cells.
     */
    public class Row {

        /**
         * Y position of this row.
         */
        private int y = 0;

        /**
         * Height of row.
         */
        private int height = 1;

        /**
         * The cells of this row.
         */
        private ArrayList<Cell> cells = new ArrayList<Cell>();

        /**
         * Row label.
         */
        private String label = "";

        /**
         * The bottom border for this row.
         */
        private Border bottomBorder = Border.NONE;

        /**
         * Constructor sets label to numbered row.
         *
         * @param row row number to use for this row
         */
        Row(final int row) {
            label = Integer.toString(row);
        }

        /**
         * Add an entry to this column.
         *
         * @param cell the cell to add
         */
        public void add(final Cell cell) {
            cells.add(cell);
        }

        /**
         * Get an entry from this row.
         *
         * @param column the entry index to get
         * @return the cell at column
         */
        public Cell get(final int column) {
            return cells.get(column);
        }
        /**
         * Get the Y position of the cells in this column.
         *
         * @return the position
         */
        public int getY() {
            return y;
        }

        /**
         * Set the Y position of the cells in this column.
         *
         * @param y the position
         */
        public void setY(final int y) {
            for (Cell cell: cells) {
                cell.setY(y);
            }
            this.y = y;
        }

    }

    /**
     * Cell represents an editable cell in the table.  Normally, navigation
     * to a cell only highlights it; pressing Enter or F2 will switch to
     * editing mode.
     */
    public class Cell extends TWidget {

        // --------------------------------------------------------------------
        // Variables ----------------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * The field containing the cell's data.
         */
        private TField field;

        /**
         * The column of this cell.
         */
        private int column;

        /**
         * The row of this cell.
         */
        private int row;

        /**
         * If true, the cell is being edited.
         */
        private boolean isEditing = false;

        /**
         * Text of field before editing.
         */
        private String fieldText;

        // --------------------------------------------------------------------
        // Constructors -------------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * Public constructor.
         *
         * @param parent parent widget
         * @param x column relative to parent
         * @param y row relative to parent
         * @param width width of widget
         * @param height height of widget
         * @param column column index of this cell
         * @param row row index of this cell
         */
        public Cell(final TTableWidget parent, final int x, final int y,
            final int width, final int height, final int column,
            final int row) {

            super(parent, x, y, width, height);
            this.column = column;
            this.row = row;

            field = addField(0, 0, width, false);
            field.setEnabled(false);
            field.setBackgroundChar(' ');
        }

        // --------------------------------------------------------------------
        // Event handlers -----------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * Handle mouse double-click events.
         *
         * @param mouse mouse double-click event
         */
        @Override
        public void onMouseDoubleClick(final TMouseEvent mouse) {
            // Use TWidget's code to pass the event to the children.
            super.onMouseDown(mouse);

            // Double-click means to start editing.
            fieldText = field.getText();
            isEditing = true;
            field.setEnabled(true);
            activate(field);

            if (isActive()) {
                // Let the table know that I was activated.
                ((TTableWidget) getParent()).selectedRow = row;
                ((TTableWidget) getParent()).selectedColumn = column;
                ((TTableWidget) getParent()).alignGrid();
            }
        }

        /**
         * Handle mouse press events.
         *
         * @param mouse mouse button press event
         */
        @Override
        public void onMouseDown(final TMouseEvent mouse) {
            // Use TWidget's code to pass the event to the children.
            super.onMouseDown(mouse);

            if (isActive()) {
                // Let the table know that I was activated.
                ((TTableWidget) getParent()).selectedRow = row;
                ((TTableWidget) getParent()).selectedColumn = column;
                ((TTableWidget) getParent()).alignGrid();
            }
        }

        /**
         * Handle mouse release events.
         *
         * @param mouse mouse button release event
         */
        @Override
        public void onMouseUp(final TMouseEvent mouse) {
            // Use TWidget's code to pass the event to the children.
            super.onMouseDown(mouse);

            if (isActive()) {
                // Let the table know that I was activated.
                ((TTableWidget) getParent()).selectedRow = row;
                ((TTableWidget) getParent()).selectedColumn = column;
                ((TTableWidget) getParent()).alignGrid();
            }
        }

        /**
         * Handle keystrokes.
         *
         * @param keypress keystroke event
         */
        @Override
        public void onKeypress(final TKeypressEvent keypress) {
            // System.err.println("Cell onKeypress: " + keypress);

            if (isEditing) {
                if (keypress.equals(kbEsc)) {
                    // ESC cancels the edit.
                    field.setText(fieldText);
                    isEditing = false;
                    field.setEnabled(false);
                    return;
                }
                if (keypress.equals(kbEnter)) {
                    // Enter ends editing.
                    fieldText = field.getText();
                    isEditing = false;
                    field.setEnabled(false);
                    return;
                }
                // Pass down to field.
                super.onKeypress(keypress);
            }

            if (keypress.equals(kbEnter) || keypress.equals(kbF2)) {
                // Enter or F2 starts editing.
                fieldText = field.getText();
                isEditing = true;
                field.setEnabled(true);
                activate(field);
                return;
            }
        }

        // --------------------------------------------------------------------
        // TWidget ------------------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * Draw this cell.
         */
        @Override
        public void draw() {
            TTableWidget table = (TTableWidget) getParent();

            if (isAbsoluteActive()) {
                if (isEditing) {
                    field.setActiveColorKey("tfield.active");
                    field.setInactiveColorKey("tfield.inactive");
                } else {
                    field.setActiveColorKey("ttable.selected");
                    field.setInactiveColorKey("ttable.selected");
                }
            } else if (((table.selectedColumn == column)
                    && ((table.selectedRow == row)
                        || (table.highlightColumn == true)))
                || ((table.selectedRow == row)
                    && ((table.selectedColumn == column)
                        || (table.highlightRow == true)))
            ) {
                field.setActiveColorKey("ttable.active");
                field.setInactiveColorKey("ttable.active");
            } else {
                field.setActiveColorKey("ttable.active");
                field.setInactiveColorKey("ttable.inactive");
            }

            assert (isVisible() == true);

            super.draw();
        }

        // --------------------------------------------------------------------
        // TTable.Cell --------------------------------------------------------
        // --------------------------------------------------------------------

        /**
         * Get field text.
         *
         * @return field text
         */
        public final String getText() {
            return field.getText();
        }

        /**
         * Set field text.
         *
         * @param text the new field text
         */
        public void setText(final String text) {
            field.setText(text);
        }

    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of widget
     * @param height height of widget
     */
    public TTableWidget(final TWidget parent, final int x, final int y,
        final int width, final int height) {

        super(parent, x, y, width, height);

        // Initialize the starting row and column.
        rows.add(new Row(0));
        columns.add(new Column(0));

        // Place a grid of cells that fit in this space.
        int row = 0;
        for (int i = 0; i < height + EXTRA_ROWS; i += rows.get(0).height) {
            int column = 0;
            for (int j = 0; j < width + EXTRA_COLUMNS;
                 j += columns.get(0).width) {

                Cell cell = new Cell(this, 0, 0, /* j, i, */ columns.get(0).width,
                    rows.get(0).height, column, row);

                // DEBUG: set a grid of cell index labels
                // TODO: remove this
                cell.setText("" + row + " " + column);
                rows.get(row).add(cell);
                columns.get(column).add(cell);
                if ((i == 0) &&
                    (j + columns.get(0).width < width + EXTRA_COLUMNS)
                ) {
                    columns.add(new Column(column + 1));
                }
                column++;
            }
            if (i + rows.get(0).height < height + EXTRA_ROWS) {
                rows.add(new Row(row + 1));
            }
            row++;
        }
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setY(i + (showColumnLabels ? COLUMN_LABEL_HEIGHT : 0));
        }
        for (int j = 0; j < columns.size(); j++) {
            columns.get(j).setX((j * COLUMN_DEFAULT_WIDTH) +
                (showRowLabels ? ROW_LABEL_WIDTH : 0));
        }
        activate(columns.get(selectedColumn).get(selectedRow));

        alignGrid();

        // Set the menu to match the flags.
        getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_ROW_LABELS).
                setChecked(showRowLabels);
        getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_COLUMN_LABELS).
                setChecked(showColumnLabels);
        getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW).
                setChecked(highlightRow);
        getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN).
                setChecked(highlightColumn);


    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouseWheelUp() || mouse.isMouseWheelDown()) {
            // Treat wheel up/down as 3 up/down
            TKeypressEvent keyEvent;
            if (mouse.isMouseWheelUp()) {
                keyEvent = new TKeypressEvent(kbUp);
            } else {
                keyEvent = new TKeypressEvent(kbDown);
            }
            for (int i = 0; i < 3; i++) {
                onKeypress(keyEvent);
            }
            return;
        }

        // Use TWidget's code to pass the event to the children.
        super.onMouseDown(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbTab)
            || keypress.equals(kbShiftTab)
        ) {
            // Squash tab and back-tab.  They don't make sense in the TTable
            // grid context.
            return;
        }

        // If editing, pass to that cell and do nothing else.
        if (getSelectedCell().isEditing) {
            super.onKeypress(keypress);
            return;
        }

        if (keypress.equals(kbLeft)) {
            // Left
            if (selectedColumn > 0) {
                selectedColumn--;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbRight)) {
            // Right
            if (selectedColumn < columns.size() - 1) {
                selectedColumn++;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbUp)) {
            // Up
            if (selectedRow > 0) {
                selectedRow--;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbDown)) {
            // Down
            if (selectedRow < rows.size() - 1) {
                selectedRow++;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbHome)) {
            // Home - leftmost column
            selectedColumn = 0;
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbEnd)) {
            // End - rightmost column
            selectedColumn = columns.size() - 1;
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbPgUp)) {
            // PgUp - Treat like multiple up
            for (int i = 0; i < getHeight() - 2; i++) {
                if (selectedRow > 0) {
                    selectedRow--;
                }
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbPgDn)) {
            // PgDn - Treat like multiple up
            for (int i = 0; i < getHeight() - 2; i++) {
                if (selectedRow < rows.size() - 1) {
                    selectedRow++;
                }
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbCtrlHome)) {
            // Ctrl-Home - go to top-left
            selectedRow = 0;
            selectedColumn = 0;
            activate(columns.get(selectedColumn).get(selectedRow));
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbCtrlEnd)) {
            // Ctrl-End - go to bottom-right
            selectedRow = rows.size() - 1;
            selectedColumn = columns.size() - 1;
            activate(columns.get(selectedColumn).get(selectedRow));
            activate(columns.get(selectedColumn).get(selectedRow));
        } else {
            // Pass to the Cell.
            super.onKeypress(keypress);
        }

        // We may have scrolled off screen.  Reset positions as needed to
        // make the newly selected cell visible.
        alignGrid();
    }

    /**
     * Handle widget resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        super.onResize(event);

        alignGrid();
    }

    /**
     * Handle posted menu events.
     *
     * @param menu menu event
     */
    @Override
    public void onMenu(final TMenuEvent menu) {
        switch (menu.getId()) {
        case TMenu.MID_TABLE_VIEW_ROW_LABELS:
            showRowLabels = getApplication().getMenuItem(menu.getId()).getChecked();
            break;
        case TMenu.MID_TABLE_VIEW_COLUMN_LABELS:
            showColumnLabels = getApplication().getMenuItem(menu.getId()).getChecked();
            break;
        case TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW:
            highlightRow = getApplication().getMenuItem(menu.getId()).getChecked();
            break;
        case TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN:
            highlightColumn = getApplication().getMenuItem(menu.getId()).getChecked();
            break;
        case TMenu.MID_TABLE_BORDER_NONE:
        case TMenu.MID_TABLE_BORDER_ALL:
        case TMenu.MID_TABLE_BORDER_RIGHT:
        case TMenu.MID_TABLE_BORDER_LEFT:
        case TMenu.MID_TABLE_BORDER_TOP:
        case TMenu.MID_TABLE_BORDER_BOTTOM:
        case TMenu.MID_TABLE_BORDER_DOUBLE_BOTTOM:
        case TMenu.MID_TABLE_BORDER_THICK_BOTTOM:
        case TMenu.MID_TABLE_DELETE_LEFT:
        case TMenu.MID_TABLE_DELETE_UP:
        case TMenu.MID_TABLE_DELETE_ROW:
        case TMenu.MID_TABLE_DELETE_COLUMN:
        case TMenu.MID_TABLE_INSERT_LEFT:
        case TMenu.MID_TABLE_INSERT_RIGHT:
        case TMenu.MID_TABLE_INSERT_ABOVE:
        case TMenu.MID_TABLE_INSERT_BELOW:
            break;
        case TMenu.MID_TABLE_COLUMN_NARROW:
            columns.get(selectedColumn).width--;
            for (Cell cell: getSelectedColumn().cells) {
                cell.setWidth(columns.get(selectedColumn).width);
                cell.field.setWidth(columns.get(selectedColumn).width);
            }
            for (int i = selectedColumn + 1; i < columns.size(); i++) {
                columns.get(i).setX(columns.get(i).getX() - 1);
            }
            alignGrid();
            break;
        case TMenu.MID_TABLE_COLUMN_WIDEN:
            columns.get(selectedColumn).width++;
            for (Cell cell: getSelectedColumn().cells) {
                cell.setWidth(columns.get(selectedColumn).width);
                cell.field.setWidth(columns.get(selectedColumn).width);
            }
            for (int i = selectedColumn + 1; i < columns.size(); i++) {
                columns.get(i).setX(columns.get(i).getX() + 1);
            }
            alignGrid();
            break;
        case TMenu.MID_TABLE_FILE_SAVE_CSV:
            // TODO
            break;
        case TMenu.MID_TABLE_FILE_SAVE_TEXT:
            // TODO
            break;
        default:
            super.onMenu(menu);
        }

        alignGrid();
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the table row/column labels, and borders.
     */
    @Override
    public void draw() {
        CellAttributes labelColor = getTheme().getColor("ttable.label");
        CellAttributes labelColorSelected = getTheme().getColor("ttable.label.selected");
        CellAttributes borderColor = getTheme().getColor("ttable.border");

        // Column labels.
        if (showColumnLabels == true) {
            for (int i = left; i < columns.size(); i++) {
                if (columns.get(i).get(top).isVisible() == false) {
                    break;
                }
                putStringXY(columns.get(i).get(top).getX(), 0,
                    String.format(" %-" +
                        (columns.get(i).width - 2)
                        + "s ", columns.get(i).label),
                    (i == selectedColumn ? labelColorSelected : labelColor));
            }
        }

        // Row labels.
        if (showRowLabels == true) {
            for (int i = top; i < rows.size(); i++) {
                if (rows.get(i).get(left).isVisible() == false) {
                    break;
                }
                putStringXY(0, rows.get(i).get(left).getY(),
                    String.format(" %-6s ", rows.get(i).label),
                    (i == selectedRow ? labelColorSelected : labelColor));
            }
        }

        // Now draw the window borders.
        super.draw();
    }

    // ------------------------------------------------------------------------
    // TTable -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the currently-selected cell.
     *
     * @return the selected cell
     */
    public Cell getSelectedCell() {
        assert (rows.get(selectedRow) != null);
        assert (rows.get(selectedRow).get(selectedColumn) != null);
        assert (columns.get(selectedColumn) != null);
        assert (columns.get(selectedColumn).get(selectedRow) != null);
        assert (rows.get(selectedRow).get(selectedColumn) ==
            columns.get(selectedColumn).get(selectedRow));

        return (columns.get(selectedColumn).get(selectedRow));
    }

    /**
     * Get the currently-selected column.
     *
     * @return the selected column
     */
    public Column getSelectedColumn() {
        assert (selectedColumn >= 0);
        assert (columns.size() > selectedColumn);
        assert (columns.get(selectedColumn) != null);
        return columns.get(selectedColumn);
    }

    /**
     * Get the currently-selected row.
     *
     * @return the selected row
     */
    public Row getSelectedRow() {
        assert (selectedRow >= 0);
        assert (rows.size() > selectedRow);
        assert (rows.get(selectedRow) != null);
        return rows.get(selectedRow);
    }

    /**
     * Get the currently-selected column number.  0 is the left-most column.
     *
     * @return the selected column number
     */
    public int getSelectedColumnNumber() {
        return selectedColumn;
    }

    /**
     * Set the currently-selected column number.  0 is the left-most column.
     *
     * @param column the column number to select
     */
    public void setSelectedColumnNumber(final int column) {
        if ((column < 0) || (column > columns.size() - 1)) {
            throw new IndexOutOfBoundsException("Column count is " +
                columns.size() + ", requested index " + column);
        }
        selectedColumn = column;
        activate(columns.get(selectedColumn).get(selectedRow));
        alignGrid();
    }

    /**
     * Get the currently-selected row number.  0 is the top-most row.
     *
     * @return the selected row number
     */
    public int getSelectedRowNumber() {
        return selectedRow;
    }

    /**
     * Set the currently-selected row number.  0 is the left-most column.
     *
     * @param row the row number to select
     */
    public void setSelectedRowNumber(final int row) {
        if ((row < 0) || (row > rows.size() - 1)) {
            throw new IndexOutOfBoundsException("Row count is " +
                rows.size() + ", requested index " + row);
        }
        selectedRow = row;
        activate(columns.get(selectedColumn).get(selectedRow));
        alignGrid();
    }

    /**
     * Get the number of columns.
     *
     * @return the number of columns
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * Get the number of rows.
     *
     * @return the number of rows
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * Align the grid so that the selected cell is fully visible.
     */
    private void alignGrid() {
        int viewColumns = getWidth();
        if (showRowLabels == true) {
            viewColumns -= ROW_LABEL_WIDTH;
        }
        if (leftBorder != Border.NONE) {
            viewColumns--;
        }
        int viewRows = getHeight();
        if (showColumnLabels == true) {
            viewRows -= COLUMN_LABEL_HEIGHT;
        }
        if (topBorder != Border.NONE) {
            viewRows--;
        }

        // If we pushed left or right, adjust the box to include the new
        // selected cell.
        if (selectedColumn < left) {
            left = selectedColumn - 1;
        }
        if (left < 0) {
            left = 0;
        }
        if (selectedRow < top) {
            top = selectedRow - 1;
        }
        if (top < 0) {
            top = 0;
        }

        /*
         * viewColumns and viewRows now contain the available columns and
         * rows available to view the selected cell.  We adjust left and top
         * to ensure the selected cell is within view, and then make all
         * cells outside the box between (left, top) and (right, bottom)
         * invisible.
         *
         * We need to calculate right and bottom now.
         */
        int right = left;

        boolean done = false;
        while (!done) {
            int rightCellX = (showRowLabels ? ROW_LABEL_WIDTH : 0);
            int maxCellX = rightCellX + viewColumns;
            right = left;
            boolean selectedIsVisible = false;
            int selectedX = 0;
            for (int x = left; x < columns.size(); x++) {
                if (x == selectedColumn) {
                    selectedX = rightCellX;
                    if (selectedX + columns.get(x).width + 1 <= maxCellX) {
                        selectedIsVisible = true;
                    }
                }
                rightCellX += columns.get(x).width + 1;
                if (rightCellX >= maxCellX) {
                    break;
                }
                right++;
            }
            if (right < selectedColumn) {
                // selectedColumn is outside the view range.  Push left over,
                // and calculate again.
                left++;
            } else if (left == selectedColumn) {
                // selectedColumn doesn't fit inside the view range, but we
                // can't go over any further either.  Bail out.
                done = true;
            } else if (selectedIsVisible == false) {
                // selectedColumn doesn't fit inside the view range, continue
                // on.
                left++;
            } else {
                // selectedColumn is fully visible, all done.
                assert (selectedIsVisible == true);
                done = true;
            }

        } // while (!done)

        // We have the left/right range correct, set cell visibility and
        // column X positions.
        int leftCellX = showRowLabels ? ROW_LABEL_WIDTH : 0;
        for (int x = 0; x < columns.size(); x++) {
            if ((x < left) || (x > right)) {
                for (int i = 0; i < rows.size(); i++) {
                    columns.get(x).get(i).setVisible(false);
                    columns.get(x).setX(getWidth() + 1);
                }
                continue;
            }
            for (int i = 0; i < rows.size(); i++) {
                columns.get(x).get(i).setVisible(true);
            }
            columns.get(x).setX(leftCellX);
            leftCellX += columns.get(x).width + 1;
        }

        int bottom = top;

        done = false;
        while (!done) {
            int bottomCellY = (showColumnLabels ? COLUMN_LABEL_HEIGHT : 0);
            int maxCellY = bottomCellY + viewRows;
            bottom = top;
            for (int y = top; y < rows.size(); y++) {
                bottomCellY += rows.get(y).height;
                if (bottomCellY >= maxCellY) {
                    break;
                }
                bottom++;
            }
            if (bottom < selectedRow) {
                // selectedRow is outside the view range.  Push top down, and
                // calculate again.
                top++;
            } else {
                // selectedRow is inside the view range, done.
                done = true;
            }
        } // while (!done)

        // We have the top/bottom range correct, set cell visibility and
        // row Y positions.
        int topCellY = showColumnLabels ? COLUMN_LABEL_HEIGHT : 0;
        for (int y = 0; y < rows.size(); y++) {
            if ((y < top) || (y > bottom)) {
                for (int i = 0; i < columns.size(); i++) {
                    rows.get(y).get(i).setVisible(false);
                }
                rows.get(y).setY(getHeight() + 1);
                continue;
            }
            for (int i = 0; i < columns.size(); i++) {
                rows.get(y).get(i).setVisible(true);
            }
            rows.get(y).setY(topCellY);
            topCellY += rows.get(y).height;
        }

    }

    /**
     * Save contents to file.
     *
     * @param filename file to save to
     * @throws IOException if a java.io operation throws
     */
    public void saveToFilename(final String filename) throws IOException {
        // TODO
    }

}
