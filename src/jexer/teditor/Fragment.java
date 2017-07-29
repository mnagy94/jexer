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

/**
 * A Fragment is the root "item" to be operated upon by the editor.  Each
 * Fragment is a "piece of the stream" that will be rendered.
 *
 * Fragments are organized as a doubly-linked list.  The have operations for
 * traversing the list, splitting a Fragment into two, and joining two
 * Fragments into one.
 */
public interface Fragment {

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
    public int getCellCount();

    /**
     * Get the next Fragment in the list, or null if this Fragment is the
     * last node.
     *
     * @return the next Fragment, or null
     */
    public Fragment next();

    /**
     * Set the next Fragment in the list.  Note that this performs no sanity
     * checking or modifications on fragment; this function can break
     * connectivity in the list.
     *
     * @param fragment the next Fragment, or null
     */
    public void setNext(final Fragment fragment);

    /**
     * Get the previous Fragment in the list, or null if this Fragment is the
     * first node.
     *
     * @return the previous Fragment, or null
     */
    public Fragment prev();

    /**
     * Set the previous Fragment in the list.  Note that this performs no
     * sanity checking or modifications on fragment; this function can break
     * connectivity in the list.
     *
     * @param fragment the previous Fragment, or null
     */
    public void setPrev(final Fragment fragment);

    /**
     * See if this Fragment can be joined with the next Fragment in list.
     *
     * @return true if the join was possible, false otherwise
     */
    public boolean isNextJoinable();

    /**
     * Join this Fragment with the next Fragment in list.
     *
     * @return true if the join was successful, false otherwise
     */
    public boolean joinNext();

    /**
     * See if this Fragment can be joined with the previous Fragment in list.
     *
     * @return true if the join was possible, false otherwise
     */
    public boolean isPrevJoinable();

    /**
     * Join this Fragment with the previous Fragment in list.
     *
     * @return true if the join was successful, false otherwise
     */
    public boolean joinPrev();

    /**
     * Split this Fragment into two.  'this' Fragment will contain length
     * cells, 'this.next()' will contain (getCellCount() - length) cells.
     *
     * @param length the number of cells to leave in this Fragment
     * @throws IndexOutOfBoundsException if length is negative, or 0, greater
     * than (getCellCount() - 1)
     */
    public void split(final int length);

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
    public void split(final int index, Fragment fragment);

    /**
     * Insert a new Fragment before this one.
     *
     * @param fragment the Fragment to insert
     */
    public void insert(Fragment fragment);

    /**
     * Append a new Fragment at the end of this one.
     *
     * @param fragment the Fragment to append
     */
    public void append(Fragment fragment);

    /**
     * Delete this Fragment from the list, and return its next().
     *
     * @return this Fragment's next(), or null if it was at the end of the
     * list
     */
    public Fragment deleteGetNext();

    /**
     * Delete this Fragment from the list, and return its prev().
     *
     * @return this Fragment's next(), or null if it was at the beginning of
     * the list
     */
    public Fragment deleteGetPrev();

    /**
     * Get the anchor position.
     *
     * @return the anchor number
     */
    public int getAnchor();

    /**
     * Set the anchor position.
     *
     * @param x the new anchor number
     */
    public void setAnchor(final int x);

}
