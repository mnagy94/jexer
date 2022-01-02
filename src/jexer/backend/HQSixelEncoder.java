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
package jexer.backend;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * HQSixelEncoder turns a BufferedImage into String of sixel image data,
 * using several strategies to produce a reasonably high quality image within
 * sixel's ~19.97 bit (101^3) color depth.
 */
public class HQSixelEncoder implements SixelEncoder {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Alpha value (0 - 255) above which to consider the pixel opaque.
     */
    private static final int ALPHA_OPAQUE = 102;        // ~40%

    /**
     * Palette is used to manage the conversion of images between 24-bit RGB
     * color and a palette of paletteSize colors.
     */
    private class Palette {

        /**
         * ColorIdx records a RGB color and its palette index.
         */
        private class ColorIdx {

            /**
             * The ~19.97-bit RGB color.  Each component has a value between
             * 0 and 100.
             */
            public int color;

            /**
             * The palette index for this color.
             */
            public int index;

            /**
             * The population count for this color.
             */
            public int count = 0;

            /**
             * Public constructor.
             *
             * @param color the ~19.97-bit sixel color
             * @param index the palette index for this color
             */
            public ColorIdx(final int color, final int index) {
                this.color = color;
                this.index = index;
                this.count = 0;
            }

            /**
             * Public constructor.  Count is set to 1, index to -1.
             *
             * @param color the ~19.97-bit sixel color
             */
            public ColorIdx(final int color) {
                this.color = color;
                this.index = -1;
                this.count = 1;
            }

            /**
             * Hash only on color.
             *
             * @return the hash
             */
            @Override
            public int hashCode() {
                return color;
            }

            /**
             * Generate a human-readable string for this entry.
             *
             * @return a human-readable string
             */
            @Override
            public String toString() {
                return String.format("color %06x index %d count %d",
                    color, index, count);
            }
        }

        /**
         * Number of colors in this palette.
         */
        private int paletteSize = 0;

        /**
         * Color palette for sixel output, sorted low to high.
         */
        private List<Integer> sixelColors = null;

        /**
         * Map of colors used in the image by RGB.
         */
        private HashMap<Integer, ColorIdx> colorMap = null;

        /**
         * Type of color quantization used.
         *
         * 0 = direct map; 1 = median cut; 2 = octree.
         */
        private int quantizationType = -1;

        /**
         * The image from the constructor, mapped to sixel color space with
         * transparent pixels removed.
         */
        private BufferedImage sixelImage;

        /**
         * If true, some pixels of the image are transparent.
         */
        private boolean transparent = false;

        /**
         * If true, sixelImage is already indexed and does not require
         * dithering.
         */
        private boolean noDither = false;

