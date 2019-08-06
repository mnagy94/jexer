/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2019 Kevin Lamonte
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
package jexer.backend;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;

import jexer.bits.Cell;

/**
 * GlyphMaker creates glyphs as bitmaps from a font.
 */
public class GlyphMaker {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The mono font resource filename (terminus).
     */
    public static final String MONO = "terminus-ttf-4.39/TerminusTTF-Bold-4.39.ttf";

    /**
     * The CJK font resource filename.
     */
    public static final String CJK = "NotoSansMonoCJKhk-Regular.otf";

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, enable debug messages.
     */
    private static boolean DEBUG = false;

    /**
     * The instance that has the mono (default) font.
     */
    private static GlyphMaker INSTANCE_MONO;

    /**
     * The instance that has the CJK font.
     */
    private static GlyphMaker INSTANCE_CJK;

    /**
     * If true, we were successful at getting the font dimensions.
     */
    private boolean gotFontDimensions = false;

    /**
     * The currently selected font.
     */
    private Font font = null;

    /**
     * The currently selected font size in points.
     */
    private int fontSize = 16;

    /**
     * Width of a character cell in pixels.
     */
    private int textWidth = 1;

    /**
     * Height of a character cell in pixels.
     */
    private int textHeight = 1;

    /**
     * Width of a character cell in pixels, as reported by font.
     */
    private int fontTextWidth = 1;

    /**
     * Height of a character cell in pixels, as reported by font.
     */
    private int fontTextHeight = 1;

    /**
     * Descent of a character cell in pixels.
     */
    private int maxDescent = 0;

    /**
     * System-dependent Y adjustment for text in the character cell.
     */
    private int textAdjustY = 0;

    /**
     * System-dependent X adjustment for text in the character cell.
     */
    private int textAdjustX = 0;

    /**
     * System-dependent height adjustment for text in the character cell.
     */
    private int textAdjustHeight = 0;

    /**
     * System-dependent width adjustment for text in the character cell.
     */
    private int textAdjustWidth = 0;

    /**
     * A cache of previously-rendered glyphs for blinking text, when it is
     * not visible.
     */
    private HashMap<Cell, BufferedImage> glyphCacheBlink;

    /**
     * A cache of previously-rendered glyphs for non-blinking, or
     * blinking-and-visible, text.
     */
    private HashMap<Cell, BufferedImage> glyphCache;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Private constructor used by the static instance methods.
     *
     * @param font the font to use
     */
    private GlyphMaker(final Font font) {
        this.font = font;
        fontSize = font.getSize();
    }

    /**
     * Public constructor.
     *
     * @param fontName the name of the font to use
     * @param fontSize the size of font to use
     */
    public GlyphMaker(final String fontName, final int fontSize) {
        font = new Font(fontName, Font.PLAIN, fontSize);
    }

    // ------------------------------------------------------------------------
    // GlyphMaker -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Obtain the GlyphMaker instance that uses the default monospace font.
     *
     * @return the instance
     */
    public static GlyphMaker getDefault() {

        synchronized (GlyphMaker.class) {
            if (INSTANCE_MONO != null) {
                return INSTANCE_MONO;
            }

            int fallbackFontSize = 16;
            Font monoRoot = null;
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                InputStream in = loader.getResourceAsStream(MONO);
                monoRoot = Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (java.awt.FontFormatException e) {
                e.printStackTrace();
                monoRoot = new Font(Font.MONOSPACED, Font.PLAIN,
                    fallbackFontSize);
            } catch (java.io.IOException e) {
                e.printStackTrace();
                monoRoot = new Font(Font.MONOSPACED, Font.PLAIN,
                    fallbackFontSize);
            }
            INSTANCE_MONO = new GlyphMaker(monoRoot);
            return INSTANCE_MONO;
        }
    }

    /**
     * Obtain the GlyphMaker instance that uses the CJK font.
     *
     * @return the instance
     */
    public static GlyphMaker getCJK() {

        synchronized (GlyphMaker.class) {
            if (INSTANCE_CJK != null) {
                return INSTANCE_CJK;
            }

            int fallbackFontSize = 16;
            Font cjkRoot = null;
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                InputStream in = loader.getResourceAsStream(CJK);
                cjkRoot = Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (java.awt.FontFormatException e) {
                e.printStackTrace();
                cjkRoot = new Font(Font.MONOSPACED, Font.PLAIN,
                    fallbackFontSize);
            } catch (java.io.IOException e) {
                e.printStackTrace();
                cjkRoot = new Font(Font.MONOSPACED, Font.PLAIN,
                    fallbackFontSize);
            }
            INSTANCE_CJK = new GlyphMaker(cjkRoot);
            return INSTANCE_CJK;
        }
    }

