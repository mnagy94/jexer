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

import jexer.*;
import static jexer.TCommand.*;
import static jexer.TKeypress.*;

/**
 * This window demonstates the TMessageBox and TInputBox widgets.
 */
public class DemoMsgBoxWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Constructor.
     *
     * @param parent the main application
     */
    DemoMsgBoxWindow(final TApplication parent) {
        this(parent, TWindow.CENTERED | TWindow.RESIZABLE);
    }

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    DemoMsgBoxWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it
        // will be centered on screen.
        super(parent, "Message Boxes", 0, 0, 60, 16, flags);

        int row = 1;

        // Add some widgets
        addLabel("Default OK message box", 1, row);
        addButton("Open O&K MB", 35, row,
            new TAction() {
                public void DO() {
                    getApplication().messageBox("OK MessageBox",
"This is an example of a OK MessageBox.  This is the\n" +
"default MessageBox.\n" +
"\n" +
"Note that the MessageBox text can span multiple\n" +
"lines.\n" +
"\n" +
"The default result (if someone hits the top-left\n" +
"close button) is OK.\n",
                        TMessageBox.Type.OK);
                }
            }
        );
        row += 2;

        addLabel("OK/Cancel message box", 1, row);
        addButton("O&pen OKC MB", 35, row,
            new TAction() {
                public void DO() {
                    getApplication().messageBox("OK/Cancel MessageBox",
"This is an example of a OK/Cancel MessageBox.\n" +
"\n" +
"Note that the MessageBox text can span multiple\n" +
"lines.\n" +
"\n" +
"The default result (if someone hits the top-left\n" +
"close button) is CANCEL.\n",
                        TMessageBox.Type.OKCANCEL);
                }
            }
        );
        row += 2;

        addLabel("Yes/No message box", 1, row);
        addButton("Open &YN MB", 35, row,
            new TAction() {
                public void DO() {
                    getApplication().messageBox("Yes/No MessageBox",
"This is an example of a Yes/No MessageBox.\n" +
"\n" +
"Note that the MessageBox text can span multiple\n" +
"lines.\n" +
"\n" +
"The default result (if someone hits the top-left\n" +
"close button) is NO.\n",
                        TMessageBox.Type.YESNO);
                }
            }
        );
        row += 2;

        addLabel("Yes/No/Cancel message box", 1, row);
        addButton("Ope&n YNC MB", 35, row,
            new TAction() {
                public void DO() {
                    getApplication().messageBox("Yes/No/Cancel MessageBox",
"This is an example of a Yes/No/Cancel MessageBox.\n" +
"\n" +
"Note that the MessageBox text can span multiple\n" +
"lines.\n" +
"\n" +
"The default result (if someone hits the top-left\n" +
"close button) is CANCEL.\n",
                        TMessageBox.Type.YESNOCANCEL);
                }
            }
        );
        row += 2;

        addLabel("Input box", 1, row);
        addButton("Open &input box", 35, row,
            new TAction() {
                public void DO() {
                    TInputBox in = getApplication().inputBox("Input Box",
"This is an example of an InputBox.\n" +
"\n" +
"Note that the InputBox text can span multiple\n" +
"lines.\n",
                        "some input text");
                    getApplication().messageBox("Your InputBox Answer",
                        "You entered: " + in.getText());
                }
            }
        );

        addButton("&Close Window", (getWidth() - 14) / 2, getHeight() - 4,
            new TAction() {
                public void DO() {
                    getApplication().closeWindow(DemoMsgBoxWindow.this);
                }
            }
        );

        statusBar = newStatusBar("Message boxes");
        statusBar.addShortcutKeypress(kbF1, cmHelp, "Help");
        statusBar.addShortcutKeypress(kbF2, cmShell, "Shell");
        statusBar.addShortcutKeypress(kbF3, cmOpen, "Open");
        statusBar.addShortcutKeypress(kbF10, cmExit, "Exit");
    }
}
