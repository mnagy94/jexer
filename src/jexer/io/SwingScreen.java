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
package jexer.io;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jexer.bits.Cell;
import jexer.bits.CellAttributes;
import jexer.session.SwingSessionInfo;

/**
 * This Screen implementation draws to a Java Swing JFrame.
 */
public final class SwingScreen extends Screen {

    /**
     * If true, use triple buffering thread.
     */
    private static boolean tripleBuffer = true;

    /**
     * Cursor style to draw.
     */
    public enum CursorStyle {
        /**
         * Use an underscore for the cursor.
         */
        UNDERLINE,

        /**
         * Use a solid block for the cursor.
         */
        BLOCK,

        /**
         * Use an outlined block for the cursor.
         */
        OUTLINE
    }

    private static Color MYBLACK;
    private static Color MYRED;
    private static Color MYGREEN;
    private static Color MYYELLOW;
    private static Color MYBLUE;
    private static Color MYMAGENTA;
    private static Color MYCYAN;
    private static Color MYWHITE;

    private static Color MYBOLD_BLACK;
    private static Color MYBOLD_RED;
    private static Color MYBOLD_GREEN;
    private static Color MYBOLD_YELLOW;
    private static Color MYBOLD_BLUE;
    private static Color MYBOLD_MAGENTA;
    private static Color MYBOLD_CYAN;
    private static Color MYBOLD_WHITE;

    private static boolean dosColors = false;

    /**
     * Setup Swing colors to match DOS color palette.
     */
    private static void setDOSColors() {
        if (dosColors) {
            return;
        }
        MYBLACK        = new Color(0x00, 0x00, 0x00);
        MYRED          = new Color(0xa8, 0x00, 0x00);
        MYGREEN        = new Color(0x00, 0xa8, 0x00);
        MYYELLOW       = new Color(0xa8, 0x54, 0x00);
        MYBLUE         = new Color(0x00, 0x00, 0xa8);
        MYMAGENTA      = new Color(0xa8, 0x00, 0xa8);
        MYCYAN         = new Color(0x00, 0xa8, 0xa8);
        MYWHITE        = new Color(0xa8, 0xa8, 0xa8);
        MYBOLD_BLACK   = new Color(0x54, 0x54, 0x54);
        MYBOLD_RED     = new Color(0xfc, 0x54, 0x54);
        MYBOLD_GREEN   = new Color(0x54, 0xfc, 0x54);
        MYBOLD_YELLOW  = new Color(0xfc, 0xfc, 0x54);
        MYBOLD_BLUE    = new Color(0x54, 0x54, 0xfc);
        MYBOLD_MAGENTA = new Color(0xfc, 0x54, 0xfc);
        MYBOLD_CYAN    = new Color(0x54, 0xfc, 0xfc);
        MYBOLD_WHITE   = new Color(0xfc, 0xfc, 0xfc);

        dosColors = true;
    }

    /**
     * SwingFrame is our top-level hook into the Swing system.
     */
    class SwingFrame extends JFrame {

        /**
         * Serializable version.
         */
        private static final long serialVersionUID = 1;

        /**
         * The terminus font resource filename.
         */
        private static final String FONTFILE = "terminus-ttf-4.39/TerminusTTF-Bold-4.39.ttf";

        /**
         * The BufferStrategy object needed for triple-buffering.
         */
        private BufferStrategy bufferStrategy;

        /**
         * A cache of previously-rendered glyphs for blinking text, when it
         * is not visible.
         */
        private HashMap<Cell, BufferedImage> glyphCacheBlink;

        /**
         * A cache of previously-rendered glyphs for non-blinking, or
         * blinking-and-visible, text.
         */
        private HashMap<Cell, BufferedImage> glyphCache;

        /**
         * The TUI Screen data.
         */
        SwingScreen screen;

        /**
         * If true, we were successful getting Terminus.
         */
        private boolean gotTerminus = false;

        /**
         * Width of a character cell.
         */
        private int textWidth = 1;

        /**
         * Height of a character cell.
         */
        private int textHeight = 1;

        /**
         * Descent of a character cell.
         */
        private int maxDescent = 0;

