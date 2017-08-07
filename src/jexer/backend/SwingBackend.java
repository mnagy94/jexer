/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2017 Kevin Lamonte
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

import java.util.List;
import javax.swing.JComponent;

import jexer.event.TInputEvent;

/**
 * This class uses standard Swing calls to handle screen, keyboard, and mouse
 * I/O.
 */
public final class SwingBackend extends GenericBackend {

    /**
     * Input events are processed by this Terminal.
     */
    private SwingTerminal terminal;

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
        terminal = new SwingTerminal(windowWidth, windowHeight, fontSize,
            listener);

        // Hang onto the session info
        this.sessionInfo = terminal.getSessionInfo();

        // SwingTerminal is the screen too
        screen = terminal;
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
        terminal = new SwingTerminal(component, windowWidth, windowHeight,
            fontSize, listener);

        // Hang onto the session info
        this.sessionInfo = terminal.getSessionInfo();

        // SwingTerminal is the screen too
        screen = terminal;
    }

    /**
     * Sync the logical screen to the physical device.
     */
    @Override
    public void flushScreen() {
        screen.flushPhysical();
    }

    /**
     * Get keyboard, mouse, and screen resize events.
     *
     * @param queue list to append new events to
     */
    @Override
    public void getEvents(final List<TInputEvent> queue) {
        if (terminal.hasEvents()) {
            terminal.getEvents(queue);
        }
    }

    /**
     * Close the I/O, restore the console, etc.
     */
    @Override
    public void shutdown() {
        terminal.closeTerminal();
    }

    /**
     * Set the window title.
     *
     * @param title the new title
     */
    @Override
    public void setTitle(final String title) {
        screen.setTitle(title);
    }

    /**
     * Set listener to a different Object.
     *
     * @param listener the new listening object that run() wakes up on new
     * input
     */
    public void setListener(final Object listener) {
        terminal.setListener(listener);
    }

}
