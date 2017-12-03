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
package jexer.backend;

import jexer.bits.Cell;
import jexer.bits.CellAttributes;
import jexer.bits.GraphicsChars;

/**
 * A logical screen composed of a 2D array of Cells.
 */
public class LogicalScreen implements Screen {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Width of the visible window.
     */
    protected int width;

    /**
     * Height of the visible window.
     */
    protected int height;

    /**
     * Drawing offset for x.
     */
    private int offsetX;

    /**
     * Drawing offset for y.
     */
    private int offsetY;

    /**
     * Ignore anything drawn right of clipRight.
     */
    private int clipRight;

    /**
     * Ignore anything drawn below clipBottom.
     */
    private int clipBottom;

    /**
     * Ignore anything drawn left of clipLeft.
     */
    private int clipLeft;

    /**
     * Ignore anything drawn above clipTop.
     */
    private int clipTop;

    /**
     * The physical screen last sent out on flush().
     */
    protected Cell [][] physical;

    /**
     * The logical screen being rendered to.
     */
    protected Cell [][] logical;

    /**
     * Set if the user explicitly wants to redraw everything starting with a
     * ECMATerminal.clearAll().
     */
    protected boolean reallyCleared;

    /**
     * If true, the cursor is visible and should be placed onscreen at
     * (cursorX, cursorY) during a call to flushPhysical().
     */
    protected boolean cursorVisible;

    /**
     * Cursor X position if visible.
     */
    protected int cursorX;

    /**
     * Cursor Y position if visible.
     */
    protected int cursorY;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  Sets everything to not-bold, white-on-black.
     */
    protected LogicalScreen() {
        offsetX  = 0;
        offsetY  = 0;
        width    = 80;
        height   = 24;
        logical  = null;
        physical = null;
        reallocate(width, height);
    }

    // ------------------------------------------------------------------------
    // Screen -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set drawing offset for x.
     *
     * @param offsetX new drawing offset
     */
    public final void setOffsetX(final int offsetX) {
        this.offsetX = offsetX;
    }

    /**
     * Set drawing offset for y.
     *
     * @param offsetY new drawing offset
     */
    public final void setOffsetY(final int offsetY) {
        this.offsetY = offsetY;
    }

    /**
     * Get right drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public final int getClipRight() {
        return clipRight;
    }

    /**
     * Set right drawing clipping boundary.
     *
     * @param clipRight new boundary
     */
    public final void setClipRight(final int clipRight) {
        this.clipRight = clipRight;
    }

    /**
     * Get bottom drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public final int getClipBottom() {
        return clipBottom;
    }

    /**
     * Set bottom drawing clipping boundary.
     *
     * @param clipBottom new boundary
     */
    public final void setClipBottom(final int clipBottom) {
        this.clipBottom = clipBottom;
    }

    /**
     * Get left drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public final int getClipLeft() {
        return clipLeft;
    }

    /**
     * Set left drawing clipping boundary.
     *
     * @param clipLeft new boundary
     */
    public final void setClipLeft(final int clipLeft) {
        this.clipLeft = clipLeft;
    }

    /**
     * Get top drawing clipping boundary.
     *
     * @return drawing boundary
     */
    public final int getClipTop() {
        return clipTop;
    }

    /**
     * Set top drawing clipping boundary.
     *
     * @param clipTop new boundary
     */
    public final void setClipTop(final int clipTop) {
        this.clipTop = clipTop;
    }

