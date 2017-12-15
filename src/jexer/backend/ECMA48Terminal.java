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
package jexer.backend;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import jexer.bits.Cell;
import jexer.bits.CellAttributes;
import jexer.bits.Color;
import jexer.event.TInputEvent;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import static jexer.TKeypress.*;

/**
 * This class reads keystrokes and mouse events and emits output to ANSI
 * X3.64 / ECMA-48 type terminals e.g. xterm, linux, vt100, ansi.sys, etc.
 */
public class ECMA48Terminal extends LogicalScreen
                            implements TerminalReader, Runnable {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * States in the input parser.
     */
    private enum ParseState {
        GROUND,
        ESCAPE,
        ESCAPE_INTERMEDIATE,
        CSI_ENTRY,
        CSI_PARAM,
        MOUSE,
        MOUSE_SGR,
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Emit debugging to stderr.
     */
    private boolean debugToStderr = false;

    /**
     * If true, emit T.416-style RGB colors for normal system colors.  This
     * is a) expensive in bandwidth, and b) potentially terrible looking for
     * non-xterms.
     */
    private static boolean doRgbColor = false;

    /**
     * The session information.
     */
    private SessionInfo sessionInfo;

    /**
     * The event queue, filled up by a thread reading on input.
     */
    private List<TInputEvent> eventQueue;

    /**
     * If true, we want the reader thread to exit gracefully.
     */
    private boolean stopReaderThread;

    /**
     * The reader thread.
     */
    private Thread readerThread;

    /**
     * Parameters being collected.  E.g. if the string is \033[1;3m, then
     * params[0] will be 1 and params[1] will be 3.
     */
    private List<String> params;

    /**
     * Current parsing state.
     */
    private ParseState state;

    /**
     * The time we entered ESCAPE.  If we get a bare escape without a code
     * following it, this is used to return that bare escape.
     */
    private long escapeTime;

    /**
     * The time we last checked the window size.  We try not to spawn stty
     * more than once per second.
     */
    private long windowSizeTime;

    /**
     * true if mouse1 was down.  Used to report mouse1 on the release event.
     */
    private boolean mouse1;

    /**
     * true if mouse2 was down.  Used to report mouse2 on the release event.
     */
    private boolean mouse2;

    /**
     * true if mouse3 was down.  Used to report mouse3 on the release event.
     */
    private boolean mouse3;

    /**
     * Cache the cursor visibility value so we only emit the sequence when we
     * need to.
     */
    private boolean cursorOn = true;

    /**
     * Cache the last window size to figure out if a TResizeEvent needs to be
     * generated.
     */
    private TResizeEvent windowResize = null;

    /**
     * If true, then we changed System.in and need to change it back.
     */
    private boolean setRawMode;

    /**
     * The terminal's input.  If an InputStream is not specified in the
     * constructor, then this InputStreamReader will be bound to System.in
     * with UTF-8 encoding.
     */
    private Reader input;

    /**
     * The terminal's raw InputStream.  If an InputStream is not specified in
     * the constructor, then this InputReader will be bound to System.in.
     * This is used by run() to see if bytes are available() before calling
     * (Reader)input.read().
     */
    private InputStream inputStream;

    /**
     * The terminal's output.  If an OutputStream is not specified in the
     * constructor, then this PrintWriter will be bound to System.out with
     * UTF-8 encoding.
     */
    private PrintWriter output;

    /**
     * The listening object that run() wakes up on new input.
     */
    private Object listener;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Constructor sets up state for getEvent().
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; shutdown() will (blindly!) put System.in in cooked
     * mode.  input is always converted to a Reader with UTF-8 encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    public ECMA48Terminal(final Object listener, final InputStream input,
        final OutputStream output, final int windowWidth,
        final int windowHeight) throws UnsupportedEncodingException {

        this(listener, input, output);

        // Send dtterm/xterm sequences, which will probably not work because
        // allowWindowOps is defaulted to false.
        String resizeString = String.format("\033[8;%d;%dt", windowHeight,
            windowWidth);
        this.output.write(resizeString);
        this.output.flush();
    }

    /**
     * Constructor sets up state for getEvent().
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input an InputStream connected to the remote user, or null for
     * System.in.  If System.in is used, then on non-Windows systems it will
     * be put in raw mode; shutdown() will (blindly!) put System.in in cooked
     * mode.  input is always converted to a Reader with UTF-8 encoding.
     * @param output an OutputStream connected to the remote user, or null
     * for System.out.  output is always converted to a Writer with UTF-8
     * encoding.
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    public ECMA48Terminal(final Object listener, final InputStream input,
        final OutputStream output) throws UnsupportedEncodingException {

        resetParser();
        mouse1           = false;
        mouse2           = false;
        mouse3           = false;
        stopReaderThread = false;
        this.listener    = listener;

        if (input == null) {
            // inputStream = System.in;
            inputStream = new FileInputStream(FileDescriptor.in);
            sttyRaw();
            setRawMode = true;
        } else {
            inputStream = input;
        }
        this.input = new InputStreamReader(inputStream, "UTF-8");

        if (input instanceof SessionInfo) {
            // This is a TelnetInputStream that exposes window size and
            // environment variables from the telnet layer.
            sessionInfo = (SessionInfo) input;
        }
        if (sessionInfo == null) {
            if (input == null) {
                // Reading right off the tty
                sessionInfo = new TTYSessionInfo();
            } else {
                sessionInfo = new TSessionInfo();
            }
        }

        if (output == null) {
            this.output = new PrintWriter(new OutputStreamWriter(System.out,
                    "UTF-8"));
        } else {
            this.output = new PrintWriter(new OutputStreamWriter(output,
                    "UTF-8"));
        }

        // Enable mouse reporting and metaSendsEscape
        this.output.printf("%s%s", mouse(true), xtermMetaSendsEscape(true));
        this.output.flush();

        // Query the screen size
        sessionInfo.queryWindowSize();
        setDimensions(sessionInfo.getWindowWidth(),
            sessionInfo.getWindowHeight());

        // Hang onto the window size
        windowResize = new TResizeEvent(TResizeEvent.Type.SCREEN,
            sessionInfo.getWindowWidth(), sessionInfo.getWindowHeight());

        // Permit RGB colors only if externally requested
        if (System.getProperty("jexer.ECMA48.rgbColor") != null) {
            if (System.getProperty("jexer.ECMA48.rgbColor").equals("true")) {
                doRgbColor = true;
            } else {
                doRgbColor = false;
            }
        }

        // Spin up the input reader
        eventQueue = new LinkedList<TInputEvent>();
        readerThread = new Thread(this);
        readerThread.start();

        // Clear the screen
        this.output.write(clearAll());
        this.output.flush();
    }

    /**
     * Constructor sets up state for getEvent().
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
     * @param setRawMode if true, set System.in into raw mode with stty.
     * This should in general not be used.  It is here solely for Demo3,
     * which uses System.in.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    public ECMA48Terminal(final Object listener, final InputStream input,
        final Reader reader, final PrintWriter writer,
        final boolean setRawMode) {

        if (input == null) {
            throw new IllegalArgumentException("InputStream must be specified");
        }
        if (reader == null) {
            throw new IllegalArgumentException("Reader must be specified");
        }
        if (writer == null) {
            throw new IllegalArgumentException("Writer must be specified");
        }
        resetParser();
        mouse1           = false;
        mouse2           = false;
        mouse3           = false;
        stopReaderThread = false;
        this.listener    = listener;

        inputStream = input;
        this.input = reader;

        if (setRawMode == true) {
            sttyRaw();
        }
        this.setRawMode = setRawMode;

        if (input instanceof SessionInfo) {
            // This is a TelnetInputStream that exposes window size and
            // environment variables from the telnet layer.
            sessionInfo = (SessionInfo) input;
        }
        if (sessionInfo == null) {
            if (setRawMode == true) {
                // Reading right off the tty
                sessionInfo = new TTYSessionInfo();
            } else {
                sessionInfo = new TSessionInfo();
            }
        }

        this.output = writer;

        // Enable mouse reporting and metaSendsEscape
        this.output.printf("%s%s", mouse(true), xtermMetaSendsEscape(true));
        this.output.flush();

        // Query the screen size
        sessionInfo.queryWindowSize();
        setDimensions(sessionInfo.getWindowWidth(),
            sessionInfo.getWindowHeight());

        // Hang onto the window size
        windowResize = new TResizeEvent(TResizeEvent.Type.SCREEN,
            sessionInfo.getWindowWidth(), sessionInfo.getWindowHeight());

        // Permit RGB colors only if externally requested
        if (System.getProperty("jexer.ECMA48.rgbColor") != null) {
            if (System.getProperty("jexer.ECMA48.rgbColor").equals("true")) {
                doRgbColor = true;
            } else {
                doRgbColor = false;
            }
        }

        // Spin up the input reader
        eventQueue = new LinkedList<TInputEvent>();
        readerThread = new Thread(this);
        readerThread.start();

        // Clear the screen
        this.output.write(clearAll());
        this.output.flush();
    }

    /**
     * Constructor sets up state for getEvent().
     *
     * @param listener the object this backend needs to wake up when new
     * input comes in
     * @param input the InputStream underlying 'reader'.  Its available()
     * method is used to determine if reader.read() will block or not.
     * @param reader a Reader connected to the remote user.
     * @param writer a PrintWriter connected to the remote user.
     * @throws IllegalArgumentException if input, reader, or writer are null.
     */
    public ECMA48Terminal(final Object listener, final InputStream input,
        final Reader reader, final PrintWriter writer) {

        this(listener, input, reader, writer, false);
    }

    // ------------------------------------------------------------------------
    // LogicalScreen ----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Set the window title.
     *
     * @param title the new title
     */
    @Override
    public void setTitle(final String title) {
        output.write(getSetTitleString(title));
        flush();
    }

    /**
     * Push the logical screen to the physical device.
     */
    @Override
    public void flushPhysical() {
        String result = flushString();
        if ((cursorVisible)
            && (cursorY >= 0)
            && (cursorX >= 0)
            && (cursorY <= height - 1)
            && (cursorX <= width - 1)
        ) {
            result += cursor(true);
            result += gotoXY(cursorX, cursorY);
        } else {
            result += cursor(false);
        }
        output.write(result);
        flush();
    }

    // ------------------------------------------------------------------------
    // TerminalReader ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Check if there are events in the queue.
     *
     * @return if true, getEvents() has something to return to the backend
     */
    public boolean hasEvents() {
        synchronized (eventQueue) {
            return (eventQueue.size() > 0);
        }
    }

    /**
     * Return any events in the IO queue.
     *
     * @param queue list to append new events to
     */
    public void getEvents(final List<TInputEvent> queue) {
        synchronized (eventQueue) {
            if (eventQueue.size() > 0) {
                synchronized (queue) {
                    queue.addAll(eventQueue);
                }
                eventQueue.clear();
            }
        }
    }

    /**
     * Restore terminal to normal state.
     */
    public void closeTerminal() {

        // System.err.println("=== shutdown() ==="); System.err.flush();

        // Tell the reader thread to stop looking at input
        stopReaderThread = true;
        try {
            readerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Disable mouse reporting and show cursor
        output.printf("%s%s%s", mouse(false), cursor(true), normal());
        output.flush();

        if (setRawMode) {
            sttyCooked();
            setRawMode = false;
            // We don't close System.in/out
        } else {
            // Shut down the streams, this should wake up the reader thread
            // and make it exit.
            try {
                if (input != null) {
                    input.close();
                    input = null;
                }
                if (output != null) {
                    output.close();
                    output = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Set listener to a different Object.
     *
     * @param listener the new listening object that run() wakes up on new
     * input
     */
    public void setListener(final Object listener) {
        this.listener = listener;
    }

    // ------------------------------------------------------------------------
    // Runnable ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Read function runs on a separate thread.
     */
    public void run() {
        boolean done = false;
        // available() will often return > 1, so we need to read in chunks to
        // stay caught up.
        char [] readBuffer = new char[128];
        List<TInputEvent> events = new LinkedList<TInputEvent>();

        while (!done && !stopReaderThread) {
            try {
                // We assume that if inputStream has bytes available, then
                // input won't block on read().
                int n = inputStream.available();

                /*
                System.err.printf("inputStream.available(): %d\n", n);
                System.err.flush();
                */

                if (n > 0) {
                    if (readBuffer.length < n) {
                        // The buffer wasn't big enough, make it huger
                        readBuffer = new char[readBuffer.length * 2];
                    }

                    // System.err.printf("BEFORE read()\n"); System.err.flush();

                    int rc = input.read(readBuffer, 0, readBuffer.length);

                    /*
                    System.err.printf("AFTER read() %d\n", rc);
                    System.err.flush();
                    */

                    if (rc == -1) {
                        // This is EOF
                        done = true;
                    } else {
                        for (int i = 0; i < rc; i++) {
                            int ch = readBuffer[i];
                            processChar(events, (char)ch);
                        }
                        getIdleEvents(events);
                        if (events.size() > 0) {
                            // Add to the queue for the backend thread to
                            // be able to obtain.
                            synchronized (eventQueue) {
                                eventQueue.addAll(events);
                            }
                            if (listener != null) {
                                synchronized (listener) {
                                    listener.notifyAll();
                                }
                            }
                            events.clear();
                        }
                    }
                } else {
                    getIdleEvents(events);
                    if (events.size() > 0) {
                        synchronized (eventQueue) {
                            eventQueue.addAll(events);
                        }
                        if (listener != null) {
                            synchronized (listener) {
                                listener.notifyAll();
                            }
                        }
                        events.clear();
                    }

                    // Wait 20 millis for more data
                    Thread.sleep(20);
                }
                // System.err.println("end while loop"); System.err.flush();
            } catch (InterruptedException e) {
                // SQUASH
            } catch (IOException e) {
                e.printStackTrace();
                done = true;
            }
        } // while ((done == false) && (stopReaderThread == false))
        // System.err.println("*** run() exiting..."); System.err.flush();
    }

    // ------------------------------------------------------------------------
    // ECMA48Terminal ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Getter for sessionInfo.
     *
     * @return the SessionInfo
     */
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    /**
     * Get the output writer.
     *
     * @return the Writer
     */
    public PrintWriter getOutput() {
        return output;
    }

    /**
     * Call 'stty' to set cooked mode.
     *
     * <p>Actually executes '/bin/sh -c stty sane cooked &lt; /dev/tty'
     */
    private void sttyCooked() {
        doStty(false);
    }

    /**
     * Call 'stty' to set raw mode.
     *
     * <p>Actually executes '/bin/sh -c stty -ignbrk -brkint -parmrk -istrip
     * -inlcr -igncr -icrnl -ixon -opost -echo -echonl -icanon -isig -iexten
     * -parenb cs8 min 1 &lt; /dev/tty'
     */
    private void sttyRaw() {
        doStty(true);
    }

    /**
     * Call 'stty' to set raw or cooked mode.
     *
     * @param mode if true, set raw mode, otherwise set cooked mode
     */
    private void doStty(final boolean mode) {
        String [] cmdRaw = {
            "/bin/sh", "-c", "stty -ignbrk -brkint -parmrk -istrip -inlcr -igncr -icrnl -ixon -opost -echo -echonl -icanon -isig -iexten -parenb cs8 min 1 < /dev/tty"
        };
        String [] cmdCooked = {
            "/bin/sh", "-c", "stty sane cooked < /dev/tty"
        };
        try {
            Process process;
            if (mode) {
                process = Runtime.getRuntime().exec(cmdRaw);
            } else {
                process = Runtime.getRuntime().exec(cmdCooked);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line = in.readLine();
            if ((line != null) && (line.length() > 0)) {
                System.err.println("WEIRD?! Normal output from stty: " + line);
            }
            while (true) {
                BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
                line = err.readLine();
                if ((line != null) && (line.length() > 0)) {
                    System.err.println("Error output from stty: " + line);
                }
                try {
                    process.waitFor();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int rc = process.exitValue();
            if (rc != 0) {
                System.err.println("stty returned error code: " + rc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Flush output.
     */
    public void flush() {
        output.flush();
    }

    /**
     * Perform a somewhat-optimal rendering of a line.
     *
     * @param y row coordinate.  0 is the top-most row.
     * @param sb StringBuilder to write escape sequences to
     * @param lastAttr cell attributes from the last call to flushLine
     */
    private void flushLine(final int y, final StringBuilder sb,
        CellAttributes lastAttr) {

        int lastX = -1;
        int textEnd = 0;
        for (int x = 0; x < width; x++) {
            Cell lCell = logical[x][y];
            if (!lCell.isBlank()) {
                textEnd = x;
            }
        }
        // Push textEnd to first column beyond the text area
        textEnd++;

        // DEBUG
        // reallyCleared = true;

        for (int x = 0; x < width; x++) {
            Cell lCell = logical[x][y];
            Cell pCell = physical[x][y];

            if (!lCell.equals(pCell) || reallyCleared) {

                if (debugToStderr) {
                    System.err.printf("\n--\n");
                    System.err.printf(" Y: %d X: %d\n", y, x);
                    System.err.printf("   lCell: %s\n", lCell);
                    System.err.printf("   pCell: %s\n", pCell);
                    System.err.printf("    ====    \n");
                }

                if (lastAttr == null) {
                    lastAttr = new CellAttributes();
                    sb.append(normal());
                }

                // Place the cell
                if ((lastX != (x - 1)) || (lastX == -1)) {
                    // Advancing at least one cell, or the first gotoXY
                    sb.append(gotoXY(x, y));
                }

                assert (lastAttr != null);

                if ((x == textEnd) && (textEnd < width - 1)) {
                    assert (lCell.isBlank());

                    for (int i = x; i < width; i++) {
                        assert (logical[i][y].isBlank());
                        // Physical is always updated
                        physical[i][y].reset();
                    }

                    // Clear remaining line
                    sb.append(clearRemainingLine());
                    lastAttr.reset();
                    return;
                }

                // Now emit only the modified attributes
                if ((lCell.getForeColor() != lastAttr.getForeColor())
                    && (lCell.getBackColor() != lastAttr.getBackColor())
                    && (!lCell.isRGB())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {
                    // Both colors changed, attributes the same
                    sb.append(color(lCell.isBold(),
                            lCell.getForeColor(), lCell.getBackColor()));

                    if (debugToStderr) {
                        System.err.printf("1 Change only fore/back colors\n");
                    }

                } else if (lCell.isRGB()
                    && (lCell.getForeColorRGB() != lastAttr.getForeColorRGB())
                    && (lCell.getBackColorRGB() != lastAttr.getBackColorRGB())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {
                    // Both colors changed, attributes the same
                    sb.append(colorRGB(lCell.getForeColorRGB(),
                            lCell.getBackColorRGB()));

                    if (debugToStderr) {
                        System.err.printf("1 Change only fore/back colors (RGB)\n");
                    }
                } else if ((lCell.getForeColor() != lastAttr.getForeColor())
                    && (lCell.getBackColor() != lastAttr.getBackColor())
                    && (!lCell.isRGB())
                    && (lCell.isBold() != lastAttr.isBold())
                    && (lCell.isReverse() != lastAttr.isReverse())
                    && (lCell.isUnderline() != lastAttr.isUnderline())
                    && (lCell.isBlink() != lastAttr.isBlink())
                ) {
                    // Everything is different
                    sb.append(color(lCell.getForeColor(),
                            lCell.getBackColor(),
                            lCell.isBold(), lCell.isReverse(),
                            lCell.isBlink(),
                            lCell.isUnderline()));

                    if (debugToStderr) {
                        System.err.printf("2 Set all attributes\n");
                    }
                } else if ((lCell.getForeColor() != lastAttr.getForeColor())
                    && (lCell.getBackColor() == lastAttr.getBackColor())
                    && (!lCell.isRGB())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {

                    // Attributes same, foreColor different
                    sb.append(color(lCell.isBold(),
                            lCell.getForeColor(), true));

                    if (debugToStderr) {
                        System.err.printf("3 Change foreColor\n");
                    }
                } else if (lCell.isRGB()
                    && (lCell.getForeColorRGB() != lastAttr.getForeColorRGB())
                    && (lCell.getBackColorRGB() == lastAttr.getBackColorRGB())
                    && (lCell.getForeColorRGB() >= 0)
                    && (lCell.getBackColorRGB() >= 0)
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {
                    // Attributes same, foreColor different
                    sb.append(colorRGB(lCell.getForeColorRGB(), true));

                    if (debugToStderr) {
                        System.err.printf("3 Change foreColor (RGB)\n");
                    }
                } else if ((lCell.getForeColor() == lastAttr.getForeColor())
                    && (lCell.getBackColor() != lastAttr.getBackColor())
                    && (!lCell.isRGB())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {
                    // Attributes same, backColor different
                    sb.append(color(lCell.isBold(),
                            lCell.getBackColor(), false));

                    if (debugToStderr) {
                        System.err.printf("4 Change backColor\n");
                    }
                } else if (lCell.isRGB()
                    && (lCell.getForeColorRGB() == lastAttr.getForeColorRGB())
                    && (lCell.getBackColorRGB() != lastAttr.getBackColorRGB())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {
                    // Attributes same, foreColor different
                    sb.append(colorRGB(lCell.getBackColorRGB(), false));

                    if (debugToStderr) {
                        System.err.printf("4 Change backColor (RGB)\n");
                    }
                } else if ((lCell.getForeColor() == lastAttr.getForeColor())
                    && (lCell.getBackColor() == lastAttr.getBackColor())
                    && (lCell.getForeColorRGB() == lastAttr.getForeColorRGB())
                    && (lCell.getBackColorRGB() == lastAttr.getBackColorRGB())
                    && (lCell.isBold() == lastAttr.isBold())
                    && (lCell.isReverse() == lastAttr.isReverse())
                    && (lCell.isUnderline() == lastAttr.isUnderline())
                    && (lCell.isBlink() == lastAttr.isBlink())
                ) {

                    // All attributes the same, just print the char
                    // NOP

                    if (debugToStderr) {
                        System.err.printf("5 Only emit character\n");
                    }
                } else {
                    // Just reset everything again
                    if (!lCell.isRGB()) {
                        sb.append(color(lCell.getForeColor(),
                                lCell.getBackColor(),
                                lCell.isBold(),
                                lCell.isReverse(),
                                lCell.isBlink(),
                                lCell.isUnderline()));

                        if (debugToStderr) {
                            System.err.printf("6 Change all attributes\n");
                        }
                    } else {
                        sb.append(colorRGB(lCell.getForeColorRGB(),
                                lCell.getBackColorRGB(),
                                lCell.isBold(),
                                lCell.isReverse(),
                                lCell.isBlink(),
                                lCell.isUnderline()));
                        if (debugToStderr) {
                            System.err.printf("6 Change all attributes (RGB)\n");
                        }
                    }

                }
                // Emit the character
                sb.append(lCell.getChar());

                // Save the last rendered cell
                lastX = x;
                lastAttr.setTo(lCell);

                // Physical is always updated
                physical[x][y].setTo(lCell);

            } // if (!lCell.equals(pCell) || (reallyCleared == true))

        } // for (int x = 0; x < width; x++)
    }

    /**
     * Render the screen to a string that can be emitted to something that
     * knows how to process ECMA-48/ANSI X3.64 escape sequences.
     *
     * @return escape sequences string that provides the updates to the
     * physical screen
     */
    private String flushString() {
        CellAttributes attr = null;

        StringBuilder sb = new StringBuilder();
        if (reallyCleared) {
            attr = new CellAttributes();
            sb.append(clearAll());
        }

        for (int y = 0; y < height; y++) {
            flushLine(y, sb, attr);
        }

        reallyCleared = false;

        String result = sb.toString();
        if (debugToStderr) {
            System.err.printf("flushString(): %s\n", result);
        }
        return result;
    }

    /**
     * Reset keyboard/mouse input parser.
     */
    private void resetParser() {
        state = ParseState.GROUND;
        params = new ArrayList<String>();
        params.clear();
        params.add("");
    }

    /**
     * Produce a control character or one of the special ones (ENTER, TAB,
     * etc.).
     *
     * @param ch Unicode code point
     * @param alt if true, set alt on the TKeypress
     * @return one TKeypress event, either a control character (e.g. isKey ==
     * false, ch == 'A', ctrl == true), or a special key (e.g. isKey == true,
     * fnKey == ESC)
     */
    private TKeypressEvent controlChar(final char ch, final boolean alt) {
        // System.err.printf("controlChar: %02x\n", ch);

        switch (ch) {
        case 0x0D:
            // Carriage return --> ENTER
            return new TKeypressEvent(kbEnter, alt, false, false);
        case 0x0A:
            // Linefeed --> ENTER
            return new TKeypressEvent(kbEnter, alt, false, false);
        case 0x1B:
            // ESC
            return new TKeypressEvent(kbEsc, alt, false, false);
        case '\t':
            // TAB
            return new TKeypressEvent(kbTab, alt, false, false);
        default:
            // Make all other control characters come back as the alphabetic
            // character with the ctrl field set.  So SOH would be 'A' +
            // ctrl.
            return new TKeypressEvent(false, 0, (char)(ch + 0x40),
                alt, true, false);
        }
    }

    /**
     * Produce special key from CSI Pn ; Pm ; ... ~
     *
     * @return one KEYPRESS event representing a special key
     */
    private TInputEvent csiFnKey() {
        int key = 0;
        if (params.size() > 0) {
            key = Integer.parseInt(params.get(0));
        }
        boolean alt = false;
        boolean ctrl = false;
        boolean shift = false;
        if (params.size() > 1) {
            shift = csiIsShift(params.get(1));
            alt = csiIsAlt(params.get(1));
            ctrl = csiIsCtrl(params.get(1));
        }

        switch (key) {
        case 1:
            return new TKeypressEvent(kbHome, alt, ctrl, shift);
        case 2:
            return new TKeypressEvent(kbIns, alt, ctrl, shift);
        case 3:
            return new TKeypressEvent(kbDel, alt, ctrl, shift);
        case 4:
            return new TKeypressEvent(kbEnd, alt, ctrl, shift);
        case 5:
            return new TKeypressEvent(kbPgUp, alt, ctrl, shift);
        case 6:
            return new TKeypressEvent(kbPgDn, alt, ctrl, shift);
        case 15:
            return new TKeypressEvent(kbF5, alt, ctrl, shift);
        case 17:
            return new TKeypressEvent(kbF6, alt, ctrl, shift);
        case 18:
            return new TKeypressEvent(kbF7, alt, ctrl, shift);
        case 19:
            return new TKeypressEvent(kbF8, alt, ctrl, shift);
        case 20:
            return new TKeypressEvent(kbF9, alt, ctrl, shift);
        case 21:
            return new TKeypressEvent(kbF10, alt, ctrl, shift);
        case 23:
            return new TKeypressEvent(kbF11, alt, ctrl, shift);
        case 24:
            return new TKeypressEvent(kbF12, alt, ctrl, shift);
        default:
            // Unknown
            return null;
        }
    }

    /**
     * Produce mouse events based on "Any event tracking" and UTF-8
     * coordinates.  See
     * http://invisible-island.net/xterm/ctlseqs/ctlseqs.html#Mouse%20Tracking
     *
     * @return a MOUSE_MOTION, MOUSE_UP, or MOUSE_DOWN event
     */
    private TInputEvent parseMouse() {
        int buttons = params.get(0).charAt(0) - 32;
        int x = params.get(0).charAt(1) - 32 - 1;
        int y = params.get(0).charAt(2) - 32 - 1;

        // Clamp X and Y to the physical screen coordinates.
        if (x >= windowResize.getWidth()) {
            x = windowResize.getWidth() - 1;
        }
        if (y >= windowResize.getHeight()) {
            y = windowResize.getHeight() - 1;
        }

        TMouseEvent.Type eventType = TMouseEvent.Type.MOUSE_DOWN;
        boolean eventMouse1 = false;
        boolean eventMouse2 = false;
        boolean eventMouse3 = false;
        boolean eventMouseWheelUp = false;
        boolean eventMouseWheelDown = false;

        // System.err.printf("buttons: %04x\r\n", buttons);

        switch (buttons) {
        case 0:
            eventMouse1 = true;
            mouse1 = true;
            break;
        case 1:
            eventMouse2 = true;
            mouse2 = true;
            break;
        case 2:
            eventMouse3 = true;
            mouse3 = true;
            break;
        case 3:
            // Release or Move
            if (!mouse1 && !mouse2 && !mouse3) {
                eventType = TMouseEvent.Type.MOUSE_MOTION;
            } else {
                eventType = TMouseEvent.Type.MOUSE_UP;
            }
            if (mouse1) {
                mouse1 = false;
                eventMouse1 = true;
            }
            if (mouse2) {
                mouse2 = false;
                eventMouse2 = true;
            }
            if (mouse3) {
                mouse3 = false;
                eventMouse3 = true;
            }
            break;

        case 32:
            // Dragging with mouse1 down
            eventMouse1 = true;
            mouse1 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 33:
            // Dragging with mouse2 down
            eventMouse2 = true;
            mouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 34:
            // Dragging with mouse3 down
            eventMouse3 = true;
            mouse3 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 96:
            // Dragging with mouse2 down after wheelUp
            eventMouse2 = true;
            mouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 97:
            // Dragging with mouse2 down after wheelDown
            eventMouse2 = true;
            mouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 64:
            eventMouseWheelUp = true;
            break;

        case 65:
            eventMouseWheelDown = true;
            break;

        default:
            // Unknown, just make it motion
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;
        }
        return new TMouseEvent(eventType, x, y, x, y,
            eventMouse1, eventMouse2, eventMouse3,
            eventMouseWheelUp, eventMouseWheelDown);
    }

    /**
     * Produce mouse events based on "Any event tracking" and SGR
     * coordinates.  See
     * http://invisible-island.net/xterm/ctlseqs/ctlseqs.html#Mouse%20Tracking
     *
     * @param release if true, this was a release ('m')
     * @return a MOUSE_MOTION, MOUSE_UP, or MOUSE_DOWN event
     */
    private TInputEvent parseMouseSGR(final boolean release) {
        // SGR extended coordinates - mode 1006
        if (params.size() < 3) {
            // Invalid position, bail out.
            return null;
        }
        int buttons = Integer.parseInt(params.get(0));
        int x = Integer.parseInt(params.get(1)) - 1;
        int y = Integer.parseInt(params.get(2)) - 1;

        // Clamp X and Y to the physical screen coordinates.
        if (x >= windowResize.getWidth()) {
            x = windowResize.getWidth() - 1;
        }
        if (y >= windowResize.getHeight()) {
            y = windowResize.getHeight() - 1;
        }

        TMouseEvent.Type eventType = TMouseEvent.Type.MOUSE_DOWN;
        boolean eventMouse1 = false;
        boolean eventMouse2 = false;
        boolean eventMouse3 = false;
        boolean eventMouseWheelUp = false;
        boolean eventMouseWheelDown = false;

        if (release) {
            eventType = TMouseEvent.Type.MOUSE_UP;
        }

        switch (buttons) {
        case 0:
            eventMouse1 = true;
            break;
        case 1:
            eventMouse2 = true;
            break;
        case 2:
            eventMouse3 = true;
            break;
        case 35:
            // Motion only, no buttons down
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 32:
            // Dragging with mouse1 down
            eventMouse1 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 33:
            // Dragging with mouse2 down
            eventMouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 34:
            // Dragging with mouse3 down
            eventMouse3 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 96:
            // Dragging with mouse2 down after wheelUp
            eventMouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 97:
            // Dragging with mouse2 down after wheelDown
            eventMouse2 = true;
            eventType = TMouseEvent.Type.MOUSE_MOTION;
            break;

        case 64:
            eventMouseWheelUp = true;
            break;

        case 65:
            eventMouseWheelDown = true;
            break;

        default:
            // Unknown, bail out
            return null;
        }
        return new TMouseEvent(eventType, x, y, x, y,
            eventMouse1, eventMouse2, eventMouse3,
            eventMouseWheelUp, eventMouseWheelDown);
    }

    /**
     * Return any events in the IO queue due to timeout.
     *
     * @param queue list to append new events to
     */
    private void getIdleEvents(final List<TInputEvent> queue) {
        long nowTime = System.currentTimeMillis();

        // Check for new window size
        long windowSizeDelay = nowTime - windowSizeTime;
        if (windowSizeDelay > 1000) {
            sessionInfo.queryWindowSize();
            int newWidth = sessionInfo.getWindowWidth();
            int newHeight = sessionInfo.getWindowHeight();

            if ((newWidth != windowResize.getWidth())
                || (newHeight != windowResize.getHeight())
            ) {

                if (debugToStderr) {
                    System.err.println("Screen size changed, old size " +
                        windowResize);
                    System.err.println("                     new size " +
                        newWidth + " x " + newHeight);
                }

                TResizeEvent event = new TResizeEvent(TResizeEvent.Type.SCREEN,
                    newWidth, newHeight);
                windowResize = new TResizeEvent(TResizeEvent.Type.SCREEN,
                    newWidth, newHeight);
                queue.add(event);
            }
            windowSizeTime = nowTime;
        }

        // ESCDELAY type timeout
        if (state == ParseState.ESCAPE) {
            long escDelay = nowTime - escapeTime;
            if (escDelay > 100) {
                // After 0.1 seconds, assume a true escape character
                queue.add(controlChar((char)0x1B, false));
                resetParser();
            }
        }
    }

    /**
     * Returns true if the CSI parameter for a keyboard command means that
     * shift was down.
     */
    private boolean csiIsShift(final String x) {
        if ((x.equals("2"))
            || (x.equals("4"))
            || (x.equals("6"))
            || (x.equals("8"))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the CSI parameter for a keyboard command means that
     * alt was down.
     */
    private boolean csiIsAlt(final String x) {
        if ((x.equals("3"))
            || (x.equals("4"))
            || (x.equals("7"))
            || (x.equals("8"))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the CSI parameter for a keyboard command means that
     * ctrl was down.
     */
    private boolean csiIsCtrl(final String x) {
        if ((x.equals("5"))
            || (x.equals("6"))
            || (x.equals("7"))
            || (x.equals("8"))
        ) {
            return true;
        }
        return false;
    }

    /**
     * Parses the next character of input to see if an InputEvent is
     * fully here.
     *
     * @param events list to append new events to
     * @param ch Unicode code point
     */
    private void processChar(final List<TInputEvent> events, final char ch) {

        // ESCDELAY type timeout
        long nowTime = System.currentTimeMillis();
        if (state == ParseState.ESCAPE) {
            long escDelay = nowTime - escapeTime;
            if (escDelay > 250) {
                // After 0.25 seconds, assume a true escape character
                events.add(controlChar((char)0x1B, false));
                resetParser();
            }
        }

        // TKeypress fields
        boolean ctrl = false;
        boolean alt = false;
        boolean shift = false;

        // System.err.printf("state: %s ch %c\r\n", state, ch);

        switch (state) {
        case GROUND:

            if (ch == 0x1B) {
                state = ParseState.ESCAPE;
                escapeTime = nowTime;
                return;
            }

            if (ch <= 0x1F) {
                // Control character
                events.add(controlChar(ch, false));
                resetParser();
                return;
            }

            if (ch >= 0x20) {
                // Normal character
                events.add(new TKeypressEvent(false, 0, ch,
                        false, false, false));
                resetParser();
                return;
            }

            break;

        case ESCAPE:
            if (ch <= 0x1F) {
                // ALT-Control character
                events.add(controlChar(ch, true));
                resetParser();
                return;
            }

            if (ch == 'O') {
                // This will be one of the function keys
                state = ParseState.ESCAPE_INTERMEDIATE;
                return;
            }

            // '[' goes to CSI_ENTRY
            if (ch == '[') {
                state = ParseState.CSI_ENTRY;
                return;
            }

            // Everything else is assumed to be Alt-keystroke
            if ((ch >= 'A') && (ch <= 'Z')) {
                shift = true;
            }
            alt = true;
            events.add(new TKeypressEvent(false, 0, ch, alt, ctrl, shift));
            resetParser();
            return;

        case ESCAPE_INTERMEDIATE:
            if ((ch >= 'P') && (ch <= 'S')) {
                // Function key
                switch (ch) {
                case 'P':
                    events.add(new TKeypressEvent(kbF1));
                    break;
                case 'Q':
                    events.add(new TKeypressEvent(kbF2));
                    break;
                case 'R':
                    events.add(new TKeypressEvent(kbF3));
                    break;
                case 'S':
                    events.add(new TKeypressEvent(kbF4));
                    break;
                default:
                    break;
                }
                resetParser();
                return;
            }

            // Unknown keystroke, ignore
            resetParser();
            return;

        case CSI_ENTRY:
            // Numbers - parameter values
            if ((ch >= '0') && (ch <= '9')) {
                params.set(params.size() - 1,
                    params.get(params.size() - 1) + ch);
                state = ParseState.CSI_PARAM;
                return;
            }
            // Parameter separator
            if (ch == ';') {
                params.add("");
                return;
            }

            if ((ch >= 0x30) && (ch <= 0x7E)) {
                switch (ch) {
                case 'A':
                    // Up
                    events.add(new TKeypressEvent(kbUp, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'B':
                    // Down
                    events.add(new TKeypressEvent(kbDown, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'C':
                    // Right
                    events.add(new TKeypressEvent(kbRight, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'D':
                    // Left
                    events.add(new TKeypressEvent(kbLeft, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'H':
                    // Home
                    events.add(new TKeypressEvent(kbHome));
                    resetParser();
                    return;
                case 'F':
                    // End
                    events.add(new TKeypressEvent(kbEnd));
                    resetParser();
                    return;
                case 'Z':
                    // CBT - Cursor backward X tab stops (default 1)
                    events.add(new TKeypressEvent(kbBackTab));
                    resetParser();
                    return;
                case 'M':
                    // Mouse position
                    state = ParseState.MOUSE;
                    return;
                case '<':
                    // Mouse position, SGR (1006) coordinates
                    state = ParseState.MOUSE_SGR;
                    return;
                default:
                    break;
                }
            }

            // Unknown keystroke, ignore
            resetParser();
            return;

        case MOUSE_SGR:
            // Numbers - parameter values
            if ((ch >= '0') && (ch <= '9')) {
                params.set(params.size() - 1,
                    params.get(params.size() - 1) + ch);
                return;
            }
            // Parameter separator
            if (ch == ';') {
                params.add("");
                return;
            }

            switch (ch) {
            case 'M':
                // Generate a mouse press event
                TInputEvent event = parseMouseSGR(false);
                if (event != null) {
                    events.add(event);
                }
                resetParser();
                return;
            case 'm':
                // Generate a mouse release event
                event = parseMouseSGR(true);
                if (event != null) {
                    events.add(event);
                }
                resetParser();
                return;
            default:
                break;
            }

            // Unknown keystroke, ignore
            resetParser();
            return;

        case CSI_PARAM:
            // Numbers - parameter values
            if ((ch >= '0') && (ch <= '9')) {
                params.set(params.size() - 1,
                    params.get(params.size() - 1) + ch);
                state = ParseState.CSI_PARAM;
                return;
            }
            // Parameter separator
            if (ch == ';') {
                params.add("");
                return;
            }

            if (ch == '~') {
                events.add(csiFnKey());
                resetParser();
                return;
            }

            if ((ch >= 0x30) && (ch <= 0x7E)) {
                switch (ch) {
                case 'A':
                    // Up
                    if (params.size() > 1) {
                        shift = csiIsShift(params.get(1));
                        alt = csiIsAlt(params.get(1));
                        ctrl = csiIsCtrl(params.get(1));
                    }
                    events.add(new TKeypressEvent(kbUp, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'B':
                    // Down
                    if (params.size() > 1) {
                        shift = csiIsShift(params.get(1));
                        alt = csiIsAlt(params.get(1));
                        ctrl = csiIsCtrl(params.get(1));
                    }
                    events.add(new TKeypressEvent(kbDown, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'C':
                    // Right
                    if (params.size() > 1) {
                        shift = csiIsShift(params.get(1));
                        alt = csiIsAlt(params.get(1));
                        ctrl = csiIsCtrl(params.get(1));
                    }
                    events.add(new TKeypressEvent(kbRight, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'D':
                    // Left
                    if (params.size() > 1) {
                        shift = csiIsShift(params.get(1));
                        alt = csiIsAlt(params.get(1));
                        ctrl = csiIsCtrl(params.get(1));
                    }
                    events.add(new TKeypressEvent(kbLeft, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'H':
                    // Home
                    if (params.size() > 1) {
                        shift = csiIsShift(params.get(1));
                        alt = csiIsAlt(params.get(1));
                        ctrl = csiIsCtrl(params.get(1));
                    }
                    events.add(new TKeypressEvent(kbHome, alt, ctrl, shift));
                    resetParser();
                    return;
                case 'F':
                    // End
                    if (params.size() > 1) {
                        shift = csiIsShift(params.get(1));
                        alt = csiIsAlt(params.get(1));
                        ctrl = csiIsCtrl(params.get(1));
                    }
                    events.add(new TKeypressEvent(kbEnd, alt, ctrl, shift));
                    resetParser();
                    return;
                default:
                    break;
                }
            }

            // Unknown keystroke, ignore
            resetParser();
            return;

        case MOUSE:
            params.set(0, params.get(params.size() - 1) + ch);
            if (params.get(0).length() == 3) {
                // We have enough to generate a mouse event
                events.add(parseMouse());
                resetParser();
            }
            return;

        default:
            break;
        }

        // This "should" be impossible to reach
        return;
    }

    /**
     * Tell (u)xterm that we want alt- keystrokes to send escape + character
     * rather than set the 8th bit.  Anyone who wants UTF8 should want this
     * enabled.
     *
     * @param on if true, enable metaSendsEscape
     * @return the string to emit to xterm
     */
    private String xtermMetaSendsEscape(final boolean on) {
        if (on) {
            return "\033[?1036h\033[?1034l";
        }
        return "\033[?1036l";
    }

    /**
     * Create an xterm OSC sequence to change the window title.
     *
     * @param title the new title
     * @return the string to emit to xterm
     */
    private String getSetTitleString(final String title) {
        return "\033]2;" + title + "\007";
    }

    /**
     * Create a SGR parameter sequence for a single color change.
     *
     * @param bold if true, set bold
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
     * @param foreground if true, this is a foreground color
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String color(final boolean bold, final Color color,
        final boolean foreground) {
        return color(color, foreground, true) +
                rgbColor(bold, color, foreground);
    }

    /**
     * Create a T.416 RGB parameter sequence for a single color change.
     *
     * @param colorRGB a 24-bit RGB value for foreground color
     * @param foreground if true, this is a foreground color
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String colorRGB(final int colorRGB, final boolean foreground) {

        int colorRed     = (colorRGB >> 16) & 0xFF;
        int colorGreen   = (colorRGB >>  8) & 0xFF;
        int colorBlue    =  colorRGB        & 0xFF;

        StringBuilder sb = new StringBuilder();
        if (foreground) {
            sb.append("\033[38;2;");
        } else {
            sb.append("\033[48;2;");
        }
        sb.append(String.format("%d;%d;%dm", colorRed, colorGreen, colorBlue));
        return sb.toString();
    }

    /**
     * Create a T.416 RGB parameter sequence for both foreground and
     * background color change.
     *
     * @param foreColorRGB a 24-bit RGB value for foreground color
     * @param backColorRGB a 24-bit RGB value for foreground color
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String colorRGB(final int foreColorRGB, final int backColorRGB) {
        int foreColorRed     = (foreColorRGB >> 16) & 0xFF;
        int foreColorGreen   = (foreColorRGB >>  8) & 0xFF;
        int foreColorBlue    =  foreColorRGB        & 0xFF;
        int backColorRed     = (backColorRGB >> 16) & 0xFF;
        int backColorGreen   = (backColorRGB >>  8) & 0xFF;
        int backColorBlue    =  backColorRGB        & 0xFF;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\033[38;2;%d;%d;%dm",
                foreColorRed, foreColorGreen, foreColorBlue));
        sb.append(String.format("\033[48;2;%d;%d;%dm",
                backColorRed, backColorGreen, backColorBlue));
        return sb.toString();
    }

    /**
     * Create a T.416 RGB parameter sequence for a single color change.
     *
     * @param bold if true, set bold
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
     * @param foreground if true, this is a foreground color
     * @return the string to emit to an xterm terminal with RGB support,
     * e.g. "\033[38;2;RR;GG;BBm"
     */
    private String rgbColor(final boolean bold, final Color color,
        final boolean foreground) {
        if (doRgbColor == false) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\033[");
        if (bold) {
            // Bold implies foreground only
            sb.append("38;2;");
            if (color.equals(Color.BLACK)) {
                sb.append("84;84;84");
            } else if (color.equals(Color.RED)) {
                sb.append("252;84;84");
            } else if (color.equals(Color.GREEN)) {
                sb.append("84;252;84");
            } else if (color.equals(Color.YELLOW)) {
                sb.append("252;252;84");
            } else if (color.equals(Color.BLUE)) {
                sb.append("84;84;252");
            } else if (color.equals(Color.MAGENTA)) {
                sb.append("252;84;252");
            } else if (color.equals(Color.CYAN)) {
                sb.append("84;252;252");
            } else if (color.equals(Color.WHITE)) {
                sb.append("252;252;252");
            }
        } else {
            if (foreground) {
                sb.append("38;2;");
            } else {
                sb.append("48;2;");
            }
            if (color.equals(Color.BLACK)) {
                sb.append("0;0;0");
            } else if (color.equals(Color.RED)) {
                sb.append("168;0;0");
            } else if (color.equals(Color.GREEN)) {
                sb.append("0;168;0");
            } else if (color.equals(Color.YELLOW)) {
                sb.append("168;84;0");
            } else if (color.equals(Color.BLUE)) {
                sb.append("0;0;168");
            } else if (color.equals(Color.MAGENTA)) {
                sb.append("168;0;168");
            } else if (color.equals(Color.CYAN)) {
                sb.append("0;168;168");
            } else if (color.equals(Color.WHITE)) {
                sb.append("168;168;168");
            }
        }
        sb.append("m");
        return sb.toString();
    }

    /**
     * Create a T.416 RGB parameter sequence for both foreground and
     * background color change.
     *
     * @param bold if true, set bold
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @return the string to emit to an xterm terminal with RGB support,
     * e.g. "\033[38;2;RR;GG;BB;48;2;RR;GG;BBm"
     */
    private String rgbColor(final boolean bold, final Color foreColor,
        final Color backColor) {
        if (doRgbColor == false) {
            return "";
        }

        return rgbColor(bold, foreColor, true) +
                rgbColor(false, backColor, false);
    }

    /**
     * Create a SGR parameter sequence for a single color change.
     *
     * @param color one of the Color.WHITE, Color.BLUE, etc. constants
     * @param foreground if true, this is a foreground color
     * @param header if true, make the full header, otherwise just emit the
     * color parameter e.g. "42;"
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[42m"
     */
    private String color(final Color color, final boolean foreground,
        final boolean header) {

        int ecmaColor = color.getValue();

        // Convert Color.* values to SGR numerics
        if (foreground) {
            ecmaColor += 30;
        } else {
            ecmaColor += 40;
        }

        if (header) {
            return String.format("\033[%dm", ecmaColor);
        } else {
            return String.format("%d;", ecmaColor);
        }
    }

    /**
     * Create a SGR parameter sequence for both foreground and background
     * color change.
     *
     * @param bold if true, set bold
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[31;42m"
     */
    private String color(final boolean bold, final Color foreColor,
        final Color backColor) {
        return color(foreColor, backColor, true) +
                rgbColor(bold, foreColor, backColor);
    }

    /**
     * Create a SGR parameter sequence for both foreground and
     * background color change.
     *
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param header if true, make the full header, otherwise just emit the
     * color parameter e.g. "31;42;"
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[31;42m"
     */
    private String color(final Color foreColor, final Color backColor,
        final boolean header) {

        int ecmaForeColor = foreColor.getValue();
        int ecmaBackColor = backColor.getValue();

        // Convert Color.* values to SGR numerics
        ecmaBackColor += 40;
        ecmaForeColor += 30;

        if (header) {
            return String.format("\033[%d;%dm", ecmaForeColor, ecmaBackColor);
        } else {
            return String.format("%d;%d;", ecmaForeColor, ecmaBackColor);
        }
    }

    /**
     * Create a SGR parameter sequence for foreground, background, and
     * several attributes.  This sequence first resets all attributes to
     * default, then sets attributes as per the parameters.
     *
     * @param foreColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param backColor one of the Color.WHITE, Color.BLUE, etc. constants
     * @param bold if true, set bold
     * @param reverse if true, set reverse
     * @param blink if true, set blink
     * @param underline if true, set underline
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0;1;31;42m"
     */
    private String color(final Color foreColor, final Color backColor,
        final boolean bold, final boolean reverse, final boolean blink,
        final boolean underline) {

        int ecmaForeColor = foreColor.getValue();
        int ecmaBackColor = backColor.getValue();

        // Convert Color.* values to SGR numerics
        ecmaBackColor += 40;
        ecmaForeColor += 30;

        StringBuilder sb = new StringBuilder();
        if        (  bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;1;7;5;");
        } else if (  bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;1;7;");
        } else if ( !bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;7;5;");
        } else if (  bold && !reverse &&  blink && !underline ) {
            sb.append("\033[0;1;5;");
        } else if (  bold && !reverse && !blink && !underline ) {
            sb.append("\033[0;1;");
        } else if ( !bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;7;");
        } else if ( !bold && !reverse &&  blink && !underline) {
            sb.append("\033[0;5;");
        } else if (  bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;7;5;4;");
        } else if (  bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;1;7;4;");
        } else if ( !bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;7;5;4;");
        } else if (  bold && !reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;5;4;");
        } else if (  bold && !reverse && !blink &&  underline ) {
            sb.append("\033[0;1;4;");
        } else if ( !bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;7;4;");
        } else if ( !bold && !reverse &&  blink &&  underline) {
            sb.append("\033[0;5;4;");
        } else if ( !bold && !reverse && !blink &&  underline) {
            sb.append("\033[0;4;");
        } else {
            assert (!bold && !reverse && !blink && !underline);
            sb.append("\033[0;");
        }
        sb.append(String.format("%d;%dm", ecmaForeColor, ecmaBackColor));
        sb.append(rgbColor(bold, foreColor, backColor));
        return sb.toString();
    }

    /**
     * Create a SGR parameter sequence for foreground, background, and
     * several attributes.  This sequence first resets all attributes to
     * default, then sets attributes as per the parameters.
     *
     * @param foreColorRGB a 24-bit RGB value for foreground color
     * @param backColorRGB a 24-bit RGB value for foreground color
     * @param bold if true, set bold
     * @param reverse if true, set reverse
     * @param blink if true, set blink
     * @param underline if true, set underline
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0;1;31;42m"
     */
    private String colorRGB(final int foreColorRGB, final int backColorRGB,
        final boolean bold, final boolean reverse, final boolean blink,
        final boolean underline) {

        int foreColorRed     = (foreColorRGB >> 16) & 0xFF;
        int foreColorGreen   = (foreColorRGB >>  8) & 0xFF;
        int foreColorBlue    =  foreColorRGB        & 0xFF;
        int backColorRed     = (backColorRGB >> 16) & 0xFF;
        int backColorGreen   = (backColorRGB >>  8) & 0xFF;
        int backColorBlue    =  backColorRGB        & 0xFF;

        StringBuilder sb = new StringBuilder();
        if        (  bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;1;7;5;");
        } else if (  bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;1;7;");
        } else if ( !bold &&  reverse &&  blink && !underline ) {
            sb.append("\033[0;7;5;");
        } else if (  bold && !reverse &&  blink && !underline ) {
            sb.append("\033[0;1;5;");
        } else if (  bold && !reverse && !blink && !underline ) {
            sb.append("\033[0;1;");
        } else if ( !bold &&  reverse && !blink && !underline ) {
            sb.append("\033[0;7;");
        } else if ( !bold && !reverse &&  blink && !underline) {
            sb.append("\033[0;5;");
        } else if (  bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;7;5;4;");
        } else if (  bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;1;7;4;");
        } else if ( !bold &&  reverse &&  blink &&  underline ) {
            sb.append("\033[0;7;5;4;");
        } else if (  bold && !reverse &&  blink &&  underline ) {
            sb.append("\033[0;1;5;4;");
        } else if (  bold && !reverse && !blink &&  underline ) {
            sb.append("\033[0;1;4;");
        } else if ( !bold &&  reverse && !blink &&  underline ) {
            sb.append("\033[0;7;4;");
        } else if ( !bold && !reverse &&  blink &&  underline) {
            sb.append("\033[0;5;4;");
        } else if ( !bold && !reverse && !blink &&  underline) {
            sb.append("\033[0;4;");
        } else {
            assert (!bold && !reverse && !blink && !underline);
            sb.append("\033[0;");
        }

        sb.append("m\033[38;2;");
        sb.append(String.format("%d;%d;%d", foreColorRed, foreColorGreen,
                foreColorBlue));
        sb.append("m\033[48;2;");
        sb.append(String.format("%d;%d;%d", backColorRed, backColorGreen,
                backColorBlue));
        sb.append("m");
        return sb.toString();
    }

    /**
     * Create a SGR parameter sequence to reset to defaults.
     *
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0m"
     */
    private String normal() {
        return normal(true) + rgbColor(false, Color.WHITE, Color.BLACK);
    }

    /**
     * Create a SGR parameter sequence to reset to defaults.
     *
     * @param header if true, make the full header, otherwise just emit the
     * bare parameter e.g. "0;"
     * @return the string to emit to an ANSI / ECMA-style terminal,
     * e.g. "\033[0m"
     */
    private String normal(final boolean header) {
        if (header) {
            return "\033[0;37;40m";
        }
        return "0;37;40";
    }

    /**
     * Create a SGR parameter sequence for enabling the visible cursor.
     *
     * @param on if true, turn on cursor
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String cursor(final boolean on) {
        if (on && !cursorOn) {
            cursorOn = true;
            return "\033[?25h";
        }
        if (!on && cursorOn) {
            cursorOn = false;
            return "\033[?25l";
        }
        return "";
    }

    /**
     * Clear the entire screen.  Because some terminals use back-color-erase,
     * set the color to white-on-black beforehand.
     *
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String clearAll() {
        return "\033[0;37;40m\033[2J";
    }

    /**
     * Clear the line from the cursor (inclusive) to the end of the screen.
     * Because some terminals use back-color-erase, set the color to
     * white-on-black beforehand.
     *
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String clearRemainingLine() {
        return "\033[0;37;40m\033[K";
    }

    /**
     * Move the cursor to (x, y).
     *
     * @param x column coordinate.  0 is the left-most column.
     * @param y row coordinate.  0 is the top-most row.
     * @return the string to emit to an ANSI / ECMA-style terminal
     */
    private String gotoXY(final int x, final int y) {
        return String.format("\033[%d;%dH", y + 1, x + 1);
    }

    /**
     * Tell (u)xterm that we want to receive mouse events based on "Any event
     * tracking", UTF-8 coordinates, and then SGR coordinates.  Ideally we
     * will end up with SGR coordinates with UTF-8 coordinates as a fallback.
     * See
     * http://invisible-island.net/xterm/ctlseqs/ctlseqs.html#Mouse%20Tracking
     *
     * Note that this also sets the alternate/primary screen buffer.
     *
     * @param on If true, enable mouse report and use the alternate screen
     * buffer.  If false disable mouse reporting and use the primary screen
     * buffer.
     * @return the string to emit to xterm
     */
    private String mouse(final boolean on) {
        if (on) {
            return "\033[?1002;1003;1005;1006h\033[?1049h";
        }
        return "\033[?1002;1003;1006;1005l\033[?1049l";
    }

}
