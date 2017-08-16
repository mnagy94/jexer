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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jexer.bits.Cell;
import jexer.bits.CellAttributes;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.tterminal.DisplayLine;
import jexer.tterminal.DisplayListener;
import jexer.tterminal.ECMA48;
import static jexer.TKeypress.*;

/**
 * TTerminalWindow exposes a ECMA-48 / ANSI X3.64 style terminal in a window.
 */
public class TTerminalWindow extends TScrollableWindow
                             implements DisplayListener {

    /**
     * The emulator.
     */
    private ECMA48 emulator;

    /**
     * The Process created by the shell spawning constructor.
     */
    private Process shell;

    /**
     * Claim the keystrokes the emulator will need.
     */
    private void addShortcutKeys() {
        addShortcutKeypress(kbCtrlA);
        addShortcutKeypress(kbCtrlB);
        addShortcutKeypress(kbCtrlC);
        addShortcutKeypress(kbCtrlD);
        addShortcutKeypress(kbCtrlE);
        addShortcutKeypress(kbCtrlF);
        addShortcutKeypress(kbCtrlG);
        addShortcutKeypress(kbCtrlH);
        addShortcutKeypress(kbCtrlU);
        addShortcutKeypress(kbCtrlJ);
        addShortcutKeypress(kbCtrlK);
        addShortcutKeypress(kbCtrlL);
        addShortcutKeypress(kbCtrlM);
        addShortcutKeypress(kbCtrlN);
        addShortcutKeypress(kbCtrlO);
        addShortcutKeypress(kbCtrlP);
        addShortcutKeypress(kbCtrlQ);
        addShortcutKeypress(kbCtrlR);
        addShortcutKeypress(kbCtrlS);
        addShortcutKeypress(kbCtrlT);
        addShortcutKeypress(kbCtrlU);
        addShortcutKeypress(kbCtrlV);
        addShortcutKeypress(kbCtrlW);
        addShortcutKeypress(kbCtrlX);
        addShortcutKeypress(kbCtrlY);
        addShortcutKeypress(kbCtrlZ);
        addShortcutKeypress(kbF1);
        addShortcutKeypress(kbF2);
        addShortcutKeypress(kbF3);
        addShortcutKeypress(kbF4);
        addShortcutKeypress(kbF5);
        addShortcutKeypress(kbF6);
        addShortcutKeypress(kbF7);
        addShortcutKeypress(kbF8);
        addShortcutKeypress(kbF9);
        addShortcutKeypress(kbF10);
        addShortcutKeypress(kbF11);
        addShortcutKeypress(kbF12);
        addShortcutKeypress(kbAltA);
        addShortcutKeypress(kbAltB);
        addShortcutKeypress(kbAltC);
        addShortcutKeypress(kbAltD);
        addShortcutKeypress(kbAltE);
        addShortcutKeypress(kbAltF);
        addShortcutKeypress(kbAltG);
        addShortcutKeypress(kbAltH);
        addShortcutKeypress(kbAltU);
        addShortcutKeypress(kbAltJ);
        addShortcutKeypress(kbAltK);
        addShortcutKeypress(kbAltL);
        addShortcutKeypress(kbAltM);
        addShortcutKeypress(kbAltN);
        addShortcutKeypress(kbAltO);
        addShortcutKeypress(kbAltP);
        addShortcutKeypress(kbAltQ);
        addShortcutKeypress(kbAltR);
        addShortcutKeypress(kbAltS);
        addShortcutKeypress(kbAltT);
        addShortcutKeypress(kbAltU);
        addShortcutKeypress(kbAltV);
        addShortcutKeypress(kbAltW);
        addShortcutKeypress(kbAltX);
        addShortcutKeypress(kbAltY);
        addShortcutKeypress(kbAltZ);
    }

    /**
     * Public constructor spawns a shell.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final int flags) {

        super(application, "Terminal", x, y, 80 + 2, 24 + 2, flags);

        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);
        setBottomValue(0);

        // Assume XTERM
        ECMA48.DeviceType deviceType = ECMA48.DeviceType.XTERM;

        try {
            String [] cmdShellWindows = {
                "cmd.exe"
            };

            // You cannot run a login shell in a bare Process interactively,
            // due to libc's behavior of buffering when stdin/stdout aren't a
            // tty.  Use 'script' instead to run a shell in a pty.  And
            // because BSD and GNU differ on the '-f' vs '-F' flags, we need
            // two different commands.  Lovely.
            String [] cmdShellGNU = {
                "script", "-fqe", "/dev/null"
            };
            String [] cmdShellBSD = {
                "script", "-q", "-F", "/dev/null"
            };
            // Spawn a shell and pass its I/O to the other constructor.

            ProcessBuilder pb;
            if (System.getProperty("os.name").startsWith("Windows")) {
                pb = new ProcessBuilder(cmdShellWindows);
            } else if (System.getProperty("os.name").startsWith("Mac")) {
                pb = new ProcessBuilder(cmdShellBSD);
            } else if (System.getProperty("os.name").startsWith("Linux")) {
                pb = new ProcessBuilder(cmdShellGNU);
            } else {
                // When all else fails, assume GNU.
                pb = new ProcessBuilder(cmdShellGNU);
            }
            Map<String, String> env = pb.environment();
            env.put("TERM", ECMA48.deviceTypeTerm(deviceType));
            env.put("LANG", ECMA48.deviceTypeLang(deviceType, "en"));
            env.put("COLUMNS", "80");
            env.put("LINES", "24");
            pb.redirectErrorStream(true);
            shell = pb.start();
            emulator = new ECMA48(deviceType, shell.getInputStream(),
                shell.getOutputStream());
            emulator.setListener(this);
        } catch (IOException e) {
            messageBox("Error", "Error launching shell: " + e.getMessage());
        }

        // Setup the scroll bars
        onResize(new TResizeEvent(TResizeEvent.Type.WIDGET, getWidth(),
                getHeight()));

        // Claim the keystrokes the emulator will need.
        addShortcutKeys();

        // Add shortcut text
        newStatusBar("Terminal session executing...");
    }

    /**
     * Terminate the child of the 'script' process used on POSIX.  This may
     * or may not work.
     */
    private void terminateShellChildProcess() {
        int pid = -1;
        if (shell.getClass().getName().equals("java.lang.UNIXProcess")) {
            /* get the PID on unix/linux systems */
            try {
                Field field = shell.getClass().getDeclaredField("pid");
                field.setAccessible(true);
                pid = field.getInt(shell);
            } catch (Throwable e) {
                // SQUASH, this didn't work.  Just bail out quietly.
                return;
            }
        }
        if (pid != -1) {
            // shell.destroy() works successfully at killing this side of
            // 'script'.  But we need to make sure the other side (child
            // process) is also killed.
            String [] cmdKillIt = {
                "pkill", "-P", Integer.toString(pid)
            };
            try {
                Runtime.getRuntime().exec(cmdKillIt);
            } catch (Throwable e) {
                // SQUASH, this didn't work.  Just bail out quietly.
                return;
            }
        }
    }

    /**
     * Draw the display buffer.
     */
    @Override
    public void draw() {
        // Synchronize against the emulator so we don't stomp on its reader
        // thread.
        synchronized (emulator) {

            // Update the scroll bars
            reflowData();

            // Draw the box using my superclass
            super.draw();

            List<DisplayLine> scrollback = emulator.getScrollbackBuffer();
            List<DisplayLine> display = emulator.getDisplayBuffer();

            // Put together the visible rows
            int visibleHeight = getHeight() - 2;
            int visibleBottom = scrollback.size() + display.size()
                + getVerticalValue();
            assert (visibleBottom >= 0);

            List<DisplayLine> preceedingBlankLines = new LinkedList<DisplayLine>();
            int visibleTop = visibleBottom - visibleHeight;
            if (visibleTop < 0) {
                for (int i = visibleTop; i < 0; i++) {
                    preceedingBlankLines.add(emulator.getBlankDisplayLine());
                }
                visibleTop = 0;
            }
            assert (visibleTop >= 0);

            List<DisplayLine> displayLines = new LinkedList<DisplayLine>();
            displayLines.addAll(scrollback);
            displayLines.addAll(display);

            List<DisplayLine> visibleLines = new LinkedList<DisplayLine>();
            visibleLines.addAll(preceedingBlankLines);
            visibleLines.addAll(displayLines.subList(visibleTop,
                    visibleBottom));

            visibleHeight -= visibleLines.size();
            assert (visibleHeight >= 0);

            // Now draw the emulator screen
            int row = 1;
            for (DisplayLine line: visibleLines) {
                int widthMax = emulator.getWidth();
                if (line.isDoubleWidth()) {
                    widthMax /= 2;
                }
                if (widthMax > getWidth() - 2) {
                    widthMax = getWidth() - 2;
                }
                for (int i = 0; i < widthMax; i++) {
                    Cell ch = line.charAt(i);
                    Cell newCell = new Cell();
                    newCell.setTo(ch);
                    boolean reverse = line.isReverseColor() ^ ch.isReverse();
                    newCell.setReverse(false);
                    if (reverse) {
                        newCell.setBackColor(ch.getForeColor());
                        newCell.setForeColor(ch.getBackColor());
                    }
                    if (line.isDoubleWidth()) {
                        getScreen().putCharXY((i * 2) + 1, row, newCell);
                        getScreen().putCharXY((i * 2) + 2, row, ' ', newCell);
                    } else {
                        getScreen().putCharXY(i + 1, row, newCell);
                    }
                }
                row++;
                if (row == getHeight() - 1) {
                    // Don't overwrite the box edge
                    break;
                }
            }
            CellAttributes background = new CellAttributes();
            // Fill in the blank lines on bottom
            for (int i = 0; i < visibleHeight; i++) {
                getScreen().hLineXY(1, i + row, getWidth() - 2, ' ',
                    background);
            }

        } // synchronized (emulator)

    }

    /**
     * Called by emulator when fresh data has come in.
     */
    public void displayChanged() {
        doRepaint();
    }

    /**
     * Handle window close.
     */
    @Override
    public void onClose() {
        emulator.close();
        if (shell != null) {
            terminateShellChildProcess();
            shell.destroy();
            shell = null;
        }
    }

    /**
     * Copy out variables from the emulator that TTerminal has to expose on
     * screen.
     */
    private void readEmulatorState() {
        // Synchronize against the emulator so we don't stomp on its reader
        // thread.
        synchronized (emulator) {

            setCursorX(emulator.getCursorX() + 1);
            setCursorY(emulator.getCursorY() + 1
                + (getHeight() - 2 - emulator.getHeight())
                - getVerticalValue());
            setCursorVisible(emulator.isCursorVisible());
            if (getCursorX() > getWidth() - 2) {
                setCursorVisible(false);
            }
            if ((getCursorY() > getHeight() - 2) || (getCursorY() < 0)) {
                setCursorVisible(false);
            }
            if (emulator.getScreenTitle().length() > 0) {
                // Only update the title if the shell is still alive
                if (shell != null) {
                    setTitle(emulator.getScreenTitle());
                }
            }

            // Check to see if the shell has died.
            if (!emulator.isReading() && (shell != null)) {
                try {
                    int rc = shell.exitValue();
                    // The emulator exited on its own, all is fine
                    setTitle(String.format("%s [Completed - %d]",
                            getTitle(), rc));
                    shell = null;
                    emulator.close();
                    clearShortcutKeypresses();
                    statusBar.setText("Terminal session completed, exit " +
                        "code " + rc + ".");
                } catch (IllegalThreadStateException e) {
                    // The emulator thread has exited, but the shell Process
                    // hasn't figured that out yet.  Do nothing, we will see
                    // this in a future tick.
                }
            } else if (emulator.isReading() && (shell != null)) {
                // The shell might be dead, let's check
                try {
                    int rc = shell.exitValue();
                    // If we got here, the shell died.
                    setTitle(String.format("%s [Completed - %d]",
                            getTitle(), rc));
                    shell = null;
                    emulator.close();
                    clearShortcutKeypresses();
                    statusBar.setText("Terminal session completed, exit " +
                        "code " + rc + ".");
                } catch (IllegalThreadStateException e) {
                    // The shell is still running, do nothing.
                }
            }

        } // synchronized (emulator)
    }

    /**
     * Handle window/screen resize events.
     *
     * @param resize resize event
     */
    @Override
    public void onResize(final TResizeEvent resize) {

        // Synchronize against the emulator so we don't stomp on its reader
        // thread.
        synchronized (emulator) {

            if (resize.getType() == TResizeEvent.Type.WIDGET) {
                // Resize the scroll bars
                reflowData();
                placeScrollbars();

                // Get out of scrollback
                setVerticalValue(0);
            }
            return;

        } // synchronized (emulator)
    }

    /**
     * Resize scrollbars for a new width/height.
     */
    @Override
    public void reflowData() {

        // Synchronize against the emulator so we don't stomp on its reader
        // thread.
        synchronized (emulator) {

            // Pull cursor information
            readEmulatorState();

            // Vertical scrollbar
            setTopValue(getHeight() - 2
                - (emulator.getScrollbackBuffer().size()
                    + emulator.getDisplayBuffer().size()));
            setVerticalBigChange(getHeight() - 2);

        } // synchronized (emulator)
    }

    /**
     * Check if a mouse press/release/motion event coordinate is over the
     * emulator.
     *
     * @param mouse a mouse-based event
     * @return whether or not the mouse is on the emulator
     */
    private final boolean mouseOnEmulator(final TMouseEvent mouse) {

        synchronized (emulator) {
            if (!emulator.isReading()) {
                return false;
            }
        }

        if ((mouse.getAbsoluteX() >= getAbsoluteX() + 1)
            && (mouse.getAbsoluteX() <  getAbsoluteX() + getWidth() - 1)
            && (mouse.getAbsoluteY() >= getAbsoluteY() + 1)
            && (mouse.getAbsoluteY() <  getAbsoluteY() + getHeight() - 1)
        ) {
            return true;
        }
        return false;
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    @Override
    public void onKeypress(final TKeypressEvent keypress) {

        // Scrollback up/down
        if (keypress.equals(kbShiftPgUp)
            || keypress.equals(kbCtrlPgUp)
            || keypress.equals(kbAltPgUp)
        ) {
            bigVerticalDecrement();
            return;
        }
        if (keypress.equals(kbShiftPgDn)
            || keypress.equals(kbCtrlPgDn)
            || keypress.equals(kbAltPgDn)
        ) {
            bigVerticalIncrement();
            return;
        }

        // Synchronize against the emulator so we don't stomp on its reader
        // thread.
        synchronized (emulator) {
            if (emulator.isReading()) {
                // Get out of scrollback
                setVerticalValue(0);
                emulator.keypress(keypress.getKey());

                // UGLY HACK TIME!  cmd.exe needs CRLF, not just CR, so if
                // this is kBEnter then also send kbCtrlJ.
                if (System.getProperty("os.name").startsWith("Windows")) {
                    if (keypress.equals(kbEnter)) {
                        emulator.keypress(kbCtrlJ);
                    }
                }

                readEmulatorState();
                return;
            }
        }

        // Process is closed, honor "normal" TUI keystrokes
        super.onKeypress(keypress);
    }

    /**
     * Handle mouse press events.
     *
     * @param mouse mouse button press event
     */
    @Override
    public void onMouseDown(final TMouseEvent mouse) {
        if (inWindowMove || inWindowResize) {
            // TWindow needs to deal with this.
            super.onMouseDown(mouse);
            return;
        }

        if (mouse.isMouseWheelUp()) {
            verticalDecrement();
            return;
        }
        if (mouse.isMouseWheelDown()) {
            verticalIncrement();
            return;
        }
        if (mouseOnEmulator(mouse)) {
            synchronized (emulator) {
                mouse.setX(mouse.getX() - 1);
                mouse.setY(mouse.getY() - 1);
                emulator.mouse(mouse);
                readEmulatorState();
                return;
            }
        }

        // Emulator didn't consume it, pass it on
        super.onMouseDown(mouse);
    }

    /**
     * Handle mouse release events.
     *
     * @param mouse mouse button release event
     */
    @Override
    public void onMouseUp(final TMouseEvent mouse) {
        if (inWindowMove || inWindowResize) {
            // TWindow needs to deal with this.
            super.onMouseUp(mouse);
            return;
        }

        if (mouseOnEmulator(mouse)) {
            synchronized (emulator) {
                mouse.setX(mouse.getX() - 1);
                mouse.setY(mouse.getY() - 1);
                emulator.mouse(mouse);
                readEmulatorState();
                return;
            }
        }

        // Emulator didn't consume it, pass it on
        super.onMouseUp(mouse);
    }

    /**
     * Handle mouse motion events.
     *
     * @param mouse mouse motion event
     */
    @Override
    public void onMouseMotion(final TMouseEvent mouse) {
        if (inWindowMove || inWindowResize) {
            // TWindow needs to deal with this.
            super.onMouseMotion(mouse);
            return;
        }

        if (mouseOnEmulator(mouse)) {
            synchronized (emulator) {
                mouse.setX(mouse.getX() - 1);
                mouse.setY(mouse.getY() - 1);
                emulator.mouse(mouse);
                readEmulatorState();
                return;
            }
        }

        // Emulator didn't consume it, pass it on
        super.onMouseMotion(mouse);
    }

}
