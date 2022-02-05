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
package jexer.effect;

import java.awt.image.BufferedImage;

import jexer.TApplication;
import jexer.TWindow;
import jexer.backend.Screen;
import jexer.event.TInputEvent;
import jexer.tackboard.Bitmap;

/**
 * Make the window look like it was was burned in with plasma fire.
 */
public class WindowBurnInEffect implements Effect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The window to burn in.
     */
    private TWindow window;

    /**
     * The fake window with the plasma effect.
     */
    private TWindow fakeWindow;

    /**
     * The bitmap for the plasma effect.
     */
    private Bitmap plasma;

    /**
     * The burn alpha.
     */
    private int alpha = 0;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param window the window to burn in
     */
    public WindowBurnInEffect(final TWindow window) {
        this.window = window;

        alpha = 220;

        final int x = window.getX();
        final int y = window.getY();
        final TApplication app = window.getApplication();

        app.invokeLater(new Runnable() {
            public void run() {
                if (app.isModalThreadRunning()) {
                    return;
                }

                fakeWindow = new TWindow(window.getApplication(), "",
                    window.getX(), window.getY(),
                    window.getWidth(), window.getHeight(),
                    TWindow.MODAL) {

                    // Disable all inputs.
                    @Override
                    public void handleEvent(final TInputEvent event) {
                        // NOP
                    }

                    @Override
                    public void draw() {
                        // Draw nothing.  TWidget.drawChildren() will draw
                        // the overlay, which contains the plasma.
                    }

                    @Override
                    public boolean disableOpenEffect() {
                        return true;
                    }

                    @Override
                    public boolean disableCloseEffect() {
                        return true;
                    }
                };
                fakeWindow.setX(x);
                fakeWindow.setY(y);
                fakeWindow.setAlpha(alpha);

                // Generate the plasma.
                Screen screen = fakeWindow.getScreen();
                int width = fakeWindow.getWidth() * screen.getTextWidth();
                int height = fakeWindow.getHeight() * screen.getTextHeight();
                BufferedImage burn = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);

                // https://lodev.org/cgtutor/plasma.html has the general
                // idea.  I just played around and it's alright for a start.
                int w = width;
                int h = height;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int red = (int) (128.0 + 2 * (Math.sin((x + y) / 2.0) +
                                Math.sin(x * x + y)) * 128.0);
                        int green = (int) (128.0 + (Math.sin(y / 7.0) +
                                Math.cos(Math.log(y * y / 6))) * 128.0);
                        int blue = (int) (128.0 + Math.cos(Math.sqrt(x * x + y * y) / 3.0) * 128.0);
                        red = Math.max(0, Math.min(red, 255));
                        green = Math.max(0, Math.min(green, 255));
                        blue = Math.max(0, Math.min(blue, 255));
                        int rgb = ( 0xFF << 24)
                                    | (  red << 16)
                                    | (green <<  8)
                                    |  blue;
                        burn.setRGB(x, y, rgb);
                    }
                }

                plasma = new Bitmap(0, 0, 0, burn);

                fakeWindow.addOverlay(plasma);
            }
        });

    }

    // ------------------------------------------------------------------------
    // Effect -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Update the effect.
     */
    public void update() {
        if (fakeWindow == null) {
            return;
        }
        if (alpha > 0) {
            // Aiming for 1/8 second, at 32 FPS = 4 frames.  256 / 4 = 64.
            alpha = Math.max(alpha - 96, 0);
            fakeWindow.setAlpha(alpha);
        }
    }

    /**
     * If true, the effect is completed and can be removed.
     *
     * @return true if this effect is finished
     */
    public boolean isCompleted() {
        if (alpha == 0) {
            if (fakeWindow != null) {
                fakeWindow.close();
            }
            return true;
        }
        return false;
    }

}
