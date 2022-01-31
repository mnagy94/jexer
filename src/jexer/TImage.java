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
package jexer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import jexer.bits.Animation;
import jexer.bits.Cell;
import jexer.bits.ImageUtils;
import jexer.event.TCommandEvent;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import static jexer.TCommand.*;
import static jexer.TKeypress.*;

/**
 * TImage renders a piece of a bitmap image or an animated image on screen.
 */
public class TImage extends TWidget implements EditMenuUser {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Selections for fitting the image to the text cells.
     */
    public enum Scale {
        /**
         * No scaling.
         */
        NONE,

        /**
         * Stretch/shrink the image in both directions to fully fill the text
         * area width/height.
         */
        STRETCH,

        /**
         * Scale the image, preserving aspect ratio, to fill the text area
         * width/height (like letterbox).  The background color for the
         * letterboxed area is specified in scaleBackColor.
         */
        SCALE,
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Scaling strategy to use.
     */
    private Scale scale = Scale.NONE;

    /**
     * Scaling strategy to use.
     */
    private java.awt.Color scaleBackColor = java.awt.Color.BLACK;

    /**
     * The action to perform when the user clicks on the image.
     */
    private TAction clickAction;

    /**
     * The image to display.
     */
    private BufferedImage image;

    /**
     * If false, the image is fully opaque.
     */
    private boolean maybeTransparent = true;

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
     * If true, this widget was resized and a new scaled image must be
     * produced.
     */
    private boolean resized = false;

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

    /**
     * Animation to display.
     */
    private Animation animation;

