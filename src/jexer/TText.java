/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2016 Kevin Lamonte
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

import static jexer.TKeypress.kbDown;
import static jexer.TKeypress.kbEnd;
import static jexer.TKeypress.kbHome;
import static jexer.TKeypress.kbLeft;
import static jexer.TKeypress.kbPgDn;
import static jexer.TKeypress.kbPgUp;
import static jexer.TKeypress.kbRight;
import static jexer.TKeypress.kbUp;

import java.util.LinkedList;
import java.util.List;

import jexer.bits.CellAttributes;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;

/**
 * TText implements a simple scrollable text area. It reflows automatically on
 * resize.
 */
public final class TText extends TWidget {

    /**
     * Text to display.
     */
    private String text;

    /**
     * Text converted to lines.
     */
    private List<String> lines;

    /**
     * Text color.
     */
    private String colorKey;

    /**
     * Vertical scrollbar.
     */
    private TVScroller vScroller;

    /**
     * Horizontal scrollbar.
     */
    private THScroller hScroller;

    /**
     * Maximum width of a single line.
     */
    private int maxLineWidth;

    /**
     * Number of lines between each paragraph.
     */
    private int lineSpacing = 1;

    /**
     * Convenience method used by TWindowLoggerOutput.
     *
     * @param line
     *            new line to add
     */
    public void addLine(final String line) {
        if (text.length() == 0) {
            text = line;
        } else {
            text += "\n\n";
            text += line;
        }
        reflow();
    }

    /**
     * Recompute the bounds for the scrollbars.
     */
    private void computeBounds() {
        maxLineWidth = 0;
        for (String line : lines) {
            if (line.length() > maxLineWidth) {
                maxLineWidth = line.length();
            }
        }

        vScroller.setBottomValue((lines.size() - getHeight()) + 1);
        if (vScroller.getBottomValue() < 0) {
            vScroller.setBottomValue(0);
        }
        if (vScroller.getValue() > vScroller.getBottomValue()) {
            vScroller.setValue(vScroller.getBottomValue());
        }

        hScroller.setRightValue((maxLineWidth - getWidth()) + 1);
        if (hScroller.getRightValue() < 0) {
            hScroller.setRightValue(0);
        }
        if (hScroller.getValue() > hScroller.getRightValue()) {
            hScroller.setValue(hScroller.getRightValue());
        }
    }

    /**
     * Insert newlines into a string to wrap it to a maximum column. Terminate
     * the final string with a newline. Note that interior newlines are
     * converted to spaces.
     *
     * @param str
     *            the string
     * @param n
     *            the maximum number of characters in a line
     * @return the wrapped string
     */
    private String wrap(final String str, final int n) {
        assert (n > 0);

        StringBuilder sb = new StringBuilder();
        StringBuilder word = new StringBuilder();
        int col = 0;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\n') {
                ch = ' ';
            }
            if (ch == ' ') {
                sb.append(word.toString());
                sb.append(ch);
                if (word.length() >= (n - 1)) {
                    sb.append('\n');
                    col = 0;
                }
                word = new StringBuilder();
            } else {
                word.append(ch);
            }

            col++;
            if (col >= (n - 1)) {
                sb.append('\n');
                col = 0;
            }
        }
        sb.append(word.toString());
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Resize text and scrollbars for a new width/height.
     */
    public void reflow() {
        // Reset the lines
        lines.clear();

        // Break up text into paragraphs
        String[] paragraphs = text.split("\n\n");
        for (String p : paragraphs) {
            String paragraph = wrap(p, getWidth() - 1);
            for (String line : paragraph.split("\n")) {
                lines.add(line);
            }
            for (int i = 0; i < lineSpacing; i++) {
                lines.add("");
            }
        }

        // Start at the top
        if (vScroller == null) {
            vScroller = new TVScroller(this, getWidth() - 1, 0, getHeight() - 1);
            vScroller.setTopValue(0);
            vScroller.setValue(0);
        } else {
            vScroller.setX(getWidth() - 1);
            vScroller.setHeight(getHeight() - 1);
        }
        vScroller.setBigChange(getHeight() - 1);

        // Start at the left
        if (hScroller == null) {
            hScroller = new THScroller(this, 0, getHeight() - 1, getWidth() - 1);
            hScroller.setLeftValue(0);
            hScroller.setValue(0);
        } else {
            hScroller.setY(getHeight() - 1);
            hScroller.setWidth(getWidth() - 1);
        }
        hScroller.setBigChange(getWidth() - 1);

        computeBounds();
    }

    /**
     * Public constructor.
     *
     * @param parent
     *            parent widget
     * @param text
     *            text on the screen
     * @param x
     *            column relative to parent
     * @param y
     *            row relative to parent
     * @param width
     *            width of text area
     * @param height
     *            height of text area
     */
    public TText(final TWidget parent, final String text, final int x,
            final int y, final int width, final int height) {

        this(parent, text, x, y, width, height, "ttext");
    }

    /**
     * Public constructor.
     *
     * @param parent
     *            parent widget
     * @param text
     *            text on the screen
     * @param x
     *            column relative to parent
     * @param y
     *            row relative to parent
     * @param width
     *            width of text area
     * @param height
     *            height of text area
     * @param colorKey
     *            ColorTheme key color to use for foreground text. Default is
     *            "ttext"
     */
    public TText(final TWidget parent, final String text, final int x,
            final int y, final int width, final int height,
            final String colorKey) {

        // Set parent and window
        super(parent, x, y, width, height);

        this.text = text;
        this.colorKey = colorKey;

        lines = new LinkedList<String>();

        reflow();
    }

    /**
     * Draw the text box.
     */
    @Override
    public void draw() {
        // Setup my color
        CellAttributes color = getTheme().getColor(colorKey);

        int begin = vScroller.getValue();
        int topY = 0;
        for (int i = begin; i < lines.size(); i++) {
            String line = lines.get(i);
            if (hScroller.getValue() < line.length()) {
                line = line.substring(hScroller.getValue());
            } else {
                line = "";
            }
            String formatString = "%-" + Integer.toString(getWidth() - 1) + "s";
            getScreen().putStringXY(0, topY, String.format(formatString, line),
                    color);
            topY++;

            if (topY >= (getHeight() - 1)) {
                break;
            }
        }

        // Pad the rest with blank lines
        for (int i = topY; i < (getHeight() - 1); i++) {
            getScreen().hLineXY(0, i, getWidth() - 1, ' ', color);
        }

    }

    /**
     * Handle mouse press events.
     *
     * @param mouse
     *            mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (mouse.isMouseWheelUp()) {
            vScroller.decrement();
            return;
        }
        if (mouse.isMouseWheelDown()) {
            vScroller.increment();
            return;
        }

        // Pass to children
        super.onMouseDown(mouse);
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress
     *            keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {
        if (keypress.equals(kbLeft)) {
            hScroller.decrement();
        } else if (keypress.equals(kbRight)) {
            hScroller.increment();
        } else if (keypress.equals(kbUp)) {
            vScroller.decrement();
        } else if (keypress.equals(kbDown)) {
            vScroller.increment();
        } else if (keypress.equals(kbPgUp)) {
            vScroller.bigDecrement();
        } else if (keypress.equals(kbPgDn)) {
            vScroller.bigIncrement();
        } else if (keypress.equals(kbHome)) {
            vScroller.toTop();
        } else if (keypress.equals(kbEnd)) {
            vScroller.toBottom();
        } else {
            // Pass other keys (tab etc.) on
            super.onKeypress(keypress);
        }
    }

}
