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
package jexer.backend;

import java.util.ArrayList;
import java.util.List;

import jexer.bits.BorderStyle;
import jexer.bits.Cell;
import jexer.bits.CellAttributes;
import jexer.bits.Clipboard;

/**
 * MultiScreen mirrors its I/O to several screens.
 */
public class MultiScreen extends LogicalScreen implements Screen {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The list of screens to use.
     */
    private List<Screen> screens = new ArrayList<Screen>();

    /**
     * The text cell width in pixels to report.
     */
    private int textWidth = 10;

    /**
     * The text cell height in pixels to report.
     */
    private int textHeight = 20;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor provides a virtual screen at 80x25.
     */
    public MultiScreen() {
        super(80, 25);
    }

    /**
     * Public constructor takes the dimensions of the first screen.
     *
     * @param screen the screen to add
     */
    public MultiScreen(final Screen screen) {
        super(screen.getWidth(), screen.getHeight());
        synchronized (screens) {
            screens.add(screen);
            this.textWidth = screen.getTextWidth();
            this.textHeight = screen.getTextHeight();
        }
    }

    // ------------------------------------------------------------------------
    // LogicalScreen ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the width of a character cell in pixels.
     *
     * @return the width in pixels of a character cell
     */
    @Override
    public int getTextWidth() {
        return textWidth;
    }

    /**
     * Get the height of a character cell in pixels.
     *
     * @return the height in pixels of a character cell
     */
    @Override
    public int getTextHeight() {
        return textHeight;
    }

    /**
     * Change the width.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param width new screen width
     */
    @Override
    public void setWidth(final int width) {
        super.setWidth(width);
        synchronized (screens) {
            for (Screen screen: screens) {
                screen.setWidth(width);
            }
        }
    }

    /**
     * Change the height.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param height new screen height
     */
    @Override
    public void setHeight(final int height) {
        super.setHeight(height);
        synchronized (screens) {
            for (Screen screen: screens) {
                screen.setHeight(height);
            }
        }
    }

    /**
     * Change the width and height.  Everything on-screen will be destroyed
     * and must be redrawn.
     *
     * @param width new screen width
     * @param height new screen height
     */
    @Override
    public void setDimensions(final int width, final int height) {
        super.setDimensions(width, height);
        synchronized (screens) {
            for (Screen screen: screens) {
                // Do not blindly call setDimension() on every screen.
                // Instead call it only on those screens that do not already
                // have the requested dimension.  With this very small check,
                // we have the ability for ANY screen in the MultiBackend to
                // resize ALL of the screens.
                if ((screen.getWidth() != width)
                    || (screen.getHeight() != height)
                ) {
                    screen.setDimensions(width, height);
                } else {
                    // The screen that didn't change is probably the one that
                    // prompted the resize.  Force it to repaint.
                    screen.clearPhysical();
                }
            }
        }
    }

    /**
     * Clear the physical screen.
     */
    @Override
    public void clearPhysical() {
        super.clearPhysical();
        synchronized (screens) {
            for (Screen screen: screens) {
                screen.clearPhysical();
            }
        }
    }

    /**
     * Classes must provide an implementation to push the logical screen to
     * the physical device.
     */
    @Override
    public void flushPhysical() {
        List<Screen> screensToFlush = new ArrayList<Screen>();
        synchronized (screens) {
            screensToFlush.addAll(screens);
        }
        for (Screen screen: screensToFlush) {
            synchronized (screen) {
                screen.copyScreen(this);
            }
        }
    }

    /**
     * Set the window title.
     *
     * @param title the new title
     */
    @Override
    public void setTitle(final String title) {
        super.setTitle(title);
        synchronized (screens) {
            for (Screen screen: screens) {
                screen.setTitle(title);
            }
        }
    }

    // ------------------------------------------------------------------------
    // MultiScreen ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Add a screen to the list.
     *
     * @param screen the screen to add
     */
    public void addScreen(final Screen screen) {
        synchronized (screens) {
            screens.add(screen);
        }
        textWidth = Math.min(textWidth, screen.getTextWidth());
        textHeight = Math.min(textHeight, screen.getTextHeight());
    }

    /**
     * Remove a screen from the list.
     *
     * @param screenToRemove the screen to remove
     */
    public void removeScreen(final Screen screenToRemove) {
        synchronized (screens) {
            if (screens.size() > 1) {
                screens.remove(screenToRemove);
            }
            for (Screen screen: screens) {
                textWidth = Math.min(textWidth, screen.getTextWidth());
                textHeight = Math.min(textHeight, screen.getTextHeight());
            }
        }
    }

}
