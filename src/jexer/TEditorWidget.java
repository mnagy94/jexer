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
public final class TEditorWidget extends TWidget {

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
            if (getCursorY() == getHeight() - 1) {
                if (document.up()) {
                    if (topLine > 0) {
                        topLine--;
                    }
                    alignCursor();
                }
            } else {
                if (topLine > 0) {
                    topLine--;
                    setCursorY(getCursorY() + 1);
                }
            }
            return;
        }
        if (mouse.isMouseWheelDown()) {
            if (getCursorY() == 0) {
                if (document.down()) {
                    if (topLine < document.getLineNumber()) {
                        topLine++;
                    }
                    alignCursor();
                }
            } else {
                if (topLine < document.getLineCount() - getHeight()) {
                    topLine++;
                    setCursorY(getCursorY() - 1);
                }
            }
            return;
        }

        if (mouse.isMouse1()) {
            // Set the row and column
            int newLine = topLine + mouse.getY();
            int newX = leftColumn + mouse.getX();
            if (newLine > document.getLineCount()) {
                // Go to the end
                document.setLineNumber(document.getLineCount() - 1);
                document.end();
                if (document.getLineCount() > getHeight()) {
                    setCursorY(getHeight() - 1);
                } else {
                    setCursorY(document.getLineCount() - 1);
                }
                alignCursor();
                return;
            }

            document.setLineNumber(newLine);
            setCursorY(mouse.getY());
            if (newX > document.getCurrentLine().getDisplayLength()) {
                document.end();
                alignCursor();
            } else {
                setCursorX(mouse.getX());
            }
            return;
        }

        // Pass to children
        super.onMouseDown(mouse);
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
            if (document.left()) {
                alignCursor();
            }
        } else if (keypress.equals(kbRight)) {
            if (document.right()) {
                alignCursor();
            }
        } else if (keypress.equals(kbUp)) {
            if (document.up()) {
                if (getCursorY() > 0) {
                    setCursorY(getCursorY() - 1);
                } else {
                    if (topLine > 0) {
                        topLine--;
                    }
                }
                alignCursor();
            }
        } else if (keypress.equals(kbDown)) {
            if (document.down()) {
                if (getCursorY() < getHeight() - 1) {
                    setCursorY(getCursorY() + 1);
                } else {
                    if (topLine < document.getLineCount() - getHeight()) {
                        topLine++;
                    }
                }
                alignCursor();
            }
        } else if (keypress.equals(kbPgUp)) {
            for (int i = 0; i < getHeight() - 1; i++) {
                if (document.up()) {
                    if (getCursorY() > 0) {
                        setCursorY(getCursorY() - 1);
                    } else {
                        if (topLine > 0) {
                            topLine--;
                        }
                    }
                    alignCursor();
                } else {
                    break;
                }
            }
        } else if (keypress.equals(kbPgDn)) {
            for (int i = 0; i < getHeight() - 1; i++) {
                if (document.down()) {
                    if (getCursorY() < getHeight() - 1) {
                        setCursorY(getCursorY() + 1);
                    } else {
                        if (topLine < document.getLineCount() - getHeight()) {
                            topLine++;
                        }
                    }
                    alignCursor();
                } else {
                    break;
                }
            }
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
            topLine = document.getLineCount() - getHeight();
            if (topLine < 0) {
                topLine = 0;
            }
            if (document.getLineCount() > getHeight()) {
                setCursorY(getHeight() - 1);
            } else {
                setCursorY(document.getLineCount() - 1);
            }
            alignCursor();
        } else if (keypress.equals(kbIns)) {
            document.setOverwrite(!document.getOverwrite());
        } else if (keypress.equals(kbDel)) {
            document.del();
        } else if (keypress.equals(kbBackspace)) {
            document.backspace();
            alignCursor();
        } else if (!keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
        ) {
            // Plain old keystroke, process it
            document.addChar(keypress.getKey().getChar());
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

}
