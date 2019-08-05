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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import jexer.backend.ECMA48Terminal;
import jexer.backend.MultiScreen;
import jexer.backend.SwingTerminal;
import jexer.bits.Cell;
import jexer.bits.CellAttributes;
import jexer.event.TKeypressEvent;
import jexer.event.TMenuEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.menu.TMenu;
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
     * Translated strings.
     */
    private static final ResourceBundle i18n = ResourceBundle.getBundle(TTerminalWindow.class.getName());

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The emulator.
     */
    private ECMA48 emulator;

    /**
     * The Process created by the shell spawning constructor.
     */
    private Process shell;

    /**
     * If true, we are using the ptypipe utility to support dynamic window
     * resizing.  ptypipe is available at
     * https://gitlab.com/klamonte/ptypipe .
     */
    private boolean ptypipe = false;

    /**
     * If true, close the window when the shell exits.
     */
    private boolean closeOnExit = false;

    /**
     * System-dependent Y adjustment for text in the character cell
     * (double-height).
     */
    private int doubleTextAdjustY = 0;

    /**
     * System-dependent X adjustment for text in the character cell
     * (double-height).
     */
    private int doubleTextAdjustX = 0;

    /**
     * Descent of a character cell in pixels (double-height).
     */
    private int doubleMaxDescent = 0;

    /**
     * Double-width font.
     */
    private Font doubleFont = null;

    /**
     * Last text width value.
     */
    private int lastTextWidth = -1;

    /**
     * Last text height value.
     */
    private int lastTextHeight = -1;

    /**
     * A cache of previously-rendered double-width glyphs.
     */
    private Map<Cell, BufferedImage> glyphCache;

    /**
     * A cache of previously-rendered double-width glyphs for blinking text,
     * when it is not visible.
     */
    private Map<Cell, BufferedImage> glyphCacheBlink;

    /**
     * The blink state, used only by ECMA48 backend and when double-width
     * chars must be drawn.
     */
    private boolean blinkState = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor spawns a custom command line.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param commandLine the command line to execute
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final String commandLine) {

        this(application, x, y, RESIZABLE, commandLine.split("\\s+"),
            System.getProperty("jexer.TTerminal.closeOnExit",
                "false").equals("true"));
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param commandLine the command line to execute
     * @param closeOnExit if true, close the window when the command exits
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final String commandLine, final boolean closeOnExit) {

        this(application, x, y, RESIZABLE, commandLine.split("\\s+"),
            closeOnExit);
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param command the command line to execute
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final int flags, final String [] command) {

        this(application, x, y, flags, command,
            System.getProperty("jexer.TTerminal.closeOnExit",
                "false").equals("true"));
    }

    /**
     * Public constructor spawns a custom command line.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param command the command line to execute
     * @param closeOnExit if true, close the window when the command exits
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final int flags, final String [] command,
        final boolean closeOnExit) {

        super(application, i18n.getString("windowTitle"), x, y,
            80 + 2, 24 + 2, flags);

        this.closeOnExit = closeOnExit;

        String [] fullCommand;

        // Spawn a shell and pass its I/O to the other constructor.
        if ((System.getProperty("jexer.TTerminal.ptypipe") != null)
            && (System.getProperty("jexer.TTerminal.ptypipe").
                equals("true"))
        ) {
            ptypipe = true;
            fullCommand = new String[command.length + 1];
            fullCommand[0] = "ptypipe";
            System.arraycopy(command, 0, fullCommand, 1, command.length);
        } else if (System.getProperty("os.name").startsWith("Windows")) {
            fullCommand = new String[3];
            fullCommand[0] = "cmd";
            fullCommand[1] = "/c";
            fullCommand[2] = stringArrayToString(command);
        } else if (System.getProperty("os.name").startsWith("Mac")) {
            fullCommand = new String[6];
            fullCommand[0] = "script";
            fullCommand[1] = "-q";
            fullCommand[2] = "-F";
            fullCommand[3] = "/dev/null";
            fullCommand[4] = "-c";
            fullCommand[5] = stringArrayToString(command);
        } else {
            // Default: behave like Linux
            fullCommand = new String[5];
            fullCommand[0] = "script";
            fullCommand[1] = "-fqe";
            fullCommand[2] = "/dev/null";
            fullCommand[3] = "-c";
            fullCommand[4] = stringArrayToString(command);
        }
        spawnShell(fullCommand);
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

        this(application, x, y, flags,
            System.getProperty("jexer.TTerminal.closeOnExit",
                "false").equals("true"));

    }

    /**
     * Public constructor spawns a shell.
     *
     * @param application TApplication that manages this window
     * @param x column relative to parent
     * @param y row relative to parent
     * @param flags mask of CENTERED, MODAL, or RESIZABLE
     * @param closeOnExit if true, close the window when the shell exits
     */
    public TTerminalWindow(final TApplication application, final int x,
        final int y, final int flags, final boolean closeOnExit) {

        super(application, i18n.getString("windowTitle"), x, y,
            80 + 2, 24 + 2, flags);

        this.closeOnExit = closeOnExit;

        String cmdShellWindows = "cmd.exe";

        // You cannot run a login shell in a bare Process interactively, due
        // to libc's behavior of buffering when stdin/stdout aren't a tty.
        // Use 'script' instead to run a shell in a pty.  And because BSD and
        // GNU differ on the '-f' vs '-F' flags, we need two different
        // commands.  Lovely.
        String cmdShellGNU = "script -fqe /dev/null";
        String cmdShellBSD = "script -q -F /dev/null";

        // ptypipe is another solution that permits dynamic window resizing.
        String cmdShellPtypipe = "ptypipe /bin/bash --login";

        // Spawn a shell and pass its I/O to the other constructor.
        if ((System.getProperty("jexer.TTerminal.ptypipe") != null)
            && (System.getProperty("jexer.TTerminal.ptypipe").
                equals("true"))
        ) {
            ptypipe = true;
            spawnShell(cmdShellPtypipe.split("\\s+"));
        } else if (System.getProperty("os.name").startsWith("Windows")) {
            spawnShell(cmdShellWindows.split("\\s+"));
        } else if (System.getProperty("os.name").startsWith("Mac")) {
            spawnShell(cmdShellBSD.split("\\s+"));
        } else if (System.getProperty("os.name").startsWith("Linux")) {
            spawnShell(cmdShellGNU.split("\\s+"));
        } else {
            // When all else fails, assume GNU.
            spawnShell(cmdShellGNU.split("\\s+"));
        }
    }

    // ------------------------------------------------------------------------
    // TScrollableWindow ------------------------------------------------------
    // ------------------------------------------------------------------------

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

            List<DisplayLine> preceedingBlankLines = new ArrayList<DisplayLine>();
            int visibleTop = visibleBottom - visibleHeight;
            if (visibleTop < 0) {
                for (int i = visibleTop; i < 0; i++) {
                    preceedingBlankLines.add(emulator.getBlankDisplayLine());
                }
                visibleTop = 0;
            }
            assert (visibleTop >= 0);

            List<DisplayLine> displayLines = new ArrayList<DisplayLine>();
            displayLines.addAll(scrollback);
            displayLines.addAll(display);

            List<DisplayLine> visibleLines = new ArrayList<DisplayLine>();
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
                        if (ch.getForeColorRGB() < 0) {
                            newCell.setBackColor(ch.getForeColor());
                            newCell.setBackColorRGB(-1);
                        } else {
                            newCell.setBackColorRGB(ch.getForeColorRGB());
                        }
                        if (ch.getBackColorRGB() < 0) {
                            newCell.setForeColor(ch.getBackColor());
                            newCell.setForeColorRGB(-1);
                        } else {
                            newCell.setForeColorRGB(ch.getBackColorRGB());
                        }
                    }
                    if (line.isDoubleWidth()) {
                        putDoubleWidthCharXY(line, (i * 2) + 1, row, newCell);
                    } else {
                        putCharXY(i + 1, row, newCell);
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
                hLineXY(1, i + row, getWidth() - 2, ' ', background);
            }

        } // synchronized (emulator)

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

                if (ptypipe) {
                    emulator.setWidth(getWidth() - 2);
                    emulator.setHeight(getHeight() - 2);

                    emulator.writeRemote("\033[8;" + (getHeight() - 2) + ";" +
                        (getWidth() - 2) + "t");
                }
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

        // If the emulator is tracking mouse buttons, it needs to see wheel
        // events.
        if (emulator.getMouseProtocol() == ECMA48.MouseProtocol.OFF) {
            if (mouse.isMouseWheelUp()) {
                verticalDecrement();
                return;
            }
            if (mouse.isMouseWheelDown()) {
                verticalIncrement();
                return;
            }
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

    // ------------------------------------------------------------------------
    // TTerminalWindow --------------------------------------------------------
    // ------------------------------------------------------------------------

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
     * Convert a string array to a whitespace-separated string.
     *
     * @param array the string array
     * @return a single string
     */
    private String stringArrayToString(final String [] array) {
        StringBuilder sb = new StringBuilder(array[0].length());
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    /**
     * Spawn the shell.
     *
     * @param command the command line to execute
     */
    private void spawnShell(final String [] command) {

        /*
        System.err.printf("spawnShell(): '%s'\n",
            stringArrayToString(command));
        */

        vScroller = new TVScroller(this, getWidth() - 2, 0, getHeight() - 2);
        setBottomValue(0);

        // Assume XTERM
        ECMA48.DeviceType deviceType = ECMA48.DeviceType.XTERM;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Map<String, String> env = pb.environment();
            env.put("TERM", ECMA48.deviceTypeTerm(deviceType));
            env.put("LANG", ECMA48.deviceTypeLang(deviceType, "en"));
            env.put("COLUMNS", "80");
            env.put("LINES", "24");
            pb.redirectErrorStream(true);
            shell = pb.start();
            emulator = new ECMA48(deviceType, shell.getInputStream(),
                shell.getOutputStream(), this);
        } catch (IOException e) {
            messageBox(i18n.getString("errorLaunchingShellTitle"),
                MessageFormat.format(i18n.getString("errorLaunchingShellText"),
                    e.getMessage()));
        }

        // Setup the scroll bars
        onResize(new TResizeEvent(TResizeEvent.Type.WIDGET, getWidth(),
                getHeight()));

        // Claim the keystrokes the emulator will need.
        addShortcutKeys();

        // Add shortcut text
        newStatusBar(i18n.getString("statusBarRunning"));

        // Pass the correct text cell width/height to the emulator
        emulator.setTextWidth(getScreen().getTextWidth());
        emulator.setTextHeight(getScreen().getTextHeight());
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
     * Called by emulator when fresh data has come in.
     */
    public void displayChanged() {
        getApplication().postEvent(new TMenuEvent(TMenu.MID_REPAINT));
    }

    /**
     * Function to call to obtain the display width.
     *
     * @return the number of columns in the display
     */
    public int getDisplayWidth() {
        if (ptypipe) {
            return getWidth() - 2;
        }
        return 80;
    }

    /**
     * Function to call to obtain the display height.
     *
     * @return the number of rows in the display
     */
    public int getDisplayHeight() {
        if (ptypipe) {
            return getHeight() - 2;
        }
        return 24;
    }

    /**
     * Hook for subclasses to be notified of the shell termination.
     */
    public void onShellExit() {
        if (closeOnExit) {
            close();
        }
        getApplication().postEvent(new TMenuEvent(TMenu.MID_REPAINT));
    }

    /**
     * Copy out variables from the emulator that TTerminal has to expose on
     * screen.
     */
    private void readEmulatorState() {
        // Synchronize against the emulator so we don't stomp on its reader
        // thread.
        synchronized (emulator) {
            setHiddenMouse(emulator.hasHiddenMousePointer());

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
                    setTitle(MessageFormat.format(i18n.
                            getString("windowTitleCompleted"), getTitle(), rc));
                    shell = null;
                    emulator.close();
                    clearShortcutKeypresses();
                    statusBar.setText(MessageFormat.format(i18n.
                            getString("statusBarCompleted"), rc));
                    onShellExit();
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
                    setTitle(MessageFormat.format(i18n.
                            getString("windowTitleCompleted"), getTitle(), rc));
                    shell = null;
                    emulator.close();
                    clearShortcutKeypresses();
                    statusBar.setText(MessageFormat.format(i18n.
                            getString("statusBarCompleted"), rc));
                    onShellExit();
                } catch (IllegalThreadStateException e) {
                    // The shell is still running, do nothing.
                }
            }

        } // synchronized (emulator)
    }

    /**
     * Check if a mouse press/release/motion event coordinate is over the
     * emulator.
     *
     * @param mouse a mouse-based event
     * @return whether or not the mouse is on the emulator
     */
    private boolean mouseOnEmulator(final TMouseEvent mouse) {

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
     * Draw glyphs for a double-width or double-height VT100 cell to two
     * screen cells.
     *
     * @param line the line this VT100 cell is in
     * @param x the X position to draw the left half to
     * @param y the Y position to draw to
     * @param cell the cell to draw
     */
    private void putDoubleWidthCharXY(final DisplayLine line, final int x,
        final int y, final Cell cell) {

        int textWidth = getScreen().getTextWidth();
        int textHeight = getScreen().getTextHeight();
        boolean cursorBlinkVisible = true;

        if (getScreen() instanceof SwingTerminal) {
            SwingTerminal terminal = (SwingTerminal) getScreen();
            cursorBlinkVisible = terminal.getCursorBlinkVisible();
        } else if (getScreen() instanceof ECMA48Terminal) {
            ECMA48Terminal terminal = (ECMA48Terminal) getScreen();

            if (!terminal.hasSixel()) {
                // The backend does not have sixel support, draw this as text
                // and bail out.
                putCharXY(x, y, cell);
                putCharXY(x + 1, y, ' ', cell);
                return;
            }
            cursorBlinkVisible = blinkState;
        } else {
            // We don't know how to dray glyphs to this screen, draw them as
            // text and bail out.
            putCharXY(x, y, cell);
            putCharXY(x + 1, y, ' ', cell);
            return;
        }

        if ((textWidth != lastTextWidth) || (textHeight != lastTextHeight)) {
            // Screen size has changed, reset all fonts.
            setupFonts(textHeight);
            lastTextWidth = textWidth;
            lastTextHeight = textHeight;
        }
        assert (doubleFont != null);

        BufferedImage image = null;
        if (cell.isBlink() && !cursorBlinkVisible) {
            image = glyphCacheBlink.get(cell);
        } else {
            image = glyphCache.get(cell);
        }
        if (image == null) {
            // Generate glyph and draw it to an image.
            image = new BufferedImage(textWidth * 2, textHeight * 2,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D gr2 = image.createGraphics();
            gr2.setFont(doubleFont);

            // Draw the background rectangle, then the foreground character.
            gr2.setColor(SwingTerminal.attrToBackgroundColor(cell));
            gr2.fillRect(0, 0, image.getWidth(), image.getHeight());
            if (!cell.isBlink()
                || (cell.isBlink() && cursorBlinkVisible)
            ) {
                gr2.setColor(SwingTerminal.attrToForegroundColor(cell));
                char [] chars = new char[1];
                chars[0] = cell.getChar();
                gr2.drawChars(chars, 0, 1, doubleTextAdjustX,
                    (textHeight * 2) - doubleMaxDescent + doubleTextAdjustY);

                if (cell.isUnderline() && (line.getDoubleHeight() != 1)) {
                    gr2.fillRect(0, textHeight - 2, textWidth, 2);
                }
            }
            gr2.dispose();

            // Now save this generated image, using a new key that will not
            // be mutated by invertCell().
            Cell key = new Cell();
            key.setTo(cell);
            if (cell.isBlink() && !cursorBlinkVisible) {
                glyphCacheBlink.put(key, image);
            } else {
                glyphCache.put(key, image);
            }
        }

        // Now that we have the double-wide glyph drawn, copy the right
        // pieces of it to the cells.
        Cell left = new Cell();
        Cell right = new Cell();
        left.setTo(cell);
        right.setTo(cell);
        right.setChar(' ');
        BufferedImage leftImage = null;
        BufferedImage rightImage = null;
        switch (line.getDoubleHeight()) {
        case 1:
            // Top half double height
            leftImage = image.getSubimage(0, 0, textWidth, textHeight);
            rightImage = image.getSubimage(textWidth, 0, textWidth, textHeight);
            break;
        case 2:
            // Bottom half double height
            leftImage = image.getSubimage(0, textHeight, textWidth, textHeight);
            rightImage = image.getSubimage(textWidth, textHeight,
                textWidth, textHeight);
            break;
        default:
            // Either single height double-width, or error fallback
            BufferedImage wideImage = new BufferedImage(textWidth * 2,
                textHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D grWide = wideImage.createGraphics();
            grWide.drawImage(image, 0, 0, wideImage.getWidth(),
                wideImage.getHeight(), null);
            grWide.dispose();
            leftImage = wideImage.getSubimage(0, 0, textWidth, textHeight);
            rightImage = wideImage.getSubimage(textWidth, 0, textWidth,
                textHeight);
            break;
        }
        left.setImage(leftImage);
        right.setImage(rightImage);
        // Since we have image data, ditch the character here.  Otherwise, a
        // drawBoxShadow() over the terminal window will show the characters
        // which looks wrong.
        left.setChar(' ');
        right.setChar(' ');
        putCharXY(x, y, left);
        putCharXY(x + 1, y, right);
    }

    /**
     * Set up the single and double-width fonts.
     *
     * @param fontSize the size of font to request for the single-width font.
     * The double-width font will be 2x this value.
     */
    private void setupFonts(final int fontSize) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream in = loader.getResourceAsStream(SwingTerminal.FONTFILE);
            Font terminusRoot = Font.createFont(Font.TRUETYPE_FONT, in);
            Font terminusDouble = terminusRoot.deriveFont(Font.PLAIN,
                fontSize * 2);
            doubleFont = terminusDouble;
        } catch (java.awt.FontFormatException e) {
            new TExceptionDialog(getApplication(), e);
            doubleFont = new Font(Font.MONOSPACED, Font.PLAIN, fontSize * 2);
        } catch (java.io.IOException e) {
            new TExceptionDialog(getApplication(), e);
            doubleFont = new Font(Font.MONOSPACED, Font.PLAIN, fontSize * 2);
        }

        // Get font descent.
        BufferedImage image = new BufferedImage(fontSize * 10, fontSize * 10,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = image.createGraphics();
        gr.setFont(doubleFont);
        FontMetrics fm = gr.getFontMetrics();
        doubleMaxDescent = fm.getMaxDescent();

        gr.dispose();

        // (Re)create the glyph caches.
        glyphCache = new HashMap<Cell, BufferedImage>();
        glyphCacheBlink = new HashMap<Cell, BufferedImage>();

        // Special case: the ECMA48 backend needs to have a timer to drive
        // its blink state.
        if (getScreen() instanceof jexer.backend.ECMA48Terminal) {
            // Blink every 500 millis.
            long millis = 500;
            getApplication().addTimer(millis, true,
                new TAction() {
                    public void DO() {
                        blinkState = !blinkState;
                        getApplication().doRepaint();
                    }
                }
            );
        }

    }

}
