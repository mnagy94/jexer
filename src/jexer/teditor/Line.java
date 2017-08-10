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
     * The current cursor position on this line.
     */
    private int cursorX;

    /**
     * The current word that the cursor position is in.
     */
    private Word currentWord;

    /**
     * We use getDisplayLength() a lot, so cache the value.
     */
    private int displayLength = -1;

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
        return displayLength;
    }

    /**
     * Construct a new Line from an existing text string.
     *
     * @param str the text string
     */
    public Line(final String str) {
        currentWord = new Word();
        words.add(currentWord);
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            Word newWord = currentWord.addChar(ch);
            if (newWord != currentWord) {
                words.add(newWord);
                currentWord = newWord;
            }
        }
    }

    /**
     * Decrement the cursor by one.  If at the first column, do nothing.
     */
    public void left() {
        if (cursorX == 0) {
            return;
        }
        // TODO
    }

    /**
     * Increment the cursor by one.  If at the last column, do nothing.
     */
    public void right() {
        if (cursorX == getDisplayLength() - 1) {
            return;
        }
        // TODO
    }

    /**
     * Go to the first column of this line.
     */
    public void home() {
        // TODO
    }

    /**
     * Go to the last column of this line.
     */
    public void end() {
        // TODO
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
     * Replace or insert a character at the cursor, depending on overwrite
     * flag.
     *
     * @param ch the character to replace or insert
     */
    public void addChar(final char ch) {
        // TODO
    }

}
