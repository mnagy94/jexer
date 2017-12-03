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

import java.util.LinkedList;
import java.util.List;

import jexer.bits.CellAttributes;
import jexer.event.TInputEvent;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.TApplication;
import jexer.TWindow;

/**
 * TWindowBackend uses a window in one TApplication to provide a backend for
 * another TApplication.
 *
 * Note that TWindow has its own getScreen() and setTitle() functions.
 * Clients in TWindowBackend's application won't be able to use it to get at
 * the other application's screen.  getOtherScreen() has been provided.
 */
public class TWindowBackend extends TWindow implements Backend {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The listening object that run() wakes up on new input.
     */
    private Object listener;

    /**
     * The object to sync on in draw().  This is normally otherScreen, but it
     * could also be a MultiScreen.
     */
    private Object drawLock;

    /**
     * The event queue, filled up by a thread reading on input.
     */
    private List<TInputEvent> eventQueue;

    /**
     * The screen to use.
     */
    private Screen otherScreen;

    /**
     * The mouse X position as seen on the other screen.
     */
    private int otherMouseX = -1;

    /**
     * The mouse Y position as seen on the other screen.
     */
    private int otherMouseY = -1;

    /**
     * The session information.
     */
    private SessionInfo sessionInfo;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.  Window will be located at (0, 0).
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param width width of window
     * @param height height of window
     */
    public TWindowBackend(final Object listener,
        final TApplication application, final String title,
        final int width, final int height) {

        super(application, title, width, height);

        this.listener = listener;
        eventQueue = new LinkedList<TInputEvent>();
        sessionInfo = new TSessionInfo(width, height);
        otherScreen = new LogicalScreen();
        otherScreen.setDimensions(width - 2, height - 2);
        drawLock = otherScreen;
    }

    /**
     * Public constructor.  Window will be located at (0, 0).
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param width width of window
     * @param height height of window
     * @param flags bitmask of RESIZABLE, CENTERED, or MODAL
     */
    public TWindowBackend(final Object listener,
        final TApplication application, final String title,
        final int width, final int height, final int flags) {

        super(application, title, width, height, flags);

        this.listener = listener;
        eventQueue = new LinkedList<TInputEvent>();
        sessionInfo = new TSessionInfo(width, height);
        otherScreen = new LogicalScreen();
        otherScreen.setDimensions(width - 2, height - 2);
        drawLock = otherScreen;
    }

    /**
     * Public constructor.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     */
    public TWindowBackend(final Object listener,
        final TApplication application, final String title,
        final int x, final int y, final int width, final int height) {

        super(application, title, x, y, width, height);

        this.listener = listener;
        eventQueue = new LinkedList<TInputEvent>();
        sessionInfo = new TSessionInfo(width, height);
        otherScreen = new LogicalScreen();
        otherScreen.setDimensions(width - 2, height - 2);
        drawLock = otherScreen;
    }

