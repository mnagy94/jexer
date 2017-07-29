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

import jexer.bits.Cell;
import jexer.bits.CellAttributes;

/**
 * A Line represents a single line of text on the screen.  Each character is
 * a Cell, so it can have color attributes in addition to the basic char.
 */
public class Line implements Fragment {

    /**
     * The cells of the line.
     */
    private List<Cell> cells;

    /**
     * The line number.
     */
    private int lineNumber;

    /**
     * The previous Fragment in the list.
     */
    private Fragment prevFrag;

    /**
     * The next Fragment in the list.
     */
    private Fragment nextFrag;

    /**
     * Construct a new Line from an existing text string.
     */
    public Line() {
        this("");
    }

    /**
     * Construct a new Line from an existing text string.
     *
     * @param text the code points of the line
     */
    public Line(final String text) {
        cells = new ArrayList<Cell>(text.length());
        for (int i = 0; i < text.length(); i++) {
            cells.add(new Cell(text.charAt(i)));
        }
    }

    /**
     * Reset all colors of this Line to white-on-black.
     */
    public void resetColors() {
        setColors(new CellAttributes());
    }

    /**
     * Set all colors of this Line to one color.
     *
     * @param color the new color to use
     */
    public void setColors(final CellAttributes color) {
        for (Cell cell: cells) {
            cell.setTo(color);
        }
    }

    /**
     * Set the color of one cell.
     *
     * @param index a cell number, between 0 and getCellCount()
     * @param color the new color to use
     * @throws IndexOutOfBoundsException if index is negative or not less
     * than getCellCount()
     */
    public void setColor(final int index, final CellAttributes color) {
        cells.get(index).setTo(color);
    }

    /**
     * Get the raw text that will be rendered.
     *
     * @return the text
     */
    public String getText() {
        char [] text = new char[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            text[i] = cells.get(i).getChar();
        }
        return new String(text);
    }

    /**
     * Get the attributes for a cell.
     *
     * @param index a cell number, between 0 and getCellCount()
     * @return the attributes
     * @throws IndexOutOfBoundsException if index is negative or not less
     * than getCellCount()
     */
    public CellAttributes getColor(final int index) {
        return cells.get(index);
    }

    /**
     * Get the number of graphical cells represented by this text.  Note that
     * a Unicode grapheme cluster can take any number of pixels, but this
     * editor is intended to be used with a fixed-width font.  So this count
     * returns the number of fixed-width cells, NOT the number of grapheme
     * clusters.
     *
     * @return the number of fixed-width cells this fragment's text will
     * render to
     */
    public int getCellCount() {
        return cells.size();
    }

    /**
     * Get the text to render for a specific fixed-width cell.
     *
     * @param index a cell number, between 0 and getCellCount()
     * @return the codepoints to render for this fixed-width cell
     * @throws IndexOutOfBoundsException if index is negative or not less
     * than getCellCount()
     */
    public Cell getCell(final int index) {
        return cells.get(index);
    }

