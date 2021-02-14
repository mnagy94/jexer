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
package jexer.event;

import jexer.TKeypress;
import jexer.backend.Backend;

/**
 * This class encapsulates a keyboard input event.
 */
public class TKeypressEvent extends TInputEvent {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Keystroke received.
     */
    private TKeypress key;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param backend the backend that generated this event
     * @param key the TKeypress received
     */
    public TKeypressEvent(final Backend backend, final TKeypress key) {
        super(backend);

        this.key = key;
    }

    /**
     * Public constructor.
     *
     * @param backend the backend that generated this event
     * @param isKey is true, this is a function key
     * @param fnKey the function key code (only valid if isKey is true)
     * @param ch the character (only valid if fnKey is false)
     * @param alt if true, ALT was pressed with this keystroke
     * @param ctrl if true, CTRL was pressed with this keystroke
     * @param shift if true, SHIFT was pressed with this keystroke
     */
    public TKeypressEvent(final Backend backend, final boolean isKey,
        final int fnKey, final int ch, final boolean alt, final boolean ctrl,
        final boolean shift) {

        super(backend);

        this.key = new TKeypress(isKey, fnKey, ch, alt, ctrl, shift);
    }

    /**
     * Public constructor.
     *
     * @param backend the backend that generated this event
     * @param key the TKeypress received
     * @param alt if true, ALT was pressed with this keystroke
     * @param ctrl if true, CTRL was pressed with this keystroke
     * @param shift if true, SHIFT was pressed with this keystroke
     */
    public TKeypressEvent(final Backend backend, final TKeypress key,
        final boolean alt, final boolean ctrl, final boolean shift) {

        super(backend);

        this.key = new TKeypress(key.isFnKey(), key.getKeyCode(), key.getChar(),
            alt, ctrl, shift);
    }

    // ------------------------------------------------------------------------
    // TInputEvent ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another TKeypressEvent or TKeypress instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof TKeypressEvent)
            && !(rhs instanceof TKeypress)
        ) {
            return false;
        }

        if (rhs instanceof TKeypressEvent) {
            TKeypressEvent that = (TKeypressEvent) rhs;
            return (key.equals(that.key)
                && (getTime().equals(that.getTime())));
        }

        TKeypress that = (TKeypress) rhs;
        return (key.equals(that));
    }

    /**
     * Hashcode uses all fields in equals().
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        int A = 13;
        int B = 23;
        int hash = A;
        hash = (B * hash) + getTime().hashCode();
        hash = (B * hash) + key.hashCode();
        return hash;
    }

    /**
     * Make human-readable description of this TKeypressEvent.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("Keypress: %s", key.toString());
    }

    // ------------------------------------------------------------------------
    // TKeypressEvent ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get keystroke.
     *
     * @return keystroke
     */
    public TKeypress getKey() {
        return key;
    }

    /**
     * Create a duplicate instance.
     *
     * @return duplicate intance
     */
    public TKeypressEvent dup() {
        TKeypressEvent keypress = new TKeypressEvent(getBackend(), key.dup());
        return keypress;
    }

}
