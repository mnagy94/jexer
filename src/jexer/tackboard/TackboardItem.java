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
 * This class represents a single item image on the tackboard.
 */
public class TackboardItem implements Comparable<TackboardItem> {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

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

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

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

        // TODO
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

}
