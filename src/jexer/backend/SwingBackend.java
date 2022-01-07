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

import java.awt.Font;
import javax.swing.JComponent;

import jexer.bits.CellAttributes;

/**
 * This class uses standard Swing calls to handle screen, keyboard, and mouse
 * I/O.
 */
public class SwingBackend extends GenericBackend {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  The window will be 80x25 with font size 20 pts.
     */
    public SwingBackend() {
        this(null, 80, 25, 20);
    }

    /**
     * Public constructor.  The window will be 80x25 with font size 20 pts.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     */
    public SwingBackend(final Object listener) {
        this(listener, 80, 25, 20);
    }

    /**
     * Public constructor will spawn a new JFrame with font size 20 pts.
     *
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     */
    public SwingBackend(final int windowWidth, final int windowHeight) {
        this(null, windowWidth, windowHeight, 20);
    }

    /**
     * Public constructor will spawn a new JFrame.
     *
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points.  Good values to pick are: 16, 20,
     * 22, and 24.
     */
    public SwingBackend(final int windowWidth, final int windowHeight,
        final int fontSize) {

        this(null, windowWidth, windowHeight, fontSize);
    }

    /**
     * Public constructor will spawn a new JFrame.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points.  Good values to pick are: 16, 20,
     * 22, and 24.
     */
    public SwingBackend(final Object listener, final int windowWidth,
        final int windowHeight, final int fontSize) {

        // Create a Swing backend using a JFrame
        terminal = new SwingTerminal(this, windowWidth, windowHeight, fontSize,
            listener);
        ((SwingTerminal) terminal).setBackend(this);

        // Hang onto the session info
        this.sessionInfo = ((SwingTerminal) terminal).getSessionInfo();

        // SwingTerminal is the screen too
        screen = (SwingTerminal) terminal;
    }

    /**
     * Public constructor will render onto a JComponent.
     *
     * @param component the Swing component to render to
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @param fontSize the size in points.  Good values to pick are: 16, 20,
     * 22, and 24.
     */
    public SwingBackend(final JComponent component, final Object listener,
        final int windowWidth, final int windowHeight, final int fontSize) {

        // Create a Swing backend using a JComponent
        terminal = new SwingTerminal(this, component, windowWidth, windowHeight,
            fontSize, listener);
        ((SwingTerminal) terminal).setBackend(this);

        // Hang onto the session info
        this.sessionInfo = ((SwingTerminal) terminal).getSessionInfo();

        // SwingTerminal is the screen too
        screen = (SwingTerminal) terminal;
    }

    // ------------------------------------------------------------------------
    // SwingBackend -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set to a new font, and resize the screen to match its dimensions.
     *
     * @param font the new font
     */
    public void setFont(final Font font) {
        ((SwingTerminal) terminal).setFont(font);
    }

    /**
     * Get the number of millis to wait before switching the blink from
     * visible to invisible.
     *
     * @return the number of milli to wait before switching the blink from
     * visible to invisible
     */
    public long getBlinkMillis() {
        return ((SwingTerminal) terminal).getBlinkMillis();
    }

    /**
     * Getter for the underlying Swing component.
     *
     * @return the SwingComponent
     */
    public SwingComponent getSwingComponent() {
        return ((SwingTerminal) terminal).getSwingComponent();
    }

    /**
     * Check if backend will support incomplete image fragments over text
     * display.
     *
     * @return true if images can partially obscure text
     */
    @Override
    public boolean isImagesOverText() {
        return ((SwingTerminal) terminal).isImagesOverText();
    }

    /**
     * Check if backend is reporting pixel-based mouse position.
     *
     * @return true if single-pixel mouse movements are reported
     */
    @Override
    public boolean isPixelMouse() {
        return ((SwingTerminal) terminal).isPixelMouse();
    }

    /**
     * Set request for backend to report pixel-based mouse position.
     *
     * @param pixelMouse if true, single-pixel mouse movements will be
     * reported, if the backend supports it
     */
    @Override
    public void setPixelMouse(final boolean pixelMouse) {
        if (pixelMouse != ((SwingTerminal) terminal).isPixelMouse()) {
            ((SwingTerminal) terminal).setPixelMouse(pixelMouse);
        }
    }

    /**
     * Set the mouse pointer (cursor) style.
     *
     * @param mouseStyle the pointer style string, one of: "default", "none",
     * "hand", "text", "move", or "crosshair"
     */
    @Override
    public void setMouseStyle(final String mouseStyle) {
        ((SwingTerminal) terminal).setMouseStyle(mouseStyle);
    }

    /**
     * Convert a CellAttributes foreground color to an AWT Color.
     *
     * @param attr the text attributes
     * @return the AWT Color
     */
    public java.awt.Color attrToForegroundColor(final CellAttributes attr) {
        // Use Swing colors.
        return SwingTerminal.attrToForegroundColor(attr);
    }

    /**
     * Convert a CellAttributes background color to an AWT Color.
     *
     * @param attr the text attributes
     * @return the AWT Color
     */
    public java.awt.Color attrToBackgroundColor(final CellAttributes attr) {
        // Use Swing colors.
        return SwingTerminal.attrToBackgroundColor(attr);
    }

}