        /**
         * Public constructor.
         *
         * @param size number of colors available for this palette
         * @param image a bitmap image
         */
        public Palette(final int size, final BufferedImage image) {
            assert (size > 2);

            paletteSize = size;
            sixelColors = new ArrayList<Integer>(size);

            if (image.getTransparency() == Transparency.TRANSLUCENT) {
                // PNG like images where transparency is carried in alpha.
                transparent = true;
            } else {
                // Indexed images where transparency is denoted by a specific
                // pixel color.
                ColorModel colorModel = image.getColorModel();
                if (colorModel instanceof IndexColorModel) {
                    IndexColorModel indexModel = (IndexColorModel) colorModel;
                    if (indexModel.getTransparentPixel() != -1) {
                        transparent = true;
                    }
                    if (indexModel.getMapSize() <= paletteSize) {
                        if (verbosity >= 1) {
                            System.err.printf("Indexed: %d colors -> direct\n",
                                indexModel.getMapSize());
                        }
                        directIndexed(image, indexModel);
                        return;
                    }
                }
            }

            int width = image.getWidth();
            int height = image.getHeight();
            sixelImage = new BufferedImage(image.getWidth(), image.getHeight(),
                 BufferedImage.TYPE_INT_ARGB);

            if (verbosity >= 1) {
                System.err.printf("Palette() image is %dx%d, bpp %d transparent %s\n",
                    width, height, image.getColorModel().getPixelSize(),
                    transparent
                );
            }

            // Perform population count on colors.
            int [] rgbArray = image.getRGB(0, 0, width, height, null, 0, width);
            colorMap = new HashMap<Integer, ColorIdx>(width * height);
            int transparent_count = 0;
            for (int i = 0; i < rgbArray.length; i++) {
                int colorRGB = rgbArray[i];
                if (transparent) {
                    int alpha = ((colorRGB >>> 24) & 0xFF);
                    if (alpha < ALPHA_OPAQUE) {
                        // This pixel is almost transparent, omit it.
                        transparent_count++;
                        rgbArray[i] = 0x00f7a8b8;
                        continue;
                    }
                }

                // Pull the 8-bit colors, and reduce them to 0-100 as per
                // sixel.
                int sixelRGB = toSixelColor(colorRGB);
                rgbArray[i] = sixelRGB;
                ColorIdx color = colorMap.get(sixelRGB & 0x00FFFFFF);
                if (color == null) {
                    color = new ColorIdx(sixelRGB & 0x00FFFFFF);
                    colorMap.put(sixelRGB & 0x00FFFFFF, color);
                } else {
                    color.count++;
                }
            }
            // Save the image data mapped to the 101^3 sixel color space.
            // This also sets any pixels with partial transparency below
            // ALPHA_OPAQUE to fully transparent (and pink).
            sixelImage.setRGB(0, 0, width, height, rgbArray, 0, width);

            if (verbosity >= 1) {
                System.err.printf("# colors in image: %d palette size %d\n",
                    colorMap.size(), paletteSize);
                System.err.printf("# transparent pixels: %d (%3.1f%%)\n",
                    transparent_count,
                    (double) transparent_count * 100.0 / (width * height));
            }
            if (transparent_count == 0) {
                transparent = false;
            }

            /*
             * Here we choose between several options:
             *
             * - If the palette size is big enough for the number of colors,
             *   then just do a straight 1-1 mapping.
             *
             * - If the (number of colors:palette size) ratio is below 10,
             *   use median cut.
             *
             * - Otherwise use octree.
             */
            if (paletteSize >= colorMap.size()) {
                quantizationType = 0;
                directMap();
            } else if ((colorMap.size() <= paletteSize * 10) || true) {
                // For now, direct map and median cut are all we get.
                quantizationType = 1;
                medianCut();
            } else {
                quantizationType = 2;
                octree();
            }
        }

        /**
         * Convert a 24-bit color to a 19.97-bit sixel color.
         *
         * @param rawColor the 24-bit color
         * @return the sixel color
         */
        public int toSixelColor(final int rawColor) {
            int red     = ((rawColor >>> 16) & 0xFF) * 100 / 255;
            int green   = ((rawColor >>>  8) & 0xFF) * 100 / 255;
            int blue    = ( rawColor         & 0xFF) * 100 / 255;
            return (0xFF << 24) | (red << 16) | (green << 8) | blue;
        }

        /**
         * Use the pre-existing indexed palette of the image.
         *
         * @param image a bitmap image
         * @param index the indexed palette
         */
        private void directIndexed(final BufferedImage image,
            final IndexColorModel index) {

            int width = image.getWidth();
            int height = image.getHeight();
            sixelImage = new BufferedImage(image.getWidth(), image.getHeight(),
                 BufferedImage.TYPE_INT_ARGB);

            if (verbosity >= 1) {
                System.err.printf("Image is %dx%d, bpp %d transparent %s\n",
                    width, height, index.getPixelSize(), transparent
                );
            }

            // Map the pre-existing image palette into sixelImage and
            // sixelColors.
            noDither = true;

            Raster raster = image.getRaster();
            Object pixel = null;
            int transferType = raster.getTransferType();
            // System.err.println("transferType " + transferType);

            int transparentPixel = index.getTransparentPixel();
            int maxColorIdx = -1;
            pixel = raster.getDataElements(0, 0, pixel);
            if (transferType != DataBuffer.TYPE_BYTE) {
                // TODO: other kinds of transfer types
                throw new RuntimeException("Transfer type " +
                    transferType + " unsupported");
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixel = raster.getDataElements(x, y, pixel);
                    byte [] indexedPixel = (byte []) pixel;
                    int idx = indexedPixel[0] & 0xFF;
                    if (idx < 0) {
                        idx += 128;
                    }
                    if (idx == transparentPixel) {
                        sixelImage.setRGB(x, y, -1);
                    } else {
                        // System.err.printf("(%d, %d) --> %d\n", x, y, idx);
                        sixelImage.setRGB(x, y, idx);
                        maxColorIdx = Math.max(idx, maxColorIdx);
                    }
                }
            }

