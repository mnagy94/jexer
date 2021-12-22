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
package jexer.tackboard;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jexer.backend.GlyphMaker;
import jexer.backend.Screen;
import jexer.bits.Cell;

/**
 * Tackboard maintains a collection of TackboardItems to draw on a Screen.
 *
 * <p>Each item has a set of X, Y, Z pixel (not text cell) coordinates.  The
 * coordinate system is right-handed: (0, 0, 0) is the top-left pixel on the
 * screen, and positive Z points away from the user.</p>
 *
 * <p>When draw() is called, all the items will be rendered in descending Z,
 * ascending Y, ascending X order (painter's algorithm) onto the cell grid,
 * and using transparent pixels.  If the Screen's backend does not support
 * imagesOverText, then the text of the Cell under transparent images will be
 * rendered via GlyphMaker, which might not look ideal if the internal font
 * is quite different from the terminal's.</p>
 *
 * <p>Tackboards were directly inspired by the Visuals (ncvisuals) of <a
 * href="https://github.com/dankamongmen/notcurses">notcurses</a>. Jexer's
 * performance is unlikely to come close to notcurses, so users requiring
 * low-latency pixel-based rendering are recommended to check out
 * notcurses.</p>
 */
public class Tackboard {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The items on this board.
     */
    private ArrayList<TackboardItem> items = new ArrayList<TackboardItem>();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     */
    public Tackboard() {
        // NOP
    }

    // ------------------------------------------------------------------------
    // Tackboard --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Add an item to the board.
     *
     * @param item the item to add
     */
    public void addItem(final TackboardItem item) {
        items.add(item);
    }

    /**
     * Draw everything to the screen.
     *
     * @param screen the screen to render to
     */
    public void draw(final Screen screen) {
        Collections.sort(items);

        for (TackboardItem item: items) {
            BufferedImage image = item.getImage();
            if (image == null) {
                continue;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            int x = item.getX();
            int y = item.getY();
            int screenWidth = screen.getWidth() * screen.getTextWidth();
            int screenHeight = screen.getHeight() * screen.getTextHeight();
            if ((x + width < 0)
                || (y + height < 0)
                || (x >= screenWidth)
                || (y >= screenHeight)
            ) {
                // No pixels of this item will be visible on the screen.
                continue;
            }

            // Some pixels need to be shown on the screen.
            int cellWidth = screen.getTextWidth();
            int cellHeight = screen.getTextHeight();
            int textX = x / cellWidth;
            int textY = y / cellHeight;
            int dx = x % cellWidth;
            int dy = y % cellHeight;
            int columns = (dx + width) / cellWidth;
            if ((dx + width) % cellWidth > 0) {
                columns++;
            }
            int rows = (dy + height) / cellHeight;
            if ((dy + height) % cellHeight > 0) {
                rows++;
            }
            int ddx = cellWidth - dx;
            int ddy = cellHeight - dy;

            /*
             * At this point:
             *
             * (cellWidth, cellHeight) is the text cell size.
             *
             * (textX, textY) is where the top-left corner of the image would
             * be drawn, in cells.
             *
             * Every cell-sized piece of the image is offset by (dx, dy)
             * pixels on the screen.  The location within image corresponding
             * to a screen position is offset by (ddx, ddy).
             *
             * Drawing the entire image would take {columns} X {rows} to be
             * fully on the screen, accounting for the cell offset.
             *
             */
            for (int sy = 0; sy < rows; sy++) {
                if ((sy + textY < 0) || (sy + textY >= screen.getHeight())) {
                    // This row of cells is off-screen, skip it.
                    continue;
                }
                for (int sx = 0; sx < columns; sx++) {
                    while (sx + textX < 0) {
                        // This cell is off-screen.
                        sx++;
                    }
                    if (sx + textX >= screen.getWidth()) {
                        // This cell is off-screen.
                        break;
                    }

                    // This cell is visible on the screen.
                    BufferedImage newImage;

                    /*
                    System.err.println("rows " + rows + " cols " + columns +
                        " sx " + sx + " sy " + sy + " " +
                        " dx " + dx + " dy " + dy + " " +
                        " ddx " + ddx + " ddy " + ddy + " "
                    );
                     */

                    if (((sy == 0) && (dy > 0))
                        || ((sy == rows - 1) && (dy > 0))
                        || ((sx == 0) && (dx > 0))
                        || ((sx == columns - 1) && (dx > 0))
                    ) {
                        // This cell on the screen will contain a fragment
                        // that does not fill the entire text cell.
                        newImage = new BufferedImage(cellWidth, cellHeight,
                            BufferedImage.TYPE_INT_ARGB);

                        // TODO: copy the RGBA's of the rectangle over the
                        // space it takes up on this cell.


                    } else {
                        /*
                        System.err.println("subImage(" +
                            image.getWidth() + "x" + image.getHeight() + ") " +
                            (((sx - 1) * cellWidth) + ddx) + ", " +
                            (((sy - 1) * cellHeight) + ddy) + ", " +
                            cellWidth + ", " + cellHeight);
                         */

                        newImage = image.getSubimage(((sx - 1) * cellWidth) + ddx,
                            ((sy - 1) * cellHeight) + ddy, cellWidth, cellHeight);
                    }

                    // newImage has the image that needs to be overlaid on
                    // (sx + textX, sy + textY)
                    assert (sx + textX < screen.getWidth());
                    assert (sy + textY < screen.getHeight());

                    Cell oldCell = screen.getCharXY(sx + textX, sy + textY);
                    if (oldCell.isImage()) {
                        // Blit this image over that one.
                        BufferedImage oldImage = oldCell.getImage();
                        java.awt.Graphics gr = oldImage.getGraphics();
                        gr.setColor(java.awt.Color.BLACK);
                        gr.drawImage(newImage, 0, 0, null, null);
                        gr.dispose();
                        oldCell.setImage(oldImage);
                    } else {
                        // Old cell is text only, just add the image.
                        oldCell.setImage(newImage);
                    }
                    screen.putCharXY(sx + textX, sy + textY, oldCell);
                }
            }

        } // for (TackboardItem item: items)
    }

}
