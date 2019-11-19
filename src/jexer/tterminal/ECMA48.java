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
package jexer.tterminal;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;

import jexer.TKeypress;
import jexer.backend.GlyphMaker;
import jexer.bits.Color;
import jexer.bits.Cell;
import jexer.bits.CellAttributes;
import jexer.bits.StringUtils;
import jexer.event.TInputEvent;
import jexer.event.TKeypressEvent;
import jexer.event.TMouseEvent;
import jexer.io.ReadTimeoutException;
import jexer.io.TimeoutInputStream;
import static jexer.TKeypress.*;

/**
 * This implements a complex ECMA-48/ISO 6429/ANSI X3.64 type console,
 * including a scrollback buffer.
 *
 * <p>
 * It currently implements VT100, VT102, VT220, and XTERM with the following
 * caveats:
 *
 * <p>
 * - The vttest scenario for VT220 8-bit controls (11.1.2.3) reports a
 *   failure with XTERM.  This is due to vttest failing to decode the UTF-8
 *   stream.
 *
 * <p>
 * - Smooth scrolling, printing, keyboard locking, keyboard leds, and tests
 *   from VT100 are not supported.
 *
 * <p>
 * - User-defined keys (DECUDK), downloadable fonts (DECDLD), and VT100/ANSI
 *   compatibility mode (DECSCL) from VT220 are not supported.  (Also,
 *   because DECSCL is not supported, it will fail the last part of the
 *   vttest "Test of VT52 mode" if DeviceType is set to VT220.)
 *
 * <p>
 * - Numeric/application keys from the number pad are not supported because
 *   they are not exposed from the TKeypress API.
 *
 * <p>
 * - VT52 HOLD SCREEN mode is not supported.
 *
 * <p>
 * - In VT52 graphics mode, the 3/, 5/, and 7/ characters (fraction
 *   numerators) are not rendered correctly.
 *
 * <p>
 * - All data meant for the 'printer' (CSI Pc ? i) is discarded.
 */
public class ECMA48 implements Runnable {

    // ------------------------------------------------------------------------
    // Constants --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The emulator can emulate several kinds of terminals.
     */
    public enum DeviceType {
        /**
         * DEC VT100 but also including the three VT102 functions.
         */
        VT100,

        /**
         * DEC VT102.
         */
        VT102,

        /**
         * DEC VT220.
         */
        VT220,

        /**
         * A subset of xterm.
         */
        XTERM
    }

    /**
     * Parser character scan states.
     */
    private enum ScanState {
        GROUND,
        ESCAPE,
        ESCAPE_INTERMEDIATE,
        CSI_ENTRY,
        CSI_PARAM,
        CSI_INTERMEDIATE,
        CSI_IGNORE,
        DCS_ENTRY,
        DCS_INTERMEDIATE,
        DCS_PARAM,
        DCS_PASSTHROUGH,
        DCS_IGNORE,
        DCS_SIXEL,
        SOSPMAPC_STRING,
        OSC_STRING,
        VT52_DIRECT_CURSOR_ADDRESS
    }

    /**
     * The selected number pad mode (DECKPAM, DECKPNM).  We record this, but
     * can't really use it in keypress() because we do not see number pad
     * events from TKeypress.
     */
    private enum KeypadMode {
        Application,
        Numeric
    }

    /**
     * Arrow keys can emit three different sequences (DECCKM or VT52
     * submode).
     */
    private enum ArrowKeyMode {
        VT52,
        ANSI,
        VT100
    }

    /**
     * Available character sets for GL, GR, G0, G1, G2, G3.
     */
    private enum CharacterSet {
        US,
        UK,
        DRAWING,
        ROM,
        ROM_SPECIAL,
        VT52_GRAPHICS,
        DEC_SUPPLEMENTAL,
        NRC_DUTCH,
        NRC_FINNISH,
        NRC_FRENCH,
        NRC_FRENCH_CA,
        NRC_GERMAN,
        NRC_ITALIAN,
        NRC_NORWEGIAN,
        NRC_SPANISH,
        NRC_SWEDISH,
        NRC_SWISS
    }

    /**
     * Single-shift states used by the C1 control characters SS2 (0x8E) and
     * SS3 (0x8F).
     */
    private enum Singleshift {
        NONE,
        SS2,
        SS3
    }

    /**
     * VT220+ lockshift states.
     */
    private enum LockshiftMode {
        NONE,
        G1_GR,
        G2_GR,
        G2_GL,
        G3_GR,
        G3_GL
    }

    /**
     * XTERM mouse reporting protocols.
     */
    public enum MouseProtocol {
        OFF,
        X10,
        NORMAL,
        BUTTONEVENT,
        ANYEVENT
    }

    /**
     * XTERM mouse reporting encodings.
     */
    private enum MouseEncoding {
        X10,
        UTF8,
        SGR
    }

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The enclosing listening object.
     */
    private DisplayListener displayListener;

    /**
     * When true, the reader thread is expected to exit.
     */
    private volatile boolean stopReaderThread = false;

    /**
     * The reader thread.
     */
    private Thread readerThread = null;

    /**
     * The type of emulator to be.
     */
    private final DeviceType type;

    /**
     * The scrollback buffer characters + attributes.
     */
    private volatile ArrayList<DisplayLine> scrollback;

    /**
     * The raw display buffer characters + attributes.
     */
    private volatile ArrayList<DisplayLine> display;

    /**
     * The maximum number of lines in the scrollback buffer.
     */
    private int scrollbackMax = 10000;

    /**
     * The terminal's input.  For type == XTERM, this is an InputStreamReader
     * with UTF-8 encoding.
     */
    private Reader input;

    /**
     * The terminal's raw InputStream.  This is used for type != XTERM.
     */
    private volatile TimeoutInputStream inputStream;

    /**
     * The terminal's output.  For type == XTERM, this wraps an
     * OutputStreamWriter with UTF-8 encoding.
     */
    private Writer output;

    /**
     * The terminal's raw OutputStream.  This is used for type != XTERM.
     */
    private OutputStream outputStream;

    /**
     * Current scanning state.
     */
    private ScanState scanState;

    /**
     * Which mouse protocol is active.
     */
    private MouseProtocol mouseProtocol = MouseProtocol.OFF;

    /**
     * Which mouse encoding is active.
     */
    private MouseEncoding mouseEncoding = MouseEncoding.X10;

    /**
     * A terminal may request that the mouse pointer be hidden using a
     * Privacy Message containing either "hideMousePointer" or
     * "showMousePointer".  This is currently only used within Jexer by
     * TTerminalWindow so that only the bottom-most instance of nested
     * Jexer's draws the mouse within its application window.
     */
    private boolean hideMousePointer = false;

    /**
     * Physical display width.  We start at 80x24, but the user can resize us
     * bigger/smaller.
     */
    private int width = 80;

    /**
     * Physical display height.  We start at 80x24, but the user can resize
     * us bigger/smaller.
     */
    private int height = 24;

    /**
     * Top margin of the scrolling region.
     */
    private int scrollRegionTop = 0;

    /**
     * Bottom margin of the scrolling region.
     */
    private int scrollRegionBottom = height - 1;

    /**
     * Right margin column number.  This can be selected by the remote side
     * to be 80/132 (rightMargin values 79/131), or it can be (width - 1).
     */
    private int rightMargin = 79;

    /**
     * Last character printed.
     */
    private int repCh;

    /**
     * VT100-style line wrapping: a character is placed in column 80 (or
     * 132), but the line does NOT wrap until another character is written to
     * column 1 of the next line, after which the cursor moves to column 2.
     */
    private boolean wrapLineFlag = false;

    /**
     * VT220 single shift flag.
     */
    private Singleshift singleshift = Singleshift.NONE;

    /**
     * true = insert characters, false = overwrite.
     */
    private boolean insertMode = false;

    /**
     * VT52 mode as selected by DECANM.  True means VT52, false means
     * ANSI. Default is ANSI.
     */
    private boolean vt52Mode = false;

    /**
     * Visible cursor (DECTCEM).
     */
    private boolean cursorVisible = true;

    /**
     * Screen title as set by the xterm OSC sequence.  Lots of applications
     * send a screenTitle regardless of whether it is an xterm client or not.
     */
    private String screenTitle = "";

    /**
     * Parameter characters being collected.
     */
    private List<Integer> csiParams;

    /**
     * Non-csi collect buffer.
     */
    private StringBuilder collectBuffer = new StringBuilder(128);

    /**
     * When true, use the G1 character set.
     */
    private boolean shiftOut = false;

    /**
     * Horizontal tab stop locations.
     */
    private List<Integer> tabStops;

    /**
     * S8C1T.  True means 8bit controls, false means 7bit controls.
     */
    private boolean s8c1t = false;

    /**
     * Printer mode.  True means send all output to printer, which discards
     * it.
     */
    private boolean printerControllerMode = false;

    /**
     * LMN line mode.  If true, linefeed() puts the cursor on the first
     * column of the next line.  If false, linefeed() puts the cursor one
     * line down on the current line.  The default is false.
     */
    private boolean newLineMode = false;

    /**
     * Whether arrow keys send ANSI, VT100, or VT52 sequences.
     */
    private ArrowKeyMode arrowKeyMode;

    /**
     * Whether number pad keys send VT100 or VT52, application or numeric
     * sequences.
     */
    @SuppressWarnings("unused")
    private KeypadMode keypadMode;

    /**
     * When true, the terminal is in 132-column mode (DECCOLM).
     */
    private boolean columns132 = false;

    /**
     * true = reverse video.  Set by DECSCNM.
     */
    private boolean reverseVideo = false;

    /**
     * false = echo characters locally.
     */
    private boolean fullDuplex = true;

    /**
     * The current terminal state.
     */
    private SaveableState currentState;

    /**
     * The last saved terminal state.
     */
    private SaveableState savedState;

    /**
     * The 88- or 256-color support RGB colors.
     */
    private List<Integer> colors88;

    /**
     * Sixel collection buffer.
     */
    private StringBuilder sixelParseBuffer = new StringBuilder(2048);

    /**
     * Sixel shared palette.
     */
    private HashMap<Integer, java.awt.Color> sixelPalette;

    /**
     * The width of a character cell in pixels.
     */
    private int textWidth = 16;

    /**
     * The height of a character cell in pixels.
     */
    private int textHeight = 20;

    /**
     * The last used height of a character cell in pixels, only used for
     * full-width chars.
     */
    private int lastTextHeight = -1;

    /**
     * The glyph drawer for full-width chars.
     */
    private GlyphMaker glyphMaker = null;

    /**
     * Input queue for keystrokes and mouse events to send to the remote
     * side.
     */
    private ArrayList<TInputEvent> userQueue = new ArrayList<TInputEvent>();

    /**
     * Number of bytes/characters passed to consume().
     */
    private long readCount = 0;

    /**
     * DECSC/DECRC save/restore a subset of the total state.  This class
     * encapsulates those specific flags/modes.
     */
    private class SaveableState {

        /**
         * When true, cursor positions are relative to the scrolling region.
         */
        public boolean originMode = false;

        /**
         * The current editing X position.
         */
        public int cursorX = 0;

        /**
         * The current editing Y position.
         */
        public int cursorY = 0;

        /**
         * Which character set is currently selected in G0.
         */
        public CharacterSet g0Charset = CharacterSet.US;

        /**
         * Which character set is currently selected in G1.
         */
        public CharacterSet g1Charset = CharacterSet.DRAWING;

        /**
         * Which character set is currently selected in G2.
         */
        public CharacterSet g2Charset = CharacterSet.US;

        /**
         * Which character set is currently selected in G3.
         */
        public CharacterSet g3Charset = CharacterSet.US;

        /**
         * Which character set is currently selected in GR.
         */
        public CharacterSet grCharset = CharacterSet.DRAWING;

        /**
         * The current drawing attributes.
         */
        public CellAttributes attr;

        /**
         * GL lockshift mode.
         */
        public LockshiftMode glLockshift = LockshiftMode.NONE;

        /**
         * GR lockshift mode.
         */
        public LockshiftMode grLockshift = LockshiftMode.NONE;

        /**
         * Line wrap.
         */
        public boolean lineWrap = true;

        /**
         * Reset to defaults.
         */
        public void reset() {
            originMode          = false;
            cursorX             = 0;
            cursorY             = 0;
            g0Charset           = CharacterSet.US;
            g1Charset           = CharacterSet.DRAWING;
            g2Charset           = CharacterSet.US;
            g3Charset           = CharacterSet.US;
            grCharset           = CharacterSet.DRAWING;
            attr                = new CellAttributes();
            glLockshift         = LockshiftMode.NONE;
            grLockshift         = LockshiftMode.NONE;
            lineWrap            = true;
        }

        /**
         * Copy attributes from another instance.
         *
         * @param that the other instance to match
         */
        public void setTo(final SaveableState that) {
            this.originMode     = that.originMode;
            this.cursorX        = that.cursorX;
            this.cursorY        = that.cursorY;
            this.g0Charset      = that.g0Charset;
            this.g1Charset      = that.g1Charset;
            this.g2Charset      = that.g2Charset;
            this.g3Charset      = that.g3Charset;
            this.grCharset      = that.grCharset;
            this.attr           = new CellAttributes();
            this.attr.setTo(that.attr);
            this.glLockshift    = that.glLockshift;
            this.grLockshift    = that.grLockshift;
            this.lineWrap       = that.lineWrap;
        }

        /**
         * Public constructor.
         */
        public SaveableState() {
            reset();
        }
    }

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param type one of the DeviceType constants to select VT100, VT102,
     * VT220, or XTERM
     * @param inputStream an InputStream connected to the remote side.  For
     * type == XTERM, inputStream is converted to a Reader with UTF-8
     * encoding.
     * @param outputStream an OutputStream connected to the remote user.  For
     * type == XTERM, outputStream is converted to a Writer with UTF-8
     * encoding.
     * @param displayListener a callback to the outer display, or null for
     * default VT100 behavior
     * @throws UnsupportedEncodingException if an exception is thrown when
     * creating the InputStreamReader
     */
    public ECMA48(final DeviceType type, final InputStream inputStream,
        final OutputStream outputStream, final DisplayListener displayListener)
        throws UnsupportedEncodingException {

        assert (inputStream != null);
        assert (outputStream != null);

        csiParams         = new ArrayList<Integer>();
        tabStops          = new ArrayList<Integer>();
        scrollback        = new ArrayList<DisplayLine>();
        display           = new ArrayList<DisplayLine>();

        this.type         = type;
        if (inputStream instanceof TimeoutInputStream) {
            this.inputStream  = (TimeoutInputStream)inputStream;
        } else {
            this.inputStream  = new TimeoutInputStream(inputStream, 2000);
        }
        if (type == DeviceType.XTERM) {
            this.input    = new InputStreamReader(new BufferedInputStream(
                this.inputStream, 1024 * 128), "UTF-8");
            this.output   = new OutputStreamWriter(new
                BufferedOutputStream(outputStream), "UTF-8");
            this.outputStream = null;
        } else {
            this.output       = null;
            this.outputStream = new BufferedOutputStream(outputStream);
        }
        this.displayListener  = displayListener;

        reset();
        for (int i = 0; i < height; i++) {
            display.add(new DisplayLine(currentState.attr));
        }
        assert (currentState.cursorY < height);
        assert (currentState.cursorX < width);

        // Spin up the input reader
        readerThread = new Thread(this);
        readerThread.start();
    }

    // ------------------------------------------------------------------------
    // Runnable ---------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Read function runs on a separate thread.
     */
    public final void run() {
        boolean utf8 = false;
        boolean done = false;

        if (type == DeviceType.XTERM) {
            utf8 = true;
        }

        // available() will often return > 1, so we need to read in chunks to
        // stay caught up.
        char [] readBufferUTF8 = null;
        byte [] readBuffer = null;
        if (utf8) {
            readBufferUTF8 = new char[2048];
        } else {
            readBuffer = new byte[2048];
        }

        while (!done && !stopReaderThread) {
            synchronized (userQueue) {
                while (userQueue.size() > 0) {
                    handleUserEvent(userQueue.remove(0));
                }
            }

            try {
                int n = inputStream.available();

                // System.err.printf("available() %d\n", n); System.err.flush();
                if (utf8) {
                    if (readBufferUTF8.length < n) {
                        // The buffer wasn't big enough, make it huger
                        int newSizeHalf = Math.max(readBufferUTF8.length,
                            n);

                        readBufferUTF8 = new char[newSizeHalf * 2];
                    }
                } else {
                    if (readBuffer.length < n) {
                        // The buffer wasn't big enough, make it huger
                        int newSizeHalf = Math.max(readBuffer.length, n);
                        readBuffer = new byte[newSizeHalf * 2];
                    }
                }
                if (n == 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // SQUASH
                    }
                    continue;
                }

                int rc = -1;
                try {
                    if (utf8) {
                        rc = input.read(readBufferUTF8, 0,
                            readBufferUTF8.length);
                    } else {
                        rc = inputStream.read(readBuffer, 0,
                            readBuffer.length);
                    }
                } catch (ReadTimeoutException e) {
                    rc = 0;
                }

                // System.err.printf("read() %d\n", rc); System.err.flush();
                if (rc == -1) {
                    // This is EOF
                    done = true;
                } else {
                    // Don't step on UI events
                    synchronized (this) {
                        if (utf8) {
                            for (int i = 0; i < rc;) {
                                int ch = Character.codePointAt(readBufferUTF8,
                                    i);
                                i += Character.charCount(ch);

                                // Special case for VT10x: 7-bit characters
                                // only.
                                if ((type == DeviceType.VT100)
                                    || (type == DeviceType.VT102)
                                ) {
                                    consume(ch & 0x7F);
                                } else {
                                    consume(ch);
                                }
                            }
                        } else {
                            for (int i = 0; i < rc; i++) {
                                // Special case for VT10x: 7-bit characters
                                // only.
                                if ((type == DeviceType.VT100)
                                    || (type == DeviceType.VT102)
                                ) {
                                    consume(readBuffer[i] & 0x7F);
                                } else {
                                    consume(readBuffer[i]);
                                }
                            }
                        }
                    }
                    // Permit my enclosing UI to know that I updated.
                    if (displayListener != null) {
                        displayListener.displayChanged();
                    }
                }
                // System.err.println("end while loop"); System.err.flush();
            } catch (IOException e) {
                done = true;

                // This is an unusual case.  We want to see the stack trace,
                // but it is related to the spawned process rather than the
                // actual UI.  We will generate the stack trace, and consume
                // it as though it was emitted by the shell.
                CharArrayWriter writer= new CharArrayWriter();
                // Send a ST and RIS to clear the emulator state.
                try {
                    writer.write("\033\\\033c");
                    writer.write("\n-----------------------------------\n");
                    e.printStackTrace(new PrintWriter(writer));
                    writer.write("\n-----------------------------------\n");
                } catch (IOException e2) {
                    // SQUASH
                }
                char [] stackTrace = writer.toCharArray();
                for (int i = 0; i < stackTrace.length; i++) {
                    if (stackTrace[i] == '\n') {
                        consume('\r');
                    }
                    consume(stackTrace[i]);
                }
            }

        } // while ((done == false) && (stopReaderThread == false))

        // Let the rest of the world know that I am done.
        stopReaderThread = true;

        try {
            inputStream.cancelRead();
            inputStream.close();
            inputStream = null;
        } catch (IOException e) {
            // SQUASH
        }
        try {
            input.close();
            input = null;
        } catch (IOException e) {
            // SQUASH
        }

        // Permit my enclosing UI to know that I updated.
        if (displayListener != null) {
            displayListener.displayChanged();
        }

