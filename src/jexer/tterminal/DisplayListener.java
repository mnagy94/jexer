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
package jexer.tterminal;

import java.util.List;

/**
 * DisplayListener is used to callback into external UI when data has come in
 * from the remote side.
 */
public interface DisplayListener {

    /**
     * Function to call when the display needs to be updated (request poll).
     *
     * @param cursorOnly if true, the screen has not changed but the cursor
     * may be on a different location.
     */
    public void displayChanged(final boolean cursorOnly);

    /**
     * Function to call when the display has updated (push).
     *
     * @param display the updated display
     */
    public void updateDisplay(final List<DisplayLine> display);

    /**
     * Function to call to obtain the number of rows from the bottom to
     * scroll back when sending updates via updateDisplay().
     *
     * @return the number of rows from the bottom to scroll back
     */
    public int getScrollBottom();

    /**
     * Function to call to obtain the display width.
     *
     * @return the number of columns in the display
     */
    public int getDisplayWidth();

    /**
     * Function to call to obtain the display height.
     *
     * @return the number of rows in the display
     */
    public int getDisplayHeight();

}
