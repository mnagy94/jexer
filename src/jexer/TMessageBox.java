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
package jexer;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import jexer.event.TKeypressEvent;
import static jexer.TKeypress.*;

/**
 * TMessageBox is a system-modal dialog with buttons for OK, Cancel, Yes, or
 * No.  Call it like:
 *
 * <pre>
 * {@code
 *     box = application.messageBox(title, caption,
 *         TMessageBox.Type.OK | TMessageBox.Type.CANCEL);
 *
 *     if (box.getResult() == TMessageBox.OK) {
 *         ... the user pressed OK, do stuff ...
 *     }
 * }
 * </pre>
 *
 */
public class TMessageBox extends TWindow {

    /**
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(TMessageBox.class.getName());

    /**
     * Message boxes have these supported types.
     */
    public enum Type {
        /**
         * Show an OK button.
         */
        OK,

        /**
         * Show both OK and Cancel buttons.
         */
        OKCANCEL,

        /**
         * Show both Yes and No buttons.
         */
        YESNO,

        /**
         * Show Yes, No, and Cancel buttons.
         */
        YESNOCANCEL
    };

    /**
     * The type of this message box.
     */
    private Type type;

    /**
     * My buttons.
     */
    private List<TButton> buttons;

    /**
     * Message boxes have these possible results.
     */
    public enum Result {
        /**
         * User clicked "OK".
         */
        OK,

        /**
         * User clicked "Cancel".
         */
        CANCEL,

        /**
         * User clicked "Yes".
         */
        YES,

        /**
         * User clicked "No".
         */
        NO
    };

    /**
     * Which button was clicked: OK, CANCEL, YES, or NO.
     */
    private Result result = Result.OK;

    /**
     * Get the result.
     *
     * @return the result: OK, CANCEL, YES, or NO.
     */
    public final Result getResult() {
        return result;
    }

    /**
     * Public constructor.  The message box will be centered on screen.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     */
    public TMessageBox(final TApplication application, final String title,
        final String caption) {

        this(application, title, caption, Type.OK, true);
    }

    /**
     * Public constructor.  The message box will be centered on screen.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @param type one of the Type constants.  Default is Type.OK.
     */
    public TMessageBox(final TApplication application, final String title,
        final String caption, final Type type) {

        this(application, title, caption, type, true);
    }

    /**
     * Public constructor.  The message box will be centered on screen.
     *
     * @param application TApplication that manages this window
     * @param title window title, will be centered along the top border
     * @param caption message to display.  Use embedded newlines to get a
     * multi-line box.
     * @param type one of the Type constants.  Default is Type.OK.
     * @param yield if true, yield this Thread.  Subclasses need to set this
     * to false and yield at their end of their constructor intead.
     */
    protected TMessageBox(final TApplication application, final String title,
        final String caption, final Type type, final boolean yield) {

        // Start as 50x50 at (1, 1).  These will be changed later.
        super(application, title, 1, 1, 100, 100, CENTERED | MODAL);

        // Hang onto type so that we can provide more convenience in
        // onKeypress().
        this.type = type;

        // Determine width and height
        String [] lines = caption.split("\n");
        int width = title.length() + 12;
        setHeight(6 + lines.length);
        for (String line: lines) {
            if (line.length() + 4 > width) {
                width = line.length() + 4;
            }
        }
        setWidth(width);
        if (getWidth() > getScreen().getWidth()) {
            setWidth(getScreen().getWidth());
        }
        // Re-center window to get an appropriate (x, y)
        center();

        // Now add my elements
        int lineI = 1;
        for (String line: lines) {
            addLabel(line, 1, lineI, "twindow.background.modal");
            lineI++;
        }

        // The button line
        lineI++;
        buttons = new ArrayList<TButton>();

        int buttonX = 0;

        // Setup button actions
        switch (type) {

        case OK:
            result = Result.OK;
            if (getWidth() < 15) {
                setWidth(15);
            }
            buttonX = (getWidth() - 11) / 2;
            buttons.add(addButton(i18n.getString("okButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.OK;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            break;

        case OKCANCEL:
            result = Result.CANCEL;
            if (getWidth() < 26) {
                setWidth(26);
            }
            buttonX = (getWidth() - 22) / 2;
            buttons.add(addButton(i18n.getString("okButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.OK;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            buttonX += 8 + 4;
            buttons.add(addButton(i18n.getString("cancelButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.CANCEL;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            break;

        case YESNO:
            result = Result.NO;
            if (getWidth() < 20) {
                setWidth(20);
            }
            buttonX = (getWidth() - 16) / 2;
            buttons.add(addButton(i18n.getString("yesButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.YES;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            buttonX += 5 + 4;
            buttons.add(addButton(i18n.getString("noButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.NO;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            break;

        case YESNOCANCEL:
            result = Result.CANCEL;
            if (getWidth() < 31) {
                setWidth(31);
            }
            buttonX = (getWidth() - 27) / 2;
            buttons.add(addButton(i18n.getString("yesButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.YES;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            buttonX += 5 + 4;
            buttons.add(addButton(i18n.getString("noButton"), buttonX, lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.NO;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            buttonX += 4 + 4;
            buttons.add(addButton(i18n.getString("cancelButton"), buttonX,
                    lineI,
                    new TAction() {
                        public void DO() {
                            result = Result.CANCEL;
                            getApplication().closeWindow(TMessageBox.this);
                        }
                    }
                )
            );
            break;

        default:
            throw new IllegalArgumentException("Invalid message box type: " + type);
        }

        // Set the secondaryThread to run me
        getApplication().enableSecondaryEventReceiver(this);

        if (yield) {
            // Yield to the secondary thread.  When I come back from the
            // constructor response will already be set.
            getApplication().yield();
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        if (this instanceof TInputBox) {
            super.onKeypress(keypress);
            return;
        }

        // Some convenience for message boxes: Alt won't be needed for the
        // buttons.
        switch (type) {

        case OK:
            if (keypress.equals(kbO)) {
                buttons.get(0).dispatch();
                return;
            }
            break;

        case OKCANCEL:
            if (keypress.equals(kbO)) {
                buttons.get(0).dispatch();
                return;
            } else if (keypress.equals(kbC)) {
                buttons.get(1).dispatch();
                return;
            }
            break;

        case YESNO:
            if (keypress.equals(kbY)) {
                buttons.get(0).dispatch();
                return;
            } else if (keypress.equals(kbN)) {
                buttons.get(1).dispatch();
                return;
            }
            break;

        case YESNOCANCEL:
            if (keypress.equals(kbY)) {
                buttons.get(0).dispatch();
                return;
            } else if (keypress.equals(kbN)) {
                buttons.get(1).dispatch();
                return;
            } else if (keypress.equals(kbC)) {
                buttons.get(2).dispatch();
                return;
            }
            break;

        default:
            throw new IllegalArgumentException("Invalid message box type: " + type);
        }

        super.onKeypress(keypress);
    }

}
