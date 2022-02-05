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

/**
 * Make the window fade in.
 */
public class WindowFadeInEffect implements Effect {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The window to fade in.
     */
    private TWindow window;

    /**
     * The window's original alpha value we are ramping up to.
     */
    private int targetAlpha = 0;
    
    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param window the window to fade in
     */
    public WindowFadeInEffect(final TWindow window) {
        this.window = window;
        targetAlpha = window.getAlpha();
        window.setAlpha(64);
    }

    // ------------------------------------------------------------------------
    // Effect -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Update the effect.
     */
    public void update() {
        if (!window.isShown()) {
            return;
        }
        int alpha = window.getAlpha();
        if (alpha < targetAlpha) {
            // Aiming for 1/8 second, at 32 FPS = 4 frames.  256 / 4 = 64.
            alpha = Math.min(alpha + 64, targetAlpha);
            window.setAlpha(alpha);
        }
    }

    /**
     * If true, the effect is completed and can be removed.
     *
     * @return true if this effect is finished
     */
    public boolean isCompleted() {
        return (window.getAlpha() >= targetAlpha);
    }

}
