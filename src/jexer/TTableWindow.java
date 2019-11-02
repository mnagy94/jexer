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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import jexer.event.TCommandEvent;
import jexer.event.TKeypressEvent;
import jexer.event.TMenuEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.menu.TMenu;
import jexer.menu.TMenuItem;
import static jexer.TCommand.*;
import static jexer.TKeypress.*;

/**
 * TTableWindow is used to display and edit regular two-dimensional tables of
 * cells.
 */
public class TTableWindow extends TScrollableWindow {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(TTableWindow.class.getName());

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The table widget.
     */
    private TTableWidget tableField;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor sets window title.
     *
     * @param parent the main application
     * @param title the window title
     */
    public TTableWindow(final TApplication parent, final String title) {

        super(parent, title, 0, 0, parent.getScreen().getWidth() / 2,
            parent.getScreen().getHeight() / 2 - 2, RESIZABLE | CENTERED);

        tableField = addTable(0, 0, getWidth() - 2, getHeight() - 2);
        setupAfterTable();
    }

    /**
     * Public constructor loads a grid from a RFC4180 CSV file.
     *
     * @param parent the main application
     * @param csvFile a File referencing the CSV data
     * @throws IOException if a java.io operation throws
     */
    public TTableWindow(final TApplication parent,
        final File csvFile) throws IOException {

        super(parent, csvFile.getName(), 0, 0,
            parent.getScreen().getWidth() / 2,
            parent.getScreen().getHeight() / 2 - 2,
            RESIZABLE | CENTERED);

        tableField = addTable(0, 0, getWidth() - 2, getHeight() - 2, 1, 1);
        setupAfterTable();
        tableField.loadCsvFile(csvFile);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Called by application.switchWindow() when this window gets the
     * focus, and also by application.addWindow().
     */
    public void onFocus() {
        // Enable the table menu items.
        getApplication().enableMenuItem(TMenu.MID_TABLE_RENAME_COLUMN);
        getApplication().enableMenuItem(TMenu.MID_TABLE_RENAME_ROW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_VIEW_ROW_LABELS);
        getApplication().enableMenuItem(TMenu.MID_TABLE_VIEW_COLUMN_LABELS);
        getApplication().enableMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_NONE);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_ALL);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_CELL_NONE);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_CELL_ALL);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_RIGHT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_LEFT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_TOP);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_BOTTOM);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_DOUBLE_BOTTOM);
        getApplication().enableMenuItem(TMenu.MID_TABLE_BORDER_THICK_BOTTOM);
        getApplication().enableMenuItem(TMenu.MID_TABLE_DELETE_LEFT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_DELETE_UP);
        getApplication().enableMenuItem(TMenu.MID_TABLE_DELETE_ROW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_DELETE_COLUMN);
        getApplication().enableMenuItem(TMenu.MID_TABLE_INSERT_LEFT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_INSERT_RIGHT);
        getApplication().enableMenuItem(TMenu.MID_TABLE_INSERT_ABOVE);
        getApplication().enableMenuItem(TMenu.MID_TABLE_INSERT_BELOW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_COLUMN_NARROW);
        getApplication().enableMenuItem(TMenu.MID_TABLE_COLUMN_WIDEN);
        getApplication().enableMenuItem(TMenu.MID_TABLE_FILE_OPEN_CSV);
        getApplication().enableMenuItem(TMenu.MID_TABLE_FILE_SAVE_CSV);
        getApplication().enableMenuItem(TMenu.MID_TABLE_FILE_SAVE_TEXT);

        if (tableField != null) {

            // Set the menu to match the flags.
            TMenuItem menuItem = getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_ROW_LABELS);
            if (menuItem != null) {
                menuItem.setChecked(tableField.getShowRowLabels());
            }
            menuItem = getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_COLUMN_LABELS);
            if (menuItem != null) {
                menuItem.setChecked(tableField.getShowColumnLabels());
            }
            menuItem = getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW);
            if (menuItem != null) {
                menuItem.setChecked(tableField.getHighlightRow());
            }
            menuItem = getApplication().getMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN);
            if (menuItem != null) {
                menuItem.setChecked(tableField.getHighlightColumn());
            }
        }
    }

    /**
     * Called by application.switchWindow() when another window gets the
     * focus.
     */
    public void onUnfocus() {
        // Disable the table menu items.
        getApplication().disableMenuItem(TMenu.MID_TABLE_RENAME_COLUMN);
        getApplication().disableMenuItem(TMenu.MID_TABLE_RENAME_ROW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_VIEW_ROW_LABELS);
        getApplication().disableMenuItem(TMenu.MID_TABLE_VIEW_COLUMN_LABELS);
        getApplication().disableMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_NONE);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_ALL);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_CELL_NONE);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_CELL_ALL);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_RIGHT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_LEFT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_TOP);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_BOTTOM);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_DOUBLE_BOTTOM);
        getApplication().disableMenuItem(TMenu.MID_TABLE_BORDER_THICK_BOTTOM);
        getApplication().disableMenuItem(TMenu.MID_TABLE_DELETE_LEFT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_DELETE_UP);
        getApplication().disableMenuItem(TMenu.MID_TABLE_DELETE_ROW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_DELETE_COLUMN);
        getApplication().disableMenuItem(TMenu.MID_TABLE_INSERT_LEFT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_INSERT_RIGHT);
        getApplication().disableMenuItem(TMenu.MID_TABLE_INSERT_ABOVE);
        getApplication().disableMenuItem(TMenu.MID_TABLE_INSERT_BELOW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_COLUMN_NARROW);
        getApplication().disableMenuItem(TMenu.MID_TABLE_COLUMN_WIDEN);
        getApplication().disableMenuItem(TMenu.MID_TABLE_FILE_OPEN_CSV);
        getApplication().disableMenuItem(TMenu.MID_TABLE_FILE_SAVE_CSV);
        getApplication().disableMenuItem(TMenu.MID_TABLE_FILE_SAVE_TEXT);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        // Use TWidget's code to pass the event to the children.
        super.onMouseDown(mouse);

        if (mouseOnTable(mouse)) {
            // The table might have changed, update the scollbars.
            setBottomValue(tableField.getRowCount() - 1);
            setVerticalValue(tableField.getSelectedRowNumber());
            setRightValue(tableField.getColumnCount() - 1);
            setHorizontalValue(tableField.getSelectedColumnNumber());
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
        super.onMouseUp(mouse);

        if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
            // Clicked/dragged on vertical scrollbar.
            tableField.setSelectedRowNumber(getVerticalValue());
        }
        if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
            // Clicked/dragged on horizontal scrollbar.
            tableField.setSelectedColumnNumber(getHorizontalValue());
        }
    }

    /**
     * Method that subclasses can override to handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        // Use TWidget's code to pass the event to the children.
        super.onMouseMotion(mouse);

        if (mouseOnTable(mouse) && mouse.isMouse1()) {
            // The table might have changed, update the scollbars.
            setBottomValue(tableField.getRowCount() - 1);
            setVerticalValue(tableField.getSelectedRowNumber());
            setRightValue(tableField.getColumnCount() - 1);
            setHorizontalValue(tableField.getSelectedColumnNumber());
        } else {
            if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
                // Clicked/dragged on vertical scrollbar.
                tableField.setSelectedRowNumber(getVerticalValue());
            }
            if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
                // Clicked/dragged on horizontal scrollbar.
                tableField.setSelectedColumnNumber(getHorizontalValue());
            }
        }

    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        // Use TWidget's code to pass the event to the children.
        super.onKeypress(keypress);

        // The table might have changed, update the scollbars.
        setBottomValue(tableField.getRowCount() - 1);
        setVerticalValue(tableField.getSelectedRowNumber());
        setRightValue(tableField.getColumnCount() - 1);
        setHorizontalValue(tableField.getSelectedColumnNumber());
    }

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            // Resize the table
            TResizeEvent tableSize = new TResizeEvent(TResizeEvent.Type.WIDGET,
                event.getWidth() - 2, event.getHeight() - 2);
            tableField.onResize(tableSize);

            // Have TScrollableWindow handle the scrollbars
            super.onResize(event);
            return;
        }

        // Pass to children instead
        for (TWidget widget: getChildren()) {
            widget.onResize(event);
        }
    }

    /**
     * Method that subclasses can override to handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmOpen)) {
            try {
                String filename = fileOpenBox(".");
                if (filename != null) {
                    try {
                        new TTableWindow(getApplication(), new File(filename));
                    } catch (IOException e) {
                        messageBox(i18n.getString("errorDialogTitle"),
                            MessageFormat.format(i18n.
                                getString("errorReadingFile"), e.getMessage()));
                    }
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorOpeningFileDialog"), e.getMessage()));
            }
            return;
        }

        if (command.equals(cmSave)) {
            try {
                String filename = fileSaveBox(".");
                if (filename != null) {
                    tableField.saveToCsvFilename(filename);
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorWritingFile"), e.getMessage()));
            }
            return;
        }

        // Didn't handle it, let children get it instead
        super.onCommand(command);
    }

    /**
     * Handle posted menu events.
     *
     * @param menu menu event
     */
    @Override
    public void onMenu(final TMenuEvent menu) {
        TInputBox inputBox = null;
        String filename = null;

        switch (menu.getId()) {
        case TMenu.MID_TABLE_RENAME_COLUMN:
            inputBox = inputBox(i18n.getString("renameColumnInputTitle"),
                i18n.getString("renameColumnInputCaption"),
                tableField.getColumnLabel(tableField.getSelectedColumnNumber()),
                TMessageBox.Type.OKCANCEL);
            if (inputBox.isOk()) {
                tableField.setColumnLabel(tableField.getSelectedColumnNumber(),
                    inputBox.getText());
            }
            return;
        case TMenu.MID_TABLE_RENAME_ROW:
            inputBox = inputBox(i18n.getString("renameRowInputTitle"),
                i18n.getString("renameRowInputCaption"),
                tableField.getRowLabel(tableField.getSelectedRowNumber()),
                TMessageBox.Type.OKCANCEL);
            if (inputBox.isOk()) {
                tableField.setRowLabel(tableField.getSelectedRowNumber(),
                    inputBox.getText());
            }
            return;
        case TMenu.MID_TABLE_VIEW_ROW_LABELS:
            tableField.setShowRowLabels(getApplication().getMenuItem(
                menu.getId()).getChecked());
            return;
        case TMenu.MID_TABLE_VIEW_COLUMN_LABELS:
            tableField.setShowColumnLabels(getApplication().getMenuItem(
                menu.getId()).getChecked());
            return;
        case TMenu.MID_TABLE_VIEW_HIGHLIGHT_ROW:
            tableField.setHighlightRow(getApplication().getMenuItem(
                menu.getId()).getChecked());
            return;
        case TMenu.MID_TABLE_VIEW_HIGHLIGHT_COLUMN:
            tableField.setHighlightColumn(getApplication().getMenuItem(
                menu.getId()).getChecked());
            return;
        case TMenu.MID_TABLE_BORDER_NONE:
            tableField.setBorderAllNone();
            return;
        case TMenu.MID_TABLE_BORDER_ALL:
            tableField.setBorderAllSingle();
            return;
        case TMenu.MID_TABLE_BORDER_CELL_NONE:
            tableField.setBorderCellNone();
            return;
        case TMenu.MID_TABLE_BORDER_CELL_ALL:
            tableField.setBorderCellSingle();
            return;
        case TMenu.MID_TABLE_BORDER_RIGHT:
            tableField.setBorderColumnRightSingle();
            return;
        case TMenu.MID_TABLE_BORDER_LEFT:
            tableField.setBorderColumnLeftSingle();
            return;
        case TMenu.MID_TABLE_BORDER_TOP:
            tableField.setBorderRowAboveSingle();
            return;
        case TMenu.MID_TABLE_BORDER_BOTTOM:
            tableField.setBorderRowBelowSingle();
            return;
        case TMenu.MID_TABLE_BORDER_DOUBLE_BOTTOM:
            tableField.setBorderRowBelowDouble();
            return;
        case TMenu.MID_TABLE_BORDER_THICK_BOTTOM:
            tableField.setBorderRowBelowThick();
            return;
        case TMenu.MID_TABLE_DELETE_LEFT:
            tableField.deleteCellShiftLeft();
            return;
        case TMenu.MID_TABLE_DELETE_UP:
            tableField.deleteCellShiftUp();
            return;
        case TMenu.MID_TABLE_DELETE_ROW:
            tableField.deleteRow(tableField.getSelectedRowNumber());
            return;
        case TMenu.MID_TABLE_DELETE_COLUMN:
            tableField.deleteColumn(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_INSERT_LEFT:
            tableField.insertColumnLeft(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_INSERT_RIGHT:
            tableField.insertColumnRight(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_INSERT_ABOVE:
            tableField.insertRowAbove(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_INSERT_BELOW:
            tableField.insertRowBelow(tableField.getSelectedColumnNumber());
            return;
        case TMenu.MID_TABLE_COLUMN_NARROW:
            tableField.setColumnWidth(tableField.getSelectedColumnNumber(),
                tableField.getColumnWidth(tableField.getSelectedColumnNumber()) - 1);
            return;
        case TMenu.MID_TABLE_COLUMN_WIDEN:
            tableField.setColumnWidth(tableField.getSelectedColumnNumber(),
                tableField.getColumnWidth(tableField.getSelectedColumnNumber()) + 1);
            return;
        case TMenu.MID_TABLE_FILE_OPEN_CSV:
            try {
                filename = fileOpenBox(".");
                if (filename != null) {
                    try {
                        new TTableWindow(getApplication(), new File(filename));
                    } catch (IOException e) {
                        messageBox(i18n.getString("errorDialogTitle"),
                            MessageFormat.format(i18n.
                                getString("errorReadingFile"), e.getMessage()));
                    }
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorOpeningFileDialog"), e.getMessage()));
            }
            return;
        case TMenu.MID_TABLE_FILE_SAVE_CSV:
            try {
                filename = fileSaveBox(".");
                if (filename != null) {
                    tableField.saveToCsvFilename(filename);
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorWritingFile"), e.getMessage()));
            }
            return;
        case TMenu.MID_TABLE_FILE_SAVE_TEXT:
            try {
                filename = fileSaveBox(".");
                if (filename != null) {
                    tableField.saveToTextFilename(filename);
                }
            } catch (IOException e) {
                messageBox(i18n.getString("errorDialogTitle"),
                    MessageFormat.format(i18n.
                        getString("errorWritingFile"), e.getMessage()));
            }
            return;
        default:
            break;
        }

        super.onMenu(menu);
    }

    // ------------------------------------------------------------------------
    // TTableWindow -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Setup other fields after the table is created.
     */
    private void setupAfterTable() {
        hScroller = new THScroller(this, 17, getHeight() - 2, getWidth() - 20);
        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);
        setMinimumWindowWidth(25);
        setMinimumWindowHeight(10);
        setTopValue(tableField.getSelectedRowNumber());
        setBottomValue(tableField.getRowCount() - 1);
        setLeftValue(tableField.getSelectedColumnNumber());
        setRightValue(tableField.getColumnCount() - 1);

        statusBar = newStatusBar(i18n.getString("statusBar"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));

        statusBar.addShortcutKeypress(kbF2, cmSave,
            i18n.getString("statusBarSave"));
        statusBar.addShortcutKeypress(kbF3, cmOpen,
            i18n.getString("statusBarOpen"));
        statusBar.addShortcutKeypress(kbF10, cmMenu,
            i18n.getString("statusBarMenu"));

        // Synchronize the menu with tableField's flags.
        onFocus();
    }

    /**
     * Check if a mouse press/release/motion event coordinate is over the
     * table.
     *
     * @param mouse a mouse-based event
     * @return whether or not the mouse is on the table
     */
    private boolean mouseOnTable(final TMouseEvent mouse) {
        if ((mouse.getAbsoluteX() >= getAbsoluteX() + 1)
            && (mouse.getAbsoluteX() <  getAbsoluteX() + getWidth() - 1)
            && (mouse.getAbsoluteY() >= getAbsoluteY() + 1)
            && (mouse.getAbsoluteY() <  getAbsoluteY() + getHeight() - 1)
        ) {
            return true;
        }
        return false;
    }

}
