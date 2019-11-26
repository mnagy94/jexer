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
package jexer;

import java.util.ArrayList;
import java.util.List;

import jexer.bits.CellAttributes;
import jexer.bits.StringUtils;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import static jexer.TKeypress.*;

/**
 * TList shows a list of strings, and lets the user select one.
 */
public class TList extends TScrollableWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The list of strings to display.
     */
    private List<String> strings;

    /**
     * Selected string.
     */
    private int selectedString = -1;

    /**
     * Maximum width of a single line.
     */
    private int maxLineWidth;

    /**
     * The action to perform when the user selects an item (double-clicks or
     * enter).
     */
    protected TAction enterAction = null;

    /**
     * The action to perform when the user selects an item (single-click).
     */
    protected TAction singleClickAction = null;

    /**
     * The action to perform when the user navigates with keyboard.
     */
    protected TAction moveAction = null;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param strings list of strings to show
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     */
    public TList(final TWidget parent, final List<String> strings, final int x,
        final int y, final int width, final int height) {

        this(parent, strings, x, y, width, height, null, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param strings list of strings to show.  This is allowed to be null
     * and set later with setList() or by subclasses.
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param enterAction action to perform when an item is selected
     */
    public TList(final TWidget parent, final List<String> strings, final int x,
        final int y, final int width, final int height,
        final TAction enterAction) {

        this(parent, strings, x, y, width, height, enterAction, null, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param strings list of strings to show.  This is allowed to be null
     * and set later with setList() or by subclasses.
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param enterAction action to perform when an item is selected
     * @param moveAction action to perform when the user navigates to a new
     * item with arrow/page keys
     */
    public TList(final TWidget parent, final List<String> strings, final int x,
        final int y, final int width, final int height,
        final TAction enterAction, final TAction moveAction) {

        this(parent, strings, x, y, width, height, enterAction, moveAction,
            null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param strings list of strings to show.  This is allowed to be null
     * and set later with setList() or by subclasses.
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of text area
     * @param height height of text area
     * @param enterAction action to perform when an item is selected
     * @param moveAction action to perform when the user navigates to a new
     * item with arrow/page keys
     * @param singleClickAction action to perform when the user clicks on an
     * item
     */
    public TList(final TWidget parent, final List<String> strings, final int x,
        final int y, final int width, final int height,
        final TAction enterAction, final TAction moveAction,
        final TAction singleClickAction) {

        super(parent, x, y, width, height);
        this.enterAction = enterAction;
        this.moveAction = moveAction;
        this.singleClickAction = singleClickAction;
        this.strings = new ArrayList<String>();
        if (strings != null) {
            this.strings.addAll(strings);
        }

        hScroller = new THScroller(this, 0, getHeight() - 1, getWidth() - 1);
        vScroller = new TVScroller(this, getWidth() - 1, 0, getHeight() - 1);
        reflowData();
    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouseWheelUp()) {
            verticalDecrement();
            return;
        }
        if (mouse.isMouseWheelDown()) {
            verticalIncrement();
            return;
        }

        if ((mouse.getX() < getWidth() - 1)
            && (mouse.getY() < getHeight() - 1)
        ) {
            if (getVerticalValue() + mouse.getY() < strings.size()) {
                selectedString = getVerticalValue() + mouse.getY();
                dispatchSingleClick();
            }
            return;
        }

        // Pass to children
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse double click.
     *
     * @param mouse mouse double click event
     */
    @Override
    public void onMouseDoubleClick(final TMouseEvent mouse) {
        if ((mouse.getX() < getWidth() - 1)
            && (mouse.getY() < getHeight() - 1)
        ) {
            if (getVerticalValue() + mouse.getY() < strings.size()) {
                selectedString = getVerticalValue() + mouse.getY();
                dispatchEnter();
            }
            return;
        }

        // Pass to children
        super.onMouseDoubleClick(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbLeft)) {
            horizontalDecrement();
        } else if (keypress.equals(kbRight)) {
            horizontalIncrement();
        } else if (keypress.equals(kbUp)) {
            if (strings.size() > 0) {
                if (selectedString >= 0) {
                    if (selectedString > 0) {
                        if (selectedString - getVerticalValue() == 0) {
                            verticalDecrement();
                        }
                        selectedString--;
                    }
                } else {
                    selectedString = strings.size() - 1;
                }
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.equals(kbDown)) {
            if (strings.size() > 0) {
                if (selectedString >= 0) {
                    if (selectedString < strings.size() - 1) {
                        selectedString++;
                        if (selectedString - getVerticalValue() == getHeight() - 1) {
                            verticalIncrement();
                        }
                    }
                } else {
                    selectedString = 0;
                }
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.equals(kbPgUp)) {
            bigVerticalDecrement();
            if (selectedString >= 0) {
                selectedString -= getHeight() - 1;
                if (selectedString < 0) {
                    selectedString = 0;
                }
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.equals(kbPgDn)) {
            bigVerticalIncrement();
            if (selectedString >= 0) {
                selectedString += getHeight() - 1;
                if (selectedString > strings.size() - 1) {
                    selectedString = strings.size() - 1;
                }
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.equals(kbHome)) {
            toTop();
            if (strings.size() > 0) {
                selectedString = 0;
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.equals(kbEnd)) {
            toBottom();
            if (strings.size() > 0) {
                selectedString = strings.size() - 1;
            }
            if (selectedString >= 0) {
                dispatchMove();
            }
        } else if (keypress.equals(kbTab)) {
            getParent().switchWidget(true);
        } else if (keypress.equals(kbShiftTab) || keypress.equals(kbBackTab)) {
            getParent().switchWidget(false);
        } else if (keypress.equals(kbEnter)) {
            if (selectedString >= 0) {
                dispatchEnter();
            }
        } else {
            // Pass other keys (tab etc.) on
            super.onKeypress(keypress);
        }
    }

    // ------------------------------------------------------------------------
    // TScrollableWidget ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget's width: we need to set child widget widths.
     *
     * @param width new widget width
     */
    @Override
    public void setWidth(final int width) {
        super.setWidth(width);
        if (hScroller != null) {
            hScroller.setWidth(getWidth() - 1);
        }
        if (vScroller != null) {
            vScroller.setX(getWidth() - 1);
        }
    }

    /**
     * Override TWidget's height: we need to set child widget heights.
     *
     * @param height new widget height
     */
    @Override
    public void setHeight(final int height) {
        super.setHeight(height);
        if (hScroller != null) {
            hScroller.setY(getHeight() - 1);
        }
        if (vScroller != null) {
            vScroller.setHeight(getHeight() - 1);
        }
    }

    /**
     * Resize for a new width/height.
     */
    @Override
    public void reflowData() {

        // Reset the lines
        selectedString = -1;
        maxLineWidth = 0;

        for (int i = 0; i < strings.size(); i++) {
            String line = strings.get(i);
            int lineLength = StringUtils.width(line);
            if (lineLength > maxLineWidth) {
                maxLineWidth = lineLength;
            }
        }

        setBottomValue(strings.size() - getHeight() + 1);
        if (getBottomValue() < 0) {
            setBottomValue(0);
        }

        setRightValue(maxLineWidth - getWidth() + 1);
        if (getRightValue() < 0) {
            setRightValue(0);
        }
    }

    /**
     * Draw the list.
     */
    @Override
    public void draw() {
        CellAttributes color = null;
        int begin = getVerticalValue();
        int topY = 0;
        for (int i = begin; i < strings.size(); i++) {
            String line = strings.get(i);
            if (line == null) {
                line = "";
            }
            if (getHorizontalValue() < line.length()) {
                line = line.substring(getHorizontalValue());
            } else {
                line = "";
            }
            if (i == selectedString) {
                if (isAbsoluteActive()) {
                    color = getTheme().getColor("tlist.selected");
                } else {
                    color = getTheme().getColor("tlist.selected.inactive");
                }
            } else if (isAbsoluteActive()) {
                color = getTheme().getColor("tlist");
            } else {
                color = getTheme().getColor("tlist.inactive");
            }
            String formatString = "%-" + Integer.toString(getWidth() - 1) + "s";
            putStringXY(0, topY, String.format(formatString, line), color);
            topY++;
            if (topY >= getHeight() - 1) {
                break;
            }
        }

        if (isAbsoluteActive()) {
            color = getTheme().getColor("tlist");
        } else {
            color = getTheme().getColor("tlist.inactive");
        }

        // Pad the rest with blank lines
        for (int i = topY; i < getHeight() - 1; i++) {
            hLineXY(0, i, getWidth() - 1, ' ', color);
        }
    }

    // ------------------------------------------------------------------------
    // TList ------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the selection index.
     *
     * @return -1 if nothing is selected, otherwise the index into the list
     */
    public final int getSelectedIndex() {
        return selectedString;
    }

    /**
     * Set the selected string index.
     *
     * @param index -1 to unselect, otherwise the index into the list
     */
    public final void setSelectedIndex(final int index) {
        selectedString = index;
    }

    /**
     * Get a selectable string by index.
     *
     * @param idx index into list
     * @return the string at idx in the list
     */
    public final String getListItem(final int idx) {
        return strings.get(idx);
    }

    /**
     * Get the selected string.
     *
     * @return the selected string, or null of nothing is selected yet
     */
    public final String getSelected() {
        if ((selectedString >= 0) && (selectedString <= strings.size() - 1)) {
            return strings.get(selectedString);
        }
        return null;
    }

    /**
     * Get the maximum selection index value.
     *
     * @return -1 if the list is empty
     */
    public final int getMaxSelectedIndex() {
        return strings.size() - 1;
    }

    /**
     * Get a copy of the list of strings to display.
     *
     * @return the list of strings
     */
    public final List<String> getList() {
        return new ArrayList<String>(strings);
    }

    /**
     * Set the new list of strings to display.
     *
     * @param list new list of strings
     */
    public final void setList(final List<String> list) {
        strings.clear();
        strings.addAll(list);
        reflowData();
    }

    /**
     * Perform user selection action.
     */
    public void dispatchEnter() {
        assert (selectedString >= 0);
        assert (selectedString < strings.size());
        if (enterAction != null) {
            enterAction.DO(this);
        }
    }

    /**
     * Perform list movement action.
     */
    public void dispatchMove() {
        assert (selectedString >= 0);
        assert (selectedString < strings.size());
        if (moveAction != null) {
            moveAction.DO(this);
        }
    }

    /**
     * Perform single-click action.
     */
    public void dispatchSingleClick() {
        assert (selectedString >= 0);
        assert (selectedString < strings.size());
        if (singleClickAction != null) {
            singleClickAction.DO(this);
        }
    }

}
