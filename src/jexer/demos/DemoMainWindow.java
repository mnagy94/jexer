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
package jexer.demos;

import jexer.*;

/**
 * This is the main "demo" application window.  It makes use of the TTimer,
 * TProgressBox, TLabel, TButton, and TField widgets.
 */
public class DemoMainWindow extends TWindow {

    // Timer that increments a number.
    private TTimer timer;

    // Timer label is updated with timer ticks.
    TLabel timerLabel;

    /**
     * We need to override onClose so that the timer will no longer be called
     * after we close the window.  TTimers currently are completely unaware
     * of the rest of the UI classes.
     */
    @Override
    public void onClose() {
        getApplication().removeTimer(timer);
    }

    /**
     * Construct demo window.  It will be centered on screen.
     *
     * @param parent the main application
     */
    public DemoMainWindow(final TApplication parent) {
        this(parent, CENTERED | RESIZABLE);
    }

    // These are used by the timer loop.  They have to be at class scope so
    // that they can be accessed by the anonymous TAction class.
    int timerI = 0;
    TProgressBar progressBar;

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    private DemoMainWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it will be
        // centered on screen.
        super(parent, "Demo Window", 0, 0, 60, 24, flags);

        int row = 1;

        // Add some widgets
        if (!isModal()) {
            addLabel("Message Boxes", 1, row);
            addButton("&MessageBoxes", 35, row,
                new TAction() {
                    public void DO() {
                        new DemoMsgBoxWindow(getApplication());
                    }
                }
            );
        }
        row += 2;

        addLabel("Open me as modal", 1, row);
        addButton("W&indow", 35, row,
            new TAction() {
                public void DO() {
                    new DemoMainWindow(getApplication(), MODAL);
                }
            }
        );

        row += 2;

        addLabel("Variable-width text field:", 1, row);
        addField(35, row++, 15, false, "Field text");
        addLabel("Fixed-width text field:", 1, row);
        addField(35, row++, 15, true);
        addLabel("Variable-width password:", 1, row);
        addPasswordField(35, row++, 15, false);
        addLabel("Fixed-width password:", 1, row);
        addPasswordField(35, row++, 15, true, "hunter2");
        row += 1;

        if (!isModal()) {
            addLabel("Radio buttons and checkboxes", 1, row);
            addButton("&Checkboxes", 35, row,
                new TAction() {
                    public void DO() {
                        new DemoCheckboxWindow(getApplication());
                    }
                }
            );
        }
        row += 2;

        /*
        if (!isModal()) {
            addLabel("Editor window", 1, row);
            addButton("Edito&r", 35, row,
                {
                    new TEditor(application, 0, 0, 60, 15);
                }
            );
        }
        row += 2;
         */

        if (!isModal()) {
            addLabel("Text areas", 1, row);
            addButton("&Text", 35, row,
                new TAction() {
                    public void DO() {
                        new DemoTextWindow(getApplication());
                    }
                }
            );
        }
        row += 2;

        if (!isModal()) {
            addLabel("Tree views", 1, row);
            addButton("Tree&View", 35, row,
                new TAction() {
                    public void DO() {
                        try {
                            new DemoTreeViewWindow(getApplication());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            );
        }
        row += 2;

        if (!isModal()) {
            addLabel("Terminal", 1, row);
            addButton("Termi&nal", 35, row,
                new TAction() {
                    public void DO() {
                        getApplication().openTerminal(0, 0);
                    }
                }
            );
        }
        row += 2;

        if (!isModal()) {
            addLabel("Color editor", 1, row);
            addButton("Co&lors", 35, row,
                new TAction() {
                    public void DO() {
                        new TEditColorThemeWindow(getApplication());
                    }
                }
            );
        }
        row += 2;

        progressBar = addProgressBar(1, row, 22, 0);
        row++;
        timerLabel = addLabel("Timer", 1, row);
        timer = getApplication().addTimer(250, true,
            new TAction() {

                public void DO() {
                    timerLabel.setLabel(String.format("Timer: %d", timerI));
                    timerLabel.setWidth(timerLabel.getLabel().length());
                    if (timerI < 100) {
                        timerI++;
                    }
                    progressBar.setValue(timerI);
                }
            }
        );
    }
}
