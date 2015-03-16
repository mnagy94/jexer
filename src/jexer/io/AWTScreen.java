/**
 * Jexer - Java Text User Interface
 *
 * License: LGPLv3 or later
 *
 * This module is licensed under the GNU Lesser General Public License
 * Version 3.  Please see the file "COPYING" in this directory for more
 * information about the GNU Lesser General Public License Version 3.
 *
 *     Copyright (C) 2015  Kevin Lamonte
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, see
 * http://www.gnu.org/licenses/, or write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 * @author Kevin Lamonte [kevin.lamonte@gmail.com]
 * @version 1
 */
package jexer.io;

import jexer.bits.Cell;
import jexer.bits.CellAttributes;
import jexer.session.AWTSessionInfo;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * This Screen implementation draws to a Java AWT Frame.
 */
public final class AWTScreen extends Screen {

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
     * Setup AWT colors to match DOS color palette.
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
     * AWTFrame is our top-level hook into the AWT system.
     */
    class AWTFrame extends Frame {

        /**
         * The terminus font resource filename.
         */
        private static final String FONTFILE = "terminus-ttf-4.39/TerminusTTF-Bold-4.39.ttf";

        /**
         * The TUI Screen data.
         */
        AWTScreen screen;

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
         * Top pixel value.
         */
        private int top = 30;

        /**
         * Left pixel value.
         */
        private int left = 30;

        /**
         * Convert a CellAttributes foreground color to an AWT Color.
         *
         * @param attr the text attributes
         * @return the AWT Color
         */
        private Color attrToForegroundColor(final CellAttributes attr) {
            /*
             * TODO:
             *   reverse
             *   blink
             *   underline
             */
            if (attr.getBold()) {
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
            throw new IllegalArgumentException("Invalid color: " + attr.getForeColor().getValue());
        }

        /**
         * Convert a CellAttributes background color to an AWT Color.
         *
         * @param attr the text attributes
         * @return the AWT Color
         */
        private Color attrToBackgroundColor(final CellAttributes attr) {
            /*
             * TODO:
             *   reverse
             *   blink
             *   underline
             */
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
            throw new IllegalArgumentException("Invalid color: " + attr.getBackColor().getValue());
        }

        /**
         * Public constructor.
         *
         * @param screen the Screen that Backend talks to
         */
        public AWTFrame(final AWTScreen screen) {
            this.screen = screen;
            setDOSColors();

            setTitle("Jexer Application");
            setBackground(java.awt.Color.black);
            // setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            // setFont(new Font("Liberation Mono", Font.BOLD, 16));
            // setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));

            try {
                // Always try to use Terminus, the one decent font.
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                InputStream in = loader.getResourceAsStream(FONTFILE);
                Font terminusRoot = Font.createFont(Font.TRUETYPE_FONT, in);
                Font terminus = terminusRoot.deriveFont(Font.PLAIN, 22);
                setFont(terminus);
            } catch (Exception e) {
                e.printStackTrace();
                // setFont(new Font("Liberation Mono", Font.PLAIN, 24));
                setFont(new Font(Font.MONOSPACED, Font.PLAIN, 24));
            }
            setVisible(true);
            resizeToScreen();

            // Kill the X11 cursor
            // Transparent 16 x 16 pixel cursor image.
            BufferedImage cursorImg = new BufferedImage(16, 16,
                BufferedImage.TYPE_INT_ARGB);

            // Create a new blank cursor.
            Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
            setCursor(blankCursor);
        }

        /**
         * Resize to font dimensions.
         */
        public void resizeToScreen() {
            Graphics gr = getGraphics();
            FontMetrics fm = gr.getFontMetrics();
            maxDescent = fm.getMaxDescent();
            Rectangle2D bounds = fm.getMaxCharBounds(gr);
            int leading = fm.getLeading();
            textWidth = (int)Math.round(bounds.getWidth());
            textHeight = (int)Math.round(bounds.getHeight()) - maxDescent;
            // This also produces the same number, but works better for ugly
            // monospace.
            textHeight = fm.getMaxAscent() + maxDescent - leading;

            // Figure out the thickness of borders and use that to set the
            // final size.
            Insets insets = getInsets();
            left = insets.left;
            top = insets.top;

            setSize(textWidth * screen.width + insets.left + insets.right,
                textHeight * screen.height + insets.top + insets.bottom);

            /*
            System.err.printf("W: %d H: %d MD: %d L: %d\n", textWidth,
                textHeight, maxDescent, leading);
             */
        }

        /**
         * Update redraws the whole screen.
         *
         * @param gr the AWT Graphics context
         */
        @Override
        public void update(final Graphics gr) {
            // The default update clears the area.  Don't do that, instead
            // just paint it directly.
            paint(gr);
        }

        /**
         * Paint redraws the whole screen.
         *
         * @param gr the AWT Graphics context
         */
        @Override
        public void paint(final Graphics gr) {
            Rectangle bounds = gr.getClipBounds();

            for (int y = 0; y < screen.height; y++) {
                for (int x = 0; x < screen.width; x++) {
                    int xPixel = x * textWidth + left;
                    int yPixel = y * textHeight + top;

                    Cell lCell = screen.logical[x][y];
                    Cell pCell = screen.physical[x][y];

                    boolean inBounds = true;
                    if (bounds != null) {
                        if (bounds.contains(xPixel, yPixel)
                            || bounds.contains(xPixel + textWidth, yPixel)
                            || bounds.contains(xPixel, yPixel + textHeight)
                            || bounds.contains(xPixel + textWidth,
                                yPixel + textHeight)
                        ) {
                            // This area is damaged and will definitely be
                            // redrawn.
                            inBounds = true;
                        }
                    }

                    if (!lCell.equals(pCell) || inBounds) {
                        // Draw the background rectangle, then the foreground
                        // character.
                        gr.setColor(attrToBackgroundColor(lCell));
                        gr.fillRect(xPixel, yPixel, textWidth, textHeight);
                        gr.setColor(attrToForegroundColor(lCell));
                        char [] chars = new char[1];
                        chars[0] = lCell.getChar();
                        gr.drawChars(chars, 0, 1, xPixel,
                            yPixel + textHeight - maxDescent);

                        // Physical is always updated
                        physical[x][y].setTo(lCell);
                    }
                }
            }

            // Draw the cursor if it is visible
            if ((cursorVisible)
                && (cursorY <= screen.height - 1)
                && (cursorX <= screen.width - 1)
            ) {
                int xPixel = cursorX * textWidth + left;
                int yPixel = cursorY * textHeight + top;
                Cell lCell = screen.logical[cursorX][cursorY];
                gr.setColor(attrToForegroundColor(lCell));
                gr.fillRect(xPixel, yPixel + textHeight - 2, textWidth, 2);
            }
        }
    }

