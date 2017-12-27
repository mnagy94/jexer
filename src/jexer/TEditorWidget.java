/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2017 Kevin Lamonte
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
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.teditor.Document;
import jexer.teditor.Line;
import jexer.teditor.Word;
import static jexer.TKeypress.*;

/**
 * TEditorWidget displays an editable text document.  It is unaware of
 * scrolling behavior, but can respond to mouse and keyboard events.
 */
public class TEditorWidget extends TWidget {

    /**
     * The number of lines to scroll on mouse wheel up/down.
     */
    private static final int wheelScrollSize = 3;

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

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
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
            }
        }
    }

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
            return;
        }

        // Pass to children
        super.onMouseDown(mouse);
    }

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
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbLeft)) {
            document.left();
            alignTopLine(false);
        } else if (keypress.equals(kbRight)) {
            document.right();
            alignTopLine(true);
        } else if (keypress.equals(kbUp)) {
            document.up();
            alignTopLine(false);
        } else if (keypress.equals(kbDown)) {
            document.down();
            alignTopLine(true);
        } else if (keypress.equals(kbPgUp)) {
            document.up(getHeight() - 1);
            alignTopLine(false);
        } else if (keypress.equals(kbPgDn)) {
            document.down(getHeight() - 1);
            alignTopLine(true);
        } else if (keypress.equals(kbHome)) {
            if (document.home()) {
                leftColumn = 0;
                if (leftColumn < 0) {
                    leftColumn = 0;
                }
                setCursorX(0);
            }
        } else if (keypress.equals(kbEnd)) {
            if (document.end()) {
                alignCursor();
            }
        } else if (keypress.equals(kbCtrlHome)) {
            document.setLineNumber(0);
            document.home();
            topLine = 0;
            leftColumn = 0;
            setCursorX(0);
            setCursorY(0);
        } else if (keypress.equals(kbCtrlEnd)) {
            document.setLineNumber(document.getLineCount() - 1);
            document.end();
            alignTopLine(false);
        } else if (keypress.equals(kbIns)) {
            document.setOverwrite(!document.getOverwrite());
        } else if (keypress.equals(kbDel)) {
            document.del();
            alignCursor();
        } else if (keypress.equals(kbBackspace)) {
            document.backspace();
            alignTopLine(false);
        } else if (keypress.equals(kbTab)) {
            // TODO: tab character.  For now just add spaces until we hit
            // modulo 8.
            for (int i = document.getCursor(); (i + 1) % 8 != 0; i++) {
                document.addChar(' ');
            }
            alignCursor();
        } else if (keypress.equals(kbEnter)) {
            document.enter();
            alignTopLine(true);
        } else if (!keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
        ) {
            // Plain old keystroke, process it
            document.addChar(keypress.getKey().getChar());
            alignCursor();
        } else {
            // Pass other keys (tab etc.) on to TWidget
            super.onKeypress(keypress);
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

}