        /**
         * System-dependent Y adjustment for text in the  character cell.
         */
        private int textAdjustY = 0;

        /**
         * System-dependent X adjustment for text in the  character cell.
         */
        private int textAdjustX = 0;

        /**
         * Top pixel absolute location.
         */
        private int top = 30;

        /**
         * Left pixel absolute location.
         */
        private int left = 30;

        /**
         * The cursor style to draw.
         */
        private CursorStyle cursorStyle = CursorStyle.UNDERLINE;

        /**
         * The number of millis to wait before switching the blink from
         * visible to invisible.
         */
        private long blinkMillis = 500;

        /**
         * If true, the cursor should be visible right now based on the blink
         * time.
         */
        private boolean cursorBlinkVisible = true;

        /**
         * The time that the blink last flipped from visible to invisible or
         * from invisible to visible.
         */
        private long lastBlinkTime = 0;

        /**
         * Convert a CellAttributes foreground color to an Swing Color.
         *
         * @param attr the text attributes
         * @return the Swing Color
         */
        private Color attrToForegroundColor(final CellAttributes attr) {
            if (attr.isBold()) {
                if (attr.getForeColor().equals(jexer.bits.Color.BLACK)) {
                    return MYBOLD_BLACK;
                } else if (attr.getForeColor().equals(jexer.bits.Color.RED)) {
                    return MYBOLD_RED;
                } else if (attr.getForeColor().equals(jexer.bits.Color.BLUE)) {
                    return MYBOLD_BLUE;
                } else if (attr.getForeColor().equals(jexer.bits.Color.GREEN)) {
                    return MYBOLD_GREEN;
                } else if (attr.getForeColor().equals(jexer.bits.Color.YELLOW)) {
                    return MYBOLD_YELLOW;
                } else if (attr.getForeColor().equals(jexer.bits.Color.CYAN)) {
                    return MYBOLD_CYAN;
                } else if (attr.getForeColor().equals(jexer.bits.Color.MAGENTA)) {
                    return MYBOLD_MAGENTA;
                } else if (attr.getForeColor().equals(jexer.bits.Color.WHITE)) {
                    return MYBOLD_WHITE;
                }
            } else {
                if (attr.getForeColor().equals(jexer.bits.Color.BLACK)) {
                    return MYBLACK;
                } else if (attr.getForeColor().equals(jexer.bits.Color.RED)) {
                    return MYRED;
                } else if (attr.getForeColor().equals(jexer.bits.Color.BLUE)) {
                    return MYBLUE;
                } else if (attr.getForeColor().equals(jexer.bits.Color.GREEN)) {
                    return MYGREEN;
                } else if (attr.getForeColor().equals(jexer.bits.Color.YELLOW)) {
                    return MYYELLOW;
                } else if (attr.getForeColor().equals(jexer.bits.Color.CYAN)) {
                    return MYCYAN;
                } else if (attr.getForeColor().equals(jexer.bits.Color.MAGENTA)) {
                    return MYMAGENTA;
                } else if (attr.getForeColor().equals(jexer.bits.Color.WHITE)) {
                    return MYWHITE;
                }
            }
            throw new IllegalArgumentException("Invalid color: " +
                attr.getForeColor().getValue());
        }

        /**
         * Convert a CellAttributes background color to an Swing Color.
         *
         * @param attr the text attributes
         * @return the Swing Color
         */
        private Color attrToBackgroundColor(final CellAttributes attr) {
            if (attr.getBackColor().equals(jexer.bits.Color.BLACK)) {
                return MYBLACK;
            } else if (attr.getBackColor().equals(jexer.bits.Color.RED)) {
                return MYRED;
            } else if (attr.getBackColor().equals(jexer.bits.Color.BLUE)) {
                return MYBLUE;
            } else if (attr.getBackColor().equals(jexer.bits.Color.GREEN)) {
                return MYGREEN;
            } else if (attr.getBackColor().equals(jexer.bits.Color.YELLOW)) {
                return MYYELLOW;
            } else if (attr.getBackColor().equals(jexer.bits.Color.CYAN)) {
                return MYCYAN;
            } else if (attr.getBackColor().equals(jexer.bits.Color.MAGENTA)) {
                return MYMAGENTA;
            } else if (attr.getBackColor().equals(jexer.bits.Color.WHITE)) {
                return MYWHITE;
            }
            throw new IllegalArgumentException("Invalid color: " +
                attr.getBackColor().getValue());
        }

