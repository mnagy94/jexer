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
 *
 * Portions of this encoder were inspired / influenced by Hans Petter
 * Jansson's chafa project: https://hpjansson.org/chafa/ .  Please refer to
 * chafa's high-performance sixel encoder for far more advanced
 * implementations of principal component analysis color mapping, and sixel
 * row encoding.
 */
package jexer.backend;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.FileInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;

import jexer.bits.MathUtils;

/**
 * HQSixelEncoder turns a BufferedImage into String of sixel image data,
 * using several strategies to produce a reasonably high quality image within
 * sixel's ~19.97 bit (101^3) color depth.
 *
 * <p>
 * Portions of this encoder were inspired / influenced by Hans Petter
 * Jansson's chafa project: https://hpjansson.org/chafa/ .  Please refer to
 * chafa's high-performance sixel encoder for far more advanced
 * implementations of principal component analysis color mapping, and sixel
 * row encoding.
 * </p>
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
    private static final int FAST_AND_DIRTY = 64;

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
             * The population count for this color.
             */
            public int count = 0;

            /**
             * The palette index for this color, only used for directMap().
             */
            public int directMapIndex = 0;

            /**
             * Public constructor.
             *
             * @param color the ~19.97-bit sixel color
             */
            public ColorIdx(final int color, final int index) {
                this.color = color;
                this.count = 0;
            }

            /**
             * Public constructor.  Count is set to 1, index to -1.
             *
             * @param color the ~19.97-bit sixel color
             */
            public ColorIdx(final int color) {
                this.color = color;
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
                return String.format("color %06x count %d", color, count);
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

            /**
             * The palette index for this bucket.  For now this points to a
             * simple average of all the colors, or black if no colors are in
             * this bucket.
             */
            public int index = 0;

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
                index       = 0;
            }

            /**
             * Get the index associated with all of the colors in this
             * bucket.
             *
             * @return the index
             */
            public int getIndex() {
                return index;
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
                if (lastAverage != -1) {
                    return lastAverage;
                }

                if (quantizationDone) {
                    if (colors.size() == 0) {
                        lastAverage = 0xFF000000;
                        return lastAverage;
                    }
                    int sixelColor = sixelColors.get(index);
                    if ((sixelColor == 0xFF000000)
                        || (sixelColor == 0xFF646464)
                    ) {
                        // This bucket is mapped to black or white.
                        lastAverage = sixelColor;
                        return lastAverage;
                    }
                }

                // Compute the average color.
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
                if (count == 0) {
                    lastAverage = 0xFF000000;
                    return lastAverage;
                }
                totalRed   = clampSixel((int) (totalRed   / count));
                totalGreen = clampSixel((int) (totalGreen / count));
                totalBlue  = clampSixel((int) (totalBlue  / count));

                lastAverage = (int) ((0xFF << 24) | (totalRed   << 16)
                                                  | (totalGreen <<  8)
                                                  |  totalBlue);
                return lastAverage;
            }

            /**
             * Generate a human-readable string for this entry.
             *
             * @return a human-readable string
             */
            @Override
            public String toString() {
                return String.format("bucket %d colors avg RGB %06x index %d",
                    colors.size(), average(), index);
            }
        };

        /**
         * A mapping of sixel color index to its first principal component.
         */
        private class PcaColor implements Comparable<PcaColor> {

            /**
             * Index into sixelColors.
             */
            private int sixelIndex = 0;

            /**
             * The first principal component value.
             */
            private double firstPca = 0;

            /**
             * The second principal component value.
             */
            private double secondPca = 0;

            /**
             * The third principal component value.
             */
            private double thirdPca = 0;

            /**
             * Public constructor.
             *
             * @param sixelIndex the the index into sixelColors
             * @param firstPca the first principal component
             * @param secondPca the second principal component
             * @param thirdPca the third principal component
             */
            public PcaColor(final int sixelIndex, final double firstPca,
                final double secondPca, final double thirdPca) {

                this.sixelIndex = sixelIndex;
                this.firstPca = firstPca;
                this.secondPca = secondPca;
                this.thirdPca = thirdPca;
            }

            /**
             * Comparison operator puts the natural ordering along the first
             * principal component.
             *
             * @param that another PcaColor
             * @return difference between this.firstPca and that.firstPca
             */
            public int compareTo(final PcaColor that) {
                return Double.compare(this.firstPca, that.firstPca);
            }
        };

        /**
         * Metadata regarding one sixel row.
         */
        private class SixelRow {

            /**
             * A set of colors that are present in this row.
             */
            private BitSet colors;

            /**
             * Public constructor.
             */
            public SixelRow() {
                colors = new BitSet(sixelColors.size());
            }

        }

        /**
         * ColorMatchCache is a FIFO cache that hangs on to the matched
         * palette index color for a RGB color.
         */
        private class ColorMatchCache {

            /**
             * Maximum size of the cache.
             */
            private int maxSize = 1024;

            /**
             * The entries stored in the cache.
             */
            private HashMap<Integer, CacheEntry> cache = null;

            /**
             * The order of entries added.
             */
            private ArrayDeque<Integer> cacheEntries = null;

            /**
             * CacheEntry is one entry in the cache.
             */
            private class CacheEntry {
                /**
                 * The cache key.
                 */
                public int key;

                /**
                 * The cache data.
                 */
                public int data;

                /**
                 * Public constructor.
                 *
                 * @param key the cache entry key
                 * @param data the cache entry data
                 */
                public CacheEntry(final int key, final int data) {
                    this.key = key;
                    this.data = data;
                }
            }

            /**
             * Public constructor.
             *
             * @param maxSize the maximum size of the cache
             */
            public ColorMatchCache(final int maxSize) {
                this.maxSize = maxSize;
                cache = new HashMap<Integer, CacheEntry>(maxSize);
                cacheEntries = new ArrayDeque<Integer>(maxSize);
            }

            /**
             * Get an entry from the cache.
             *
             * @param color the RGB color
             * @return the palette index, or -1 if this RGB color is not in
             * the cache
             */
            public int get(final int color) {
                CacheEntry entry = cache.get(color);
                if (entry == null) {
                    return -1;
                }
                return entry.data;
            }

            /**
             * Put an entry into the cache.
             *
             * @param color the RGB color
             * @param data the palette index
             */
            public void put(final int color, final int data) {

                assert (cache.size() <= maxSize);
                if (cache.size() == maxSize) {
                    // Cache is at limit, evict oldest entry.
                    int keyToRemove = cacheEntries.removeFirst();
                    assert (keyToRemove != -1);
                    cache.remove(keyToRemove);
                }
                assert (cache.size() <= maxSize);
                CacheEntry entry = new CacheEntry(color, data);
                assert (color == entry.key);
                cache.put(color, entry);
                cacheEntries.addLast(color);
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
         * Color palette for sixel output, sorted low to high by the first
         * principal component.
         */
        private List<PcaColor> pcaColors = null;

        /**
         * Comparator used for the first principal component search.
         */
        private final Comparator<PcaColor> nearby = new Comparator<PcaColor>(){
            @Override
            public int compare(final PcaColor a, final PcaColor b) {
                return Double.compare(a.firstPca, b.firstPca);
            }
        };

        /**
         * A map of recent matching colors.
         */
        private ColorMatchCache recentColorMatch;

        /**
         * The key used for binary search.
         */
        private PcaColor pcaKey = new PcaColor(0, 0, 0, 0);

        /**
         * The index into pcaColors last found by binary search.
         */
        private int lastPcaSearchIndex = 0;

        /**
         * The distance along the first principal component axis at which two
         * colors are deemed close to each other.
         */
        private double pcaThreshold = 0;

        /**
         * The PCA change of basis matrix.
         */
        private double [][] PCA;

        /**
         * Map of colors used in the image by RGB.
         */
        private HashMap<Integer, ColorIdx> colorMap = null;

        /**
         * Type of color quantization used.
         *
         * -1 = direct map indexed; 0 = direct map; 1 = median cut.
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
         * The sixel rows of this image.
         */
        private SixelRow [] sixelRows;

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
            assert (image.getWidth() > 0);
            assert (image.getHeight() > 0);

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
            sixelRows = new SixelRow[(image.getHeight() / 6) + 1];
            for (int i = 0; i < sixelRows.length; i++) {
                sixelRows[i] = new SixelRow();
            }

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
            int totalPixels = sixelImageWidth * sixelImageHeight;

            if (verbosity >= 1) {
                System.err.printf("Image: %dx%d (%d px)\n",
                    sixelImageWidth, sixelImageHeight, totalPixels);
                System.err.printf("       bpp %d transparent %s allowed %s\n",
                    image.getColorModel().getPixelSize(),
                    transparent, allowTransparent);
            }

            // Sample the colors.  We will be taking SAMPLE_SIZE'd pixel
            // bands uniformly through the image data, with numColors bands.
            final int SAMPLE_SIZE = 4;
            int stride = 0;
            if (totalPixels > SAMPLE_SIZE * numColors) {
                stride = Math.max(0, totalPixels / numColors);
            }
            if (verbosity >= 1) {
                System.err.printf("Sampling %d pixels per color stride=%d\n",
                    SAMPLE_SIZE, stride);
            }

            int [] rgbArray = image.getRGB(0, 0,
                sixelImageWidth, sixelImageHeight, null, 0, sixelImageWidth);
            sixelImage = rgbArray;
            colorMap = new HashMap<Integer, ColorIdx>(sixelImageWidth * sixelImageHeight);
            int transparent_count = 0;

            int strideI = 0;
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
                if ((stride == 0) || (strideI < SAMPLE_SIZE)) {
                    ColorIdx color = colorMap.get(sixelRGB & 0x00FFFFFF);
                    if (color == null) {
                        color = new ColorIdx(sixelRGB & 0x00FFFFFF);
                        colorMap.put(sixelRGB & 0x00FFFFFF, color);
                    } else {
                        color.count++;
                    }
                }
                if (strideI >= stride) {
                    strideI = 0;
                } else {
                    strideI++;
                }
            } // for (int i = 0; i < rgbArray.length; i++)

            /*
             * At this point:
             *
             * 1. The image data is mapped to the 101^3 sixel color space.
             *
             * 2. Any pixels with partial transparency below ALPHA_OPAQUE are
             *    fully transparent (and pink).
             *
             * 3. The sampled color map is populated with up to SAMPLE_SIZE *
             *    numColors samples.
             */

            if (verbosity >= 1) {
                System.err.printf("# colors in image (sampled): %d palette size %d\n",
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
             *   and we know that because we sampled every pixel (stride ==
             *   0), then just do a straight 1-1 mapping.
             *
             * - Otherwise use median cut.
             */
            if ((stride == 0) && (colorMap.size() <= numColors)) {
                quantizationType = 0;
                directMap();
            } else if (true || (colorMap.size() <= numColors * 10)) {
                // For now, direct map and median cut are all we get.
                quantizationType = 1;
                medianCut();
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
            SixelRow sixelRow;
            for (int y = 0; y < sixelImageHeight; y++) {
                sixelRow = sixelRows[y / 6];
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
                        sixelRow.colors.set(idx);
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
                colorMap.get(sixelColors.get(i)).directMapIndex = i;
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

            // Populate the "total" bucket.
            Bucket bucket = new Bucket(colorMap.size());
            for (ColorIdx color: colorMap.values()) {
                bucket.add(color);

                int rgb = color.color;
                int red   = (rgb >>> 16) & 0xFF;
                int green = (rgb >>>  8) & 0xFF;
                int blue  =  rgb         & 0xFF;
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

            // Buckets are partitioned.  Now assign them to the sixel
            // palette, and also find the darkest and lightest buckets and
            // assign them to black and white.
            int idx = 0;
            int darkest = Integer.MAX_VALUE;
            int lightest = 0;
            int darkestIdx = -1;
            int lightestIdx = -1;
            final int diff = 1000;
            for (Bucket b: buckets) {
                int rgb = b.average();
                b.index = idx;
                int red   = (rgb >>> 16) & 0xFF;
                int green = (rgb >>>  8) & 0xFF;
                int blue  =  rgb         & 0xFF;
                int color2 = (red * red) + (green * green) + (blue * blue);
                if (color2 < diff) {
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
                sixelColors.add(rgb);
                idx++;
            }
            if (darkestIdx != -1) {
                sixelColors.set(darkestIdx, 0xFF000000);
            }
            if (lightestIdx != -1) {
                sixelColors.set(lightestIdx, 0xFF646464);
            }

            quantizationDone = true;
            if (verbosity >= 5) {
                System.err.printf("COLOR MAP: %d entries\n",
                    sixelColors.size());
                for (int i = 0; i < sixelColors.size(); i++) {
                    System.err.printf("   %03d %08x\n", i,
                        sixelColors.get(i));
                }
            }

            // Now that colors have been established, build the search
            // structure for them.
            buildSearchMap();

            if (timings != null) {
                timings.buildColorMapTime = System.nanoTime();
            }
        }

        /**
         * Sort the palette colors by principal component so that they can be
         * located quickly in dither().  This approach was first brought to
         * open-source by Hans Petter Jansson's chafa project:
         * https://hpjansson.org/chafa/ .
         */
        private void buildSearchMap() {
            // Build the covariance matrix and find its eigenvalues.  These
            // will be the principal components.
            //
            // (The computational chemist in me is SO HAPPY that we finally
            // have an eigenvalue solver in Jexer. 💗)
            double [][] A = new double[3][3];

            double redMean   = 0;
            double greenMean = 0;
            double blueMean  = 0;
            int n = sixelColors.size();
            for (int rgbColor: sixelColors) {
                redMean   += (rgbColor >>> 16) & 0xFF;
                greenMean += (rgbColor >>>  8) & 0xFF;
                blueMean  +=  rgbColor         & 0xFF;
            }
            redMean   /= n;
            greenMean /= n;
            blueMean  /= n;
            double covRedRed     = 0;
            double covRedGreen   = 0;
            double covRedBlue    = 0;
            double covGreenGreen = 0;
            double covGreenBlue  = 0;
            double covBlueBlue   = 0;
            for (int rgbColor: sixelColors) {
                int red   = (rgbColor >>> 16) & 0xFF;
                int green = (rgbColor >>>  8) & 0xFF;
                int blue  =  rgbColor         & 0xFF;

                covRedRed     += (  red -   redMean) * (  red -   redMean);
                covRedGreen   += (  red -   redMean) * (green - greenMean);
                covRedBlue    += (  red -   redMean) * ( blue -  blueMean);
                covGreenGreen += (green - greenMean) * (green - greenMean);
                covGreenBlue  += (green - greenMean) * ( blue -  blueMean);
                covBlueBlue   += ( blue -  blueMean) * ( blue -  blueMean);
            }
            covRedRed     /= (n - 1);
            covRedGreen   /= (n - 1);
            covRedBlue    /= (n - 1);
            covGreenGreen /= (n - 1);
            covGreenBlue  /= (n - 1);
            covBlueBlue   /= (n - 1);

            A[0][0] = covRedRed;
            A[0][1] = covRedGreen;
            A[0][2] = covRedBlue;
            A[1][0] = covRedGreen;
            A[1][1] = covGreenGreen;
            A[1][2] = covGreenBlue;
            A[2][0] = covRedGreen;
            A[2][1] = covGreenBlue;
            A[2][2] = covBlueBlue;

            double [][] V = new double[3][3];
            double [] d = new double[3];

            MathUtils.eigen3(A, V, d);

            if (verbosity >= 1) {
                System.err.printf("PCA => eigenvalues: %8.4f %8.4f %8.4f\n",
                    d[0], d[1], d[2]);
                double pcaSum = d[0] + d[1] + d[2];
                System.err.printf("                   %7.2f%%  %7.2f%%  %7.2f%%\n",
                    Math.abs((d[0] / pcaSum) * 100.0),
                    Math.abs((d[1] / pcaSum) * 100.0),
                    Math.abs((d[2] / pcaSum) * 100.0));

                System.err.printf("PCA => [ %8.4f %8.4f %8.4f]\n       [ %8.4f %8.4f %8.4f]\n       [ %8.4f %8.4f %8.4f]\n",
                    V[0][0], V[0][1], V[0][2],
                    V[1][0], V[1][1], V[1][2],
                    V[2][0], V[2][1], V[2][2]
                );
            }

            // The principal component scalars are in d[3] (first), d[2]
            // (second), and d[1] (third).

            // Now sort sixelColors by first PCA.

            // This involves creation of a "change of basis matrix from RGB
            // to PCA".  If I am reading page 3 of
            // http://www.math.lsa.umich.edu/~kesmith/CoordinateChange.pdf
            // correctly, then V _is_ the change of basis matrix because we
            // know that all of its vectors are orthogonal.
            PCA = V;
            pcaColors = new ArrayList<PcaColor>(sixelColors.size());
            int idx = 0;
            for (int rgbColor: sixelColors) {
                pcaColors.add(new PcaColor(idx, firstPca(rgbColor),
                        secondPca(rgbColor), thirdPca(rgbColor)));
                idx++;
            }
            Collections.sort(pcaColors);

            // A first principal component difference within 8 indices will
            // be deemed in the same neighborhood.
            pcaThreshold = ((pcaColors.get(idx - 1).firstPca - pcaColors.get(0).firstPca) / idx) * 8.0;

            // Allow up the last 8192 colors to be re-used, or 10% of the
            // image size.
            recentColorMatch = new ColorMatchCache(Math.min(
                sixelImageWidth * sixelImageHeight / 10, 8192));
        }

        /**
         * Find the first principal component value of an RGB color.
         *
         * @param color the RGB color
         * @return the color's PCA1 coordinate in PCA space
         */
        private double firstPca(final int color) {
            int red   = (color >>> 16) & 0xFF;
            int green = (color >>>  8) & 0xFF;
            int blue  =  color         & 0xFF;

            // Due to how MathUtils.eigen3() sorts the eigenvalues, the first
            // principal component is the last column in the PCA matrix.
            return (PCA[2][0] * red) + (PCA[2][1] * green) + (PCA[2][2] * blue);
        }

        /**
         * Find the second principal component value of an RGB color.
         *
         * @param color the RGB color
         * @return the color's PCA1 coordinate in PCA space
         */
        private double secondPca(final int color) {
            int red   = (color >>> 16) & 0xFF;
            int green = (color >>>  8) & 0xFF;
            int blue  =  color         & 0xFF;

            // Due to how MathUtils.eigen3() sorts the eigenvalues, the second
            // principal component is the middle column in the PCA matrix.
            return (PCA[1][0] * red) + (PCA[1][1] * green) + (PCA[1][2] * blue);
        }

        /**
         * Find the third principal component value of an RGB color.
         *
         * @param color the RGB color
         * @return the color's PCA1 coordinate in PCA space
         */
        private double thirdPca(final int color) {
            int red   = (color >>> 16) & 0xFF;
            int green = (color >>>  8) & 0xFF;
            int blue  =  color         & 0xFF;

            // Due to how MathUtils.eigen3() sorts the eigenvalues, the third
            // principal component is the first column in the PCA matrix.
            return (PCA[0][0] * red) + (PCA[0][1] * green) + (PCA[0][2] * blue);
        }

        /**
         * Search through the palette and find the best RGB match in the
         * sixel palette.  This particular approach was first done by Hans
         * Petter Jansson's chafa project: https://hpjansson.org/chafa/ .
         * The palette colors have been sorted by their principal component
         * (see principal component analysis), such that a binary search can
         * quickly find the region where the closest matching color resides.
         * One can then search forward and backward to find all nearby
         * colors.
         *
         * @param red the red component, from 0-100
         * @param green the green component, from 0-100
         * @param blue the blue component, from 0-100
         * @return the palette index of the nearest color in RGB space
         */
        private int findNearestColor(final int red, final int green,
            final int blue) {

            // Search pcaColors by first PCA.
            double pca1 = (PCA[2][0] * red)
                        + (PCA[2][1] * green)
                        + (PCA[2][2] * blue);

            PcaColor lastPcaColor = pcaColors.get(lastPcaSearchIndex);
            PcaColor centerPca = null;
            int idx = lastPcaSearchIndex;
            if (Math.abs(lastPcaColor.firstPca - pca1) < pcaThreshold) {
                // Skip the binary search, we are already close.
                centerPca = lastPcaColor;
            } else {

                // This version uses standard Java binary search.  It's
                // faster than my first attempt, so it can stay.
                pcaKey.firstPca = pca1;

                // pcaIndex will almost certainly come back negative, because
                // doubles cannot exactly be equal in practice.
                int pcaIndex = Math.abs(Collections.binarySearch(pcaColors,
                        pcaKey, nearby));

                // idx is near the center of the neighborhood.
                idx = Math.max(0, Math.min(sixelColors.size() - 1, pcaIndex));
                lastPcaSearchIndex = idx;
                centerPca = pcaColors.get(idx);
            }

            // Begin at the starting point.
            int result = centerPca.sixelIndex;
            int bestRgbDistance = 0;
            {
                int sixelRgb = sixelColors.get(centerPca.sixelIndex);
                int red2   = (sixelRgb >>> 16) & 0xFF;
                int green2 = (sixelRgb >>>  8) & 0xFF;
                int blue2  =  sixelRgb         & 0xFF;
                bestRgbDistance = (red2 - red) * (red2 - red)
                                + (green2 - green) * (green2 - green)
                                + (blue2 - blue) * (blue2 - blue);
            }

            // Now search up and down along pca1 finding colors that are a
            // closer match in PCA space than the color found by binary
            // search.

            double pca2 = (PCA[1][0] * red)
                        + (PCA[1][1] * green)
                        + (PCA[1][2] * blue);
            double pca3 = (PCA[0][0] * red)
                        + (PCA[0][1] * green)
                        + (PCA[0][2] * blue);

            // The distance between the search color and the best-fit color
            // in PCA space.
            final double centerPca1 = centerPca.firstPca;
            final double centerPca2 = centerPca.secondPca;
            final double centerPca3 = centerPca.thirdPca;
            double pcaDistance = (centerPca1 - pca1) * (centerPca1 - pca1)
                               + (centerPca2 - pca2) * (centerPca2 - pca2)
                               + (centerPca3 - pca3) * (centerPca3 - pca3);

            // The distance between the search color and the colors being
            // looked at above/below in PCA space.
            double abovePcaDistance = 0;
            double belowPcaDistance = 0;

            // The starting point.
            int below = idx;
            int above = idx;
            int n = pcaColors.size();

            // Search up
            while (above + 1 < n) {
                above++;
                final PcaColor abovePca = pcaColors.get(above);
                final double abovePca1 = abovePca.firstPca;
                final double abovePca2 = abovePca.secondPca;
                final double abovePca3 = abovePca.thirdPca;
                abovePcaDistance = (abovePca1 - pca1) * (abovePca1 - pca1)
                                 + (abovePca2 - pca2) * (abovePca2 - pca2)
                                 + (abovePca3 - pca3) * (abovePca3 - pca3);
                if (abovePcaDistance <= pcaDistance) {
                    // This is a valid point to look at.
                    int sixelRgb = sixelColors.get(abovePca.sixelIndex);
                    int red2   = (sixelRgb >>> 16) & 0xFF;
                    int green2 = (sixelRgb >>>  8) & 0xFF;
                    int blue2  =  sixelRgb         & 0xFF;
                    int rgbDistance = (red2 - red) * (red2 - red)
                                    + (green2 - green) * (green2 - green)
                                    + (blue2 - blue) * (blue2 - blue);
                    if (rgbDistance < bestRgbDistance) {
                        result = abovePca.sixelIndex;
                        bestRgbDistance = rgbDistance;
                    }
                    pcaDistance = abovePcaDistance;
                }
                if ((abovePca.firstPca - pca1) > pcaDistance) {
                    // There are no closer points in that direction.
                    break;
                }
            }

            // Search down
            while (below > 0) {
                below--;
                final PcaColor belowPca = pcaColors.get(below);
                final double belowPca1 = belowPca.firstPca;
                final double belowPca2 = belowPca.secondPca;
                final double belowPca3 = belowPca.thirdPca;
                belowPcaDistance = (belowPca1 - pca1) * (belowPca1 - pca1)
                                 + (belowPca2 - pca2) * (belowPca2 - pca2)
                                 + (belowPca3 - pca3) * (belowPca3 - pca3);
                if (belowPcaDistance <= pcaDistance) {
                    // This is a valid point to look at.
                    int sixelRgb = sixelColors.get(belowPca.sixelIndex);
                    int red2   = (sixelRgb >>> 16) & 0xFF;
                    int green2 = (sixelRgb >>>  8) & 0xFF;
                    int blue2  =  sixelRgb         & 0xFF;
                    int rgbDistance = (red2 - red) * (red2 - red)
                                    + (green2 - green) * (green2 - green)
                                    + (blue2 - blue) * (blue2 - blue);
                    if (rgbDistance < bestRgbDistance) {
                        result = belowPca.sixelIndex;
                        bestRgbDistance = rgbDistance;
                    }
                    pcaDistance = belowPcaDistance;
                }
                if ((pca1 - belowPca.firstPca) > pcaDistance) {
                    // There are no closer points in that direction.
                    break;
                }
            }
            // No more valid points in the neighborhood.
            return result;
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
         * Dither an image to a paletteSize palette.  The dithered
         * image cells will contain indexes into the palette.
         *
         * @return the dithered image rgb data.  Every pixel is an index into
         * the palette.
         */
        public int [] ditherImage() {
            int [] rgbArray = sixelImage;
            if (noDither) {
                return rgbArray;
            }

            int height = sixelImageHeight;
            int width = sixelImageWidth;
            SixelRow sixelRow;
            for (int imageY = 0; imageY < height; imageY++) {
                sixelRow = sixelRows[imageY / 6];
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
                    int colorIdx = 0;
                    int color = oldPixel & 0x00FFFFFF;
                    if (quantizationType == 0) {
                        colorIdx = colorMap.get(color).directMapIndex;
                    } else {
                        // See if this entry has been seen before recently.
                        colorIdx = recentColorMatch.get(color);
                        if (colorIdx < 0) {
                            // We need to search for it.
                            int red   = (color >>> 16) & 0xFF;
                            int green = (color >>>  8) & 0xFF;
                            int blue  =  color         & 0xFF;
                            colorIdx = findNearestColor(red, green, blue);
                            recentColorMatch.put(color, colorIdx);
                        }
                    }

                    assert (colorIdx >= 0);
                    assert (colorIdx < sixelColors.size());
                    int newPixel = sixelColors.get(colorIdx);
                    rgbArray[imageX + (width * imageY)] = colorIdx;
                    sixelRow.colors.set(colorIdx);

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
         */
        public void emitPalette(final StringBuilder sb) {
            for (int i = 0; i < sixelColors.size(); i++) {
                int sixelColor = sixelColors.get(i);
                int red   = ((sixelColor >>> 16) & 0xFF);
                int green = ((sixelColor >>>  8) & 0xFF);
                int blue  = ( sixelColor         & 0xFF);

                sb.append("#");
                sb.append(Integer.toString(i));
                sb.append(";2;");
                sb.append(Integer.toString(red));
                sb.append(";");
                sb.append(Integer.toString(green));
                sb.append(";");
                sb.append(Integer.toString(blue));
            }
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
     * 1024.  For HQ encoder the default is 128.
     */
    private int paletteSize = 128;

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
        int paletteSize = 128;
        try {
            paletteSize = Integer.parseInt(System.getProperty(
                "jexer.ECMA48.sixelPaletteSize", "128"));
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

        // Emit the palette.
        palette.emitPalette(sb);

        // Render the entire row of cells.
        int width = bitmap.getWidth();

        int colorsN = palette.sixelColors.size();
        for (int currentRow = 0; currentRow < fullHeight; currentRow += 6) {
            Palette.SixelRow sixelRow = palette.sixelRows[currentRow / 6];

            for (int i = 0; i < colorsN; i++) {
                if (!sixelRow.colors.get(i)) {
                    continue;
                }

                /*
                 * We want to avoid tons of memory access, so for each color:
                 *
                 * 1. Create an array for the full width to collect the sum.
                 *
                 * 2. Go down the full row, adding up on the sums.  You have
                 *    to do this up to six times.
                 *
                 * 3. Go one last time down the array and emit the sums.
                 *
                 * It doesn't look that much more complicated than the naive
                 * sum, but should be faster as many cells are captured on
                 * one memory access.
                 */
                int [] row = new int[width];
                for (int j = 0;
                     (j < 6) && (currentRow + j < fullHeight);
                     j++) {

                    int base = width * (currentRow + j);
                    int value = 1 << j;
                    for (int imageX = 0; imageX < width; imageX++) {
                        // Is there was a way to do this without the if?
                        if (rgbArray[base + imageX] == i) {
                            row[imageX] += value;
                        }
                    }
                }

                // Set to the beginning of scan line for the next set of
                // colored pixels, and select the color.
                sb.append("$#");
                sb.append(Integer.toString(i));

                int oldData = -1;
                int oldDataCount = 0;
                for (int imageX = 0; imageX < width; imageX++) {
                    int data = row[imageX];

                    assert (data >= 0);
                    assert (data < 64);
                    data += 63;

                    if (data == oldData) {
                        oldDataCount++;
                    } else {
                        if (oldDataCount == 1) {
                            // assert (oldData != -1);
                            sb.append((char) oldData);
                        } else if (oldDataCount > 1) {
                            sb.append("!");
                            sb.append(Integer.toString(oldDataCount));
                            // assert (oldData != -1);
                            sb.append((char) oldData);
                        }
                        oldDataCount = 1;
                        oldData = data;
                    }

                } // for (int imageX = 0; imageX < width; imageX++)

                // Emit the last sequence.
                if (oldDataCount == 1) {
                    // assert (oldData != -1);
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
        sb.insert(0, String.format("\"1;1;%d;%d", bitmap.getWidth(),
                bitmap.getHeight()));

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
            System.err.println("USAGE: java jexer.backend.HQSixelEncoder [ -p | -t | -v | -vv ] { file1 [ file2 ... ] }");
            System.exit(-1);
        }

        HQSixelEncoder encoder = new HQSixelEncoder();
        int successCount = 0;
        boolean allowTransparent = true;
        boolean performance = false;
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
            if ((i == 0) && args[i].equals("-p")) {
                encoder.verbosity = 1;
                encoder.doTimings = true;
                performance = true;
                continue;
            }

            try {
                BufferedImage image = ImageIO.read(new FileInputStream(args[i]));
                int count = 1;
                if (performance) {
                    count = 20;
                }
                for (int j = 0; j < count; j++) {
                    // Put together the image.
                    StringBuilder sb = new StringBuilder();
                    SixelResult result = encoder.toSixelResult(image,
                        allowTransparent);
                    result.palette.emitPalette(sb);
                    sb.append(result.encodedImage);
                    sb.append("\033\\");
                    // If there are transparent pixels, we need to note that
                    // at the beginning.
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

                } // for (int j = 0; j < count; j++)
            } catch (Exception e) {
                System.err.println("Error reading file:");
                e.printStackTrace();
            }

        } // for (int i = 0; i < args.length; i++)

        System.out.print("\033[?1070h");
        System.out.flush();
        if (successCount == args.length) {
            System.exit(0);
        } else {
            System.exit(successCount);
        }
    }

}
