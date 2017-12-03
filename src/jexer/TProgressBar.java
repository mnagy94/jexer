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
import jexer.bits.GraphicsChars;

/**
 * TProgressBar implements a simple progress bar.
 */
public final class TProgressBar extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Value that corresponds to 0% progress.
     */
    private int minValue = 0;

    /**
     * Value that corresponds to 100% progress.
     */
    private int maxValue = 100;

    /**
     * Current value of the progress.
     */
    private int value = 0;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of progress bar
     * @param value initial value of percent complete
     */
    public TProgressBar(final TWidget parent, final int x, final int y,
        final int width, final int value) {

        // Set parent and window
        super(parent, false, x, y, width, 1);

        this.value = value;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw a static progress bar.
     */
    @Override
    public void draw() {
        CellAttributes completeColor = getTheme().getColor("tprogressbar.complete");
        CellAttributes incompleteColor = getTheme().getColor("tprogressbar.incomplete");

        float progress = ((float)value - minValue) / ((float)maxValue - minValue);
        int progressInt = (int)(progress * 100);
        int progressUnit = 100 / (getWidth() - 2);

        getScreen().putCharXY(0, 0, GraphicsChars.CP437[0xC3], incompleteColor);
        for (int i = 0; i < getWidth() - 2; i++) {
            float iProgress = (float)i / (getWidth() - 2);
            int iProgressInt = (int)(iProgress * 100);
            if (iProgressInt <= progressInt - progressUnit) {
                getScreen().putCharXY(i + 1, 0, GraphicsChars.BOX,
                    completeColor);
            } else {
                getScreen().putCharXY(i + 1, 0, GraphicsChars.SINGLE_BAR,
                    incompleteColor);
            }
        }
        if (value >= maxValue) {
            getScreen().putCharXY(getWidth() - 2, 0, GraphicsChars.BOX,
                completeColor);
        } else {
            getScreen().putCharXY(getWidth() - 2, 0, GraphicsChars.SINGLE_BAR,
                incompleteColor);
        }
        getScreen().putCharXY(getWidth() - 1, 0, GraphicsChars.CP437[0xB4],
            incompleteColor);
    }

    // ------------------------------------------------------------------------
    // TProgressBar -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the value that corresponds to 0% progress.
     *
     * @return the value that corresponds to 0% progress
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * Set the value that corresponds to 0% progress.
     *
     * @param minValue the value that corresponds to 0% progress
     */
    public void setMinValue(final int minValue) {
        this.minValue = minValue;
    }

    /**
     * Get the value that corresponds to 100% progress.
     *
     * @return the value that corresponds to 100% progress
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * Set the value that corresponds to 100% progress.
     *
     * @param maxValue the value that corresponds to 100% progress
     */
    public void setMaxValue(final int maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Get the current value of the progress.
     *
     * @return the current value of the progress
     */
    public int getValue() {
        return value;
    }

    /**
     * Set the current value of the progress.
     *
     * @param value the current value of the progress
     */
    public void setValue(final int value) {
        this.value = value;
    }

}