        /**
         * Public constructor.
         *
         * @param screen the Screen that Backend talks to
         * @param fontSize the size in points.  Good values to pick are: 16,
         * 20, 22, and 24.
         */
        public SwingFrame(final SwingScreen screen, final int fontSize) {
            this.screen = screen;
            setDOSColors();

            // Figure out my cursor style
            String cursorStyleString = System.getProperty(
                "jexer.Swing.cursorStyle", "underline").toLowerCase();

            if (cursorStyleString.equals("underline")) {
                cursorStyle = CursorStyle.UNDERLINE;
            } else if (cursorStyleString.equals("outline")) {
                cursorStyle = CursorStyle.OUTLINE;
            } else if (cursorStyleString.equals("block")) {
                cursorStyle = CursorStyle.BLOCK;
            }

            if (System.getProperty("jexer.Swing.tripleBuffer") != null) {
                if (System.getProperty("jexer.Swing.tripleBuffer").
                    equals("false")) {

                    SwingScreen.tripleBuffer = false;
                }
            }

            setTitle("Jexer Application");
            setBackground(Color.black);

            try {
                // Always try to use Terminus, the one decent font.
                ClassLoader loader = Thread.currentThread().
                        getContextClassLoader();
                InputStream in = loader.getResourceAsStream(FONTFILE);
                Font terminusRoot = Font.createFont(Font.TRUETYPE_FONT, in);
                Font terminus = terminusRoot.deriveFont(Font.PLAIN, fontSize);
                setFont(terminus);
                gotTerminus = true;
            } catch (Exception e) {
                e.printStackTrace();
                // setFont(new Font("Liberation Mono", Font.PLAIN, 24));
                setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
            }
            pack();

            // Kill the X11 cursor
            // Transparent 16 x 16 pixel cursor image.
            BufferedImage cursorImg = new BufferedImage(16, 16,
                BufferedImage.TYPE_INT_ARGB);
            // Create a new blank cursor.
            Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
            setCursor(blankCursor);

            // Be capable of seeing Tab / Shift-Tab
            setFocusTraversalKeysEnabled(false);

            // Save the text cell width/height
            getFontDimensions();

            // Cache glyphs as they are rendered
            glyphCacheBlink = new HashMap<Cell, BufferedImage>();
            glyphCache = new HashMap<Cell, BufferedImage>();

            // Setup triple-buffering
            if (SwingScreen.tripleBuffer) {
                setIgnoreRepaint(true);
                createBufferStrategy(3);
                bufferStrategy = getBufferStrategy();
            }
        }

