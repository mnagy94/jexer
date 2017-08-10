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
 * A Document represents a text file, as a collection of lines.
 */
public class Document {

    /**
     * The list of lines.
     */
    private ArrayList<Line> lines = new ArrayList<Line>();

    /**
     * The current line number being edited.  Note that this is 0-based, the
     * first line is line number 0.
     */
    private int lineNumber = 0;

    /**
     * The overwrite flag.  When true, characters overwrite data.
     */
    private boolean overwrite = false;

    /**
     * Get the overwrite flag.
     *
     * @return true if addChar() overwrites data, false if it inserts
     */
    public boolean getOverwrite() {
        return overwrite;
    }

    /**
     * Set the overwrite flag.
     *
     * @param overwrite true if addChar() should overwrite data, false if it
     * should insert
     */
    public void setOverwrite(final boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Get the current line number being edited.
     *
     * @return the line number.  Note that this is 0-based: 0 is the first
     * line.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get a specific line by number.
     *
     * @param lineNumber the line number.  Note that this is 0-based: 0 is
     * the first line.
     * @return the line
     */
    public Line getLine(final int lineNumber) {
        return lines.get(lineNumber);
    }

    /**
     * Set the current line number being edited.
     *
     * @param n the line number.  Note that this is 0-based: 0 is the first
     * line.
     */
    public void setLineNumber(final int n) {
        if ((n < 0) || (n > lines.size())) {
            throw new IndexOutOfBoundsException("Line size is " + lines.size() +
                ", requested index " + n);
        }
        lineNumber = n;
    }

    /**
     * Increment the line number by one.  If at the last line, do nothing.
     */
    public void down() {
        if (lineNumber < lines.size() - 1) {
            lineNumber++;
        }
    }

    /**
     * Increment the line number by n.  If n would go past the last line,
     * increment only to the last line.
     *
     * @param n the number of lines to increment by
     */
    public void down(final int n) {
        lineNumber += n;
        if (lineNumber > lines.size() - 1) {
            lineNumber = lines.size() - 1;
        }
    }

    /**
     * Decrement the line number by one.  If at the first line, do nothing.
     */
    public void up() {
        if (lineNumber > 0) {
            lineNumber--;
        }
    }

    /**
     * Decrement the line number by n.  If n would go past the first line,
     * decrement only to the first line.
     *
     * @param n the number of lines to decrement by
     */
    public void up(final int n) {
        lineNumber -= n;
        if (lineNumber < 0) {
            lineNumber = 0;
        }
    }

    /**
     * Decrement the cursor by one.  If at the first column, do nothing.
     */
    public void left() {
        lines.get(lineNumber).left();
    }

    /**
     * Increment the cursor by one.  If at the last column, do nothing.
     */
    public void right() {
        lines.get(lineNumber).right();
    }

    /**
     * Go to the first column of this line.
     */
    public void home() {
        lines.get(lineNumber).home();
    }

    /**
     * Go to the last column of this line.
     */
    public void end() {
        lines.get(lineNumber).end();
    }

    /**
     * Delete the character under the cursor.
     */
    public void del() {
        lines.get(lineNumber).del();
    }

    /**
     * Delete the character immediately preceeding the cursor.
     */
    public void backspace() {
        lines.get(lineNumber).backspace();
    }

    /**
     * Replace or insert a character at the cursor, depending on overwrite
     * flag.
     *
     * @param ch the character to replace or insert
     */
    public void addChar(final char ch) {
        lines.get(lineNumber).addChar(ch);
    }

    /**
     * Get a (shallow) copy of the list of lines.
     *
     * @return the list of lines
     */
    public List<Line> getLines() {
        return new ArrayList<Line>(lines);
    }

    /**
     * Get the number of lines.
     *
     * @return the number of lines
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * Compute the maximum line length for this document.
     *
     * @return the number of cells needed to display the longest line
     */
    public int getLineLengthMax() {
        int n = 0;
        for (Line line : lines) {
            if (line.getDisplayLength() > n) {
                n = line.getDisplayLength();
            }
        }
        return n;
    }

    /**
     * Construct a new Document from an existing text string.
     *
     * @param str the text string
     */
    public Document(final String str) {
        String [] rawLines = str.split("\n");
        for (int i = 0; i < rawLines.length; i++) {
            lines.add(new Line(rawLines[i]));
        }
    }

}
