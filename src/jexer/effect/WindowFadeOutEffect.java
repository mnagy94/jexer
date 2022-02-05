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

import jexer.TWindow;
import jexer.backend.Screen;
import jexer.event.TInputEvent;

/**
 * A desktop or window effect does a blingy transformation before the screen
 * is sent to the device.
 */
public class WindowFadeOutEffect implements Effect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The fake window to fade out.
     */
    private TWindow fakeWindow;

    /**
     * The region of the screen the window last rendered to.
     */
    private Screen oldScreen;

    /**
     * The alpha value to set fakeWindow to.
     */
    private int alpha;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param window the window to fade in
     */
    public WindowFadeOutEffect(final TWindow window) {
        final Screen oldScreen = window.getScreen().snapshotPhysical(
            window.getX(), window.getY(),
            window.getWidth(), window.getHeight());

        alpha = window.getAlpha();

        final int x = window.getX();
        final int y = window.getY();

        window.getApplication().invokeLater(new Runnable() {
            public void run() {
                fakeWindow = new TWindow(window.getApplication(), "",
                    window.getX(), window.getY(),
                    window.getWidth(), window.getHeight(),
                    TWindow.MODAL) {

                    // Disable all inputs.
                    @Override
                    public void handleEvent(final TInputEvent event) {
                        // NOP
                    }

                    // Draw the old screen.
                    @Override
                    public void draw() {
                        for (int y = 0; y < getHeight(); y++) {
                            for (int x = 0; x < getWidth(); x++) {
                                putCharXY(x, y, oldScreen.getCharXY(x, y));
                            }
                        }
                    }

                    @Override
                    public boolean disableCloseEffect() {
                        return true;
                    }
                };
                fakeWindow.setX(x);
                fakeWindow.setY(y);
                fakeWindow.setAlpha(alpha);
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
        if (fakeWindow != null) {
            if (alpha == 0) {
                fakeWindow.close();
                return true;
            }
        }
        return false;
    }

}
