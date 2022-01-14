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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import jexer.TAction;
import jexer.TApplication;
import jexer.TEditorWindow;
import jexer.TLabel;
import jexer.TTimer;
import jexer.TWidget;
import jexer.TWindow;
import jexer.bits.Animation;
import jexer.bits.ImageUtils;
import jexer.event.TCommandEvent;
import jexer.layout.StretchLayoutManager;
import jexer.tackboard.Bitmap;
import jexer.tackboard.MousePointer;
import jexer.tackboard.TackboardItem;
import jexer.tackboard.Text;
import static jexer.TCommand.*;
import static jexer.TKeypress.*;

/**
 * Pixel-based operations.
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

    /**
     * The floating text.
     */
    Text floatingText = null;

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
        super(parent, i18n.getString("windowTitle"), 0, 0, 64, 17,
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
                    } else {
                        // Turn it on.
                        try {
                            ClassLoader loader;
                            loader = Thread.currentThread().getContextClassLoader();
                            BufferedImage image;
                            image = ImageIO.read(loader.
                                getResource("demo/cute_icon.png"));
                            TApplication app = getApplication();
                            app.setCustomMousePointer(new MousePointer(0, 0, 0,
                                    image, image.getWidth() / 2,
                                    image.getHeight() / 2));
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
                    if (floatingText == null) {
                        int fontSize = 31;
                        Font fontRoot = null;
                        Font font = null;
                        try {
                            ClassLoader loader = Thread.currentThread().getContextClassLoader();
                            InputStream in = loader.getResourceAsStream("demo/5thgradecursive.ttf");
                            fontRoot = Font.createFont(Font.TRUETYPE_FONT, in);
                            font = fontRoot.deriveFont(Font.PLAIN, fontSize);
                        } catch (FontFormatException e) {
                            font = new Font(Font.SANS_SERIF, Font.PLAIN,
                                fontSize);
                        } catch (IOException e) {
                            font = new Font(Font.SANS_SERIF, Font.PLAIN,
                                fontSize);
                        }
                        floatingText = new Text(30, 21, 2, "Heat from fire",
                            font, fontSize,
                            new java.awt.Color(0xF7, 0xA8, 0xB8));
                        addOverlay(floatingText);
                    } else {
                        floatingText.remove();
                        floatingText = null;
                    }
                }
            }
        );
        row += 2;

        addLabel(i18n.getString("textField1"), 1, row);
        TWidget field = addField(35, row, 15, false, "Field text");
        try {
            ClassLoader loader;
            loader = Thread.currentThread().getContextClassLoader();
            BufferedImage image;
            image = ImageIO.read(loader.
                getResource("demo/ibeam.png"));
            TApplication app = getApplication();
            field.setCustomMousePointer(new MousePointer(0, 0, 0,
                    image, 24, 24));
        } catch (Exception e) {
            new jexer.TExceptionDialog(getApplication(), e);
        }
        row += 2;

        // TODO: more things

        // Put some floating hearts on the screen.
        try {
            ClassLoader loader;
            loader = Thread.currentThread().getContextClassLoader();
            BufferedImage image;
            image = ImageIO.read(loader.getResource("demo/trans_icon.png"));
            Animation animation;
            animation = ImageUtils.getAnimation(loader.getResource(
                "demo/butterfly.gif"));
            addUnderlay(new Bitmap(17, 33, 0, animation, getApplication()));
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
                            if (item instanceof Text) {
                                continue;
                            }

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
        super.onClose();
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
