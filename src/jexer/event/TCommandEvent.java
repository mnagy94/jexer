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
package jexer.event;

import jexer.TCommand;
import jexer.backend.Backend;

/**
 * This class encapsulates a user command event.  User commands can be
 * generated by menu actions, keyboard accelerators, and other UI elements.
 * Commands can operate on both the application and individual widgets.
 */
public class TCommandEvent extends TInputEvent {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Command dispatched.
     */
    private TCommand cmd;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public contructor.
     *
     * @param backend the backend that generated this event
     * @param cmd the TCommand dispatched
     */
    public TCommandEvent(final Backend backend, final TCommand cmd) {
        super(backend);

        this.cmd = cmd;
    }

    // ------------------------------------------------------------------------
    // TInputEvent ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another TCommandEvent or TCommand instance
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(final Object rhs) {
        if (!(rhs instanceof TCommandEvent)
            && !(rhs instanceof TCommand)
        ) {
            return false;
        }

        if (rhs instanceof TCommandEvent) {
            TCommandEvent that = (TCommandEvent) rhs;
            return (cmd.equals(that.cmd)
                && (getTime().equals(that.getTime())));
        }

        TCommand that = (TCommand) rhs;
        return (cmd.equals(that));
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
        hash = (B * hash) + cmd.hashCode();
        return hash;
    }

    /**
     * Make human-readable description of this TCommandEvent.
     *
     * @return displayable String
     */
    @Override
    public String toString() {
        return String.format("CommandEvent: %s", cmd.toString());
    }

    // ------------------------------------------------------------------------
    // TCommandEvent ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get TCommand.
     *
     * @return the TCommand
     */
    public TCommand getCmd() {
        return cmd;
    }

}
