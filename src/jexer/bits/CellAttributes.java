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
package jexer.bits;

import jexer.backend.Backend;

/**
 * The attributes used by a Cell: color, bold, blink, etc.
 */
public class CellAttributes {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Bold attribute.
     */
    private static final int BOLD       = 0x01;

    /**
     * Blink attribute.
     */
    private static final int BLINK      = 0x02;

    /**
     * Reverse attribute.
     */
    private static final int REVERSE    = 0x04;

    /**
     * Underline attribute.
     */
    private static final int UNDERLINE  = 0x08;

    /**
     * Protected attribute.
     */
    private static final int PROTECT    = 0x10;

    /**
     * Animation bits for time-dependent transforms.
     */
    private static final int ANIMATION_MASK       = 0xFFFFF000;

    /**
     * Animation: pulse color.  8 bits: rrrgggbb.  Fades and pulses will go
     * between this color and the foregound RGB color.
     */
    private static final int ANIMATION_COLOR_MASK = 0xFF000000;

    /**
     * Animation: timer seed value.  Currently supporting 6 bits.
     */
    private static final int ANIMATION_TIME_MASK  = 0x00FC0000;

    /**
     * Animation: fade in.
     */
    // private static final int ANIMATION_FADE_IN    = 0x00001000;

    /**
     * Animation: fade out.
     */
    // private static final int ANIMATION_FADE_OUT   = 0x00002000;

    /**
     * Animation: pulse slowly.
     */
    private static final int ANIMATION_PULSE      = 0x00004000;

    /**
     * Animation: pulse quickly.
     */
    private static final int ANIMATION_PULSE_FAST = 0x00008000;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Boolean flags.
     */
    private int flags = 0;

    /**
     * Foreground color.  Color.WHITE, Color.RED, etc.
     */
    private Color foreColor = Color.WHITE;

    /**
     * Background color.  Color.WHITE, Color.RED, etc.
     */
    private Color backColor = Color.BLACK;

    /**
     * Foreground color as 24-bit RGB value.  Negative value means not set.
     */
    private int foreColorRGB = -1;

