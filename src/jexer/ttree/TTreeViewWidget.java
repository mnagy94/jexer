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
package jexer.ttree;

import jexer.TAction;
import jexer.THScroller;
import jexer.TKeypress;
import jexer.TScrollableWidget;
import jexer.TVScroller;
import jexer.TWidget;
import jexer.bits.StringUtils;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import static jexer.TKeypress.*;

/**
 * TTreeViewWidget wraps a tree view with horizontal and vertical scrollbars.
 */
public class TTreeViewWidget extends TScrollableWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The TTreeView
     */
    private TTreeView treeView;

    /**
     * If true, move the window to put the selected item in view.  This
     * normally only happens once after setting treeRoot.
     */
    private boolean centerWindow = false;

    /**
     * Maximum width of a single line.
     */
    private int maxLineWidth;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of tree view
     * @param height height of tree view
     */
    public TTreeViewWidget(final TWidget parent, final int x, final int y,
        final int width, final int height) {

        this(parent, x, y, width, height, null);
    }

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param width width of tree view
     * @param height height of tree view
     * @param action action to perform when an item is selected
     */
    public TTreeViewWidget(final TWidget parent, final int x, final int y,
        final int width, final int height, final TAction action) {

        super(parent, x, y, width, height);

        treeView = new TTreeView(this, 0, 0, getWidth() - 1, getHeight() - 1,
            action);

        vScroller = new TVScroller(this, getWidth() - 1, 0, getHeight() - 1);
        hScroller = new THScroller(this, 0, getHeight() - 1, getWidth() - 1);

    }

    // ------------------------------------------------------------------------
    // Event handlers ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    @Override
    public void onResize(final TResizeEvent event) {
        super.onResize(event);

        if (event.getType() == TResizeEvent.Type.WIDGET) {
            treeView.onResize(new TResizeEvent(TResizeEvent.Type.WIDGET,
                    getWidth() - 1, getHeight() - 1));
            return;
        } else {
            super.onResize(event);
        }
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouseWheelUp()) {
            verticalDecrement();
        } else if (mouse.isMouseWheelDown()) {
            verticalIncrement();
        } else {
            // Pass to the TreeView or scrollbars
            super.onMouseDown(mouse);
        }

        // Update the view to reflect the new scrollbar positions
        treeView.setTopLine(getVerticalValue());
        treeView.setLeftColumn(getHorizontalValue());
        reflowData();
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        // Pass to the TreeView or scrollbars
        super.onMouseUp(mouse);

        // Update the view to reflect the new scrollbar positions
        treeView.setTopLine(getVerticalValue());
        treeView.setLeftColumn(getHorizontalValue());
        reflowData();
    }

    /**
     * Handle mouse motion events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        // Pass to the TreeView or scrollbars
        super.onMouseMotion(mouse);

        // Update the view to reflect the new scrollbar positions
        treeView.setTopLine(getVerticalValue());
        treeView.setLeftColumn(getHorizontalValue());
        reflowData();
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbShiftLeft)
            || keypress.equals(kbCtrlLeft)
            || keypress.equals(kbAltLeft)
        ) {
            horizontalDecrement();
        } else if (keypress.equals(kbShiftRight)
            || keypress.equals(kbCtrlRight)
            || keypress.equals(kbAltRight)
        ) {
            horizontalIncrement();
        } else if (keypress.equals(kbShiftUp)
            || keypress.equals(kbCtrlUp)
            || keypress.equals(kbAltUp)
        ) {
            verticalDecrement();
        } else if (keypress.equals(kbShiftDown)
            || keypress.equals(kbCtrlDown)
            || keypress.equals(kbAltDown)
        ) {
            verticalIncrement();
        } else if (keypress.equals(kbShiftPgUp)
            || keypress.equals(kbCtrlPgUp)
            || keypress.equals(kbAltPgUp)
        ) {
            bigVerticalDecrement();
        } else if (keypress.equals(kbShiftPgDn)
            || keypress.equals(kbCtrlPgDn)
            || keypress.equals(kbAltPgDn)
        ) {
            bigVerticalIncrement();
        } else if (keypress.equals(kbPgDn)) {
            for (int i = 0; i < getHeight() - 2; i++) {
                treeView.onKeypress(new TKeypressEvent(TKeypress.kbDown));
            }
            reflowData();
            return;
        } else if (keypress.equals(kbPgUp)) {
            for (int i = 0; i < getHeight() - 2; i++) {
                treeView.onKeypress(new TKeypressEvent(TKeypress.kbUp));
            }
            reflowData();
            return;
        } else if (keypress.equals(kbHome)) {
            treeView.setSelected((TTreeItem) treeView.getChildren().get(0),
                false);
            treeView.setTopLine(0);
            reflowData();
            return;
        } else if (keypress.equals(kbEnd)) {
            treeView.setSelected((TTreeItem)  treeView.getChildren().get(
                treeView.getChildren().size() - 1), true);
            reflowData();
            return;
        } else if (keypress.equals(kbTab)) {
            getParent().switchWidget(true);
            return;
        } else if (keypress.equals(kbShiftTab)
                || keypress.equals(kbBackTab)) {
            getParent().switchWidget(false);
            return;
        } else {
            treeView.onKeypress(keypress);

            // Update the scrollbars to reflect the new data position
            reflowData();
            return;
        }

        // Update the view to reflect the new scrollbar position
        treeView.setTopLine(getVerticalValue());
        treeView.setLeftColumn(getHorizontalValue());
        reflowData();
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
        if (treeView != null) {
            treeView.setWidth(getWidth() - 1);
        }
        reflowData();
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
        if (treeView != null) {
            treeView.setHeight(getHeight() - 1);
        }
        reflowData();
    }

    /**
     * Resize text and scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {
        if (treeView == null) {
            return;
        }

        int selectedRow = 0;
        boolean foundSelectedRow = false;

        // Reset the keyboard list, expandTree() will recreate it.
        for (TWidget widget: treeView.getChildren()) {
            TTreeItem item = (TTreeItem) widget;
            item.keyboardPrevious = null;
            item.keyboardNext = null;
        }

        // Expand the tree into a linear list
        treeView.getChildren().clear();
        treeView.getChildren().addAll(treeView.getTreeRoot().expandTree("",
                true));

        // Locate the selected row and maximum line width
        for (TWidget widget: treeView.getChildren()) {
            TTreeItem item = (TTreeItem) widget;

            if (item == treeView.getSelected()) {
                foundSelectedRow = true;
            }
            if (!foundSelectedRow) {
                selectedRow++;
            }

            int lineWidth = StringUtils.width(item.getText())
                + item.getPrefix().length() + 4;
            if (lineWidth > maxLineWidth) {
                maxLineWidth = lineWidth;
            }
        }

        if ((centerWindow) && (foundSelectedRow)) {
            if ((selectedRow < getVerticalValue())
                || (selectedRow > getVerticalValue() + getHeight() - 2)
            ) {
                treeView.setTopLine(selectedRow);
                centerWindow = false;
            }
        }
        treeView.alignTree();

        // Rescale the scroll bars
        setVerticalValue(treeView.getTopLine());
        setBottomValue(treeView.getTotalLineCount() - (getHeight() - 1));
        if (getBottomValue() < getTopValue()) {
            setBottomValue(getTopValue());
        }
        if (getVerticalValue() > getBottomValue()) {
            setVerticalValue(getBottomValue());
        }
        setRightValue(maxLineWidth - 2);
        if (getHorizontalValue() > getRightValue()) {
            setHorizontalValue(getRightValue());
        }

    }

    // ------------------------------------------------------------------------
    // TTreeView --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the underlying TTreeView.
     *
     * @return the TTreeView
     */
    public TTreeView getTreeView() {
        return treeView;
    }

    /**
     * Get the root of the tree.
     *
     * @return the root of the tree
     */
    public final TTreeItem getTreeRoot() {
        return treeView.getTreeRoot();
    }

    /**
     * Set the root of the tree.
     *
     * @param treeRoot the new root of the tree
     */
    public final void setTreeRoot(final TTreeItem treeRoot) {
        treeView.setTreeRoot(treeRoot);
    }

    /**
     * Set treeRoot.
     *
     * @param treeRoot ultimate root of tree
     * @param centerWindow if true, move the window to put the root in view
     */
    public void setTreeRoot(final TTreeItem treeRoot,
        final boolean centerWindow) {

        treeView.setTreeRoot(treeRoot);
        this.centerWindow = centerWindow;
    }

    /**
     * Get the tree view item that was selected.
     *
     * @return the selected item, or null if no item is selected
     */
    public final TTreeItem getSelected() {
        return treeView.getSelected();
    }

    /**
     * Set the new selected tree view item.
     *
     * @param item new item that became selected
     * @param centerWindow if true, move the window to put the selected into
     * view
     */
    public void setSelected(final TTreeItem item, final boolean centerWindow) {
        treeView.setSelected(item, centerWindow);
    }

    /**
     * Perform user selection action.
     */
    public void dispatch() {
        treeView.dispatch();
    }

}