    /**
     * Anti-aliasing support.  It is normally off for performance.
     */
    private boolean antiAlias = false;

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
        final int width, final int height, final BufferedImage image,
        final int left, final int top) {

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
        final int width, final int height, final BufferedImage image,
        final int left, final int top, final TAction clickAction) {

        // Set parent and window
        super(parent, x, y, width, height);

        setCursorVisible(false);
        this.originalImage = image;
        this.left = left;
        this.top = top;
        this.clickAction = clickAction;

        sizeToImage(true);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width number of text cells for width of the image
     * @param height number of text cells for height of the image
     * @param animation the animation to display
     * @param left left column of the image.  0 is the left-most column.
     * @param top top row of the image.  0 is the top-most row.
     */
    public TImage(final TWidget parent, final int x, final int y,
        final int width, final int height, final Animation animation,
        final int left, final int top) {

        this(parent, x, y, width, height, animation, left, top, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width number of text cells for width of the image
     * @param height number of text cells for height of the image
     * @param animation the animation to display
     * @param left left column of the image.  0 is the left-most column.
     * @param top top row of the image.  0 is the top-most row.
     * @param clickAction function to call when mouse is pressed
     */
    public TImage(final TWidget parent, final int x, final int y,
        final int width, final int height, final Animation animation,
        final int left, final int top, final TAction clickAction) {

        // Set parent and window
        super(parent, x, y, width, height);

        setCursorVisible(false);
        animation.start(getApplication());
        this.animation = animation;
        this.originalImage = animation.getFrame();
        this.left = left;
        this.top = top;
        this.clickAction = clickAction;

        sizeToImage(true);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (clickAction != null) {
            clickAction.DO(this);
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

        if (keypress.equals(kbShiftLeft)) {
            switch (scale) {
            case NONE:
                setScaleType(Scale.SCALE);
                return;
            case STRETCH:
                setScaleType(Scale.NONE);
                return;
            case SCALE:
                setScaleType(Scale.STRETCH);
                return;
            }
        }
        if (keypress.equals(kbShiftRight)) {
            switch (scale) {
            case NONE:
                setScaleType(Scale.STRETCH);
                return;
            case STRETCH:
                setScaleType(Scale.SCALE);
                return;
            case SCALE:
                setScaleType(Scale.NONE);
                return;
            }
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    /**
     * Handle resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        // Get my width/height set correctly.
        super.onResize(event);

        if (scale == Scale.NONE) {
            return;
        }
        image = null;
        resized = true;
    }

    /**
     * Handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmCopy)) {
            // Copy image to clipboard.
            getClipboard().copyImage(image);
            return;
        }
    }

    /**
     * Stop the animation on close.
     */
    @Override
    public void close() {
        if (animation != null) {
            animation.stop();
        }
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the image.
     */
    @Override
    public void draw() {
        if (animation != null) {
            BufferedImage newFrame = animation.getFrame();
            if (newFrame != originalImage) {
                originalImage = newFrame;
                image = null;
                sizeToImage(true);
            } else {
                sizeToImage(false);
            }
        } else {
            sizeToImage(false);
        }

        // We have already broken the image up, just draw the previously
        // created set of cells.
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

                putCharXY(x, y, cells[x + left][y + top]);
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

        scaleBackColor = getApplication().getBackend().attrToBackgroundColor(getWindow().getBackground());

        int textWidth = getScreen().getTextWidth();
        int textHeight = getScreen().getTextHeight();

        if (image == null) {
            image = rotateImage(originalImage, clockwise);
            image = scaleImage(image, scaleFactor, getWidth(), getHeight(),
                textWidth, textHeight);
        }

        if ((always == true) ||
            (resized == true) ||
            ((textWidth > 0)
                && (textWidth != lastTextWidth)
                && (textHeight > 0)
                && (textHeight != lastTextHeight))
        ) {
            resized = false;

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

            int imageId = System.identityHashCode(this);
            imageId ^= (int) System.currentTimeMillis();
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
                    cell.setTo(getWindow().getBackground());

                    // Render over a full-cell-size image.
                    BufferedImage newImage = ImageUtils.createImage(image,
                        textWidth, textHeight);
                    Graphics gr = newImage.getGraphics();
                    BufferedImage subImage = image.getSubimage(x * textWidth,
                        y * textHeight, width, height);
                    gr.drawImage(subImage, 0, 0, null, null);
                    gr.dispose();

                    cell.setImage(newImage);
                    if (!maybeTransparent) {
                        cell.setOpaqueImage();
                    } else if (!ImageUtils.isFullyTransparent(newImage)) {
                        cell.flattenImage(false, getApplication().getBackend());
                    }
                    imageId++;
                    cell.setImageId(imageId & 0x7FFFFFFF);
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
     * Get anti-aliasing value.
     *
     * @return true if anti-aliasing is enabled
     */
    public boolean isAntiAlias() {
        return antiAlias;
    }

    /**
     * Set anti-aliasing.
     *
     * @param antiAlias if true, anti-aliasing will be enabled
     */
    public void setAntiAlias(final boolean antiAlias) {
        this.antiAlias = antiAlias;
        sizeToImage(true);
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
     * Get the raw (unprocessed) image.
     *
     * @return the image
     */
    public BufferedImage getImage() {
        return originalImage;
    }

    /**
     * Set the raw image, and reprocess to make the visible image.
     *
     * @param image the new image
     */
    public void setImage(final BufferedImage image) {
        setImage(image, true);
    }

    /**
     * Set the raw image, and reprocess to make the visible image.
     *
     * @param image the new image
     * @param maybeTransparent if false, the image is fully opaque
     */
    public void setImage(final BufferedImage image,
        final boolean maybeTransparent) {

        this.originalImage = image;
        this.maybeTransparent = maybeTransparent;
        this.image = null;
        sizeToImage(true);
        if (animation != null) {
            animation.stop();
            animation = null;
        }
    }

    /**
     * Set the image space to an animation, and reprocess to make the visible
     * image.
     *
     * @param animation the new animation
     */
    public void setAnimation(final Animation animation) {
        if (this.animation != null) {
            this.animation.stop();
            this.animation = null;
        }
        this.animation = animation;
        originalImage = animation.getFrame();
        image = null;
        sizeToImage(true);
    }

    /**
     * Get the visible (processed) image.
     *
     * @return the image that is currently on screen
     */
    public BufferedImage getVisibleImage() {
        return image;
    }

    /**
     * Get the scaling strategy.
     *
     * @return Scale.NONE, Scale.STRETCH, etc.
     */
    public Scale getScaleType() {
        return scale;
    }

    /**
     * Set the scaling strategy.
     *
     * @param scale Scale.NONE, Scale.STRETCH, etc.
     */
    public void setScaleType(final Scale scale) {
        this.scale = scale;
        this.image = null;
        sizeToImage(true);
    }

    /**
     * Get the scale factor.
     *
     * @return the scale factor
     */
    public double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Set the scale factor.  1.0 means no scaling.
     *
     * @param scaleFactor the new scale factor
     */
    public void setScaleFactor(final double scaleFactor) {
        this.scaleFactor = scaleFactor;
        image = null;
        sizeToImage(true);
    }

    /**
     * Get the rotation, as degrees.
     *
     * @return the rotation in degrees
     */
    public int getRotation() {
        switch (clockwise) {
        case 0:
            return 0;
        case 1:
            return 90;
        case 2:
            return 180;
        case 3:
            return 270;
        default:
            // Don't know how this happened, but fix it.
            clockwise = 0;
            image = null;
            sizeToImage(true);
            return 0;
        }
    }

    /**
     * Set the rotation, as degrees clockwise.
     *
     * @param rotation 0, 90, 180, or 270
     */
    public void setRotation(final int rotation) {
        switch (rotation) {
        case 0:
            clockwise = 0;
            break;
        case 90:
            clockwise = 1;
            break;
        case 180:
            clockwise = 2;
            break;
        case 270:
            clockwise = 3;
            break;
        default:
            // Don't know how this happened, but fix it.
            clockwise = 0;
            break;
        }

        image = null;
        sizeToImage(true);
    }

    /**
     * Scale an image to be scaleFactor size, OR stretch it.
     *
     * @param image the image to scale
     * @param factor the scale to make the new image
     * @param width the number of text cell columns for the destination image
     * @param height the number of text cell rows for the destination image
     * @param textWidth the width in pixels for one text cell
     * @param textHeight the height in pixels for one text cell
     */
    private BufferedImage scaleImage(final BufferedImage image,
        final double factor, final int width, final int height,
        final int textWidth, final int textHeight) {

        if ((scale == Scale.NONE) && (Math.abs(factor - 1.0) < 0.03)) {
            // If we are within 3% of 1.0, just return the original image.
            return image;
        }

        int destWidth = 0;
        int destHeight = 0;
        int x = 0;
        int y = 0;

        BufferedImage newImage = null;

        switch (scale) {
        case NONE:
            destWidth = (int) (image.getWidth() * factor);
            destHeight = (int) (image.getHeight() * factor);
            newImage = ImageUtils.createImage(image,
                Math.max(1, destWidth), Math.max(1, destHeight));
            break;
        case STRETCH:
            destWidth = Math.max(1, width) * textWidth;
            destHeight = Math.max(1, height) * textHeight;
            newImage = ImageUtils.createImage(image, destWidth, destHeight);
            break;
        case SCALE:
            double a = (double) image.getWidth() / image.getHeight();
            double b = (double) (width * textWidth) / (height * textHeight);
            assert (a > 0);
            assert (b > 0);

            /*
            System.err.println("Scale: original " + image.getWidth() +
                "x" + image.getHeight());
            System.err.println("         screen " + (width * textWidth) +
                "x" + (height * textHeight));
            System.err.println("A " + a + " B " + b);
             */

            if (a > b) {
                // Horizontal letterbox
                destWidth = Math.max(1, width) * textWidth;
                destHeight = (int) (destWidth / a);
                y = ((Math.max(1, height) * textHeight) - destHeight) / 2;
                assert (y >= 0);
                /*
                System.err.println("Horizontal letterbox: " + destWidth +
                    "x" + destHeight + ", Y offset " + y);
                 */
            } else {
                // Vertical letterbox
                destHeight = Math.max(1, height) * textHeight;
                destWidth = (int) (destHeight * a);
                x = ((Math.max(1, width) * textWidth) - destWidth) / 2;
                assert (x >= 0);
                /*
                System.err.println("Vertical letterbox: " + destWidth +
                    "x" + destHeight + ", X offset " + x);
                 */
            }
            newImage = ImageUtils.createImage(image,
                Math.max(1, width) * textWidth,
                Math.max(1, height) * textHeight);
            break;
        }

        Graphics gr = newImage.createGraphics();
        if (gr instanceof Graphics2D) {
            if (antiAlias) {
                ((Graphics2D) gr).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                ((Graphics2D) gr).setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            } else {
                ((Graphics2D) gr).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
                ((Graphics2D) gr).setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
            }
        }
        if (scale == Scale.SCALE) {
            gr.setColor(scaleBackColor);
            gr.fillRect(0, 0, width * textWidth, height * textHeight);
        }
        gr.drawImage(image, x, y, destWidth, destHeight, null);
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
            newImage = ImageUtils.createImage(image,
                image.getHeight(), image.getWidth());
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    newImage.setRGB(y, x,
                        image.getRGB(x, image.getHeight() - 1 - y));
                }
            }
        } else if (clockwise % 4 == 2) {
            // 180 degrees clockwise
            newImage = ImageUtils.createImage(image,
                image.getWidth(), image.getHeight());
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    newImage.setRGB(x, y,
                        image.getRGB(image.getWidth() - 1 - x,
                            image.getHeight() - 1 - y));
                }
            }
        } else if (clockwise % 4 == 3) {
            // 270 degrees clockwise
            newImage = ImageUtils.createImage(image,
                image.getHeight(), image.getWidth());
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    newImage.setRGB(y, x,
                        image.getRGB(image.getWidth() - 1 - x, y));
                }
            }
        }

        return newImage;
    }

    // ------------------------------------------------------------------------
    // EditMenuUser -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if the cut menu item should be enabled.
     *
     * @return true if the cut menu item should be enabled
     */
    public boolean isEditMenuCut() {
        return false;
    }

    /**
     * Check if the copy menu item should be enabled.
     *
     * @return true if the copy menu item should be enabled
     */
    public boolean isEditMenuCopy() {
        return true;
    }

    /**
     * Check if the paste menu item should be enabled.
     *
     * @return true if the paste menu item should be enabled
     */
    public boolean isEditMenuPaste() {
        return false;
    }

    /**
     * Check if the clear menu item should be enabled.
     *
     * @return true if the clear menu item should be enabled
     */
    public boolean isEditMenuClear() {
        return false;
    }

}
