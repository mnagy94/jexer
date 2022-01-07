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

import java.util.List;

import jexer.bits.CellAttributes;
import jexer.event.TInputEvent;

/**
 * HeadlessBackend
 */
public class HeadlessBackend extends LogicalScreen implements Backend {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The session information.
     */
    private SessionInfo sessionInfo;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     */
    public HeadlessBackend() {
        sessionInfo = new TSessionInfo(width, height);
    }

    // ------------------------------------------------------------------------
    // Backend ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Getter for sessionInfo.
     *
     * @return the SessionInfo
     */
    public final SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    /**
     * Get a Screen, which displays the text cells to the user.
     *
     * @return the Screen
     */
    public Screen getScreen() {
        return this;
    }

    /**
     * Subclasses must provide an implementation that syncs the logical
     * screen to the physical device.
     */
    public void flushScreen() {
        // NOP
    }

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the application
     */
    public boolean hasEvents() {
        return false;
    }

    /**
     * Subclasses must provide an implementation to get keyboard, mouse, and
     * screen resize events.
     *
     * @param queue list to append new events to
     */
    public void getEvents(List<TInputEvent> queue) {
        // NOP
    }

    /**
     * Subclasses must provide an implementation that closes sockets,
     * restores console, etc.
     */
    public void shutdown() {
        // NOP
    }

    /**
     * Set listener to a different Object.
     *
     * @param listener the new listening object that run() wakes up on new
     * input
     */
    public void setListener(final Object listener) {
        // NOP
    }

    /**
     * Reload backend options from System properties.
     */
    public void reloadOptions() {
        // NOP
    }

    /**
     * Check if backend is read-only.  For a HeadlessBackend, this is always
     * false.
     *
     * @return true
     */
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Set read-only flag.  This does nothing for HeadlessBackend.
     *
     * @param readOnly ignored
     */
    public void setReadOnly(final boolean readOnly) {
        // NOP
    }

    /**
     * Check if backend will support incomplete image fragments over text
     * display.
     *
     * @return true if images can partially obscure text
     */
    public boolean isImagesOverText() {
        return false;
    }

    /**
     * Check if backend is reporting pixel-based mouse position.
     *
     * @return true if single-pixel mouse movements are reported
     */
    public boolean isPixelMouse() {
        return false;
    }

    /**
     * Set request for backend to report pixel-based mouse position.
     *
     * @param pixelMouse if true, single-pixel mouse movements will be
     * reported, if the backend supports it
     */
    public void setPixelMouse(final boolean pixelMouse) {
        // NOP
    }

    /**
     * Set the mouse pointer (cursor) style.
     *
     * @param mouseStyle the pointer style string, one of: "default", "none",
     * "hand", "text", "move", or "crosshair"
     */
    @Override
    public void setMouseStyle(final String mouseStyle) {
        // NOP
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
