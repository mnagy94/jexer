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
package jexer.tackboard;

import java.util.ArrayList;
import java.util.List;

import jexer.backend.GlyphMaker;
import jexer.backend.Screen;
import jexer.bits.Cell;

/**
 * Tackboard maintains a collection of TackboardItems to draw on a Screen.
 *
 * <p>Each item has a set of X, Y, Z pixel (not text cell) coordinates.  The
 * coordinate system is right-handed: (0, 0, 0) is the top-left pixel on the
 * screen, and positive Z points away from the user.</p>
 *
 * <p>When draw() is called, all the items will be rendered in descending Z,
 * ascending Y, ascending X order (painter's algorithm) onto the cell grid,
 * and using transparent pixels.  If the Screen's backend does not support
 * imagesOverText, then the text of the Cell under transparent images will be
 * rendered via GlyphMaker, which might not look ideal if the internal font
 * is quite different from the terminal's.</p>
 *
 * <p>Tackboards were directly inspired by the Visuals (ncvisuals) of <a
 * href="https://github.com/dankamongmen/notcurses">notcurses</a>. Jexer's
 * performance is unlikely to come close to notcurses, so users requiring
 * low-latency pixel-based rendering are recommended to check out
 * notcurses.</p>
 */
public class Tackboard {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The items on this board.
     */
    private ArrayList<TackboardItem> items = new ArrayList<TackboardItem>();
    
    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     */
    public Tackboard() {
        // NOP
    }

    // ------------------------------------------------------------------------
    // Tackboard --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw everything to the screen.
     *
     * @param screen the screen to render to
     */
    public void draw(final Screen screen) {
        // TODO
    }

}
