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
package jexer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import static jexer.TKeypress.*;

/**
 * TTextPictureWindow shows an ASCII/ANSI art file with scrollbars.
 */
public class TTextPictureWindow extends TScrollableWindow {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The number of lines to scroll on mouse wheel up/down.
     */
    private static final int wheelScrollSize = 3;

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Hang onto the TTextPicture so I can resize it with the window.
     */
    private TTextPicture pictureField;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor opens a file.
     *
     * @param parent the main application
     * @param filename the file to open
     * @throws IOException if a java.io operation throws
     */
    public TTextPictureWindow(final TApplication parent,
        final String filename) throws IOException {

        this(parent, filename, 0, 0, 82, 27);
    }

    /**
     * Public constructor opens a file.
     *
     * @param parent the main application
     * @param filename the file to open
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @throws IOException if a java.io operation throws
     */
    public TTextPictureWindow(final TApplication parent, final String filename,
        final int x, final int y, final int width,
        final int height) throws IOException {

        super(parent, filename, x, y, width, height, RESIZABLE);

        pictureField = new TTextPicture(this, filename, 0, 0,
            getWidth() - 2, getHeight() - 2);
        setTitle(filename);

        setupAfterTextPicture();
    }

    /**
     * Setup other fields after the picture is created.
     */
    private void setupAfterTextPicture() {
        if (pictureField.getHeight() < getHeight() - 2) {
            setHeight(pictureField.getHeight() + 2);
        }
        if (pictureField.getWidth() < getWidth() - 2) {
            setWidth(pictureField.getWidth() + 2);
        }

        hScroller = new THScroller(this,
            Math.min(Math.max(0, getWidth() - 17), 17),
            getHeight() - 2,
            getWidth() - Math.min(Math.max(0, getWidth() - 17), 17) - 3);
        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);
        reflowData();
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        super.onMouseDown(mouse);
        setVerticalValue(pictureField.getVerticalValue());
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        super.onMouseUp(mouse);

        if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
            // Clicked/dragged on vertical scrollbar
            pictureField.setVerticalValue(getVerticalValue());
        }
        if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
            // Clicked/dragged on horizontal scrollbar
            pictureField.setHorizontalValue(getHorizontalValue());
        }
    }

    /**
     * Method that subclasses can override to handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        super.onMouseMotion(mouse);

        if (mouse.isMouse1() && mouseOnVerticalScroller(mouse)) {
            // Clicked/dragged on vertical scrollbar
            pictureField.setVerticalValue(getVerticalValue());
        }
        if (mouse.isMouse1() && mouseOnHorizontalScroller(mouse)) {
            // Clicked/dragged on horizontal scrollbar
            pictureField.setHorizontalValue(getHorizontalValue());
        }
    }

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        if (event.getType() == TResizeEvent.Type.WIDGET) {
            // Resize the picture field
            TResizeEvent pictureSize = new TResizeEvent(event.getBackend(),
                TResizeEvent.Type.WIDGET, event.getWidth() - 2,
                event.getHeight() - 2);
            pictureField.onResize(pictureSize);

            // Have TScrollableWindow handle the scrollbars
            super.onResize(event);
            return;
        }

        // Pass to children instead
        for (TWidget widget: getChildren()) {
            widget.onResize(event);
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbUp)) {
            verticalDecrement();
            pictureField.setVerticalValue(getVerticalValue());
            return;
        }
        if (keypress.equals(kbDown)) {
            verticalIncrement();
            pictureField.setVerticalValue(getVerticalValue());
            return;
        }
        if (keypress.equals(kbPgUp)) {
            bigVerticalDecrement();
            pictureField.setVerticalValue(getVerticalValue());
            return;
        }
        if (keypress.equals(kbPgDn)) {
            bigVerticalIncrement();
            pictureField.setVerticalValue(getVerticalValue());
            return;
        }
        if (keypress.equals(kbRight)) {
            horizontalIncrement();
            pictureField.setHorizontalValue(getHorizontalValue());
            return;
        }
        if (keypress.equals(kbLeft)) {
            horizontalDecrement();
            pictureField.setHorizontalValue(getHorizontalValue());
            return;
        }

        // We did not take it, let the TTextPicture instance see it.
        super.onKeypress(keypress);
    }

    // ------------------------------------------------------------------------
    // TScrollableWindow ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the window.
     */
    @Override
    public void draw() {
        reflowData();
        super.draw();
    }

    /**
     * Resize scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        pictureField.reflowData();
        setTopValue(pictureField.getTopValue());
        setBottomValue(pictureField.getBottomValue());
        setVerticalBigChange(pictureField.getVerticalBigChange());
        setVerticalValue(pictureField.getVerticalValue());
        setHorizontalValue(pictureField.getHorizontalValue());

        setRightValue(Math.min(80, 80 - pictureField.getWidth()));
    }

}
