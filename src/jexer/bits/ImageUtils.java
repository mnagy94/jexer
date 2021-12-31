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
package jexer.bits;

import java.awt.image.BufferedImage;

/**
 * ImageUtils contains methods to:
 *
 *    - Check if an image is fully transparent.
 *
 *    - Scale an image and preserve aspect ratio.
 */
public class ImageUtils {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Selections for fitting the image to the text cells.
     */
    public enum Scale {
        /**
         * Stretch/shrink the image in both directions to fully fill the text
         * area width/height.
         */
        STRETCH,

        /**
         * Scale the image, preserving aspect ratio, to fill the text area
         * width/height (like letterbox).  The background color for the
         * letterboxed area is specified in the backColor argument to
         * scaleImage().
         */
        SCALE,
    }

    // ------------------------------------------------------------------------
    // ImageUtils -------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if any pixels in an image have not-0% alpha value.
     *
     * @return true if every pixel is fully transparent
     */
    public static boolean isFullyTransparent(final BufferedImage image) {
        assert (image != null);

        int [] rgbArray = image.getRGB(0, 0,
            image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        if (rgbArray.length == 0) {
            // No image data, fully transparent.
            return true;
        }

        for (int i = 0; i < rgbArray.length; i++) {
            int alpha = (rgbArray[i] >>> 24) & 0xFF;
            if (alpha != 0x00) {
                // A not-fully transparent pixel is found.
                return false;
            }
        }
        // Every pixel was transparent.
        return true;
    }

    /**
     * Check if any pixels in an image have not-100% alpha value.
     *
     * @return true if every pixel is fully transparent
     */
    public static boolean isFullyOpaque(final BufferedImage image) {
        assert (image != null);

        int [] rgbArray = image.getRGB(0, 0,
            image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        if (rgbArray.length == 0) {
            // No image data, fully transparent.
            return true;
        }

        for (int i = 0; i < rgbArray.length; i++) {
            int alpha = (rgbArray[i] >>> 24) & 0xFF;
            if (alpha != 0xFF) {
                // A partially transparent pixel is found.
                return false;
            }
        }
        // Every pixel was opaque.
        return true;
    }

    /**
     * Scale an image to be scaleFactor size and/or stretch it to fit a
     * target box.
     *
     * @param image the image to scale
     * @param width the width in pixels for the destination image
     * @param height the height in pixels for the destination image
     * @param scale the scaling type
     * @param backColor the background color to use for Scale.SCALE
     */
    public static BufferedImage scaleImage(final BufferedImage image,
        final int width, final int height,
        final Scale scale, final java.awt.Color backColor) {

        BufferedImage newImage = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);

        int x = 0;
        int y = 0;
        int destWidth = width;
        int destHeight = height;
        switch (scale) {
        case STRETCH:
            break;
        case SCALE:
            double a = (double) image.getWidth() / image.getHeight();
            double b = (double) width / height;
            double h = (double) height / image.getHeight();
            double w = (double) width / image.getWidth();
            assert (a > 0);
            assert (b > 0);

            if (a > b) {
                // Horizontal letterbox
                destHeight = (int) (image.getWidth() / a * w);
                destWidth = (int) (image.getWidth() * w);
                y = (height - destHeight) / 2;
                assert (y >= 0);
            } else {
                // Vertical letterbox
                destHeight = (int) (image.getHeight() * h);
                destWidth = (int) (image.getHeight() * a * h);
                x = (width - destWidth) / 2;
                assert (x >= 0);
            }
            break;
        }

        java.awt.Graphics gr = newImage.createGraphics();
        if (scale == Scale.SCALE) {
            gr.setColor(backColor);
            gr.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
        }
        gr.drawImage(image, x, y, destWidth, destHeight, null);
        gr.dispose();
        return newImage;
    }

}
