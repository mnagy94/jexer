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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import jexer.bits.BorderStyle;
import jexer.bits.Cell;
import jexer.bits.CellAttributes;
import jexer.bits.Color;
import jexer.bits.GraphicsChars;
import jexer.bits.MnemonicString;
import jexer.bits.StringUtils;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import static jexer.TKeypress.kbEnter;
import static jexer.TKeypress.kbSpace;

/**
 * TButton implements a simple button.  To make the button do something, pass
 * a TAction class to its constructor.
 *
 * @see TAction#DO()
 */
public class TButton extends TWidget {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Available styles of the button.
     */
    public static enum Style {

        /**
         * A fully-text square button.  The default style.
         */
        SQUARE,

        /**
         * A button with round semi-circle edges.
         */
        ROUND,

        /**
         * A button with diamond end points.
         */
        DIAMOND,

        /**
         * A button arrow pointing left.
         */
        ARROW_LEFT,

        /**
         * A button arrow pointing right.
         */
        ARROW_RIGHT,
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The shortcut and button text.
     */
    private MnemonicString mnemonic;

    /**
     * Remember mouse state.
     */
    private TMouseEvent mouse;

    /**
     * True when the button is being pressed and held down.
     */
    private boolean inButtonPress = false;

    /**
     * The action to perform when the button is clicked.
     */
    private TAction action;

    /**
     * The background color used for the button "shadow", or null for "no
     * shadow".
     */
    private CellAttributes shadowColor;

    /**
     * The background color used for the button shadow, as set by
     * setShadowColor().
     */
    private CellAttributes givenShadowColor;

    /**
     * The style of button to draw.
     */
    private Style style = Style.SQUARE;

    /**
     * The left edge character.
     */
    private Cell leftEdgeChar;

    /**
     * The right edge character.
     */
    private Cell rightEdgeChar;

    /**
     * The left edge character.
     */
    private Cell leftEdgeShadowChar;

    /**
     * The right edge character.
     */
    private Cell rightEdgeShadowCharTop;

    /**
     * The right edge character.
     */
    private Cell rightEdgeShadowCharBottom;