    /**
     * Background color as 24-bit RGB value.  Negative value means not set.
     */
    private int backColorRGB = -1;

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
        // NOP
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
        return ((flags & BOLD) != 0);
    }

    /**
     * Setter for bold.
     *
     * @param bold new bold value
     */
    public final void setBold(final boolean bold) {
        if (bold) {
            flags |= BOLD;
        } else {
            flags &= ~BOLD;
        }
    }

    /**
     * Getter for blink.
     *
     * @return blink value
     */
    public final boolean isBlink() {
        return ((flags & BLINK) == 0 ? false : true);
    }

    /**
     * Setter for blink.
     *
     * @param blink new blink value
     */
    public final void setBlink(final boolean blink) {
        if (blink) {
            flags |= BLINK;
        } else {
            flags &= ~BLINK;
        }
    }

    /**
     * Getter for reverse.
     *
     * @return reverse value
     */
    public final boolean isReverse() {
        return ((flags & REVERSE) == 0 ? false : true);
    }

    /**
     * Setter for reverse.
     *
     * @param reverse new reverse value
     */
    public final void setReverse(final boolean reverse) {
        if (reverse) {
            flags |= REVERSE;
        } else {
            flags &= ~REVERSE;
        }
    }

    /**
     * Getter for underline.
     *
     * @return underline value
     */
    public final boolean isUnderline() {
        return ((flags & UNDERLINE) == 0 ? false : true);
    }

    /**
     * Setter for underline.
     *
     * @param underline new underline value
     */
    public final void setUnderline(final boolean underline) {
        if (underline) {
            flags |= UNDERLINE;
        } else {
            flags &= ~UNDERLINE;
        }
    }

    /**
     * Getter for protect.
     *
     * @return protect value
     */
    public final boolean isProtect() {
        return ((flags & PROTECT) == 0 ? false : true);
    }

    /**
     * Setter for protect.
     *
     * @param protect new protect value
     */
    public final void setProtect(final boolean protect) {
        if (protect) {
            flags |= PROTECT;
        } else {
            flags &= ~PROTECT;
        }
    }

    /**
     * Getter for animation flags.
     *
     * @return animation flags.
     */
    public final int getAnimations() {
        return (flags & ANIMATION_MASK);
    }

    /**
     * Setter for animations.
     *
     * @param animationFlags new animation flags
     */
    public final void setAnimations(final int animationFlags) {
        flags &= ~ANIMATION_MASK;
        flags |= animationFlags;
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
        this.foreColorRGB = -1;
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
        this.backColorRGB = -1;
    }

    /**
     * Get the current pulsed RGB value for foreColor.
     *
     * @param backend the backend that can obtain the correct ANSI
     * (palette-based) foreground color
     * @param pulseTimeMillis the time to compute the pulse color for in
     * millis
     * @return foreColor RGB value at this time slice.
     */
    public final int getForeColorPulseRGB(final Backend backend,
        final long pulseTimeMillis) {

        if ((flags & (ANIMATION_PULSE | ANIMATION_PULSE_FAST)) == 0) {
            // Not pulsed.
            if (foreColorRGB != -1) {
                return foreColorRGB;
            }
            return backend.attrToForegroundColor(this).getRGB() & 0xFFFFFF;
        }

        int offsetSlice = (flags & ANIMATION_TIME_MASK) >>> 16;
        int slice = 0;
        int sliceN = 16;
        int sliceI = 0;
        if ((flags & ANIMATION_PULSE_FAST) == 0) {
            // This is a fast pulse: 32 steps / second
            sliceN = 32;
            slice = (int) (pulseTimeMillis * sliceN / 1000);
            sliceI = (slice + offsetSlice) & 0x1F;
        } else {
            // Slow pulse: 16 steps / second
            slice = (int) (pulseTimeMillis * (sliceN * 2) / 1000);
            sliceI = (slice + offsetSlice) & 0x0F;
        }
        if (sliceI >= (sliceN / 2)) {
            sliceI = sliceN - sliceI;
        }
        int pulseColorRGB = getPulseColorRGB();
        int foreColorRGB = backend.attrToForegroundColor(this).getRGB();
        double fraction = (double) sliceI * 2.0 / (sliceN - 1);
        int finalColor = ImageUtils.rgbMove(foreColorRGB, pulseColorRGB,
            fraction);
        return finalColor;
    }

    /**
     * Getter for foreColor RGB.  Note that this is always a RGB value,
     * i.e. alpha is 0.
     *
     * @return foreColor value.  Negative means unset.
     */
    public final int getForeColorRGB() {
        return foreColorRGB;
    }

    /**
     * Setter for foreColor RGB.
     *
     * @param foreColorRGB new foreColor RGB value
     */
    public final void setForeColorRGB(final int foreColorRGB) {
        this.foreColorRGB = foreColorRGB & 0xFFFFFF;
        this.foreColor = Color.WHITE;
    }

    /**
     * Getter for backColor RGB.  Note that this is always a RGB value,
     * i.e. alpha is 0.
     *
     * @return backColor value.  Negative means unset.
     */
    public final int getBackColorRGB() {
        return backColorRGB;
    }

    /**
     * Setter for backColor RGB.
     *
     * @param backColorRGB new backColor RGB value
     */
    public final void setBackColorRGB(final int backColorRGB) {
        this.backColorRGB = backColorRGB & 0xFFFFFF;
        this.backColor = Color.BLACK;
    }

    /**
     * See if this cell uses RGB or ANSI colors.
     *
     * @return true if this cell has a RGB color
     */
    public final boolean isRGB() {
        return (foreColorRGB >= 0) || (backColorRGB >= 0);
    }

    /**
     * Set to default: white foreground on black background, no
     * bold/underline/blink/rever/protect.
     */
    public void reset() {
        flags           = 0;
        foreColor       = Color.WHITE;
        backColor       = Color.BLACK;
        foreColorRGB    = -1;
        backColorRGB    = -1;
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
        return ((flags == that.flags)
            && (foreColor == that.foreColor)
            && (backColor == that.backColor)
            && (foreColorRGB == that.foreColorRGB)
            && (backColorRGB == that.backColorRGB));
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
        hash = (B * hash) + flags;
        hash = (B * hash) + foreColor.hashCode();
        hash = (B * hash) + backColor.hashCode();
        hash = (B * hash) + foreColorRGB;
        hash = (B * hash) + backColorRGB;
        return hash;
    }

    /**
     * Set my field values to that's field.
     *
     * @param rhs another CellAttributes instance
     */
    public void setTo(final Object rhs) {
        CellAttributes that = (CellAttributes) rhs;

        this.flags              = that.flags;
        this.foreColor          = that.foreColor;
        this.backColor          = that.backColor;
        this.foreColorRGB       = that.foreColorRGB;
        this.backColorRGB       = that.backColorRGB;
    }

    /**
     * Make human-readable description of this CellAttributes.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        if ((foreColorRGB >= 0) || (backColorRGB >= 0)) {
            StringBuilder sb = new StringBuilder("RGB: ");

            if (foreColorRGB < 0) {
                sb.append(foreColor.toRgbString());
            } else {
                sb.append(String.format("#%06x",
                        (foreColorRGB & 0xFFFFFF)));
            }
            sb.append(" on ");
            if (backColorRGB < 0) {
                sb.append(backColor.toRgbString());
            } else {
                sb.append(String.format("#%06x",
                        (backColorRGB & 0xFFFFFF)));
            }
            return sb.toString();
        }
        return String.format("%s%s%s on %s", (isBold() ? "bold " : ""),
            (isBlink() ? "blink " : ""), foreColor, backColor);
    }

    /**
     * Convert these cell attributes into the style attributes of an HTML
     * &lt;font&gt; tag.
     *
     * @return the HTML string
     */
    public String toHtml() {
        String fontWeight = "normal";
        String textDecoration = "none";
        String fgText;
        String bgText;

        if (isBlink() && isUnderline()) {
            textDecoration = "blink, underline";
        } else if (isUnderline()) {
            textDecoration = "underline";
        } else if (isBlink()) {
            textDecoration = "blink";
        }
        if (isReverse()) {
            fgText = backColor.toRgbString(false);
            if (isBold()) {
                bgText = foreColor.toRgbString(true);
            } else {
                bgText = foreColor.toRgbString(false);
            }
        } else {
            bgText = backColor.toRgbString(false);
            if (isBold()) {
                fgText = foreColor.toRgbString(true);
            } else {
                fgText = foreColor.toRgbString(false);
            }
        }

        return String.format("style=\"color: %s; background-color: %s; " +
            "text-decoration: %s; font-weight: %s\"",
            fgText, bgText, textDecoration, fontWeight);
    }

    /**
     * Getter for animation pulse.
     *
     * @return pulse value
     */
    public final boolean isPulse() {
        if ((flags & (ANIMATION_PULSE | ANIMATION_PULSE_FAST)) != 0) {
            return true;
        }
        return false;
    }

    /**
     * Setter for animation pulse.
     *
     * @param pulse new pulse value
     * @param fast if true, fast pulse
     * @param offset number of frames (up to 16 for slow, up to 32 for
     * fast) to offset the pulse animation
     */
    public final void setPulse(final boolean pulse, final boolean fast,
        int offset) {

        flags &= ~(ANIMATION_PULSE | ANIMATION_PULSE_FAST);
        if (!pulse && !fast) {
            return;
        }
        int sliceN = 16;
        if (fast) {
            flags |= ANIMATION_PULSE_FAST;
            sliceN = 32;
            offset &= 0x1F;
        } else {
            flags |= ANIMATION_PULSE;
            offset &= 0x0F;
        }
        flags &= ~ANIMATION_TIME_MASK;
        flags |= (offset & 0x3F) << 16;
    }

    /**
     * Getter for pulse color RGB.
     *
     * @return pulse color RGB value
     */
    public final int getPulseColorRGB() {
        int pulseColor = (flags >>> 24) & 0xFF;
        int colorRGB = ((pulseColor & 0xE0) << 16)
                | ((pulseColor & 0x1C) << 11)
                | ((pulseColor & 0x03) << 6);

        return colorRGB;
    }

    /**
     * Setter for pulse color RGB.
     *
     * @param pulseColorRGB new pulse color RGB value
     */
    public final void setPulseColorRGB(final int pulseColorRGB) {
        int color = ((pulseColorRGB & 0xE00000) >>> 16)
                | ((pulseColorRGB & 0xE000) >>> 11)
                | ((pulseColorRGB & 0xC0) >>> 6);

        flags &= ~ANIMATION_COLOR_MASK;
        flags |= color << 24;

        // System.err.printf("setPulseColorRGB(): %08x\n", getPulseColorRGB());
    }

}