    /**
     * Obtain the GlyphMaker instance that uses the correct font for this
     * character.
     *
     * @param ch the character
     * @return the instance
     */
    public static GlyphMaker getInstance(final int ch) {
        if (((ch >= 0x4e00) && (ch <= 0x9fff))
            || ((ch >= 0x3400) && (ch <= 0x4dbf))
            || ((ch >= 0x20000) && (ch <= 0x2ebef))
        ) {
            return getCJK();
        }
        return getDefault();
    }

    /**
     * Get a derived font at a specific size.
     *
     * @param fontSize the size to use
     * @return a new instance at that font size
     */
    public GlyphMaker size(final int fontSize) {
        GlyphMaker maker = new GlyphMaker(font.deriveFont(Font.PLAIN,
                fontSize));
        return maker;
    }

    /**
     * Get a glyph image, using the font's idea of cell width and height.
     *
     * @param cell the character to draw
     * @return the glyph as an image
     */
    public BufferedImage getImage(final Cell cell) {
        return getImage(cell, textWidth, textHeight, true);
    }

    /**
     * Get a glyph image.
     *
     * @param cell the character to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @return the glyph as an image
     */
    public BufferedImage getImage(final Cell cell, final int cellWidth,
        final int cellHeight) {

        return getImage(cell, cellWidth, cellHeight, true);
    }

    /**
     * Get a glyph image.
     *
     * @param cell the character to draw
     * @param cellWidth the width of the text cell to draw into
     * @param cellHeight the height of the text cell to draw into
     * @param blinkVisible if true, the cell is visible if it is blinking
     * @return the glyph as an image
     */
    public BufferedImage getImage(final Cell cell, final int cellWidth,
        final int cellHeight, final boolean blinkVisible) {

        if (gotFontDimensions == false) {
            // Lazy-load the text width/height and adjustments.
            getFontDimensions();
        }

        BufferedImage image = null;
        if (cell.isBlink() && !blinkVisible) {
            image = glyphCacheBlink.get(cell);
        } else {
            image = glyphCache.get(cell);
        }
        if (image != null) {
            return image;
        }

        // Generate glyph and draw it.
        image = new BufferedImage(cellWidth, cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr2 = image.createGraphics();
        gr2.setFont(font);

        Cell cellColor = new Cell();
        cellColor.setTo(cell);

        // Check for reverse
        if (cell.isReverse()) {
            cellColor.setForeColor(cell.getBackColor());
            cellColor.setBackColor(cell.getForeColor());
        }

        // Draw the background rectangle, then the foreground character.
        gr2.setColor(SwingTerminal.attrToBackgroundColor(cellColor));
        gr2.fillRect(0, 0, cellWidth, cellHeight);

        // Handle blink and underline
        if (!cell.isBlink()
            || (cell.isBlink() && blinkVisible)
        ) {
            gr2.setColor(SwingTerminal.attrToForegroundColor(cellColor));
            char [] chars = new char[1];
            chars[0] = cell.getChar();
            gr2.drawChars(chars, 0, 1, textAdjustX,
                cellHeight - maxDescent + textAdjustY);

            if (cell.isUnderline()) {
                gr2.fillRect(0, cellHeight - 2, cellWidth, 2);
            }
        }
        gr2.dispose();

        // We need a new key that will not be mutated by invertCell().
        Cell key = new Cell();
        key.setTo(cell);
        if (cell.isBlink() && !blinkVisible) {
            glyphCacheBlink.put(key, image);
        } else {
            glyphCache.put(key, image);
        }

        return image;
    }

    /**
     * Figure out my font dimensions.
     */
    private void getFontDimensions() {
        glyphCacheBlink = new HashMap<Cell, BufferedImage>();
        glyphCache = new HashMap<Cell, BufferedImage>();

        BufferedImage image = new BufferedImage(fontSize * 2, fontSize * 2,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = image.createGraphics();
        gr.setFont(font);
        FontMetrics fm = gr.getFontMetrics();
        maxDescent = fm.getMaxDescent();
        Rectangle2D bounds = fm.getMaxCharBounds(gr);
        int leading = fm.getLeading();
        fontTextWidth = (int)Math.round(bounds.getWidth());
        // fontTextHeight = (int)Math.round(bounds.getHeight()) - maxDescent;

        // This produces the same number, but works better for ugly
        // monospace.
        fontTextHeight = fm.getMaxAscent() + maxDescent - leading;
        gr.dispose();

        textHeight = fontTextHeight + textAdjustHeight;
        textWidth = fontTextWidth + textAdjustWidth;

        gotFontDimensions = true;
    }

}
