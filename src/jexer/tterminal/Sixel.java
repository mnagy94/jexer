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
package jexer.tterminal;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Sixel parses a buffer of sixel image data into a BufferedImage.
 */
public class Sixel {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Parser character scan states.
     */
    private enum ScanState {
        GROUND,
        QUOTE,
        COLOR,
        REPEAT,
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, enable debug messages.
     */
    private static boolean DEBUG = false;

    /**
     * Number of pixels to increment when we need more horizontal room.
     */
    private static int WIDTH_INCREASE = 400;

    /**
     * Number of pixels to increment when we need more vertical room.
     */
    private static int HEIGHT_INCREASE = 400;

    /**
     * Current scanning state.
     */
    private ScanState scanState = ScanState.GROUND;

    /**
     * Parameter characters being collected.
     */
    private ArrayList<Integer> colorParams;

    /**
     * The sixel palette colors specified.
     */
    private HashMap<Integer, Color> palette;

    /**
     * The buffer to parse.
     */
    private String buffer;

    /**
     * The image being drawn to.
     */
    private BufferedImage image;

    /**
     * The real width of image.
     */
    private int width = 0;

    /**
     * The real height of image.
     */
    private int height = 0;

    /**
     * The repeat count.
     */
    private int repeatCount = -1;

    /**
     * The current drawing x position.
     */
    private int x = 0;

    /**
     * The current drawing color.
     */
    private Color color = Color.BLACK;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param buffer the sixel data to parse
     */
    public Sixel(final String buffer) {
        this.buffer = buffer;
        colorParams = new ArrayList<Integer>();
        palette = new HashMap<Integer, Color>();
        image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < buffer.length(); i++) {
            consume(buffer.charAt(i));
        }
    }

    // ------------------------------------------------------------------------
    // Sixel ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the image.
     *
     * @return the sixel data as an image.
     */
    public BufferedImage getImage() {
        if ((width > 0) && (height > 0)) {
            return image.getSubimage(0, 0, width, height + 6);
        }
        return null;
    }

    /**
     * Resize image to a new size.
     *
     * @param newWidth new width of image
     * @param newHeight new height of image
     */
    private void resizeImage(final int newWidth, final int newHeight) {
        BufferedImage newImage = new BufferedImage(newWidth, newHeight,
            BufferedImage.TYPE_INT_ARGB);

        Graphics2D gr = newImage.createGraphics();
        gr.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        gr.dispose();
        image = newImage;
    }

    /**
     * Clear the parameters and flags.
     */
    private void toGround() {
        colorParams.clear();
        scanState = ScanState.GROUND;
        repeatCount = -1;
    }

    /**
     * Save a byte into the color parameters buffer.
     *
     * @param ch byte to save
     */
    private void param(final byte ch) {
        if (colorParams.size() == 0) {
            colorParams.add(Integer.valueOf(0));
        }
        Integer n = colorParams.get(colorParams.size() - 1);
        if ((ch >= '0') && (ch <= '9')) {
            n *= 10;
            n += (ch - '0');
            colorParams.set(colorParams.size() - 1, n);
        }

        if ((ch == ';') && (colorParams.size() < 16)) {
            colorParams.add(Integer.valueOf(0));
        }
    }

    /**
     * Get a color parameter value, with a default.
     *
     * @param position parameter index.  0 is the first parameter.
     * @param defaultValue value to use if colorParams[position] doesn't exist
     * @return parameter value
     */
    private int getColorParam(final int position, final int defaultValue) {
        if (colorParams.size() < position + 1) {
            return defaultValue;
        }
        return colorParams.get(position).intValue();
    }

    /**
     * Get a color parameter value, clamped to within min/max.
     *
     * @param position parameter index.  0 is the first parameter.
     * @param defaultValue value to use if colorParams[position] doesn't exist
     * @param minValue minimum value inclusive
     * @param maxValue maximum value inclusive
     * @return parameter value
     */
    private int getColorParam(final int position, final int defaultValue,
        final int minValue, final int maxValue) {

        assert (minValue <= maxValue);
        int value = getColorParam(position, defaultValue);
        if (value < minValue) {
            value = minValue;
        }
        if (value > maxValue) {
            value = maxValue;
        }
        return value;
    }

