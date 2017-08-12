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

import java.util.ArrayList;
import java.util.List;

import jexer.bits.CellAttributes;

/**
 * A Line represents a single line of text on the screen, as a collection of
 * words.
 */
public class Line {

    /**
     * The list of words.
     */
    private ArrayList<Word> words = new ArrayList<Word>();

    /**
     * The default color for the TEditor class.
     */
    private CellAttributes defaultColor = null;

    /**
     * The text highlighter to use.
     */
    private Highlighter highlighter = null;

    /**
     * The current cursor position on this line.
     */
    private int cursor = 0;

    /**
     * The current word that the cursor position is in.
     */
    private Word currentWord;

    /**
     * We use getDisplayLength() a lot, so cache the value.
     */
    private int displayLength = -1;

    /**
     * Get the current cursor position.
     *
     * @return the cursor position
     */
    public int getCursor() {
        return cursor;
    }

    /**
     * Set the current cursor position.
     *
     * @param cursor the new cursor position
     */
    public void setCursor(final int cursor) {
        if ((cursor < 0)
            || ((cursor >= getDisplayLength())
                && (getDisplayLength() > 0))
        ) {
            throw new IndexOutOfBoundsException("Max length is " +
                getDisplayLength() + ", requested position " + cursor);
        }
        this.cursor = cursor;
        // TODO: set word
    }

    /**
     * Get a (shallow) copy of the list of words.
     *
     * @return the list of words
     */
    public List<Word> getWords() {
        return new ArrayList<Word>(words);
    }

    /**
     * Get the on-screen display length.
     *
     * @return the number of cells needed to display this line
     */
    public int getDisplayLength() {
        if (displayLength != -1) {
            return displayLength;
        }
        int n = 0;
        for (Word word: words) {
            n += word.getDisplayLength();
        }
        displayLength = n;

        // If we have any visible characters, add one to the display so that
        // the cursor is immediately after the data.
        if (displayLength > 0) {
            displayLength++;
        }
        return displayLength;
    }

    /**
     * Construct a new Line from an existing text string, and highlight
     * certain strings.
     *
     * @param str the text string
     * @param defaultColor the color for unhighlighted text
     * @param highlighter the highlighter to use
     */
    public Line(final String str, final CellAttributes defaultColor,
        final Highlighter highlighter) {

        this.defaultColor = defaultColor;
        this.highlighter = highlighter;

        currentWord = new Word(this.defaultColor, this.highlighter);
        words.add(currentWord);
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            Word newWord = currentWord.addChar(ch);
            if (newWord != currentWord) {
                words.add(newWord);
                currentWord = newWord;
            }
        }
        for (Word word: words) {
            word.applyHighlight();
        }
    }

    /**
     * Construct a new Line from an existing text string.
     *
     * @param str the text string
     * @param defaultColor the color for unhighlighted text
     */
    public Line(final String str, final CellAttributes defaultColor) {
        this(str, defaultColor, null);
    }

    /**
     * Decrement the cursor by one.  If at the first column, do nothing.
     *
     * @return true if the cursor position changed
     */
    public boolean left() {
        if (cursor == 0) {
            return false;
        }
        // TODO: switch word
        cursor--;
        return true;
    }

    /**
     * Increment the cursor by one.  If at the last column, do nothing.
     *
     * @return true if the cursor position changed
     */
    public boolean right() {
        if (getDisplayLength() == 0) {
            return false;
        }
        if (cursor == getDisplayLength() - 1) {
            return false;
        }
        // TODO: switch word
        cursor++;
        return true;
    }

    /**
     * Go to the first column of this line.
     *
     * @return true if the cursor position changed
     */
    public boolean home() {
        if (cursor > 0) {
            cursor = 0;
            currentWord = words.get(0);
            return true;
        }
        return false;
    }

    /**
     * Go to the last column of this line.
     *
     * @return true if the cursor position changed
     */
    public boolean end() {
        if (cursor != getDisplayLength() - 1) {
            cursor = getDisplayLength() - 1;
            if (cursor < 0) {
                cursor = 0;
            }
            currentWord = words.get(words.size() - 1);
            return true;
        }
        return false;
    }

    /**
     * Delete the character under the cursor.
     */
    public void del() {
        // TODO
    }

    /**
     * Delete the character immediately preceeding the cursor.
     */
    public void backspace() {
        // TODO
    }

    /**
     * Insert a character at the cursor.
     *
     * @param ch the character to insert
     */
    public void addChar(final char ch) {
        // TODO
    }

    /**
     * Replace a character at the cursor.
     *
     * @param ch the character to replace
     */
    public void replaceChar(final char ch) {
        // TODO
    }

}
