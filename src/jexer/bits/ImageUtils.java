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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * ImageUtils contains methods to:
 *
 *    - Check if an image is fully transparent.
 *
 *    - Scale an image and preserve aspect ratio.
 *
 *    - Open an animated image as an Animation.
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

    /**
     * Open an image as an Animation.
     *
     * @param filename the name of the file that contains an animation
     * @return the animation, or null on error
     */
    public static Animation getAnimation(final String filename) {
        return getAnimation(new File(filename));
    }

    /**
     * Open an image as an Animation.
     *
     * @param file the file that contains an animation
     * @return the animation, or null on error
     */
    public static Animation getAnimation(final File file) {
        try {
            List<BufferedImage> frames = new LinkedList<BufferedImage>();
            List<String> disposals = new LinkedList<String>();
            int delays = 0;

            /*
             * Assume infinite loop.  Finite-count looping in GIFs is an
             * Application Extension made popular by Netscape 2.0: see
             * http://giflib.sourceforge.net/whatsinagif/bits_and_bytes.html
             * .
             *
             * Unfortunately the Sun GIF decoder did not read and expose
             * this.
             */
            int loopCount = 0;

            ImageReader reader = null;
            ImageInputStream stream;
            stream = ImageIO.createImageInputStream(new FileInputStream(file));
            Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
            while (iter.hasNext()) {
                reader = iter.next();
                break;
            }
            if (reader == null) {
                return null;
            }
            reader.setInput(stream);

            int width = -1;
            int height = -1;
            java.awt.Color backgroundColor = null;

            IIOMetadata metadata = reader.getStreamMetadata();
            if (metadata != null) {
                IIOMetadataNode gblRoot;
                gblRoot = (IIOMetadataNode) metadata.getAsTree(metadata.
                    getNativeMetadataFormatName());
                NodeList gblScreenDesc;
                gblScreenDesc = gblRoot.getElementsByTagName(
                        "LogicalScreenDescriptor");
                if ((gblScreenDesc != null)
                    && (gblScreenDesc.getLength() > 0)
                ) {
                    IIOMetadataNode screenDescriptor;
                    screenDescriptor = (IIOMetadataNode) gblScreenDesc.item(0);

                    if (screenDescriptor != null) {
                        width = Integer.parseInt(screenDescriptor.
                            getAttribute("logicalScreenWidth"));
                        height = Integer.parseInt(screenDescriptor.
                            getAttribute("logicalScreenHeight"));
                    }
                }
                NodeList gblColorTable = gblRoot.getElementsByTagName(
                        "GlobalColorTable");

                if ((gblColorTable != null)
                    && (gblColorTable.getLength() > 0)
                ) {
                    IIOMetadataNode colorTable = (IIOMetadataNode) gblColorTable.item(0);

                    if (colorTable != null) {
                        String bgIndex = colorTable.getAttribute(
                                "backgroundColorIndex");

                        IIOMetadataNode color;
                        color = (IIOMetadataNode) colorTable.getFirstChild();
                        while (color != null) {
                            if (color.getAttribute("index").equals(bgIndex)) {
                                int red = Integer.parseInt(
                                        color.getAttribute("red"));
                                int green = Integer.parseInt(
                                        color.getAttribute("green"));
                                int blue = Integer.parseInt(
                                        color.getAttribute("blue"));
                                backgroundColor = new java.awt.Color(red,
                                    green, blue);
                                break;
                            }

                            color = (IIOMetadataNode) color.getNextSibling();
                        }
                    }
                }

            }
            BufferedImage master = null;
            Graphics2D masterGraphics = null;
            int lastx = 0;
            int lasty = 0;
            boolean hasBackround = false;

            for (int frameIndex = 0; ; frameIndex++) {
                BufferedImage image;
                try {
                    image = reader.read(frameIndex);
                } catch (IndexOutOfBoundsException io) {
                    break;
                }
                assert (image != null);

                if (width == -1 || height == -1) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
                IIOMetadataNode root;
                root = (IIOMetadataNode) reader.getImageMetadata(frameIndex).
                        getAsTree("javax_imageio_gif_image_1.0");
                IIOMetadataNode gce;
                gce = (IIOMetadataNode) root.getElementsByTagName(
                        "GraphicControlExtension").item(0);
                int delay = Integer.valueOf(gce.getAttribute("delayTime"));
                String disposal = gce.getAttribute("disposalMethod");

                int x = 0;
                int y = 0;

                if (master == null) {
                    master = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);
                    masterGraphics = master.createGraphics();
                    masterGraphics.setBackground(new java.awt.Color(0, 0, 0, 0));
                    if ((image.getWidth() == width)
                        && (image.getHeight() == height)
                    ) {
                        hasBackround = true;
                    }
                } else {
                    NodeList children = root.getChildNodes();
                    for (int nodeIndex = 0; nodeIndex < children.getLength();
                         nodeIndex++) {

                        Node nodeItem = children.item(nodeIndex);
                        if (nodeItem.getNodeName().equals("ImageDescriptor")) {
                            NamedNodeMap map = nodeItem.getAttributes();
                            x = Integer.valueOf(map.getNamedItem(
                                "imageLeftPosition").getNodeValue());
                            y = Integer.valueOf(map.getNamedItem(
                                "imageTopPosition").getNodeValue());
                        }
                    }
                }
                masterGraphics.drawImage(image, x, y, null);
                lastx = x;
                lasty = y;

                BufferedImage copy = new BufferedImage(master.getColorModel(),
                    master.copyData(null), master.isAlphaPremultiplied(), null);
                frames.add(copy);
                disposals.add(disposal);
                delays += delay;

                if (disposal.equals("restoreToPrevious")) {
                    BufferedImage from = null;
                    for (int i = frameIndex - 1; i >= 0; i--) {
                        if (!disposals.get(i).equals("restoreToPrevious")
                            || (frameIndex == 0)
                        ) {
                            from = frames.get(i);
                            break;
                        }
                    }

                    master = new BufferedImage(from.getColorModel(),
                        from.copyData(null), from.isAlphaPremultiplied(), null);
                    masterGraphics = master.createGraphics();
                    masterGraphics.setBackground(new java.awt.Color(0, 0, 0, 0));
                } else if (disposal.equals("restoreToBackgroundColor")
                    && (backgroundColor != null)) {

                    if (!hasBackround || (frameIndex > 1)) {
                        master.createGraphics().fillRect(lastx, lasty,
                            frames.get(frameIndex - 1).getWidth(),
                            frames.get(frameIndex - 1).getHeight());
                    }
                }
            }
            reader.dispose();

            if (frames.size() == 1) {
                loopCount = 1;
            }
            if (frames.size() == 0) {
                return null;
            }
            Animation animation = new Animation(frames,
                (delays * 10 / frames.size()), loopCount);
            return animation;

        } catch (IOException e) {
            // SQUASH
            return null;
        }
    }

}
