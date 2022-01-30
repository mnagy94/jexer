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

import java.util.ArrayList;
import java.util.List;

import jexer.bits.CellAttributes;
import jexer.event.TCommandEvent;
import jexer.event.TInputEvent;
import static jexer.TCommand.*;

/**
 * MultiBackend mirrors its I/O to several backends.
 */
public class MultiBackend implements Backend {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The screen to use.
     */
    private MultiScreen multiScreen;

    /**
     * The list of backends to use.
     */
    private List<Backend> backends = new ArrayList<Backend>();

    /**
     * The SessionInfo to return.
     */
    private SessionInfo sessionInfo;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor requires one backend.  Note that this backend's
     * screen will be replaced with a MultiScreen.
     *
     * @param backend the backend to add
     */
    public MultiBackend(final Backend backend) {
        backends.add(backend);
        if (backend instanceof TWindowBackend) {
            multiScreen = new MultiScreen(((TWindowBackend) backend).getOtherScreen());
        } else {
            multiScreen = new MultiScreen(backend.getScreen());
        }
        multiScreen.setBackend(this);
        if (backend instanceof GenericBackend) {
            ((GenericBackend) backend).abortOnDisconnect = false;
        }
        sessionInfo = backend.getSessionInfo();
    }

    // ------------------------------------------------------------------------
    // Backend ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Getter for sessionInfo.
     *
     * @return the SessionInfo
     */
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    /**
     * Getter for screen.
     *
     * @return the Screen
     */
    public Screen getScreen() {
        return multiScreen;
    }

