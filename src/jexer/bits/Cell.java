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
package jexer.bits;

import java.awt.image.BufferedImage;
import jexer.backend.Backend;
import jexer.backend.GlyphMaker;

/**
 * This class represents a single text cell or bit of image on the screen.
 */
public class Cell extends CellAttributes {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * How this cell needs to be displayed if it is part of a larger glyph.
     */
    public enum Width {
        /**
         * This cell is an entire glyph on its own.
         */
        SINGLE,

        /**
         * This cell is the left half of a wide glyph.
         */
        LEFT,

        /**
         * This cell is the right half of a wide glyph.
         */
        RIGHT,
    }

    /**
     * The special "this cell is unset" (null) value.  This is the Unicode
     * "not a character" value.
     */
    private static final char UNSET_VALUE = (char) 65535;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The character at this cell.
     */
    private int ch = ' ';

    /**
     * The display width of this cell.
     */
    private Width width = Width.SINGLE;

    /**
     * The image at this cell.
     */
    private BufferedImage image = null;

    /**
     * The image at this cell, inverted.
     */
    private BufferedImage invertedImage = null;

    /**
     * The background color used for the area the image portion might not
     * cover.
     */
    private java.awt.Color background = java.awt.Color.BLACK;

    /**
     * hashCode() needs to call image.hashCode(), which can get quite
     * expensive.
     */
    private int imageHashCode = 0;

    /**
     * hashCode() needs to call background.hashCode(), which can get quite
     * expensive.
     */
    private int backgroundHashCode = 0;

    /**
     * If this cell has image data, whether or not it also has transparent
     * pixels.  -1 = no image data; 0 = unknown if transparent pixels are
     * present; 1 = transparent pixels are present; 2 = transparent pixels
     * are not present; 3 = the entire image is transparent; 4 = transparent
     * pixels are present, but not all of the image.
     */
    private int hasTransparentPixels = -1;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor sets default values of the cell to blank.
     *
     * @see #isBlank()
     * @see #reset()
     */
    public Cell() {
        // NOP
    }

    /**
     * Public constructor sets the character.  Attributes are the same as
     * default.
     *
     * @param ch character to set to
     * @see #reset()
     */
    public Cell(final int ch) {
        this.ch = ch;
    }

    /**
     * Public constructor sets the attributes.
     *
     * @param attr attributes to use
     */
    public Cell(final CellAttributes attr) {
        super(attr);
    }

    /**
     * Public constructor sets the character and attributes.
     *
     * @param ch character to set to
     * @param attr attributes to use
     */
    public Cell(final int ch, final CellAttributes attr) {
        super(attr);
        this.ch = ch;
    }

    /**
     * Public constructor creates a duplicate.
     *
     * @param cell the instance to copy
     */
    public Cell(final Cell cell) {
        setTo(cell);
    }

    // ------------------------------------------------------------------------
    // Cell -------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the image data for this cell.
     *
     * @param image the image for this cell
     */
    public void setImage(final BufferedImage image) {
        this.image = image;
        hasTransparentPixels = 0;
        imageHashCode = image.hashCode();
        width = Width.SINGLE;
    }

    /**
     * Get the image data for this cell.
     *
     * @return the image for this cell
     */
    public BufferedImage getImage() {
        if (invertedImage != null) {
            return invertedImage;
        }
        return image;
    }

