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
package jexer;

import jexer.bits.CellAttributes;

/**
 * TLabel implements a simple label.
 */
public final class TLabel extends TWidget {

    /**
     * Label text.
     */
    private String label = "";

    /**
     * Get label text.
     *
     * @return label text
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set label text.
     *
     * @param label new label text
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Label color.
     */
    private String colorKey;

    /**
     * Public constructor, using the default "tlabel" for colorKey.
     *
     * @param parent parent widget
     * @param text label on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     */
    public TLabel(final TWidget parent, final String text, final int x,
        final int y) {

        this(parent, text, x, y, "tlabel");
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text label on the screen
     * @param x column relative to parent
     * @param y row relative to parent
     * @param colorKey ColorTheme key color to use for foreground text
     */
    public TLabel(final TWidget parent, final String text, final int x,
        final int y, final String colorKey) {

        // Set parent and window
        super(parent, false, x, y, text.length(), 1);

        this.label = text;
        this.colorKey = colorKey;
    }

    /**
     * Draw a static label.
     */
    @Override
    public void draw() {
        // Setup my color
        CellAttributes color = new CellAttributes();
        color.setTo(getTheme().getColor(colorKey));
        CellAttributes background = getWindow().getBackground();
        color.setBackColor(background.getBackColor());

        getScreen().putStringXY(0, 0, label, color);
    }

}
