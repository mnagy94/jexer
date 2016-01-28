/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2016 Kevin Lamonte
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
package jexer.demos;

import jexer.*;

/**
 * This window demonstates the TRadioGroup, TRadioButton, and TCheckbox
 * widgets.
 */
public class DemoCheckboxWindow extends TWindow {

    /**
     * Constructor.
     *
     * @param parent the main application
     */
    DemoCheckboxWindow(final TApplication parent) {
        this(parent, CENTERED | RESIZABLE);
    }

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    DemoCheckboxWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it will be
        // centered on screen.
        super(parent, "Radiobuttons and Checkboxes", 0, 0, 60, 15, flags);

        int row = 1;

        // Add some widgets
        addLabel("Check box example 1", 1, row);
        addCheckbox(35, row++, "Checkbox 1", false);
        addLabel("Check box example 2", 1, row);
        addCheckbox(35, row++, "Checkbox 2", true);
        row += 2;

        TRadioGroup group = addRadioGroup(1, row, "Group 1");
        group.addRadioButton("Radio option 1");
        group.addRadioButton("Radio option 2");
        group.addRadioButton("Radio option 3");

        addButton("&Close Window", (getWidth() - 14) / 2, getHeight() - 4,
            new TAction() {
                public void DO() {
                    DemoCheckboxWindow.this.getApplication()
                        .closeWindow(DemoCheckboxWindow.this);
                }
            }
        );
    }

}