    /**
     * Subclasses must provide an implementation that syncs the logical
     * screen to the physical device.
     */
    public void flushScreen() {
        multiScreen.flushPhysical();
        int n = backends.size();
        for (int i = 0; i < n; i++) {
            final Backend backend = backends.get(Math.min(i, backends.size()));
            // Flush to the physical device on another thread.
            (new Thread(new Runnable() {
                public void run() {
                    synchronized (backend.getScreen()) {
                        backend.flushScreen();
                    }
                }
            })).start();
        }
    }

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the application
     */
    public boolean hasEvents() {
        if (backends.size() == 0) {
            return true;
        }
        for (Backend backend: backends) {
            if (backend.hasEvents()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Subclasses must provide an implementation to get keyboard, mouse, and
     * screen resize events.
     *
     * @param queue list to append new events to
     */
    public void getEvents(List<TInputEvent> queue) {
        List<Backend> backendsToRemove = null;
        for (Backend backend: backends) {
            if (backend.hasEvents()) {
                backend.getEvents(queue);

                // This default backend assumes a single user, and if that
                // user becomes disconnected we should terminate the
                // application.
                if (queue.size() > 0) {
                    TInputEvent event = queue.get(queue.size() - 1);
                    if (event instanceof TCommandEvent) {
                        TCommandEvent command = (TCommandEvent) event;
                        if (command.equals(cmBackendDisconnect)) {
                            if (backendsToRemove == null) {
                                backendsToRemove = new ArrayList<Backend>();
                            }
                            backendsToRemove.add(backend);
                        }
                    }
                }
            }
        }
        if (backendsToRemove != null) {
            for (Backend backend: backendsToRemove) {
                multiScreen.removeScreen(backend.getScreen());
                backends.remove(backend);
                backend.shutdown();
            }
        }
        if (backends.size() == 0) {
            queue.add(new TCommandEvent(null, cmAbort));
        }
    }

    /**
     * Subclasses must provide an implementation that closes sockets,
     * restores console, etc.
     */
    public void shutdown() {
        for (Backend backend: backends) {
            backend.shutdown();
        }
    }

    /**
     * Subclasses must provide an implementation that sets the window title.
     *
     * @param title the new title
     */
    public void setTitle(final String title) {
        for (Backend backend: backends) {
            backend.setTitle(title);
        }
    }

    /**
     * Set listener to a different Object.
     *
     * @param listener the new listening object that run() wakes up on new
     * input
     */
    public void setListener(final Object listener) {
        for (Backend backend: backends) {
            backend.setListener(listener);
        }
    }

    /**
     * Reload backend options from System properties.
     */
    public void reloadOptions() {
        for (Backend backend: backends) {
            backend.reloadOptions();
        }
    }

    /**
     * Check if backend will support incomplete image fragments over text
     * display.
     *
     * @return true if images can partially obscure text
     */
    public boolean isImagesOverText() {
        // If any connected backends can do it, then this one can too.
        for (Backend backend: backends) {
            if (backend.isImagesOverText()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // MultiBackend -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Add a backend to the list.
     *
     * @param backend the backend to add
     */
    public void addBackend(final Backend backend) {
        addBackend(backend, false);
    }

    /**
     * Add a backend to the list.
     *
     * @param backend the backend to add
     * @param readOnly set the backend as read-only.  If this would result in
     * all backends begin read-only, it is ignored.
     */
    public void addBackend(final Backend backend, final boolean readOnly) {
        backends.add(backend);
        if (backend instanceof TWindowBackend) {
            multiScreen.addScreen(((TWindowBackend) backend).getOtherScreen());
        } else {
            multiScreen.addScreen(backend.getScreen());
        }
        if (backend instanceof GenericBackend) {
            ((GenericBackend) backend).abortOnDisconnect = false;
        }

        boolean allReadOnly = true;
        for (Backend b: backends) {
            // If a read-write backend has been idle for too long, treat it
            // like a read-only backend so that someone else can take over
            // the session if needed.
            if (b.getSessionInfo().getIdleTime() > 600) {
                continue;
            }

            if (!b.isReadOnly()) {
                allReadOnly = false;
                break;
            }
        }
        if (allReadOnly) {
            backend.setReadOnly(false);
        } else {
            backend.setReadOnly(readOnly);
        }

    }

    /**
     * Remove a backend from the list.
     *
     * @param backend the backend to remove
     */
    public void removeBackend(final Backend backend) {
        if (backends.size() > 1) {
            if (backend instanceof TWindowBackend) {
                multiScreen.removeScreen(((TWindowBackend) backend).getOtherScreen());
            } else {
                multiScreen.removeScreen(backend.getScreen());
            }
            backends.remove(backend);
        }
    }

    /**
     * Get the active backends.
     *
     * @return the list of active (not headless) backends, including
     * read-only backends
     */
    public List<Backend> getBackends() {
        ArrayList<Backend> result = new ArrayList<Backend>();
        for (Backend backend: backends) {
            if (!(backend instanceof HeadlessBackend)) {
                result.add(backend);
            }
        }
        return result;
    }

    /**
     * Check if backend is read-only.  For a MultiBackend, this is always
     * false.
     *
     * @return false
     */
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Set read-only flag.  This does nothing for MultiBackend.
     *
     * @param readOnly ignored
     */
    public void setReadOnly(final boolean readOnly) {
        // NOP
    }

    /**
     * Check if backend is reporting pixel-based mouse position.
     *
     * @return true if single-pixel mouse movements are reported
     */
    public boolean isPixelMouse() {
        // If any connected backends can do it, then this one can too.
        for (Backend backend: backends) {
            if (backend.isPixelMouse()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set request for backend to report pixel-based mouse position.
     *
     * @param pixelMouse if true, single-pixel mouse movements will be
     * reported, if the backend supports it
     */
    public void setPixelMouse(final boolean pixelMouse) {
        for (Backend backend: backends) {
            backend.setPixelMouse(pixelMouse);
        }
    }

    /**
     * Set the mouse pointer (cursor) style.
     *
     * @param mouseStyle the pointer style string, one of: "default", "none",
     * "hand", "text", "move", or "crosshair"
     */
    public void setMouseStyle(final String mouseStyle) {
        for (Backend backend: backends) {
            backend.setMouseStyle(mouseStyle);
        }
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