    /**
     * Get the text to render for several fixed-width cells.
     *
     * @param start a cell number, between 0 and getCellCount()
     * @param length the number of cells to return
     * @return the codepoints to render for this fixed-width cell
     * @throws IndexOutOfBoundsException if start or (start + length) is
     * negative or not less than getCellCount()
     */
    public String getCells(final int start, final int length) {
        char [] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = cells.get(i + start).getChar();
        }
        return new String(text);
    }

    /**
     * Sets (replaces) the text to render for a specific fixed-width cell.
     *
     * @param index a cell number, between 0 and getCellCount()
     * @param ch the character for this fixed-width cell
     * @throws IndexOutOfBoundsException if index is negative or not less
     * than getCellCount()
     */
    public void setCell(final int index, final char ch) {
        cells.set(index, new Cell(ch));
    }

    /**
     * Sets (replaces) the text to render for a specific fixed-width cell.
     *
     * @param index a cell number, between 0 and getCellCount()
     * @param cell the new value for this fixed-width cell
     * @throws IndexOutOfBoundsException if index is negative or not less
     * than getCellCount()
     */
    public void setCell(final int index, final Cell cell) {
        cells.set(index, cell);
    }

    /**
     * Inserts a char to render for a specific fixed-width cell.
     *
     * @param index a cell number, between 0 and getCellCount() - 1
     * @param ch the character for this fixed-width cell
     * @throws IndexOutOfBoundsException if index is negative or not less
     * than getCellCount()
     */
    public void insertCell(final int index, final char ch) {
        cells.add(index, new Cell(ch));
    }

    /**
     * Inserts a Cell to render for a specific fixed-width cell.
     *
     * @param index a cell number, between 0 and getCellCount() - 1
     * @param cell the new value for this fixed-width cell
     * @throws IndexOutOfBoundsException if index is negative or not less
     * than getCellCount()
     */
    public void insertCell(final int index, final Cell cell) {
        cells.add(index, cell);
    }

    /**
     * Delete a specific fixed-width cell.
     *
     * @param index a cell number, between 0 and getCellCount() - 1
     * @throws IndexOutOfBoundsException if index is negative or not less
     * than getCellCount()
     */
    public void deleteCell(final int index) {
        cells.remove(index);
    }

    /**
     * Delete several fixed-width cells.
     *
     * @param start a cell number, between 0 and getCellCount() - 1
     * @param length the number of cells to delete
     * @throws IndexOutOfBoundsException if index is negative or not less
     * than getCellCount()
     */
    public void deleteCells(final int start, final int length) {
        for (int i = 0; i < length; i++) {
            cells.remove(start);
        }
    }

    /**
     * Appends a char to render for a specific fixed-width cell.
     *
     * @param ch the character for this fixed-width cell
     */
    public void appendCell(final char ch) {
        cells.add(new Cell(ch));
    }

    /**
     * Inserts a Cell to render for a specific fixed-width cell.
     *
     * @param cell the new value for this fixed-width cell
     */
    public void appendCell(final Cell cell) {
        cells.add(cell);
    }

    /**
     * Get the next Fragment in the list, or null if this Fragment is the
     * last node.
     *
     * @return the next Fragment, or null
     */
    public Fragment next() {
        return nextFrag;
    }

    /**
     * Get the previous Fragment in the list, or null if this Fragment is the
     * first node.
     *
     * @return the previous Fragment, or null
     */
    public Fragment prev() {
        return prevFrag;
    }

    /**
     * See if this Fragment can be joined with the next Fragment in list.
     *
     * @return true if the join was possible, false otherwise
     */
    public boolean isNextJoinable() {
        if ((nextFrag != null) && (nextFrag instanceof Line)) {
            return true;
        }
        return false;
    }

    /**
     * Join this Fragment with the next Fragment in list.
     *
     * @return true if the join was successful, false otherwise
     */
    public boolean joinNext() {
        if ((nextFrag == null) || !(nextFrag instanceof Line)) {
            return false;
        }
        Line q = (Line) nextFrag;
        ArrayList<Cell> newCells = new ArrayList<Cell>(this.cells.size() +
            q.cells.size());
        newCells.addAll(this.cells);
        newCells.addAll(q.cells);
        this.cells = newCells;
        ((Line) q.nextFrag).prevFrag = this;
        nextFrag = q.nextFrag;
        return true;
    }

    /**
     * See if this Fragment can be joined with the previous Fragment in list.
     *
     * @return true if the join was possible, false otherwise
     */
    public boolean isPrevJoinable() {
        if ((prevFrag != null) && (prevFrag instanceof Line)) {
            return true;
        }
        return false;
    }

    /**
     * Join this Fragment with the previous Fragment in list.
     *
     * @return true if the join was successful, false otherwise
     */
    public boolean joinPrev() {
        if ((prevFrag == null) || !(prevFrag instanceof Line)) {
            return false;
        }
        Line p = (Line) prevFrag;
        ArrayList<Cell> newCells = new ArrayList<Cell>(this.cells.size() +
            p.cells.size());
        newCells.addAll(p.cells);
        newCells.addAll(this.cells);
        this.cells = newCells;
        ((Line) p.prevFrag).nextFrag = this;
        prevFrag = p.prevFrag;
        return true;
    }

    /**
     * Set the next Fragment in the list.  Note that this performs no sanity
     * checking or modifications on fragment; this function can break
     * connectivity in the list.
     *
     * @param fragment the next Fragment, or null
     */
    public void setNext(Fragment fragment) {
        nextFrag = fragment;
    }

    /**
     * Set the previous Fragment in the list.  Note that this performs no
     * sanity checking or modifications on fragment; this function can break
     * connectivity in the list.
     *
     * @param fragment the previous Fragment, or null
     */
    public void setPrev(Fragment fragment) {
        prevFrag = fragment;
    }

    /**
     * Split this Fragment into two.  'this' Fragment will contain length
     * cells, 'this.next()' will contain (getCellCount() - length) cells.
     *
     * @param length the number of cells to leave in this Fragment
     * @throws IndexOutOfBoundsException if length is negative, or 0, greater
     * than (getCellCount() - 1)
     */
    public void split(final int length) {
        // Create the next node
        Line q = new Line();
        q.nextFrag = nextFrag;
        q.prevFrag = this;
        ((Line) nextFrag).prevFrag = q;
        nextFrag = q;

        // Split cells
        q.cells = new ArrayList<Cell>(cells.size() - length);
        q.cells.addAll(cells.subList(length, cells.size()));
        cells = cells.subList(0, length);
    }

    /**
     * Insert a new Fragment at a position, splitting the contents of this
     * Fragment into two around it.  'this' Fragment will contain the cells
     * between 0 and index, 'this.next()' will be the inserted fragment, and
     * 'this.next().next()' will contain the cells between 'index' and
     * getCellCount() - 1.
     *
     * @param index the number of cells to leave in this Fragment
     * @param fragment the Fragment to insert
     * @throws IndexOutOfBoundsException if length is negative, or 0, greater
     * than (getCellCount() - 1)
     */
    public void split(final int index, Fragment fragment) {
        // Create the next node and insert into the list.
        Line q = new Line();
        q.nextFrag = nextFrag;
        q.nextFrag.setPrev(q);
        q.prevFrag = fragment;
        fragment.setNext(q);
        fragment.setPrev(this);
        nextFrag = fragment;

        // Split cells
        q.cells = new ArrayList<Cell>(cells.size() - index);
        q.cells.addAll(cells.subList(index, cells.size()));
        cells = cells.subList(0, index);
    }

    /**
     * Insert a new Fragment before this one.
     *
     * @param fragment the Fragment to insert
     */
    public void insert(Fragment fragment) {
        fragment.setNext(this);
        fragment.setPrev(prevFrag);
        prevFrag.setNext(fragment);
        prevFrag = fragment;
    }

    /**
     * Append a new Fragment at the end of this one.
     *
     * @param fragment the Fragment to append
     */
    public void append(Fragment fragment) {
        fragment.setNext(nextFrag);
        fragment.setPrev(this);
        nextFrag.setPrev(fragment);
        nextFrag = fragment;
    }

    /**
     * Delete this Fragment from the list, and return its next().
     *
     * @return this Fragment's next(), or null if it was at the end of the
     * list
     */
    public Fragment deleteGetNext() {
        Fragment result = nextFrag;
        nextFrag.setPrev(prevFrag);
        prevFrag.setNext(nextFrag);
        prevFrag = null;
        nextFrag = null;
        return result;
    }

    /**
     * Delete this Fragment from the list, and return its prev().
     *
     * @return this Fragment's next(), or null if it was at the beginning of
     * the list
     */
    public Fragment deleteGetPrev() {
        Fragment result = prevFrag;
        nextFrag.setPrev(prevFrag);
        prevFrag.setNext(nextFrag);
        prevFrag = null;
        nextFrag = null;
        return result;
    }

    /**
     * Get the anchor position.
     *
     * @return the anchor number
     */
    public int getAnchor() {
        return lineNumber;
    }

    /**
     * Set the anchor position.
     *
     * @param x the new anchor number
     */
    public void setAnchor(final int x) {
        lineNumber = x;
    }

}
