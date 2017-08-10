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
package jexer.teditor;

import jexer.bits.CellAttributes;

/**
 * A Word represents text that was entered by the user.  It can be either
 * whitespace or non-whitespace.
 */
public class Word {

    /**
     * The color to render this word as on screen.
     */
    private CellAttributes color = new CellAttributes();

    /**
     * The actual text of this word.  Average word length is 6 characters,
     * with a lot of shorter ones, so start with 3.
     */
    private StringBuilder text = new StringBuilder(3);

    /**
     * Get the color used to display this word on screen.
     *
     * @return the color
     */
    public CellAttributes getColor() {
        return new CellAttributes(color);
    }

    /**
     * Set the color used to display this word on screen.
     *
     * @param color the color
     */
    public void setColor(final CellAttributes color) {
        color.setTo(color);
    }

    /**
     * Get the text to display.
     *
     * @return the text
     */
    public String getText() {
        return text.toString();
    }

    /**
     * Get the on-screen display length.
     *
     * @return the number of cells needed to display this word
     */
    public int getDisplayLength() {
        // For now, just use the text length.  In the future, this will be a
        // grapheme count.

        // TODO: figure out how to handle the tab character.  Do we have a
        // global tab stops list and current word position?
        return text.length();
    }

    /**
     * See if this is a whitespace word.  Note that empty string is
     * considered whitespace.
     *
     * @return true if this word is whitespace
     */
    public boolean isWhitespace() {
        if (text.length() == 0) {
            return true;
        }
        if (Character.isWhitespace(text.charAt(0))) {
            return true;
        }
        return false;
    }

    /**
     * Construct a word with one character.
     *
     * @param ch the first character of the word
     */
    public Word(final char ch) {
        text.append(ch);
    }

    /**
     * Construct a word with an empty string.
     */
    public Word() {}

    /**
     * Add a character to this word.  If this is a whitespace character
     * adding to a non-whitespace word, create a new word and return that;
     * similarly if this a non-whitespace character adding to a whitespace
     * word, create a new word and return that.
     *
     * @param ch the new character to add
     * @return either this word (if it was added), or a new word that
     * contains ch
     */
    public Word addChar(final char ch) {
        if (text.length() == 0) {
            text.append(ch);
            return this;
        }
        if (Character.isWhitespace(text.charAt(0))
            && Character.isWhitespace(ch)
        ) {
            text.append(ch);
            return this;
        }
        if (!Character.isWhitespace(text.charAt(0))
            && !Character.isWhitespace(ch)
        ) {
            text.append(ch);
            return this;
        }

        // We will be splitting here.
        Word newWord = new Word(ch);
        return newWord;
    }

}
