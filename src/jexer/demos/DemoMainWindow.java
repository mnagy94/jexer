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
package jexer.demos;

import java.io.*;
import java.util.*;

import jexer.*;
import jexer.event.*;
import static jexer.TCommand.*;
import static jexer.TKeypress.*;

/**
 * This is the main "demo" application window.  It makes use of the TTimer,
 * TProgressBox, TLabel, TButton, and TField widgets.
 */
public class DemoMainWindow extends TWindow {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Timer that increments a number.
     */
    private TTimer timer;

    /**
     * Timer label is updated with timer ticks.
     */
    TLabel timerLabel;

    /**
     * Timer increment used by the timer loop.  Has to be at class scope so
     * that it can be accessed by the anonymous TAction class.
     */
    int timerI = 0;

    /**
     * Progress bar used by the timer loop.  Has to be at class scope so that
     * it can be accessed by the anonymous TAction class.
     */
    TProgressBar progressBar;

    /**
     * Day of week label is updated with TSpinner clicks.
     */
    TLabel dayOfWeekLabel;

    /**
     * Day of week to demonstrate TSpinner.  Has to be at class scope so that
     * it can be accessed by the anonymous TAction class.
     */
    GregorianCalendar calendar = new GregorianCalendar();

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Construct demo window.  It will be centered on screen.
     *
     * @param parent the main application
     */
    public DemoMainWindow(final TApplication parent) {
        this(parent, CENTERED | RESIZABLE);
    }

    /**
     * Constructor.
     *
     * @param parent the main application
     * @param flags bitmask of MODAL, CENTERED, or RESIZABLE
     */
    private DemoMainWindow(final TApplication parent, final int flags) {
        // Construct a demo window.  X and Y don't matter because it will be
        // centered on screen.
        super(parent, "Demo Window", 0, 0, 64, 23, flags);

        int row = 1;

        // Add some widgets
        addLabel("Message Boxes", 1, row);
        TWidget first = addButton("&MessageBoxes", 35, row,
            new TAction() {
                public void DO() {
                    new DemoMsgBoxWindow(getApplication());
                }
            }
        );
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

        addLabel("Text fields and calendar", 1, row);
        addButton("Field&s", 35, row,
            new TAction() {
                public void DO() {
                    new DemoTextFieldWindow(getApplication());
                }
            }
        );
        row += 2;

        addLabel("Radio buttons, check and combobox", 1, row);
        addButton("&CheckBoxes", 35, row,
            new TAction() {
                public void DO() {
                    new DemoCheckBoxWindow(getApplication());
                }
            }
        );
        row += 2;

        addLabel("Editor window", 1, row);
        addButton("&1 Widget", 35, row,
            new TAction() {
                public void DO() {
                    new DemoEditorWindow(getApplication());
                }
            }
        );
        addButton("&2 Window", 48, row,
            new TAction() {
                public void DO() {
                    new TEditorWindow(getApplication());
                }
            }
        );
        row += 2;

        addLabel("Text areas", 1, row);
        addButton("&Text", 35, row,
            new TAction() {
                public void DO() {
                    new DemoTextWindow(getApplication());
                }
            }
        );
        row += 2;

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
        row += 2;

        addLabel("Terminal", 1, row);
        addButton("Termi&nal", 35, row,
            new TAction() {
                public void DO() {
                    getApplication().openTerminal(0, 0);
                }
            }
        );
        row += 2;

        addLabel("Color editor", 1, row);
        addButton("Co&lors", 35, row,
            new TAction() {
                public void DO() {
                    new TEditColorThemeWindow(getApplication());
                }
            }
        );
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
                    } else {
                        timer.setRecurring(false);
                    }
                    progressBar.setValue(timerI);
                }
            }
        );

        dayOfWeekLabel = addLabel("Wednesday-", 35, row - 1, "tmenu", false);
        dayOfWeekLabel.setLabel(String.format("%-10s",
                calendar.getDisplayName(Calendar.DAY_OF_WEEK,
                    Calendar.LONG, Locale.getDefault())));

        addSpinner(35 + dayOfWeekLabel.getWidth(), row - 1,
            new TAction() {
                public void DO() {
                    calendar.add(Calendar.DAY_OF_WEEK, 1);
                    dayOfWeekLabel.setLabel(String.format("%-10s",
                            calendar.getDisplayName(
                            Calendar.DAY_OF_WEEK, Calendar.LONG,
                            Locale.getDefault())));
                }
            },
            new TAction() {
                public void DO() {
                    calendar.add(Calendar.DAY_OF_WEEK, -1);
                    dayOfWeekLabel.setLabel(String.format("%-10s",
                            calendar.getDisplayName(
                            Calendar.DAY_OF_WEEK, Calendar.LONG,
                            Locale.getDefault())));
                }
            }
        );


        activate(first);

        statusBar = newStatusBar("Demo Main Window");
        statusBar.addShortcutKeypress(kbF1, cmHelp, "Help");
        statusBar.addShortcutKeypress(kbF2, cmShell, "Shell");
        statusBar.addShortcutKeypress(kbF3, cmOpen, "Open");
        statusBar.addShortcutKeypress(kbF10, cmExit, "Exit");
    }

    // ------------------------------------------------------------------------
    // TWindow ----------------------------------------------------------------
    // ------------------------------------------------------------------------

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
     * Method that subclasses can override to handle posted command events.
     *
     * @param command command event
     */
    @Override
    public void onCommand(final TCommandEvent command) {
        if (command.equals(cmOpen)) {
            try {
                String filename = fileOpenBox(".");
                if (filename != null) {
                    try {
                        new TEditorWindow(getApplication(), new File(filename));
                    } catch (IOException e) {
                        messageBox("Error", "Error reading file: " +
                            e.getMessage());
                    }
                }
            } catch (IOException e) {
                messageBox("Error", "Error opening file dialog: " +
                    e.getMessage());
            }
            return;
        }

        // Didn't handle it, let children get it instead
        super.onCommand(command);
    }

}