    /**
     * Get dirty flag.
     *
     * @return if true, the logical screen is not in sync with the physical
     * screen
     */
    public final boolean isDirty() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!logical[x][y].equals(physical[x][y])) {
                    return true;
                }
                if (logical[x][y].isBlink()) {
                    // Blinking screens are always dirty.  There is
                    // opportunity for a Netscape blink tag joke here...
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get the attributes at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return attributes at (x, y)
     */
    public final CellAttributes getAttrXY(final int x, final int y) {
        CellAttributes attr = new CellAttributes();
        if ((x >= 0) && (x < width) && (y >= 0) && (y < height)) {
            attr.setTo(logical[x][y]);
        }
        return attr;
    }

    /**
     * Get the cell at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return the character + attributes
     */
    public Cell getCharXY(final int x, final int y) {
        Cell cell = new Cell();
        if ((x >= 0) && (x < width) && (y >= 0) && (y < height)) {
            cell.setTo(logical[x][y]);
        }
        return cell;
    }

    /**
     * Set the attributes at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void putAttrXY(final int x, final int y,
        final CellAttributes attr) {

        putAttrXY(x, y, attr, true);
    }

    /**
     * Set the attributes at one location.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param attr attributes to use (bold, foreColor, backColor)
     * @param clip if true, honor clipping/offset
     */
    public final void putAttrXY(final int x, final int y,
        final CellAttributes attr, final boolean clip) {

        int X = x;
        int Y = y;

        if (clip) {
            if ((x < clipLeft)
                || (x >= clipRight)
                || (y < clipTop)
                || (y >= clipBottom)
            ) {
                return;
            }
            X += offsetX;
            Y += offsetY;
        }

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {
            logical[X][Y].setTo(attr);

            // If this happens to be the cursor position, make the position
            // dirty.
            if ((cursorX == X) && (cursorY == Y)) {
                if (physical[cursorX][cursorY].getChar() == 'Q') {
                    physical[cursorX][cursorY].setChar('X');
                } else {
                    physical[cursorX][cursorY].setChar('Q');
                }
            }
        }
    }

    /**
     * Fill the entire screen with one character with attributes.
     *
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void putAll(final char ch, final CellAttributes attr) {

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                putCharXY(x, y, ch, attr);
            }
        }
    }

    /**
     * Render one character with attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character + attributes to draw
     */
    public final void putCharXY(final int x, final int y, final Cell ch) {
        putCharXY(x, y, ch.getChar(), ch);
    }

    /**
     * Render one character with attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void putCharXY(final int x, final int y, final char ch,
        final CellAttributes attr) {

        if ((x < clipLeft)
            || (x >= clipRight)
            || (y < clipTop)
            || (y >= clipBottom)
        ) {
            return;
        }

        int X = x + offsetX;
        int Y = y + offsetY;

        // System.err.printf("putCharXY: %d, %d, %c\n", X, Y, ch);

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {

            // Do not put control characters on the display
            assert (ch >= 0x20);
            assert (ch != 0x7F);

            logical[X][Y].setTo(attr);
            logical[X][Y].setChar(ch);

            // If this happens to be the cursor position, make the position
            // dirty.
            if ((cursorX == X) && (cursorY == Y)) {
                if (physical[cursorX][cursorY].getChar() == 'Q') {
                    physical[cursorX][cursorY].setChar('X');
                } else {
                    physical[cursorX][cursorY].setChar('Q');
                }
            }
        }
    }

    /**
     * Render one character without changing the underlying attributes.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param ch character to draw
     */
    public final void putCharXY(final int x, final int y, final char ch) {

        if ((x < clipLeft)
            || (x >= clipRight)
            || (y < clipTop)
            || (y >= clipBottom)
        ) {
            return;
        }

        int X = x + offsetX;
        int Y = y + offsetY;

        // System.err.printf("putCharXY: %d, %d, %c\n", X, Y, ch);

        if ((X >= 0) && (X < width) && (Y >= 0) && (Y < height)) {
            logical[X][Y].setChar(ch);

            // If this happens to be the cursor position, make the position
            // dirty.
            if ((cursorX == X) && (cursorY == Y)) {
                if (physical[cursorX][cursorY].getChar() == 'Q') {
                    physical[cursorX][cursorY].setChar('X');
                } else {
                    physical[cursorX][cursorY].setChar('Q');
                }
            }
        }
    }

    /**
     * Render a string.  Does not wrap if the string exceeds the line.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param str string to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void putStringXY(final int x, final int y, final String str,
        final CellAttributes attr) {

        int i = x;
        for (int j = 0; j < str.length(); j++) {
            char ch = str.charAt(j);
            putCharXY(i, y, ch, attr);
            i++;
            if (i == width) {
                break;
            }
        }
    }

    /**
     * Render a string without changing the underlying attribute.  Does not
     * wrap if the string exceeds the line.
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param str string to draw
     */
    public final void putStringXY(final int x, final int y, final String str) {

        int i = x;
        for (int j = 0; j < str.length(); j++) {
            char ch = str.charAt(j);
            putCharXY(i, y, ch);
            i++;
            if (i == width) {
                break;
            }
        }
    }

    /**
     * Draw a vertical line from (x, y) to (x, y + n).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void vLineXY(final int x, final int y, final int n,
        final char ch, final CellAttributes attr) {

        for (int i = y; i < y + n; i++) {
            putCharXY(x, i, ch, attr);
        }
    }

    /**
     * Draw a horizontal line from (x, y) to (x + n, y).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @param n number of characters to draw
     * @param ch character to draw
     * @param attr attributes to use (bold, foreColor, backColor)
     */
    public final void hLineXY(final int x, final int y, final int n,
        final char ch, final CellAttributes attr) {

        for (int i = x; i < x + n; i++) {
            putCharXY(i, y, ch, attr);
        }
    }

    /**
     * Change the width.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param width new screen width
     */
    public final synchronized void setWidth(final int width) {
        reallocate(width, this.height);
    }

    /**
     * Change the height.  Everything on-screen will be destroyed and must be
     * redrawn.
     *
     * @param height new screen height
     */
    public final synchronized void setHeight(final int height) {
        reallocate(this.width, height);
    }

    /**
     * Change the width and height.  Everything on-screen will be destroyed
     * and must be redrawn.
     *
     * @param width new screen width
     * @param height new screen height
     */
    public final void setDimensions(final int width, final int height) {
        reallocate(width, height);
    }

    /**
     * Get the height.
     *
     * @return current screen height
     */
    public final synchronized int getHeight() {
        return this.height;
    }

    /**
     * Get the width.
     *
     * @return current screen width
     */
    public final synchronized int getWidth() {
        return this.width;
    }

    /**
     * Reset screen to not-bold, white-on-black.  Also flushes the offset and
     * clip variables.
     */
    public final synchronized void reset() {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                logical[col][row].reset();
            }
        }
        resetClipping();
    }

    /**
     * Flush the offset and clip variables.
     */
    public final void resetClipping() {
        offsetX    = 0;
        offsetY    = 0;
        clipLeft   = 0;
        clipTop    = 0;
        clipRight  = width;
        clipBottom = height;
    }

    /**
     * Clear the logical screen.
     */
    public final void clear() {
        reset();
    }

    /**
     * Draw a box with a border and empty background.
     *
     * @param left left column of box.  0 is the left-most row.
     * @param top top row of the box.  0 is the top-most row.
     * @param right right column of box
     * @param bottom bottom row of the box
     * @param border attributes to use for the border
     * @param background attributes to use for the background
     */
    public final void drawBox(final int left, final int top,
        final int right, final int bottom,
        final CellAttributes border, final CellAttributes background) {

        drawBox(left, top, right, bottom, border, background, 1, false);
    }

    /**
     * Draw a box with a border and empty background.
     *
     * @param left left column of box.  0 is the left-most row.
     * @param top top row of the box.  0 is the top-most row.
     * @param right right column of box
     * @param bottom bottom row of the box
     * @param border attributes to use for the border
     * @param background attributes to use for the background
     * @param borderType if 1, draw a single-line border; if 2, draw a
     * double-line border; if 3, draw double-line top/bottom edges and
     * single-line left/right edges (like Qmodem)
     * @param shadow if true, draw a "shadow" on the box
     */
    public final void drawBox(final int left, final int top,
        final int right, final int bottom,
        final CellAttributes border, final CellAttributes background,
        final int borderType, final boolean shadow) {

        int boxWidth = right - left;
        int boxHeight = bottom - top;

        char cTopLeft;
        char cTopRight;
        char cBottomLeft;
        char cBottomRight;
        char cHSide;
        char cVSide;

        switch (borderType) {
        case 1:
            cTopLeft = GraphicsChars.ULCORNER;
            cTopRight = GraphicsChars.URCORNER;
            cBottomLeft = GraphicsChars.LLCORNER;
            cBottomRight = GraphicsChars.LRCORNER;
            cHSide = GraphicsChars.SINGLE_BAR;
            cVSide = GraphicsChars.WINDOW_SIDE;
            break;

        case 2:
            cTopLeft = GraphicsChars.WINDOW_LEFT_TOP_DOUBLE;
            cTopRight = GraphicsChars.WINDOW_RIGHT_TOP_DOUBLE;
            cBottomLeft = GraphicsChars.WINDOW_LEFT_BOTTOM_DOUBLE;
            cBottomRight = GraphicsChars.WINDOW_RIGHT_BOTTOM_DOUBLE;
            cHSide = GraphicsChars.DOUBLE_BAR;
            cVSide = GraphicsChars.WINDOW_SIDE_DOUBLE;
            break;

        case 3:
            cTopLeft = GraphicsChars.WINDOW_LEFT_TOP;
            cTopRight = GraphicsChars.WINDOW_RIGHT_TOP;
            cBottomLeft = GraphicsChars.WINDOW_LEFT_BOTTOM;
            cBottomRight = GraphicsChars.WINDOW_RIGHT_BOTTOM;
            cHSide = GraphicsChars.WINDOW_TOP;
            cVSide = GraphicsChars.WINDOW_SIDE;
            break;
        default:
            throw new IllegalArgumentException("Invalid border type: "
                + borderType);
        }

        // Place the corner characters
        putCharXY(left, top, cTopLeft, border);
        putCharXY(left + boxWidth - 1, top, cTopRight, border);
        putCharXY(left, top + boxHeight - 1, cBottomLeft, border);
        putCharXY(left + boxWidth - 1, top + boxHeight - 1, cBottomRight,
            border);

        // Draw the box lines
        hLineXY(left + 1, top, boxWidth - 2, cHSide, border);
        vLineXY(left, top + 1, boxHeight - 2, cVSide, border);
        hLineXY(left + 1, top + boxHeight - 1, boxWidth - 2, cHSide, border);
        vLineXY(left + boxWidth - 1, top + 1, boxHeight - 2, cVSide, border);

        // Fill in the interior background
        for (int i = 1; i < boxHeight - 1; i++) {
            hLineXY(1 + left, i + top, boxWidth - 2, ' ', background);
        }

        if (shadow) {
            // Draw a shadow
            drawBoxShadow(left, top, right, bottom);
        }
    }

    /**
     * Draw a box shadow.
     *
     * @param left left column of box.  0 is the left-most row.
     * @param top top row of the box.  0 is the top-most row.
     * @param right right column of box
     * @param bottom bottom row of the box
     */
    public final void drawBoxShadow(final int left, final int top,
        final int right, final int bottom) {

        int boxTop = top;
        int boxLeft = left;
        int boxWidth = right - left;
        int boxHeight = bottom - top;
        CellAttributes shadowAttr = new CellAttributes();

        // Shadows do not honor clipping but they DO honor offset.
        int oldClipRight = clipRight;
        int oldClipBottom = clipBottom;
        /*
        clipRight = boxWidth + 2;
        clipBottom = boxHeight + 1;
        */
        clipRight = width;
        clipBottom = height;

        for (int i = 0; i < boxHeight; i++) {
            putAttrXY(boxLeft + boxWidth, boxTop + 1 + i, shadowAttr);
            putAttrXY(boxLeft + boxWidth + 1, boxTop + 1 + i, shadowAttr);
        }
        for (int i = 0; i < boxWidth; i++) {
            putAttrXY(boxLeft + 2 + i, boxTop + boxHeight, shadowAttr);
        }
        clipRight = oldClipRight;
        clipBottom = oldClipBottom;
    }

    /**
     * Default implementation does nothing.
     */
    public void flushPhysical() {}

    /**
     * Put the cursor at (x,y).
     *
     * @param visible if true, the cursor should be visible
     * @param x column coordinate to put the cursor on
     * @param y row coordinate to put the cursor on
     */
    public void putCursor(final boolean visible, final int x, final int y) {
        if ((cursorY >= 0)
            && (cursorX >= 0)
            && (cursorY <= height - 1)
            && (cursorX <= width - 1)
        ) {
            // Make the current cursor position dirty
            if (physical[cursorX][cursorY].getChar() == 'Q') {
                physical[cursorX][cursorY].setChar('X');
            } else {
                physical[cursorX][cursorY].setChar('Q');
            }
        }

        cursorVisible = visible;
        cursorX = x;
        cursorY = y;
    }

    /**
     * Hide the cursor.
     */
    public final void hideCursor() {
        cursorVisible = false;
    }

    /**
     * Get the cursor visibility.
     *
     * @return true if the cursor is visible
     */
    public boolean isCursorVisible() {
        return cursorVisible;
    }

    /**
     * Get the cursor X position.
     *
     * @return the cursor x column position
     */
    public int getCursorX() {
        return cursorX;
    }

    /**
     * Get the cursor Y position.
     *
     * @return the cursor y row position
     */
    public int getCursorY() {
        return cursorY;
    }

    /**
     * Set the window title.  Default implementation does nothing.
     *
     * @param title the new title
     */
    public void setTitle(final String title) {}

    // ------------------------------------------------------------------------
    // LogicalScreen ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Reallocate screen buffers.
     *
     * @param width new width
     * @param height new height
     */
    private synchronized void reallocate(final int width, final int height) {
        if (logical != null) {
            for (int row = 0; row < this.height; row++) {
                for (int col = 0; col < this.width; col++) {
                    logical[col][row] = null;
                }
            }
            logical = null;
        }
        logical = new Cell[width][height];
        if (physical != null) {
            for (int row = 0; row < this.height; row++) {
                for (int col = 0; col < this.width; col++) {
                    physical[col][row] = null;
                }
            }
            physical = null;
        }
        physical = new Cell[width][height];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                physical[col][row] = new Cell();
                logical[col][row] = new Cell();
            }
        }

        this.width = width;
        this.height = height;

        clipLeft = 0;
        clipTop = 0;
        clipRight = width;
        clipBottom = height;

        reallyCleared = true;
    }

    /**
     * Clear the physical screen.
     */
    public final void clearPhysical() {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                physical[col][row].reset();
            }
        }
    }

}