        // System.err.println("*** run() exiting..."); System.err.flush();
    }

    // ------------------------------------------------------------------------
    // ECMA48 -----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Wait for a period of time to get output from the launched process.
     *
     * @param millis millis to wait for, or 0 to wait forever
     * @return true if the launched process has emitted something
     */
    public boolean waitForOutput(final int millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout must be >= 0");
        }
        int waitedMillis = millis;
        final int pollTimeout = 5;
        while (true) {
            if (readCount != 0) {
                return true;
            }
            if ((millis > 0) && (waitedMillis < 0)){
                return false;
            }
            try {
                Thread.sleep(pollTimeout);
            } catch (InterruptedException e) {
                // SQUASH
            }
            waitedMillis -= pollTimeout;
        }
    }

    /**
     * Process keyboard and mouse events from the user.
     *
     * @param event the input event to consume
     */
    private void handleUserEvent(final TInputEvent event) {
        if (event instanceof TKeypressEvent) {
            keypress(((TKeypressEvent) event).getKey());
        }
        if (event instanceof TMouseEvent) {
            mouse((TMouseEvent) event);
        }
    }

    /**
     * Add a keyboard and mouse event from the user to the queue.
     *
     * @param event the input event to consume
     */
    public void addUserEvent(final TInputEvent event) {
        synchronized (userQueue) {
            userQueue.add(event);
        }
    }

    /**
     * Return the proper primary Device Attributes string.
     *
     * @return string to send to remote side that is appropriate for the
     * this.type
     */
    private String deviceTypeResponse() {
        switch (type) {
        case VT100:
            // "I am a VT100 with advanced video option" (often VT102)
            return "\033[?1;2c";

        case VT102:
            // "I am a VT102"
            return "\033[?6c";

        case VT220:
        case XTERM:
            // "I am a VT220" - 7 bit version, with sixel and Jexer image
            // support.
            if (!s8c1t) {
                return "\033[?62;1;6;9;4;22;444c";
            }
            // "I am a VT220" - 8 bit version, with sixel and Jexer image
            // support.
            return "\u009b?62;1;6;9;4;22;444c";
        default:
            throw new IllegalArgumentException("Invalid device type: " + type);
        }
    }

    /**
     * Return the proper TERM environment variable for this device type.
     *
     * @param deviceType DeviceType.VT100, DeviceType, XTERM, etc.
     * @return "vt100", "xterm", etc.
     */
    public static String deviceTypeTerm(final DeviceType deviceType) {
        switch (deviceType) {
        case VT100:
            return "vt100";

        case VT102:
            return "vt102";

        case VT220:
            return "vt220";

        case XTERM:
            return "xterm";

        default:
            throw new IllegalArgumentException("Invalid device type: "
                + deviceType);
        }
    }

    /**
     * Return the proper LANG for this device type.  Only XTERM devices know
     * about UTF-8, the others are defined by their standard to be either
     * 7-bit or 8-bit characters only.
     *
     * @param deviceType DeviceType.VT100, DeviceType, XTERM, etc.
     * @param baseLang a base language without UTF-8 flag such as "C" or
     * "en_US"
     * @return "en_US", "en_US.UTF-8", etc.
     */
    public static String deviceTypeLang(final DeviceType deviceType,
        final String baseLang) {

        switch (deviceType) {

        case VT100:
        case VT102:
        case VT220:
            return baseLang;

        case XTERM:
            return baseLang + ".UTF-8";

        default:
            throw new IllegalArgumentException("Invalid device type: "
                + deviceType);
        }
    }

    /**
     * Write a string directly to the remote side.
     *
     * @param str string to send
     */
    public void writeRemote(final String str) {
        if (stopReaderThread) {
            // Reader hit EOF, bail out now.
            close();
            return;
        }

        // System.err.printf("writeRemote() '%s'\n", str);

        switch (type) {
        case VT100:
        case VT102:
        case VT220:
            if (outputStream == null) {
                return;
            }
            try {
                outputStream.flush();
                for (int i = 0; i < str.length(); i++) {
                    outputStream.write(str.charAt(i));
                }
                outputStream.flush();
            } catch (IOException e) {
                // Assume EOF
                close();
            }
            break;
        case XTERM:
            if (output == null) {
                return;
            }
            try {
                output.flush();
                output.write(str);
                output.flush();
            } catch (IOException e) {
                // Assume EOF
                close();
            }
            break;
        default:
            throw new IllegalArgumentException("Invalid device type: " + type);
        }
    }

    /**
     * Close the input and output streams and stop the reader thread.  Note
     * that it is safe to call this multiple times.
     */
    public final void close() {

        // Tell the reader thread to stop looking at input.  It will close
        // the input streams.
        if (stopReaderThread == false) {
            stopReaderThread = true;
        }

        // Now close the output stream.
        switch (type) {
        case VT100:
        case VT102:
        case VT220:
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // SQUASH
                }
                outputStream = null;
            }
            break;
        case XTERM:
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // SQUASH
                }
                outputStream = null;
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    // SQUASH
                }
                output = null;
            }
            break;
        default:
            throw new IllegalArgumentException("Invalid device type: " +
                type);
        }
    }

    /**
     * See if the reader thread is still running.
     *
     * @return if true, we are still connected to / reading from the remote
     * side
     */
    public final boolean isReading() {
        return (!stopReaderThread);
    }

    /**
     * Obtain a new blank display line for an external user
     * (e.g. TTerminalWindow).
     *
     * @return new blank line
     */
    public final DisplayLine getBlankDisplayLine() {
        return new DisplayLine(currentState.attr);
    }

    /**
     * Get the scrollback buffer.
     *
     * @return the scrollback buffer
     */
    public final List<DisplayLine> getScrollbackBuffer() {
        return scrollback;
    }

    /**
     * Get the display buffer.
     *
     * @return the display buffer
     */
    public final List<DisplayLine> getDisplayBuffer() {
        return display;
    }

    /**
     * Get the visible display + scrollback buffer, offset by a specified
     * number of rows from the bottom.
     *
     * @param visibleHeight the total height of the display to show
     * @param scrollBottom the number of rows from the bottom to scroll back
     * @return a copy of the display + scrollback buffers
     */
    public final List<DisplayLine> getVisibleDisplay(final int visibleHeight,
        final int scrollBottom) {

        assert (visibleHeight >= 0);
        assert (scrollBottom >= 0);

        int visibleBottom = scrollback.size() + display.size() - scrollBottom;

        List<DisplayLine> preceedingBlankLines = new ArrayList<DisplayLine>();
        int visibleTop = visibleBottom - visibleHeight;
        if (visibleTop < 0) {
            for (int i = visibleTop; i < 0; i++) {
                preceedingBlankLines.add(getBlankDisplayLine());
            }
            visibleTop = 0;
        }
        assert (visibleTop >= 0);

        List<DisplayLine> displayLines = new ArrayList<DisplayLine>();
        displayLines.addAll(scrollback);
        displayLines.addAll(display);

        List<DisplayLine> visibleLines = new ArrayList<DisplayLine>();
        visibleLines.addAll(preceedingBlankLines);
        visibleLines.addAll(displayLines.subList(visibleTop, visibleBottom));

        // Fill in the blank lines on bottom
        int bottomBlankLines = visibleHeight - visibleLines.size();
        assert (bottomBlankLines >= 0);
        for (int i = 0; i < bottomBlankLines; i++) {
            visibleLines.add(getBlankDisplayLine());
        }

        return copyBuffer(visibleLines);
    }

    /**
     * Copy a display buffer.
     *
     * @param buffer the buffer to copy
     * @return a deep copy of the buffer's data
     */
    private List<DisplayLine> copyBuffer(final List<DisplayLine> buffer) {
        ArrayList<DisplayLine> result = new ArrayList<DisplayLine>(buffer.size());
        for (DisplayLine line: buffer) {
            result.add(new DisplayLine(line));
        }
        return result;
    }

    /**
     * Get the display width.
     *
     * @return the width (usually 80 or 132)
     */
    public final int getWidth() {
        return width;
    }

    /**
     * Set the display width.
     *
     * @param width the new width
     */
    public final synchronized void setWidth(final int width) {
        this.width = width;
        rightMargin = width - 1;
        if (currentState.cursorX >= width) {
            currentState.cursorX = width - 1;
        }
        if (savedState.cursorX >= width) {
            savedState.cursorX = width - 1;
        }
    }

    /**
     * Get the display height.
     *
     * @return the height (usually 24)
     */
    public final int getHeight() {
        return height;
    }

    /**
     * Set the display height.
     *
     * @param height the new height
     */
    public final synchronized void setHeight(final int height) {
        int delta = height - this.height;
        this.height = height;
        scrollRegionBottom += delta;
        if ((scrollRegionBottom < 0) || (scrollRegionTop > height - 1)) {
            scrollRegionBottom = height - 1;
        }
        if (scrollRegionTop >= scrollRegionBottom) {
            scrollRegionTop = 0;
        }
        if (currentState.cursorY >= height) {
            currentState.cursorY = height - 1;
        }
        if (savedState.cursorY >= height) {
            savedState.cursorY = height - 1;
        }
        while (display.size() < height) {
            DisplayLine line = new DisplayLine(currentState.attr);
            line.setReverseColor(reverseVideo);
            display.add(line);
        }
        while (display.size() > height) {
            appendScrollbackLine(display.remove(0));
        }
    }

    /**
     * Get the maximum number of lines in the scrollback buffer.
     *
     * @return the maximum number of lines in the scrollback buffer
     */
    public int getScrollbackMax() {
        return scrollbackMax;
    }

    /**
     * Set the maximum number of lines for the scrollback buffer.
     *
     * @param scrollbackMax the maximum number of lines for the scrollback
     * buffer
     */
    public final void setScrollbackMax(final int scrollbackMax) {
        this.scrollbackMax = scrollbackMax;
    }

    /**
     * Get visible cursor flag.
     *
     * @return if true, the cursor is visible
     */
    public final boolean isCursorVisible() {
        return cursorVisible;
    }

    /**
     * Get the screen title as set by the xterm OSC sequence.  Lots of
     * applications send a screenTitle regardless of whether it is an xterm
     * client or not.
     *
     * @return screen title
     */
    public final String getScreenTitle() {
        return screenTitle;
    }

    /**
     * Get 132 columns value.
     *
     * @return if true, the terminal is in 132 column mode
     */
    public final boolean isColumns132() {
        return columns132;
    }

    /**
     * Clear the CSI parameters and flags.
     */
    private void toGround() {
        csiParams.clear();
        collectBuffer.setLength(0);
        scanState = ScanState.GROUND;
    }

    /**
     * Reset the tab stops list.
     */
    private void resetTabStops() {
        tabStops.clear();
        for (int i = 0; (i * 8) <= rightMargin; i++) {
            tabStops.add(Integer.valueOf(i * 8));
        }
    }

    /**
     * Reset the 88- or 256-colors.
     */
    private void resetColors() {
        colors88 = new ArrayList<Integer>(256);
        for (int i = 0; i < 256; i++) {
            colors88.add(0);
        }

        // Set default system colors.  These match DOS colors.
        colors88.set(0, 0x00000000);
        colors88.set(1, 0x00a80000);
        colors88.set(2, 0x0000a800);
        colors88.set(3, 0x00a85400);
        colors88.set(4, 0x000000a8);
        colors88.set(5, 0x00a800a8);
        colors88.set(6, 0x0000a8a8);
        colors88.set(7, 0x00a8a8a8);

        colors88.set(8, 0x00545454);
        colors88.set(9, 0x00fc5454);
        colors88.set(10, 0x0054fc54);
        colors88.set(11, 0x00fcfc54);
        colors88.set(12, 0x005454fc);
        colors88.set(13, 0x00fc54fc);
        colors88.set(14, 0x0054fcfc);
        colors88.set(15, 0x00fcfcfc);

        // These match xterm's default colors from 256colres.h.
        colors88.set(16, 0x000000);
        colors88.set(17, 0x00005f);
        colors88.set(18, 0x000087);
        colors88.set(19, 0x0000af);
        colors88.set(20, 0x0000d7);
        colors88.set(21, 0x0000ff);
        colors88.set(22, 0x005f00);
        colors88.set(23, 0x005f5f);
        colors88.set(24, 0x005f87);
        colors88.set(25, 0x005faf);
        colors88.set(26, 0x005fd7);
        colors88.set(27, 0x005fff);
        colors88.set(28, 0x008700);
        colors88.set(29, 0x00875f);
        colors88.set(30, 0x008787);
        colors88.set(31, 0x0087af);
        colors88.set(32, 0x0087d7);
        colors88.set(33, 0x0087ff);
        colors88.set(34, 0x00af00);
        colors88.set(35, 0x00af5f);
        colors88.set(36, 0x00af87);
        colors88.set(37, 0x00afaf);
        colors88.set(38, 0x00afd7);
        colors88.set(39, 0x00afff);
        colors88.set(40, 0x00d700);
        colors88.set(41, 0x00d75f);
        colors88.set(42, 0x00d787);
        colors88.set(43, 0x00d7af);
        colors88.set(44, 0x00d7d7);
        colors88.set(45, 0x00d7ff);
        colors88.set(46, 0x00ff00);
        colors88.set(47, 0x00ff5f);
        colors88.set(48, 0x00ff87);
        colors88.set(49, 0x00ffaf);
        colors88.set(50, 0x00ffd7);
        colors88.set(51, 0x00ffff);
        colors88.set(52, 0x5f0000);
        colors88.set(53, 0x5f005f);
        colors88.set(54, 0x5f0087);
        colors88.set(55, 0x5f00af);
        colors88.set(56, 0x5f00d7);
        colors88.set(57, 0x5f00ff);
        colors88.set(58, 0x5f5f00);
        colors88.set(59, 0x5f5f5f);
        colors88.set(60, 0x5f5f87);
        colors88.set(61, 0x5f5faf);
        colors88.set(62, 0x5f5fd7);
        colors88.set(63, 0x5f5fff);
        colors88.set(64, 0x5f8700);
        colors88.set(65, 0x5f875f);
        colors88.set(66, 0x5f8787);
        colors88.set(67, 0x5f87af);
        colors88.set(68, 0x5f87d7);
        colors88.set(69, 0x5f87ff);
        colors88.set(70, 0x5faf00);
        colors88.set(71, 0x5faf5f);
        colors88.set(72, 0x5faf87);
        colors88.set(73, 0x5fafaf);
        colors88.set(74, 0x5fafd7);
        colors88.set(75, 0x5fafff);
        colors88.set(76, 0x5fd700);
        colors88.set(77, 0x5fd75f);
        colors88.set(78, 0x5fd787);
        colors88.set(79, 0x5fd7af);
        colors88.set(80, 0x5fd7d7);
        colors88.set(81, 0x5fd7ff);
        colors88.set(82, 0x5fff00);
        colors88.set(83, 0x5fff5f);
        colors88.set(84, 0x5fff87);
        colors88.set(85, 0x5fffaf);
        colors88.set(86, 0x5fffd7);
        colors88.set(87, 0x5fffff);
        colors88.set(88, 0x870000);
        colors88.set(89, 0x87005f);
        colors88.set(90, 0x870087);
        colors88.set(91, 0x8700af);
        colors88.set(92, 0x8700d7);
        colors88.set(93, 0x8700ff);
        colors88.set(94, 0x875f00);
        colors88.set(95, 0x875f5f);
        colors88.set(96, 0x875f87);
        colors88.set(97, 0x875faf);
        colors88.set(98, 0x875fd7);
        colors88.set(99, 0x875fff);
        colors88.set(100, 0x878700);
        colors88.set(101, 0x87875f);
        colors88.set(102, 0x878787);
        colors88.set(103, 0x8787af);
        colors88.set(104, 0x8787d7);
        colors88.set(105, 0x8787ff);
        colors88.set(106, 0x87af00);
        colors88.set(107, 0x87af5f);
        colors88.set(108, 0x87af87);
        colors88.set(109, 0x87afaf);
        colors88.set(110, 0x87afd7);
        colors88.set(111, 0x87afff);
        colors88.set(112, 0x87d700);
        colors88.set(113, 0x87d75f);
        colors88.set(114, 0x87d787);
        colors88.set(115, 0x87d7af);
        colors88.set(116, 0x87d7d7);
        colors88.set(117, 0x87d7ff);
        colors88.set(118, 0x87ff00);
        colors88.set(119, 0x87ff5f);
        colors88.set(120, 0x87ff87);
        colors88.set(121, 0x87ffaf);
        colors88.set(122, 0x87ffd7);
        colors88.set(123, 0x87ffff);
        colors88.set(124, 0xaf0000);
        colors88.set(125, 0xaf005f);
        colors88.set(126, 0xaf0087);
        colors88.set(127, 0xaf00af);
        colors88.set(128, 0xaf00d7);
        colors88.set(129, 0xaf00ff);
        colors88.set(130, 0xaf5f00);
        colors88.set(131, 0xaf5f5f);
        colors88.set(132, 0xaf5f87);
        colors88.set(133, 0xaf5faf);
        colors88.set(134, 0xaf5fd7);
        colors88.set(135, 0xaf5fff);
        colors88.set(136, 0xaf8700);
        colors88.set(137, 0xaf875f);
        colors88.set(138, 0xaf8787);
        colors88.set(139, 0xaf87af);
        colors88.set(140, 0xaf87d7);
        colors88.set(141, 0xaf87ff);
        colors88.set(142, 0xafaf00);
        colors88.set(143, 0xafaf5f);
        colors88.set(144, 0xafaf87);
        colors88.set(145, 0xafafaf);
        colors88.set(146, 0xafafd7);
        colors88.set(147, 0xafafff);
        colors88.set(148, 0xafd700);
        colors88.set(149, 0xafd75f);
        colors88.set(150, 0xafd787);
        colors88.set(151, 0xafd7af);
        colors88.set(152, 0xafd7d7);
        colors88.set(153, 0xafd7ff);
        colors88.set(154, 0xafff00);
        colors88.set(155, 0xafff5f);
        colors88.set(156, 0xafff87);
        colors88.set(157, 0xafffaf);
        colors88.set(158, 0xafffd7);
        colors88.set(159, 0xafffff);
        colors88.set(160, 0xd70000);
        colors88.set(161, 0xd7005f);
        colors88.set(162, 0xd70087);
        colors88.set(163, 0xd700af);
        colors88.set(164, 0xd700d7);
        colors88.set(165, 0xd700ff);
        colors88.set(166, 0xd75f00);
        colors88.set(167, 0xd75f5f);
        colors88.set(168, 0xd75f87);
        colors88.set(169, 0xd75faf);
        colors88.set(170, 0xd75fd7);
        colors88.set(171, 0xd75fff);
        colors88.set(172, 0xd78700);
        colors88.set(173, 0xd7875f);
        colors88.set(174, 0xd78787);
        colors88.set(175, 0xd787af);
        colors88.set(176, 0xd787d7);
        colors88.set(177, 0xd787ff);
        colors88.set(178, 0xd7af00);
        colors88.set(179, 0xd7af5f);
        colors88.set(180, 0xd7af87);
        colors88.set(181, 0xd7afaf);
        colors88.set(182, 0xd7afd7);
        colors88.set(183, 0xd7afff);
        colors88.set(184, 0xd7d700);
        colors88.set(185, 0xd7d75f);
        colors88.set(186, 0xd7d787);
        colors88.set(187, 0xd7d7af);
        colors88.set(188, 0xd7d7d7);
        colors88.set(189, 0xd7d7ff);
        colors88.set(190, 0xd7ff00);
        colors88.set(191, 0xd7ff5f);
        colors88.set(192, 0xd7ff87);
        colors88.set(193, 0xd7ffaf);
        colors88.set(194, 0xd7ffd7);
        colors88.set(195, 0xd7ffff);
        colors88.set(196, 0xff0000);
        colors88.set(197, 0xff005f);
        colors88.set(198, 0xff0087);
        colors88.set(199, 0xff00af);
        colors88.set(200, 0xff00d7);
        colors88.set(201, 0xff00ff);
        colors88.set(202, 0xff5f00);
        colors88.set(203, 0xff5f5f);
        colors88.set(204, 0xff5f87);
        colors88.set(205, 0xff5faf);
        colors88.set(206, 0xff5fd7);
        colors88.set(207, 0xff5fff);
        colors88.set(208, 0xff8700);
        colors88.set(209, 0xff875f);
        colors88.set(210, 0xff8787);
        colors88.set(211, 0xff87af);
        colors88.set(212, 0xff87d7);
        colors88.set(213, 0xff87ff);
        colors88.set(214, 0xffaf00);
        colors88.set(215, 0xffaf5f);
        colors88.set(216, 0xffaf87);
        colors88.set(217, 0xffafaf);
        colors88.set(218, 0xffafd7);
        colors88.set(219, 0xffafff);
        colors88.set(220, 0xffd700);
        colors88.set(221, 0xffd75f);
        colors88.set(222, 0xffd787);
        colors88.set(223, 0xffd7af);
        colors88.set(224, 0xffd7d7);
        colors88.set(225, 0xffd7ff);
        colors88.set(226, 0xffff00);
        colors88.set(227, 0xffff5f);
        colors88.set(228, 0xffff87);
        colors88.set(229, 0xffffaf);
        colors88.set(230, 0xffffd7);
        colors88.set(231, 0xffffff);
        colors88.set(232, 0x080808);
        colors88.set(233, 0x121212);
        colors88.set(234, 0x1c1c1c);
        colors88.set(235, 0x262626);
        colors88.set(236, 0x303030);
        colors88.set(237, 0x3a3a3a);
        colors88.set(238, 0x444444);
        colors88.set(239, 0x4e4e4e);
        colors88.set(240, 0x585858);
        colors88.set(241, 0x626262);
        colors88.set(242, 0x6c6c6c);
        colors88.set(243, 0x767676);
        colors88.set(244, 0x808080);
        colors88.set(245, 0x8a8a8a);
        colors88.set(246, 0x949494);
        colors88.set(247, 0x9e9e9e);
        colors88.set(248, 0xa8a8a8);
        colors88.set(249, 0xb2b2b2);
        colors88.set(250, 0xbcbcbc);
        colors88.set(251, 0xc6c6c6);
        colors88.set(252, 0xd0d0d0);
        colors88.set(253, 0xdadada);
        colors88.set(254, 0xe4e4e4);
        colors88.set(255, 0xeeeeee);

    }

    /**
     * Get the RGB value of one of the indexed colors.
     *
     * @param index the color index
     * @return the RGB value
     */
    private int get88Color(final int index) {
        // System.err.print("get88Color: " + index);
        if ((index < 0) || (index > colors88.size())) {
            // System.err.println(" -- UNKNOWN");
            return 0;
        }
        // System.err.printf(" %08x\n", colors88.get(index));
        return colors88.get(index);
    }

    /**
     * Set one of the indexed colors to a color specification.
     *
     * @param index the color index
     * @param spec the specification, typically something like "rgb:aa/bb/cc"
     */
    private void set88Color(final int index, final String spec) {
        // System.err.println("set88Color: " + index + " '" + spec + "'");

        if ((index < 0) || (index > colors88.size())) {
            return;
        }
        if (spec.startsWith("rgb:")) {
            String [] rgbTokens = spec.substring(4).split("/");
            if (rgbTokens.length == 3) {
                try {
                    int rgb = (Integer.parseInt(rgbTokens[0], 16) << 16);
                    rgb |= Integer.parseInt(rgbTokens[1], 16) << 8;
                    rgb |= Integer.parseInt(rgbTokens[2], 16);
                    // System.err.printf("  set to %08x\n", rgb);
                    colors88.set(index, rgb);
                } catch (NumberFormatException e) {
                    // SQUASH
                }
            }
            return;
        }

        if (spec.toLowerCase().equals("black")) {
            colors88.set(index, 0x00000000);
        } else if (spec.toLowerCase().equals("red")) {
            colors88.set(index, 0x00a80000);
        } else if (spec.toLowerCase().equals("green")) {
            colors88.set(index, 0x0000a800);
        } else if (spec.toLowerCase().equals("yellow")) {
            colors88.set(index, 0x00a85400);
        } else if (spec.toLowerCase().equals("blue")) {
            colors88.set(index, 0x000000a8);
        } else if (spec.toLowerCase().equals("magenta")) {
            colors88.set(index, 0x00a800a8);
        } else if (spec.toLowerCase().equals("cyan")) {
            colors88.set(index, 0x0000a8a8);
        } else if (spec.toLowerCase().equals("white")) {
            colors88.set(index, 0x00a8a8a8);
        }

    }

    /**
     * Reset the emulation state.
     */
    private void reset() {

        currentState            = new SaveableState();
        savedState              = new SaveableState();
        scanState               = ScanState.GROUND;
        if (displayListener != null) {
            width = displayListener.getDisplayWidth();
            height = displayListener.getDisplayHeight();
        } else {
            width               = 80;
            height              = 24;
        }
        scrollRegionTop         = 0;
        scrollRegionBottom      = height - 1;
        rightMargin             = width - 1;
        newLineMode             = false;
        arrowKeyMode            = ArrowKeyMode.ANSI;
        keypadMode              = KeypadMode.Numeric;
        wrapLineFlag            = false;

        // Flags
        shiftOut                = false;
        vt52Mode                = false;
        insertMode              = false;
        columns132              = false;
        newLineMode             = false;
        reverseVideo            = false;
        fullDuplex              = true;
        cursorVisible           = true;

        // VT220
        singleshift             = Singleshift.NONE;
        s8c1t                   = false;
        printerControllerMode   = false;

        // XTERM
        mouseProtocol           = MouseProtocol.OFF;
        mouseEncoding           = MouseEncoding.X10;

        // Tab stops
        resetTabStops();

        // Reset extra colors
        resetColors();

        // Clear CSI stuff
        toGround();
    }

    /**
     * Append a to the scrollback buffer, clearing image data for lines more
     * than three screenfuls in.
     */
    private void appendScrollbackLine(DisplayLine line) {
        scrollback.add(line);
        if (scrollback.size() > height * 3) {
            scrollback.get(scrollback.size() - (height * 3)).clearImages();
        }
    }

    /**
     * Append a new line to the bottom of the display, adding lines off the
     * top to the scrollback buffer.
     */
    private void newDisplayLine() {
        // Scroll the top line off into the scrollback buffer
        appendScrollbackLine(display.get(0));
        while (scrollback.size() > scrollbackMax) {
            scrollback.remove(0);
            scrollback.trimToSize();
        }
        display.remove(0);
        display.trimToSize();
        DisplayLine line = new DisplayLine(currentState.attr);
        line.setReverseColor(reverseVideo);
        display.add(line);
    }

    /**
     * Wraps the current line.
     */
    private void wrapCurrentLine() {
        if (currentState.cursorY == height - 1) {
            newDisplayLine();
        }
        if (currentState.cursorY < height - 1) {
            currentState.cursorY++;
        }
        currentState.cursorX = 0;
    }

    /**
     * Handle a carriage return.
     */
    private void carriageReturn() {
        currentState.cursorX = 0;
        wrapLineFlag = false;
    }

    /**
     * Reverse the color of the visible display.
     */
    private void invertDisplayColors() {
        for (DisplayLine line: display) {
            line.setReverseColor(!line.isReverseColor());
        }
    }

    /**
     * Handle a linefeed.
     */
    private void linefeed() {
        if (currentState.cursorY < scrollRegionBottom) {
            // Increment screen y
            currentState.cursorY++;
        } else {

            // Screen y does not increment

            /*
             * Two cases: either we're inside a scrolling region or not.  If
             * the scrolling region bottom is the bottom of the screen, then
             * push the top line into the buffer.  Else scroll the scrolling
             * region up.
             */
            if ((scrollRegionBottom == height - 1) && (scrollRegionTop == 0)) {

                // We're at the bottom of the scroll region, AND the scroll
                // region is the entire screen.

                // New line
                newDisplayLine();

            } else {
                // We're at the bottom of the scroll region, AND the scroll
                // region is NOT the entire screen.
                scrollingRegionScrollUp(scrollRegionTop, scrollRegionBottom, 1);
            }
        }

        if (newLineMode) {
            currentState.cursorX = 0;
        }
        wrapLineFlag = false;
    }

    /**
     * Prints one character to the display buffer.
     *
     * @param ch character to display
     */
    private void printCharacter(final int ch) {
        int rightMargin = this.rightMargin;

        if (StringUtils.width(ch) == 2) {
            // This is a full-width character.  Save two spaces, and then
            // draw the character as two image halves.
            int x0 = currentState.cursorX;
            int y0 = currentState.cursorY;
            printCharacter(' ');
            printCharacter(' ');
            if ((currentState.cursorX == x0 + 2)
                && (currentState.cursorY == y0)
            ) {
                // We can draw both halves of the character.
                drawHalves(x0, y0, x0 + 1, y0, ch);
            } else if ((currentState.cursorX == x0 + 1)
                && (currentState.cursorY == y0)
            ) {
                // VT100 line wrap behavior: we should be at the right
                // margin.  We can draw both halves of the character.
                drawHalves(x0, y0, x0 + 1, y0, ch);
            } else {
                // The character splits across the line.  Draw the entire
                // character on the new line, giving one more space for it.
                x0 = currentState.cursorX - 1;
                y0 = currentState.cursorY;
                printCharacter(' ');
                drawHalves(x0, y0, x0 + 1, y0, ch);
            }
            return;
        }

        // Check if we have double-width, and if so chop at 40/66 instead of
        // 80/132
        if (display.get(currentState.cursorY).isDoubleWidth()) {
            rightMargin = ((rightMargin + 1) / 2) - 1;
        }

        // Check the unusually-complicated line wrapping conditions...
        if (currentState.cursorX == rightMargin) {

            if (currentState.lineWrap == true) {
                /*
                 * This case happens when: the cursor was already on the
                 * right margin (either through printing or by an explicit
                 * placement command), and a character was printed.
                 *
                 * The line wraps only when a new character arrives AND the
                 * cursor is already on the right margin AND has placed a
                 * character in its cell.  Easier to see than to explain.
                 */
                if (wrapLineFlag == false) {
                    /*
                     * This block marks the case that we are in the margin
                     * and the first character has been received and printed.
                     */
                    wrapLineFlag = true;
                } else {
                    /*
                     * This block marks the case that we are in the margin
                     * and the second character has been received and
                     * printed.
                     */
                    wrapLineFlag = false;
                    wrapCurrentLine();
                }
            }
        } else if (currentState.cursorX <= rightMargin) {
            /*
             * This is the normal case: a character came in and was printed
             * to the left of the right margin column.
             */

            // Turn off VT100 special-case flag
            wrapLineFlag = false;
        }

        // "Print" the character
        Cell newCell = new Cell(ch);
        CellAttributes newCellAttributes = (CellAttributes) newCell;
        newCellAttributes.setTo(currentState.attr);
        DisplayLine line = display.get(currentState.cursorY);

        if (StringUtils.width(ch) == 1) {
            // Insert mode special case
            if (insertMode == true) {
                line.insert(currentState.cursorX, newCell);
            } else {
                // Replace an existing character
                line.replace(currentState.cursorX, newCell);
            }

            // Increment horizontal
            if (wrapLineFlag == false) {
                currentState.cursorX++;
                if (currentState.cursorX > rightMargin) {
                    currentState.cursorX--;
                }
            }
        }
    }

    /**
     * Translate the mouse event to a VT100, VT220, or XTERM sequence and
     * send to the remote side.
     *
     * @param mouse mouse event received from the local user
     */
    private void mouse(final TMouseEvent mouse) {

        /*
        System.err.printf("mouse(): protocol %s encoding %s mouse %s\n",
            mouseProtocol, mouseEncoding, mouse);
         */

        if (mouseEncoding == MouseEncoding.X10) {
            // We will support X10 but only for (160,94) and smaller.
            if ((mouse.getX() >= 160) || (mouse.getY() >= 94)) {
                return;
            }
        }

        switch (mouseProtocol) {

        case OFF:
            // Do nothing
            return;

        case X10:
            // Only report button presses
            if (mouse.getType() != TMouseEvent.Type.MOUSE_DOWN) {
                return;
            }
            break;

        case NORMAL:
            // Only report button presses and releases
            if ((mouse.getType() != TMouseEvent.Type.MOUSE_DOWN)
                && (mouse.getType() != TMouseEvent.Type.MOUSE_UP)
            ) {
                return;
            }
            break;

        case BUTTONEVENT:
            /*
             * Only report button presses, button releases, and motions that
             * have a button down (i.e. drag-and-drop).
             */
            if (mouse.getType() == TMouseEvent.Type.MOUSE_MOTION) {
                if (!mouse.isMouse1()
                    && !mouse.isMouse2()
                    && !mouse.isMouse3()
                    && !mouse.isMouseWheelUp()
                    && !mouse.isMouseWheelDown()
                ) {
                    return;
                }
            }
            break;

        case ANYEVENT:
            // Report everything
            break;
        }

        // Now encode the event
        StringBuilder sb = new StringBuilder(6);
        if (mouseEncoding == MouseEncoding.SGR) {
            sb.append((char) 0x1B);
            sb.append("[<");
            int buttons = 0;

            if (mouse.isMouse1()) {
                if (mouse.getType() == TMouseEvent.Type.MOUSE_MOTION) {
                    buttons = 32;
                } else {
                    buttons = 0;
                }
            } else if (mouse.isMouse2()) {
                if (mouse.getType() == TMouseEvent.Type.MOUSE_MOTION) {
                    buttons = 33;
                } else {
                    buttons = 1;
                }
            } else if (mouse.isMouse3()) {
                if (mouse.getType() == TMouseEvent.Type.MOUSE_MOTION) {
                    buttons = 34;
                } else {
                    buttons = 2;
                }
            } else if (mouse.isMouseWheelUp()) {
                buttons = 64;
            } else if (mouse.isMouseWheelDown()) {
                buttons = 65;
            } else {
                // This is motion with no buttons down.
                buttons = 35;
            }
            if (mouse.isAlt()) {
                buttons |= 0x08;
            }
            if (mouse.isCtrl()) {
                buttons |= 0x10;
            }
            if (mouse.isShift()) {
                buttons |= 0x04;
            }

            sb.append(String.format("%d;%d;%d", buttons, mouse.getX() + 1,
                    mouse.getY() + 1));

            if (mouse.getType() == TMouseEvent.Type.MOUSE_UP) {
                sb.append("m");
            } else {
                sb.append("M");
            }

        } else {
            // X10 and UTF8 encodings
            sb.append((char) 0x1B);
            sb.append('[');
            sb.append('M');
            int buttons = 0;
            if (mouse.getType() == TMouseEvent.Type.MOUSE_UP) {
                buttons = 0x03 + 32;
            } else if (mouse.isMouse1()) {
                if (mouse.getType() == TMouseEvent.Type.MOUSE_MOTION) {
                    buttons = 0x00 + 32 + 32;
                } else {
                    buttons = 0x00 + 32;
                }
            } else if (mouse.isMouse2()) {
                if (mouse.getType() == TMouseEvent.Type.MOUSE_MOTION) {
                    buttons = 0x01 + 32 + 32;
                } else {
                    buttons = 0x01 + 32;
                }
            } else if (mouse.isMouse3()) {
                if (mouse.getType() == TMouseEvent.Type.MOUSE_MOTION) {
                    buttons = 0x02 + 32 + 32;
                } else {
                    buttons = 0x02 + 32;
                }
            } else if (mouse.isMouseWheelUp()) {
                buttons = 0x04 + 64;
            } else if (mouse.isMouseWheelDown()) {
                buttons = 0x05 + 64;
            } else {
                // This is motion with no buttons down.
                buttons = 0x03 + 32;
            }
            if (mouse.isAlt()) {
                buttons |= 0x08;
            }
            if (mouse.isCtrl()) {
                buttons |= 0x10;
            }
            if (mouse.isShift()) {
                buttons |= 0x04;
            }

            sb.append((char) (buttons & 0xFF));
            sb.append((char) (mouse.getX() + 33));
            sb.append((char) (mouse.getY() + 33));
        }

        // System.err.printf("Would write: \'%s\'\n", sb.toString());
        writeRemote(sb.toString());
    }

    /**
     * Translate the keyboard press to a VT100, VT220, or XTERM sequence and
     * send to the remote side.
     *
     * @param keypress keypress received from the local user
     */
    private void keypress(final TKeypress keypress) {
        writeRemote(keypressToString(keypress));
    }

    /**
     * Build one of the complex xterm keystroke sequences, storing the result in
     * xterm_keystroke_buffer.
     *
     * @param ss3 the prefix to use based on VT100 state.
     * @param first the first character, usually a number.
     * @param first the last character, one of the following: ~ A B C D F H
     * @param ctrl whether or not ctrl is down
     * @param alt whether or not alt is down
     * @param shift whether or not shift is down
     * @return the buffer with the full key sequence
     */
    private String xtermBuildKeySequence(final String ss3, final char first,
        final char last, boolean ctrl, boolean alt, boolean shift) {

        StringBuilder sb = new StringBuilder(ss3);
        if ((last == '~') || (ctrl == true) || (alt == true)
            || (shift == true)
        ) {
            sb.append(first);
            if (       (ctrl == false) && (alt == false) && (shift == true)) {
                sb.append(";2");
            } else if ((ctrl == false) && (alt == true) && (shift == false)) {
                sb.append(";3");
            } else if ((ctrl == false) && (alt == true) && (shift == true)) {
                sb.append(";4");
            } else if ((ctrl == true) && (alt == false) && (shift == false)) {
                sb.append(";5");
            } else if ((ctrl == true) && (alt == false) && (shift == true)) {
                sb.append(";6");
            } else if ((ctrl == true) && (alt == true) && (shift == false)) {
                sb.append(";7");
            } else if ((ctrl == true) && (alt == true) && (shift == true)) {
                sb.append(";8");
            }
        }
        sb.append(last);
        return sb.toString();
    }

    /**
     * Translate the keyboard press to a VT100, VT220, or XTERM sequence.
     *
     * @param keypress keypress received from the local user
     * @return string to transmit to the remote side
     */
    @SuppressWarnings("fallthrough")
    private String keypressToString(final TKeypress keypress) {

        if ((fullDuplex == false) && (!keypress.isFnKey())) {
            /*
             * If this is a control character, process it like it came from
             * the remote side.
             */
            if (keypress.getChar() < 0x20) {
                handleControlChar((char) keypress.getChar());
            } else {
                // Local echo for everything else
                printCharacter(keypress.getChar());
            }
            if (displayListener != null) {
                displayListener.displayChanged();
            }
        }

        if ((newLineMode == true) && (keypress.equals(kbEnter))) {
            // NLM: send CRLF
            return "\015\012";
        }

        // Handle control characters
        if ((keypress.isCtrl()) && (!keypress.isFnKey())) {
            StringBuilder sb = new StringBuilder();
            int ch = keypress.getChar();
            ch -= 0x40;
            sb.append(Character.toChars(ch));
            return sb.toString();
        }

        // Handle alt characters
        if ((keypress.isAlt()) && (!keypress.isFnKey())) {
            StringBuilder sb = new StringBuilder("\033");
            int ch = keypress.getChar();
            sb.append(Character.toChars(ch));
            return sb.toString();
        }

        if (keypress.equals(kbBackspaceDel)) {
            switch (type) {
            case VT100:
                return "\010";
            case VT102:
                return "\010";
            case VT220:
                return "\177";
            case XTERM:
                return "\177";
            }
        }

        if (keypress.equalsWithoutModifiers(kbLeft)) {
            switch (type) {
            case XTERM:
                switch (arrowKeyMode) {
                case ANSI:
                    return xtermBuildKeySequence("\033[", '1', 'D',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT52:
                    return xtermBuildKeySequence("\033", '1', 'D',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT100:
                    return xtermBuildKeySequence("\033O", '1', 'D',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                }
            default:
                switch (arrowKeyMode) {
                case ANSI:
                    return "\033[D";
                case VT52:
                    return "\033D";
                case VT100:
                    return "\033OD";
                }
            }
        }

        if (keypress.equalsWithoutModifiers(kbRight)) {
            switch (type) {
            case XTERM:
                switch (arrowKeyMode) {
                case ANSI:
                    return xtermBuildKeySequence("\033[", '1', 'C',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT52:
                    return xtermBuildKeySequence("\033", '1', 'C',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT100:
                    return xtermBuildKeySequence("\033O", '1', 'C',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                }
            default:
                switch (arrowKeyMode) {
                case ANSI:
                    return "\033[C";
                case VT52:
                    return "\033C";
                case VT100:
                    return "\033OC";
                }
            }
        }

        if (keypress.equalsWithoutModifiers(kbUp)) {
            switch (type) {
            case XTERM:
                switch (arrowKeyMode) {
                case ANSI:
                    return xtermBuildKeySequence("\033[", '1', 'A',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT52:
                    return xtermBuildKeySequence("\033", '1', 'A',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT100:
                    return xtermBuildKeySequence("\033O", '1', 'A',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                }
            default:
                switch (arrowKeyMode) {
                case ANSI:
                    return "\033[A";
                case VT52:
                    return "\033A";
                case VT100:
                    return "\033OA";
                }
            }
        }

        if (keypress.equalsWithoutModifiers(kbDown)) {
            switch (type) {
            case XTERM:
                switch (arrowKeyMode) {
                case ANSI:
                    return xtermBuildKeySequence("\033[", '1', 'B',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT52:
                    return xtermBuildKeySequence("\033", '1', 'B',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT100:
                    return xtermBuildKeySequence("\033O", '1', 'B',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                }
            default:
                switch (arrowKeyMode) {
                case ANSI:
                    return "\033[B";
                case VT52:
                    return "\033B";
                case VT100:
                    return "\033OB";
                }
            }
        }

        if (keypress.equalsWithoutModifiers(kbHome)) {
            switch (type) {
            case XTERM:
                switch (arrowKeyMode) {
                case ANSI:
                    return xtermBuildKeySequence("\033[", '1', 'H',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT52:
                    return xtermBuildKeySequence("\033", '1', 'H',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT100:
                    return xtermBuildKeySequence("\033O", '1', 'H',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                }
            default:
                switch (arrowKeyMode) {
                case ANSI:
                    return "\033[H";
                case VT52:
                    return "\033H";
                case VT100:
                    return "\033OH";
                }
            }
        }

        if (keypress.equalsWithoutModifiers(kbEnd)) {
            switch (type) {
            case XTERM:
                switch (arrowKeyMode) {
                case ANSI:
                    return xtermBuildKeySequence("\033[", '1', 'F',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT52:
                    return xtermBuildKeySequence("\033", '1', 'F',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                case VT100:
                    return xtermBuildKeySequence("\033O", '1', 'F',
                        keypress.isCtrl(), keypress.isAlt(),
                        keypress.isShift());
                }
            default:
                switch (arrowKeyMode) {
                case ANSI:
                    return "\033[F";
                case VT52:
                    return "\033F";
                case VT100:
                    return "\033OF";
                }
            }
        }

        if (keypress.equals(kbF1)) {
            // PF1
            if (vt52Mode) {
                return "\033P";
            }
            return "\033OP";
        }

        if (keypress.equals(kbF2)) {
            // PF2
            if (vt52Mode) {
                return "\033Q";
            }
            return "\033OQ";
        }

        if (keypress.equals(kbF3)) {
            // PF3
            if (vt52Mode) {
                return "\033R";
            }
            return "\033OR";
        }

        if (keypress.equals(kbF4)) {
            // PF4
            if (vt52Mode) {
                return "\033S";
            }
            return "\033OS";
        }

        if (keypress.equals(kbF5)) {
            switch (type) {
            case VT100:
                return "\033Ot";
            case VT102:
                return "\033Ot";
            case VT220:
                return "\033[15~";
            case XTERM:
                return "\033[15~";
            }
        }

        if (keypress.equals(kbF6)) {
            switch (type) {
            case VT100:
                return "\033Ou";
            case VT102:
                return "\033Ou";
            case VT220:
                return "\033[17~";
            case XTERM:
                return "\033[17~";
            }
        }

        if (keypress.equals(kbF7)) {
            switch (type) {
            case VT100:
                return "\033Ov";
            case VT102:
                return "\033Ov";
            case VT220:
                return "\033[18~";
            case XTERM:
                return "\033[18~";
            }
        }

        if (keypress.equals(kbF8)) {
            switch (type) {
            case VT100:
                return "\033Ol";
            case VT102:
                return "\033Ol";
            case VT220:
                return "\033[19~";
            case XTERM:
                return "\033[19~";
            }
        }

        if (keypress.equals(kbF9)) {
            switch (type) {
            case VT100:
                return "\033Ow";
            case VT102:
                return "\033Ow";
            case VT220:
                return "\033[20~";
            case XTERM:
                return "\033[20~";
            }
        }

        if (keypress.equals(kbF10)) {
            switch (type) {
            case VT100:
                return "\033Ox";
            case VT102:
                return "\033Ox";
            case VT220:
                return "\033[21~";
            case XTERM:
                return "\033[21~";
            }
        }

        if (keypress.equals(kbF11)) {
            return "\033[23~";
        }

        if (keypress.equals(kbF12)) {
            return "\033[24~";
        }

        if (keypress.equals(kbShiftF1)) {
            // Shifted PF1
            if (vt52Mode) {
                return "\0332P";
            }
            if (type == DeviceType.XTERM) {
                return "\0331;2P";
            }
            return "\033O2P";
        }

        if (keypress.equals(kbShiftF2)) {
            // Shifted PF2
            if (vt52Mode) {
                return "\0332Q";
            }
            if (type == DeviceType.XTERM) {
                return "\0331;2Q";
            }
            return "\033O2Q";
        }

        if (keypress.equals(kbShiftF3)) {
            // Shifted PF3
            if (vt52Mode) {
                return "\0332R";
            }
            if (type == DeviceType.XTERM) {
                return "\0331;2R";
            }
            return "\033O2R";
        }

        if (keypress.equals(kbShiftF4)) {
            // Shifted PF4
            if (vt52Mode) {
                return "\0332S";
            }
            if (type == DeviceType.XTERM) {
                return "\0331;2S";
            }
            return "\033O2S";
        }

        if (keypress.equals(kbShiftF5)) {
            // Shifted F5
            return "\033[15;2~";
        }

        if (keypress.equals(kbShiftF6)) {
            // Shifted F6
            return "\033[17;2~";
        }

        if (keypress.equals(kbShiftF7)) {
            // Shifted F7
            return "\033[18;2~";
        }

        if (keypress.equals(kbShiftF8)) {
            // Shifted F8
            return "\033[19;2~";
        }

        if (keypress.equals(kbShiftF9)) {
            // Shifted F9
            return "\033[20;2~";
        }

        if (keypress.equals(kbShiftF10)) {
            // Shifted F10
            return "\033[21;2~";
        }

        if (keypress.equals(kbShiftF11)) {
            // Shifted F11
            return "\033[23;2~";
        }

        if (keypress.equals(kbShiftF12)) {
            // Shifted F12
            return "\033[24;2~";
        }

        if (keypress.equals(kbCtrlF1)) {
            // Control PF1
            if (vt52Mode) {
                return "\0335P";
            }
            if (type == DeviceType.XTERM) {
                return "\0331;5P";
            }
            return "\033O5P";
        }

        if (keypress.equals(kbCtrlF2)) {
            // Control PF2
            if (vt52Mode) {
                return "\0335Q";
            }
            if (type == DeviceType.XTERM) {
                return "\0331;5Q";
            }
            return "\033O5Q";
        }

        if (keypress.equals(kbCtrlF3)) {
            // Control PF3
            if (vt52Mode) {
                return "\0335R";
            }
            if (type == DeviceType.XTERM) {
                return "\0331;5R";
            }
            return "\033O5R";
        }

        if (keypress.equals(kbCtrlF4)) {
            // Control PF4
            if (vt52Mode) {
                return "\0335S";
            }
            if (type == DeviceType.XTERM) {
                return "\0331;5S";
            }
            return "\033O5S";
        }

        if (keypress.equals(kbCtrlF5)) {
            // Control F5
            return "\033[15;5~";
        }

        if (keypress.equals(kbCtrlF6)) {
            // Control F6
            return "\033[17;5~";
        }

        if (keypress.equals(kbCtrlF7)) {
            // Control F7
            return "\033[18;5~";
        }

        if (keypress.equals(kbCtrlF8)) {
            // Control F8
            return "\033[19;5~";
        }

        if (keypress.equals(kbCtrlF9)) {
            // Control F9
            return "\033[20;5~";
        }

        if (keypress.equals(kbCtrlF10)) {
            // Control F10
            return "\033[21;5~";
        }

        if (keypress.equals(kbCtrlF11)) {
            // Control F11
            return "\033[23;5~";
        }

        if (keypress.equals(kbCtrlF12)) {
            // Control F12
            return "\033[24;5~";
        }

        if (keypress.equalsWithoutModifiers(kbPgUp)) {
            switch (type) {
            case XTERM:
                return xtermBuildKeySequence("\033[", '5', '~',
                    keypress.isCtrl(), keypress.isAlt(),
                    keypress.isShift());
            default:
                return "\033[5~";
            }
        }

        if (keypress.equalsWithoutModifiers(kbPgDn)) {
            switch (type) {
            case XTERM:
                return xtermBuildKeySequence("\033[", '6', '~',
                    keypress.isCtrl(), keypress.isAlt(),
                    keypress.isShift());
            default:
                return "\033[6~";
            }
        }

        if (keypress.equalsWithoutModifiers(kbIns)) {
            switch (type) {
            case XTERM:
                return xtermBuildKeySequence("\033[", '2', '~',
                    keypress.isCtrl(), keypress.isAlt(),
                    keypress.isShift());
            default:
                return "\033[2~";
            }
        }

        if (keypress.equalsWithoutModifiers(kbDel)) {
            switch (type) {
            case XTERM:
                return xtermBuildKeySequence("\033[", '3', '~',
                    keypress.isCtrl(), keypress.isAlt(),
                    keypress.isShift());
            default:
                // Delete sends real delete for VTxxx
                return "\177";
            }
        }

        if (keypress.equals(kbEnter)) {
            return "\015";
        }

        if (keypress.equals(kbEsc)) {
            return "\033";
        }

        if (keypress.equals(kbAltEsc)) {
            return "\033\033";
        }

        if (keypress.equals(kbTab)) {
            return "\011";
        }

        if ((keypress.equalsWithoutModifiers(kbBackTab)) ||
            (keypress.equals(kbShiftTab))
        ) {
            switch (type) {
            case XTERM:
                return "\033[Z";
            default:
                return "\011";
            }
        }

        // Non-alt, non-ctrl characters
        if (!keypress.isFnKey()) {
            StringBuilder sb = new StringBuilder();
            sb.append(Character.toChars(keypress.getChar()));
            return sb.toString();
        }
        return "";
    }

    /**
     * Map a symbol in any one of the VT100/VT220 character sets to a Unicode
     * symbol.
     *
     * @param ch 8-bit character from the remote side
     * @param charsetGl character set defined for GL
     * @param charsetGr character set defined for GR
     * @return character to display on the screen
     */
    private char mapCharacterCharset(final int ch,
        final CharacterSet charsetGl,
        final CharacterSet charsetGr) {

        int lookupChar = ch;
        CharacterSet lookupCharset = charsetGl;

        if (ch >= 0x80) {
            assert ((type == DeviceType.VT220) || (type == DeviceType.XTERM));
            lookupCharset = charsetGr;
            lookupChar &= 0x7F;
        }

        switch (lookupCharset) {

        case DRAWING:
            return DECCharacterSets.SPECIAL_GRAPHICS[lookupChar];

        case UK:
            return DECCharacterSets.UK[lookupChar];

        case US:
            return DECCharacterSets.US_ASCII[lookupChar];

        case NRC_DUTCH:
            return DECCharacterSets.NL[lookupChar];

        case NRC_FINNISH:
            return DECCharacterSets.FI[lookupChar];

        case NRC_FRENCH:
            return DECCharacterSets.FR[lookupChar];

        case NRC_FRENCH_CA:
            return DECCharacterSets.FR_CA[lookupChar];

        case NRC_GERMAN:
            return DECCharacterSets.DE[lookupChar];

        case NRC_ITALIAN:
            return DECCharacterSets.IT[lookupChar];

        case NRC_NORWEGIAN:
            return DECCharacterSets.NO[lookupChar];

        case NRC_SPANISH:
            return DECCharacterSets.ES[lookupChar];

        case NRC_SWEDISH:
            return DECCharacterSets.SV[lookupChar];

        case NRC_SWISS:
            return DECCharacterSets.SWISS[lookupChar];

        case DEC_SUPPLEMENTAL:
            return DECCharacterSets.DEC_SUPPLEMENTAL[lookupChar];

        case VT52_GRAPHICS:
            return DECCharacterSets.VT52_SPECIAL_GRAPHICS[lookupChar];

        case ROM:
            return DECCharacterSets.US_ASCII[lookupChar];

        case ROM_SPECIAL:
            return DECCharacterSets.US_ASCII[lookupChar];

        default:
            throw new IllegalArgumentException("Invalid character set value: "
                + lookupCharset);
        }
    }

    /**
     * Map an 8-bit byte into a printable character.
     *
     * @param ch either 8-bit or Unicode character from the remote side
     * @return character to display on the screen
     */
    private int mapCharacter(final int ch) {
        if (ch >= 0x100) {
            // Unicode character, just return it
            return ch;
        }

        CharacterSet charsetGl = currentState.g0Charset;
        CharacterSet charsetGr = currentState.grCharset;

        if (vt52Mode == true) {
            if (shiftOut == true) {
                // Shifted out character, pull from VT52 graphics
                charsetGl = currentState.g1Charset;
                charsetGr = CharacterSet.US;
            } else {
                // Normal
                charsetGl = currentState.g0Charset;
                charsetGr = CharacterSet.US;
            }

            // Pull the character
            return mapCharacterCharset(ch, charsetGl, charsetGr);
        }

        // shiftOout
        if (shiftOut == true) {
            // Shifted out character, pull from G1
            charsetGl = currentState.g1Charset;
            charsetGr = currentState.grCharset;

            // Pull the character
            return mapCharacterCharset(ch, charsetGl, charsetGr);
        }

        // SS2
        if (singleshift == Singleshift.SS2) {

            singleshift = Singleshift.NONE;

            // Shifted out character, pull from G2
            charsetGl = currentState.g2Charset;
            charsetGr = currentState.grCharset;
        }

        // SS3
        if (singleshift == Singleshift.SS3) {

            singleshift = Singleshift.NONE;

            // Shifted out character, pull from G3
            charsetGl = currentState.g3Charset;
            charsetGr = currentState.grCharset;
        }

        if ((type == DeviceType.VT220) || (type == DeviceType.XTERM)) {
            // Check for locking shift

            switch (currentState.glLockshift) {

            case G1_GR:
                throw new IllegalArgumentException("programming bug");

            case G2_GR:
                throw new IllegalArgumentException("programming bug");

            case G3_GR:
                throw new IllegalArgumentException("programming bug");

            case G2_GL:
                // LS2
                charsetGl = currentState.g2Charset;
                break;

            case G3_GL:
                // LS3
                charsetGl = currentState.g3Charset;
                break;

            case NONE:
                // Normal
                charsetGl = currentState.g0Charset;
                break;
            }

            switch (currentState.grLockshift) {

            case G2_GL:
                throw new IllegalArgumentException("programming bug");

            case G3_GL:
                throw new IllegalArgumentException("programming bug");

            case G1_GR:
                // LS1R
                charsetGr = currentState.g1Charset;
                break;

            case G2_GR:
                // LS2R
                charsetGr = currentState.g2Charset;
                break;

            case G3_GR:
                // LS3R
                charsetGr = currentState.g3Charset;
                break;

            case NONE:
                // Normal
                charsetGr = CharacterSet.DEC_SUPPLEMENTAL;
                break;
            }


        }

        // Pull the character
        return mapCharacterCharset(ch, charsetGl, charsetGr);
    }

    /**
     * Scroll the text within a scrolling region up n lines.
     *
     * @param regionTop top row of the scrolling region
     * @param regionBottom bottom row of the scrolling region
     * @param n number of lines to scroll
     */
    private void scrollingRegionScrollUp(final int regionTop,
        final int regionBottom, final int n) {

        if (regionTop >= regionBottom) {
            return;
        }

        // Sanity check: see if there will be any characters left after the
        // scroll
        if (regionBottom + 1 - regionTop <= n) {
            // There won't be anything left in the region, so just call
            // eraseScreen() and return.
            eraseScreen(regionTop, 0, regionBottom, width - 1, false);
            return;
        }

        int remaining = regionBottom + 1 - regionTop - n;
        List<DisplayLine> displayTop = display.subList(0, regionTop);
        List<DisplayLine> displayBottom = display.subList(regionBottom + 1,
            display.size());
        List<DisplayLine> displayMiddle = display.subList(regionBottom + 1
            - remaining, regionBottom + 1);
        display = new ArrayList<DisplayLine>(displayTop);
        display.addAll(displayMiddle);
        for (int i = 0; i < n; i++) {
            DisplayLine line = new DisplayLine(currentState.attr);
            line.setReverseColor(reverseVideo);
            display.add(line);
        }
        display.addAll(displayBottom);

        assert (display.size() == height);
    }

    /**
     * Scroll the text within a scrolling region down n lines.
     *
     * @param regionTop top row of the scrolling region
     * @param regionBottom bottom row of the scrolling region
     * @param n number of lines to scroll
     */
    private void scrollingRegionScrollDown(final int regionTop,
        final int regionBottom, final int n) {

        if (regionTop >= regionBottom) {
            return;
        }

        // Sanity check: see if there will be any characters left after the
        // scroll
        if (regionBottom + 1 - regionTop <= n) {
            // There won't be anything left in the region, so just call
            // eraseScreen() and return.
            eraseScreen(regionTop, 0, regionBottom, width - 1, false);
            return;
        }

        int remaining = regionBottom + 1 - regionTop - n;
        List<DisplayLine> displayTop = display.subList(0, regionTop);
        List<DisplayLine> displayBottom = display.subList(regionBottom + 1,
            display.size());
        List<DisplayLine> displayMiddle = display.subList(regionTop,
            regionTop + remaining);
        display = new ArrayList<DisplayLine>(displayTop);
        for (int i = 0; i < n; i++) {
            DisplayLine line = new DisplayLine(currentState.attr);
            line.setReverseColor(reverseVideo);
            display.add(line);
        }
        display.addAll(displayMiddle);
        display.addAll(displayBottom);

        assert (display.size() == height);
    }

    /**
     * Process a control character.
     *
     * @param ch 8-bit character from the remote side
     */
    private void handleControlChar(final char ch) {
        assert ((ch <= 0x1F) || ((ch >= 0x7F) && (ch <= 0x9F)));

        switch (ch) {

        case 0x00:
            // NUL - discard
            return;

        case 0x05:
            // ENQ

            // Transmit the answerback message.
            // Not supported
            break;

        case 0x07:
            // BEL
            // Not supported
            break;

        case 0x08:
            // BS
            cursorLeft(1, false);
            break;

        case 0x09:
            // HT
            advanceToNextTabStop();
            break;

        case 0x0A:
            // LF
            linefeed();
            break;

        case 0x0B:
            // VT
            linefeed();
            break;

        case 0x0C:
            // FF
            linefeed();
            break;

        case 0x0D:
            // CR
            carriageReturn();
            break;

        case 0x0E:
            // SO
            shiftOut = true;
            currentState.glLockshift = LockshiftMode.NONE;
            break;

        case 0x0F:
            // SI
            shiftOut = false;
            currentState.glLockshift = LockshiftMode.NONE;
            break;

        case 0x84:
            // IND
            ind();
            break;

        case 0x85:
            // NEL
            nel();
            break;

        case 0x88:
            // HTS
            hts();
            break;

        case 0x8D:
            // RI
            ri();
            break;

        case 0x8E:
            // SS2
            singleshift = Singleshift.SS2;
            break;

        case 0x8F:
            // SS3
            singleshift = Singleshift.SS3;
            break;

        default:
            break;
        }

    }

    /**
     * Advance the cursor to the next tab stop.
     */
    private void advanceToNextTabStop() {
        if (tabStops.size() == 0) {
            // Go to the rightmost column
            cursorRight(rightMargin - currentState.cursorX, false);
            return;
        }
        for (Integer stop: tabStops) {
            if (stop > currentState.cursorX) {
                cursorRight(stop - currentState.cursorX, false);
                return;
            }
        }
        /*
         * We got here, meaning there isn't a tab stop beyond the current
         * cursor position.  Place the cursor of the right-most edge of the
         * screen.
         */
        cursorRight(rightMargin - currentState.cursorX, false);
    }

    /**
     * Save a character into the collect buffer.
     *
     * @param ch character to save
     */
    private void collect(final char ch) {
        collectBuffer.append(ch);
    }

    /**
     * Save a byte into the CSI parameters buffer.
     *
     * @param ch byte to save
     */
    private void param(final byte ch) {
        if (csiParams.size() == 0) {
            csiParams.add(Integer.valueOf(0));
        }
        Integer x = csiParams.get(csiParams.size() - 1);
        if ((ch >= '0') && (ch <= '9')) {
            x *= 10;
            x += (ch - '0');
            csiParams.set(csiParams.size() - 1, x);
        }

        if ((ch == ';') && (csiParams.size() < 16)) {
            csiParams.add(Integer.valueOf(0));
        }
    }

    /**
     * Get a CSI parameter value, with a default.
     *
     * @param position parameter index.  0 is the first parameter.
     * @param defaultValue value to use if csiParams[position] doesn't exist
     * @return parameter value
     */
    private int getCsiParam(final int position, final int defaultValue) {
        if (csiParams.size() < position + 1) {
            return defaultValue;
        }
        return csiParams.get(position).intValue();
    }

    /**
     * Get a CSI parameter value, clamped to within min/max.
     *
     * @param position parameter index.  0 is the first parameter.
     * @param defaultValue value to use if csiParams[position] doesn't exist
     * @param minValue minimum value inclusive
     * @param maxValue maximum value inclusive
     * @return parameter value
     */
    private int getCsiParam(final int position, final int defaultValue,
        final int minValue, final int maxValue) {

        assert (minValue <= maxValue);
        int value = getCsiParam(position, defaultValue);
        if (value < minValue) {
            value = minValue;
        }
        if (value > maxValue) {
            value = maxValue;
        }
        return value;
    }

    /**
     * Set or unset a toggle.
     *
     * @param value true for set ('h'), false for reset ('l')
     */
    private void setToggle(final boolean value) {
        boolean decPrivateModeFlag = false;

        for (int i = 0; i < collectBuffer.length(); i++) {
            if (collectBuffer.charAt(i) == '?') {
                decPrivateModeFlag = true;
                break;
            }
        }

        for (Integer i: csiParams) {

            switch (i) {

            case 1:
                if (decPrivateModeFlag == true) {
                    // DECCKM
                    if (value == true) {
                        // Use application arrow keys
                        arrowKeyMode = ArrowKeyMode.VT100;
                    } else {
                        // Use ANSI arrow keys
                        arrowKeyMode = ArrowKeyMode.ANSI;
                    }
                }
                break;
            case 2:
                if (decPrivateModeFlag == true) {
                    if (value == false) {

                        // DECANM
                        vt52Mode = true;
                        arrowKeyMode = ArrowKeyMode.VT52;

                        /*
                         * From the VT102 docs: "You use ANSI mode to select
                         * most terminal features; the terminal uses the same
                         * features when it switches to VT52 mode. You
                         * cannot, however, change most of these features in
                         * VT52 mode."
                         *
                         * In other words, do not reset any other attributes
                         * when switching between VT52 submode and ANSI.
                         *
                         * HOWEVER, the real vt100 does switch the character
                         * set according to Usenet.
                         */
                        currentState.g0Charset = CharacterSet.US;
                        currentState.g1Charset = CharacterSet.DRAWING;
                        shiftOut = false;

                        if ((type == DeviceType.VT220)
                            || (type == DeviceType.XTERM)) {

                            // VT52 mode is explicitly 7-bit
                            s8c1t = false;
                            singleshift = Singleshift.NONE;
                        }
                    }
                } else {
                    // KAM
                    if (value == true) {
                        // Turn off keyboard
                        // Not supported
                    } else {
                        // Turn on keyboard
                        // Not supported
                    }
                }
                break;
            case 3:
                if (decPrivateModeFlag == true) {
                    // DECCOLM
                    if (value == true) {
                        // 132 columns
                        columns132 = true;
                        rightMargin = 131;
                    } else {
                        // 80 columns
                        columns132 = false;
                        if ((displayListener != null)
                            && (type == DeviceType.XTERM)
                        ) {
                            // For xterms, reset to the actual width, not 80
                            // columns.
                            width = displayListener.getDisplayWidth();
                            rightMargin = width - 1;
                        } else {
                            rightMargin = 79;
                            width = rightMargin + 1;
                        }
                    }
                    // Entire screen is cleared, and scrolling region is
                    // reset
                    eraseScreen(0, 0, height - 1, width - 1, false);
                    scrollRegionTop = 0;
                    scrollRegionBottom = height - 1;
                    // Also home the cursor
                    cursorPosition(0, 0);
                }
                break;
            case 4:
                if (decPrivateModeFlag == true) {
                    // DECSCLM
                    if (value == true) {
                        // Smooth scroll
                        // Not supported
                    } else {
                        // Jump scroll
                        // Not supported
                    }
                } else {
                    // IRM
                    if (value == true) {
                        insertMode = true;
                    } else {
                        insertMode = false;
                    }
                }
                break;
            case 5:
                if (decPrivateModeFlag == true) {
                    // DECSCNM
                    if (value == true) {
                        /*
                         * Set selects reverse screen, a white screen
                         * background with black characters.
                         */
                        if (reverseVideo != true) {
                            /*
                             * If in normal video, switch it back
                             */
                            invertDisplayColors();
                        }
                        reverseVideo = true;
                    } else {
                        /*
                         * Reset selects normal screen, a black screen
                         * background with white characters.
                         */
                        if (reverseVideo == true) {
                            /*
                             * If in reverse video already, switch it back
                             */
                            invertDisplayColors();
                        }
                        reverseVideo = false;
                    }
                }
                break;
            case 6:
                if (decPrivateModeFlag == true) {
                    // DECOM
                    if (value == true) {
                        // Origin is relative to scroll region cursor.
                        // Cursor can NEVER leave scrolling region.
                        currentState.originMode = true;
                        cursorPosition(0, 0);
                    } else {
                        // Origin is absolute to entire screen.  Cursor can
                        // leave the scrolling region via cup() and hvp().
                        currentState.originMode = false;
                        cursorPosition(0, 0);
                    }
                }
                break;
            case 7:
                if (decPrivateModeFlag == true) {
                    // DECAWM
                    if (value == true) {
                        // Turn linewrap on
                        currentState.lineWrap = true;
                    } else {
                        // Turn linewrap off
                        currentState.lineWrap = false;
                    }
                }
                break;
            case 8:
                if (decPrivateModeFlag == true) {
                    // DECARM
                    if (value == true) {
                        // Keyboard auto-repeat on
                        // Not supported
                    } else {
                        // Keyboard auto-repeat off
                        // Not supported
                    }
                }
                break;
            case 12:
                if (decPrivateModeFlag == false) {
                    // SRM
                    if (value == true) {
                        // Local echo off
                        fullDuplex = true;
                    } else {
                        // Local echo on
                        fullDuplex = false;
                    }
                }
                break;
            case 18:
                if (decPrivateModeFlag == true) {
                    // DECPFF
                    // Not supported
                }
                break;
            case 19:
                if (decPrivateModeFlag == true) {
                    // DECPEX
                    // Not supported
                }
                break;
            case 20:
                if (decPrivateModeFlag == false) {
                    // LNM
                    if (value == true) {
                        /*
                         * Set causes a received linefeed, form feed, or
                         * vertical tab to move cursor to first column of
                         * next line. RETURN transmits both a carriage return
                         * and linefeed. This selection is also called new
                         * line option.
                         */
                        newLineMode = true;
                    } else {
                        /*
                         * Reset causes a received linefeed, form feed, or
                         * vertical tab to move cursor to next line in
                         * current column. RETURN transmits a carriage
                         * return.
                         */
                        newLineMode = false;
                    }
                }
                break;

            case 25:
                if ((type == DeviceType.VT220) || (type == DeviceType.XTERM)) {
                    if (decPrivateModeFlag == true) {
                        // DECTCEM
                        if (value == true) {
                            // Visible cursor
                            cursorVisible = true;
                        } else {
                            // Invisible cursor
                            cursorVisible = false;
                        }
                    }
                }
                break;

            case 42:
                if ((type == DeviceType.VT220) || (type == DeviceType.XTERM)) {
                    if (decPrivateModeFlag == true) {
                        // DECNRCM
                        if (value == true) {
                            // Select national mode NRC
                            // Not supported
                        } else {
                            // Select multi-national mode
                            // Not supported
                        }
                    }
                }

                break;

            case 80:
                if (type == DeviceType.XTERM) {
                    if (decPrivateModeFlag == true) {
                        if (value == true) {
                            // Enable sixel scrolling (default).
                            // Not supported
                        } else {
                            // Disable sixel scrolling.
                            // Not supported
                        }
                    }
                }

                break;

            case 1000:
                if ((type == DeviceType.XTERM)
                    && (decPrivateModeFlag == true)
                ) {
                    // Mouse: normal tracking mode
                    if (value == true) {
                        mouseProtocol = MouseProtocol.NORMAL;
                    } else {
                        mouseProtocol = MouseProtocol.OFF;
                    }
                }
                break;

            case 1002:
                if ((type == DeviceType.XTERM)
                    && (decPrivateModeFlag == true)
                ) {
                    // Mouse: normal tracking mode
                    if (value == true) {
                        mouseProtocol = MouseProtocol.BUTTONEVENT;
                    } else {
                        mouseProtocol = MouseProtocol.OFF;
                    }
                }
                break;

            case 1003:
                if ((type == DeviceType.XTERM)
                    && (decPrivateModeFlag == true)
                ) {
                    // Mouse: Any-event tracking mode
                    if (value == true) {
                        mouseProtocol = MouseProtocol.ANYEVENT;
                    } else {
                        mouseProtocol = MouseProtocol.OFF;
                    }
                }
                break;

            case 1005:
                if ((type == DeviceType.XTERM)
                    && (decPrivateModeFlag == true)
                ) {
                    // Mouse: UTF-8 coordinates
                    if (value == true) {
                        mouseEncoding = MouseEncoding.UTF8;
                    } else {
                        mouseEncoding = MouseEncoding.X10;
                    }
                }
                break;

            case 1006:
                if ((type == DeviceType.XTERM)
                    && (decPrivateModeFlag == true)
                ) {
                    // Mouse: SGR coordinates
                    if (value == true) {
                        mouseEncoding = MouseEncoding.SGR;
                    } else {
                        mouseEncoding = MouseEncoding.X10;
                    }
                }
                break;

            case 1070:
                if (type == DeviceType.XTERM) {
                    if (decPrivateModeFlag == true) {
                        if (value == true) {
                            // Use private color registers for each sixel
                            // graphic (default).
                            sixelPalette = null;
                        } else {
                            // Use shared color registers for each sixel
                            // graphic.
                            sixelPalette = new HashMap<Integer, java.awt.Color>();
                        }
                    }
                }
                break;

            default:
                break;

            }
        }
    }

    /**
     * DECSC - Save cursor.
     */
    private void decsc() {
        savedState.setTo(currentState);
    }

    /**
     * DECRC - Restore cursor.
     */
    private void decrc() {
        currentState.setTo(savedState);
    }

    /**
     * IND - Index.
     */
    private void ind() {
        // Move the cursor and scroll if necessary.  If at the bottom line
        // already, a scroll up is supposed to be performed.
        if (currentState.cursorY == scrollRegionBottom) {
            scrollingRegionScrollUp(scrollRegionTop, scrollRegionBottom, 1);
        }
        cursorDown(1, true);
    }

    /**
     * RI - Reverse index.
     */
    private void ri() {
        // Move the cursor and scroll if necessary.  If at the top line
        // already, a scroll down is supposed to be performed.
        if (currentState.cursorY == scrollRegionTop) {
            scrollingRegionScrollDown(scrollRegionTop, scrollRegionBottom, 1);
        }
        cursorUp(1, true);
    }

    /**
     * NEL - Next line.
     */
    private void nel() {
        // Move the cursor and scroll if necessary.  If at the bottom line
        // already, a scroll up is supposed to be performed.
        if (currentState.cursorY == scrollRegionBottom) {
            scrollingRegionScrollUp(scrollRegionTop, scrollRegionBottom, 1);
        }
        cursorDown(1, true);

        // Reset to the beginning of the next line
        currentState.cursorX = 0;
    }

    /**
     * DECKPAM - Keypad application mode.
     */
    private void deckpam() {
        keypadMode = KeypadMode.Application;
    }

    /**
     * DECKPNM - Keypad numeric mode.
     */
    private void deckpnm() {
        keypadMode = KeypadMode.Numeric;
    }

    /**
     * Move up n spaces.
     *
     * @param n number of spaces to move
     * @param honorScrollRegion if true, then do nothing if the cursor is
     * outside the scrolling region
     */
    private void cursorUp(final int n, final boolean honorScrollRegion) {
        int top;

        /*
         * Special case: if a user moves the cursor from the right margin, we
         * have to reset the VT100 right margin flag.
         */
        if (n > 0) {
            wrapLineFlag = false;
        }

        for (int i = 0; i < n; i++) {
            if (honorScrollRegion == true) {
                // Honor the scrolling region
                if ((currentState.cursorY < scrollRegionTop)
                    || (currentState.cursorY > scrollRegionBottom)
                ) {
                    // Outside region, do nothing
                    return;
                }
                // Inside region, go up
                top = scrollRegionTop;
            } else {
                // Non-scrolling case
                top = 0;
            }

            if (currentState.cursorY > top) {
                currentState.cursorY--;
            }
        }
    }

    /**
     * Move down n spaces.
     *
     * @param n number of spaces to move
     * @param honorScrollRegion if true, then do nothing if the cursor is
     * outside the scrolling region
     */
    private void cursorDown(final int n, final boolean honorScrollRegion) {
        int bottom;

        /*
         * Special case: if a user moves the cursor from the right margin, we
         * have to reset the VT100 right margin flag.
         */
        if (n > 0) {
            wrapLineFlag = false;
        }

        for (int i = 0; i < n; i++) {

            if (honorScrollRegion == true) {
                // Honor the scrolling region
                if (currentState.cursorY > scrollRegionBottom) {
                    // Outside region, do nothing
                    return;
                }
                // Inside region, go down
                bottom = scrollRegionBottom;
            } else {
                // Non-scrolling case
                bottom = height - 1;
            }

            if (currentState.cursorY < bottom) {
                currentState.cursorY++;
            }
        }
    }

    /**
     * Move left n spaces.
     *
     * @param n number of spaces to move
     * @param honorScrollRegion if true, then do nothing if the cursor is
     * outside the scrolling region
     */
    private void cursorLeft(final int n, final boolean honorScrollRegion) {
        /*
         * Special case: if a user moves the cursor from the right margin, we
         * have to reset the VT100 right margin flag.
         */
        if (n > 0) {
            wrapLineFlag = false;
        }

        for (int i = 0; i < n; i++) {
            if (honorScrollRegion == true) {
                // Honor the scrolling region
                if ((currentState.cursorY < scrollRegionTop)
                    || (currentState.cursorY > scrollRegionBottom)
                ) {
                    // Outside region, do nothing
                    return;
                }
            }

            if (currentState.cursorX > 0) {
                currentState.cursorX--;
            }
        }
    }

    /**
     * Move right n spaces.
     *
     * @param n number of spaces to move
     * @param honorScrollRegion if true, then do nothing if the cursor is
     * outside the scrolling region
     */
    private void cursorRight(final int n, final boolean honorScrollRegion) {
        int rightMargin = this.rightMargin;

        /*
         * Special case: if a user moves the cursor from the right margin, we
         * have to reset the VT100 right margin flag.
         */
        if (n > 0) {
            wrapLineFlag = false;
        }

        if (display.get(currentState.cursorY).isDoubleWidth()) {
            rightMargin = ((rightMargin + 1) / 2) - 1;
        }

        for (int i = 0; i < n; i++) {
            if (honorScrollRegion == true) {
                // Honor the scrolling region
                if ((currentState.cursorY < scrollRegionTop)
                    || (currentState.cursorY > scrollRegionBottom)
                ) {
                    // Outside region, do nothing
                    return;
                }
            }

            if (currentState.cursorX < rightMargin) {
                currentState.cursorX++;
            }
        }
    }

    /**
     * Move cursor to (col, row) where (0, 0) is the top-left corner.
     *
     * @param row row to move to
     * @param col column to move to
     */
    private void cursorPosition(int row, final int col) {
        int rightMargin = this.rightMargin;

        assert (col >= 0);
        assert (row >= 0);

        if (display.get(currentState.cursorY).isDoubleWidth()) {
            rightMargin = ((rightMargin + 1) / 2) - 1;
        }

        // Set column number
        currentState.cursorX = col;

        // Sanity check, bring column back to margin.
        if (currentState.cursorX > rightMargin) {
            currentState.cursorX = rightMargin;
        }

        // Set row number
        if (currentState.originMode == true) {
            row += scrollRegionTop;
        }
        if (currentState.cursorY < row) {
            cursorDown(row - currentState.cursorY, false);
        } else if (currentState.cursorY > row) {
            cursorUp(currentState.cursorY - row, false);
        }

        wrapLineFlag = false;
    }

    /**
     * HTS - Horizontal tabulation set.
     */
    private void hts() {
        for (Integer stop: tabStops) {
            if (stop == currentState.cursorX) {
                // Already have a tab stop here
                return;
            }
        }

        // Append a tab stop to the end of the array and resort them
        tabStops.add(currentState.cursorX);
        Collections.sort(tabStops);
    }

    /**
     * DECSWL - Single-width line.
     */
    private void decswl() {
        display.get(currentState.cursorY).setDoubleWidth(false);
        display.get(currentState.cursorY).setDoubleHeight(0);
    }

    /**
     * DECDWL - Double-width line.
     */
    private void decdwl() {
        display.get(currentState.cursorY).setDoubleWidth(true);
        display.get(currentState.cursorY).setDoubleHeight(0);
    }

    /**
     * DECHDL - Double-height + double-width line.
     *
     * @param topHalf if true, this sets the row to be the top half row of a
     * double-height row
     */
    private void dechdl(final boolean topHalf) {
        display.get(currentState.cursorY).setDoubleWidth(true);
        if (topHalf == true) {
            display.get(currentState.cursorY).setDoubleHeight(1);
        } else {
            display.get(currentState.cursorY).setDoubleHeight(2);
        }
    }

    /**
     * DECALN - Screen alignment display.
     */
    private void decaln() {
        Cell newCell = new Cell('E');
        for (DisplayLine line: display) {
            for (int i = 0; i < line.length(); i++) {
                line.replace(i, newCell);
            }
        }
    }

    /**
     * DECSCL - Compatibility level.
     */
    private void decscl() {
        int i = getCsiParam(0, 0);
        int j = getCsiParam(1, 0);

        if (i == 61) {
            // Reset fonts
            currentState.g0Charset = CharacterSet.US;
            currentState.g1Charset = CharacterSet.DRAWING;
            s8c1t = false;
        } else if (i == 62) {

            if ((j == 0) || (j == 2)) {
                s8c1t = true;
            } else if (j == 1) {
                s8c1t = false;
            }
        }
    }

    /**
     * CUD - Cursor down.
     */
    private void cud() {
        cursorDown(getCsiParam(0, 1, 1, height), true);
    }

    /**
     * CUF - Cursor forward.
     */
    private void cuf() {
        cursorRight(getCsiParam(0, 1, 1, rightMargin + 1), true);
    }

    /**
     * CUB - Cursor backward.
     */
    private void cub() {
        cursorLeft(getCsiParam(0, 1, 1, currentState.cursorX + 1), true);
    }

    /**
     * CUU - Cursor up.
     */
    private void cuu() {
        cursorUp(getCsiParam(0, 1, 1, currentState.cursorY + 1), true);
    }

    /**
     * CUP - Cursor position.
     */
    private void cup() {
        cursorPosition(getCsiParam(0, 1, 1, height) - 1,
            getCsiParam(1, 1, 1, rightMargin + 1) - 1);
    }

    /**
     * CNL - Cursor down and to column 1.
     */
    private void cnl() {
        cursorDown(getCsiParam(0, 1, 1, height), true);
        // To column 0
        cursorLeft(currentState.cursorX, true);
    }

    /**
     * CPL - Cursor up and to column 1.
     */
    private void cpl() {
        cursorUp(getCsiParam(0, 1, 1, currentState.cursorY + 1), true);
        // To column 0
        cursorLeft(currentState.cursorX, true);
    }

    /**
     * CHA - Cursor to column # in current row.
     */
    private void cha() {
        cursorPosition(currentState.cursorY,
            getCsiParam(0, 1, 1, rightMargin + 1) - 1);
    }

    /**
     * VPA - Cursor to row #, same column.
     */
    private void vpa() {
        cursorPosition(getCsiParam(0, 1, 1, height) - 1,
            currentState.cursorX);
    }

    /**
     * ED - Erase in display.
     */
    private void ed() {
        boolean honorProtected = false;
        boolean decPrivateModeFlag = false;

        for (int i = 0; i < collectBuffer.length(); i++) {
            if (collectBuffer.charAt(i) == '?') {
                decPrivateModeFlag = true;
                break;
            }
        }

        if (((type == DeviceType.VT220) || (type == DeviceType.XTERM))
            && (decPrivateModeFlag == true)
        ) {
            honorProtected = true;
        }

        int i = getCsiParam(0, 0);

        if (i == 0) {
            // Erase from here to end of screen
            if (currentState.cursorY < height - 1) {
                eraseScreen(currentState.cursorY + 1, 0, height - 1, width - 1,
                    honorProtected);
            }
            eraseLine(currentState.cursorX, width - 1, honorProtected);
        } else if (i == 1) {
            // Erase from beginning of screen to here
            eraseScreen(0, 0, currentState.cursorY - 1, width - 1,
                honorProtected);
            eraseLine(0, currentState.cursorX, honorProtected);
        } else if (i == 2) {
            // Erase entire screen
            eraseScreen(0, 0, height - 1, width - 1, honorProtected);
        }
    }

    /**
     * EL - Erase in line.
     */
    private void el() {
        boolean honorProtected = false;
        boolean decPrivateModeFlag = false;

        for (int i = 0; i < collectBuffer.length(); i++) {
            if (collectBuffer.charAt(i) == '?') {
                decPrivateModeFlag = true;
                break;
            }
        }

        if (((type == DeviceType.VT220) || (type == DeviceType.XTERM))
            && (decPrivateModeFlag == true)
        ) {
            honorProtected = true;
        }

        int i = getCsiParam(0, 0);

        if (i == 0) {
            // Erase from here to end of line
            eraseLine(currentState.cursorX, width - 1, honorProtected);
        } else if (i == 1) {
            // Erase from beginning of line to here
            eraseLine(0, currentState.cursorX, honorProtected);
        } else if (i == 2) {
            // Erase entire line
            eraseLine(0, width - 1, honorProtected);
        }
    }

    /**
     * ECH - Erase # of characters in current row.
     */
    private void ech() {
        int i = getCsiParam(0, 1, 1, width);

        // Erase from here to i characters
        eraseLine(currentState.cursorX, currentState.cursorX + i - 1, false);
    }

    /**
     * IL - Insert line.
     */
    private void il() {
        int i = getCsiParam(0, 1);

        if ((currentState.cursorY >= scrollRegionTop)
            && (currentState.cursorY <= scrollRegionBottom)
        ) {

            // I can get the same effect with a scroll-down
            scrollingRegionScrollDown(currentState.cursorY,
                scrollRegionBottom, i);
        }
    }

    /**
     * DCH - Delete char.
     */
    private void dch() {
        int n = getCsiParam(0, 1);
        DisplayLine line = display.get(currentState.cursorY);
        Cell blank = new Cell();
        for (int i = 0; i < n; i++) {
            line.delete(currentState.cursorX, blank);
        }
    }

    /**
     * ICH - Insert blank char at cursor.
     */
    private void ich() {
        int n = getCsiParam(0, 1);
        DisplayLine line = display.get(currentState.cursorY);
        Cell blank = new Cell();
        for (int i = 0; i < n; i++) {
            line.insert(currentState.cursorX, blank);
        }
    }

    /**
     * DL - Delete line.
     */
    private void dl() {
        int i = getCsiParam(0, 1);

        if ((currentState.cursorY >= scrollRegionTop)
            && (currentState.cursorY <= scrollRegionBottom)) {

            // I can get the same effect with a scroll-down
            scrollingRegionScrollUp(currentState.cursorY,
                scrollRegionBottom, i);
        }
    }

    /**
     * HVP - Horizontal and vertical position.
     */
    private void hvp() {
        cup();
    }

    /**
     * REP - Repeat character.
     */
    private void rep() {
        int n = getCsiParam(0, 1);
        for (int i = 0; i < n; i++) {
            printCharacter(repCh);
        }
    }

    /**
     * SU - Scroll up.
     */
    private void su() {
        scrollingRegionScrollUp(scrollRegionTop, scrollRegionBottom,
            getCsiParam(0, 1, 1, height));
    }

    /**
     * SD - Scroll down.
     */
    private void sd() {
        scrollingRegionScrollDown(scrollRegionTop, scrollRegionBottom,
            getCsiParam(0, 1, 1, height));
    }

    /**
     * CBT - Go back X tab stops.
     */
    private void cbt() {
        int tabsToMove = getCsiParam(0, 1);
        int tabI;

        for (int i = 0; i < tabsToMove; i++) {
            int j = currentState.cursorX;
            for (tabI = 0; tabI < tabStops.size(); tabI++) {
                if (tabStops.get(tabI) >= currentState.cursorX) {
                    break;
                }
            }
            tabI--;
            if (tabI <= 0) {
                j = 0;
            } else {
                j = tabStops.get(tabI);
            }
            cursorPosition(currentState.cursorY, j);
        }
    }

    /**
     * CHT - Advance X tab stops.
     */
    private void cht() {
        int n = getCsiParam(0, 1);
        for (int i = 0; i < n; i++) {
            advanceToNextTabStop();
        }
    }

    /**
     * SGR - Select graphics rendition.
     */
    private void sgr() {

        if (csiParams.size() == 0) {
            currentState.attr.reset();
            return;
        }

        int sgrColorMode = -1;
        boolean idx88Color = false;
        boolean rgbColor = false;
        int rgbRed = -1;
        int rgbGreen = -1;

        for (Integer i: csiParams) {

            if ((sgrColorMode == 38) || (sgrColorMode == 48)) {

                assert (type == DeviceType.XTERM);

                if (idx88Color) {
                    /*
                     * Indexed color mode, we now have the index number.
                     */
                    if (sgrColorMode == 38) {
                        currentState.attr.setForeColorRGB(get88Color(i));
                    } else {
                        assert (sgrColorMode == 48);
                        currentState.attr.setBackColorRGB(get88Color(i));
                    }
                    sgrColorMode = -1;
                    idx88Color = false;
                    continue;
                }

                if (rgbColor) {
                    /*
                     * RGB color mode, we are collecting tokens.
                     */
                    if (rgbRed == -1) {
                        rgbRed = i & 0xFF;
                    } else if (rgbGreen == -1) {
                        rgbGreen = i & 0xFF;
                    } else {
                        int rgb = rgbRed << 16;
                        rgb |= rgbGreen << 8;
                        rgb |= i & 0xFF;

                        // System.err.printf("RGB: %08x\n", rgb);

                        if (sgrColorMode == 38) {
                            currentState.attr.setForeColorRGB(rgb);
                        } else {
                            assert (sgrColorMode == 48);
                            currentState.attr.setBackColorRGB(rgb);
                        }
                        rgbRed = -1;
                        rgbGreen = -1;
                        sgrColorMode = -1;
                        rgbColor = false;
                    }
                    continue;
                }

                switch (i) {

                case 2:
                    /*
                     * RGB color mode.
                     */
                    rgbColor = true;
                    continue;

                case 5:
                    /*
                     * Indexed color mode.
                     */
                    idx88Color = true;
                    continue;

                default:
                    /*
                     * This is neither indexed nor RGB color.  Bail out.
                     */
                    return;
                }

            } // if ((sgrColorMode == 38) || (sgrColorMode == 48))

            switch (i) {

            case 0:
                // Normal
                currentState.attr.reset();
                break;

            case 1:
                // Bold
                currentState.attr.setBold(true);
                break;

            case 4:
                // Underline
                currentState.attr.setUnderline(true);
                break;

            case 5:
                // Blink
                currentState.attr.setBlink(true);
                break;

            case 7:
                // Reverse
                currentState.attr.setReverse(true);
                break;

            default:
                break;
            }

            if (type == DeviceType.XTERM) {

                switch (i) {

                case 8:
                    // Invisible
                    // Not supported
                    break;

                case 90:
                    // Set black foreground
                    currentState.attr.setForeColorRGB(get88Color(8));
                    break;
                case 91:
                    // Set red foreground
                    currentState.attr.setForeColorRGB(get88Color(9));
                    break;
                case 92:
                    // Set green foreground
                    currentState.attr.setForeColorRGB(get88Color(10));
                    break;
                case 93:
                    // Set yellow foreground
                    currentState.attr.setForeColorRGB(get88Color(11));
                    break;
                case 94:
                    // Set blue foreground
                    currentState.attr.setForeColorRGB(get88Color(12));
                    break;
                case 95:
                    // Set magenta foreground
                    currentState.attr.setForeColorRGB(get88Color(13));
                    break;
                case 96:
                    // Set cyan foreground
                    currentState.attr.setForeColorRGB(get88Color(14));
                    break;
                case 97:
                    // Set white foreground
                    currentState.attr.setForeColorRGB(get88Color(15));
                    break;

                case 100:
                    // Set black background
                    currentState.attr.setBackColorRGB(get88Color(8));
                    break;
                case 101:
                    // Set red background
                    currentState.attr.setBackColorRGB(get88Color(9));
                    break;
                case 102:
                    // Set green background
                    currentState.attr.setBackColorRGB(get88Color(10));
                    break;
                case 103:
                    // Set yellow background
                    currentState.attr.setBackColorRGB(get88Color(11));
                    break;
                case 104:
                    // Set blue background
                    currentState.attr.setBackColorRGB(get88Color(12));
                    break;
                case 105:
                    // Set magenta background
                    currentState.attr.setBackColorRGB(get88Color(13));
                    break;
                case 106:
                    // Set cyan background
                    currentState.attr.setBackColorRGB(get88Color(14));
                    break;
                case 107:
                    // Set white background
                    currentState.attr.setBackColorRGB(get88Color(15));
                    break;

                default:
                    break;
                }
            }

            if ((type == DeviceType.VT220)
                || (type == DeviceType.XTERM)) {

                switch (i) {

                case 22:
                    // Normal intensity
                    currentState.attr.setBold(false);
                    break;

                case 24:
                    // No underline
                    currentState.attr.setUnderline(false);
                    break;

                case 25:
                    // No blink
                    currentState.attr.setBlink(false);
                    break;

                case 27:
                    // Un-reverse
                    currentState.attr.setReverse(false);
                    break;

                default:
                    break;
                }
            }

            // A true VT100/102/220 does not support color, however everyone
            // is used to their terminal emulator supporting color so we will
            // unconditionally support color for all DeviceType's.

            switch (i) {

            case 30:
                // Set black foreground
                currentState.attr.setForeColor(Color.BLACK);
                currentState.attr.setForeColorRGB(-1);
                break;
            case 31:
                // Set red foreground
                currentState.attr.setForeColor(Color.RED);
                currentState.attr.setForeColorRGB(-1);
                break;
            case 32:
                // Set green foreground
                currentState.attr.setForeColor(Color.GREEN);
                currentState.attr.setForeColorRGB(-1);
                break;
            case 33:
                // Set yellow foreground
                currentState.attr.setForeColor(Color.YELLOW);
                currentState.attr.setForeColorRGB(-1);
                break;
            case 34:
                // Set blue foreground
                currentState.attr.setForeColor(Color.BLUE);
                currentState.attr.setForeColorRGB(-1);
                break;
            case 35:
                // Set magenta foreground
                currentState.attr.setForeColor(Color.MAGENTA);
                currentState.attr.setForeColorRGB(-1);
                break;
            case 36:
                // Set cyan foreground
                currentState.attr.setForeColor(Color.CYAN);
                currentState.attr.setForeColorRGB(-1);
                break;
            case 37:
                // Set white foreground
                currentState.attr.setForeColor(Color.WHITE);
                currentState.attr.setForeColorRGB(-1);
                break;
            case 38:
                if (type == DeviceType.XTERM) {
                    /*
                     * Xterm supports T.416 / ISO-8613-3 codes to select
                     * either an indexed color or an RGB value.  (It also
                     * permits these ISO-8613-3 SGR sequences to be separated
                     * by colons rather than semicolons.)
                     *
                     * We will support only the following:
                     *
                     * 1. Indexed color mode (88- or 256-color modes).
                     *
                     * 2. Direct RGB.
                     *
                     * These cover most of the use cases in the real world.
                     *
                     * HOWEVER, note that this is an awful broken "standard",
                     * with no way to do it "right".  See
                     * http://invisible-island.net/ncurses/ncurses.faq.html#xterm_16MegaColors
                     * for a detailed discussion of the current state of RGB
                     * in various terminals, the point of which is that none
                     * of them really do the same thing despite all appearing
                     * to be "xterm".
                     *
                     * Also see
                     * https://bugs.kde.org/show_bug.cgi?id=107487#c3 .
                     * where it is assumed that supporting just the "indexed
                     * mode" of these sequences (which could align easily
                     * with existing SGR colors) is assumed to mean full
                     * support of 24-bit RGB.  So it is all or nothing.
                     *
                     * Finally, these sequences break the assumptions of
                     * standard ECMA-48 style parsers as pointed out at
                     * https://bugs.kde.org/show_bug.cgi?id=107487#c11 .
                     * Therefore in order to keep a clean display, we cannot
                     * parse anything else in this sequence.
                     */
                    sgrColorMode = 38;
                    continue;
                } else {
                    // Underscore on, default foreground color
                    currentState.attr.setUnderline(true);
                    currentState.attr.setForeColor(Color.WHITE);
                }
                break;
            case 39:
                // Underscore off, default foreground color
                currentState.attr.setUnderline(false);
                currentState.attr.setForeColor(Color.WHITE);
                currentState.attr.setForeColorRGB(-1);
                break;
            case 40:
                // Set black background
                currentState.attr.setBackColor(Color.BLACK);
                currentState.attr.setBackColorRGB(-1);
                break;
            case 41:
                // Set red background
                currentState.attr.setBackColor(Color.RED);
                currentState.attr.setBackColorRGB(-1);
                break;
            case 42:
                // Set green background
                currentState.attr.setBackColor(Color.GREEN);
                currentState.attr.setBackColorRGB(-1);
                break;
            case 43:
                // Set yellow background
                currentState.attr.setBackColor(Color.YELLOW);
                currentState.attr.setBackColorRGB(-1);
                break;
            case 44:
                // Set blue background
                currentState.attr.setBackColor(Color.BLUE);
                currentState.attr.setBackColorRGB(-1);
                break;
            case 45:
                // Set magenta background
                currentState.attr.setBackColor(Color.MAGENTA);
                currentState.attr.setBackColorRGB(-1);
                break;
            case 46:
                // Set cyan background
                currentState.attr.setBackColor(Color.CYAN);
                currentState.attr.setBackColorRGB(-1);
                break;
            case 47:
                // Set white background
                currentState.attr.setBackColor(Color.WHITE);
                currentState.attr.setBackColorRGB(-1);
                break;
            case 48:
                if (type == DeviceType.XTERM) {
                    /*
                     * Xterm supports T.416 / ISO-8613-3 codes to select
                     * either an indexed color or an RGB value.  (It also
                     * permits these ISO-8613-3 SGR sequences to be separated
                     * by colons rather than semicolons.)
                     *
                     * We will support only the following:
                     *
                     * 1. Indexed color mode (88- or 256-color modes).
                     *
                     * 2. Direct RGB.
                     *
                     * These cover most of the use cases in the real world.
                     *
                     * HOWEVER, note that this is an awful broken "standard",
                     * with no way to do it "right".  See
                     * http://invisible-island.net/ncurses/ncurses.faq.html#xterm_16MegaColors
                     * for a detailed discussion of the current state of RGB
                     * in various terminals, the point of which is that none
                     * of them really do the same thing despite all appearing
                     * to be "xterm".
                     *
                     * Also see
                     * https://bugs.kde.org/show_bug.cgi?id=107487#c3 .
                     * where it is assumed that supporting just the "indexed
                     * mode" of these sequences (which could align easily
                     * with existing SGR colors) is assumed to mean full
                     * support of 24-bit RGB.  So it is all or nothing.
                     *
                     * Finally, these sequences break the assumptions of
                     * standard ECMA-48 style parsers as pointed out at
                     * https://bugs.kde.org/show_bug.cgi?id=107487#c11 .
                     * Therefore in order to keep a clean display, we cannot
                     * parse anything else in this sequence.
                     */
                    sgrColorMode = 48;
                    continue;
                }
                break;
            case 49:
                // Default background
                currentState.attr.setBackColor(Color.BLACK);
                currentState.attr.setBackColorRGB(-1);
                break;

            default:
                break;
            }
        }
    }

    /**
     * DA - Device attributes.
     */
    private void da() {
        int extendedFlag = 0;
        int i = 0;
        if (collectBuffer.length() > 0) {
            String args = collectBuffer.substring(1);
            if (collectBuffer.charAt(0) == '>') {
                extendedFlag = 1;
                if (collectBuffer.length() >= 2) {
                    i = Integer.parseInt(args);
                }
            } else if (collectBuffer.charAt(0) == '=') {
                extendedFlag = 2;
                if (collectBuffer.length() >= 2) {
                    i = Integer.parseInt(args);
                }
            } else {
                // Unknown code, bail out
                return;
            }
        }

        if ((i != 0) && (i != 1)) {
            return;
        }

        if ((extendedFlag == 0) && (i == 0)) {
            // Send string directly to remote side
            writeRemote(deviceTypeResponse());
            return;
        }

        if ((type == DeviceType.VT220) || (type == DeviceType.XTERM)) {

            if ((extendedFlag == 1) && (i == 0)) {
                /*
                 * Request "What type of terminal are you, what is your
                 * firmware version, and what hardware options do you have
                 * installed?"
                 *
                 * Respond: "I am a VT220 (identification code of 1), my
                 * firmware version is _____ (Pv), and I have _____ Po
                 * options installed."
                 *
                 * (Same as xterm)
                 *
                 */

                if (s8c1t == true) {
                    writeRemote("\u009b>1;10;0c");
                } else {
                    writeRemote("\033[>1;10;0c");
                }
            }
        }

        // VT420 and up
        if ((extendedFlag == 2) && (i == 0)) {

            /*
             * Request "What is your unit ID?"
             *
             * Respond: "I was manufactured at site 00 and have a unique ID
             * number of 123."
             *
             */
            writeRemote("\033P!|00010203\033\\");
        }
    }

    /**
     * DECSTBM - Set top and bottom margins.
     */
    private void decstbm() {
        boolean decPrivateModeFlag = false;

        for (int i = 0; i < collectBuffer.length(); i++) {
            if (collectBuffer.charAt(i) == '?') {
                decPrivateModeFlag = true;
                break;
            }
        }
        if (decPrivateModeFlag) {
            // This could be restore DEC private mode values.
            // Ignore it.
        } else {
            // DECSTBM
            int top = getCsiParam(0, 1, 1, height) - 1;
            int bottom = getCsiParam(1, height, 1, height) - 1;
            if (bottom > height - 1) {
                bottom = height - 1;
            }

            if (top > bottom) {
                top = bottom;
            }
            scrollRegionTop = top;
            scrollRegionBottom = bottom;

            // Home cursor
            cursorPosition(0, 0);
        }
    }

    /**
     * DECREQTPARM - Request terminal parameters.
     */
    private void decreqtparm() {
        int i = getCsiParam(0, 0);

        if ((i != 0) && (i != 1)) {
                return;
        }

        String str = "";

        /*
         * Request terminal parameters.
         *
         * Respond with:
         *
         *     Parity NONE, 8 bits, xmitspeed 38400, recvspeed 38400.
         *     (CLoCk MULtiplier = 1, STP option flags = 0)
         *
         * (Same as xterm)
         */
        if (((type == DeviceType.VT220) || (type == DeviceType.XTERM))
            && (s8c1t == true)
        ) {
            str = String.format("\u009b%d;1;1;128;128;1;0x", i + 2);
        } else {
            str = String.format("\033[%d;1;1;128;128;1;0x", i + 2);
        }
        writeRemote(str);
    }

    /**
     * DECSCA - Select Character Attributes.
     */
    private void decsca() {
        int i = getCsiParam(0, 0);

        if ((i == 0) || (i == 2)) {
            // Protect mode OFF
            currentState.attr.setProtect(false);
        }
        if (i == 1) {
            // Protect mode ON
            currentState.attr.setProtect(true);
        }
    }

    /**
     * DECSTR - Soft Terminal Reset.
     */
    private void decstr() {
        // Do exactly like RIS - Reset to initial state
        reset();
        // Do I clear screen too? I think so...
        eraseScreen(0, 0, height - 1, width - 1, false);
        cursorPosition(0, 0);
    }

    /**
     * DSR - Device status report.
     */
    private void dsr() {
        boolean decPrivateModeFlag = false;
        int row = currentState.cursorY;

        for (int i = 0; i < collectBuffer.length(); i++) {
            if (collectBuffer.charAt(i) == '?') {
                decPrivateModeFlag = true;
                break;
            }
        }

        int i = getCsiParam(0, 0);

        switch (i) {

        case 5:
            // Request status report. Respond with "OK, no malfunction."

            // Send string directly to remote side
            if (((type == DeviceType.VT220) || (type == DeviceType.XTERM))
                && (s8c1t == true)
            ) {
                writeRemote("\u009b0n");
            } else {
                writeRemote("\033[0n");
            }
            break;

        case 6:
            // Request cursor position.  Respond with current position.
            if (currentState.originMode == true) {
                row -= scrollRegionTop;
            }
            String str = "";
            if (((type == DeviceType.VT220) || (type == DeviceType.XTERM))
                && (s8c1t == true)
            ) {
                str = String.format("\u009b%d;%dR", row + 1,
                    currentState.cursorX + 1);
            } else {
                str = String.format("\033[%d;%dR", row + 1,
                    currentState.cursorX + 1);
            }

            // Send string directly to remote side
            writeRemote(str);
            break;

        case 15:
            if (decPrivateModeFlag == true) {

                // Request printer status report.  Respond with "Printer not
                // connected."

                if (((type == DeviceType.VT220) || (type == DeviceType.XTERM))
                    && (s8c1t == true)) {
                    writeRemote("\u009b?13n");
                } else {
                    writeRemote("\033[?13n");
                }
            }
            break;

        case 25:
            if (((type == DeviceType.VT220) || (type == DeviceType.XTERM))
                && (decPrivateModeFlag == true)
            ) {

                // Request user-defined keys are locked or unlocked.  Respond
                // with "User-defined keys are locked."

                if (s8c1t == true) {
                    writeRemote("\u009b?21n");
                } else {
                    writeRemote("\033[?21n");
                }
            }
            break;

        case 26:
            if (((type == DeviceType.VT220) || (type == DeviceType.XTERM))
                && (decPrivateModeFlag == true)
            ) {

                // Request keyboard language.  Respond with "Keyboard
                // language is North American."

                if (s8c1t == true) {
                    writeRemote("\u009b?27;1n");
                } else {
                    writeRemote("\033[?27;1n");
                }

            }
            break;

        default:
            // Some other option, ignore
            break;
        }
    }

    /**
     * TBC - Tabulation clear.
     */
    private void tbc() {
        int i = getCsiParam(0, 0);
        if (i == 0) {
            List<Integer> newStops = new ArrayList<Integer>();
            for (Integer stop: tabStops) {
                if (stop == currentState.cursorX) {
                    continue;
                }
                newStops.add(stop);
            }
            tabStops = newStops;
        }
        if (i == 3) {
            tabStops.clear();
        }
    }

    /**
     * Erase the characters in the current line from the start column to the
     * end column, inclusive.
     *
     * @param start starting column to erase (between 0 and width - 1)
     * @param end ending column to erase (between 0 and width - 1)
     * @param honorProtected if true, do not erase characters with the
     * protected attribute set
     */
    private void eraseLine(int start, int end, final boolean honorProtected) {

        if (start > end) {
            return;
        }
        if (end > width - 1) {
            end = width - 1;
        }
        if (start < 0) {
            start = 0;
        }

        for (int i = start; i <= end; i++) {
            DisplayLine line = display.get(currentState.cursorY);
            if ((!honorProtected)
                || ((honorProtected) && (!line.charAt(i).isProtect()))) {

                switch (type) {
                case VT100:
                case VT102:
                case VT220:
                    /*
                     * From the VT102 manual:
                     *
                     * Erasing a character also erases any character
                     * attribute of the character.
                     */
                    line.setBlank(i);
                    break;
                case XTERM:
                    /*
                     * Erase with the current color a.k.a. back-color erase
                     * (bce).
                     */
                    line.setChar(i, ' ');
                    line.setAttr(i, currentState.attr);
                    break;
                }
            }
        }
    }

    /**
     * Erase a rectangular section of the screen, inclusive.  end column,
     * inclusive.
     *
     * @param startRow starting row to erase (between 0 and height - 1)
     * @param startCol starting column to erase (between 0 and width - 1)
     * @param endRow ending row to erase (between 0 and height - 1)
     * @param endCol ending column to erase (between 0 and width - 1)
     * @param honorProtected if true, do not erase characters with the
     * protected attribute set
     */
    private void eraseScreen(final int startRow, final int startCol,
        final int endRow, final int endCol, final boolean honorProtected) {

        int oldCursorY;

        if ((startRow < 0)
            || (startCol < 0)
            || (endRow < 0)
            || (endCol < 0)
            || (endRow < startRow)
            || (endCol < startCol)
        ) {
            return;
        }

        oldCursorY = currentState.cursorY;
        for (int i = startRow; i <= endRow; i++) {
            currentState.cursorY = i;
            eraseLine(startCol, endCol, honorProtected);

            // Erase display clears the double attributes
            display.get(i).setDoubleWidth(false);
            display.get(i).setDoubleHeight(0);
        }
        currentState.cursorY = oldCursorY;
    }

    /**
     * VT220 printer functions.  All of these are parsed, but won't do
     * anything.
     */
    private void printerFunctions() {
        boolean decPrivateModeFlag = false;
        for (int i = 0; i < collectBuffer.length(); i++) {
            if (collectBuffer.charAt(i) == '?') {
                decPrivateModeFlag = true;
                break;
            }
        }

        int i = getCsiParam(0, 0);

        switch (i) {

        case 0:
            if (decPrivateModeFlag == false) {
                // Print screen
            }
            break;

        case 1:
            if (decPrivateModeFlag == true) {
                // Print cursor line
            }
            break;

        case 4:
            if (decPrivateModeFlag == true) {
                // Auto print mode OFF
            } else {
                // Printer controller OFF

                // Characters re-appear on the screen
                printerControllerMode = false;
            }
            break;

        case 5:
            if (decPrivateModeFlag == true) {
                // Auto print mode

            } else {
                // Printer controller

                // Characters get sucked into oblivion
                printerControllerMode = true;
            }
            break;

        default:
            break;

        }
    }

    /**
     * Handle the SCAN_OSC_STRING state.  Handle this in VT100 because lots
     * of remote systems will send an XTerm title sequence even if TERM isn't
     * xterm.
     *
     * @param xtermChar the character received from the remote side
     */
    private void oscPut(final char xtermChar) {
        // System.err.println("oscPut: " + xtermChar);

        boolean oscEnd = false;

        if (xtermChar == 0x07) {
            oscEnd = true;
        }
        if ((xtermChar == '\\')
            && (collectBuffer.charAt(collectBuffer.length() - 1) == '\033')
        ) {
            oscEnd = true;
        }

        // Collect first
        collectBuffer.append(xtermChar);

        // Xterm cases...
        if (oscEnd) {
            String args = null;
            if (xtermChar == 0x07) {
                args = collectBuffer.substring(0, collectBuffer.length() - 1);
            } else {
                args = collectBuffer.substring(0, collectBuffer.length() - 2);
            }

            String [] p = args.split(";");
            if (p.length > 0) {
                if ((p[0].equals("0")) || (p[0].equals("2"))) {
                    if (p.length > 1) {
                        // Screen title
                        screenTitle = p[1];
                    }
                }

                if (p[0].equals("4")) {
                    for (int i = 1; i + 1 < p.length; i += 2) {
                        // Set a color index value
                        try {
                            set88Color(Integer.parseInt(p[i]), p[i + 1]);
                        } catch (NumberFormatException e) {
                            // SQUASH
                        }
                    }
                }

                if (p[0].equals("10")) {
                    if (p[1].equals("?")) {
                        // Respond with foreground color.
                        java.awt.Color color = jexer.backend.SwingTerminal.attrToForegroundColor(currentState.attr);

                        writeRemote(String.format(
                            "\033]10;rgb:%04x/%04x/%04x\033\\",
                                color.getRed() << 8,
                                color.getGreen() << 8,
                                color.getBlue() << 8));
                    }
                }

                if (p[0].equals("11")) {
                    if (p[1].equals("?")) {
                        // Respond with background color.
                        java.awt.Color color = jexer.backend.SwingTerminal.attrToBackgroundColor(currentState.attr);

                        writeRemote(String.format(
                            "\033]11;rgb:%04x/%04x/%04x\033\\",
                                color.getRed() << 8,
                                color.getGreen() << 8,
                                color.getBlue() << 8));
                    }
                }

                if (p[0].equals("444")) {
                    if (p[1].equals("0") && (p.length == 6)) {
                        // Jexer image - RGB
                        parseJexerImageRGB(p[2], p[3], p[4], p[5]);
                    } else if (p[1].equals("1") && (p.length == 4)) {
                        // Jexer image - PNG
                        parseJexerImageFile(1, p[2], p[3]);
                    } else if (p[1].equals("2") && (p.length == 4)) {
                        // Jexer image - JPG
                        parseJexerImageFile(2, p[2], p[3]);
                    }
                }
            }

            // Go to SCAN_GROUND state
            toGround();
            return;
        }
    }

    /**
     * Handle the SCAN_SOSPMAPC_STRING state.  This is currently only used by
     * Jexer ECMA48Terminal to talk to ECMA48.
     *
     * @param pmChar the character received from the remote side
     */
    private void pmPut(final char pmChar) {
        // System.err.println("pmPut: " + pmChar);

        boolean pmEnd = false;

        if ((pmChar == '\\')
            && (collectBuffer.charAt(collectBuffer.length() - 1) == '\033')
        ) {
            pmEnd = true;
        }

        // Collect first
        collectBuffer.append(pmChar);

        // Xterm cases...
        if (pmEnd) {
            String arg = null;
            arg = collectBuffer.substring(0, collectBuffer.length() - 2);

            // System.err.println("arg: '" + arg + "'");

            if (arg.equals("hideMousePointer")) {
                hideMousePointer = true;
            }
            if (arg.equals("showMousePointer")) {
                hideMousePointer = false;
            }

            // Go to SCAN_GROUND state
            toGround();
            return;
        }
    }

    /**
     * Perform xterm window operations.
     */
    private void xtermWindowOps() {
        boolean xtermPrivateModeFlag = false;

        for (int i = 0; i < collectBuffer.length(); i++) {
            if (collectBuffer.charAt(i) == '?') {
                xtermPrivateModeFlag = true;
                break;
            }
        }

        int i = getCsiParam(0, 0);

        if (!xtermPrivateModeFlag) {
            switch (i) {
            case 14:
                // Report xterm text area size in pixels as CSI 4 ; height ;
                // width t
                writeRemote(String.format("\033[4;%d;%dt", textHeight * height,
                        textWidth * width));
                break;
            case 16:
                // Report character size in pixels as CSI 6 ; height ; width
                // t
                writeRemote(String.format("\033[6;%d;%dt", textHeight,
                        textWidth));
                break;
            case 18:
                // Report the text are size in characters as CSI 8 ; height ;
                // width t
                writeRemote(String.format("\033[8;%d;%dt", height, width));
                break;
            default:
                break;
            }
        }
    }

    /**
     * Respond to xterm sixel query.
     */
    private void xtermSixelQuery() {
        int item = getCsiParam(0, 0);
        int action = getCsiParam(1, 0);
        int value = getCsiParam(2, 0);

        switch (item) {
        case 1:
            if (action == 1) {
                // Report number of color registers.
                writeRemote(String.format("\033[?%d;%d;%dS", item, 0, 1024));
                return;
            }
            break;
        default:
            break;
        }
        // We will not support this option.
        writeRemote(String.format("\033[?%d;%dS", item, action));
    }

    /**
     * Run this input character through the ECMA48 state machine.
     *
     * @param ch character from the remote side
     */
    private void consume(final int ch) {
        readCount++;

        // DEBUG
        // System.err.printf("%c STATE = %s\n", ch, scanState);

        // Special "anywhere" states

        // 18, 1A                     --> execute, then switch to SCAN_GROUND
        if ((ch == 0x18) || (ch == 0x1A)) {
            // CAN and SUB abort escape sequences
            toGround();
            return;
        }

        // 80-8F, 91-97, 99, 9A, 9C   --> execute, then switch to SCAN_GROUND

        // 0x1B == ESCAPE
        if (ch == 0x1B) {
            if ((type == DeviceType.XTERM)
                && ((scanState == ScanState.OSC_STRING)
                    || (scanState == ScanState.DCS_SIXEL)
                    || (scanState == ScanState.SOSPMAPC_STRING))
            ) {
                // Xterm can pass ESCAPE to its OSC sequence.
                // Xterm can pass ESCAPE to its DCS sequence.
                // Jexer can pass ESCAPE to its PM sequence.
            } else if ((scanState != ScanState.DCS_ENTRY)
                && (scanState != ScanState.DCS_INTERMEDIATE)
                && (scanState != ScanState.DCS_IGNORE)
                && (scanState != ScanState.DCS_PARAM)
                && (scanState != ScanState.DCS_PASSTHROUGH)
            ) {
                scanState = ScanState.ESCAPE;
                return;
            }
        }

        // 0x9B == CSI 8-bit sequence
        if (ch == 0x9B) {
            scanState = ScanState.CSI_ENTRY;
            return;
        }

        // 0x9D goes to ScanState.OSC_STRING
        if (ch == 0x9D) {
            scanState = ScanState.OSC_STRING;
            return;
        }

        // 0x90 goes to DCS_ENTRY
        if (ch == 0x90) {
            scanState = ScanState.DCS_ENTRY;
            return;
        }

        // 0x98, 0x9E, and 0x9F go to SOSPMAPC_STRING
        if ((ch == 0x98) || (ch == 0x9E) || (ch == 0x9F)) {
            scanState = ScanState.SOSPMAPC_STRING;
            return;
        }

        // 0x7F (DEL) is always discarded
        if (ch == 0x7F) {
            return;
        }

        switch (scanState) {

        case GROUND:
            // 00-17, 19, 1C-1F --> execute
            // 80-8F, 91-9A, 9C --> execute
            if ((ch <= 0x1F) || ((ch >= 0x80) && (ch <= 0x9F))) {
                handleControlChar((char) ch);
            }

            // 20-7F            --> print
            if (((ch >= 0x20) && (ch <= 0x7F))
                || (ch >= 0xA0)
            ) {

                // VT220 printer --> trash bin
                if (((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM))
                    && (printerControllerMode == true)
                ) {
                    return;
                }

                // Hang onto this character
                repCh = mapCharacter(ch);

                // Print this character
                printCharacter(repCh);
            }
            return;

        case ESCAPE:
            // 00-17, 19, 1C-1F --> execute
            if (ch <= 0x1F) {
                handleControlChar((char) ch);
                return;
            }

            // 20-2F            --> collect, then switch to ESCAPE_INTERMEDIATE
            if ((ch >= 0x20) && (ch <= 0x2F)) {
                collect((char) ch);
                scanState = ScanState.ESCAPE_INTERMEDIATE;
                return;
            }

            // 30-4F, 51-57, 59, 5A, 5C, 60-7E --> dispatch, then switch to GROUND
            if ((ch >= 0x30) && (ch <= 0x4F)) {
                switch (ch) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                    break;
                case '7':
                    // DECSC - Save cursor
                    // Note this code overlaps both ANSI and VT52 mode
                    decsc();
                    break;

                case '8':
                    // DECRC - Restore cursor
                    // Note this code overlaps both ANSI and VT52 mode
                    decrc();
                    break;

                case '9':
                case ':':
                case ';':
                    break;
                case '<':
                    if (vt52Mode == true) {
                        // DECANM - Enter ANSI mode
                        vt52Mode = false;
                        arrowKeyMode = ArrowKeyMode.VT100;

                        /*
                         * From the VT102 docs: "You use ANSI mode to select
                         * most terminal features; the terminal uses the same
                         * features when it switches to VT52 mode. You
                         * cannot, however, change most of these features in
                         * VT52 mode."
                         *
                         * In other words, do not reset any other attributes
                         * when switching between VT52 submode and ANSI.
                         */

                        // Reset fonts
                        currentState.g0Charset = CharacterSet.US;
                        currentState.g1Charset = CharacterSet.DRAWING;
                        s8c1t = false;
                        singleshift = Singleshift.NONE;
                        currentState.glLockshift = LockshiftMode.NONE;
                        currentState.grLockshift = LockshiftMode.NONE;
                    }
                    break;
                case '=':
                    // DECKPAM - Keypad application mode
                    // Note this code overlaps both ANSI and VT52 mode
                    deckpam();
                    break;
                case '>':
                    // DECKPNM - Keypad numeric mode
                    // Note this code overlaps both ANSI and VT52 mode
                    deckpnm();
                    break;
                case '?':
                case '@':
                    break;
                case 'A':
                    if (vt52Mode == true) {
                        // Cursor up, and stop at the top without scrolling
                        cursorUp(1, false);
                    }
                    break;
                case 'B':
                    if (vt52Mode == true) {
                        // Cursor down, and stop at the bottom without scrolling
                        cursorDown(1, false);
                    }
                    break;
                case 'C':
                    if (vt52Mode == true) {
                        // Cursor right, and stop at the right without scrolling
                        cursorRight(1, false);
                    }
                    break;
                case 'D':
                    if (vt52Mode == true) {
                        // Cursor left, and stop at the left without scrolling
                        cursorLeft(1, false);
                    } else {
                        // IND - Index
                        ind();
                    }
                    break;
                case 'E':
                    if (vt52Mode == true) {
                        // Nothing
                    } else {
                        // NEL - Next line
                        nel();
                    }
                    break;
                case 'F':
                    if (vt52Mode == true) {
                        // G0 --> Special graphics
                        currentState.g0Charset = CharacterSet.VT52_GRAPHICS;
                    }
                    break;
                case 'G':
                    if (vt52Mode == true) {
                        // G0 --> ASCII set
                        currentState.g0Charset = CharacterSet.US;
                    }
                    break;
                case 'H':
                    if (vt52Mode == true) {
                        // Cursor to home
                        cursorPosition(0, 0);
                    } else {
                        // HTS - Horizontal tabulation set
                        hts();
                    }
                    break;
                case 'I':
                    if (vt52Mode == true) {
                        // Reverse line feed.  Same as RI.
                        ri();
                    }
                    break;
                case 'J':
                    if (vt52Mode == true) {
                        // Erase to end of screen
                        eraseLine(currentState.cursorX, width - 1, false);
                        eraseScreen(currentState.cursorY + 1, 0, height - 1,
                            width - 1, false);
                    }
                    break;
                case 'K':
                    if (vt52Mode == true) {
                        // Erase to end of line
                        eraseLine(currentState.cursorX, width - 1, false);
                    }
                    break;
                case 'L':
                    break;
                case 'M':
                    if (vt52Mode == true) {
                        // Nothing
                    } else {
                        // RI - Reverse index
                        ri();
                    }
                    break;
                case 'N':
                    if (vt52Mode == false) {
                        // SS2
                        singleshift = Singleshift.SS2;
                    }
                    break;
                case 'O':
                    if (vt52Mode == false) {
                        // SS3
                        singleshift = Singleshift.SS3;
                    }
                    break;
                }
                toGround();
                return;
            }
            if ((ch >= 0x51) && (ch <= 0x57)) {
                switch (ch) {
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                    break;
                }
                toGround();
                return;
            }
            if (ch == 0x59) {
                // 'Y'
                if (vt52Mode == true) {
                    scanState = ScanState.VT52_DIRECT_CURSOR_ADDRESS;
                } else {
                    toGround();
                }
                return;
            }
            if (ch == 0x5A) {
                // 'Z'
                if (vt52Mode == true) {
                    // Identify
                    // Send string directly to remote side
                    writeRemote("\033/Z");
                } else {
                    // DECID
                    // Send string directly to remote side
                    writeRemote(deviceTypeResponse());
                }
                toGround();
                return;
            }
            if (ch == 0x5C) {
                // '\'
                toGround();
                return;
            }

            // VT52 cannot get to any of these other states
            if (vt52Mode == true) {
                toGround();
                return;
            }

            if ((ch >= 0x60) && (ch <= 0x7E)) {
                switch (ch) {
                case '`':
                case 'a':
                case 'b':
                    break;
                case 'c':
                    // RIS - Reset to initial state
                    reset();
                    // Do I clear screen too? I think so...
                    eraseScreen(0, 0, height - 1, width - 1, false);
                    cursorPosition(0, 0);
                    break;
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                    break;
                case 'n':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        // VT220 lockshift G2 into GL
                        currentState.glLockshift = LockshiftMode.G2_GL;
                        shiftOut = false;
                    }
                    break;
                case 'o':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        // VT220 lockshift G3 into GL
                        currentState.glLockshift = LockshiftMode.G3_GL;
                        shiftOut = false;
                    }
                    break;
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                case '{':
                    break;
                case '|':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        // VT220 lockshift G3 into GR
                        currentState.grLockshift = LockshiftMode.G3_GR;
                        shiftOut = false;
                    }
                    break;
                case '}':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        // VT220 lockshift G2 into GR
                        currentState.grLockshift = LockshiftMode.G2_GR;
                        shiftOut = false;
                    }
                    break;

                case '~':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        // VT220 lockshift G1 into GR
                        currentState.grLockshift = LockshiftMode.G1_GR;
                        shiftOut = false;
                    }
                    break;
                }
                toGround();
            }

            // 7F               --> ignore

            // 0x5B goes to CSI_ENTRY
            if (ch == 0x5B) {
                scanState = ScanState.CSI_ENTRY;
            }

            // 0x5D goes to OSC_STRING
            if (ch == 0x5D) {
                scanState = ScanState.OSC_STRING;
            }

            // 0x50 goes to DCS_ENTRY
            if (ch == 0x50) {
                scanState = ScanState.DCS_ENTRY;
            }

            // 0x58, 0x5E, and 0x5F go to SOSPMAPC_STRING
            if ((ch == 0x58) || (ch == 0x5E) || (ch == 0x5F)) {
                scanState = ScanState.SOSPMAPC_STRING;
            }

            return;

        case ESCAPE_INTERMEDIATE:
            // 00-17, 19, 1C-1F    --> execute
            if (ch <= 0x1F) {
                handleControlChar((char) ch);
            }

            // 20-2F               --> collect
            if ((ch >= 0x20) && (ch <= 0x2F)) {
                collect((char) ch);
            }

            // 30-7E               --> dispatch, then switch to GROUND
            if ((ch >= 0x30) && (ch <= 0x7E)) {
                switch (ch) {
                case '0':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '(')) {
                        // G0 --> Special graphics
                        currentState.g0Charset = CharacterSet.DRAWING;
                    }
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == ')')) {
                        // G1 --> Special graphics
                        currentState.g1Charset = CharacterSet.DRAWING;
                    }
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> Special graphics
                            currentState.g2Charset = CharacterSet.DRAWING;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> Special graphics
                            currentState.g3Charset = CharacterSet.DRAWING;
                        }
                    }
                    break;
                case '1':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '(')) {
                        // G0 --> Alternate character ROM standard character set
                        currentState.g0Charset = CharacterSet.ROM;
                    }
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == ')')) {
                        // G1 --> Alternate character ROM standard character set
                        currentState.g1Charset = CharacterSet.ROM;
                    }
                    break;
                case '2':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '(')) {
                        // G0 --> Alternate character ROM special graphics
                        currentState.g0Charset = CharacterSet.ROM_SPECIAL;
                    }
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == ')')) {
                        // G1 --> Alternate character ROM special graphics
                        currentState.g1Charset = CharacterSet.ROM_SPECIAL;
                    }
                    break;
                case '3':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '#')) {
                        // DECDHL - Double-height line (top half)
                        dechdl(true);
                    }
                    break;
                case '4':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '#')) {
                        // DECDHL - Double-height line (bottom half)
                        dechdl(false);
                    }
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> DUTCH
                            currentState.g0Charset = CharacterSet.NRC_DUTCH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> DUTCH
                            currentState.g1Charset = CharacterSet.NRC_DUTCH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> DUTCH
                            currentState.g2Charset = CharacterSet.NRC_DUTCH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> DUTCH
                            currentState.g3Charset = CharacterSet.NRC_DUTCH;
                        }
                    }
                    break;
                case '5':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '#')) {
                        // DECSWL - Single-width line
                        decswl();
                    }
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> FINNISH
                            currentState.g0Charset = CharacterSet.NRC_FINNISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> FINNISH
                            currentState.g1Charset = CharacterSet.NRC_FINNISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> FINNISH
                            currentState.g2Charset = CharacterSet.NRC_FINNISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> FINNISH
                            currentState.g3Charset = CharacterSet.NRC_FINNISH;
                        }
                    }
                    break;
                case '6':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '#')) {
                        // DECDWL - Double-width line
                        decdwl();
                    }
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> NORWEGIAN
                            currentState.g0Charset = CharacterSet.NRC_NORWEGIAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> NORWEGIAN
                            currentState.g1Charset = CharacterSet.NRC_NORWEGIAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> NORWEGIAN
                            currentState.g2Charset = CharacterSet.NRC_NORWEGIAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> NORWEGIAN
                            currentState.g3Charset = CharacterSet.NRC_NORWEGIAN;
                        }
                    }
                    break;
                case '7':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> SWEDISH
                            currentState.g0Charset = CharacterSet.NRC_SWEDISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> SWEDISH
                            currentState.g1Charset = CharacterSet.NRC_SWEDISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> SWEDISH
                            currentState.g2Charset = CharacterSet.NRC_SWEDISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> SWEDISH
                            currentState.g3Charset = CharacterSet.NRC_SWEDISH;
                        }
                    }
                    break;
                case '8':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '#')) {
                        // DECALN - Screen alignment display
                        decaln();
                    }
                    break;
                case '9':
                case ':':
                case ';':
                    break;
                case '<':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> DEC_SUPPLEMENTAL
                            currentState.g0Charset = CharacterSet.DEC_SUPPLEMENTAL;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> DEC_SUPPLEMENTAL
                            currentState.g1Charset = CharacterSet.DEC_SUPPLEMENTAL;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> DEC_SUPPLEMENTAL
                            currentState.g2Charset = CharacterSet.DEC_SUPPLEMENTAL;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> DEC_SUPPLEMENTAL
                            currentState.g3Charset = CharacterSet.DEC_SUPPLEMENTAL;
                        }
                    }
                    break;
                case '=':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> SWISS
                            currentState.g0Charset = CharacterSet.NRC_SWISS;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> SWISS
                            currentState.g1Charset = CharacterSet.NRC_SWISS;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> SWISS
                            currentState.g2Charset = CharacterSet.NRC_SWISS;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> SWISS
                            currentState.g3Charset = CharacterSet.NRC_SWISS;
                        }
                    }
                    break;
                case '>':
                case '?':
                case '@':
                    break;
                case 'A':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '(')) {
                        // G0 --> United Kingdom set
                        currentState.g0Charset = CharacterSet.UK;
                    }
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == ')')) {
                        // G1 --> United Kingdom set
                        currentState.g1Charset = CharacterSet.UK;
                    }
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> United Kingdom set
                            currentState.g2Charset = CharacterSet.UK;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> United Kingdom set
                            currentState.g3Charset = CharacterSet.UK;
                        }
                    }
                    break;
                case 'B':
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == '(')) {
                        // G0 --> ASCII set
                        currentState.g0Charset = CharacterSet.US;
                    }
                    if ((collectBuffer.length() == 1)
                        && (collectBuffer.charAt(0) == ')')) {
                        // G1 --> ASCII set
                        currentState.g1Charset = CharacterSet.US;
                    }
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> ASCII
                            currentState.g2Charset = CharacterSet.US;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> ASCII
                            currentState.g3Charset = CharacterSet.US;
                        }
                    }
                    break;
                case 'C':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> FINNISH
                            currentState.g0Charset = CharacterSet.NRC_FINNISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> FINNISH
                            currentState.g1Charset = CharacterSet.NRC_FINNISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> FINNISH
                            currentState.g2Charset = CharacterSet.NRC_FINNISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> FINNISH
                            currentState.g3Charset = CharacterSet.NRC_FINNISH;
                        }
                    }
                    break;
                case 'D':
                    break;
                case 'E':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> NORWEGIAN
                            currentState.g0Charset = CharacterSet.NRC_NORWEGIAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> NORWEGIAN
                            currentState.g1Charset = CharacterSet.NRC_NORWEGIAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> NORWEGIAN
                            currentState.g2Charset = CharacterSet.NRC_NORWEGIAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> NORWEGIAN
                            currentState.g3Charset = CharacterSet.NRC_NORWEGIAN;
                        }
                    }
                    break;
                case 'F':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ' ')) {
                            // S7C1T
                            s8c1t = false;
                        }
                    }
                    break;
                case 'G':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ' ')) {
                            // S8C1T
                            s8c1t = true;
                        }
                    }
                    break;
                case 'H':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> SWEDISH
                            currentState.g0Charset = CharacterSet.NRC_SWEDISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> SWEDISH
                            currentState.g1Charset = CharacterSet.NRC_SWEDISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> SWEDISH
                            currentState.g2Charset = CharacterSet.NRC_SWEDISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> SWEDISH
                            currentState.g3Charset = CharacterSet.NRC_SWEDISH;
                        }
                    }
                    break;
                case 'I':
                case 'J':
                    break;
                case 'K':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> GERMAN
                            currentState.g0Charset = CharacterSet.NRC_GERMAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> GERMAN
                            currentState.g1Charset = CharacterSet.NRC_GERMAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> GERMAN
                            currentState.g2Charset = CharacterSet.NRC_GERMAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> GERMAN
                            currentState.g3Charset = CharacterSet.NRC_GERMAN;
                        }
                    }
                    break;
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                    break;
                case 'Q':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> FRENCH_CA
                            currentState.g0Charset = CharacterSet.NRC_FRENCH_CA;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> FRENCH_CA
                            currentState.g1Charset = CharacterSet.NRC_FRENCH_CA;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> FRENCH_CA
                            currentState.g2Charset = CharacterSet.NRC_FRENCH_CA;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> FRENCH_CA
                            currentState.g3Charset = CharacterSet.NRC_FRENCH_CA;
                        }
                    }
                    break;
                case 'R':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> FRENCH
                            currentState.g0Charset = CharacterSet.NRC_FRENCH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> FRENCH
                            currentState.g1Charset = CharacterSet.NRC_FRENCH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> FRENCH
                            currentState.g2Charset = CharacterSet.NRC_FRENCH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> FRENCH
                            currentState.g3Charset = CharacterSet.NRC_FRENCH;
                        }
                    }
                    break;
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                    break;
                case 'Y':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> ITALIAN
                            currentState.g0Charset = CharacterSet.NRC_ITALIAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> ITALIAN
                            currentState.g1Charset = CharacterSet.NRC_ITALIAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> ITALIAN
                            currentState.g2Charset = CharacterSet.NRC_ITALIAN;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> ITALIAN
                            currentState.g3Charset = CharacterSet.NRC_ITALIAN;
                        }
                    }
                    break;
                case 'Z':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '(')) {
                            // G0 --> SPANISH
                            currentState.g0Charset = CharacterSet.NRC_SPANISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == ')')) {
                            // G1 --> SPANISH
                            currentState.g1Charset = CharacterSet.NRC_SPANISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '*')) {
                            // G2 --> SPANISH
                            currentState.g2Charset = CharacterSet.NRC_SPANISH;
                        }
                        if ((collectBuffer.length() == 1)
                            && (collectBuffer.charAt(0) == '+')) {
                            // G3 --> SPANISH
                            currentState.g3Charset = CharacterSet.NRC_SPANISH;
                        }
                    }
                    break;
                case '[':
                case '\\':
                case ']':
                case '^':
                case '_':
                case '`':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                case '{':
                case '|':
                case '}':
                case '~':
                    break;
                }
                toGround();
            }

            // 7F                  --> ignore

            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                toGround();
            }

            return;

        case CSI_ENTRY:
            // 00-17, 19, 1C-1F    --> execute
            if (ch <= 0x1F) {
                handleControlChar((char) ch);
            }

            // 20-2F               --> collect, then switch to CSI_INTERMEDIATE
            if ((ch >= 0x20) && (ch <= 0x2F)) {
                collect((char) ch);
                scanState = ScanState.CSI_INTERMEDIATE;
            }

            // 30-39, 3B           --> param, then switch to CSI_PARAM
            if ((ch >= '0') && (ch <= '9')) {
                param((byte) ch);
                scanState = ScanState.CSI_PARAM;
            }
            if (ch == ';') {
                param((byte) ch);
                scanState = ScanState.CSI_PARAM;
            }

            // 3C-3F               --> collect, then switch to CSI_PARAM
            if ((ch >= 0x3C) && (ch <= 0x3F)) {
                collect((char) ch);
                scanState = ScanState.CSI_PARAM;
            }

            // 40-7E               --> dispatch, then switch to GROUND
            if ((ch >= 0x40) && (ch <= 0x7E)) {
                switch (ch) {
                case '@':
                    // ICH - Insert character
                    ich();
                    break;
                case 'A':
                    // CUU - Cursor up
                    cuu();
                    break;
                case 'B':
                    // CUD - Cursor down
                    cud();
                    break;
                case 'C':
                    // CUF - Cursor forward
                    cuf();
                    break;
                case 'D':
                    // CUB - Cursor backward
                    cub();
                    break;
                case 'E':
                    // CNL - Cursor down and to column 1
                    if (type == DeviceType.XTERM) {
                        cnl();
                    }
                    break;
                case 'F':
                    // CPL - Cursor up and to column 1
                    if (type == DeviceType.XTERM) {
                        cpl();
                    }
                    break;
                case 'G':
                    // CHA - Cursor to column # in current row
                    if (type == DeviceType.XTERM) {
                        cha();
                    }
                    break;
                case 'H':
                    // CUP - Cursor position
                    cup();
                    break;
                case 'I':
                    // CHT - Cursor forward X tab stops (default 1)
                    if (type == DeviceType.XTERM) {
                        cht();
                    }
                    break;
                case 'J':
                    // ED - Erase in display
                    ed();
                    break;
                case 'K':
                    // EL - Erase in line
                    el();
                    break;
                case 'L':
                    // IL - Insert line
                    il();
                    break;
                case 'M':
                    // DL - Delete line
                    dl();
                    break;
                case 'N':
                case 'O':
                    break;
                case 'P':
                    // DCH - Delete character
                    dch();
                    break;
                case 'Q':
                case 'R':
                    break;
                case 'S':
                    // Scroll up X lines (default 1)
                    if (type == DeviceType.XTERM) {
                        boolean xtermPrivateModeFlag = false;
                        for (int i = 0; i < collectBuffer.length(); i++) {
                            if (collectBuffer.charAt(i) == '?') {
                                xtermPrivateModeFlag = true;
                                break;
                            }
                        }
                        if (xtermPrivateModeFlag) {
                            xtermSixelQuery();
                        } else {
                            su();
                        }
                    }
                    break;
                case 'T':
                    // Scroll down X lines (default 1)
                    if (type == DeviceType.XTERM) {
                        sd();
                    }
                    break;
                case 'U':
                case 'V':
                case 'W':
                    break;
                case 'X':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        // ECH - Erase character
                        ech();
                    }
                    break;
                case 'Y':
                    break;
                case 'Z':
                    // CBT - Cursor backward X tab stops (default 1)
                    if (type == DeviceType.XTERM) {
                        cbt();
                    }
                    break;
                case '[':
                case '\\':
                case ']':
                case '^':
                case '_':
                    break;
                case '`':
                    // HPA - Cursor to column # in current row.  Same as CHA
                    if (type == DeviceType.XTERM) {
                        cha();
                    }
                    break;
                case 'a':
                    // HPR - Cursor right.  Same as CUF
                    if (type == DeviceType.XTERM) {
                        cuf();
                    }
                    break;
                case 'b':
                    // REP - Repeat last char X times
                    if (type == DeviceType.XTERM) {
                        rep();
                    }
                    break;
                case 'c':
                    // DA - Device attributes
                    da();
                    break;
                case 'd':
                    // VPA - Cursor to row, current column.
                    if (type == DeviceType.XTERM) {
                        vpa();
                    }
                    break;
                case 'e':
                    // VPR - Cursor down.  Same as CUD
                    if (type == DeviceType.XTERM) {
                        cud();
                    }
                    break;
                case 'f':
                    // HVP - Horizontal and vertical position
                    hvp();
                    break;
                case 'g':
                    // TBC - Tabulation clear
                    tbc();
                    break;
                case 'h':
                    // Sets an ANSI or DEC private toggle
                    setToggle(true);
                    break;
                case 'i':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        // Printer functions
                        printerFunctions();
                    }
                    break;
                case 'j':
                case 'k':
                    break;
                case 'l':
                    // Sets an ANSI or DEC private toggle
                    setToggle(false);
                    break;
                case 'm':
                    // SGR - Select graphics rendition
                    sgr();
                    break;
                case 'n':
                    // DSR - Device status report
                    dsr();
                    break;
                case 'o':
                case 'p':
                    break;
                case 'q':
                    // DECLL - Load leds
                    // Not supported
                    break;
                case 'r':
                    // DECSTBM - Set top and bottom margins
                    decstbm();
                    break;
                case 's':
                    // Save cursor (ANSI.SYS)
                    if (type == DeviceType.XTERM) {
                        savedState.cursorX = currentState.cursorX;
                        savedState.cursorY = currentState.cursorY;
                    }
                    break;
                case 't':
                    if (type == DeviceType.XTERM) {
                        // Window operations
                        xtermWindowOps();
                    }
                    break;
                case 'u':
                    // Restore cursor (ANSI.SYS)
                    if (type == DeviceType.XTERM) {
                        cursorPosition(savedState.cursorY, savedState.cursorX);
                    }
                    break;
                case 'v':
                case 'w':
                    break;
                case 'x':
                    // DECREQTPARM - Request terminal parameters
                    decreqtparm();
                    break;
                case 'y':
                case 'z':
                case '{':
                case '|':
                case '}':
                case '~':
                    break;
                }
                toGround();
            }

            // 7F                  --> ignore

            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                toGround();
            }

            // 0x3A goes to CSI_IGNORE
            if (ch == 0x3A) {
                scanState = ScanState.CSI_IGNORE;
            }
            return;

        case CSI_PARAM:
            // 00-17, 19, 1C-1F    --> execute
            if (ch <= 0x1F) {
                handleControlChar((char) ch);
            }

            // 20-2F               --> collect, then switch to CSI_INTERMEDIATE
            if ((ch >= 0x20) && (ch <= 0x2F)) {
                collect((char) ch);
                scanState = ScanState.CSI_INTERMEDIATE;
            }

            // 30-39, 3B           --> param
            if ((ch >= '0') && (ch <= '9')) {
                param((byte) ch);
            }
            if (ch == ';') {
                param((byte) ch);
            }

            // 0x3A goes to CSI_IGNORE
            if (ch == 0x3A) {
                scanState = ScanState.CSI_IGNORE;
            }
            // 0x3C-3F goes to CSI_IGNORE
            if ((ch >= 0x3C) && (ch <= 0x3F)) {
                scanState = ScanState.CSI_IGNORE;
            }

            // 40-7E               --> dispatch, then switch to GROUND
            if ((ch >= 0x40) && (ch <= 0x7E)) {
                switch (ch) {
                case '@':
                    // ICH - Insert character
                    ich();
                    break;
                case 'A':
                    // CUU - Cursor up
                    cuu();
                    break;
                case 'B':
                    // CUD - Cursor down
                    cud();
                    break;
                case 'C':
                    // CUF - Cursor forward
                    cuf();
                    break;
                case 'D':
                    // CUB - Cursor backward
                    cub();
                    break;
                case 'E':
                    // CNL - Cursor down and to column 1
                    if (type == DeviceType.XTERM) {
                        cnl();
                    }
                    break;
                case 'F':
                    // CPL - Cursor up and to column 1
                    if (type == DeviceType.XTERM) {
                        cpl();
                    }
                    break;
                case 'G':
                    // CHA - Cursor to column # in current row
                    if (type == DeviceType.XTERM) {
                        cha();
                    }
                    break;
                case 'H':
                    // CUP - Cursor position
                    cup();
                    break;
                case 'I':
                    // CHT - Cursor forward X tab stops (default 1)
                    if (type == DeviceType.XTERM) {
                        cht();
                    }
                    break;
                case 'J':
                    // ED - Erase in display
                    ed();
                    break;
                case 'K':
                    // EL - Erase in line
                    el();
                    break;
                case 'L':
                    // IL - Insert line
                    il();
                    break;
                case 'M':
                    // DL - Delete line
                    dl();
                    break;
                case 'N':
                case 'O':
                    break;
                case 'P':
                    // DCH - Delete character
                    dch();
                    break;
                case 'Q':
                case 'R':
                    break;
                case 'S':
                    // Scroll up X lines (default 1)
                    if (type == DeviceType.XTERM) {
                        boolean xtermPrivateModeFlag = false;
                        for (int i = 0; i < collectBuffer.length(); i++) {
                            if (collectBuffer.charAt(i) == '?') {
                                xtermPrivateModeFlag = true;
                                break;
                            }
                        }
                        if (xtermPrivateModeFlag) {
                            xtermSixelQuery();
                        } else {
                            su();
                        }
                    }
                    break;
                case 'T':
                    // Scroll down X lines (default 1)
                    if (type == DeviceType.XTERM) {
                        sd();
                    }
                    break;
                case 'U':
                case 'V':
                case 'W':
                    break;
                case 'X':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        // ECH - Erase character
                        ech();
                    }
                    break;
                case 'Y':
                    break;
                case 'Z':
                    // CBT - Cursor backward X tab stops (default 1)
                    if (type == DeviceType.XTERM) {
                        cbt();
                    }
                    break;
                case '[':
                case '\\':
                case ']':
                case '^':
                case '_':
                    break;
                case '`':
                    // HPA - Cursor to column # in current row.  Same as CHA
                    if (type == DeviceType.XTERM) {
                        cha();
                    }
                    break;
                case 'a':
                    // HPR - Cursor right.  Same as CUF
                    if (type == DeviceType.XTERM) {
                        cuf();
                    }
                    break;
                case 'b':
                    // REP - Repeat last char X times
                    if (type == DeviceType.XTERM) {
                        rep();
                    }
                    break;
                case 'c':
                    // DA - Device attributes
                    da();
                    break;
                case 'd':
                    // VPA - Cursor to row, current column.
                    if (type == DeviceType.XTERM) {
                        vpa();
                    }
                    break;
                case 'e':
                    // VPR - Cursor down.  Same as CUD
                    if (type == DeviceType.XTERM) {
                        cud();
                    }
                    break;
                case 'f':
                    // HVP - Horizontal and vertical position
                    hvp();
                    break;
                case 'g':
                    // TBC - Tabulation clear
                    tbc();
                    break;
                case 'h':
                    // Sets an ANSI or DEC private toggle
                    setToggle(true);
                    break;
                case 'i':
                    if ((type == DeviceType.VT220)
                        || (type == DeviceType.XTERM)) {

                        // Printer functions
                        printerFunctions();
                    }
                    break;
                case 'j':
                case 'k':
                    break;
                case 'l':
                    // Sets an ANSI or DEC private toggle
                    setToggle(false);
                    break;
                case 'm':
                    // SGR - Select graphics rendition
                    sgr();
                    break;
                case 'n':
                    // DSR - Device status report
                    dsr();
                    break;
                case 'o':
                case 'p':
                    break;
                case 'q':
                    // DECLL - Load leds
                    // Not supported
                    break;
                case 'r':
                    // DECSTBM - Set top and bottom margins
                    decstbm();
                    break;
                case 's':
                    break;
                case 't':
                    if (type == DeviceType.XTERM) {
                        // Window operations
                        xtermWindowOps();
                    }
                    break;
                case 'u':
                case 'v':
                case 'w':
                    break;
                case 'x':
                    // DECREQTPARM - Request terminal parameters
                    decreqtparm();
                    break;
                case 'y':
                case 'z':
                case '{':
                case '|':
                case '}':
                case '~':
                    break;
                }
                toGround();
            }

            // 7F                  --> ignore
            return;

        case CSI_INTERMEDIATE:
            // 00-17, 19, 1C-1F    --> execute
            if (ch <= 0x1F) {
                handleControlChar((char) ch);
            }

            // 20-2F               --> collect
            if ((ch >= 0x20) && (ch <= 0x2F)) {
                collect((char) ch);
            }

            // 0x30-3F goes to CSI_IGNORE
            if ((ch >= 0x30) && (ch <= 0x3F)) {
                scanState = ScanState.CSI_IGNORE;
            }

            // 40-7E               --> dispatch, then switch to GROUND
            if ((ch >= 0x40) && (ch <= 0x7E)) {
                switch (ch) {
                case '@':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                case '[':
                case '\\':
                case ']':
                case '^':
                case '_':
                case '`':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                    break;
                case 'p':
                    if (((type == DeviceType.VT220)
                            || (type == DeviceType.XTERM))
                        && (collectBuffer.charAt(collectBuffer.length() - 1) == '\"')
                    ) {
                        // DECSCL - compatibility level
                        decscl();
                    }
                    if ((type == DeviceType.XTERM)
                        && (collectBuffer.charAt(collectBuffer.length() - 1) == '!')
                    ) {
                        // DECSTR - Soft terminal reset
                        decstr();
                    }
                    break;
                case 'q':
                    if (((type == DeviceType.VT220)
                            || (type == DeviceType.XTERM))
                        && (collectBuffer.charAt(collectBuffer.length() - 1) == '\"')
                    ) {
                        // DECSCA
                        decsca();
                    }
                    break;
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                case '{':
                case '|':
                case '}':
                case '~':
                    break;
                }
                toGround();
            }

            // 7F                  --> ignore
            return;

        case CSI_IGNORE:
            // 00-17, 19, 1C-1F    --> execute
            if (ch <= 0x1F) {
                handleControlChar((char) ch);
            }

            // 20-2F               --> collect
            if ((ch >= 0x20) && (ch <= 0x2F)) {
                collect((char) ch);
            }

            // 40-7E               --> ignore, then switch to GROUND
            if ((ch >= 0x40) && (ch <= 0x7E)) {
                toGround();
            }

            // 20-3F, 7F           --> ignore

            return;

        case DCS_ENTRY:

            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                toGround();
            }

            // 0x1B 0x5C goes to GROUND
            if (ch == 0x1B) {
                collect((char) ch);
            }
            if (ch == 0x5C) {
                if ((collectBuffer.length() > 0)
                    && (collectBuffer.charAt(collectBuffer.length() - 1) == 0x1B)
                ) {
                    toGround();
                }
            }

            // 20-2F               --> collect, then switch to DCS_INTERMEDIATE
            if ((ch >= 0x20) && (ch <= 0x2F)) {
                collect((char) ch);
                scanState = ScanState.DCS_INTERMEDIATE;
            }

            // 30-39, 3B           --> param, then switch to DCS_PARAM
            if ((ch >= '0') && (ch <= '9')) {
                param((byte) ch);
                scanState = ScanState.DCS_PARAM;
            }
            if (ch == ';') {
                param((byte) ch);
                scanState = ScanState.DCS_PARAM;
            }

            // 3C-3F               --> collect, then switch to DCS_PARAM
            if ((ch >= 0x3C) && (ch <= 0x3F)) {
                collect((char) ch);
                scanState = ScanState.DCS_PARAM;
            }

            // 00-17, 19, 1C-1F, 7F    --> ignore

            // 0x3A goes to DCS_IGNORE
            if (ch == 0x3F) {
                scanState = ScanState.DCS_IGNORE;
            }

            // 0x71 goes to DCS_SIXEL
            if (ch == 0x71) {
                sixelParseBuffer.setLength(0);
                scanState = ScanState.DCS_SIXEL;
            } else if ((ch >= 0x40) && (ch <= 0x7E)) {
                // 0x40-7E goes to DCS_PASSTHROUGH
                scanState = ScanState.DCS_PASSTHROUGH;
            }
            return;

        case DCS_INTERMEDIATE:

            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                toGround();
            }

            // 0x1B 0x5C goes to GROUND
            if (ch == 0x1B) {
                collect((char) ch);
            }
            if (ch == 0x5C) {
                if ((collectBuffer.length() > 0)
                    && (collectBuffer.charAt(collectBuffer.length() - 1) == 0x1B)
                ) {
                    toGround();
                }
            }

            // 0x30-3F goes to DCS_IGNORE
            if ((ch >= 0x30) && (ch <= 0x3F)) {
                scanState = ScanState.DCS_IGNORE;
            }

            // 0x40-7E goes to DCS_PASSTHROUGH
            if ((ch >= 0x40) && (ch <= 0x7E)) {
                scanState = ScanState.DCS_PASSTHROUGH;
            }

            // 00-17, 19, 1C-1F, 7F    --> ignore
            return;

        case DCS_PARAM:

            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                toGround();
            }

            // 0x1B 0x5C goes to GROUND
            if (ch == 0x1B) {
                collect((char) ch);
            }
            if (ch == 0x5C) {
                if ((collectBuffer.length() > 0)
                    && (collectBuffer.charAt(collectBuffer.length() - 1) == 0x1B)
                ) {
                    toGround();
                }
            }

            // 20-2F          --> collect, then switch to DCS_INTERMEDIATE
            if ((ch >= 0x20) && (ch <= 0x2F)) {
                collect((char) ch);
                scanState = ScanState.DCS_INTERMEDIATE;
            }

            // 30-39, 3B      --> param
            if ((ch >= '0') && (ch <= '9')) {
                param((byte) ch);
            }
            if (ch == ';') {
                param((byte) ch);
            }

            // 00-17, 19, 1C-1F, 7F    --> ignore

            // 0x3A, 3C-3F goes to DCS_IGNORE
            if (ch == 0x3F) {
                scanState = ScanState.DCS_IGNORE;
            }
            if ((ch >= 0x3C) && (ch <= 0x3F)) {
                scanState = ScanState.DCS_IGNORE;
            }

            // 0x71 goes to DCS_SIXEL
            if (ch == 0x71) {
                sixelParseBuffer.setLength(0);
                scanState = ScanState.DCS_SIXEL;
            } else if ((ch >= 0x40) && (ch <= 0x7E)) {
                // 0x40-7E goes to DCS_PASSTHROUGH
                scanState = ScanState.DCS_PASSTHROUGH;
            }
            return;

        case DCS_PASSTHROUGH:
            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                toGround();
            }

            // 0x1B 0x5C goes to GROUND
            if (ch == 0x1B) {
                collect((char) ch);
            }
            if (ch == 0x5C) {
                if ((collectBuffer.length() > 0)
                    && (collectBuffer.charAt(collectBuffer.length() - 1) == 0x1B)
                ) {
                    toGround();
                }
            }

            // 00-17, 19, 1C-1F, 20-7E   --> put
            if (ch <= 0x17) {
                // We ignore all DCS except sixel.
                return;
            }
            if (ch == 0x19) {
                // We ignore all DCS except sixel.
                return;
            }
            if ((ch >= 0x1C) && (ch <= 0x1F)) {
                // We ignore all DCS except sixel.
                return;
            }
            if ((ch >= 0x20) && (ch <= 0x7E)) {
                // We ignore all DCS except sixel.
                return;
            }

            // 7F                        --> ignore

            return;

        case DCS_IGNORE:
            // 00-17, 19, 1C-1F, 20-7F --> ignore

            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                toGround();
            }

            return;

        case DCS_SIXEL:
            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                parseSixel();
                toGround();
                return;
            }

            // 0x1B 0x5C goes to GROUND
            if (ch == 0x1B) {
                collect((char) ch);
                return;
            }
            if (ch == 0x5C) {
                if ((collectBuffer.length() > 0)
                    && (collectBuffer.charAt(collectBuffer.length() - 1) == 0x1B)
                ) {
                    parseSixel();
                    toGround();
                    return;
                }
            }

            // 00-17, 19, 1C-1F, 20-7E   --> put
            if ((ch <= 0x17)
                || (ch == 0x19)
                || ((ch >= 0x1C) && (ch <= 0x1F))
                || ((ch >= 0x20) && (ch <= 0x7E))
            ) {
                sixelParseBuffer.append((char) ch);
            }

            // 7F                        --> ignore
            return;

        case SOSPMAPC_STRING:
            // 00-17, 19, 1C-1F, 20-7F --> ignore

            // Special case for Jexer: PM can pass one control character
            if (ch == 0x1B) {
                pmPut((char) ch);
            }

            if ((ch >= 0x20) && (ch <= 0x7F)) {
                pmPut((char) ch);
            }

            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                toGround();
            }

            return;

        case OSC_STRING:
            // Special case for Xterm: OSC can pass control characters
            if ((ch == 0x9C) || (ch == 0x07) || (ch == 0x1B)) {
                oscPut((char) ch);
            }

            // 00-17, 19, 1C-1F        --> ignore

            // 20-7F                   --> osc_put
            if ((ch >= 0x20) && (ch <= 0x7F)) {
                oscPut((char) ch);
            }

            // 0x9C goes to GROUND
            if (ch == 0x9C) {
                toGround();
            }

            return;

        case VT52_DIRECT_CURSOR_ADDRESS:
            // This is a special case for the VT52 sequence "ESC Y l c"
            if (collectBuffer.length() == 0) {
                collect((char) ch);
            } else if (collectBuffer.length() == 1) {
                // We've got the two characters, one in the buffer and the
                // other in ch.
                cursorPosition(collectBuffer.charAt(0) - '\040', ch - '\040');
                toGround();
            }
            return;
        }

    }

    /**
     * Expose current cursor X to outside world.
     *
     * @return current cursor X
     */
    public final int getCursorX() {
        if (display.get(currentState.cursorY).isDoubleWidth()) {
            return currentState.cursorX * 2;
        }
        return currentState.cursorX;
    }

    /**
     * Expose current cursor Y to outside world.
     *
     * @return current cursor Y
     */
    public final int getCursorY() {
        return currentState.cursorY;
    }

    /**
     * Returns true if this terminal has requested the mouse pointer be
     * hidden.
     *
     * @return true if this terminal has requested the mouse pointer be
     * hidden
     */
    public final boolean hasHiddenMousePointer() {
        return hideMousePointer;
    }

    /**
     * Get the mouse protocol.
     *
     * @return MouseProtocol.OFF, MouseProtocol.X10, etc.
     */
    public MouseProtocol getMouseProtocol() {
        return mouseProtocol;
    }

    /**
     * Draw the left and right cells of a two-cell-wide (full-width) glyph.
     *
     * @param leftX the x position to draw the left half to
     * @param leftY the y position to draw the left half to
     * @param rightX the x position to draw the right half to
     * @param rightY the y position to draw the right half to
     * @param ch the character to draw
     */
    private void drawHalves(final int leftX, final int leftY,
        final int rightX, final int rightY, final int ch) {

        // System.err.println("drawHalves(): " + Integer.toHexString(ch));

        if (lastTextHeight != textHeight) {
            glyphMaker = GlyphMaker.getInstance(textHeight);
            lastTextHeight = textHeight;
        }

        Cell cell = new Cell(ch, currentState.attr);
        BufferedImage image = glyphMaker.getImage(cell, textWidth * 2,
            textHeight);
        BufferedImage leftImage = image.getSubimage(0, 0, textWidth,
            textHeight);
        BufferedImage rightImage = image.getSubimage(textWidth, 0, textWidth,
            textHeight);

        Cell left = new Cell(cell);
        left.setImage(leftImage);
        left.setWidth(Cell.Width.LEFT);
        display.get(leftY).replace(leftX, left);

        Cell right = new Cell(cell);
        right.setImage(rightImage);
        right.setWidth(Cell.Width.RIGHT);
        display.get(rightY).replace(rightX, right);
    }

    /**
     * Set the width of a character cell in pixels.
     *
     * @param textWidth the width in pixels of a character cell
     */
    public void setTextWidth(final int textWidth) {
        this.textWidth = textWidth;
    }

    /**
     * Set the height of a character cell in pixels.
     *
     * @param textHeight the height in pixels of a character cell
     */
    public void setTextHeight(final int textHeight) {
        this.textHeight = textHeight;
    }

    /**
     * Parse a sixel string into a bitmap image, and overlay that image onto
     * the text cells.
     */
    private void parseSixel() {

        /*
        System.err.println("parseSixel(): '" + sixelParseBuffer.toString()
            + "'");
        */

        Sixel sixel = new Sixel(sixelParseBuffer.toString(), sixelPalette);
        BufferedImage image = sixel.getImage();

        // System.err.println("parseSixel(): image " + image);

        if (image == null) {
            // Sixel data was malformed in some way, bail out.
            return;
        }
        if ((image.getWidth() < 1)
            || (image.getWidth() > 10000)
            || (image.getHeight() < 1)
            || (image.getHeight() > 10000)
        ) {
            return;
        }

        imageToCells(image, true);
    }

    /**
     * Parse a "Jexer" RGB image string into a bitmap image, and overlay that
     * image onto the text cells.
     *
     * @param pw width token
     * @param ph height token
     * @param ps scroll token
     * @param data pixel data
     */
    private void parseJexerImageRGB(final String pw, final String ph,
        final String ps, final String data) {

        int imageWidth = 0;
        int imageHeight = 0;
        boolean scroll = false;
        try {
            imageWidth = Integer.parseInt(pw);
            imageHeight = Integer.parseInt(ph);
        } catch (NumberFormatException e) {
            // SQUASH
            return;
        }
        if ((imageWidth < 1)
            || (imageWidth > 10000)
            || (imageHeight < 1)
            || (imageHeight > 10000)
        ) {
            return;
        }
        if (ps.equals("1")) {
            scroll = true;
        } else if (ps.equals("0")) {
            scroll = false;
        } else {
            return;
        }

        byte [] bytes = StringUtils.fromBase64(data.getBytes());
        if (bytes.length != (imageWidth * imageHeight * 3)) {
            return;
        }

        BufferedImage image = new BufferedImage(imageWidth, imageHeight,
            BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < imageWidth; x++) {
            for (int y = 0; y < imageHeight; y++) {
                int red   = bytes[(y * imageWidth * 3) + (x * 3)    ];
                if (red < 0) {
                    red += 256;
                }
                int green = bytes[(y * imageWidth * 3) + (x * 3) + 1];
                if (green < 0) {
                    green += 256;
                }
                int blue  = bytes[(y * imageWidth * 3) + (x * 3) + 2];
                if (blue < 0) {
                    blue += 256;
                }
                int rgb = 0xFF000000 | (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, rgb);
            }
        }

        imageToCells(image, scroll);
    }

    /**
     * Parse a "Jexer" PNG or JPG image string into a bitmap image, and
     * overlay that image onto the text cells.
     *
     * @param type 1 for PNG, 2 for JPG
     * @param ps scroll token
     * @param data pixel data
     */
    private void parseJexerImageFile(final int type, final String ps,
        final String data) {

        int imageWidth = 0;
        int imageHeight = 0;
        boolean scroll = false;
        BufferedImage image = null;
        try {
            byte [] bytes = StringUtils.fromBase64(data.getBytes());

            switch (type) {
            case 1:
                if ((bytes[0] != (byte) 0x89)
                    || (bytes[1] != 'P')
                    || (bytes[2] != 'N')
                    || (bytes[3] != 'G')
                    || (bytes[4] != (byte) 0x0D)
                    || (bytes[5] != (byte) 0x0A)
                    || (bytes[6] != (byte) 0x1A)
                    || (bytes[7] != (byte) 0x0A)
                ) {
                    // File does not have PNG header, bail out.
                    return;
                }
                break;

            case 2:
                if ((bytes[0] != (byte) 0XFF)
                    || (bytes[1] != (byte) 0xD8)
                    || (bytes[2] != (byte) 0xFF)
                ) {
                    // File does not have JPG header, bail out.
                    return;
                }
                break;

            default:
                // Unsupported type, bail out.
                return;
            }

            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            // SQUASH
            return;
        }
        assert (image != null);
        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        if ((imageWidth < 1)
            || (imageWidth > 10000)
            || (imageHeight < 1)
            || (imageHeight > 10000)
        ) {
            return;
        }
        if (ps.equals("1")) {
            scroll = true;
        } else if (ps.equals("0")) {
            scroll = false;
        } else {
            return;
        }

        imageToCells(image, scroll);
    }

    /**
     * Break up an image into the cells at the current cursor.
     *
     * @param image the image to display
     * @param scroll if true, scroll the image and move the cursor
     */
    private void imageToCells(final BufferedImage image, final boolean scroll) {
        assert (image != null);

        /*
         * Procedure:
         *
         * Break up the image into text cell sized pieces as a new array of
         * Cells.
         *
         * Note original column position x0.
         *
         * For each cell:
         *
         * 1. Advance (printCharacter(' ')) for horizontal increment, or
         *    index (linefeed() + cursorPosition(y, x0)) for vertical
         *    increment.
         *
         * 2. Set (x, y) cell image data.
         *
         * 3. For the right and bottom edges:
         *
         *   a. Render the text to pixels using Terminus font.
         *
         *   b. Blit the image on top of the text, using alpha channel.
         */
        int cellColumns = image.getWidth() / textWidth;
        if (cellColumns * textWidth < image.getWidth()) {
            cellColumns++;
        }
        int cellRows = image.getHeight() / textHeight;
        if (cellRows * textHeight < image.getHeight()) {
            cellRows++;
        }

        // Break the image up into an array of cells.
        Cell [][] cells = new Cell[cellColumns][cellRows];

        for (int x = 0; x < cellColumns; x++) {
            for (int y = 0; y < cellRows; y++) {

                int width = textWidth;
                if ((x + 1) * textWidth > image.getWidth()) {
                    width = image.getWidth() - (x * textWidth);
                }
                int height = textHeight;
                if ((y + 1) * textHeight > image.getHeight()) {
                    height = image.getHeight() - (y * textHeight);
                }

                Cell cell = new Cell();
                if ((width != textWidth) || (height != textHeight)) {
                    BufferedImage newImage;
                    newImage = new BufferedImage(textWidth, textHeight,
                        BufferedImage.TYPE_INT_ARGB);

                    Graphics gr = newImage.getGraphics();
                    gr.drawImage(image.getSubimage(x * textWidth,
                            y * textHeight, width, height),
                        0, 0, null, null);
                    gr.dispose();
                    cell.setImage(newImage);
                } else {
                    cell.setImage(image.getSubimage(x * textWidth,
                            y * textHeight, width, height));
                }

                cells[x][y] = cell;
            }
        }

        int x0 = currentState.cursorX;
        int y0 = currentState.cursorY;
        for (int y = 0; y < cellRows; y++) {
            for (int x = 0; x < cellColumns; x++) {
                assert (currentState.cursorX <= rightMargin);

                // A real sixel terminal would render the text of the current
                // cell first, then image over it (accounting for blank
                // pixels).  We do not support that.  A cell is either text,
                // or image, but not a mix of image-over-text.
                DisplayLine line = display.get(currentState.cursorY);
                line.replace(currentState.cursorX, cells[x][y]);

                // If at the end of the visible screen, stop.
                if (currentState.cursorX == rightMargin) {
                    break;
                }
                // Room for more image on the visible screen.
                currentState.cursorX++;
            }
            if (currentState.cursorY < scrollRegionBottom - 1) {
                // Not at the bottom, down a line.
                linefeed();
            } else if (scroll == true) {
                // At the bottom, scroll as needed.
                linefeed();
            } else {
                // At the bottom, no more scrolling, done.
                break;
            }

            cursorPosition(currentState.cursorY, x0);
        }

        if (scroll == false) {
            cursorPosition(y0, x0);
        }

    }

}