            int [] rgbs = new int[index.getMapSize()];
            index.getRGBs(rgbs);
            assert (sixelColors.size() == 0);
            for (int i = 0; i < rgbs.length && i <= maxColorIdx; i++) {
                int red   = ((rgbs[i] >>> 16) & 0xFF) * 100 / 255;
                int green = ((rgbs[i] >>>  8) & 0xFF) * 100 / 255;
                int blue  = ((rgbs[i]       ) & 0xFF) * 100 / 255;
                int sixelRGB = (red << 16) | (green << 8) | blue;
                sixelColors.add(sixelRGB);
            }
            assert (sixelColors.size() == maxColorIdx + 1);

            if (verbosity >= 5) {
                System.err.printf("COLOR MAP: %d entries\n",
                    sixelColors.size());
                for (int i = 0; i < sixelColors.size(); i++) {
                    System.err.printf("   %03d %08x\n", i,
                        sixelColors.get(i));
                }
            }

        }

        /**
         * Assign palette entries to the image colors.  This requires at
         * least as many palette colors as number of colors used in the
         * image.
         */
        public void directMap() {
            assert (paletteSize >= colorMap.size());

            if (verbosity >= 1) {
                System.err.println("Direct-map colors");
            }

            // The simplest thing: just put the used colors in RGB order.  We
            // don't _need_ an ordering, but it does make it nicer to look at
            // the generated output and understand what's going on.
            sixelColors = new ArrayList<Integer>(colorMap.size());
            for (Integer key: colorMap.keySet()) {
                sixelColors.add(colorMap.get(key).color);
            }
            Collections.sort(sixelColors);
            assert (sixelColors.size() == colorMap.size());
            for (int i = 0; i < sixelColors.size(); i++) {
                colorMap.get(sixelColors.get(i)).index = i;
            }

            if (verbosity >= 1) {
                System.err.printf("colorMap size %d sixelColors size %d\n",
                    colorMap.size(), sixelColors.size());
                if (verbosity >= 5) {
                    System.err.printf("COLOR MAP:\n");
                    for (int i = 0; i < sixelColors.size(); i++) {
                        System.err.printf("   %03d %s\n", i,
                            colorMap.get(sixelColors.get(i)));
                    }
                }
            }
        }

        /**
         * Perform median cut algorithm to generate a palette that fits
         * within the palette size.
         */
        public void medianCut() {
            // TODO
        }

        /**
         * Perform octree-based color quantization.
         */
        public void octree() {
            // TODO
        }

        /**
         * Clamp an int value to [0, 100].
         *
         * @param x the int value
         * @return an int between 0 and 100.
         */
        private int clampSixel(final int x) {
            if (x < 0) {
                return 0;
            }
            if (x > 100) {
                return 100;
            }
            return x;
        }

