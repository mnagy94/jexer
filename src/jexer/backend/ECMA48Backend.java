/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2016 Kevin Lamonte
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import jexer.event.TInputEvent;
import jexer.io.ECMA48Screen;
import jexer.io.ECMA48Terminal;

/**
 * This class uses an xterm/ANSI X3.64/ECMA-48 type terminal to provide a
 * screen, keyboard, and mouse to TApplication.
 */
public final class ECMA48Backend extends Backend {

    /**
     * Input events are processed by this Terminal.
     */
    private ECMA48Terminal terminal;

    /**
     * Public constructor.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; shutdown() will (blindly!) put System.in in cooked
     * mode.  input is always converted to a Reader with UTF-8 encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    public ECMA48Backend(final Object listener, final InputStream input,
        final OutputStream output) throws UnsupportedEncodingException {

        // Create a terminal and explicitly set stdin into raw mode
        terminal = new ECMA48Terminal(listener, input, output);

        // Keep the terminal's sessionInfo so that TApplication can see it
        sessionInfo = terminal.getSessionInfo();

        // Create a screen
        screen = new ECMA48Screen(terminal);

        // Clear the screen
        terminal.getOutput().write(terminal.clearAll());
        terminal.flush();
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
        terminal.shutdown();
    }

}
