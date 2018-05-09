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

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

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
     * The raw text of this line, what is passed to Word to determine
     * highlighting behavior.
     */
    private StringBuilder rawText;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

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
        this.rawText = new StringBuilder(str);

        scanLine();
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

    // ------------------------------------------------------------------------
    // Line -------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get a (shallow) copy of the words in this line.
     *
     * @return a copy of the word list
     */
    public List<Word> getWords() {
        return new ArrayList<Word>(words);
    }

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
    }

    /**
     * Get the on-screen display length.
     *
     * @return the number of cells needed to display this line
     */
    public int getDisplayLength() {
        int n = rawText.length();

        // For now just return the raw text length.
        if (n > 0) {
            // If we have any visible characters, add one to the display so
            // that the cursor is immediately after the data.
            return n + 1;
        }
        return n;
    }

    /**
     * Get the raw string that matches this line.
     *
     * @return the string
     */
    public String getRawString() {
        return rawText.toString();
    }

    /**
     * Scan rawText and make words out of it.
     */
    private void scanLine() {
        words.clear();
        Word word = new Word(this.defaultColor, this.highlighter);
        words.add(word);
        for (int i = 0; i < rawText.length(); i++) {
            char ch = rawText.charAt(i);
            Word newWord = word.addChar(ch);
            if (newWord != word) {
                words.add(newWord);
                word = newWord;
            }
        }
        for (Word w: words) {
            w.applyHighlight();
        }
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
            return true;
        }
        return false;
    }

    /**
     * Delete the character under the cursor.
     */
    public void del() {
        assert (words.size() > 0);

        if (cursor < getDisplayLength()) {
            rawText.deleteCharAt(cursor);
        }

        // Re-scan the line to determine the new word boundaries.
        scanLine();
    }

    /**
     * Delete the character immediately preceeding the cursor.
     */
    public void backspace() {
        if (left()) {
            del();
        }
    }

    /**
     * Insert a character at the cursor.
     *
     * @param ch the character to insert
     */
    public void addChar(final char ch) {
        if (cursor < getDisplayLength() - 1) {
            rawText.insert(cursor, ch);
        } else {
            rawText.append(ch);
        }
        scanLine();
        cursor++;
    }

    /**
     * Replace a character at the cursor.
     *
     * @param ch the character to replace
     */
    public void replaceChar(final char ch) {
        if (cursor < getDisplayLength() - 1) {
            rawText.setCharAt(cursor, ch);
        } else {
            rawText.append(ch);
        }
        scanLine();
        cursor++;
    }

}
