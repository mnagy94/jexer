/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2022 Autumn Lamonte
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
 * @author Autumn Lamonte ⚧ Trans Liberation Now
 * @version 1
 */
package jexer.demos;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;

import jexer.TApplication;
import jexer.TButton;
import jexer.TDesktop;
import jexer.TEditColorThemeWindow;
import jexer.TEditorWindow;
import jexer.TWidget;
import jexer.TWindow;
import jexer.bits.BorderStyle;
import jexer.backend.ECMA48Terminal;
import jexer.event.TMenuEvent;
import jexer.menu.TMenu;
import jexer.menu.TMenuItem;
import jexer.menu.TSubMenu;
import jexer.backend.Backend;
import jexer.backend.SwingTerminal;

/**
 * The demo application itself.
 */
public class DemoApplication extends TApplication {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(DemoApplication.class.getName());

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

        getBackend().setTitle(i18n.getString("applicationTitle"));
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

        getBackend().setTitle(i18n.getString("applicationTitle"));
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
        // For the Swing demo, use an initial size of 82x28 so that a
        // terminal window precisely fits the window.
        super(backendType, (backendType == BackendType.SWING ? 82 : -1),
            (backendType == BackendType.SWING ? 28 : -1), 20);
        addAllWidgets();
        getBackend().setTitle(i18n.getString("applicationTitle"));

        // Use cute theme by default.
        onMenu(new TMenuEvent(getBackend(), 10001));
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

        if (menu.getId() == 10000) {
            new DemoMainWindow(this);
            return true;
        }

        if (menu.getId() == 10001) {
            // Look cute: switch the color theme, window borders, and button
            // styles.
            System.setProperty("jexer.TWindow.borderStyleForeground", "round");
            System.setProperty("jexer.TWindow.borderStyleModal", "round");
            System.setProperty("jexer.TWindow.borderStyleMoving", "round");
            System.setProperty("jexer.TWindow.borderStyleInactive", "round");
            System.setProperty("jexer.TEditColorTheme.borderStyle", "round");
            System.setProperty("jexer.TEditColorTheme.options.borderStyle", "round");
            System.setProperty("jexer.TRadioGroup.borderStyle", "round");
            System.setProperty("jexer.TScreenOptions.borderStyle", "round");
            System.setProperty("jexer.TScreenOptions.grid.borderStyle", "round");
            System.setProperty("jexer.TScreenOptions.options.borderStyle", "round");
            System.setProperty("jexer.TWindow.opacity", "80");
            System.setProperty("jexer.TImage.opacity", "80");
            System.setProperty("jexer.TTerminal.opacity", "80");
            System.setProperty("jexer.TButton.style", "round");

            getTheme().setFemme();
            for (TWindow window: getAllWindows()) {
                window.setBorderStyleForeground("round");
                window.setBorderStyleModal("round");
                window.setBorderStyleMoving("round");
                window.setBorderStyleInactive("round");
                window.setAlpha(80 * 255 / 100);

                for (TWidget widget: window.getChildren()) {
                    if (widget instanceof TButton) {
                        ((TButton) widget).setStyle(TButton.Style.ROUND);
                    }
                }
            }
            for (TMenu m: getAllMenus()) {
                m.setBorderStyleForeground("round");
                m.setBorderStyleModal("round");
                m.setBorderStyleMoving("round");
                m.setBorderStyleInactive("round");
                m.setAlpha(90 * 255 / 100);
            }
            setDesktop(null);
            setHideStatusBar(true);
            return true;
        }

