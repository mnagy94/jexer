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
package jexer.demos;

import java.util.*;

import jexer.*;
import static jexer.TCommand.*;
import static jexer.TKeypress.*;

/**
 * This window demonstates the TField and TPasswordField widgets.
 */
public class DemoTextFieldWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Calendar.  Has to be at class scope so that it can be accessed by the
     * anonymous TAction class.
     */
    TCalendar calendar = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Constructor.
     *
     * @param parent the main application
     */
    DemoTextFieldWindow(final TApplication parent) {
        this(parent, TWindow.CENTERED | TWindow.RESIZABLE);
    }

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    DemoTextFieldWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it
        // will be centered on screen.
        super(parent, "Text Fields", 0, 0, 60, 20, flags);

        int row = 1;

        addLabel("Variable-width text field:", 1, row);
        addField(35, row++, 15, false, "Field text");
        addLabel("Fixed-width text field:", 1, row);
        addField(35, row++, 15, true);
        addLabel("Variable-width password:", 1, row);
        addPasswordField(35, row++, 15, false);
        addLabel("Fixed-width password:", 1, row);
        addPasswordField(35, row++, 15, true, "hunter2");
        addLabel("Very long text field:", 1, row);
        TField selected = addField(35, row++, 40, false,
            "Very very long field text that should be outside the window");
        row += 1;

        calendar = addCalendar(1, row++,
            new TAction() {
                public void DO() {
                    getApplication().messageBox("Calendar",
                        "You selected the following date:\n" +
                        "\n" +
                        new Date(calendar.getValue().getTimeInMillis()) +
                        "\n",
                        TMessageBox.Type.OK);
                }
            }
        );

        addButton("&Close Window", (getWidth() - 14) / 2, getHeight() - 4,
            new TAction() {
                public void DO() {
                    getApplication().closeWindow(DemoTextFieldWindow.this);
                }
            }
        );

        activate(selected);

        statusBar = newStatusBar("Text fields");
        statusBar.addShortcutKeypress(kbF1, cmHelp, "Help");
        statusBar.addShortcutKeypress(kbF2, cmShell, "Shell");
        statusBar.addShortcutKeypress(kbF3, cmOpen, "Open");
        statusBar.addShortcutKeypress(kbF10, cmExit, "Exit");
    }

}
