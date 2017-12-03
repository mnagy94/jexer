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
import jexer.bits.GraphicsChars;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import static jexer.TKeypress.*;

/**
 * TField implements an editable text field.
 */
public class TField extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Field text.
     */
    protected String text = "";

    /**
     * If true, only allow enough characters that will fit in the width.  If
     * false, allow the field to scroll to the right.
     */
    protected boolean fixed = false;

    /**
     * Current editing position within text.
     */
    protected int position = 0;

    /**
     * Beginning of visible portion.
     */
    protected int windowStart = 0;

    /**
     * If true, new characters are inserted at position.
     */
    protected boolean insertMode = true;

    /**
     * Remember mouse state.
     */
    protected TMouseEvent mouse;

    /**
     * The action to perform when the user presses enter.
     */
    protected TAction enterAction;

    /**
     * The action to perform when the text is updated.
     */
    protected TAction updateAction;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     */
    public TField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed) {

        this(parent, x, y, width, fixed, "", null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     * @param text initial text, default is empty string
     */
    public TField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed, final String text) {

        this(parent, x, y, width, fixed, text, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width visible text width
     * @param fixed if true, the text cannot exceed the display width
     * @param text initial text, default is empty string
     * @param enterAction function to call when enter key is pressed
     * @param updateAction function to call when the text is updated
     */
    public TField(final TWidget parent, final int x, final int y,
        final int width, final boolean fixed, final String text,
        final TAction enterAction, final TAction updateAction) {

        // Set parent and window
        super(parent, x, y, width, 1);

        setCursorVisible(true);
        this.fixed = fixed;
        this.text = text;
        this.enterAction = enterAction;
        this.updateAction = updateAction;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the field.
     *
     * @return if true the mouse is currently on the field
     */
    protected boolean mouseOnField() {
        int rightEdge = getWidth() - 1;
        if ((mouse != null)
            && (mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() <= rightEdge)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        if ((mouseOnField()) && (mouse.isMouse1())) {
            // Move cursor
            int deltaX = mouse.getX() - getCursorX();
            position += deltaX;
            if (position > text.length()) {
                position = text.length();
            }
            updateCursor();
            return;
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        if (keypress.equals(kbLeft)) {
            if (position > 0) {
                position--;
            }
            if (fixed == false) {
                if ((position == windowStart) && (windowStart > 0)) {
                    windowStart--;
                }
            }
            normalizeWindowStart();
            return;
        }

        if (keypress.equals(kbRight)) {
            if (position < text.length()) {
                position++;
                if (fixed == true) {
                    if (position == getWidth()) {
                        position--;
                    }
                } else {
                    if ((position - windowStart) == getWidth()) {
                        windowStart++;
                    }
                }
            }
            return;
        }

        if (keypress.equals(kbEnter)) {
            dispatch(true);
            return;
        }

        if (keypress.equals(kbIns)) {
            insertMode = !insertMode;
            return;
        }
        if (keypress.equals(kbHome)) {
            home();
            return;
        }

        if (keypress.equals(kbEnd)) {
            end();
            return;
        }

        if (keypress.equals(kbDel)) {
            if ((text.length() > 0) && (position < text.length())) {
                text = text.substring(0, position)
                        + text.substring(position + 1);
            }
            dispatch(false);
            return;
        }

        if (keypress.equals(kbBackspace) || keypress.equals(kbBackspaceDel)) {
            if (position > 0) {
                position--;
                text = text.substring(0, position)
                        + text.substring(position + 1);
            }
            if (fixed == false) {
                if ((position == windowStart)
                    && (windowStart > 0)
                ) {
                    windowStart--;
                }
            }
            dispatch(false);
            normalizeWindowStart();
            return;
        }

        if (!keypress.getKey().isFnKey()
            && !keypress.getKey().isAlt()
            && !keypress.getKey().isCtrl()
        ) {
            // Plain old keystroke, process it
            if ((position == text.length())
                && (text.length() < getWidth())) {

                // Append case
                appendChar(keypress.getKey().getChar());
            } else if ((position < text.length())
                && (text.length() < getWidth())) {

                // Overwrite or insert a character
                if (insertMode == false) {
                    // Replace character
                    text = text.substring(0, position)
                            + keypress.getKey().getChar()
                            + text.substring(position + 1);
                    position++;
                } else {
                    // Insert character
                    insertChar(keypress.getKey().getChar());
                }
            } else if ((position < text.length())
                && (text.length() >= getWidth())) {

                // Multiple cases here
                if ((fixed == true) && (insertMode == true)) {
                    // Buffer is full, do nothing
                } else if ((fixed == true) && (insertMode == false)) {
                    // Overwrite the last character, maybe move position
                    text = text.substring(0, position)
                            + keypress.getKey().getChar()
                            + text.substring(position + 1);
                    if (position < getWidth() - 1) {
                        position++;
                    }
                } else if ((fixed == false) && (insertMode == false)) {
                    // Overwrite the last character, definitely move position
                    text = text.substring(0, position)
                            + keypress.getKey().getChar()
                            + text.substring(position + 1);
                    position++;
                } else {
                    if (position == text.length()) {
                        // Append this character
                        appendChar(keypress.getKey().getChar());
                    } else {
                        // Insert this character
                        insertChar(keypress.getKey().getChar());
                    }
                }
            } else {
                assert (!fixed);

                // Append this character
                appendChar(keypress.getKey().getChar());
            }
            dispatch(false);
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the text field.
     */
    @Override
    public void draw() {
        CellAttributes fieldColor;

        if (isAbsoluteActive()) {
            fieldColor = getTheme().getColor("tfield.active");
        } else {
            fieldColor = getTheme().getColor("tfield.inactive");
        }

        int end = windowStart + getWidth();
        if (end > text.length()) {
            end = text.length();
        }
        getScreen().hLineXY(0, 0, getWidth(), GraphicsChars.HATCH, fieldColor);
        getScreen().putStringXY(0, 0, text.substring(windowStart, end),
            fieldColor);

        // Fix the cursor, it will be rendered by TApplication.drawAll().
        updateCursor();
    }

    // ------------------------------------------------------------------------
    // TField -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get field text.
     *
     * @return field text
     */
    public final String getText() {
        return text;
    }

    /**
     * Set field text.
     *
     * @param text the new field text
     */
    public final void setText(String text) {
        this.text = text;
        position = 0;
        windowStart = 0;
    }

    /**
     * Dispatch to the action function.
     *
     * @param enter if true, the user pressed Enter, else this was an update
     * to the text.
     */
    protected void dispatch(final boolean enter) {
        if (enter) {
            if (enterAction != null) {
                enterAction.DO();
            }
        } else {
            if (updateAction != null) {
                updateAction.DO();
            }
        }
    }

    /**
     * Update the visible cursor position to match the location of position
     * and windowStart.
     */
    protected void updateCursor() {
        if ((position > getWidth()) && fixed) {
            setCursorX(getWidth());
        } else if ((position - windowStart == getWidth()) && !fixed) {
            setCursorX(getWidth() - 1);
        } else {
            setCursorX(position - windowStart);
        }
    }

    /**
     * Normalize windowStart such that most of the field data if visible.
     */
    protected void normalizeWindowStart() {
        if (fixed) {
            // windowStart had better be zero, there is nothing to do here.
            assert (windowStart == 0);
            return;
        }
        windowStart = position - (getWidth() - 1);
        if (windowStart < 0) {
            windowStart = 0;
        }

        updateCursor();
    }

    /**
     * Append char to the end of the field.
     *
     * @param ch = char to append
     */
    protected void appendChar(final char ch) {
        // Append the LAST character
        text += ch;
        position++;

        assert (position == text.length());

        if (fixed) {
            if (position == getWidth()) {
                position--;
            }
        } else {
            if ((position - windowStart) == getWidth()) {
                windowStart++;
            }
        }
    }

    /**
     * Insert char somewhere in the middle of the field.
     *
     * @param ch char to append
     */
    protected void insertChar(final char ch) {
        text = text.substring(0, position) + ch + text.substring(position);
        position++;
        if ((position - windowStart) == getWidth()) {
            assert (!fixed);
            windowStart++;
        }
    }

    /**
     * Position the cursor at the first column.  The field may adjust the
     * window start to show as much of the field as possible.
     */
    public void home() {
        position = 0;
        windowStart = 0;
    }

    /**
     * Set the editing position to the last filled character.  The field may
     * adjust the window start to show as much of the field as possible.
     */
    public void end() {
        position = text.length();
        if (fixed == true) {
            if (position >= getWidth()) {
                position = text.length() - 1;
            }
        } else {
            windowStart = text.length() - getWidth() + 1;
            if (windowStart < 0) {
                windowStart = 0;
            }
        }
    }

    /**
     * Set the editing position.  The field may adjust the window start to
     * show as much of the field as possible.
     *
     * @param position the new position
     * @throws IndexOutOfBoundsException if position is outside the range of
     * the available text
     */
    public void setPosition(final int position) {
        if ((position < 0) || (position >= text.length())) {
            throw new IndexOutOfBoundsException("Max length is " +
                text.length() + ", requested position " + position);
        }
        this.position = position;
        normalizeWindowStart();
    }

}
