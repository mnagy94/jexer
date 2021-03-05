/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2021 Autumn Lamonte
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
 * @author Autumn Lamonte [AutumnWalksTheLake@gmail.com] ⚧ Trans Liberation Now
 * @version 1
 */
package jexer.bits;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import jexer.TWidget;
import jexer.TWindow;
import jexer.event.TResizeEvent;

/**
 * WidgetUtils contains methods to:
 *
 *    - Tile windows.
 */
public class WidgetUtils {

    /**
     * Re-layout a list of widgets as non-overlapping tiles into a
     * rectangular space.  This produces almost the same results as Turbo
     * Pascal 7.0's IDE.
     *
     * @param widgets the list of widgets
     * @param width the width of the rectangle to fit the widgets in
     * @param height the height of the rectangle to fit the widgets in
     * @param topLine the Y top use for a top-line widget
     */
    public static void tileWidgets(final List<? extends TWidget> widgets,
        final int width, final int height, final int topLine) {

        int z = widgets.size();
        for (TWidget w: widgets) {
            if (w instanceof TWindow) {
                if (((TWindow) w).isHidden()) {
                    z--;
                }
                continue;
            }
            if (!w.isVisible()) {
                z--;
            }
        }
        if (z == 0) {
            return;
        }
        assert (z > 0);

        int a = 0;
        int b = 0;
        a = (int)(Math.sqrt(z));
        int c = 0;
        while (c < a) {
            b = (z - c) / a;
            if (((a * b) + c) == z) {
                break;
            }
            c++;
        }
        assert (a > 0);
        assert (b > 0);
        assert (c < a);
        int newWidth = width / a;
        int newHeight1 = height / b;
        int newHeight2 = height / (b + c);

        List<TWidget> sorted = new ArrayList<TWidget>();
        for (TWidget w: widgets) {
            if (w instanceof TWindow) {
                if (((TWindow) w).isShown()) {
                    sorted.add(w);
                }
                continue;
            }
            if (w.isVisible()) {
                sorted.add(w);
            }
        }

        Collections.sort(sorted);
        if (sorted.get(0) instanceof TWindow) {
            Collections.reverse(sorted);
        }
        for (int i = 0; i < sorted.size(); i++) {
            int logicalX = i / b;
            int logicalY = i % b;
            if (i >= ((a - 1) * b)) {
                logicalX = a - 1;
                logicalY = i - ((a - 1) * b);
            }

            TWidget w = sorted.get(i);
            int oldWidth = w.getWidth();
            int oldHeight = w.getHeight();

            w.setX(logicalX * newWidth);
            w.setWidth(newWidth);
            if (i >= ((a - 1) * b)) {
                w.setY((logicalY * newHeight2) + topLine);
                w.setHeight(newHeight2);
            } else {
                w.setY((logicalY * newHeight1) + topLine);
                w.setHeight(newHeight1);
            }
            if ((w.getWidth() != oldWidth)
                || (w.getHeight() != oldHeight)
            ) {
                w.onResize(new TResizeEvent(null, TResizeEvent.Type.WIDGET,
                        w.getWidth(), w.getHeight()));
            }
        }
    }

}
