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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import jexer.bits.CellAttributes;

/**
 * A Document represents a text file, as a collection of lines.
 */
public class Document {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

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
     * If true, the document has been edited.
     */
    private boolean dirty = false;

    /**
     * The default color for the TEditor class.
     */
    private CellAttributes defaultColor = null;

    /**
     * The text highlighter to use.
     */
    private Highlighter highlighter = new Highlighter();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Construct a new Document from an existing text string.
     *
     * @param str the text string
     * @param defaultColor the color for unhighlighted text
     */
    public Document(final String str, final CellAttributes defaultColor) {
        this.defaultColor = defaultColor;

        // TODO: set different colors based on file extension
        highlighter.setJavaColors();

        String [] rawLines = str.split("\n");
        for (int i = 0; i < rawLines.length; i++) {
            lines.add(new Line(rawLines[i], this.defaultColor, highlighter));
        }
    }

    // ------------------------------------------------------------------------
    // Document ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the overwrite flag.
     *
     * @return true if addChar() overwrites data, false if it inserts
     */
    public boolean getOverwrite() {
        return overwrite;
    }

    /**
     * Get the dirty value.
     *
     * @return true if the buffer is dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Save contents to file.
     *
     * @param filename file to save to
     * @throws IOException if a java.io operation throws
     */
    public void saveToFilename(final String filename) throws IOException {
        OutputStreamWriter output = null;
        try {
            output = new OutputStreamWriter(new FileOutputStream(filename),
                "UTF-8");

            for (Line line: lines) {
                output.write(line.getRawString());
                output.write("\n");
            }

            dirty = false;
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
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
     * Get the current editing line.
     *
     * @return the line
     */
    public Line getCurrentLine() {
        return lines.get(lineNumber);
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
            throw new IndexOutOfBoundsException("Lines array size is " +
                lines.size() + ", requested index " + n);
        }
        lineNumber = n;
    }

    /**
     * Get the current cursor position of the editing line.
     *
     * @return the cursor position
     */
    public int getCursor() {
        return lines.get(lineNumber).getCursor();
    }

    /**
     * Set the current cursor position of the editing line.  0-based.
     *
     * @param cursor the new cursor position
     */
    public void setCursor(final int cursor) {
        if (cursor >= lines.get(lineNumber).getDisplayLength()) {
            lines.get(lineNumber).end();
        } else {
            lines.get(lineNumber).setCursor(cursor);
        }
    }

    /**
     * Increment the line number by one.  If at the last line, do nothing.
     *
     * @return true if the editing line changed
     */
    public boolean down() {
        if (lineNumber < lines.size() - 1) {
            int x = lines.get(lineNumber).getCursor();
            lineNumber++;
            if (x >= lines.get(lineNumber).getDisplayLength()) {
                lines.get(lineNumber).end();
            } else {
                lines.get(lineNumber).setCursor(x);
            }
            return true;
        }
        return false;
    }

    /**
     * Increment the line number by n.  If n would go past the last line,
     * increment only to the last line.
     *
     * @param n the number of lines to increment by
     * @return true if the editing line changed
     */
    public boolean down(final int n) {
        if (lineNumber < lines.size() - 1) {
            int x = lines.get(lineNumber).getCursor();
            lineNumber += n;
            if (lineNumber > lines.size() - 1) {
                lineNumber = lines.size() - 1;
            }
            if (x >= lines.get(lineNumber).getDisplayLength()) {
                lines.get(lineNumber).end();
            } else {
                lines.get(lineNumber).setCursor(x);
            }
            return true;
        }
        return false;
    }

    /**
     * Decrement the line number by one.  If at the first line, do nothing.
     *
     * @return true if the editing line changed
     */
    public boolean up() {
        if (lineNumber > 0) {
            int x = lines.get(lineNumber).getCursor();
            lineNumber--;
            if (x >= lines.get(lineNumber).getDisplayLength()) {
                lines.get(lineNumber).end();
            } else {
                lines.get(lineNumber).setCursor(x);
            }
            return true;
        }
        return false;
    }

    /**
     * Decrement the line number by n.  If n would go past the first line,
     * decrement only to the first line.
     *
     * @param n the number of lines to decrement by
     * @return true if the editing line changed
     */
    public boolean up(final int n) {
        if (lineNumber > 0) {
            int x = lines.get(lineNumber).getCursor();
            lineNumber -= n;
            if (lineNumber < 0) {
                lineNumber = 0;
            }
            if (x >= lines.get(lineNumber).getDisplayLength()) {
                lines.get(lineNumber).end();
            } else {
                lines.get(lineNumber).setCursor(x);
            }
            return true;
        }
        return false;
    }

    /**
     * Decrement the cursor by one.  If at the first column, do nothing.
     *
     * @return true if the cursor position changed
     */
    public boolean left() {
        if (!lines.get(lineNumber).left()) {
            // We are on the leftmost column, wrap
            if (up()) {
                end();
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Increment the cursor by one.  If at the last column, do nothing.
     *
     * @return true if the cursor position changed
     */
    public boolean right() {
        if (!lines.get(lineNumber).right()) {
            // We are on the rightmost column, wrap
            if (down()) {
                home();
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Go to the first column of this line.
     *
     * @return true if the cursor position changed
     */
    public boolean home() {
        return lines.get(lineNumber).home();
    }

    /**
     * Go to the last column of this line.
     *
     * @return true if the cursor position changed
     */
    public boolean end() {
        return lines.get(lineNumber).end();
    }

    /**
     * Delete the character under the cursor.
     */
    public void del() {
        dirty = true;
        int cursor = lines.get(lineNumber).getCursor();
        if (cursor < lines.get(lineNumber).getDisplayLength() - 1) {
            lines.get(lineNumber).del();
        } else if (lineNumber < lines.size() - 2) {
            // Join two lines
            StringBuilder newLine = new StringBuilder(lines.
                get(lineNumber).getRawString());
            newLine.append(lines.get(lineNumber + 1).getRawString());
            lines.set(lineNumber, new Line(newLine.toString(),
                    defaultColor, highlighter));
            lines.get(lineNumber).setCursor(cursor);
            lines.remove(lineNumber + 1);
        }
    }

    /**
     * Delete the character immediately preceeding the cursor.
     */
    public void backspace() {
        dirty = true;
        int cursor = lines.get(lineNumber).getCursor();
        if (cursor > 0) {
            lines.get(lineNumber).backspace();
        } else if (lineNumber > 0) {
            // Join two lines
            lineNumber--;
            String firstLine = lines.get(lineNumber).getRawString();
            if (firstLine.length() > 0) {
                // Backspacing combining two lines
                StringBuilder newLine = new StringBuilder(firstLine);
                newLine.append(lines.get(lineNumber + 1).getRawString());
                lines.set(lineNumber, new Line(newLine.toString(),
                        defaultColor, highlighter));
                lines.get(lineNumber).setCursor(firstLine.length());
                lines.remove(lineNumber + 1);
            } else {
                // Backspacing an empty line
                lines.remove(lineNumber);
                lines.get(lineNumber).setCursor(0);
            }
        }
    }

    /**
     * Split the current line into two, like pressing the enter key.
     */
    public void enter() {
        dirty = true;
        int cursor = lines.get(lineNumber).getCursor();
        String original = lines.get(lineNumber).getRawString();
        String firstLine = original.substring(0, cursor);
        String secondLine = original.substring(cursor);
        lines.add(lineNumber + 1, new Line(secondLine, defaultColor,
                highlighter));
        lines.set(lineNumber, new Line(firstLine, defaultColor, highlighter));
        lineNumber++;
        lines.get(lineNumber).home();
    }

    /**
     * Replace or insert a character at the cursor, depending on overwrite
     * flag.
     *
     * @param ch the character to replace or insert
     */
    public void addChar(final char ch) {
        dirty = true;
        if (overwrite) {
            lines.get(lineNumber).replaceChar(ch);
        } else {
            lines.get(lineNumber).addChar(ch);
        }
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
     * Get the current line length.
     *
     * @return the number of cells needed to display the current line
     */
    public int getLineLength() {
        return lines.get(lineNumber).getDisplayLength();
    }

}
