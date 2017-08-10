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
        document = new Document(text);
    }

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
        // Setup my color
        CellAttributes color = getTheme().getColor("teditor");

        int lineNumber = document.getLineNumber();
        for (int i = 0; i < getHeight(); i++) {
            // Background line
            getScreen().hLineXY(0, i, getWidth(), ' ', color);

            // Now draw document's line
            if (lineNumber + i < document.getLineCount()) {
                Line line = document.getLine(lineNumber + i);
                int x = 0;
                for (Word word: line.getWords()) {
                    getScreen().putStringXY(x, i, word.getText(),
                        word.getColor());
                    x += word.getDisplayLength();
                    if (x > getWidth()) {
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
            document.up();
            return;
        }
        if (mouse.isMouseWheelDown()) {
            document.down();
            return;
        }

        // TODO: click sets row and column

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
        if (keypress.equals(kbLeft)) {
            document.left();
        } else if (keypress.equals(kbRight)) {
            document.right();
        } else if (keypress.equals(kbUp)) {
            document.up();
        } else if (keypress.equals(kbDown)) {
            document.down();
        } else if (keypress.equals(kbPgUp)) {
            document.up(getHeight() - 1);
        } else if (keypress.equals(kbPgDn)) {
            document.down(getHeight() - 1);
        } else if (keypress.equals(kbHome)) {
            document.home();
        } else if (keypress.equals(kbEnd)) {
            document.end();
        } else if (keypress.equals(kbCtrlHome)) {
            document.setLineNumber(0);
            document.home();
        } else if (keypress.equals(kbCtrlEnd)) {
            document.setLineNumber(document.getLineCount() - 1);
            document.end();
        } else if (keypress.equals(kbIns)) {
            document.setOverwrite(!document.getOverwrite());
        } else if (keypress.equals(kbDel)) {
            document.del();
        } else if (keypress.equals(kbBackspace)) {
            document.backspace();
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

}
