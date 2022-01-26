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
import java.util.Comparator;
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
     * When fastAndDirty is set, the effective palette size for non-indexed
     * images.
     */
    private static final int FAST_AND_DIRTY = 16;

    /**
     * If true, try to partition the search space.  This is currently not
     * working well, but it can be quite a bit faster.  The inaccuracy is
     * very easy to see on a color wheel: tons of artifacts.
     */
    private static final boolean SEARCH_BUCKETS = true;

    /**
     * When run from the command line, we need both the image, and to know if
     * the image is transparent in order to set to correct sixel introducer.
     * So toSixel() returns a tuple now.
     */
    private class SixelResult {
        /**
         * The encoded image.
         */
        public String encodedImage;

        /**
         * If true, this image has transparent pixels.
         */
        public boolean transparent = false;

        /**
         * The palette used by this image.
         */
        public Palette palette;
    }

    /**
     * Palette is used to manage the conversion of images between 24-bit RGB
     * color and a palette of paletteSize colors.
     */
    private class Palette {

        /**
         * Timings records time points in the image generation cycle.
         */
        private class Timings {
            /**
             * Nanotime when the timings were begun.
             */
            public long startTime;

            /**
             * Nanotime after the image was scanned for color analysis.
             */
            public long scanImageTime;

            /**
             * Nanotime after the color map was produced.
             */
            public long buildColorMapTime;

            /**
             * Nanotime after which the RGB image was dithered into an
             * indexed image.
             */
            public long ditherImageTime;

            /**
             * Nanotime after which the dithered image was converted to sixel
             * and emitted.
             */
            public long emitSixelTime;

            /**
             * Nanotime when the timings were finished.
             */
            public long endTime;
        }

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
         * A bucket contains colors that will all be mapped to the same
         * weighted average color value.
         */
        private class Bucket {
            /**
             * The colors in this bucket.
             */
            private ArrayList<ColorIdx> colors;

            // The minimum and maximum, and "total" component values in this
            // bucket.
            private int minRed   = 0xFF;
            private int maxRed   = 0;
            private int minGreen = 0xFF;
            private int maxGreen = 0;
            private int minBlue  = 0xFF;
            private int maxBlue  = 0;

            // The last computed average() value.
            private int lastAverage = -1;

            /**
             * Public constructor.
             *
             * @param n the expected number of colors that will be in this
             * bucket
             */
            public Bucket(final int n) {
                reset(n);
            }

            /**
             * Reset the stats.
             *
             * @param n the expected number of colors that will be in this
             * bucket
             */
            private void reset(final int n) {
                colors      = new ArrayList<ColorIdx>(n);
                minRed      = 0xFF;
                maxRed      = 0;
                minGreen    = 0xFF;
                maxGreen    = 0;
                minBlue     = 0xFF;
                maxBlue     = 0;
                lastAverage = -1;
            }

            /**
             * Get the index associated with all of the colors in this
             * bucket.
             *
             * @return the index
             */
            public int getIndex() {
                return colors.get(0).index;
            }

            /**
             * Add a color to the bucket.
             *
             * @param color the color to add
             */
            public void add(final ColorIdx color) {
                colors.add(color);

                int rgb   = color.color;
                int red   = (rgb >>> 16) & 0xFF;
                int green = (rgb >>>  8) & 0xFF;
                int blue  =  rgb         & 0xFF;
                if (red > maxRed) {
                    maxRed = red;
                }
                if (red < minRed) {
                    minRed = red;
                }
                if (green > maxGreen) {
                    maxGreen = green;
                }
                if (green < minGreen) {
                    minGreen = green;
                }
                if (blue > maxBlue) {
                    maxBlue = blue;
                }
                if (blue < minBlue) {
                    minBlue = blue;
                }
            }

            /**
             * Partition this bucket into two buckets, split along the color
             * with the maximum range.
             *
             * @return the other bucket
             */
            public Bucket partition() {
                int redDiff = Math.max(0, (maxRed - minRed));
                int greenDiff = Math.max(0, (maxGreen - minGreen));
                int blueDiff = Math.max(0, (maxBlue - minBlue));
                if (verbosity >= 5) {
                    System.err.printf("partn colors %d Δr %d Δg %d Δb %d\n",
                        colors.size(), redDiff, greenDiff, blueDiff);
                }

                if ((redDiff > greenDiff) && (redDiff > blueDiff)) {
                    // Partition on red.
                    if (verbosity >= 5) {
                        System.err.println("    RED");
                    }
                    Collections.sort(colors, new Comparator<ColorIdx>() {
                        public int compare(ColorIdx c1, ColorIdx c2) {
                            int red1 = (c1.color >>> 16) & 0xFF;
                            int red2 = (c2.color >>> 16) & 0xFF;
                            return red1 - red2;
                        }
                    });
                } else if ((greenDiff > blueDiff) && (greenDiff > redDiff)) {
                    // Partition on green.
                    if (verbosity >= 5) {
                        System.err.println("    GREEN");
                    }
                    Collections.sort(colors, new Comparator<ColorIdx>() {
                        public int compare(ColorIdx c1, ColorIdx c2) {
                            int green1 = (c1.color >>> 8) & 0xFF;
                            int green2 = (c2.color >>> 8) & 0xFF;
                            return green1 - green2;
                        }
                    });
                } else {
                    // Partition on blue.
                    if (verbosity >= 5) {
                        System.err.println("    BLUE");
                    }
                    Collections.sort(colors, new Comparator<ColorIdx>() {
                        public int compare(ColorIdx c1, ColorIdx c2) {
                            int blue1 = c1.color & 0xFF;
                            int blue2 = c2.color & 0xFF;
                            return blue1 - blue2;
                        }
                    });
                }

                int oldN = colors.size();

                List<ColorIdx> newBucketColors;
                newBucketColors = colors.subList(oldN / 2, oldN);
                Bucket newBucket = new Bucket(newBucketColors.size());
                for (ColorIdx color: newBucketColors) {
                    newBucket.add(color);
                }

                List<ColorIdx> newColors;
                newColors = colors.subList(0, oldN - newBucketColors.size());
                reset(newColors.size());
                for (ColorIdx color: newColors) {
                    add(color);
                }
                assert (newBucketColors.size() + newColors.size() == oldN);
                return newBucket;
            }

            /**
             * Average the colors in this bucket.
             *
             * @return an averaged RGB value
             */
            public int average() {
                if (quantizationDone) {
                    int sixelColor = sixelColors.get(colors.get(0).index);
                    if ((sixelColor == 0xFF000000)
                        || (sixelColor == 0xFF646464)
                    ) {
                        // This bucket is mapped to black or white.
                        lastAverage = sixelColor;
                    }
                }
                if (lastAverage != -1) {
                    return lastAverage;
                }

                long totalRed = 0;
                long totalGreen = 0;
                long totalBlue = 0;
                long count = 0;
                for (ColorIdx color: colors) {
                    int rgb = color.color;
                    int red   = (rgb >>> 16) & 0xFF;
                    int green = (rgb >>>  8) & 0xFF;
                    int blue  =  rgb         & 0xFF;
                    totalRed   += color.count * red;
                    totalGreen += color.count * green;
                    totalBlue  += color.count * blue;
                    count += color.count;
                }
                totalRed   = clampSixel((int) (totalRed   / count));
                totalGreen = clampSixel((int) (totalGreen / count));
                totalBlue  = clampSixel((int) (totalBlue  / count));

                lastAverage = (int) ((0xFF << 24) | (totalRed   << 16)
                                                  | (totalGreen <<  8)
                                                  |  totalBlue);
                return lastAverage;
            }

        };

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
         * -1 = direct map indexed; 0 = direct map; 1 = median cut; 2 = octree.
         */
        private int quantizationType = -1;

        /**
         * The image from the constructor, mapped to sixel color space with
         * transparent pixels removed.
         */
        private int [] sixelImage;

        /**
         * The width of the image.
         */
        private int sixelImageWidth;

        /**
         * The width of the image.
         */
        private int sixelImageHeight;

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
         * The buckets produced by median cut.
         */
        private ArrayList<Bucket> buckets;

        /**
         * A fixed-size array of median cut bucket RGBs masked down to 8
         * bits, each entry containing a list of buckets.
         */
        private ArrayList<ArrayList<Bucket>> searchBuckets;

        /**
         * The mask for bits for red in searchBuckets.  Default is 3.
         */
        private int redMask = 0xE00000;

        /**
         * The number of bits for green in searchBuckets.  Default is 3.
         */
        private int greenMask = 0xE000;

        /**
         * The number of bits for blue in searchBuckets.  Default is 2.
         */
        private int blueMask = 0xC0;

        /**
         * If true, quantization is done.
         */
        private boolean quantizationDone = false;

        /**
         * Timings.
         */
        private Timings timings;

        /**
         * Public constructor.
         *
         * @param size number of colors available for this palette
         * @param image a bitmap image
         * @param allowTransparent if true, allow transparent pixels to be
         * specified
         */
        public Palette(final int size, final BufferedImage image,
            final boolean allowTransparent) {

            assert (size >= 2);

            if (doTimings) {
                timings = new Timings();
                timings.startTime = System.nanoTime();
            }

            paletteSize = size;
            int numColors = paletteSize;
            if (fastAndDirty) {
                // Fast and dirty: use fewer colors.  Horizontal banding will
                // result, but might not be noticeable for fast-moving
                // scenes.
                numColors = Math.min(paletteSize, FAST_AND_DIRTY);
            }
            sixelColors = new ArrayList<Integer>(numColors);

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
                        if (timings != null) {
                            timings.scanImageTime = System.nanoTime();
                        }
                        directIndexed(image, indexModel);
                        return;
                    }
                }
            }

            sixelImageWidth = image.getWidth();
            sixelImageHeight = image.getHeight();

            if (verbosity >= 1) {
                System.err.printf("Palette() image is %dx%d, bpp %d transparent %s allowed %s\n",
                    sixelImageWidth, sixelImageHeight,
                    image.getColorModel().getPixelSize(),
                    transparent, allowTransparent);
            }

            // Perform population count on colors.
            int [] rgbArray = image.getRGB(0, 0,
                sixelImageWidth, sixelImageHeight, null, 0, sixelImageWidth);
            sixelImage = rgbArray;
            colorMap = new HashMap<Integer, ColorIdx>(sixelImageWidth * sixelImageHeight);
            int transparent_count = 0;

            for (int i = 0; i < rgbArray.length; i++) {
                int colorRGB = rgbArray[i];
                if (transparent) {
                    int alpha = ((colorRGB >>> 24) & 0xFF);
                    if (alpha < ALPHA_OPAQUE) {
                        // This pixel is almost transparent, omit it.
                        transparent_count++;
                        if (allowTransparent) {
                            rgbArray[i] = 0x00f7a8b8;
                            continue;
                        } else {
                            rgbArray[i] = 0xFF000000;
                        }
                    }
                } else if ((colorRGB & 0xFF000000) != 0xFF000000) {
                    if (verbosity >= 10) {
                        System.err.printf("EH? color at %d is %08x\n", i,
                            colorRGB);
                    }
                    rgbArray[i] = 0xFF000000;
                }

                // Pull the 8-bit colors, and reduce them to 0-100 as per
                // sixel.
                int sixelRGB = toSixelColor(colorRGB,
                    (quantizationType == 0 ? false : true));
                rgbArray[i] = sixelRGB;
                ColorIdx color = colorMap.get(sixelRGB & 0x00FFFFFF);
                if (color == null) {
                    color = new ColorIdx(sixelRGB & 0x00FFFFFF);
                    colorMap.put(sixelRGB & 0x00FFFFFF, color);
                } else {
                    color.count++;
                }
            }
            // At this point the image data is mapped to the 101^3 sixel
            // color space, and any pixels with partial transparency below
            // ALPHA_OPAQUE are fully transparent (and pink).

            if (verbosity >= 1) {
                System.err.printf("# colors in image: %d palette size %d\n",
                    colorMap.size(), paletteSize);
                System.err.printf("# transparent pixels: %d (%3.1f%%)\n",
                    transparent_count,
                    (double) transparent_count * 100.0 /
                        (sixelImageWidth * sixelImageHeight));
            }
            if ((transparent_count == 0) || !allowTransparent) {
                transparent = false;
            }

            assert (colorMap.size() > 0);

            if (timings != null) {
                timings.scanImageTime = System.nanoTime();
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
            if (numColors >= colorMap.size()) {
                quantizationType = 0;
                directMap();
            } else if (true || (colorMap.size() <= numColors * 10)) {
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
         * Convert a 24-bit color to a 19.97-bit sixel color.
         *
         * @param rawColor the 24-bit color
         * @param checkBlackWhite if true, return pure black or pure white
         * for colors that are close to those
         * @return the sixel color
         */
        public int toSixelColor(final int rawColor, boolean checkBlackWhite) {

            if (quantizationType == 0) {
                assert (checkBlackWhite == false);
            }

            int red     = ((rawColor >>> 16) & 0xFF) * 100 / 255;
            int green   = ((rawColor >>>  8) & 0xFF) * 100 / 255;
            int blue    = ( rawColor         & 0xFF) * 100 / 255;

            if (quantizationType == 0) {
                return (0xFF << 24) | (red << 16) | (green << 8) | blue;
            }

            // These values are arbitrary.  Too low and you can get "static"
            // on images that have a very wide color range compared to
            // palette entries.  Too high and you lose a lot of detail on
            // otherwise great images.
            final int blackDiff = 100;
            final int whiteDiff = 0;
            if (((red * red) + (green * green) + (blue * blue)) < blackDiff) {
                if (verbosity >= 10) {
                    System.err.printf("mapping to black: %08x\n", rawColor);
                }

                // Black is a closer match.
                return 0xFF000000;
            } else if ((((100 - red) * (100 - red)) +
                    ((100 - green) * (100 - green)) +
                    ((100 - blue) * (100 - blue))) < whiteDiff) {

                if (verbosity >= 10) {
                    System.err.printf("mapping to white: %08x\n", rawColor);
                }
                // White is a closer match.
                return 0xFF646464;
            }
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

            assert (quantizationType == -1);

            sixelImageWidth = image.getWidth();
            sixelImageHeight = image.getHeight();
            sixelImage = new int[sixelImageWidth * sixelImageHeight];

            if (verbosity >= 1) {
                System.err.printf("Image is %dx%d, bpp %d transparent %s\n",
                    sixelImageWidth, sixelImageHeight, index.getPixelSize(),
                    transparent);
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
            for (int y = 0; y < sixelImageHeight; y++) {
                for (int x = 0; x < sixelImageWidth; x++) {
                    pixel = raster.getDataElements(x, y, pixel);
                    byte [] indexedPixel = (byte []) pixel;
                    int idx = indexedPixel[0] & 0xFF;
                    if (idx < 0) {
                        idx += 128;
                    }
                    if (idx == transparentPixel) {
                        sixelImage[x + (y * sixelImageWidth)] = -1;
                    } else {
                        // System.err.printf("(%d, %d) --> %d\n", x, y, idx);
                        sixelImage[x + (y * sixelImageWidth)] = idx;
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

            quantizationDone = true;
            if (verbosity >= 5) {
                System.err.printf("COLOR MAP: %d entries\n",
                    sixelColors.size());
                for (int i = 0; i < sixelColors.size(); i++) {
                    System.err.printf("   %03d %08x\n", i,
                        sixelColors.get(i));
                }
            }

            if (timings != null) {
                timings.buildColorMapTime = System.nanoTime();
            }

        }

        /**
         * Assign palette entries to the image colors.  This requires at
         * least as many palette colors as number of colors used in the
         * image.
         */
        public void directMap() {
            assert (quantizationType == 0);
            assert (paletteSize >= colorMap.size());

            if (verbosity >= 1) {
                System.err.println("Direct-map colors");
            }

            // The simplest thing: just put the used colors in RGB order.  We
            // don't _need_ an ordering, but it does make it nicer to look at
            // the generated output and understand what's going on.
            sixelColors = new ArrayList<Integer>(colorMap.size());
            for (ColorIdx color: colorMap.values()) {
                sixelColors.add(color.color);
            }
            if (verbosity >= 5) {
                Collections.sort(sixelColors);
            }
            assert (sixelColors.size() == colorMap.size());
            for (int i = 0; i < sixelColors.size(); i++) {
                colorMap.get(sixelColors.get(i)).index = i;
            }

            quantizationDone = true;
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
            if (timings != null) {
                timings.buildColorMapTime = System.nanoTime();
            }
        }

        /**
         * Perform median cut algorithm to generate a palette that fits
         * within the palette size.
         */
        public void medianCut() {
            assert (quantizationType == 1);

            // Populate the "total" bucket, performing some stats along the
            // way.

            /*
             * Persumably the final colors in the palette will roughly
             * partition the colors in the source image to maximize
             * variability.  Meaning that if there is a lot of distinct (say)
             * red shades, then there will be a lot of distinct red colors in
             * the final palette.  We can measure the variance of each
             * channel as we are building the initial bucket, and assign bits
             * to the search masks to proportionally match.
             *
             * This currently does not work great: there could be a neighbor
             * nearer to a palette color in RGB space that is in a different
             * search bucket and will not be checked.  Really I need a data
             * structure that is guaranteed to find the closest color.  I can
             * cheat a bit by making more search buckets (say 10-12 bit
             * space), but that's really just putting it off.  Let's live
             * with some noise for now.
             */
            double redMean = 0;
            double redDeltaSum = 0;
            double greenMean = 0;
            double greenDeltaSum = 0;
            double blueMean = 0;
            double blueDeltaSum = 0;
            int count = 0;

            Bucket bucket = new Bucket(colorMap.size());
            for (ColorIdx color: colorMap.values()) {
                bucket.add(color);

                int rgb = color.color;
                int red   = (rgb >>> 16) & 0xFF;
                int green = (rgb >>>  8) & 0xFF;
                int blue  =  rgb         & 0xFF;

                if (SEARCH_BUCKETS) {
                    // We compute an "online" version of the mean and variance.
                    count++;
                    double redDelta = red - redMean;
                    redMean += redDelta / count;
                    double redDelta2 = red - redMean;
                    redDeltaSum += redDelta * redDelta2;
                    double greenDelta = green - greenMean;
                    greenMean += greenDelta / count;
                    double greenDelta2 = green - greenMean;
                    greenDeltaSum += greenDelta * greenDelta2;
                    double blueDelta = blue - blueMean;
                    blueMean += blueDelta / count;
                    double blueDelta2 = blue - blueMean;
                    blueDeltaSum += blueDelta * blueDelta2;
                }
            }

            if (SEARCH_BUCKETS) {
                int colorN = colorMap.size();
                if (colorN > 2) {
                    double redSq = redDeltaSum / (colorN - 1);
                    double greenSq = greenDeltaSum / (colorN - 1);
                    double blueSq = blueDeltaSum / (colorN - 1);

                    double totalSq = redSq + greenSq + blueSq;
                    double redFrac = redSq / totalSq;
                    double greenFrac = greenSq / totalSq;
                    double blueFrac = blueSq / totalSq;

                    int redBits = Math.max(1, Math.min((int) (redFrac * 8.0), 6));
                    int greenBits = Math.max(1, Math.min((int) (greenFrac * 8.0), 6));
                    // Must have at least 1 bit for blue.
                    int blueBits = Math.max(1, 8 - redBits - greenBits);
                    // Steal it from green if needed.
                    greenBits = 8 - redBits - blueBits;
                    assert (redBits + greenBits + blueBits == 8);
                    assert ((redBits >= 1) && (redBits <= 6));
                    assert ((greenBits >= 1) && (greenBits <= 6));
                    assert ((blueBits >= 1) && (blueBits <= 6));

                    if (verbosity >= 1) {
                        System.err.printf("Variance: %4.2f%% red %4.2f%% green %4.2f%% blue\n",
                            redFrac * 100.0, greenFrac * 100.0, blueFrac * 100.0);
                        System.err.printf("    Bits:     %d  red     %d  green     %d  blue\n",
                            redBits, greenBits, blueBits);
                    }

                    // There is a faster way to do this surely.
                    switch (redBits) {
                    case 1:
                        redMask = 0x800000;
                        break;
                    case 2:
                        redMask = 0xC00000;
                        break;
                    case 3:
                        redMask = 0xE00000;
                        break;
                    case 4:
                        redMask = 0xF00000;
                        break;
                    case 5:
                        redMask = 0xF80000;
                        break;
                    case 6:
                        redMask = 0xFC0000;
                        break;
                    default:
                        break;
                    }
                    switch (greenBits) {
                    case 1:
                        greenMask = 0x8000;
                        break;
                    case 2:
                        greenMask = 0xC000;
                        break;
                    case 3:
                        greenMask = 0xE000;
                        break;
                    case 4:
                        greenMask = 0xF000;
                        break;
                    case 5:
                        greenMask = 0xF800;
                        break;
                    case 6:
                        greenMask = 0xFC00;
                        break;
                    default:
                        break;
                    }
                    switch (blueBits) {
                    case 1:
                        blueMask = 0x80;
                        break;
                    case 2:
                        blueMask = 0xC0;
                        break;
                    case 3:
                        blueMask = 0xE0;
                        break;
                    case 4:
                        blueMask = 0xF0;
                        break;
                    case 5:
                        blueMask = 0xF8;
                        break;
                    case 6:
                        blueMask = 0xFC;
                        break;
                    default:
                        break;
                    }
                }
            }

            int numColors = paletteSize;
            if (fastAndDirty) {
                // Fast and dirty: use fewer colors.  Horizontal banding will
                // result, but might not be noticeable for fast-moving
                // scenes.
                numColors = Math.min(paletteSize, FAST_AND_DIRTY);
            }

            // Find the number of buckets we can have based on the palette
            // size.
            int log2 = 31 - Integer.numberOfLeadingZeros(numColors);
            int totalBuckets = 1 << log2;
            if (verbosity >= 1) {
                System.err.println("Total buckets possible: " + totalBuckets);
            }

            buckets = new ArrayList<Bucket>(totalBuckets);
            buckets.add(bucket);
            while (buckets.size() < totalBuckets) {
                int n = buckets.size();
                for (int i = 0; i < n; i++) {
                    buckets.add(buckets.get(i).partition());
                }
            }
            assert (buckets.size() == totalBuckets);

            // Buckets are partitioned.  Now assign the colors in each to a
            // sixelColor index.  The darkest and lightest colors are
            // assigned to black and white, respectively.
            int idx = 0;
            int darkest = Integer.MAX_VALUE;
            int lightest = 0;
            int darkestIdx = -1;
            int lightestIdx = -1;
            final int diff = 1000;
            for (Bucket b: buckets) {
                for (ColorIdx color: b.colors) {
                    color.index = idx;

                    int rgb = color.color;
                    int red   = (rgb >>> 16) & 0xFF;
                    int green = (rgb >>>  8) & 0xFF;
                    int blue  =  rgb         & 0xFF;
                    int color2 = (red * red) + (green * green) + (blue * blue);
                    if (((red * red) + (green * green) + (blue * blue)) < diff) {
                        // Black is a close match.
                        if (color2 < darkest) {
                            darkest = color2;
                            darkestIdx = idx;
                        }
                    } else if ((((100 - red) * (100 - red)) +
                            ((100 - green) * (100 - green)) +
                            ((100 - blue) * (100 - blue))) < diff) {

                        // White is a close match.
                        if (color2 > lightest) {
                            lightest = color2;
                            lightestIdx = idx;
                        }
                    }
                }
                sixelColors.add(b.average());
                idx++;
            }
            if (darkestIdx != -1) {
                sixelColors.set(darkestIdx, 0xFF000000);
            }
            if (lightestIdx != -1) {
                sixelColors.set(lightestIdx, 0xFF646464);
            }

            // This provides a modest speedup: partition the bucket colors by
            // 3 bits red, 3 bits green, 2 bits blue.  Next stage is to
            // assign the number of bits to each channel to maximize coverage
            // of the palette space and make each searchBucket as small as
            // possible.
            if (SEARCH_BUCKETS) {
                searchBuckets = new ArrayList<ArrayList<Bucket>>(256);
                for (int i = 0; i < 256; i++) {
                    searchBuckets.add(new ArrayList<Bucket>());
                }
                for (Bucket b: buckets) {
                    int averageColor = b.average();
                    int maskedColor = ((averageColor & redMask  ) >>> 16)
                                    | ((averageColor & greenMask) >>> 8)
                                    |  (averageColor & blueMask );
                    searchBuckets.get(maskedColor).add(b);
                }
            }

            quantizationDone = true;
            if (verbosity >= 5) {
                System.err.printf("COLOR MAP: %d entries\n",
                    sixelColors.size());
                for (int i = 0; i < sixelColors.size(); i++) {
                    System.err.printf("   %03d %08x\n", i,
                        sixelColors.get(i));
                }
                if (SEARCH_BUCKETS) {
                    System.err.printf("searchBuckets\n",
                        sixelColors.size());
                    for (int i = 0; i < searchBuckets.size(); i++) {
                        System.err.printf("   %03d\n",
                            searchBuckets.get(i).size());
                    }
                }
            }

            if (timings != null) {
                timings.buildColorMapTime = System.nanoTime();
            }
        }

        /**
         * Perform octree-based color quantization.
         */
        public void octree() {
            // TODO: octree
            assert (quantizationType == 2);
        }

        /**
         * Clamp an int value to [0, 100].
         *
         * @param x the int value
         * @return an int between 0 and 100.
         */
        private final int clampSixel(final int x) {
            return Math.max(0, Math.min(x, 100));
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
                if (colorIdx == null) {
                    if (verbosity >= 5) {
                        // This should not be possible.  What happened?
                        System.err.println("FAILED TO MATCH ON DIRECT?!");
                        System.err.printf("   Color: %08x\n", color);
                        System.err.printf("COLOR MAP: %d entries\n",
                            sixelColors.size());
                        for (int i = 0; i < sixelColors.size(); i++) {
                            System.err.printf("   %03d %08x\n", i,
                                sixelColors.get(i));
                        }
                    }
                }
                return colorIdx.index;
            } else if (quantizationType == 1) {
                ColorIdx colorIdx = colorMap.get(color);
                if (verbosity >= 10) {
                    System.err.printf("matchColor(): %08x %d colorIdx %s\n",
                        color, color, colorIdx);
                }
                if (colorIdx != null) {
                    return colorIdx.index;
                }

                // Due to dithering, we are close but not quite on a color in
                // the index.  Do a search in the buckets to find the one
                // that is closest to this color.
                //
                // TODO: Make this faster, it's a search through every
                // bucket!  This is a HUGE bottleneck for median cut.  chafa
                // has a very fast way to search through a palette, can that
                // approach be adapted for use here?
                //
                // https://github.com/hpjansson/chafa/issues/27#issuecomment-647584817

                // See first if there are buckets on the same masked color.
                ArrayList<Bucket> bucketsToSearch = buckets;
                if (SEARCH_BUCKETS) {
                    int maskedColor = ((color & redMask  ) >>> 16)
                                    | ((color & greenMask) >>> 8)
                                    |  (color & blueMask );
                    if (searchBuckets.get(maskedColor).size() > 0) {
                        bucketsToSearch = searchBuckets.get(maskedColor);
                        if (verbosity >= 5) {
                            System.err.printf("Can search fast: %d buckets\n",
                                searchBuckets.get(maskedColor).size());
                        }
                    }
                }

                int red   = (color >>> 16) & 0xFF;
                int green = (color >>>  8) & 0xFF;
                int blue  =  color         & 0xFF;
                double diff = Double.MAX_VALUE;
                int idx = -1;
                int i = 0;
                for (Bucket b: bucketsToSearch) {
                    int rgbColor = b.average();
                    double newDiff = 0;
                    int red2   = (rgbColor >>> 16) & 0xFF;
                    int green2 = (rgbColor >>>  8) & 0xFF;
                    int blue2  =  rgbColor         & 0xFF;
                    newDiff += Math.pow(red2 - red, 2);
                    newDiff += Math.pow(green2 - green, 2);
                    newDiff += Math.pow(blue2 - blue, 2);
                    if (newDiff < diff) {
                        idx = b.getIndex();
                        diff = newDiff;
                    }
                }
                assert (idx != -1);
                if (verbosity >= 10) {
                    System.err.printf("matchColor(): --> %08x idx %d %08x\n",
                        color, idx, sixelColors.get(idx));
                }
                return idx;
            } else {
                // TODO: octree
                return 0;
            }

        }

        /**
         * Dither an image to a paletteSize palette.  The dithered
         * image cells will contain indexes into the palette.
         *
         * @return the dithered image rgb data.  Every pixel is an index into
         * the palette.
         */
        public int [] ditherImage() {
            if (quantizationType == 2) {
                // TODO: octree
                return null;
            }

            int [] rgbArray = sixelImage;
            if (noDither) {
                return rgbArray;
            }

            int height = sixelImageHeight;
            int width = sixelImageWidth;
            for (int imageY = 0; imageY < height; imageY++) {
                for (int imageX = 0; imageX < width; imageX++) {
                    int oldPixel = rgbArray[imageX + (width * imageY)];
                    if ((oldPixel & 0xFF000000) != 0xFF000000) {
                        // This is a transparent pixel.
                        if (verbosity >= 10) {
                            System.err.printf("transparent oldPixel(%d, %d) %08x\n",
                                imageX, imageY, oldPixel);
                        }
                        rgbArray[imageX + (width * imageY)] = -1;
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
                    rgbArray[imageX + (width * imageY)] = colorIdx;

                    if (quantizationType == 0) {
                        // For direct map, every possible color is already in
                        // the color map.  There should be no color error to
                        // dither out.
                        continue;
                    }

                    int oldRed   = (oldPixel >>> 16) & 0xFF;
                    int oldGreen = (oldPixel >>>  8) & 0xFF;
                    int oldBlue  =  oldPixel         & 0xFF;

                    int newRed   = (newPixel >>> 16) & 0xFF;
                    int newGreen = (newPixel >>>  8) & 0xFF;
                    int newBlue  =  newPixel         & 0xFF;

                    /*
                     * The dithering error values are different for sixel
                     * color space:
                     *
                     *   24-bit colorspace | Sixel colorspace
                     *   ------------------|-----------------
                     *           16        |       6
                     *            7        |       3
                     *            3        |       1
                     *            5        |       2
                     */

                    // 16 --> 6
                    int redError   = (  oldRed - newRed)   / 6;
                    int greenError = (oldGreen - newGreen) / 6;
                    int blueError  = ( oldBlue - newBlue)  / 6;

                    int red, green, blue;
                    if (imageX < sixelImageWidth - 1) {
                        int pXpY = rgbArray[imageX + 1 + (width * imageY)];
                        if ((pXpY & 0xFF000000) == 0xFF000000) {
                            // 7 --> 3
                            red   = ((pXpY >>> 16) & 0xFF) + (3 * redError);
                            green = ((pXpY >>>  8) & 0xFF) + (3 * greenError);
                            blue  = ( pXpY         & 0xFF) + (3 * blueError);
                            red = clampSixel(red);
                            green = clampSixel(green);
                            blue = clampSixel(blue);
                            pXpY = (0xFF << 24) | ((red & 0xFF) << 16)
                                 | ((green & 0xFF) << 8) | (blue & 0xFF);
                            rgbArray[imageX + 1 + (width * imageY)] = pXpY;
                        } else {
                            assert (transparent == true);
                            rgbArray[imageX + 1 + (width * imageY)] = 0;
                        }
                        if (imageY < sixelImageHeight - 1) {
                            int pXpYp = rgbArray[imageX + 1 + (width * (imageY + 1))];
                            if ((pXpYp & 0xFF000000) == 0xFF000000) {
                                red   = ((pXpYp >>> 16) & 0xFF) + redError;
                                green = ((pXpYp >>>  8) & 0xFF) + greenError;
                                blue  = ( pXpYp         & 0xFF) + blueError;
                                red = clampSixel(red);
                                green = clampSixel(green);
                                blue = clampSixel(blue);
                                pXpYp = (0xFF << 24) | ((red & 0xFF) << 16)
                                      | ((green & 0xFF) << 8) | (blue & 0xFF);
                                rgbArray[imageX + 1 + (width * (imageY + 1))] = pXpYp;
                            } else {
                                assert (transparent == true);
                                rgbArray[imageX + 1 + (width * (imageY + 1))] = 0;
                            }
                        }
                    } else if (imageY < sixelImageHeight - 1) {
                        int pXmYp = rgbArray[imageX - 1 + (width * (imageY + 1))];
                        int pXYp = rgbArray[imageX + (width * (imageY + 1))];

                        if ((pXmYp & 0xFF000000) == 0xFF000000) {
                            // 3 --> 1
                            red   = ((pXmYp >>> 16) & 0xFF) + (1 * redError);
                            green = ((pXmYp >>>  8) & 0xFF) + (1 * greenError);
                            blue  = ( pXmYp         & 0xFF) + (1 * blueError);
                            red = clampSixel(red);
                            green = clampSixel(green);
                            blue = clampSixel(blue);
                            pXmYp = (0xFF << 24) | ((red & 0xFF) << 16)
                                  | ((green & 0xFF) << 8) | (blue & 0xFF);
                            rgbArray[imageX - 1 + (width * (imageY + 1))] = pXmYp;
                        } else {
                            assert (transparent == true);
                            rgbArray[imageX - 1 + (width * (imageY + 1))] = 0;
                        }

                        if ((pXYp & 0xFF000000) == 0xFF000000) {
                            // 5 --> 2
                            red   = ((pXYp >>> 16) & 0xFF) + (2 * redError);
                            green = ((pXYp >>>  8) & 0xFF) + (2 * greenError);
                            blue  = ( pXYp         & 0xFF) + (2 * blueError);
                            red = clampSixel(red);
                            green = clampSixel(green);
                            blue = clampSixel(blue);
                            pXYp = (0xFF << 24) | ((red & 0xFF) << 16)
                                 | ((green & 0xFF) << 8) | (blue & 0xFF);
                            rgbArray[imageX + (width * (imageY + 1))] = pXYp;
                        } else {
                            assert (transparent == true);
                            rgbArray[imageX + (width * (imageY + 1))] = 0;
                        }
                    }
                } // for (int imageY = 0; imageY < height; imageY++)
            } // for (int imageX = 0; imageX < width; imageX++)

            return rgbArray;
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
     * 1024.  For HQ encoder the default is 256.
     */
    private int paletteSize = 256;

    /**
     * The palette used in the last image.
     */
    private Palette lastPalette;

    /**
     * If true, record timings for the image.
     */
    private boolean doTimings = false;

    /**
     * If true, be fast and dirty.
     */
    private boolean fastAndDirty = false;

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
        int paletteSize = 256;
        try {
            paletteSize = Integer.parseInt(System.getProperty(
                "jexer.ECMA48.sixelPaletteSize", "256"));
            switch (paletteSize) {
            case 2:
            case 4:
            case 8:
            case 16:
            case 32:
            case 64:
            case 128:
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
        if (System.getProperty("jexer.ECMA48.sixelFastAndDirty",
                "false").equals("true")
        ) {
            fastAndDirty = true;
        } else {
            fastAndDirty = false;
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
        return toSixel(bitmap, false);
    }

    /**
     * Create a sixel string representing a bitmap.  The returned string does
     * NOT include the DCS start or ST end sequences.
     *
     * @param bitmap the bitmap data
     * @param allowTransparent if true, allow transparent pixels to be
     * specified
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    public String toSixel(final BufferedImage bitmap,
        final boolean allowTransparent) {

        return toSixelResult(bitmap, allowTransparent).encodedImage;
    }

    /**
     * Create a sixel string representing a bitmap.  The returned string does
     * NOT include the DCS start or ST end sequences.
     *
     * @param bitmap the bitmap data
     * @param allowTransparent if true, allow transparent pixels to be
     * specified
     * @return the encoded string and transparency flag
     */
    private SixelResult toSixelResult(final BufferedImage bitmap,
        final boolean allowTransparent) {

        // Start with 16k potential total output.
        StringBuilder sb = new StringBuilder(16384);

        assert (bitmap != null);
        int fullHeight = bitmap.getHeight();

        SixelResult result = new SixelResult();

        // Anaylze the picture and generate a palette.
        Palette palette = new Palette(paletteSize, bitmap, allowTransparent);
        result.palette = palette;
        result.transparent = palette.transparent;

        // Dither the image.  We don't bother wrapping it in a BufferedImage.
        int [] rgbArray = palette.ditherImage();

        if (palette.timings != null) {
            palette.timings.ditherImageTime = System.nanoTime();
        }

        if (rgbArray == null) {
            if (palette.timings != null) {
                palette.timings.emitSixelTime = System.nanoTime();
                palette.timings.endTime = System.nanoTime();
            }
            result.encodedImage = "";
            return result;
        }

        // Collect the raster information
        int rasterHeight = 0;
        int rasterWidth = bitmap.getWidth();

        // Emit the palette.
        palette.emitPalette(sb);

        // Render the entire row of cells.
        int width = bitmap.getWidth();
        int [][] sixels = new int[width][6];

        for (int currentRow = 0; currentRow < fullHeight; currentRow += 6) {

            // See which colors are actually used in this band of sixels.
            boolean [] usedColors = new boolean[palette.sixelColors.size()];

            for (int imageX = 0; imageX < width; imageX++) {
                for (int imageY = 0;
                     (imageY < 6) && (imageY + currentRow < fullHeight);
                     imageY++) {

                    // There is a small performance gain reading the array
                    // all at once.
                    // int colorIdx = image.getRGB(imageX, imageY + currentRow);
                    int colorIdx = rgbArray[imageX +
                        (width * (imageY + currentRow))];

                    if (allowTransparent && (colorIdx == -1)) {
                        sixels[imageX][imageY] = colorIdx;
                        continue;
                    }
                    if (!palette.noDither) {
                        assert (colorIdx >= 0);
                        assert (colorIdx < palette.sixelColors.size());
                    }
                    if (!allowTransparent) {
                        assert (colorIdx != -1);
                    }
                    sixels[imageX][imageY] = colorIdx;
                    usedColors[colorIdx] = true;
                }
            }

            for (int i = 0; i < usedColors.length; i++) {
                if (!usedColors[i]) {
                    continue;
                }

                // Set to the beginning of scan line for the next set of
                // colored pixels, and select the color.
                sb.append("$#");
                sb.append(Integer.toString(i));

                int oldData = -1;
                int oldDataCount = 0;
                for (int imageX = 0; imageX < width; imageX++) {

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

                    // DEBUG
                    /*
                    if (i == 63) {
                        System.err.printf("color63 %d %d %c %d\n",
                            data, oldData, (char) oldData, oldDataCount);
                    }
                     */

                    if (data == oldData) {
                        oldDataCount++;
                    } else {
                        if (oldDataCount == 1) {
                            assert (oldData != -1);
                            sb.append((char) oldData);
                        } else if (oldDataCount > 1) {
                            sb.append("!");
                            sb.append(Integer.toString(oldDataCount));
                            assert (oldData != -1);
                            sb.append((char) oldData);
                        }
                        oldDataCount = 1;
                        oldData = data;
                    }

                } // for (int imageX = 0; imageX < width; imageX++)

                // Emit the last sequence.
                if (oldDataCount == 1) {
                    assert (oldData != -1);
                    sb.append((char) oldData);
                } else if (oldDataCount > 1) {
                    assert (oldData != -1);
                    sb.append("!");
                    sb.append(Integer.toString(oldDataCount));
                    sb.append((char) oldData);
                }

            } // for (int i = 0; i < palette.sixelColors.size(); i++)

            // Advance to the next scan line.
            sb.append("-");

        } // for (int currentRow = 0; currentRow < imageHeight; currentRow += 6)

        // Kill the very last "-", because it is unnecessary.
        sb.deleteCharAt(sb.length() - 1);

        // Add the raster information.
        sb.insert(0, String.format("\"1;1;%d;%d", rasterWidth, rasterHeight));

        if (palette.timings != null) {
            palette.timings.emitSixelTime = System.nanoTime();
            palette.timings.endTime = System.nanoTime();
        }
        result.encodedImage = sb.toString();
        return result;
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
        case 4:
        case 8:
        case 16:
        case 32:
        case 64:
        case 128:
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
     * Convert all filenames to sixel.
     *
     * @param args[] the filenames to read
     */
    public static void main(final String [] args) {
        if ((args.length == 0)
            || ((args.length == 1) && args[0].equals("-v"))
            || ((args.length == 1) && args[0].equals("-vv"))
            || ((args.length == 1) && args[0].equals("-t"))
        ) {
            System.err.println("USAGE: java jexer.backend.HQSixelEncoder [ -t | -v | -vv ] { file1 [ file2 ... ] }");
            System.exit(-1);
        }

        HQSixelEncoder encoder = new HQSixelEncoder();
        int successCount = 0;
        boolean allowTransparent = true;
        if (encoder.hasSharedPalette()) {
            System.out.print("\033[?1070l");
        } else {
            System.out.print("\033[?1070h");
        }
        System.out.flush();
        for (int i = 0; i < args.length; i++) {
            if ((i == 0) && args[i].equals("-v")) {
                encoder.verbosity = 1;
                encoder.doTimings = true;
                continue;
            }
            if ((i == 0) && args[i].equals("-vv")) {
                encoder.verbosity = 10;
                encoder.doTimings = true;
                continue;
            }
            if ((i == 0) && args[i].equals("-t")) {
                encoder.doTimings = true;
                continue;
            }
            try {
                BufferedImage image = ImageIO.read(new FileInputStream(args[i]));
                // Put together the image.
                StringBuilder sb = new StringBuilder();
                SixelResult result = encoder.toSixelResult(image,
                    allowTransparent);
                result.palette.emitPalette(sb);
                sb.append(result.encodedImage);
                sb.append("\033\\");
                // If there are transparent pixels, we need to note that at
                // the beginning.
                String header = "\033Pq";
                if (result.transparent && allowTransparent) {
                    header = "\033P0;1;0q";
                }
                // Now put it together.
                System.out.print(header);
                System.out.print(sb.toString());
                System.out.flush();

                if (encoder.doTimings) {
                    Palette.Timings timings = result.palette.timings;
                    double scanTime = (double) (timings.scanImageTime - timings.startTime) / 1.0e9;
                    double mapTime = (double) (timings.buildColorMapTime - timings.scanImageTime) / 1.0e9;
                    double ditherTime = (double) (timings.ditherImageTime - timings.buildColorMapTime) / 1.0e9;
                    double emitSixelTime = (double) (timings.emitSixelTime - timings.ditherImageTime) / 1.0e9;
                    double totalTime = (double) (timings.endTime - timings.startTime) / 1.0e9;

                    System.err.println("Timings:");
                    System.err.printf(" Act. scan %6.4fs\tmap %6.4fs\tdither %6.4fs\temit %6.4fs\n",
                        scanTime, mapTime, ditherTime, emitSixelTime);
                    System.err.printf(" Pct. scan %4.2f%%\tmap %4.2f%%\tdither %4.2f%%\temit %4.2f%%\n",
                        100.0 * scanTime / totalTime,
                        100.0 * mapTime / totalTime,
                        100.0 * ditherTime / totalTime,
                        100.0 * emitSixelTime / totalTime);
                    System.err.printf(" total %6.4fs\n", totalTime);
                }
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