        if (menu.getId() == 10002) {
            // Look bland: switch the color theme, window borders, and button
            // styles.
            System.clearProperty("jexer.TWindow.borderStyleForeground");
            System.clearProperty("jexer.TWindow.borderStyleModal");
            System.clearProperty("jexer.TWindow.borderStyleMoving");
            System.clearProperty("jexer.TWindow.borderStyleInactive");
            System.clearProperty("jexer.TEditColorTheme.borderStyle");
            System.clearProperty("jexer.TEditColorTheme.options.borderStyle");
            System.clearProperty("jexer.TRadioGroup.borderStyle");
            System.clearProperty("jexer.TScreenOptions.borderStyle");
            System.clearProperty("jexer.TScreenOptions.grid.borderStyle");
            System.clearProperty("jexer.TScreenOptions.options.borderStyle");
            System.clearProperty("jexer.TWindow.opacity");
            System.clearProperty("jexer.TImage.opacity");
            System.clearProperty("jexer.TTerminal.opacity");
            System.clearProperty("jexer.TButton.style");

            getTheme().setDefaultTheme();
            for (TWindow window: getAllWindows()) {
                window.setBorderStyleForeground(null);
                window.setBorderStyleModal(null);
                window.setBorderStyleMoving(null);
                window.setBorderStyleInactive(null);
                window.setAlpha(90 * 255 / 100);

                for (TWidget widget: window.getChildren()) {
                    if (widget instanceof TButton) {
                        ((TButton) widget).setStyle(TButton.Style.SQUARE);
                    }
                }
            }
            for (TMenu m: getAllMenus()) {
                m.setBorderStyleForeground(null);
                m.setBorderStyleModal(null);
                m.setBorderStyleMoving(null);
                m.setBorderStyleInactive(null);
                m.setAlpha(90 * 255 / 100);
            }
            setDesktop(new TDesktop(this));
            setHideStatusBar(false);
            return true;
        }

        return super.onMenu(menu);
    }

    /**
     * Show FPS.
     */
    @Override
    protected void onPreDraw() {
        if (getScreen() instanceof ECMA48Terminal) {
            ECMA48Terminal terminal = (ECMA48Terminal) getScreen();
            int bytes = terminal.getBytesPerSecond();
            String bps = "";
            if (bytes > 1024 * 1024 * 1024) {
                bps = String.format("%4.2f GB/s",
                    ((double) bytes / (1024 * 1024 * 1024)));
            } else if (bytes > 1024 * 1024) {
                bps = String.format("%4.2f MB/s",
                    ((double) bytes / (1024 * 1024)));
            } else if (bytes > 1024) {
                bps = String.format("%4.2f KB/s",
                    ((double) bytes / 1024));
            } else {
                bps = String.format("%d bytes/s", bytes);
            }
            menuTrayText = String.format("%s FPS %d", bps,
                getFramesPerSecond());
        } else {
            menuTrayText = String.format("FPS %d", getFramesPerSecond());
        }
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
        addToolMenu();
        addFileMenu();
        addEditMenu();

        TMenu demoMenu = addMenu(i18n.getString("demo"));
        demoMenu.addItem(10000, i18n.getString("mainWindow"));
        demoMenu.addSeparator();
        demoMenu.addItem(10001, i18n.getString("lookCute"));
        demoMenu.addItem(10002, i18n.getString("lookBland"));
        demoMenu.addSeparator();
        TMenuItem item = demoMenu.addItem(2000, i18n.getString("checkable"));
        item.setCheckable(true);
        item = demoMenu.addItem(2001, i18n.getString("disabled"));
        item.setEnabled(false);
        item = demoMenu.addItem(2002, i18n.getString("normal"));
        TSubMenu subMenu = demoMenu.addSubMenu(i18n.getString("subMenu"));
        item = demoMenu.addItem(2010, i18n.getString("normal"));
        item = demoMenu.addItem(2050, i18n.getString("colors"));

        item = subMenu.addItem(2000, i18n.getString("checkableSub"));
        item.setCheckable(true);
        item = subMenu.addItem(2001, i18n.getString("disabledSub"));
        item.setEnabled(false);
        item = subMenu.addItem(2002, i18n.getString("normalSub"));

        subMenu = subMenu.addSubMenu(i18n.getString("subMenu"));
        item = subMenu.addItem(2000, i18n.getString("checkableSub"));
        item.setCheckable(true);
        item = subMenu.addItem(2001, i18n.getString("disabledSub"));
        item.setEnabled(false);
        item = subMenu.addItem(2002, i18n.getString("normalSub"));

        if (getScreen() instanceof SwingTerminal) {
            TMenu swingMenu = addMenu(i18n.getString("swing"));
            item = swingMenu.addItem(3000, i18n.getString("bigger"));
            item = swingMenu.addItem(3001, i18n.getString("smaller"));
        }

        addTableMenu();
        addWindowMenu();
        addHelpMenu();
    }

}