    /**
     * Public constructor.
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of window
     * @param height height of window
     * @param flags mask of RESIZABLE, CENTERED, or MODAL
     */
    public TWindowBackend(final Object listener,
        final TApplication application, final String title,
        final int x, final int y, final int width, final int height,
        final int flags) {

        super(application, title, x, y, width, height, flags);

        this.listener = listener;
        eventQueue = new LinkedList<TInputEvent>();
        sessionInfo = new TSessionInfo(width, height);
        otherScreen = new LogicalScreen();
        otherScreen.setDimensions(width - 2, height - 2);
        drawLock = otherScreen;
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Returns true if the mouse is currently in the otherScreen window.
     *
     * @param mouse mouse event
     * @return true if mouse is currently in the otherScreen window.
     */
    protected boolean mouseOnOtherScreen(final TMouseEvent mouse) {
        if ((mouse.getY() >= 1)
            && (mouse.getY() <= otherScreen.getHeight())
            && (mouse.getX() >= 1)
            && (mouse.getX() <= otherScreen.getWidth())
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouseOnOtherScreen(mouse)) {
            TMouseEvent event = mouse.dup();
            event.setX(mouse.getX() - 1);
            event.setY(mouse.getY() - 1);
            event.setAbsoluteX(event.getX());
            event.setAbsoluteY(event.getY());
            synchronized (eventQueue) {
                eventQueue.add(event);
            }
            synchronized (listener) {
                listener.notifyAll();
            }
        }
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if (mouseOnOtherScreen(mouse)) {
            TMouseEvent event = mouse.dup();
            event.setX(mouse.getX() - 1);
            event.setY(mouse.getY() - 1);
            event.setAbsoluteX(event.getX());
            event.setAbsoluteY(event.getY());
            synchronized (eventQueue) {
                eventQueue.add(event);
            }
            synchronized (listener) {
                listener.notifyAll();
            }
        }
        super.onMouseUp(mouse);
    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        if (mouseOnOtherScreen(mouse)) {
            TMouseEvent event = mouse.dup();
            event.setX(mouse.getX() - 1);
            event.setY(mouse.getY() - 1);
            event.setAbsoluteX(event.getX());
            event.setAbsoluteY(event.getY());
            otherMouseX = event.getX() + getX() + 1;
            otherMouseY = event.getY() + getY() + 1;
            synchronized (eventQueue) {
                eventQueue.add(event);
            }
            synchronized (listener) {
                listener.notifyAll();
            }
        } else {
            otherMouseX = -1;
            otherMouseY = -1;
        }
        super.onMouseMotion(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        TKeypressEvent event = keypress.dup();
        synchronized (eventQueue) {
            eventQueue.add(event);
        }
        synchronized (listener) {
            listener.notifyAll();
        }
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Draw the foreground colors grid.
     */
    @Override
    public void draw() {

        // Sync on other screen, so that we do not draw in the middle of
        // their screen update.
        synchronized (drawLock) {
            // Draw the box
            super.draw();

            // Draw every cell of the other screen
            for (int y = 0; y < otherScreen.getHeight(); y++) {
                for (int x = 0; x < otherScreen.getWidth(); x++) {
                    putCharXY(x + 1, y + 1, otherScreen.getCharXY(x, y));
                }
            }

            // If the mouse pointer is over the other window, draw its
            // pointer again here.  (Their TApplication drew it, then our
            // TApplication drew it again (undo-ing it), so now we draw it a
            // third time so that it is visible.)
            if ((otherMouseX != -1) && (otherMouseY != -1)) {
                CellAttributes attr = getAttrXY(otherMouseX, otherMouseY);
                attr.setForeColor(attr.getForeColor().invert());
                attr.setBackColor(attr.getBackColor().invert());
                putAttrXY(otherMouseX, otherMouseY, attr, false);
            }

            // If their cursor is visible, draw that here too.
            if (otherScreen.isCursorVisible()) {
                setCursorX(otherScreen.getCursorX() + 1);
                setCursorY(otherScreen.getCursorY() + 1);
                setCursorVisible(true);
            } else {
                setCursorVisible(false);
            }
        }
    }

    /**
     * Subclasses should override this method to cleanup resources.  This is
     * called by application.closeWindow().
     */
    @Override
    public void onClose() {
        // TODO: send a screen disconnect
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
     * Subclasses must provide an implementation that syncs the logical
     * screen to the physical device.
     */
    public void flushScreen() {
        getApplication().doRepaint();
    }

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the application
     */
    public boolean hasEvents() {
        synchronized (eventQueue) {
            return (eventQueue.size() > 0);
        }
    }

    /**
     * Subclasses must provide an implementation to get keyboard, mouse, and
     * screen resize events.
     *
     * @param queue list to append new events to
     */
    public void getEvents(List<TInputEvent> queue) {
        synchronized (eventQueue) {
            if (eventQueue.size() > 0) {
                synchronized (queue) {
                    queue.addAll(eventQueue);
                }
                eventQueue.clear();
            }
        }
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
        this.listener = listener;
    }

    // ------------------------------------------------------------------------
    // TWindowBackend ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the object to sync to in draw().
     *
     * @param drawLock the object to synchronize on
     */
    public void setDrawLock(final Object drawLock) {
        this.drawLock = drawLock;
    }

    /**
     * Getter for the other application's screen.
     *
     * @return the Screen
     */
    public Screen getOtherScreen() {
        return otherScreen;
    }

}