    /**
     * The bottom shadow character.
     */
    private Cell shadowCharBottom;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor.
     *
     * @param parent parent widget
     * @param text label on the button
     * @param x column relative to parent
     * @param y row relative to parent
     */
    private TButton(final TWidget parent, final String text,
        final int x, final int y) {

        // Set parent and window
        super(parent);

        mnemonic = new MnemonicString(text);

        setX(x);
        setY(y);
        super.setHeight(2);
        super.setWidth(StringUtils.width(mnemonic.getRawLabel()) + 3);

        setStyle((String) null);

        // Since we set dimensions after TWidget's constructor, we need to
        // update the layout manager.
        if (getParent().getLayoutManager() != null) {
            getParent().getLayoutManager().remove(this);
            getParent().getLayoutManager().add(this);
        }
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param text label on the button
     * @param x column relative to parent
     * @param y row relative to parent
     * @param action to call when button is pressed
     */
    public TButton(final TWidget parent, final String text,
        final int x, final int y, final TAction action) {

        this(parent, text, x, y);
        this.action = action;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently on the button.
     *
     * @return if true the mouse is currently on the button
     */
    private boolean mouseOnButton() {
        int rightEdge = getWidth() - 1;
        if (inButtonPress) {
            rightEdge++;
        }
        if ((mouse != null)
            && (mouse.getY() == 0)
            && (mouse.getX() >= 0)
            && (mouse.getX() < rightEdge)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        if ((mouseOnButton()) && (mouse.isMouse1())) {
            if (!inButtonPress) {
                rightEdgeShadowCharTop = null;
            }
            // Begin button press
            inButtonPress = true;
        }
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        this.mouse = mouse;

        if (inButtonPress && mouse.isMouse1()) {
            // Dispatch the event
            dispatch();
        }

    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        this.mouse = mouse;

        if (!mouseOnButton()) {
            if (inButtonPress) {
                rightEdgeShadowCharTop = null;
            }
            inButtonPress = false;
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbEnter)
            || keypress.equals(kbSpace)
        ) {
            // Dispatch
            dispatch();
            return;
        }

        // Pass to parent for the things we don't care about.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget.setActive() so that the button ends are redrawn.
     *
     * @param enabled if true, this widget can be tabbed to or receive events
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        rightEdgeShadowCharBottom = null;
        shadowColor = null;
    }

    /**
     * Override TWidget's width: we can only set width at construction time.
     *
     * @param width new widget width (ignored)
     */
    @Override
    public void setWidth(final int width) {
        // Do nothing
    }

    /**
     * Override TWidget's height: we can only set height at construction
     * time.
     *
     * @param height new widget height (ignored)
     */
    @Override
    public void setHeight(final int height) {
        // Do nothing
    }

    /**
     * Draw a button with a shadow.
     */
    @Override
    public void draw() {
        CellAttributes buttonColor;
        CellAttributes menuMnemonicColor;

        if (shadowColor == null) {
            shadowColor = new CellAttributes();
            if (givenShadowColor == null) {
                shadowColor.setTo(getWindow().getBackground());
            } else {
                shadowColor.setTo(givenShadowColor);
            }
            shadowColor.setForeColor(Color.BLACK);
            shadowColor.setBold(false);
        }

        if (!isEnabled()) {
            buttonColor = getTheme().getColor("tbutton.disabled");
            menuMnemonicColor = getTheme().getColor("tbutton.disabled");
        } else if (isAbsoluteActive()) {
            buttonColor = getTheme().getColor("tbutton.active");
            menuMnemonicColor = getTheme().getColor("tbutton.mnemonic.highlighted");
        } else {
            buttonColor = getTheme().getColor("tbutton.inactive");
            menuMnemonicColor = getTheme().getColor("tbutton.mnemonic");
        }

        if ((leftEdgeChar == null)
            || (rightEdgeChar == null)
            || (leftEdgeShadowChar == null)
            || (rightEdgeShadowCharTop == null)
            || (rightEdgeShadowCharBottom == null)
            || (shadowCharBottom == null)
        ) {
            // TODO: If the color theme changes, we need to regenerate these.
            drawEnds(getWindow().getBackground(), buttonColor);
        }

        buttonColor = new CellAttributes(buttonColor);
        buttonColor.setForeColorRGB(getScreen().getBackend().
            attrToForegroundColor(buttonColor).getRGB());
        menuMnemonicColor = new CellAttributes(menuMnemonicColor);
        menuMnemonicColor.setForeColorRGB(getScreen().getBackend().
            attrToForegroundColor(menuMnemonicColor).getRGB());

        // Pulse colors.
        if (isActive() && getWindow().isActive()) {
            buttonColor.setPulse(true, false, 0);
            buttonColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getTheme().getColor(
                    "tbutton.pulse")).getRGB());
            menuMnemonicColor.setPulse(true, false, 0);
            menuMnemonicColor.setPulseColorRGB(getScreen().getBackend().
                attrToForegroundColor(getTheme().getColor(
                    "tbutton.mnemonic.pulse")).getRGB());
        }

        if (inButtonPress) {
            putCharXY(1, 0, leftEdgeChar);
            putStringXY(2, 0, mnemonic.getRawLabel(), buttonColor);
            putCharXY(getWidth() - 1, 0, rightEdgeChar);
        } else {
            putCharXY(0, 0, leftEdgeChar);
            putStringXY(1, 0, mnemonic.getRawLabel(), buttonColor);
            putCharXY(getWidth() - 2, 0, rightEdgeChar);

            if (shadowColor != null) {
                putCharXY(1, 1, leftEdgeShadowChar);
                hLineXY(2, 1, getWidth() - 3, shadowCharBottom);
                putCharXY(getWidth() - 1, 0, rightEdgeShadowCharTop);
                putCharXY(getWidth() - 1, 1, rightEdgeShadowCharBottom);
            }
        }
        if (mnemonic.getScreenShortcutIdx() >= 0) {
            if (inButtonPress) {
                putCharXY(2 + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), menuMnemonicColor);
            } else {
                putCharXY(1 + mnemonic.getScreenShortcutIdx(), 0,
                    mnemonic.getShortcut(), menuMnemonicColor);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TButton ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the mnemonic string for this button.
     *
     * @return mnemonic string
     */
    public MnemonicString getMnemonic() {
        return mnemonic;
    }

    /**
     * Act as though the button was pressed.  This is useful for other UI
     * elements to get the same action as if the user clicked the button.
     */
    public void dispatch() {
        if (action != null) {
            action.DO(this);
            if (inButtonPress) {
                rightEdgeShadowCharTop = null;
            }
            inButtonPress = false;
        }
    }

    /**
     * Set the background color used for the button "shadow".  If null, no
     * shadow will be drawn.
     *
     * @param color the new background color, or null for no shadow
     */
    public void setShadowColor(final CellAttributes color) {
        if (color != null) {
            givenShadowColor = new CellAttributes();
            givenShadowColor.setTo(color);
            givenShadowColor.setForeColor(Color.BLACK);
            givenShadowColor.setBold(false);
        } else {
            givenShadowColor = null;
        }
        rightEdgeShadowCharBottom = null;
        shadowColor = null;
    }

    /**
     * Set the button style.
     *
     * @param style SQUARE, ROUND, etc.
     */
    public void setStyle(final Style style) {
        this.style = style;
        leftEdgeChar = null;
        rightEdgeChar = null;
        leftEdgeShadowChar = null;
        rightEdgeShadowCharTop = null;
        rightEdgeShadowCharBottom = null;
        shadowColor = null;
    }

    /**
     * Draw the button ends and populate leftEdgeChar, rightEdgeChar,
     * leftEdgeShadowChar, and rightEdgeShadowChar.
     *
     * @param rectangleColor the background color
     * @param buttonColor the button foreground color
     */
    private void drawEnds(final CellAttributes rectangleColor,
        final CellAttributes buttonColor) {

        if (style == Style.SQUARE) {
            leftEdgeChar = new Cell(buttonColor);
            rightEdgeChar = new Cell(buttonColor);
            leftEdgeShadowChar = new Cell(GraphicsChars.CP437[0xDF],
                shadowColor);
            rightEdgeShadowCharTop = new Cell(GraphicsChars.CP437[0xDC],
                shadowColor);
            rightEdgeShadowCharBottom = new Cell(GraphicsChars.CP437[0xDF],
                shadowColor);
            shadowCharBottom = new Cell(GraphicsChars.CP437[0xDF],
                shadowColor);
            return;
        }
        leftEdgeChar = new Cell(buttonColor);
        rightEdgeChar = new Cell(buttonColor);
        leftEdgeShadowChar = new Cell(shadowColor);
        rightEdgeShadowCharTop = new Cell(shadowColor);
        rightEdgeShadowCharBottom = new Cell(shadowColor);
        shadowCharBottom = new Cell(shadowColor);

        int cellWidth = getScreen().getTextWidth();
        int cellHeight = getScreen().getTextHeight();

        BufferedImage image = new BufferedImage(cellWidth * 2, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        BufferedImage shadowImage = new BufferedImage(cellWidth * 2,
            cellHeight * 2, BufferedImage.TYPE_INT_ARGB);

        java.awt.Color shadowRgb = null;
        if (shadowColor.getForeColorRGB() < 0) {
            shadowRgb = getApplication().getBackend().
                attrToForegroundColor(shadowColor);
        } else {
            shadowRgb = new java.awt.Color(shadowColor.getForeColorRGB());
        }
        java.awt.Color rectangleRgb = null;
        if (rectangleColor.getBackColorRGB() < 0) {
            rectangleRgb = getApplication().getBackend().
                attrToBackgroundColor(rectangleColor);
        } else {
            rectangleRgb = new java.awt.Color(rectangleColor.getBackColorRGB());
        }

        java.awt.Color buttonRgb = null;
        if (buttonColor.getBackColorRGB() < 0) {
            buttonRgb = getApplication().getBackend().
                attrToBackgroundColor(buttonColor);
        } else {
            buttonRgb = new java.awt.Color(buttonColor.getBackColorRGB());
        }

        // Draw the shadow first, so that it be underneath the right edge.
        Graphics2D gr2s = shadowImage.createGraphics();
        gr2s.setColor(rectangleRgb);
        gr2s.fillRect(0, 0, cellWidth * 2, cellHeight * 2);
        gr2s.setColor(shadowRgb);

        int [] xPoints;
        int [] yPoints;
        switch (style) {
        case ROUND:
            gr2s.fillOval(0, cellHeight / 2, cellWidth * 2, cellHeight);
            break;
        case DIAMOND:
            xPoints = new int[4];
            yPoints = new int[4];
            xPoints[0] = 0;
            xPoints[1] = cellWidth;
            xPoints[2] = 2 * cellWidth;
            xPoints[3] = cellWidth;
            yPoints[0] = cellHeight;
            yPoints[1] = cellHeight / 2;
            yPoints[2] = cellHeight;
            yPoints[3] = cellHeight + cellHeight / 2;
            gr2s.fillPolygon(xPoints, yPoints, 4);
            break;
        case ARROW_LEFT:
            xPoints = new int[6];
            yPoints = new int[6];
            xPoints[0] = 0;
            xPoints[1] = cellWidth;
            xPoints[2] = 2 * cellWidth;
            xPoints[3] = cellWidth;
            xPoints[4] = 2 * cellWidth;
            xPoints[5] = cellWidth;
            yPoints[0] = cellHeight;
            yPoints[1] = cellHeight / 2;
            yPoints[2] = cellHeight / 2;
            yPoints[3] = cellHeight;
            yPoints[4] = cellHeight + cellHeight / 2;
            yPoints[5] = cellHeight + cellHeight / 2;
            gr2s.fillPolygon(xPoints, yPoints, 6);
            break;
        case ARROW_RIGHT:
            xPoints = new int[6];
            yPoints = new int[6];
            xPoints[0] = 2 * cellWidth;
            xPoints[1] = cellWidth;
            xPoints[2] = 0;
            xPoints[3] = cellWidth;
            xPoints[4] = 0;
            xPoints[5] = cellWidth;
            yPoints[0] = cellHeight;
            yPoints[1] = cellHeight / 2;
            yPoints[2] = cellHeight / 2;
            yPoints[3] = cellHeight;
            yPoints[4] = cellHeight + cellHeight / 2;
            yPoints[5] = cellHeight + cellHeight / 2;
            gr2s.fillPolygon(xPoints, yPoints, 6);
            break;
        case SQUARE:
            // Not possible.
            return;
        }
        gr2s.dispose();
        // gr2s now has the shadow bits, shifted half a cell down from 0.

        Graphics2D gr2 = image.createGraphics();
        gr2.setColor(rectangleRgb);
        gr2.fillRect(0, 0, cellWidth * 2, cellHeight);
        if (!inButtonPress) {
            gr2.setColor(shadowRgb);
            gr2.fillRect(cellWidth, cellHeight / 2, cellWidth,
                cellHeight - (cellHeight / 2));
        }
        gr2.setColor(buttonRgb);
        switch (style) {
        case ROUND:
            gr2.fillOval(0, 0, cellWidth * 2, cellHeight);
            break;
        case DIAMOND:
            xPoints = new int[4];
            yPoints = new int[4];
            xPoints[0] = 0;
            xPoints[1] = cellWidth;
            xPoints[2] = 2 * cellWidth;
            xPoints[3] = cellWidth;
            yPoints[0] = cellHeight / 2;
            yPoints[1] = 0;
            yPoints[2] = cellHeight / 2;
            yPoints[3] = cellHeight;
            gr2.fillPolygon(xPoints, yPoints, 4);
            break;
        case ARROW_LEFT:
            xPoints = new int[6];
            yPoints = new int[6];
            xPoints[0] = 0;
            xPoints[1] = cellWidth;
            xPoints[2] = 2 * cellWidth;
            xPoints[3] = cellWidth;
            xPoints[4] = 2 * cellWidth;
            xPoints[5] = cellWidth;
            yPoints[0] = cellHeight / 2;
            yPoints[1] = 0;
            yPoints[2] = 0;
            yPoints[3] = cellHeight / 2;
            yPoints[4] = cellHeight;
            yPoints[5] = cellHeight;
            gr2.fillPolygon(xPoints, yPoints, 6);
            break;
        case ARROW_RIGHT:
            xPoints = new int[6];
            yPoints = new int[6];
            xPoints[0] = 2 * cellWidth;
            xPoints[1] = cellWidth;
            xPoints[2] = 0;
            xPoints[3] = cellWidth;
            xPoints[4] = 0;
            xPoints[5] = cellWidth;
            yPoints[0] = cellHeight / 2;
            yPoints[1] = 0;
            yPoints[2] = 0;
            yPoints[3] = cellHeight / 2;
            yPoints[4] = cellHeight;
            yPoints[5] = cellHeight;
            gr2.fillPolygon(xPoints, yPoints, 6);
            break;
        case SQUARE:
            // Not possible.
            return;
        }
        gr2.dispose();
        // gr2 now has the foreground ends, on both halves.

        int imageId = System.identityHashCode(this);
        imageId ^= (int) System.currentTimeMillis();

        // Left edge: left half of image
        BufferedImage cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        gr2 = cellImage.createGraphics();
        gr2.drawImage(image.getSubimage(0, 0, cellWidth, cellHeight),
            0, 0, null);
        gr2.dispose();
        imageId++;
        leftEdgeChar.setImage(cellImage, imageId & 0x7FFFFFFF);
        leftEdgeChar.setOpaqueImage();

        // Right edge: left half of image
        cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        gr2 = cellImage.createGraphics();
        gr2.drawImage(image.getSubimage(cellWidth, 0, cellWidth, cellHeight),
            0, 0, null);
        gr2.dispose();
        imageId++;
        rightEdgeChar.setImage(cellImage, imageId & 0x7FFFFFFF);
        rightEdgeChar.setOpaqueImage();

        // Left shadow edge: bottom-left half of shadowImage
        cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        gr2s = cellImage.createGraphics();
        gr2s.drawImage(shadowImage.getSubimage(0, cellHeight,
                cellWidth, cellHeight), 0, 0, null);
        gr2s.dispose();
        imageId++;
        leftEdgeShadowChar.setImage(cellImage, imageId & 0x7FFFFFFF);
        leftEdgeShadowChar.setOpaqueImage();

        // Right shadow edge top: top-right half of shadowImage
        cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        gr2s = cellImage.createGraphics();
        gr2s.drawImage(shadowImage.getSubimage(cellWidth, 0,
                cellWidth, cellHeight), 0, 0, null);
        gr2s.dispose();
        imageId++;
        rightEdgeShadowCharTop.setImage(cellImage, imageId & 0x7FFFFFFF);
        rightEdgeShadowCharTop.setOpaqueImage();

        // Right shadow edge bottom: bottom-right half of shadowImage
        cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        gr2s = cellImage.createGraphics();
        gr2s.drawImage(shadowImage.getSubimage(cellWidth, cellHeight,
                cellWidth, cellHeight), 0, 0, null);
        gr2s.dispose();
        imageId++;
        rightEdgeShadowCharBottom.setImage(cellImage, imageId & 0x7FFFFFFF);
        rightEdgeShadowCharBottom.setOpaqueImage();

        cellImage = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        gr2s = cellImage.createGraphics();
        gr2s.setColor(rectangleRgb);
        gr2s.fillRect(0, 0, cellWidth, cellHeight);
        gr2s.setColor(shadowRgb);
        gr2s.fillRect(0, 0, cellWidth, cellHeight / 2);
        gr2s.dispose();
        imageId++;
        shadowCharBottom.setImage(cellImage, imageId & 0x7FFFFFFF);
        shadowCharBottom.setOpaqueImage();

    }

    /**
     * Set the button style.
     *
     * @param buttonStyle the button style string, one of: "square", "round",
     * "diamond", "leftArrow", or "rightArrow"; or null to use the value from
     * jexer.TButton.style.
     */
    public void setStyle(final String buttonStyle) {
        String styleString = System.getProperty("jexer.TButton.style",
            "square");
        if (buttonStyle != null) {
            styleString = buttonStyle.toLowerCase();
        }
        if (styleString.equals("square")) {
            style = Style.SQUARE;
        } else if (styleString.equals("round")) {
            style = Style.ROUND;
        } else if (styleString.equals("diamond")) {
            style = Style.DIAMOND;
        } else if (styleString.equals("arrowleft")
            || styleString.equals("leftarrow")
        ) {
            style = Style.ARROW_LEFT;
        } else if (styleString.equals("arrowright")
            || styleString.equals("rightarrow")
        ) {
            style = Style.ARROW_RIGHT;
        } else {
            style = Style.SQUARE;
        }
    }


}
