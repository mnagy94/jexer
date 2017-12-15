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

import static jexer.TKeypress.kbSpace;
import jexer.bits.CellAttributes;
import jexer.bits.GraphicsChars;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;

/**
 * TCheckBox implements an on/off checkbox.
 */
public class TCheckBox extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Checkbox state, true means checked.
     */
    private boolean checked = false;

    /**
     * Label for this checkbox.
     */
    private String label;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display next to (right of) the checkbox
     * @param checked initial check state
     */
    public TCheckBox(final TWidget parent, final int x, final int y,
        final String label, final boolean checked) {

        // Set parent and window
        super(parent, x, y, label.length() + 4, 1);

        this.label = label;
        this.checked = checked;

        setCursorVisible(true);
        setCursorX(1);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the checkbox.
     *
     * @param mouse mouse event
     * @return true if the mouse is currently on the checkbox
     */
    private boolean mouseOnCheckbox(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() <= 2)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse checkbox presses.
     *
     * @param mouse mouse button down event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if ((mouseOnCheckbox(mouse)) && (mouse.isMouse1())) {
            // Switch state
            checked = !checked;
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbSpace)) {
            checked = !checked;
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a checkbox with label.
     */
    @Override
    public void draw() {
        CellAttributes checkboxColor;

        if (isAbsoluteActive()) {
            checkboxColor = getTheme().getColor("tcheckbox.active");
        } else {
            checkboxColor = getTheme().getColor("tcheckbox.inactive");
        }

        getScreen().putCharXY(0, 0, '[', checkboxColor);
        if (checked) {
            getScreen().putCharXY(1, 0, GraphicsChars.CHECK, checkboxColor);
        } else {
            getScreen().putCharXY(1, 0, ' ', checkboxColor);
        }
        getScreen().putCharXY(2, 0, ']', checkboxColor);
        getScreen().putStringXY(4, 0, label, checkboxColor);
    }

    // ------------------------------------------------------------------------
    // TCheckBox --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get checked value.
     *
     * @return if true, this is checked
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Set checked value.
     *
     * @param checked new checked value.
     */
    public void setChecked(final boolean checked) {
        this.checked = checked;
    }

}
