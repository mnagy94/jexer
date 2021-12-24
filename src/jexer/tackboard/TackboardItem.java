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

import java.awt.image.BufferedImage;

/**
 * TackboardItem class represents a single item that can generate pixels on
 * the tackboard.
 */
public class TackboardItem implements Comparable<TackboardItem> {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The board this item is on.
     */
    private Tackboard tackboard;

    /**
     * X pixel coordinate.
     */
    private int x = 0;

    /**
     * Y pixel coordinate.
     */
    private int y = 0;

    /**
     * Z pixel coordinate.
     */
    private int z = 0;

    /**
     * Dirty flag, if true then getImage() needs to generate a rendering
     * aligned to the text cells.
     */
    protected boolean dirty = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param x X pixel coordinate
     * @param y Y pixel coordinate
     * @param z Z coordinate
     */
    public TackboardItem(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Public constructor.
     */
    public TackboardItem() {
        // NOP
    }

    // ------------------------------------------------------------------------
    // TackboardItem ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get X position.
     *
     * @return absolute X position of the top-left corner in pixels
     */
    public final int getX() {
        return x;
    }

    /**
     * Set X position.
     *
     * @param x absolute X position of the top-left corner in pixels
     */
    public final void setX(final int x) {
        if (x != this.x) {
            this.x = x;
            dirty = true;
        }
    }

    /**
     * Get Y position.
     *
     * @return absolute Y position of the top-left corner in pixels
     */
    public final int getY() {
        return y;
    }

    /**
     * Set Y position.
     *
     * @param y absolute Y position of the top-left corner in pixels
     */
    public final void setY(final int y) {
        if (y != this.y) {
            this.y = y;
            dirty = true;
        }
    }

    /**
     * Get Z position.
     *
     * @return absolute Z position
     */
    public final int getZ() {
        return z;
    }

    /**
     * Set Z position.
     *
     * @param z absolute Z position
     */
    public final void setZ(final int z) {
        if (z != this.z) {
            this.z = z;
            dirty = true;
        }
    }

    /**
     * Get dirty flag.
     *
     * @return if true, the image data is not aligned with the physical
     * screen cells
     */
    public final boolean isDirty() {
        return dirty;
    }

    /**
     * Set dirty flag.
     */
    public final void setDirty() {
        dirty = true;
    }

    /**
     * Set the tackboard this item is on.
     *
     * @param tackboard
     */
    public final void setTackboard(final Tackboard tackboard) {
        this.tackboard = tackboard;
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another TackboardItem instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof TackboardItem)) {
            return false;
        }
        TackboardItem that = (TackboardItem) rhs;
        return ((this.tackboard == that.tackboard)
            && (this.x == that.x)
            && (this.y == that.y)
            && (this.z == that.z));
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
        hash = (B * hash) + x;
        hash = (B * hash) + y;
        hash = (B * hash) + z;
        return hash;
    }

    /**
     * Comparison operator.
     *
     * @param that another TackboardItem instance
     * @return differences between this.x/y/z and that.x/y/z
     */
    public int compareTo(final TackboardItem that) {
        if (this.z != that.z) {
            return this.z - that.z;
        }
        if (this.y != that.y) {
            return that.y - this.y;
        }
        return that.x - this.x;
    }

    /**
     * Make human-readable description of this item.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("(%d, %d, %d)", x, y, z);
    }

    /**
     * Get this item rendered to a bitmap, offset to align on a grid of
     * cells with pixel dimensions (textWidth, textHeight).
     *
     * @param textWidth the width of a text cell
     * @param textHeight the height of a text cell
     * @return the image, or null if this item does not have any pixels to
     * show
     */
    public BufferedImage getImage(final int textWidth, final int textHeight) {
        // Default does nothing.
        return null;
    }

    /**
     * Remove this item from its board.  Subclasses can use this for cleanup
     * also.
     */
    public void remove() {
        if (tackboard != null) {
            tackboard.getItems().remove(this);
        }
        tackboard = null;
    }

}