    /**
     * Get the image data for this cell.
     *
     * @param copy if true, return a copy of the image
     * @return the image for this cell
     */
    public BufferedImage getImage(final boolean copy) {
        if (!copy) {
            return getImage();
        }
        if (image == null) {
            return null;
        }

        int textWidth = image.getWidth();
        int textHeight = image.getHeight();
        BufferedImage newImage = new BufferedImage(textWidth,
            textHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics gr = newImage.getGraphics();
        if (invertedImage != null) {
            assert (image != null);
            gr.drawImage(invertedImage, 0, 0, null, null);
        } else {
            gr.drawImage(image, 0, 0, null, null);
        }
        gr.dispose();
        return newImage;
    }

    /**
     * Flatten the image on this cell by rendering it either onto the
     * background color, or generating the glyph and rendering over that.
     *
     * @param overGlyph if true, render over the glyph
     * @param backend the backend that can obtain the correct background
     * color
     */
    public void flattenImage(final boolean overGlyph, final Backend backend) {
        if (!isImage()) {
            return;
        }

        if (hasTransparentPixels == 2) {
            // The image already covers the entire cell.
            return;
        }

        // We will be opaque when done.
        hasTransparentPixels = 2;

        int textWidth = image.getWidth();
        int textHeight = image.getHeight();
        BufferedImage newImage = new BufferedImage(textWidth,
            textHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics gr = newImage.getGraphics();
        gr.setColor(backend.attrToBackgroundColor(this));

        if (overGlyph) {
            // Render this cell to a flat image.  The bad news is that we
            // won't get to use the actual terminal's font.
            GlyphMaker glyphMaker = GlyphMaker.getInstance(textHeight);
            gr.drawImage(glyphMaker.getImage(this, textWidth, textHeight,
                    backend), 0, 0, null, null);
        } else {
            // Put the background color behind the pixels.
            gr.fillRect(0, 0, newImage.getWidth(),
                newImage.getHeight());
        }
        gr.drawImage(image, 0, 0, null, null);
        gr.dispose();
        setImage(newImage);
    }

    /**
     * Blit another cell's image on top of the image data for this cell.
     *
     * @param cell the other cell
     */
    public void blitImage(final Cell cell) {
        if (!cell.isImage() || cell.isFullyTransparentImage()) {
            // The other cell has no image data.
            return;
        }
        if (!isImage() || !cell.isTransparentImage()) {
            // Just replace this cell's image.
            setImage(cell.getImage());
            return;
        }
        assert (isImage() && cell.isTransparentImage());
        assert (image.getWidth() > 0);
        assert (image.getHeight() > 0);
        assert (cell.getImage().getWidth() > 0);
        assert (cell.getImage().getHeight() > 0);

        // Blit the new cell image over this cell's image.
        int textWidth = Math.min(image.getWidth(), cell.getImage().getWidth());
        int textHeight = Math.min(image.getHeight(),
            cell.getImage().getHeight());
        BufferedImage newImage = new BufferedImage(textWidth,
            textHeight, BufferedImage.TYPE_INT_ARGB);

        java.awt.Graphics gr = newImage.getGraphics();
        gr.setColor(java.awt.Color.BLACK);
        gr.drawImage(image, 0, 0, null, null);
        gr.drawImage(cell.getImage(), 0, 0, null, null);
        gr.dispose();
        setImage(newImage);
    }

    /**
     * Get the bitmap image background color for this cell.
     *
     * @return the bitmap image background color
     */
    public java.awt.Color getBackground() {
        return background;
    }

    /**
     * If true, this cell has image data.
     *
     * @return true if this cell is an image rather than a character with
     * attributes
     */
    public boolean isImage() {
        if (image != null) {
            return true;
        }
        return false;
    }

    /**
     * If true, this cell has image data and some of the pixels are
     * transparent.
     *
     * @return true if this cell has image data with transparent pixels
     */
    public boolean isTransparentImage() {
        if (image == null) {
            return false;
        }
        if (hasTransparentPixels == 0) {
            // Scan for transparent pixels.
            int [] rgbArray = image.getRGB(0, 0,
                image.getWidth(), image.getHeight(), null, 0, image.getWidth());

            for (int i = 0; i < rgbArray.length; i++) {
                if (((rgbArray[i] >>> 24) & 0xFF) != 0xFF) {
                    // A pixel might be transparent.
                    hasTransparentPixels = 1;
                    return true;
                }
            }
            // No transparent pixels.
            hasTransparentPixels = 2;
        }
        if ((hasTransparentPixels == 1)
            || (hasTransparentPixels == 3)
            || (hasTransparentPixels == 4)
        ) {
            // Transparent pixels were found at some time.
            return true;
        }
        assert (hasTransparentPixels == 2);
        return false;
    }

    /**
     * If true, this cell has image data and all of its pixels are fully
     * transparent (alpha of 0).
     *
     * @return true if this cell has image data with only transparent pixels
     */
    public boolean isFullyTransparentImage() {
        if (image == null) {
            return false;
        }
        if ((hasTransparentPixels == 0) || (hasTransparentPixels == 1)) {
            // Scan for transparent pixels.  Only if ALL pixels are
            // transparent do we return true.
            int [] rgbArray = image.getRGB(0, 0,
                image.getWidth(), image.getHeight(), null, 0, image.getWidth());

            if (rgbArray.length == 0) {
                // No image data, fully transparent.
                hasTransparentPixels = 3;
                return true;
            }

            boolean allOpaque = true;
            boolean allTransparent = true;
            for (int i = 0; i < rgbArray.length; i++) {
                int alpha = (rgbArray[i] >>> 24) & 0xFF;
                if ((alpha != 0xFF) && (alpha != 0x00)) {
                    // Some transparent pixels, but not fully transparent.
                    hasTransparentPixels = 4;
                    return false;
                }
                // This pixel is either fully opaque or fully transparent.
                if (alpha == 0xFF) {
                    allTransparent = false;
                } else {
                    allOpaque = false;
                }
            }
            if (allOpaque == true) {
                // No transparent pixels.
                hasTransparentPixels = 2;
            } else {
                assert (allTransparent == true);
                hasTransparentPixels = 3;
            }
        }
        if (hasTransparentPixels == 3) {
            // Fully transparent.
            return true;
        }
        return false;
    }

    /**
     * Force calls to isTransparentImage() to always return false.
     */
    public void setOpaqueImage() {
        if (image == null) {
            hasTransparentPixels = -1;
        }
        hasTransparentPixels = 2;
    }

    /**
     * Restore the image in this cell to its normal version, if it has one.
     */
    public void restoreImage() {
        invertedImage = null;
    }

    /**
     * If true, this cell has image data, and that data is inverted.
     *
     * @return true if this cell is an image rather than a character with
     * attributes, and the data is inverted
     */
    public boolean isInvertedImage() {
        if ((image != null) && (invertedImage != null)) {
            return true;
        }
        return false;
    }

    /**
     * Invert the image in this cell, if it has one.
     */
    public void invertImage() {
        if (image == null) {
            return;
        }
        if (invertedImage == null) {
            invertedImage = new BufferedImage(image.getWidth(),
                image.getHeight(), BufferedImage.TYPE_INT_ARGB);

            int [] rgbArray = image.getRGB(0, 0,
                image.getWidth(), image.getHeight(), null, 0, image.getWidth());

            for (int i = 0; i < rgbArray.length; i++) {
                // Set the colors to fully inverted.
                if (rgbArray[i] != 0x00FFFFFF) {
                    rgbArray[i] ^= 0x00FFFFFF;
                }
            }
            invertedImage.setRGB(0, 0, image.getWidth(), image.getHeight(),
                rgbArray, 0, image.getWidth());
        }
    }

    /**
     * Getter for cell character.
     *
     * @return cell character
     */
    public int getChar() {
        return ch;
    }

    /**
     * Setter for cell character.
     *
     * @param ch new cell character
     */
    public void setChar(final int ch) {
        this.ch = ch;
    }

    /**
     * Getter for cell width.
     *
     * @return Width.SINGLE, Width.LEFT, or Width.RIGHT
     */
    public Width getWidth() {
        return width;
    }

    /**
     * Setter for cell width.
     *
     * @param width new cell width, one of Width.SINGLE, Width.LEFT, or
     * Width.RIGHT
     */
    public void setWidth(final Width width) {
        this.width = width;
    }

    /**
     * Reset this cell to a blank.
     */
    @Override
    public void reset() {
        super.reset();
        ch = ' ';
        width = Width.SINGLE;
        image = null;
        imageHashCode = 0;
        invertedImage = null;
        background = java.awt.Color.BLACK;
        backgroundHashCode = 0;
        hasTransparentPixels = -1;
    }

    /**
     * UNset this cell.  It will not be equal to any other cell until it has
     * been assigned attributes and a character.
     */
    public void unset() {
        super.reset();
        ch = UNSET_VALUE;
        width = Width.SINGLE;
        image = null;
        imageHashCode = 0;
        invertedImage = null;
        background = java.awt.Color.BLACK;
        backgroundHashCode = 0;
        hasTransparentPixels = -1;
    }

    /**
     * Check to see if this cell has default attributes: white foreground,
     * black background, no bold/blink/reverse/underline/protect, and a
     * character value of ' ' (space).
     *
     * @return true if this cell has default attributes.
     */
    public boolean isBlank() {
        if ((ch == UNSET_VALUE) || (image != null)) {
            return false;
        }
        if ((getForeColor().equals(jexer.bits.Color.WHITE))
            && (getBackColor().equals(jexer.bits.Color.BLACK))
            && !isBold()
            && !isBlink()
            && !isReverse()
            && !isUnderline()
            && !isProtect()
            && !isRGB()
            && !isImage()
            && (width == Width.SINGLE)
            && (ch == ' ')
        ) {
            return true;
        }
        return false;
    }

    /**
     * If true, this cell can be placed in a glyph cache somewhere so that it
     * does not have to be re-rendered many times.
     *
     * @return true if this cell can be placed in a cache
     */
    public boolean isCacheable() {
        /*
         * Heuristics, omit cells that:
         *
         *   - Are text only and have 24-bit RGB color.
         *
         *   - Are image over a glyph.
         */
        if ((image == null)
            && (getForeColorRGB() != -1)
            && (getBackColorRGB() != -1)
        ) {
            return false;
        }
        if ((image != null) && (ch != ' ')) {
            return false;
        }
        return true;
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another Cell instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof Cell)) {
            return false;
        }

        Cell that = (Cell) rhs;

        // Unsetted cells can never be equal.
        if ((ch == UNSET_VALUE) || (that.ch == UNSET_VALUE)) {
            return false;
        }

        // If this or rhs has an image and the other doesn't, these are not
        // equal.
        if ((image != null) && (that.image == null)) {
            return false;
        }
        if ((image == null) && (that.image != null)) {
            return false;
        }
        // If this and rhs have images, both must match.
        if ((image != null) && (that.image != null)) {
            if ((invertedImage == null) && (that.invertedImage != null)) {
                return false;
            }
            if ((invertedImage != null) && (that.invertedImage == null)) {
                return false;
            }
            // Either both objects have their image inverted, or neither do.
            if ((imageHashCode == that.imageHashCode)
                && (background.equals(that.background))
            ) {
                // Fall through to the attributes check below.
                // ...
            } else {
                // The cells are not the same visually.
                return false;
            }
        }

        // Normal case: character and attributes must match.
        if ((ch == that.ch) && (width == that.width)) {
            return super.equals(rhs);
        }
        return false;
    }

