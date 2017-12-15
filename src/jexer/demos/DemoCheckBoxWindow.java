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

import java.util.ArrayList;
import java.util.List;

import jexer.*;
import static jexer.TCommand.*;
import static jexer.TKeypress.*;

/**
 * This window demonstates the TRadioGroup, TRadioButton, and TCheckBox
 * widgets.
 */
public class DemoCheckBoxWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Combo box.  Has to be at class scope so that it can be accessed by the
     * anonymous TAction class.
     */
    TComboBox comboBox = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Constructor.
     *
     * @param parent the main application
     */
    DemoCheckBoxWindow(final TApplication parent) {
        this(parent, CENTERED | RESIZABLE);
    }

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    DemoCheckBoxWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it will be
        // centered on screen.
        super(parent, "Radiobuttons, CheckBoxes, and ComboBox",
            0, 0, 60, 17, flags);

        int row = 1;

        // Add some widgets
        addLabel("Check box example 1", 1, row);
        addCheckBox(35, row++, "CheckBox 1", false);
        addLabel("Check box example 2", 1, row);
        addCheckBox(35, row++, "CheckBox 2", true);
        row += 2;

        TRadioGroup group = addRadioGroup(1, row, "Group 1");
        group.addRadioButton("Radio option 1");
        group.addRadioButton("Radio option 2");
        group.addRadioButton("Radio option 3");

        List<String> comboValues = new ArrayList<String>();
        comboValues.add("String 0");
        comboValues.add("String 1");
        comboValues.add("String 2");
        comboValues.add("String 3");
        comboValues.add("String 4");
        comboValues.add("String 5");
        comboValues.add("String 6");
        comboValues.add("String 7");
        comboValues.add("String 8");
        comboValues.add("String 9");
        comboValues.add("String 10");

        comboBox = addComboBox(35, row, 12, comboValues, 2, 6,
            new TAction() {
                public void DO() {
                    getApplication().messageBox("ComboBox",
                        "You selected the following value:\n" +
                        "\n" +
                        comboBox.getText() +
                        "\n",
                        TMessageBox.Type.OK);
                }
            }
        );

        addButton("&Close Window", (getWidth() - 14) / 2, getHeight() - 4,
            new TAction() {
                public void DO() {
                    DemoCheckBoxWindow.this.getApplication()
                        .closeWindow(DemoCheckBoxWindow.this);
                }
            }
        );

        statusBar = newStatusBar("Radiobuttons and checkboxes");
        statusBar.addShortcutKeypress(kbF1, cmHelp, "Help");
        statusBar.addShortcutKeypress(kbF2, cmShell, "Shell");
        statusBar.addShortcutKeypress(kbF3, cmOpen, "Open");
        statusBar.addShortcutKeypress(kbF10, cmExit, "Exit");
    }

}