        /**
         * Find the nearest match for a color in the palette.
         *
         * @param color the sixel color
         * @return the color in the palette that is closest to color
         */
        public int matchColor(final int color) {

            assert (color >= 0);

            assert ((quantizationType == 0)
                || (quantizationType == 1)
                || (quantizationType == 2));

            if (quantizationType == 0) {
                ColorIdx colorIdx = colorMap.get(color);
                if (verbosity >= 10) {
                    System.err.printf("matchColor(): %08x %d colorIdx %s\n",
                        color, color, colorIdx);
                }
                return colorIdx.index;
            } else if (quantizationType == 1) {
                // TODO: median cut
                return 0;
            } else {
                // TODO: octree
                return 0;
            }

        }

        /**
         * Dither an image to a paletteSize palette.  The dithered
         * image cells will contain indexes into the palette.
         *
         * @return the dithered image.  Every pixel is an index into the
         * palette.
         */
        public BufferedImage ditherImage() {
            if (noDither) {
                return sixelImage;
            }

            if (quantizationType == 1) {
                // TODO: support median cut
                return null;
            }
            if (quantizationType == 2) {
                // TODO: support octree
                return null;
            }

            BufferedImage ditheredImage = new BufferedImage(sixelImage.getWidth(),
                sixelImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

            int [] rgbArray = sixelImage.getRGB(0, 0, sixelImage.getWidth(),
                sixelImage.getHeight(), null, 0, sixelImage.getWidth());
            ditheredImage.setRGB(0, 0, sixelImage.getWidth(),
                sixelImage.getHeight(), rgbArray, 0, sixelImage.getWidth());

            for (int imageY = 0; imageY < sixelImage.getHeight(); imageY++) {
                for (int imageX = 0; imageX < sixelImage.getWidth(); imageX++) {
                    int oldPixel = ditheredImage.getRGB(imageX,
                        imageY);
                    if ((oldPixel & 0xFF000000) != 0xFF000000) {
                        // This is a transparent pixel.
                        if (verbosity >= 10) {
                            System.err.printf("transparent oldPixel(%d, %d) %08x\n",
                                imageX, imageY, oldPixel);
                        }
                        ditheredImage.setRGB(imageX, imageY, -1);
                        continue;
                    }
                    if (verbosity >= 10) {
                        System.err.printf("opaque oldPixel(%d, %d) %08x\n",
                            imageX, imageY, oldPixel);
                    }
                    int colorIdx = matchColor(oldPixel & 0x00FFFFFF);
                    assert (colorIdx >= 0);
                    assert (colorIdx < sixelColors.size());
                    int newPixel = sixelColors.get(colorIdx);
                    ditheredImage.setRGB(imageX, imageY, colorIdx);

                    int oldRed   = (oldPixel >>> 16) & 0xFF;
                    int oldGreen = (oldPixel >>>  8) & 0xFF;
                    int oldBlue  =  oldPixel         & 0xFF;

                    int newRed   = (newPixel >>> 16) & 0xFF;
                    int newGreen = (newPixel >>>  8) & 0xFF;
                    int newBlue  =  newPixel         & 0xFF;

                    int redError   = (oldRed - newRed) / 16;
                    int greenError = (oldGreen - newGreen) / 16;
                    int blueError  = (oldBlue - newBlue) / 16;

                    int red, green, blue;
                    if (imageX < sixelImage.getWidth() - 1) {
                        int pXpY  = ditheredImage.getRGB(imageX + 1, imageY);
                        if ((pXpY & 0xFF000000) == 0xFF000000) {
                            red   = ((pXpY >>> 16) & 0xFF) + (7 * redError);
                            green = ((pXpY >>>  8) & 0xFF) + (7 * greenError);
                            blue  = ( pXpY         & 0xFF) + (7 * blueError);
                            red = clampSixel(red);
                            green = clampSixel(green);
                            blue = clampSixel(blue);
                            pXpY = (0xFF << 24) | ((red & 0xFF) << 16);
                            pXpY |= ((green & 0xFF) << 8) | (blue & 0xFF);
                            ditheredImage.setRGB(imageX + 1, imageY, pXpY);
                        }
                        if (imageY < sixelImage.getHeight() - 1) {
                            int pXpYp = ditheredImage.getRGB(imageX + 1,
                                imageY + 1);
                            if ((pXpYp & 0xFF000000) == 0xFF000000) {
                                red   = ((pXpYp >>> 16) & 0xFF) + redError;
                                green = ((pXpYp >>>  8) & 0xFF) + greenError;
                                blue  = ( pXpYp         & 0xFF) + blueError;
                                red = clampSixel(red);
                                green = clampSixel(green);
                                blue = clampSixel(blue);
                                pXpYp = (0xFF << 24) | ((red & 0xFF) << 16);
                                pXpYp |= ((green & 0xFF) << 8) | (blue & 0xFF);
                                ditheredImage.setRGB(imageX + 1, imageY + 1,
                                    pXpYp);
                            }
                        }
                    } else if (imageY < sixelImage.getHeight() - 1) {
                        int pXmYp = ditheredImage.getRGB(imageX - 1,
                            imageY + 1);
                        int pXYp  = ditheredImage.getRGB(imageX,
                            imageY + 1);

                        if ((pXmYp & 0xFF000000) == 0xFF000000) {
                            red   = ((pXmYp >>> 16) & 0xFF) + (3 * redError);
                            green = ((pXmYp >>>  8) & 0xFF) + (3 * greenError);
                            blue  = ( pXmYp         & 0xFF) + (3 * blueError);
                            red = clampSixel(red);
                            green = clampSixel(green);
                            blue = clampSixel(blue);
                            pXmYp = ((red & 0xFF) << 16);
                            pXmYp |= ((green & 0xFF) << 8) | (blue & 0xFF);
                            ditheredImage.setRGB(imageX - 1, imageY + 1, pXmYp);
                        }

                        if ((pXYp & 0xFF000000) == 0xFF000000) {
                            red   = ((pXYp >>> 16) & 0xFF) + (5 * redError);
                            green = ((pXYp >>>  8) & 0xFF) + (5 * greenError);
                            blue  = ( pXYp         & 0xFF) + (5 * blueError);
                            red = clampSixel(red);
                            green = clampSixel(green);
                            blue = clampSixel(blue);
                            pXYp = (0xFF << 24) | ((red & 0xFF) << 16);
                            pXYp |= ((green & 0xFF) << 8) | (blue & 0xFF);
                            ditheredImage.setRGB(imageX,     imageY + 1, pXYp);
                        }
                    }
                } // for (int imageY = 0; imageY < image.getHeight(); imageY++)
            } // for (int imageX = 0; imageX < image.getWidth(); imageX++)

            return ditheredImage;
        }

        /**
         * Emit the sixel palette.
         *
         * @param sb the StringBuilder to append to
         * @return the string to emit to an ANSI / ECMA-style terminal
         */
        public String emitPalette(final StringBuilder sb) {
            for (int i = 0; i < sixelColors.size(); i++) {
                int sixelColor = sixelColors.get(i);
                sb.append(String.format("#%d;2;%d;%d;%d", i,
                        ((sixelColor >>> 16) & 0xFF),
                        ((sixelColor >>>  8) & 0xFF),
                        ( sixelColor         & 0xFF)));
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Verbosity level for analysis mode.
     */
    private int verbosity = 0;

    /**
     * Number of colors in the sixel palette.  Xterm 335 defines the max as
     * 1024.
     */
    private int paletteSize = 1024;

    /**
     * The palette used in the last image.
     */
    private Palette lastPalette;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     */
    public HQSixelEncoder() {
        reloadOptions();
    }

    // ------------------------------------------------------------------------
    // HQSixelEncoder ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Reload options from System properties.
     */
    public void reloadOptions() {
        // Palette size
        int paletteSize = 1024;
        try {
            paletteSize = Integer.parseInt(System.getProperty(
                "jexer.ECMA48.sixelPaletteSize", "1024"));
            switch (paletteSize) {
            case 2:
            case 256:
            case 512:
            case 1024:
            case 2048:
                this.paletteSize = paletteSize;
                break;
            default:
                // Ignore value
                break;
            }
        } catch (NumberFormatException e) {
            // SQUASH
        }
    }

    /**
     * Create a sixel string representing a bitmap.  The returned string does
     * NOT include the DCS start or ST end sequences.
     *
     * @param bitmap the bitmap data
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    public String toSixel(final BufferedImage bitmap) {
        StringBuilder sb = new StringBuilder();

        assert (bitmap != null);

        int fullHeight = bitmap.getHeight();

        // Anaylze the picture and generate a palette.
        lastPalette = new Palette(paletteSize, bitmap);

        // Dither the image
        BufferedImage image = lastPalette.ditherImage();

        if (image == null) {
            return "";
        }

        // Collect the raster information
        int rasterHeight = 0;
        int rasterWidth = bitmap.getWidth();

        // Emit the palette.
        lastPalette.emitPalette(sb);

        // Render the entire row of cells.
        for (int currentRow = 0; currentRow < fullHeight; currentRow += 6) {
            int [][] sixels = new int[image.getWidth()][6];

            // See which colors are actually used in this band of sixels.
            for (int imageX = 0; imageX < image.getWidth(); imageX++) {
                for (int imageY = 0;
                     (imageY < 6) && (imageY + currentRow < fullHeight);
                     imageY++) {

                    int colorIdx = image.getRGB(imageX, imageY + currentRow);
                    if (colorIdx == -1) {
                        sixels[imageX][imageY] = colorIdx;
                        continue;
                    }
                    if (!lastPalette.noDither) {
                        assert (colorIdx >= 0);
                        assert (colorIdx < lastPalette.sixelColors.size());
                    }

                    sixels[imageX][imageY] = colorIdx;
                }
            }

            for (int i = 0; i < lastPalette.sixelColors.size(); i++) {
                boolean isUsed = false;
                for (int imageX = 0; imageX < image.getWidth(); imageX++) {
                    for (int j = 0; j < 6; j++) {
                        if (sixels[imageX][j] == i) {
                            isUsed = true;
                        }
                    }
                }
                if (isUsed == false) {
                    continue;
                }

                // Set to the beginning of scan line for the next set of
                // colored pixels, and select the color.
                sb.append(String.format("$#%d", i));

                int oldData = -1;
                int oldDataCount = 0;
                for (int imageX = 0; imageX < image.getWidth(); imageX++) {

                    // Add up all the pixels that match this color.
                    int data = 0;
                    for (int j = 0;
                         (j < 6) && (currentRow + j < fullHeight);
                         j++) {

                        if (sixels[imageX][j] == i) {
                            switch (j) {
                            case 0:
                                data += 1;
                                break;
                            case 1:
                                data += 2;
                                break;
                            case 2:
                                data += 4;
                                break;
                            case 3:
                                data += 8;
                                break;
                            case 4:
                                data += 16;
                                break;
                            case 5:
                                data += 32;
                                break;
                            }
                            if ((currentRow + j + 1) > rasterHeight) {
                                rasterHeight = currentRow + j + 1;
                            }
                        }
                    }
                    assert (data >= 0);
                    assert (data < 64);
                    data += 63;

                    if (data == oldData) {
                        oldDataCount++;
                    } else {
                        if (oldDataCount == 1) {
                            sb.append((char) oldData);
                        } else if (oldDataCount > 1) {
                            sb.append(String.format("!%d", oldDataCount));
                            sb.append((char) oldData);
                        }
                        oldDataCount = 1;
                        oldData = data;
                    }

                } // for (int imageX = 0; imageX < image.getWidth(); imageX++)

                // Emit the last sequence.
                if (oldDataCount == 1) {
                    sb.append((char) oldData);
                } else if (oldDataCount > 1) {
                    sb.append(String.format("!%d", oldDataCount));
                    sb.append((char) oldData);
                }

            } // for (int i = 0; i < lastPalette.sixelColors.size(); i++)

            // Advance to the next scan line.
            sb.append("-");

        } // for (int currentRow = 0; currentRow < imageHeight; currentRow += 6)

        // Kill the very last "-", because it is unnecessary.
        sb.deleteCharAt(sb.length() - 1);

        // Add the raster information
        sb.insert(0, String.format("\"1;1;%d;%d", rasterWidth, rasterHeight));

        return sb.toString();
    }

    /**
     * If the palette is shared for the entire terminal, emit it to a
     * StringBuilder.
     *
     * @param sb the StringBuilder to write the shared palette to
     */
    public void emitPalette(final StringBuilder sb) {
        // NOP
    }

    /**
     * Get the sixel shared palette option.
     *
     * @return true if all sixel output is using the same palette that is set
     * in one DCS sequence and used in later sequences
     */
    public boolean hasSharedPalette() {
        return false;
    }

    /**
     * Set the sixel shared palette option.
     *
     * @param sharedPalette if true, then all sixel output will use the same
     * palette that is set in one DCS sequence and used in later sequences
     */
    public void setSharedPalette(final boolean sharedPalette) {
        // NOP
    }

    /**
     * Get the number of colors in the sixel palette.
     *
     * @return the palette size
     */
    public int getPaletteSize() {
        return paletteSize;
    }

    /**
     * Set the number of colors in the sixel palette.
     *
     * @param paletteSize the new palette size
     */
    public void setPaletteSize(final int paletteSize) {
        if (this.paletteSize == paletteSize) {
            return;
        }

        switch (paletteSize) {
        case 2:
        case 256:
        case 512:
        case 1024:
        case 2048:
            break;
        default:
            throw new IllegalArgumentException("Unsupported sixel palette " +
                " size: " + paletteSize);
        }

        this.paletteSize = paletteSize;
    }

    /**
     * Clear the sixel palette.  It will be regenerated on the next image
     * encode.
     */
    public void clearPalette() {
        // NOP
    }

    /**
     * Get the sixel transparency option.
     *
     * @return true if some pixels will be transparent
     */
    public boolean isTransparent() {
        if (lastPalette != null) {
            return lastPalette.transparent;
        }
        return false;
    }

    /**
     * Convert all filenames to sixel.
     *
     * @param args[] the filenames to read
     */
    public static void main(final String [] args) {
        if ((args.length == 0)
            || ((args.length == 1) && args[0].equals("-v"))
            || ((args.length == 1) && args[0].equals("-vv"))
        ) {
            System.err.println("USAGE: java jexer.backend.HQSixelEncoder [ -v | -vv ] { file1 [ file2 ... ] }");
            System.exit(-1);
        }

        HQSixelEncoder encoder = new HQSixelEncoder();
        int successCount = 0;
        if (encoder.hasSharedPalette()) {
            System.out.print("\033[?1070l");
        } else {
            System.out.print("\033[?1070h");
        }
        System.out.flush();
        for (int i = 0; i < args.length; i++) {
            if ((i == 0) && args[i].equals("-v")) {
                encoder.verbosity = 1;
                continue;
            }
            if ((i == 0) && args[i].equals("-vv")) {
                encoder.verbosity = 10;
                continue;
            }
            try {
                BufferedImage image = ImageIO.read(new FileInputStream(args[i]));
                // Put together the image.
                StringBuilder sb = new StringBuilder();
                encoder.emitPalette(sb);
                sb.append(encoder.toSixel(image));
                sb.append("\033\\");
                // If there are transparent pixels, we need to note that at
                // the beginning.
                String header = "\033Pq";
                if (encoder.isTransparent()) {
                    header = "\033P0;1;0q";
                }
                // Now put it together.
                System.out.print(header);
                System.out.print(sb.toString());
                System.out.flush();
            } catch (Exception e) {
                System.err.println("Error reading file:");
                e.printStackTrace();
            }

        }
        System.out.print("\033[?1070h");
        System.out.flush();
        if (successCount == args.length) {
            System.exit(0);
        } else {
            System.exit(successCount);
        }
    }

}
