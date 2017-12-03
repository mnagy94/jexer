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
package jexer.bits;

/**
 * The attributes used by a Cell: color, bold, blink, etc.
 */
public class CellAttributes {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Bold attribute.
     */
    private boolean bold;

    /**
     * Blink attribute.
     */
    private boolean blink;

    /**
     * Reverse attribute.
     */
    private boolean reverse;

    /**
     * Underline attribute.
     */
    private boolean underline;

    /**
     * Protected attribute.
     */
    private boolean protect;

    /**
     * Foreground color.  Color.WHITE, Color.RED, etc.
     */
    private Color foreColor;

    /**
     * Background color.  Color.WHITE, Color.RED, etc.
     */
    private Color backColor;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor sets default values of the cell to white-on-black,
     * no bold/blink/reverse/underline/protect.
     *
     * @see #reset()
     */
    public CellAttributes() {
        reset();
    }

    /**
     * Public constructor makes a copy from another instance.
     *
     * @param that another CellAttributes instance
     * @see #reset()
     */
    public CellAttributes(final CellAttributes that) {
        setTo(that);
    }

    // ------------------------------------------------------------------------
    // CellAttributes ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Getter for bold.
     *
     * @return bold value
     */
    public final boolean isBold() {
        return bold;
    }

    /**
     * Setter for bold.
     *
     * @param bold new bold value
     */
    public final void setBold(final boolean bold) {
        this.bold = bold;
    }

    /**
     * Getter for blink.
     *
     * @return blink value
     */
    public final boolean isBlink() {
        return blink;
    }

    /**
     * Setter for blink.
     *
     * @param blink new blink value
     */
    public final void setBlink(final boolean blink) {
        this.blink = blink;
    }

    /**
     * Getter for reverse.
     *
     * @return reverse value
     */
    public final boolean isReverse() {
        return reverse;
    }

    /**
     * Setter for reverse.
     *
     * @param reverse new reverse value
     */
    public final void setReverse(final boolean reverse) {
        this.reverse = reverse;
    }

    /**
     * Getter for underline.
     *
     * @return underline value
     */
    public final boolean isUnderline() {
        return underline;
    }

    /**
     * Setter for underline.
     *
     * @param underline new underline value
     */
    public final void setUnderline(final boolean underline) {
        this.underline = underline;
    }

    /**
     * Getter for protect.
     *
     * @return protect value
     */
    public final boolean isProtect() {
        return protect;
    }

    /**
     * Setter for protect.
     *
     * @param protect new protect value
     */
    public final void setProtect(final boolean protect) {
        this.protect = protect;
    }

    /**
     * Getter for foreColor.
     *
     * @return foreColor value
     */
    public final Color getForeColor() {
        return foreColor;
    }

    /**
     * Setter for foreColor.
     *
     * @param foreColor new foreColor value
     */
    public final void setForeColor(final Color foreColor) {
        this.foreColor = foreColor;
    }

    /**
     * Getter for backColor.
     *
     * @return backColor value
     */
    public final Color getBackColor() {
        return backColor;
    }

    /**
     * Setter for backColor.
     *
     * @param backColor new backColor value
     */
    public final void setBackColor(final Color backColor) {
        this.backColor = backColor;
    }

    /**
     * Set to default: white foreground on black background, no
     * bold/underline/blink/rever/protect.
     */
    public void reset() {
        bold      = false;
        blink     = false;
        reverse   = false;
        underline = false;
        protect   = false;
        foreColor = Color.WHITE;
        backColor = Color.BLACK;
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another CellAttributes instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof CellAttributes)) {
            return false;
        }

        CellAttributes that = (CellAttributes) rhs;
        return ((foreColor == that.foreColor)
            && (backColor == that.backColor)
            && (bold == that.bold)
            && (reverse == that.reverse)
            && (underline == that.underline)
            && (blink == that.blink)
            && (protect == that.protect));
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
        hash = (B * hash) + (bold ? 1 : 0);
        hash = (B * hash) + (blink ? 1 : 0);
        hash = (B * hash) + (underline ? 1 : 0);
        hash = (B * hash) + (reverse ? 1 : 0);
        hash = (B * hash) + (protect ? 1 : 0);
        hash = (B * hash) + foreColor.hashCode();
        hash = (B * hash) + backColor.hashCode();
        return hash;
    }

    /**
     * Set my field values to that's field.
     *
     * @param rhs another CellAttributes instance
     */
    public void setTo(final Object rhs) {
        CellAttributes that = (CellAttributes) rhs;

        this.bold      = that.bold;
        this.blink     = that.blink;
        this.reverse   = that.reverse;
        this.underline = that.underline;
        this.protect   = that.protect;
        this.foreColor = that.foreColor;
        this.backColor = that.backColor;
    }

    /**
     * Make human-readable description of this CellAttributes.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("%s%s%s on %s", (bold == true ? "bold " : ""),
            (blink == true ? "blink " : ""), foreColor, backColor);
    }

}
