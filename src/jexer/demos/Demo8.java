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
package jexer.demos;

import java.util.ResourceBundle;

import jexer.TApplication;
import jexer.TPanel;
import jexer.TSplitPane;
import jexer.TTerminalWidget;
import jexer.TText;
import jexer.TWindow;
import jexer.layout.BoxLayoutManager;
import jexer.menu.TMenu;

/**
 * This class shows off TSplitPane.
 */
public class Demo8 {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(Demo8.class.getName());

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static String TEXT =
"This is an example of a reflowable text field.  Some example text follows.\n" +
"\n" +
"Notice that some menu items should be disabled when this window has focus.\n" +
"\n" +
"This library implements a text-based windowing system loosely " +
"reminiscent of Borland's [Turbo " +
"Vision](http://en.wikipedia.org/wiki/Turbo_Vision) library.  For those " +
"wishing to use the actual C++ Turbo Vision library, see [Sergio " +
"Sigala's updated version](http://tvision.sourceforge.net/) that runs " +
"on many more platforms.\n" +
"\n" +
"This library is licensed MIT.  See the file LICENSE for the full license " +
"for the details.\n";

    // ------------------------------------------------------------------------
    // Demo8 ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) throws Exception {
        // This demo will build everything "from the outside".

        // Swing is the default backend on Windows unless explicitly
        // overridden by jexer.Swing.
        TApplication.BackendType backendType = TApplication.BackendType.XTERM;
        if (System.getProperty("os.name").startsWith("Windows")) {
            backendType = TApplication.BackendType.SWING;
        }
        if (System.getProperty("os.name").startsWith("Mac")) {
            backendType = TApplication.BackendType.SWING;
        }
        if (System.getProperty("jexer.Swing") != null) {
            if (System.getProperty("jexer.Swing", "false").equals("true")) {
                backendType = TApplication.BackendType.SWING;
            } else {
                backendType = TApplication.BackendType.XTERM;
            }
        }

        // For this demo, let's disable the status bar.
        System.setProperty("jexer.hideStatusBar", "true");

        TApplication app = new TApplication(backendType);
        app.addToolMenu();
        app.addFileMenu();
        TWindow window = new TWindow(app, i18n.getString("windowTitle"),
            60, 22);

        TMenu paneMenu = app.addMenu(i18n.getString("paneMenu"));
        paneMenu.addDefaultItem(TMenu.MID_SPLIT_VERTICAL, true);
        paneMenu.addDefaultItem(TMenu.MID_SPLIT_HORIZONTAL, true);

        TSplitPane pane = window.addSplitPane(0, 0, window.getWidth() - 2,
            window.getHeight() - 2, true);

        // pane.setLeft(new TText(null, TEXT, 0, 0, 10, 10));
        // pane.setRight(new TText(null, TEXT, 0, 0, 10, 10));

        // For this demo, let's require ptypipe
        System.setProperty("jexer.TTerminal.ptypipe", "true");
        pane.setLeft(new TTerminalWidget(null, 0, 0));
        pane.setRight(new TTerminalWidget(null, 0, 0));

        app.run();
    }

}