    /**
     * Add sixel data to the image.
     *
     * @param ch the character of sixel data
     */
    private void addSixel(final char ch) {
        int n = ((int) ch - 63);

        if (DEBUG && (color == null)) {
            System.err.println("color is null?!");
            System.err.println(buffer);
        }

        int rgb = color.getRGB();
        int rep = (repeatCount == -1 ? 1 : repeatCount);

        if (DEBUG) {
            System.err.println("addSixel() rep " + rep + " char " +
                Integer.toHexString(n) + " color " + color);
        }

        assert (n >= 0);

        if (x + rep > image.getWidth()) {
            // Resize the image, give us another max(rep, WIDTH_INCREASE)
            // pixels of horizontal length.
            resizeImage(image.getWidth() + Math.max(rep, WIDTH_INCREASE),
                image.getHeight());
        }

        // If nothing will be drawn, just advance x.
        if (n == 0) {
            x += rep;
            if (x > width) {
                width = x;
            }
            return;
        }

        for (int i = 0; i < rep; i++) {
            if ((n & 0x01) != 0) {
                image.setRGB(x, height + 0, rgb);
            }
            if ((n & 0x02) != 0) {
                image.setRGB(x, height + 1, rgb);
            }
            if ((n & 0x04) != 0) {
                image.setRGB(x, height + 2, rgb);
            }
            if ((n & 0x08) != 0) {
                image.setRGB(x, height + 3, rgb);
            }
            if ((n & 0x10) != 0) {
                image.setRGB(x, height + 4, rgb);
            }
            if ((n & 0x20) != 0) {
                image.setRGB(x, height + 5, rgb);
            }
            x++;
            if (x > width) {
                width++;
                assert (x == width);
            }
        }
    }

    /**
     * Process a color palette change.
     */
    private void setPalette() {
        int idx = getColorParam(0, 0);

        if (colorParams.size() == 1) {
            Color newColor = palette.get(idx);
            if (newColor != null) {
                color = newColor;
            } else {
                if (DEBUG) {
                    System.err.println("COLOR " + idx + " NOT FOUND");
                }
                color = Color.BLACK;
            }

            if (DEBUG) {
                System.err.println("set color " + idx + " " + color);
            }
            return;
        }

        int type = getColorParam(1, 0);
        float red   = (float) (getColorParam(2, 0, 0, 100) / 100.0);
        float green = (float) (getColorParam(3, 0, 0, 100) / 100.0);
        float blue  = (float) (getColorParam(4, 0, 0, 100) / 100.0);

        if (type == 2) {
            Color newColor = new Color(red, green, blue);
            palette.put(idx, newColor);
            if (DEBUG) {
                System.err.println("Palette color " + idx + " --> " + newColor);
            }
        } else {
            if (DEBUG) {
                System.err.println("UNKNOWN COLOR TYPE " + type + ": " + type +
                    " " + idx + " R " + red + " G " + green + " B " + blue);
            }
        }
    }

    /**
     * Run this input character through the sixel state machine.
     *
     * @param ch character from the remote side
     */
    private void consume(char ch) {

        // DEBUG
        // System.err.printf("Sixel.consume() %c STATE = %s\n", ch, scanState);

        // Between decimal 63 (inclusive) and 127 (exclusive) --> pixels
        if ((ch >= 63) && (ch < 127)) {
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            addSixel(ch);
            toGround();
            return;
        }

        if (ch == '#') {
            // Next color is here, parse what we had before.
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            scanState = ScanState.COLOR;
            return;
        }

        if (ch == '!') {
            // Repeat count
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            scanState = ScanState.REPEAT;
            repeatCount = 0;
            return;
        }

        if (ch == '-') {
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }

            if (height + 6 < image.getHeight()) {
                // Resize the image, give us another HEIGHT_INCREASE
                // pixels of vertical length.
                resizeImage(image.getWidth(),
                    image.getHeight() + HEIGHT_INCREASE);
            }
            height += 6;
            x = 0;
            return;
        }

        if (ch == '$') {
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            x = 0;
            return;
        }

        if (ch == '"') {
            if (scanState == ScanState.COLOR) {
                setPalette();
                toGround();
            }
            scanState = ScanState.QUOTE;
            return;
        }

        switch (scanState) {

        case GROUND:
            // Unknown character.
            if (DEBUG) {
                System.err.println("UNKNOWN CHAR: " + ch);
            }
            return;

        case QUOTE:
            // Ignore everything else in the quote header.
            return;

        case COLOR:
            // 30-39, 3B --> param
            if ((ch >= '0') && (ch <= '9')) {
                param((byte) ch);
            }
            if (ch == ';') {
                param((byte) ch);
            }
            return;

        case REPEAT:
            if ((ch >= '0') && (ch <= '9')) {
                if (repeatCount == -1) {
                    repeatCount = (int) (ch - '0');
                } else {
                    repeatCount *= 10;
                    repeatCount += (int) (ch - '0');
                }
            }
            return;

        }

    }

}