        /**
         * Figure out what textAdjustX and textAdjustY should be, based on
         * the location of a vertical bar (to find textAdjustY) and a
         * horizontal bar (to find textAdjustX).
         *
         * @return true if textAdjustX and textAdjustY were guessed at
         * correctly
         */
        private boolean getFontAdjustments() {
            BufferedImage image = null;

            // What SHOULD happen is that the topmost/leftmost white pixel is
            // at position (gr2x, gr2y).  But it might also be off by a pixel
            // in either direction.

            Graphics2D gr2 = null;
            int gr2x = 3;
            int gr2y = 3;
            image = new BufferedImage(textWidth * 2, textHeight * 2,
                BufferedImage.TYPE_INT_ARGB);

            gr2 = image.createGraphics();
            gr2.setFont(getFont());
            gr2.setColor(java.awt.Color.BLACK);
            gr2.fillRect(0, 0, textWidth * 2, textHeight * 2);
            gr2.setColor(java.awt.Color.WHITE);
            char [] chars = new char[1];
            chars[0] = jexer.bits.GraphicsChars.VERTICAL_BAR;
            gr2.drawChars(chars, 0, 1, gr2x, gr2y + textHeight - maxDescent);
            gr2.dispose();

            for (int x = 0; x < textWidth; x++) {
                for (int y = 0; y < textHeight; y++) {

                    /*
                    System.err.println("X: " + x + " Y: " + y + " " +
                        image.getRGB(x, y));
                     */

                    if ((image.getRGB(x, y) & 0xFFFFFF) != 0) {
                        textAdjustY = (gr2y - y);

                        // System.err.println("textAdjustY: " + textAdjustY);
                        x = textWidth;
                        break;
                    }
                }
            }

            gr2 = image.createGraphics();
            gr2.setFont(getFont());
            gr2.setColor(java.awt.Color.BLACK);
            gr2.fillRect(0, 0, textWidth * 2, textHeight * 2);
            gr2.setColor(java.awt.Color.WHITE);
            chars[0] = jexer.bits.GraphicsChars.SINGLE_BAR;
            gr2.drawChars(chars, 0, 1, gr2x, gr2y + textHeight - maxDescent);
            gr2.dispose();

            for (int x = 0; x < textWidth; x++) {
                for (int y = 0; y < textHeight; y++) {

                    /*
                    System.err.println("X: " + x + " Y: " + y + " " +
                        image.getRGB(x, y));
                     */

                    if ((image.getRGB(x, y) & 0xFFFFFF) != 0) {
                        textAdjustX = (gr2x - x);

                        // System.err.println("textAdjustX: " + textAdjustX);
                        return true;
                    }
                }
            }

            // Something weird happened, don't rely on this function.
            // System.err.println("getFontAdjustments: false");
            return false;
        }


        /**
         * Figure out my font dimensions.
         */
        private void getFontDimensions() {
            Graphics gr = getGraphics();
            FontMetrics fm = gr.getFontMetrics();
            maxDescent = fm.getMaxDescent();
            Rectangle2D bounds = fm.getMaxCharBounds(gr);
            int leading = fm.getLeading();
            textWidth = (int)Math.round(bounds.getWidth());
            // textHeight = (int)Math.round(bounds.getHeight()) - maxDescent;

            // This produces the same number, but works better for ugly
            // monospace.
            textHeight = fm.getMaxAscent() + maxDescent - leading;

            if (gotTerminus == true) {
                textHeight++;
            }

            if (getFontAdjustments() == false) {
                // We were unable to programmatically determine textAdjustX
                // and textAdjustY, so try some guesses based on VM vendor.
                String runtime = System.getProperty("java.runtime.name");
                if ((runtime != null) && (runtime.contains("Java(TM)"))) {
                    textAdjustY = -1;
                    textAdjustX = 0;
                }
            }
        }

        /**
         * Resize to font dimensions.
         */
        public void resizeToScreen() {
            // Figure out the thickness of borders and use that to set the
            // final size.
            Insets insets = getInsets();
            left = insets.left;
            top = insets.top;

            setSize(textWidth * screen.width + insets.left + insets.right,
                textHeight * screen.height + insets.top + insets.bottom);
        }

        /**
         * Update redraws the whole screen.
         *
         * @param gr the Swing Graphics context
         */
        @Override
        public void update(final Graphics gr) {
            // The default update clears the area.  Don't do that, instead
            // just paint it directly.
            paint(gr);
        }

