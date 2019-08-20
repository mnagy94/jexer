/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2019 Kevin Lamonte
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
package jexer;

import jexer.bits.CellAttributes;
import jexer.bits.GraphicsChars;
import jexer.event.TKeypressEvent;
import jexer.event.TMenuEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;

/**
 * TDesktop is a special-class window that is drawn underneath everything
 * else.  Like a TWindow, it can contain widgets and perform "background"
 * processing via onIdle().  But unlike a TWindow, it cannot be hidden,
 * moved, or resized.
 *
 * <p>
 * Events are passed to TDesktop as follows:
 * <ul>
 * <li>Mouse events are seen if they do not cover any other windows.</li>
 * <li>Keypress events are seen if no other windows are open.</li>
 * <li>Menu events are seen if no other windows are open.</li>
 * <li>Command events are seen if no other windows are open.</li>
 * </ul>
 */
public class TDesktop extends TWindow {

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent application
     */
    public TDesktop(final TApplication parent) {
        super(parent, "", 0, 0, parent.getScreen().getWidth(),
            parent.getDesktopBottom() - parent.getDesktopTop());

        setActive(false);
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {
        if (getChildren().size() == 1) {
            TWidget child = getChildren().get(0);
            if (!(child instanceof TWindow)) {
                // Only one child, resize it to match my size.
                child.onResize(new TResizeEvent(TResizeEvent.Type.WIDGET,
                        getWidth(), getHeight()));
            }
        }
        if (resize.getType() == TResizeEvent.Type.SCREEN) {
            // Let children see the screen resize
            for (TWidget widget: getChildren()) {
                widget.onResize(resize);
            }
        }
    }

    /**
     * Handle mouse button presses.
     *
     * @param mouse mouse button event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        this.mouse = mouse;

        // Pass to children
        for (TWidget widget: getChildren()) {
            if (widget.mouseWouldHit(mouse)) {
                // Dispatch to this child, also activate it
                activate(widget);

                // Set x and y relative to the child's coordinates
                mouse.setX(mouse.getAbsoluteX() - widget.getAbsoluteX());
                mouse.setY(mouse.getAbsoluteY() - widget.getAbsoluteY());
                widget.handleEvent(mouse);
                return;
            }
        }
    }

    /**
     * Handle mouse button releases.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        this.mouse = mouse;

        // Pass to children
        for (TWidget widget: getChildren()) {
            if (widget.mouseWouldHit(mouse)) {
                // Dispatch to this child, also activate it
                activate(widget);

                // Set x and y relative to the child's coordinates
                mouse.setX(mouse.getAbsoluteX() - widget.getAbsoluteX());
                mouse.setY(mouse.getAbsoluteY() - widget.getAbsoluteY());
                widget.handleEvent(mouse);
                return;
            }
        }
    }

    /**
     * Handle mouse movements.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        this.mouse = mouse;

        // Default: do nothing, pass to children instead
        super.onMouseMotion(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        // Default: do nothing, pass to children instead
        super.onKeypress(keypress);
    }

    /**
     * Handle posted menu events.
     *
     * @param menu menu event
     */
    @Override
    public void onMenu(final TMenuEvent menu) {
        // Default: do nothing, pass to children instead
        super.onMenu(menu);
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The default TDesktop draws a hatch character across everything.
     */
    @Override
    public void draw() {
        CellAttributes background = getTheme().getColor("tdesktop.background");
        putAll(GraphicsChars.HATCH, background);

        /*
        // For debugging, let's see where the desktop bounds really are.
        putCharXY(0, 0, '0', background);
        putCharXY(getWidth() - 1, 0, '1', background);
        putCharXY(0, getHeight() - 1, '2', background);
        putCharXY(getWidth() - 1, getHeight() - 1, '3', background);
         */
    }

    /**
     * Hide window.  This is a NOP for TDesktop.
     */
    @Override
    public final void hide() {}

    /**
     * Show window.  This is a NOP for TDesktop.
     */
    @Override
    public final void show() {}

    /**
     * Called by hide().  This is a NOP for TDesktop.
     */
    @Override
    public final void onHide() {}

    /**
     * Called by show().  This is a NOP for TDesktop.
     */
    @Override
    public final void onShow() {}

    /**
     * Returns true if the mouse is currently on the close button.
     *
     * @return true if mouse is currently on the close button
     */
    @Override
    protected final boolean mouseOnClose() {
        return false;
    }

    /**
     * Returns true if the mouse is currently on the maximize/restore button.
     *
     * @return true if the mouse is currently on the maximize/restore button
     */
    @Override
    protected final boolean mouseOnMaximize() {
        return false;
    }

    /**
     * Returns true if the mouse is currently on the resizable lower right
     * corner.
     *
     * @return true if the mouse is currently on the resizable lower right
     * corner
     */
    @Override
    protected final boolean mouseOnResize() {
        return false;
    }

}