    /**
     * The raw AWT Frame.  Note package private access.
     */
    AWTFrame frame;

    /**
     * Public constructor.
     */
    public AWTScreen() {
        frame = new AWTFrame(this);
    }

    /**
     * Create the AWTSessionInfo.  Note package private access.
     *
     * @return the sessionInfo
     */
    AWTSessionInfo getSessionInfo() {
        AWTSessionInfo sessionInfo = new AWTSessionInfo(frame, frame.textWidth,
            frame.textHeight);
        return sessionInfo;
    }

    /**
     * Push the logical screen to the physical device.
     */
    @Override
    public void flushPhysical() {
        // Request a repaint, let the frame's repaint/update methods do the
        // right thing.
        // Find the minimum-size damaged region.
        int xMin = frame.getWidth();
        int xMax = 0;
        int yMin = frame.getHeight();
        int yMax = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell lCell = logical[x][y];
                Cell pCell = physical[x][y];

                int xPixel = x * frame.textWidth + frame.left;
                int yPixel = y * frame.textHeight + frame.top;

                if (!lCell.equals(pCell)
                    || ((x == cursorX) && (y == cursorY))
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

        // Ask for a repaint sometime in the next 10 millis.
        frame.repaint(10, xMin, yMin, xMax - xMin, yMax - yMin);
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
        if ((cursorVisible)
            && (cursorY <= height - 1)
            && (cursorX <= width - 1)
        ) {
            // Make the current cursor position dirty
            if (physical[cursorX][cursorY].getChar() == ' ') {
                physical[cursorX][cursorY].setChar('X');
            } else {
                physical[cursorX][cursorY].setChar(' ');
            }
        }

        super.putCursor(visible, x, y);
    }

}