        /**
         * Draw one glyph to the screen.
         *
         * @param gr the Swing Graphics context
         * @param cell the Cell to draw
         * @param xPixel the x-coordinate to render to.  0 means the
         * left-most pixel column.
         * @param yPixel the y-coordinate to render to.  0 means the top-most
         * pixel row.
         */
        private void drawGlyph(final Graphics gr, final Cell cell,
            final int xPixel, final int yPixel) {

            BufferedImage image = null;
            if (cell.isBlink() && !cursorBlinkVisible) {
                image = glyphCacheBlink.get(cell);
            } else {
                image = glyphCache.get(cell);
            }
            if (image != null) {
                gr.drawImage(image, xPixel, yPixel, this);
                return;
            }

            // Generate glyph and draw it.
            Graphics2D gr2 = null;
            int gr2x = xPixel;
            int gr2y = yPixel;
            if (tripleBuffer) {
                image = new BufferedImage(textWidth, textHeight,
                    BufferedImage.TYPE_INT_ARGB);
                gr2 = image.createGraphics();
                gr2.setFont(getFont());
                gr2x = 0;
                gr2y = 0;
            } else {
                gr2 = (Graphics2D) gr;
            }

            Cell cellColor = new Cell();
            cellColor.setTo(cell);

            // Check for reverse
            if (cell.isReverse()) {
                cellColor.setForeColor(cell.getBackColor());
                cellColor.setBackColor(cell.getForeColor());
            }

            // Draw the background rectangle, then the foreground character.
            gr2.setColor(attrToBackgroundColor(cellColor));
            gr2.fillRect(gr2x, gr2y, textWidth, textHeight);

            // Handle blink and underline
            if (!cell.isBlink()
                || (cell.isBlink() && cursorBlinkVisible)
            ) {
                gr2.setColor(attrToForegroundColor(cellColor));
                char [] chars = new char[1];
                chars[0] = cell.getChar();
                gr2.drawChars(chars, 0, 1, gr2x + textAdjustX,
                    gr2y + textHeight - maxDescent + textAdjustY);

                if (cell.isUnderline()) {
                    gr2.fillRect(gr2x, gr2y + textHeight - 2, textWidth, 2);
                }
            }

            if (tripleBuffer) {
                gr2.dispose();

                // We need a new key that will not be mutated by
                // invertCell().
                Cell key = new Cell();
                key.setTo(cell);
                if (cell.isBlink() && !cursorBlinkVisible) {
                    glyphCacheBlink.put(key, image);
                } else {
                    glyphCache.put(key, image);
                }

                gr.drawImage(image, xPixel, yPixel, this);
            }

        }

        /**
         * Check if the cursor is visible, and if so draw it.
         *
         * @param gr the Swing Graphics context
         */
        private void drawCursor(final Graphics gr) {

            if (cursorVisible
                && (cursorY <= screen.height - 1)
                && (cursorX <= screen.width - 1)
                && cursorBlinkVisible
            ) {
                int xPixel = cursorX * textWidth + left;
                int yPixel = cursorY * textHeight + top;
                Cell lCell = screen.logical[cursorX][cursorY];
                gr.setColor(attrToForegroundColor(lCell));
                switch (cursorStyle) {
                default:
                    // Fall through...
                case UNDERLINE:
                    gr.fillRect(xPixel, yPixel + textHeight - 2, textWidth, 2);
                    break;
                case BLOCK:
                    gr.fillRect(xPixel, yPixel, textWidth, textHeight);
                    break;
                case OUTLINE:
                    gr.drawRect(xPixel, yPixel, textWidth - 1, textHeight - 1);
                    break;
                }
            }
        }

        /**
         * Paint redraws the whole screen.
         *
         * @param gr the Swing Graphics context
         */
        @Override
        public void paint(final Graphics gr) {
            // Do nothing until the screen reference has been set.
            if (screen == null) {
                return;
            }
            if (screen.frame == null) {
                return;
            }

            // See if it is time to flip the blink time.
            long nowTime = (new Date()).getTime();
            if (nowTime > blinkMillis + lastBlinkTime) {
                lastBlinkTime = nowTime;
                cursorBlinkVisible = !cursorBlinkVisible;
            }

            int xCellMin = 0;
            int xCellMax = screen.width;
            int yCellMin = 0;
            int yCellMax = screen.height;

            Rectangle bounds = gr.getClipBounds();
            if (bounds != null) {
                // Only update what is in the bounds
                xCellMin = screen.textColumn(bounds.x);
                xCellMax = screen.textColumn(bounds.x + bounds.width);
                if (xCellMax > screen.width) {
                    xCellMax = screen.width;
                }
                if (xCellMin >= xCellMax) {
                    xCellMin = xCellMax - 2;
                }
                if (xCellMin < 0) {
                    xCellMin = 0;
                }
                yCellMin = screen.textRow(bounds.y);
                yCellMax = screen.textRow(bounds.y + bounds.height);
                if (yCellMax > screen.height) {
                    yCellMax = screen.height;
                }
                if (yCellMin >= yCellMax) {
                    yCellMin = yCellMax - 2;
                }
                if (yCellMin < 0) {
                    yCellMin = 0;
                }
            } else {
                // We need a total repaint
                reallyCleared = true;
            }

            // Prevent updates to the screen's data from the TApplication
            // threads.
            synchronized (screen) {
                /*
                System.err.printf("bounds %s X %d %d Y %d %d\n",
                    bounds, xCellMin, xCellMax, yCellMin, yCellMax);
                 */

                for (int y = yCellMin; y < yCellMax; y++) {
                    for (int x = xCellMin; x < xCellMax; x++) {

                        int xPixel = x * textWidth + left;
                        int yPixel = y * textHeight + top;

                        Cell lCell = screen.logical[x][y];
                        Cell pCell = screen.physical[x][y];

                        if (!lCell.equals(pCell)
                            || lCell.isBlink()
                            || reallyCleared) {

                            drawGlyph(gr, lCell, xPixel, yPixel);

                            // Physical is always updated
                            physical[x][y].setTo(lCell);
                        }
                    }
                }
                drawCursor(gr);

                dirty = false;
                reallyCleared = false;
            } // synchronized (screen)
        }

    } // class SwingFrame

