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

import java.io.*;

import jexer.*;
import jexer.event.*;
import jexer.menu.*;
import jexer.backend.Backend;
import jexer.backend.SwingTerminal;

/**
 * The demo application itself.
 */
public class DemoApplication extends TApplication {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; shutdown() will (blindly!) put System.in in cooked
     * mode.  input is always converted to a Reader with UTF-8 encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    public DemoApplication(final InputStream input,
        final OutputStream output) throws UnsupportedEncodingException {
        super(input, output);
        addAllWidgets();

        getBackend().setTitle("Jexer Demo Application");
    }

    /**
     * Public constructor.
     *
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
     * @param setRawMode if true, set System.in into raw mode with stty.
     * This should in general not be used.  It is here solely for Demo3,
     * which uses System.in.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    public DemoApplication(final InputStream input, final Reader reader,
        final PrintWriter writer, final boolean setRawMode) {
        super(input, reader, writer, setRawMode);
        addAllWidgets();

        getBackend().setTitle("Jexer Demo Application");
    }

    /**
     * Public constructor.
     *
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    public DemoApplication(final InputStream input, final Reader reader,
        final PrintWriter writer) {

        this(input, reader, writer, false);
    }

    /**
     * Public constructor.
     *
     * @param backend a Backend that is already ready to go.
     */
    public DemoApplication(final Backend backend) {
        super(backend);

        addAllWidgets();
    }

    /**
     * Public constructor.
     *
     * @param backendType one of the TApplication.BackendType values
     * @throws Exception if TApplication can't instantiate the Backend.
     */
    public DemoApplication(final BackendType backendType) throws Exception {
        super(backendType);
        addAllWidgets();
        getBackend().setTitle("Jexer Demo Application");
    }

    // ------------------------------------------------------------------------
    // TApplication -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle menu events.
     *
     * @param menu menu event
     * @return if true, the event was processed and should not be passed onto
     * a window
     */
    @Override
    public boolean onMenu(final TMenuEvent menu) {

        if (menu.getId() == 3000) {
            // Bigger +2
            assert (getScreen() instanceof SwingTerminal);
            SwingTerminal terminal = (SwingTerminal) getScreen();
            terminal.setFontSize(terminal.getFontSize() + 2);
            return true;
        }
        if (menu.getId() == 3001) {
            // Smaller -2
            assert (getScreen() instanceof SwingTerminal);
            SwingTerminal terminal = (SwingTerminal) getScreen();
            terminal.setFontSize(terminal.getFontSize() - 2);
            return true;
        }

        if (menu.getId() == 2050) {
            new TEditColorThemeWindow(this);
            return true;
        }

        if (menu.getId() == TMenu.MID_OPEN_FILE) {
            try {
                String filename = fileOpenBox(".");
                 if (filename != null) {
                     try {
                         new TEditorWindow(this, new File(filename));
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return super.onMenu(menu);
    }

    // ------------------------------------------------------------------------
    // DemoApplication --------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Add all the widgets of the demo.
     */
    private void addAllWidgets() {
        new DemoMainWindow(this);

        // Add the menus
        addFileMenu();
        addEditMenu();

        TMenu demoMenu = addMenu("&Demo");
        TMenuItem item = demoMenu.addItem(2000, "&Checkable");
        item.setCheckable(true);
        item = demoMenu.addItem(2001, "Disabled");
        item.setEnabled(false);
        item = demoMenu.addItem(2002, "&Normal");
        TSubMenu subMenu = demoMenu.addSubMenu("Sub-&Menu");
        item = demoMenu.addItem(2010, "N&ormal A&&D");
        item = demoMenu.addItem(2050, "Co&lors...");

        item = subMenu.addItem(2000, "&Checkable (sub)");
        item.setCheckable(true);
        item = subMenu.addItem(2001, "Disabled (sub)");
        item.setEnabled(false);
        item = subMenu.addItem(2002, "&Normal (sub)");

        subMenu = subMenu.addSubMenu("Sub-&Menu");
        item = subMenu.addItem(2000, "&Checkable (sub)");
        item.setCheckable(true);
        item = subMenu.addItem(2001, "Disabled (sub)");
        item.setEnabled(false);
        item = subMenu.addItem(2002, "&Normal (sub)");

        if (getScreen() instanceof SwingTerminal) {
            TMenu swingMenu = addMenu("Swin&g");
            item = swingMenu.addItem(3000, "&Bigger +2");
            item = swingMenu.addItem(3001, "&Smaller -2");
        }

        addWindowMenu();
        addHelpMenu();
    }

}
