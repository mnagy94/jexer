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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import jexer.TScrollableWidget;
import jexer.backend.ECMA48Terminal;
import jexer.backend.GlyphMaker;
import jexer.backend.SwingTerminal;
import jexer.bits.Cell;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.tterminal.DisplayLine;
import jexer.tterminal.DisplayListener;
import jexer.tterminal.ECMA48;
import static jexer.TKeypress.*;


/**
 * TTextPicture displays a color-and-text canvas, also called "ANSI Art" or
 * "ASCII Art".
 */
public class TTextPicture extends TScrollableWidget
                          implements DisplayListener {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The terminal containing the display.
     */
    private ECMA48 terminal;

    /**
     * If true, the terminal is not reading and is closed.
     */
    private boolean terminalClosed = true;

    /**
     * Double-height font.
     */
    private GlyphMaker doubleFont;

    /**
     * Last text width value.
     */
    private int lastTextWidth = -1;

    /**
     * Last text height value.
     */
    private int lastTextHeight = -1;

    /**
     * The blink state, used only by ECMA48 backend and when double-width
     * chars must be drawn.
     */
    private boolean blinkState = true;

    /**
     * Timer, used only by ECMA48 backend and when double-width chars must be
     * drawn.
     */
    private TTimer blinkTimer = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param filename the file containing the picture data
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    public TTextPicture(final TWidget parent, final String filename,
        final int x, final int y, final int width, final int height) {

        // Set parent and window
        super(parent, x, y, width, height);

        try {
            terminal = new ECMA48(ECMA48.DeviceType.XTERM,
                new FileInputStream(filename), new ByteArrayOutputStream(),
                this, getApplication().getBackend());

            terminalClosed = false;
        } catch (FileNotFoundException e) {
            // SQUASH
            terminal = null;
        } catch (UnsupportedEncodingException e) {
            // SQUASH
            terminal = null;
        }

        // We will have scrollers for data fields and mouse event handling,
        // but do not want to draw it.
        vScroller = new TVScroller(null, getWidth(), 0, getHeight());
        vScroller.setVisible(false);
        setBottomValue(0);
        hScroller = new THScroller(null, 0, getHeight() - 1,
            Math.max(1, getWidth() - 1));
        hScroller.setVisible(false);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        // Let TWidget set my size.
        super.onResize(resize);

        if (terminal == null) {
            return;
        }

        synchronized (terminal) {
            if (resize.getType() == TResizeEvent.Type.WIDGET) {
                // Resize the scroll bars
                reflowData();
                placeScrollbars();

                // Get out of scrollback
                setVerticalValue(0);

                terminal.setWidth(getWidth());
                terminal.setHeight(getHeight());
            }
        } // synchronized (emulator)
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        // Scrollback up/down/home/end
        if (keypress.equals(kbShiftHome)
            || keypress.equals(kbCtrlHome)
            || keypress.equals(kbAltHome)
        ) {
            toTop();
            return;
        }
        if (keypress.equals(kbShiftEnd)
            || keypress.equals(kbCtrlEnd)
            || keypress.equals(kbAltEnd)
        ) {
            toBottom();
            return;
        }
        if (keypress.equals(kbShiftPgUp)
            || keypress.equals(kbCtrlPgUp)
            || keypress.equals(kbAltPgUp)
        ) {
            bigVerticalDecrement();
            return;
        }
        if (keypress.equals(kbShiftPgDn)
            || keypress.equals(kbCtrlPgDn)
            || keypress.equals(kbAltPgDn)
        ) {
            bigVerticalIncrement();
            return;
        }

        super.onKeypress(keypress);
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouseWheelUp()) {
            verticalDecrement();
            return;
        }
        if (mouse.isMouseWheelDown()) {
            verticalIncrement();
            return;
        }
        super.onMouseDown(mouse);
    }

    // ------------------------------------------------------------------------
    // TScrollableWidget ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle widget close.
     */
    @Override
    public void close() {
        if (terminal != null) {
            terminal.close();
        }
        if (blinkTimer != null) {
            TApplication app = getApplication();
            if (app != null) {
                app.removeTimer(blinkTimer);
            }
        }
    }

    /**
     * Resize scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        if (terminal == null) {
            return;
        }

        // Vertical scrollbar
        setTopValue(getHeight()
            - (terminal.getScrollbackBuffer().size()
                + terminal.getDisplayBuffer().size()));
        setVerticalBigChange(getHeight());
    }

    // ------------------------------------------------------------------------
    // DisplayListener --------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Called by emulator when fresh data has come in (request poll).
     *
     * @param cursorOnly if true, the screen has not changed but the cursor
     * may be on a different location.
     */
    public void displayChanged(final boolean cursorOnly) {
        // Do nothing
    }

    /**
     * Called by emulator when fresh data has come in (push).
     *
     * @param display the updated display
     */
    public void updateDisplay(final List<DisplayLine> display) {
        // Do nothing
    }

    /**
     * Function to call to obtain the number of rows from the bottom to
     * scroll back when sending updates via updateDisplay().
     *
     * @return the number of rows from the bottom to scroll back
     */
    public int getScrollBottom() {
        return -getVerticalValue();
    }

    /**
     * Function to call to obtain the display width.
     *
     * @return the number of columns in the display
     */
    public int getDisplayWidth() {
        return getWidth();
    }

    /**
     * Function to call to obtain the display height.
     *
     * @return the number of rows in the display
     */
    public int getDisplayHeight() {
        return getHeight();
    }

    // ------------------------------------------------------------------------
    // TTextPicture -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
        if (terminal == null) {
            return;
        }

        // Check to see if the shell has died.
        if (!terminalClosed && !terminal.isReading()) {
            try {
                terminal.close();
                terminalClosed = true;
            } catch (IllegalThreadStateException e) {
                // SQUASH
            }
        }

        int width = 80;
        int left = 0;
        List<DisplayLine> display = null;

        synchronized (terminal) {
            // Update the scroll bars
            reflowData();

            display = terminal.getVisibleDisplay(getHeight(),
                -getVerticalValue());
            assert (display.size() == getHeight());
            width = terminal.getWidth();
            left = getHorizontalValue();
        } // synchronized (terminal)

        int row = 0;
        for (DisplayLine line: display) {
            int widthMax = width;
            if (line.isDoubleWidth()) {
                widthMax /= 2;
            }
            if (widthMax > getWidth()) {
                widthMax = getWidth();
            }
            for (int i = 0; i < widthMax; i++) {
                Cell ch = line.charAt(i + left);

                if (ch.isImage()) {
                    putCharXY(i, row, ch);
                    continue;
                }

                Cell newCell = new Cell(ch);
                boolean reverse = line.isReverseColor() ^ ch.isReverse();
                newCell.setReverse(false);
                if (reverse) {
                    if (ch.getForeColorRGB() < 0) {
                        newCell.setBackColor(ch.getForeColor());
                    } else {
                        newCell.setBackColorRGB(ch.getForeColorRGB());
                    }
                    if (ch.getBackColorRGB() < 0) {
                        newCell.setForeColor(ch.getBackColor());
                    } else {
                        newCell.setForeColorRGB(ch.getBackColorRGB());
                    }
                }
                if (line.isDoubleWidth()) {
                    putDoubleWidthCharXY(line, (i * 2), row, newCell);
                } else {
                    putCharXY(i, row, newCell);
                }
            }
            row++;
        }
    }

    /**
     * Draw glyphs for a double-width or double-height VT100 cell to two
     * screen cells.
     *
     * @param line the line this VT100 cell is in
     * @param x the X position to draw the left half to
     * @param y the Y position to draw to
     * @param cell the cell to draw
     */
    private void putDoubleWidthCharXY(final DisplayLine line, final int x,
        final int y, final Cell cell) {

        int textWidth = getScreen().getTextWidth();
        int textHeight = getScreen().getTextHeight();
        boolean cursorBlinkVisible = true;

        if (getScreen() instanceof SwingTerminal) {
            SwingTerminal terminal = (SwingTerminal) getScreen();
            cursorBlinkVisible = terminal.getCursorBlinkVisible();
        } else if (getScreen() instanceof ECMA48Terminal) {
            ECMA48Terminal terminal = (ECMA48Terminal) getScreen();

            if (!terminal.hasSixel()
                && !terminal.hasJexerImages()
                && !terminal.hasIterm2Images()
            ) {
                // The backend does not have images support, draw this as
                // text and bail out.
                putCharXY(x, y, cell);
                putCharXY(x + 1, y, ' ', cell);
                return;
            }
            cursorBlinkVisible = blinkState;
        } else {
            // We don't know how to dray glyphs to this screen, draw them as
            // text and bail out.
            putCharXY(x, y, cell);
            putCharXY(x + 1, y, ' ', cell);
            return;
        }

        if ((textWidth != lastTextWidth) || (textHeight != lastTextHeight)) {
            // Screen size has changed, reset the font.
            setupFont(textHeight);
            lastTextWidth = textWidth;
            lastTextHeight = textHeight;
        }
        assert (doubleFont != null);

        BufferedImage image;
        if (line.getDoubleHeight() == 1) {
            // Double-height top half: don't draw the underline.
            Cell newCell = new Cell(cell);
            newCell.setUnderline(false);
            image = doubleFont.getImage(newCell, textWidth * 2, textHeight * 2,
                getApplication().getBackend(), cursorBlinkVisible);
        } else {
            image = doubleFont.getImage(cell,  textWidth * 2, textHeight * 2,
                getApplication().getBackend(), cursorBlinkVisible);
        }

        // Now that we have the double-wide glyph drawn, copy the right
        // pieces of it to the cells.
        Cell left = new Cell(cell);
        Cell right = new Cell(cell);
        right.setChar(' ');
        BufferedImage leftImage = null;
        BufferedImage rightImage = null;
        /*
        System.err.println("image " + image + " textWidth " + textWidth +
            " textHeight " + textHeight);
         */

        switch (line.getDoubleHeight()) {
        case 1:
            // Top half double height
            leftImage = image.getSubimage(0, 0, textWidth, textHeight);
            rightImage = image.getSubimage(textWidth, 0, textWidth, textHeight);
            break;
        case 2:
            // Bottom half double height
            leftImage = image.getSubimage(0, textHeight, textWidth, textHeight);
            rightImage = image.getSubimage(textWidth, textHeight,
                textWidth, textHeight);
            break;
        default:
            // Either single height double-width, or error fallback
            BufferedImage wideImage = new BufferedImage(textWidth * 2,
                textHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D grWide = wideImage.createGraphics();
            grWide.drawImage(image, 0, 0, wideImage.getWidth(),
                wideImage.getHeight(), null);
            grWide.dispose();
            leftImage = wideImage.getSubimage(0, 0, textWidth, textHeight);
            rightImage = wideImage.getSubimage(textWidth, 0, textWidth,
                textHeight);
            break;
        }
        left.setImage(leftImage);
        right.setImage(rightImage);
        // Since we have image data, ditch the character here.  Otherwise, a
        // drawBoxShadow() over the terminal window will show the characters
        // which looks wrong.
        left.setChar(' ');
        right.setChar(' ');
        putCharXY(x, y, left);
        putCharXY(x + 1, y, right);
    }

    /**
     * Set up the double-width font.
     *
     * @param fontSize the size of font to request for the single-width font.
     * The double-width font will be 2x this value.
     */
    private void setupFont(final int fontSize) {
        doubleFont = GlyphMaker.getInstance(fontSize * 2);

        // Special case: the ECMA48 backend needs to have a timer to drive
        // its blink state.
        if (getScreen() instanceof jexer.backend.ECMA48Terminal) {
            if (blinkTimer == null) {
                // Blink every 500 millis.
                long millis = 500;
                blinkTimer = getApplication().addTimer(millis, true,
                    new TAction() {
                        public void DO() {
                            blinkState = !blinkState;
                            TApplication app = getApplication();
                            if (app != null) {
                                app.doRepaint();
                            }
                        }
                    }
                );
            }
        }
    }

}