    /**
     * The raw Swing JFrame.  Note package private access.
     */
    SwingFrame frame;

    /**
     * Restore terminal to normal state.
     */
    public void shutdown() {
        frame.dispose();
    }

    /**
     * Public constructor.
     *
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points.  Good values to pick are: 16, 20,
     * 22, and 24.
     */
    public SwingScreen(final int windowWidth, final int windowHeight,
        final int fontSize) {

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    SwingScreen.this.frame = new SwingFrame(SwingScreen.this,
                        fontSize);
                    SwingScreen.this.sessionInfo =
                        new SwingSessionInfo(SwingScreen.this.frame,
                            frame.textWidth, frame.textHeight,
                            windowWidth, windowHeight);

                    SwingScreen.this.setDimensions(sessionInfo.getWindowWidth(),
                        sessionInfo.getWindowHeight());

                    SwingScreen.this.frame.resizeToScreen();
                    SwingScreen.this.frame.setVisible(true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The sessionInfo.
     */
    private SwingSessionInfo sessionInfo;

    /**
     * Create the SwingSessionInfo.  Note package private access.
     *
     * @return the sessionInfo
     */
    SwingSessionInfo getSessionInfo() {
        return sessionInfo;
    }

    /**
     * Push the logical screen to the physical device.
     */
    @Override
    public void flushPhysical() {

        /*
        System.err.printf("flushPhysical(): reallyCleared %s dirty %s\n",
            reallyCleared, dirty);
         */

        // If reallyCleared is set, we have to draw everything.
        if ((frame.bufferStrategy != null) && (reallyCleared == true)) {
            // Triple-buffering: we have to redraw everything on this thread.
            Graphics gr = frame.bufferStrategy.getDrawGraphics();
            frame.paint(gr);
            gr.dispose();
            frame.bufferStrategy.show();
            // sync() doesn't seem to help the tearing for me.
            // Toolkit.getDefaultToolkit().sync();
            return;
        } else if ((frame.bufferStrategy == null) && (reallyCleared == true)) {
            // Repaint everything on the Swing thread.
            frame.repaint();
            return;
        }

        // Do nothing if nothing happened.
        if (!dirty) {
            return;
        }

        if (frame.bufferStrategy != null) {
            // See if it is time to flip the blink time.
            long nowTime = (new Date()).getTime();
            if (nowTime > frame.blinkMillis + frame.lastBlinkTime) {
                frame.lastBlinkTime = nowTime;
                frame.cursorBlinkVisible = !frame.cursorBlinkVisible;
            }

            Graphics gr = frame.bufferStrategy.getDrawGraphics();

            synchronized (this) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        Cell lCell = logical[x][y];
                        Cell pCell = physical[x][y];

                        int xPixel = x * frame.textWidth + frame.left;
                        int yPixel = y * frame.textHeight + frame.top;

                        if (!lCell.equals(pCell)
                            || ((x == cursorX)
                                && (y == cursorY)
                                && cursorVisible)
                            || (lCell.isBlink())
                        ) {
                            frame.drawGlyph(gr, lCell, xPixel, yPixel);
                            physical[x][y].setTo(lCell);
                        }
                    }
                }
                frame.drawCursor(gr);
            } // synchronized (this)

            gr.dispose();
            frame.bufferStrategy.show();
            // sync() doesn't seem to help the tearing for me.
            // Toolkit.getDefaultToolkit().sync();
            return;
        }

