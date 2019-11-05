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

import jexer.bits.CellAttributes;
import jexer.bits.StringUtils;
import jexer.event.TCommandEvent;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.teditor.Document;
import jexer.teditor.Line;
import jexer.teditor.Word;
import static jexer.TCommand.*;
import static jexer.TKeypress.*;

/**
 * TEditorWidget displays an editable text document.  It is unaware of
 * scrolling behavior, but can respond to mouse and keyboard events.
 */
public class TEditorWidget extends TWidget implements EditMenuUser {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The number of lines to scroll on mouse wheel up/down.
     */
    private static final int wheelScrollSize = 3;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The document being edited.
     */
    private Document document;

    /**
     * The default color for the TEditor class.
     */
    private CellAttributes defaultColor = null;

    /**
     * The topmost line number in the visible area.  0-based.
     */
    private int topLine = 0;

    /**
     * The leftmost column number in the visible area.  0-based.
     */
    private int leftColumn = 0;

    /**
     * If true, the mouse is dragging a selection.
     */
    private boolean inSelection = false;

    /**
     * Selection starting column.
     */
    private int selectionColumn0;

    /**
     * Selection starting line.
     */
    private int selectionLine0;

    /**
     * Selection ending column.
     */
    private int selectionColumn1;

    /**
     * Selection ending line.
     */
    private int selectionLine1;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text text on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    public TEditorWidget(final TWidget parent, final String text, final int x,
        final int y, final int width, final int height) {

        // Set parent and window
        super(parent, x, y, width, height);

        setCursorVisible(true);

        defaultColor = getTheme().getColor("teditor");
        document = new Document(text, defaultColor);
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
        if (mouse.isMouseWheelUp()) {
            for (int i = 0; i < wheelScrollSize; i++) {
                if (topLine > 0) {
                    topLine--;
                    alignDocument(false);
                }
            }
            return;
        }
        if (mouse.isMouseWheelDown()) {
            for (int i = 0; i < wheelScrollSize; i++) {
                if (topLine < document.getLineCount() - 1) {
                    topLine++;
                    alignDocument(true);
                }
            }
            return;
        }

        if (mouse.isMouse1()) {
            // Selection.
            if (inSelection) {
                selectionColumn1 = leftColumn + mouse.getX();
                selectionLine1 = topLine + mouse.getY();
            } else if (mouse.isShift()) {
                inSelection = true;
                selectionColumn0 = leftColumn + mouse.getX();
                selectionLine0 = topLine + mouse.getY();
                selectionColumn1 = selectionColumn0;
                selectionLine1 = selectionLine0;
            }

            // Set the row and column
            int newLine = topLine + mouse.getY();
            int newX = leftColumn + mouse.getX();
            if (newLine > document.getLineCount() - 1) {
                // Go to the end
                document.setLineNumber(document.getLineCount() - 1);
                document.end();
                if (newLine > document.getLineCount() - 1) {
                    setCursorY(document.getLineCount() - 1 - topLine);
                } else {
                    setCursorY(mouse.getY());
                }
                alignCursor();
                if (inSelection) {
                    selectionColumn1 = document.getCursor();
                    selectionLine1 = document.getLineNumber();
                }
                return;
            }

            document.setLineNumber(newLine);
            setCursorY(mouse.getY());
            if (newX >= document.getCurrentLine().getDisplayLength()) {
                document.end();
                alignCursor();
            } else {
                document.setCursor(newX);
                setCursorX(mouse.getX());
            }
            if (inSelection) {
                selectionColumn1 = document.getCursor();
                selectionLine1 = document.getLineNumber();
            }
            return;
        } else {
            inSelection = false;
        }

        // Pass to children
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse motion events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {

        if (mouse.isMouse1()) {
            // Selection.
            if (inSelection) {
                selectionColumn1 = leftColumn + mouse.getX();
                selectionLine1 = topLine + mouse.getY();
            } else if (mouse.isShift()) {
                inSelection = true;
                selectionColumn0 = leftColumn + mouse.getX();
                selectionLine0 = topLine + mouse.getY();
                selectionColumn1 = selectionColumn0;
                selectionLine1 = selectionLine0;
            }

            // Set the row and column
            int newLine = topLine + mouse.getY();
            int newX = leftColumn + mouse.getX();
            if (newLine > document.getLineCount() - 1) {
                // Go to the end
                document.setLineNumber(document.getLineCount() - 1);
                document.end();
                if (newLine > document.getLineCount() - 1) {
                    setCursorY(document.getLineCount() - 1 - topLine);
                } else {
                    setCursorY(mouse.getY());
                }
                alignCursor();
                if (inSelection) {
                    selectionColumn1 = document.getCursor();
                    selectionLine1 = document.getLineNumber();
                }
                return;
            }

            document.setLineNumber(newLine);
            setCursorY(mouse.getY());
            if (newX >= document.getCurrentLine().getDisplayLength()) {
                document.end();
                alignCursor();
            } else {
                document.setCursor(newX);
                setCursorX(mouse.getX());
            }
            if (inSelection) {
                selectionColumn1 = document.getCursor();
                selectionLine1 = document.getLineNumber();
            }
            return;
        } else {
            inSelection = false;
        }

        // Pass to children
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        inSelection = false;

        // Pass to children
        super.onMouseDown(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.getKey().isShift()) {
            // Selection.
            if (!inSelection) {
                inSelection = true;
                selectionColumn0 = document.getCursor();
                selectionLine0 = document.getLineNumber();
                selectionColumn1 = selectionColumn0;
                selectionLine1 = selectionLine0;
            }
        } else {
            if (keypress.equals(kbLeft)
                || keypress.equals(kbRight)
                || keypress.equals(kbUp)
                || keypress.equals(kbDown)
                || keypress.equals(kbPgDn)
                || keypress.equals(kbPgUp)
                || keypress.equals(kbHome)
                || keypress.equals(kbEnd)
            ) {
                // Non-shifted navigation keys disable selection.
                inSelection = false;
            }
        }

        if (keypress.equals(kbLeft)
            || keypress.equals(kbShiftLeft)
        ) {
            document.left();
            alignTopLine(false);
        } else if (keypress.equals(kbRight)
            || keypress.equals(kbShiftRight)
        ) {
            document.right();
            alignTopLine(true);
        } else if (keypress.equals(kbAltLeft)
            || keypress.equals(kbCtrlLeft)
            || keypress.equals(kbAltShiftLeft)
            || keypress.equals(kbCtrlShiftLeft)
        ) {
            document.backwardsWord();
            alignTopLine(false);
        } else if (keypress.equals(kbAltRight)
            || keypress.equals(kbCtrlRight)
            || keypress.equals(kbAltShiftRight)
            || keypress.equals(kbCtrlShiftRight)
        ) {
            document.forwardsWord();
            alignTopLine(true);
        } else if (keypress.equals(kbUp)
            || keypress.equals(kbShiftUp)
        ) {
            document.up();
            alignTopLine(false);
        } else if (keypress.equals(kbDown)
            || keypress.equals(kbShiftDown)
        ) {
            document.down();
            alignTopLine(true);
        } else if (keypress.equals(kbPgUp)
            || keypress.equals(kbShiftPgUp)
        ) {
            document.up(getHeight() - 1);
            alignTopLine(false);
        } else if (keypress.equals(kbPgDn)
            || keypress.equals(kbShiftPgDn)
        ) {
            document.down(getHeight() - 1);
            alignTopLine(true);
        } else if (keypress.equals(kbHome)
            || keypress.equals(kbShiftHome)
        ) {
            if (document.home()) {
                leftColumn = 0;
                if (leftColumn < 0) {
                    leftColumn = 0;
                }
                setCursorX(0);
            }
        } else if (keypress.equals(kbEnd)
            || keypress.equals(kbShiftEnd)
        ) {
            if (document.end()) {
                alignCursor();
            }
        } else if (keypress.equals(kbCtrlHome)
            || keypress.equals(kbCtrlShiftHome)
        ) {
            document.setLineNumber(0);
            document.home();
            topLine = 0;
            leftColumn = 0;
            setCursorX(0);
            setCursorY(0);
        } else if (keypress.equals(kbCtrlEnd)
            || keypress.equals(kbCtrlShiftEnd)
        ) {
            document.setLineNumber(document.getLineCount() - 1);
            document.end();
            alignTopLine(false);
        } else if (keypress.equals(kbIns)) {
            document.setOverwrite(!document.getOverwrite());
        } else if (keypress.equals(kbDel)) {
            if (inSelection) {
                deleteSelection();
            } else {
                document.del();
            }
            alignCursor();
        } else if (keypress.equals(kbBackspace)
            || keypress.equals(kbBackspaceDel)
        ) {
            if (inSelection) {
                deleteSelection();
            } else {
                document.backspace();
            }
            alignTopLine(false);
        } else if (keypress.equals(kbTab)) {
            deleteSelection();
            // Add spaces until we hit modulo 8.
            for (int i = document.getCursor(); (i + 1) % 8 != 0; i++) {
                document.addChar(' ');
            }
            alignCursor();
        } else if (keypress.equals(kbEnter)) {
            deleteSelection();
            document.enter();
            alignTopLine(true);
        } else if (!keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
        ) {
            // Plain old keystroke, process it
            deleteSelection();
            document.addChar(keypress.getKey().getChar());
            alignCursor();
        } else {
            // Pass other keys (tab etc.) on to TWidget
            super.onKeypress(keypress);
        }

        if (inSelection) {
            selectionColumn1 = document.getCursor();
            selectionLine1 = document.getLineNumber();
        }
    }

    /**
     * Method that subclasses can override to handle window/screen resize
     * events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        // Change my width/height, and pull the cursor in as needed.
        if (resize.getType() == TResizeEvent.Type.WIDGET) {
            setWidth(resize.getWidth());
            setHeight(resize.getHeight());
            // See if the cursor is now outside the window, and if so move
            // things.
            if (getCursorX() >= getWidth()) {
                leftColumn += getCursorX() - (getWidth() - 1);
                setCursorX(getWidth() - 1);
            }
            if (getCursorY() >= getHeight()) {
                topLine += getCursorY() - (getHeight() - 1);
                setCursorY(getHeight() - 1);
            }
        } else {
            // Let superclass handle it
            super.onResize(resize);
        }
    }

    /**
     * Handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmCut)) {
            // Copy text to clipboard, and then remove it.
            copySelection();
            deleteSelection();
            return;
        }

        if (command.equals(cmCopy)) {
            // Copy text to clipboard.
            copySelection();
            return;
        }

        if (command.equals(cmPaste)) {
            // Delete selected text, then paste text from clipboard.
            deleteSelection();

            String text = getClipboard().pasteText();
            if (text != null) {
                for (int i = 0; i < text.length(); ) {
                    int ch = text.codePointAt(i);
                    onKeypress(new TKeypressEvent(false, 0, ch, false, false,
                            false));
                    i += Character.charCount(ch);
                }
            }
            return;
        }

        if (command.equals(cmClear)) {
            // Remove text.
            deleteSelection();
            return;
        }

    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
        CellAttributes selectedColor = getTheme().getColor("teditor.selected");

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 <= selectionLine0))
            || ((selectionColumn1 <= selectionColumn0)
                && (selectionLine1 < selectionLine0))
        ) {
            // The user selected from bottom-right to top-left.  Reverse the
            // coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;
        }

        for (int i = 0; i < getHeight(); i++) {
            // Background line
            getScreen().hLineXY(0, i, getWidth(), ' ', defaultColor);

            // Now draw document's line
            if (topLine + i < document.getLineCount()) {
                Line line = document.getLine(topLine + i);
                int x = 0;
                for (Word word: line.getWords()) {
                    // For now, we are cheating: draw outside the left region
                    // if needed and let screen do the clipping.
                    getScreen().putStringXY(x - leftColumn, i, word.getText(),
                        word.getColor());
                    x += word.getDisplayLength();
                    if (x - leftColumn > getWidth()) {
                        break;
                    }
                }

                // Highlight selected region
                if (inSelection) {
                    if (startRow == endRow) {
                        if (topLine + i == startRow) {
                            for (x = startCol; x <= endCol; x++) {
                                putAttrXY(x - leftColumn, i, selectedColor);
                            }
                        }
                    } else {
                        if (topLine + i == startRow) {
                            for (x = startCol; x < line.getDisplayLength(); x++) {
                                putAttrXY(x - leftColumn, i, selectedColor);
                            }
                        } else if (topLine + i == endRow) {
                            for (x = 0; x <= endCol; x++) {
                                putAttrXY(x - leftColumn, i, selectedColor);
                            }
                        } else if ((topLine + i >= startRow)
                            && (topLine + i <= endRow)
                        ) {
                            for (x = 0; x < getWidth(); x++) {
                                putAttrXY(x, i, selectedColor);
                            }
                        }
                    }
                }

            }
        }
    }

    // ------------------------------------------------------------------------
    // TEditorWidget ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Align visible area with document current line.
     *
     * @param topLineIsTop if true, make the top visible line the document
     * current line if it was off-screen.  If false, make the bottom visible
     * line the document current line.
     */
    private void alignTopLine(final boolean topLineIsTop) {
        int line = document.getLineNumber();

        if ((line < topLine) || (line > topLine + getHeight() - 1)) {
            // Need to move topLine to bring document back into view.
            if (topLineIsTop) {
                topLine = line - (getHeight() - 1);
                if (topLine < 0) {
                    topLine = 0;
                }
                assert (topLine >= 0);
            } else {
                topLine = line;
                assert (topLine >= 0);
            }
        }

        /*
        System.err.println("line " + line + " topLine " + topLine);
        */

        // Document is in view, let's set cursorY
        assert (line >= topLine);
        setCursorY(line - topLine);
        alignCursor();
    }

    /**
     * Align document current line with visible area.
     *
     * @param topLineIsTop if true, make the top visible line the document
     * current line if it was off-screen.  If false, make the bottom visible
     * line the document current line.
     */
    private void alignDocument(final boolean topLineIsTop) {
        int line = document.getLineNumber();
        int cursor = document.getCursor();

        if ((line < topLine) || (line > topLine + getHeight() - 1)) {
            // Need to move document to ensure it fits view.
            if (topLineIsTop) {
                document.setLineNumber(topLine);
            } else {
                document.setLineNumber(topLine + (getHeight() - 1));
            }
            if (cursor < document.getCurrentLine().getDisplayLength()) {
                document.setCursor(cursor);
            }
        }

        /*
        System.err.println("getLineNumber() " + document.getLineNumber() +
            " topLine " + topLine);
        */

        // Document is in view, let's set cursorY
        setCursorY(document.getLineNumber() - topLine);
        alignCursor();
    }

    /**
     * Align visible cursor with document cursor.
     */
    private void alignCursor() {
        int width = getWidth();

        int desiredX = document.getCursor() - leftColumn;
        if (desiredX < 0) {
            // We need to push the screen to the left.
            leftColumn = document.getCursor();
        } else if (desiredX > width - 1) {
            // We need to push the screen to the right.
            leftColumn = document.getCursor() - (width - 1);
        }

        /*
        System.err.println("document cursor " + document.getCursor() +
            " leftColumn " + leftColumn);
        */


        setCursorX(document.getCursor() - leftColumn);
    }

    /**
     * Get the number of lines in the underlying Document.
     *
     * @return the number of lines
     */
    public int getLineCount() {
        return document.getLineCount();
    }

    /**
     * Get the current visible top row number.  1-based.
     *
     * @return the visible top row number.  Row 1 is the first row.
     */
    public int getVisibleRowNumber() {
        return topLine + 1;
    }

    /**
     * Set the current visible row number.  1-based.
     *
     * @param row the new visible row number.  Row 1 is the first row.
     */
    public void setVisibleRowNumber(final int row) {
        assert (row > 0);
        if ((row > 0) && (row < document.getLineCount())) {
            topLine = row - 1;
            alignDocument(true);
        }
    }

    /**
     * Get the current editing row number.  1-based.
     *
     * @return the editing row number.  Row 1 is the first row.
     */
    public int getEditingRowNumber() {
        return document.getLineNumber() + 1;
    }

    /**
     * Set the current editing row number.  1-based.
     *
     * @param row the new editing row number.  Row 1 is the first row.
     */
    public void setEditingRowNumber(final int row) {
        assert (row > 0);
        if ((row > 0) && (row < document.getLineCount())) {
            document.setLineNumber(row - 1);
            alignTopLine(true);
        }
    }

    /**
     * Set the current visible column number.  1-based.
     *
     * @return the visible column number.  Column 1 is the first column.
     */
    public int getVisibleColumnNumber() {
        return leftColumn + 1;
    }

    /**
     * Set the current visible column number.  1-based.
     *
     * @param column the new visible column number.  Column 1 is the first
     * column.
     */
    public void setVisibleColumnNumber(final int column) {
        assert (column > 0);
        if ((column > 0) && (column < document.getLineLengthMax())) {
            leftColumn = column - 1;
            alignDocument(true);
        }
    }

    /**
     * Get the current editing column number.  1-based.
     *
     * @return the editing column number.  Column 1 is the first column.
     */
    public int getEditingColumnNumber() {
        return document.getCursor() + 1;
    }

    /**
     * Set the current editing column number.  1-based.
     *
     * @param column the new editing column number.  Column 1 is the first
     * column.
     */
    public void setEditingColumnNumber(final int column) {
        if ((column > 0) && (column < document.getLineLength())) {
            document.setCursor(column - 1);
            alignCursor();
        }
    }

    /**
     * Get the maximum possible row number.  1-based.
     *
     * @return the maximum row number.  Row 1 is the first row.
     */
    public int getMaximumRowNumber() {
        return document.getLineCount() + 1;
    }

    /**
     * Get the maximum possible column number.  1-based.
     *
     * @return the maximum column number.  Column 1 is the first column.
     */
    public int getMaximumColumnNumber() {
        return document.getLineLengthMax() + 1;
    }

    /**
     * Get the dirty value.
     *
     * @return true if the buffer is dirty
     */
    public boolean isDirty() {
        return document.isDirty();
    }

    /**
     * Save contents to file.
     *
     * @param filename file to save to
     * @throws IOException if a java.io operation throws
     */
    public void saveToFilename(final String filename) throws IOException {
        document.saveToFilename(filename);
    }

    /**
     * Delete text within the selection bounds.
     */
    private void deleteSelection() {
        if (inSelection == false) {
            return;
        }
        inSelection = false;

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 <= selectionLine0))
            || ((selectionColumn1 <= selectionColumn0)
                && (selectionLine1 < selectionLine0))
        ) {
            // The user selected from bottom-right to top-left.  Reverse the
            // coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;
        }

        // Place the cursor on the selection end, and "press backspace" until
        // the cursor matches the selection start.
        document.setLineNumber(endRow);
        document.setCursor(endCol + 1);
        while (!((document.getLineNumber() == startRow)
                && (document.getCursor() == startCol))
        ) {
            document.backspace();
        }
        alignTopLine(true);
    }

    /**
     * Copy text within the selection bounds to clipboard.
     */
    private void copySelection() {
        if (inSelection == false) {
            return;
        }

        int startCol = selectionColumn0;
        int startRow = selectionLine0;
        int endCol = selectionColumn1;
        int endRow = selectionLine1;

        if (((selectionColumn1 < selectionColumn0)
                && (selectionLine1 <= selectionLine0))
            || ((selectionColumn1 <= selectionColumn0)
                && (selectionLine1 < selectionLine0))
        ) {
            // The user selected from bottom-right to top-left.  Reverse the
            // coordinates for the inverted section.
            startCol = selectionColumn1;
            startRow = selectionLine1;
            endCol = selectionColumn0;
            endRow = selectionLine0;
        }

        StringBuilder sb = new StringBuilder();

        if (endRow > startRow) {
            // First line
            String line = document.getLine(startRow).getRawString();
            int x = 0;
            for (int i = 0; i < line.length(); ) {
                int ch = line.codePointAt(i);

                if (x >= startCol) {
                    sb.append(Character.toChars(ch));
                }
                x += StringUtils.width(ch);
                i += Character.charCount(ch);
            }
            sb.append("\n");

            // Middle lines
            for (int y = startRow + 1; y < endRow; y++) {
                sb.append(document.getLine(y).getRawString());
                sb.append("\n");
            }

            // Final line
            line = document.getLine(endRow).getRawString();
            x = 0;
            for (int i = 0; i < line.length(); ) {
                int ch = line.codePointAt(i);

                if (x > endCol) {
                    break;
                }

                sb.append(Character.toChars(ch));
                x += StringUtils.width(ch);
                i += Character.charCount(ch);
            }
        } else {
            assert (startRow == endRow);

            // Only one line
            String line = document.getLine(startRow).getRawString();
            int x = 0;
            for (int i = 0; i < line.length(); ) {
                int ch = line.codePointAt(i);

                if ((x >= startCol) && (x <= endCol)) {
                    sb.append(Character.toChars(ch));
                }

                x += StringUtils.width(ch);
                i += Character.charCount(ch);
            }
        }

        getClipboard().copyText(sb.toString());
    }

    // ------------------------------------------------------------------------
    // EditMenuUser -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if the cut menu item should be enabled.
     *
     * @return true if the cut menu item should be enabled
     */
    public boolean isEditMenuCut() {
        return true;
    }

    /**
     * Check if the copy menu item should be enabled.
     *
     * @return true if the copy menu item should be enabled
     */
    public boolean isEditMenuCopy() {
        return true;
    }

    /**
     * Check if the paste menu item should be enabled.
     *
     * @return true if the paste menu item should be enabled
     */
    public boolean isEditMenuPaste() {
        return true;
    }

    /**
     * Check if the clear menu item should be enabled.
     *
     * @return true if the clear menu item should be enabled
     */
    public boolean isEditMenuClear() {
        return true;
    }

}
