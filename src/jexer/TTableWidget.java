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

import java.util.ArrayList;
import java.util.List;

import jexer.event.TKeypressEvent;
import jexer.event.TMenuEvent;
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
    private boolean highlightRow = true;

    /**
     * If true, highlight the entire column of the currently-selected cell.
     */
    private boolean highlightColumn = true;

    /**
     * If true, show the row labels as the first column.
     */
    private boolean showRowLabels = true;

    /**
     * Column represents a column of cells.
     */
    public class Column {

        /**
         * Width of column.
         */
        private int width = 8;

        /**
         * The cells of this column.
         */
        private ArrayList<Cell> cells = new ArrayList<Cell>();

        /**
         * Column label.
         */
        private String label;

        /**
         * The border for this column.
         */
        private Border border = Border.NONE;

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
    }

    /**
     * Row represents a row of cells.
     */
    public class Row {

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
         * The border for this row.
         */
        private Border border = Border.NONE;

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

            field = addField(0, 0, width - 1, false);
            field.setEnabled(false);
            field.setBackgroundChar(' ');
        }

        // --------------------------------------------------------------------
        // Event handlers -----------------------------------------------------
        // --------------------------------------------------------------------

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
        for (int i = 0; i < height; i += rows.get(0).height) {
            int column = 0;
            for (int j = 0; j < width; j += columns.get(0).width) {
                Cell cell = new Cell(this, j, i, columns.get(0).width,
                    rows.get(0).height, column, row);

                cell.setText("" + row + " " + column);
                rows.get(row).add(cell);
                columns.get(column).add(cell);
                if ((i == 0) && (j + columns.get(0).width < width)) {
                    columns.add(new Column(column + 1));
                }
                column++;
            }
            if (i + rows.get(0).height < height) {
                rows.add(new Row(row + 1));
            }
            row++;
        }
        activate(columns.get(selectedColumn).get(selectedRow));

        alignGrid();
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

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
            if (selectedColumn > 0) {
                selectedColumn--;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbRight)) {
            if (selectedColumn < columns.size() - 1) {
                selectedColumn++;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbUp)) {
            if (selectedRow > 0) {
                selectedRow--;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbDown)) {
            if (selectedRow < rows.size() - 1) {
                selectedRow++;
            }
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbHome)) {
            selectedColumn = 0;
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbEnd)) {
            selectedColumn = columns.size() - 1;
            activate(columns.get(selectedColumn).get(selectedRow));
        } else if (keypress.equals(kbPgUp)) {
            // TODO
        } else if (keypress.equals(kbPgDn)) {
            // TODO
        } else if (keypress.equals(kbCtrlHome)) {
            // TODO
        } else if (keypress.equals(kbCtrlEnd)) {
            // TODO
        } else {
            // Pass to the Cell.
            super.onKeypress(keypress);
        }

        // We may have scrolled off screen.  Reset positions as needed to
        // make the newly selected cell visible.
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
                cell.field.setWidth(columns.get(selectedColumn).width - 1);
            }
            for (int i = selectedColumn + 1; i < columns.size(); i++) {
                for (Cell cell: columns.get(i).cells) {
                    cell.setX(cell.getX() - 1);
                }
            }
            alignGrid();
            break;
        case TMenu.MID_TABLE_COLUMN_WIDEN:
            columns.get(selectedColumn).width++;
            for (Cell cell: getSelectedColumn().cells) {
                cell.setWidth(columns.get(selectedColumn).width);
                cell.field.setWidth(columns.get(selectedColumn).width - 1);
            }
            for (int i = selectedColumn + 1; i < columns.size(); i++) {
                for (Cell cell: columns.get(i).cells) {
                    cell.setX(cell.getX() + 1);
                }
            }
            alignGrid();
            break;
        case TMenu.MID_TABLE_FILE_SAVE_CSV:
        case TMenu.MID_TABLE_FILE_SAVE_TEXT:
            break;
        default:
            super.onMenu(menu);
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

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
     * Get the full horizontal width of this table.
     *
     * @return the width required to render the entire table
     */
    public int getMaximumWidth() {
        int totalWidth = 0;
        if (showRowLabels == true) {
            // For now, all row labels are 8 cells wide.  TODO: make this
            // adjustable.
            totalWidth += 8;
        }
        for (Cell cell: getSelectedRow().cells) {
            totalWidth += cell.getWidth() + 1;
        }
        return totalWidth;
    }

    /**
     * Align the grid so that the selected cell is fully visible.
     */
    private void alignGrid() {
        // Determine if we need to shift left or right.
        int width = getMaximumWidth();
        int leftCellX = 0;
        if (showRowLabels == true) {
            // For now, all row labels are 8 cells wide.  TODO: make this
            // adjustable.
            leftCellX += 8;
        }
        // TODO


        // TODO: determine shift up/down


    }

}