        // Swing thread version: request a repaint, but limit it to the area
        // that has changed.

        // Find the minimum-size damaged region.
        int xMin = frame.getWidth();
        int xMax = 0;
        int yMin = frame.getHeight();
        int yMax = 0;

        synchronized (this) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Cell lCell = logical[x][y];
                    Cell pCell = physical[x][y];

                    int xPixel = x * frame.textWidth + frame.left;
                    int yPixel = y * frame.textHeight + frame.top;

                    if (!lCell.equals(pCell)
                        || ((x == cursorX)
                            && (y == cursorY)
                            && cursorVisible)
                        || lCell.isBlink()
                    ) {
                        if (xPixel < xMin) {
                            xMin = xPixel;
                        }
                        if (xPixel + frame.textWidth > xMax) {
                            xMax = xPixel + frame.textWidth;
                        }
                        if (yPixel < yMin) {
                            yMin = yPixel;
                        }
                        if (yPixel + frame.textHeight > yMax) {
                            yMax = yPixel + frame.textHeight;
                        }
                    }
                }
            }
        }
        if (xMin + frame.textWidth >= xMax) {
            xMax += frame.textWidth;
        }
        if (yMin + frame.textHeight >= yMax) {
            yMax += frame.textHeight;
        }

        // Repaint the desired area
        /*
        System.err.printf("REPAINT X %d %d Y %d %d\n", xMin, xMax,
            yMin, yMax);
         */
        if (frame.bufferStrategy != null) {
            // This path should never be taken, but is left here for
            // completeness.
            Graphics gr = frame.bufferStrategy.getDrawGraphics();
            Rectangle bounds = new Rectangle(xMin, yMin, xMax - xMin,
                yMax - yMin);
            gr.setClip(bounds);
            frame.paint(gr);
            gr.dispose();
            frame.bufferStrategy.show();
            // sync() doesn't seem to help the tearing for me.
            // Toolkit.getDefaultToolkit().sync();
        } else {
            // Repaint on the Swing thread.
            frame.repaint(xMin, yMin, xMax - xMin, yMax - yMin);
        }
    }

    /**
     * Put the cursor at (x,y).
     *
     * @param visible if true, the cursor should be visible
     * @param x column coordinate to put the cursor on
     * @param y row coordinate to put the cursor on
     */
    @Override
    public void putCursor(final boolean visible, final int x, final int y) {

        if ((visible == cursorVisible) && ((x == cursorX) && (y == cursorY))) {
            // See if it is time to flip the blink time.
            long nowTime = (new Date()).getTime();
            if (nowTime < frame.blinkMillis + frame.lastBlinkTime) {
                // Nothing has changed, so don't do anything.
                return;
            }
        }

        if (cursorVisible
            && (cursorY <= height - 1)
            && (cursorX <= width - 1)
        ) {
            // Make the current cursor position dirty
            if (physical[cursorX][cursorY].getChar() == 'Q') {
                physical[cursorX][cursorY].setChar('X');
            } else {
                physical[cursorX][cursorY].setChar('Q');
            }
        }

        super.putCursor(visible, x, y);
    }

    /**
     * Convert pixel column position to text cell column position.
     *
     * @param x pixel column position
     * @return text cell column position
     */
    public int textColumn(final int x) {
        return ((x - frame.left) / frame.textWidth);
    }

    /**
     * Convert pixel row position to text cell row position.
     *
     * @param y pixel row position
     * @return text cell row position
     */
    public int textRow(final int y) {
        return ((y - frame.top) / frame.textHeight);
    }

    /**
     * Set the window title.
     *
     * @param title the new title
     */
    public void setTitle(final String title) {
        frame.setTitle(title);
    }

}
