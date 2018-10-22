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

import jexer.bits.CellAttributes;
import jexer.bits.GraphicsChars;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import static jexer.TKeypress.*;

/**
 * TRadioButton implements a selectable radio button.
 */
public class TRadioButton extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * RadioButton state, true means selected.
     */
    private boolean selected = false;

    /**
     * Label for this radio button.
     */
    private String label;

    /**
     * ID for this radio button.  Buttons start counting at 1 in the
     * RadioGroup.
     */
    private int id;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display next to (right of) the radiobutton
     * @param id ID for this radio button
     */
    public TRadioButton(final TRadioGroup parent, final int x, final int y,
        final String label, final int id) {

        // Set parent and window
        super(parent, x, y, label.length() + 4, 1);

        this.label = label;
        this.id = id;

        setCursorVisible(true);
        setCursorX(1);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the radio button.
     *
     * @param mouse mouse event
     * @return if true the mouse is currently on the radio button
     */
    private boolean mouseOnRadioButton(final TMouseEvent mouse) {
        if ((mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() <= 2)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if ((mouseOnRadioButton(mouse)) && (mouse.isMouse1())) {
            // Switch state
            selected = !selected;
            if (selected) {
                ((TRadioGroup) getParent()).setSelected(this);
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

        if (keypress.equals(kbSpace)) {
            selected = !selected;
            if (selected) {
                ((TRadioGroup) getParent()).setSelected(this);
            }
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a radio button with label.
     */
    @Override
    public void draw() {
        CellAttributes radioButtonColor;

        if (isAbsoluteActive()) {
            radioButtonColor = getTheme().getColor("tradiobutton.active");
        } else {
            radioButtonColor = getTheme().getColor("tradiobutton.inactive");
        }

        putCharXY(0, 0, '(', radioButtonColor);
        if (selected) {
            putCharXY(1, 0, GraphicsChars.CP437[0x07], radioButtonColor);
        } else {
            putCharXY(1, 0, ' ', radioButtonColor);
        }
        putCharXY(2, 0, ')', radioButtonColor);
        putStringXY(4, 0, label, radioButtonColor);
    }

    // ------------------------------------------------------------------------
    // TRadioButton -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get RadioButton state, true means selected.
     *
     * @return if true then this is the one button in the group that is
     * selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Set RadioButton state, true means selected.  Note package private
     * access.
     *
     * @param selected if true then this is the one button in the group that
     * is selected
     */
    void setSelected(final boolean selected) {
        this.selected = selected;
    }

    /**
     * Get ID for this radio button.  Buttons start counting at 1 in the
     * RadioGroup.
     *
     * @return the ID
     */
    public int getId() {
        return id;
    }

}