    /**
     * Hashcode uses all fields in equals().
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        int A = 13;
        int B = 23;
        int hash = A;
        hash = (B * hash) + super.hashCode();
        hash = (B * hash) + ch;
        hash = (B * hash) + width.hashCode();
        if (image != null) {
            /*
            hash = (B * hash) + image.hashCode();
            hash = (B * hash) + background.hashCode();
             */
            hash = (B * hash) + imageHashCode;
            hash = (B * hash) + backgroundHashCode;
        }
        if (invertedImage != null) {
            hash = (B * hash) + invertedImage.hashCode();
        }
        return hash;
    }

    /**
     * Set my field values to that's field.
     *
     * @param rhs an instance of either Cell or CellAttributes
     */
    @Override
    public void setTo(final Object rhs) {
        if (rhs instanceof Cell) {
            Cell that = (Cell) rhs;
            this.ch = that.ch;
            this.width = that.width;
            this.image = that.image;
            this.invertedImage = that.invertedImage;
            this.background = that.background;
            this.imageHashCode = that.imageHashCode;
            this.backgroundHashCode = that.backgroundHashCode;
            this.hasTransparentPixels = that.hasTransparentPixels;
        } else {
            this.image = null;
            this.imageHashCode = 0;
            this.backgroundHashCode = 0;
            this.hasTransparentPixels = -1;
            this.width = Width.SINGLE;
        }
        // Let this throw a ClassCastException
        CellAttributes thatAttr = (CellAttributes) rhs;
        super.setTo(thatAttr);
    }

    /**
     * Set my field attr values to that's field.
     *
     * @param that a CellAttributes instance
     */
    public void setAttr(final CellAttributes that) {
        image = null;
        hasTransparentPixels = -1;
        super.setTo(that);
    }

    /**
     * Set my field attr values to that's field.
     *
     * @param that a CellAttributes instance
     * @param keepImage if true, retain the image data
     */
    public void setAttr(final CellAttributes that, final boolean keepImage) {
        if (!keepImage) {
            image = null;
            hasTransparentPixels = -1;
        }
        super.setTo(that);
    }

    /**
     * Make human-readable description of this Cell.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("%s fore: %s back: %s bold: %s blink: %s ch %c",
            (isImage() ? "IMAGE" : ""),
            getForeColor(), getBackColor(), isBold(), isBlink(), ch);
    }

    /**
     * Convert this cell into an HTML entity inside a &lt;font&gt; tag.
     *
     * @return the HTML string
     */
    public String toHtml() {
        StringBuilder sb = new StringBuilder("<font ");
        sb.append(super.toHtml());
        sb.append('>');
        if (ch == ' ') {
            sb.append("&nbsp;");
        } else if (ch == '<') {
            sb.append("&lt;");
        } else if (ch == '>') {
            sb.append("&gt;");
        } else if (ch < 0x7F) {
            sb.append((char) ch);
        } else {
            sb.append(String.format("&#%d;", ch));
        }
        sb.append("</font>");
        return sb.toString();
    }

}
