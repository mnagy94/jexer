/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2019 Kevin Lamonte
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

import java.awt.image.BufferedImage;

import jexer.backend.ECMA48Terminal;
import jexer.backend.MultiScreen;
import jexer.backend.SwingTerminal;
import jexer.bits.Cell;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import static jexer.TKeypress.*;

/**
 * TImage renders a piece of a bitmap image on screen.
 */
public class TImage extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The action to perform when the user clicks on the image.
     */
    private TAction clickAction;

    /**
     * The image to display.
     */
    private BufferedImage image;

    /**
     * The original image from construction time.
     */
    private BufferedImage originalImage;

    /**
     * The current scaling factor for the image.
     */
    private double scaleFactor = 1.0;

    /**
     * The current clockwise rotation for the image.
     */
    private int clockwise = 0;

    /**
     * Left column of the image.  0 is the left-most column.
     */
    private int left;

    /**
     * Top row of the image.  0 is the top-most row.
     */
    private int top;

    /**
     * The cells containing the broken up image pieces.
     */
    private Cell cells[][];

    /**
     * The number of rows in cells[].
     */
    private int cellRows;

    /**
     * The number of columns in cells[].
     */
    private int cellColumns;

    /**
     * Last text width value.
     */
    private int lastTextWidth = -1;

    /**
     * Last text height value.
     */
    private int lastTextHeight = -1;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width number of text cells for width of the image
     * @param height number of text cells for height of the image
     * @param image the image to display
     * @param left left column of the image.  0 is the left-most column.
     * @param top top row of the image.  0 is the top-most row.
     */
    public TImage(final TWidget parent, final int x, final int y,
        final int width, final int height,
        final BufferedImage image, final int left, final int top) {

        this(parent, x, y, width, height, image, left, top, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width number of text cells for width of the image
     * @param height number of text cells for height of the image
     * @param image the image to display
     * @param left left column of the image.  0 is the left-most column.
     * @param top top row of the image.  0 is the top-most row.
     * @param clickAction function to call when mouse is pressed
     */
    public TImage(final TWidget parent, final int x, final int y,
        final int width, final int height,
        final BufferedImage image, final int left, final int top,
        final TAction clickAction) {

        // Set parent and window
        super(parent, x, y, width, height);

        setCursorVisible(false);
        this.originalImage = image;
        this.left = left;
        this.top = top;
        this.clickAction = clickAction;

        sizeToImage(true);

        getApplication().addImage(this);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Subclasses should override this method to cleanup resources.  This is
     * called by TWindow.onClose().
     */
    @Override
    protected void close() {
        getApplication().removeImage(this);
        super.close();
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (clickAction != null) {
            clickAction.DO();
            return;
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (!keypress.getKey().isFnKey()) {
            if (keypress.getKey().getChar() == '+') {
                // Make the image bigger.
                scaleFactor *= 1.25;
                image = null;
                sizeToImage(true);
                return;
            }
            if (keypress.getKey().getChar() == '-') {
                // Make the image smaller.
                scaleFactor *= 0.80;
                image = null;
                sizeToImage(true);
                return;
            }
        }
        if (keypress.equals(kbAltUp)) {
            // Make the image bigger.
            scaleFactor *= 1.25;
            image = null;
            sizeToImage(true);
            return;
        }
        if (keypress.equals(kbAltDown)) {
            // Make the image smaller.
            scaleFactor *= 0.80;
            image = null;
            sizeToImage(true);
            return;
        }
        if (keypress.equals(kbAltRight)) {
            // Rotate clockwise.
            clockwise++;
            clockwise %= 4;
            image = null;
            sizeToImage(true);
            return;
        }
        if (keypress.equals(kbAltLeft)) {
            // Rotate counter-clockwise.
            clockwise--;
            if (clockwise < 0) {
                clockwise = 3;
            }
            image = null;
            sizeToImage(true);
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the image.
     */
    @Override
    public void draw() {
        sizeToImage(false);

        // We have already broken the image up, just draw the last set of
        // cells.
        for (int x = 0; (x < getWidth()) && (x + left < cellColumns); x++) {
            if ((left + x) * lastTextWidth > image.getWidth()) {
                continue;
            }

            for (int y = 0; (y < getHeight()) && (y + top < cellRows); y++) {
                if ((top + y) * lastTextHeight > image.getHeight()) {
                    continue;
                }
                assert (x + left < cellColumns);
                assert (y + top < cellRows);

                getWindow().putCharXY(x, y, cells[x + left][y + top]);
            }
        }

    }

    // ------------------------------------------------------------------------
    // TImage -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Size cells[][] according to the screen font size.
     *
     * @param always if true, always resize the cells
     */
    private void sizeToImage(final boolean always) {
        int textWidth = 16;
        int textHeight = 20;

        if (getScreen() instanceof SwingTerminal) {
            SwingTerminal terminal = (SwingTerminal) getScreen();

            textWidth = terminal.getTextWidth();
            textHeight = terminal.getTextHeight();
        } if (getScreen() instanceof MultiScreen) {
            MultiScreen terminal = (MultiScreen) getScreen();

            textWidth = terminal.getTextWidth();
            textHeight = terminal.getTextHeight();
        } else if (getScreen() instanceof ECMA48Terminal) {
            ECMA48Terminal terminal = (ECMA48Terminal) getScreen();

            textWidth = terminal.getTextWidth();
            textHeight = terminal.getTextHeight();
        }

        if (image == null) {
            image = scaleImage(originalImage, scaleFactor);
            image = rotateImage(image, clockwise);
        }

        if ((always == true) ||
            ((textWidth > 0)
                && (textWidth != lastTextWidth)
                && (textHeight > 0)
                && (textHeight != lastTextHeight))
        ) {
            cellColumns = image.getWidth() / textWidth;
            if (cellColumns * textWidth < image.getWidth()) {
                cellColumns++;
            }
            cellRows = image.getHeight() / textHeight;
            if (cellRows * textHeight < image.getHeight()) {
                cellRows++;
            }

            // Break the image up into an array of cells.
            cells = new Cell[cellColumns][cellRows];

            for (int x = 0; x < cellColumns; x++) {
                for (int y = 0; y < cellRows; y++) {

                    int width = textWidth;
                    if ((x + 1) * textWidth > image.getWidth()) {
                        width = image.getWidth() - (x * textWidth);
                    }
                    int height = textHeight;
                    if ((y + 1) * textHeight > image.getHeight()) {
                        height = image.getHeight() - (y * textHeight);
                    }

                    Cell cell = new Cell();
                    cell.setImage(image.getSubimage(x * textWidth,
                            y * textHeight, width, height));

                    cells[x][y] = cell;
                }
            }

            lastTextWidth = textWidth;
            lastTextHeight = textHeight;
        }

        if ((left + getWidth()) > cellColumns) {
            left = cellColumns - getWidth();
        }
        if (left < 0) {
            left = 0;
        }
        if ((top + getHeight()) > cellRows) {
            top = cellRows - getHeight();
        }
        if (top < 0) {
            top = 0;
        }
    }

    /**
     * Get the top corner to render.
     *
     * @return the top row
     */
    public int getTop() {
        return top;
    }

    /**
     * Set the top corner to render.
     *
     * @param top the new top row
     */
    public void setTop(final int top) {
        this.top = top;
        if (this.top > cellRows - getHeight()) {
            this.top = cellRows - getHeight();
        }
        if (this.top < 0) {
            this.top = 0;
        }
    }

    /**
     * Get the left corner to render.
     *
     * @return the left column
     */
    public int getLeft() {
        return left;
    }

    /**
     * Set the left corner to render.
     *
     * @param left the new left column
     */
    public void setLeft(final int left) {
        this.left = left;
        if (this.left > cellColumns - getWidth()) {
            this.left = cellColumns - getWidth();
        }
        if (this.left < 0) {
            this.left = 0;
        }
    }

    /**
     * Get the number of text cell rows for this image.
     *
     * @return the number of rows
     */
    public int getRows() {
        return cellRows;
    }

    /**
     * Get the number of text cell columns for this image.
     *
     * @return the number of columns
     */
    public int getColumns() {
        return cellColumns;
    }

    /**
     * Scale an image by to be scaleFactor size.
     *
     * @param image the image to scale
     * @param factor the scale to make the new image
     */
    private BufferedImage scaleImage(final BufferedImage image,
        final double factor) {

        if (Math.abs(factor - 1.0) < 0.03) {
            // If we are within 3% of 1.0, just return the original image.
            return image;
        }

        int width = (int) (image.getWidth() * factor);
        int height = (int) (image.getHeight() * factor);

        BufferedImage newImage = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);

        java.awt.Graphics gr = newImage.createGraphics();
        gr.drawImage(image, 0, 0, width, height, null);
        gr.dispose();

        return newImage;
    }

    /**
     * Rotate an image either clockwise or counterclockwise.
     *
     * @param image the image to scale
     * @param clockwise number of turns clockwise
     */
    private BufferedImage rotateImage(final BufferedImage image,
        final int clockwise) {

        if (clockwise % 4 == 0) {
            return image;
        }

        BufferedImage newImage = null;

        if (clockwise % 4 == 1) {
            // 90 degrees clockwise
            newImage = new BufferedImage(image.getHeight(), image.getWidth(),
                BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    newImage.setRGB(y, x,
                        image.getRGB(x, image.getHeight() - 1 - y));
                }
            }
        } else if (clockwise % 4 == 2) {
            // 180 degrees clockwise
            newImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    newImage.setRGB(x, y,
                        image.getRGB(image.getWidth() - 1 - x,
                            image.getHeight() - 1 - y));
                }
            }
        } else if (clockwise % 4 == 3) {
            // 270 degrees clockwise
            newImage = new BufferedImage(image.getHeight(), image.getWidth(),
                BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    newImage.setRGB(y, x,
                        image.getRGB(image.getWidth() - 1 - x, y));
                }
            }
        }

        return newImage;
    }

}
