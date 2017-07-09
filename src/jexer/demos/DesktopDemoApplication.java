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
import java.util.*;

import jexer.*;
import jexer.event.*;
import jexer.menu.*;

/**
 * The demo application itself.
 */
public class DesktopDemoApplication extends TApplication {

    /**
     * Add all the widgets of the demo.
     */
    private void addAllWidgets() {

        // Add the menus
        addFileMenu();
        addEditMenu();
        addWindowMenu();
        addHelpMenu();

        final DesktopDemo desktop = new DesktopDemo(this);
        setDesktop(desktop);

        desktop.addButton("&Remove HATCH", 2, 5,
            new TAction() {
                public void DO() {
                    desktop.drawHatch = false;
                }
            }
        );
        desktop.addButton("&Show HATCH", 2, 8,
            new TAction() {
                public void DO() {
                    desktop.drawHatch = true;
                }
            }
        );
    }

    /**
     * Handle menu events.
     *
     * @param menu menu event
     * @return if true, the event was processed and should not be passed onto
     * a window
     */
    @Override
    public boolean onMenu(final TMenuEvent menu) {

        if (menu.getId() == TMenu.MID_OPEN_FILE) {
            try {
                String filename = fileOpenBox(".");
                 if (filename != null) {
                     try {
                         File file = new File(filename);
                         StringBuilder fileContents = new StringBuilder();
                         Scanner scanner = new Scanner(file);
                         String EOL = System.getProperty("line.separator");

                         try {
                             while (scanner.hasNextLine()) {
                                 fileContents.append(scanner.nextLine() + EOL);
                             }
                             new DemoTextWindow(this, filename,
                                 fileContents.toString());
                         } finally {
                             scanner.close();
                         }
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

    /**
     * Public constructor.
     *
     * @param backendType one of the TApplication.BackendType values
     * @throws Exception if TApplication can't instantiate the Backend.
     */
    public DesktopDemoApplication(final BackendType backendType) throws Exception {
        super(backendType);
        addAllWidgets();
        getBackend().setTitle("Jexer Demo Application");
    }
}
