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
package jexer.tackboard;

import java.awt.image.BufferedImage;

/**
 * MousePointer is a Bitmap with a hotspot location to represent the "tip" of
 * a mouse icon.
 */
public class MousePointer extends Bitmap implements Pointer {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the hotspot X location relative to the X location of the icon.
     */
    private int hotspotX = 0;

    /**
     * Get the hotspot Y location relative to the Y location of the icon.
     */
    private int hotspotY = 0;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param x X pixel coordinate
     * @param y Y pixel coordinate
     * @param z Z coordinate
     * @param image the image
     * @param hotspotX the hotspot X location relative to x
     * @param hotspotY the hotspot Y location relative to y
     */
    public MousePointer(final int x, final int y, final int z,
        final BufferedImage image, final int hotspotX, final int hotspotY) {

        super(x, y, z, image);
        this.hotspotX = hotspotX;
        this.hotspotY = hotspotY;
    }

    // ------------------------------------------------------------------------
    // TackboardItem ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another MousePointer instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof MousePointer)) {
            return false;
        }
        MousePointer that = (MousePointer) rhs;
        return (super.equals(rhs)
            && (this.hotspotX == that.hotspotX)
            && (this.hotspotY == that.hotspotY));
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
        hash = (B * hash) + hotspotX;
        hash = (B * hash) + hotspotY;
        return hash;
    }

    /**
     * Make human-readable description of this item.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("MousePointer[%d, %d]: %s",
            hotspotX, hotspotY, super.toString());
    }

    // ------------------------------------------------------------------------
    // Pointer ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the hotspot X location relative to the X location of the icon.
     */
    public int getHotspotX() {
        return hotspotX;
    }

    /**
     * Get the hotspot Y location relative to the Y location of the icon.
     */
    public int getHotspotY() {
        return hotspotY;
    }

}
