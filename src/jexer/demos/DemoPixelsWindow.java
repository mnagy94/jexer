/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2021 Autumn Lamonte
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
 * @author Autumn Lamonte [AutumnWalksTheLake@gmail.com] ⚧ Trans Liberation Now
 * @version 1
 */
package jexer.demos;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

import jexer.TAction;
import jexer.TApplication;
import jexer.TEditColorThemeWindow;
import jexer.TEditorWindow;
import jexer.TLabel;
import jexer.TProgressBar;
import jexer.TTableWindow;
import jexer.TTimer;
import jexer.TWidget;
import jexer.TWindow;
import jexer.event.TCommandEvent;
import jexer.layout.StretchLayoutManager;
import jexer.tackboard.Bitmap;
import jexer.tackboard.TackboardItem;
import static jexer.TCommand.*;
import static jexer.TKeypress.*;

/**
 * This is the main "demo" application window.  It makes use of the TTimer,
 * TProgressBox, TLabel, TButton, and TField widgets.
 */
public class DemoPixelsWindow extends TWindow {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(DemoPixelsWindow.class.getName());

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Timer that moves things.
     */
    private TTimer timer3;

    /**
     * Timer label is updated with timer ticks.
     */
    TLabel timerLabel;

    /**
     * Direction for the bitmaps to move.
     */
    boolean direction = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent the main application
     */
    public DemoPixelsWindow(final TApplication parent) {
        // Construct a demo window.  X and Y don't matter because it will be
        // centered on screen.
        super(parent, i18n.getString("windowTitle"), 0, 0, 64, 11,
            CENTERED | RESIZABLE);

        setLayoutManager(new StretchLayoutManager(getWidth() - 2,
                getHeight() - 2));

        int row = 1;

        // Add some widgets
        addLabel(i18n.getString("customMouseLabel"), 1, row);
        TWidget first = addButton(i18n.getString("customMouseButton"), 35, row,
            new TAction() {
                public void DO() {
                    TackboardItem mouse = getApplication().getCustomMousePointer();
                    if (mouse != null) {
                        // Turn it off.
                        getApplication().setCustomMousePointer(null);
                        getApplication().getBackend().setPixelMouse(false);
                    } else {
                        // Turn it on.
                        try {
                            ClassLoader loader;
                            loader = Thread.currentThread().getContextClassLoader();
                            BufferedImage image;
                            image = ImageIO.read(loader.
                                getResource("cute_icon.png"));
                            getApplication().setCustomMousePointer(new Bitmap(0,
                                    0, 0, image));
                            getApplication().getBackend().setPixelMouse(true);
                        } catch (Exception e) {
                            new jexer.TExceptionDialog(getApplication(), e);
                        }
                    }
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("floatingTextLabel"), 1, row);
        addButton(i18n.getString("floatingTextButton"), 35, row,
            new TAction() {
                public void DO() {
                    // TODO
                }
            }
        );
        row += 2;

        // TODO: more things

        // Put some floating hearts on the screen.
        try {
            ClassLoader loader;
            loader = Thread.currentThread().getContextClassLoader();
            BufferedImage image;
            image = ImageIO.read(loader.getResource("trans_icon.png"));
            addUnderlay(new Bitmap(17, 33, 0, image));
            addOverlay(new Bitmap(11, 97, 0, image));

            timer3 = getApplication().addTimer(100, true,
                new TAction() {
                    public void DO() {
                        List<TackboardItem> items;
                        items = new ArrayList<TackboardItem>();
                        if (underlay != null) {
                            items.addAll(underlay.getItems());
                        }
                        if (overlay != null) {
                            items.addAll(overlay.getItems());
                        }
                        int i = 0;
                        for (TackboardItem item: items) {
                            i++;
                            int x = item.getX();
                            int y = item.getY();
                            if (i % 2 == 0) {
                                if (direction) {
                                    item.setX(x + 1);
                                } else {
                                    item.setX(x - 1);
                                }
                            } else {
                                if (direction) {
                                    item.setY(y + 1);
                                } else {
                                    item.setY(y - 1);
                                }
                            }
                            if ((item.getX() < 0)
                                || (item.getX() > 100)
                                || (item.getY() < 0)
                                || (item.getY() > 100)
                            ) {
                                direction = !direction;
                            }
                        }
                    }
                }
            );
        } catch (Exception e) {
            new jexer.TExceptionDialog(getApplication(), e);
        }

        activate(first);

        statusBar = newStatusBar(i18n.getString("statusBar"));
        statusBar.addShortcutKeypress(kbF1, cmHelp,
            i18n.getString("statusBarHelp"));
        statusBar.addShortcutKeypress(kbF2, cmShell,
            i18n.getString("statusBarShell"));
        statusBar.addShortcutKeypress(kbF3, cmOpen,
            i18n.getString("statusBarOpen"));
        statusBar.addShortcutKeypress(kbF10, cmExit,
            i18n.getString("statusBarExit"));
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * We need to override onClose so that the timer will no longer be called
     * after we close the window.  TTimers currently are completely unaware
     * of the rest of the UI classes.
     */
    @Override
    public void onClose() {
        getApplication().removeTimer(timer3);
    }

    /**
     * Method that subclasses can override to handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmOpen)) {
            try {
                String filename = fileOpenBox(".");
                if (filename != null) {
                    try {
                        new TEditorWindow(getApplication(),
                            new File(filename));
                    } catch (IOException e) {
                        messageBox(i18n.getString("errorTitle"),
                            MessageFormat.format(i18n.
                                getString("errorReadingFile"), e.getMessage()));
                    }
                }
            } catch (IOException e) {
                        messageBox(i18n.getString("errorTitle"),
                            MessageFormat.format(i18n.
                                getString("errorOpeningFile"), e.getMessage()));
            }
            return;
        }

        // Didn't handle it, let children get it instead
        super.onCommand(command);
    }

}
