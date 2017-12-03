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
package jexer.bits;

/**
 * MnemonicString is used to render a string like "&File" into a highlighted
 * 'F' and the rest of 'ile'.  To insert a literal '&', use two '&&'
 * characters, e.g. "&File && Stuff" would be "File & Stuff" with the first
 * 'F' highlighted.
 */
public final class MnemonicString {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Keyboard shortcut to activate this item.
     */
    private char shortcut;

    /**
     * Location of the highlighted character.
     */
    private int shortcutIdx = -1;

    /**
     * The raw (uncolored) string.
     */
    private String rawLabel;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param label widget label or title.  Label must contain a keyboard
     * shortcut, denoted by prefixing a letter with "&", e.g. "&File"
     */
    public MnemonicString(final String label) {

        // Setup the menu shortcut
        String newLabel = "";
        boolean foundAmp = false;
        boolean foundShortcut = false;
        int scanShortcutIdx = 0;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c == '&') {
                if (foundAmp) {
                    newLabel += '&';
                    scanShortcutIdx++;
                } else {
                    foundAmp = true;
                }
            } else {
                newLabel += c;
                if (foundAmp) {
                    if (!foundShortcut) {
                        shortcut = c;
                        foundAmp = false;
                        foundShortcut = true;
                        shortcutIdx = scanShortcutIdx;
                    }
                } else {
                    scanShortcutIdx++;
                }
            }
        }
        this.rawLabel = newLabel;
    }

    // ------------------------------------------------------------------------
    // MnemonicString ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the keyboard shortcut character.
     *
     * @return the highlighted character
     */
    public char getShortcut() {
        return shortcut;
    }

    /**
     * Get location of the highlighted character.
     *
     * @return location of the highlighted character
     */
    public int getShortcutIdx() {
        return shortcutIdx;
    }

    /**
     * Get the raw (uncolored) string.
     *
     * @return the raw (uncolored) string
     */
    public String getRawLabel() {
        return rawLabel;
    }

}
