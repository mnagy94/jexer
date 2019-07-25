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
package jexer.net;

import java.io.OutputStream;
import java.io.IOException;

import static jexer.net.TelnetSocket.*;

/**
 * TelnetOutputStream works with TelnetSocket to perform the telnet protocol.
 */
public class TelnetOutputStream extends OutputStream {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * The root TelnetSocket that has my telnet protocol state.
     */
    private TelnetSocket master;

    /**
     * The raw socket's OutputStream.
     */
    private OutputStream output;

    /**
     * When true, the last byte the caller passed to write() was a CR.
     */
    private boolean writeCR = false;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Package private constructor.
     *
     * @param master the master TelnetSocket
     * @param output the underlying socket's OutputStream
     */
    TelnetOutputStream(final TelnetSocket master, final OutputStream output) {
        this.master = master;
        this.output = output;
    }

    // ------------------------------------------------------------------------
    // OutputStrem ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Closes this output stream and releases any system resources associated
     * with this stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (output != null) {
            output.close();
            output = null;
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes to be
     * written out.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        if ((master.binaryMode == false) && (writeCR == true)) {
            // The last byte sent to this.write() was a CR, which was never
            // actually sent.  So send the CR in ascii mode, then flush.
            // CR <anything> -> CR NULL
            output.write(C_CR);
            output.write(C_NUL);
            writeCR = false;
        }
        output.flush();
    }

    /**
     * Writes b.length bytes from the specified byte array to this output
     * stream.
     *
     * @param b the data.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final byte[] b) throws IOException {
        writeImpl(b, 0, b.length);
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off
     * to this output stream.
     *
     * @param b the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final byte[] b, final int off,
        final int len) throws IOException {

        writeImpl(b, off, len);
    }

    /**
     * Writes the specified byte to this output stream.
     *
     * @param b the byte to write.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final int b) throws IOException {
        byte [] bytes = new byte[1];
        bytes[0] = (byte) b;
        writeImpl(bytes, 0, 1);
    }

    // ------------------------------------------------------------------------
    // TelnetOutputStrem ------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Writes b.length bytes from the specified byte array to this output
     * stream.  Note package private access.
     *
     * @param b the data.
     * @throws IOException if an I/O error occurs
     */
    void rawWrite(final byte[] b) throws IOException {
        output.write(b, 0, b.length);
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off
     * to this output stream.
     *
     * @param b the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs
     */
    private void writeImpl(final byte[] b, final int off,
        final int len) throws IOException {

        byte [] writeBuffer = new byte[Math.max(len, 4)];
        int writeBufferI = 0;

        for (int i = 0; i < len; i++) {
            if (writeBufferI >= writeBuffer.length - 4) {
                // Flush what we have generated so far and reset the buffer,
                // because the next byte could generate up to 4 output bytes
                // (CR <something> <IAC> <IAC>).
                output.write(writeBuffer, 0, writeBufferI);
                writeBufferI = 0;
            }

            // Pull the next byte
            byte ch = b[i + off];

            if (master.binaryMode == true) {

                if (ch == TELNET_IAC) {
                    // IAC -> IAC IAC
                    writeBuffer[writeBufferI++] = (byte) TELNET_IAC;
                    writeBuffer[writeBufferI++] = (byte) TELNET_IAC;
                } else {
                    // Anything else -> just send
                    writeBuffer[writeBufferI++] = ch;
                }
                continue;
            }

            // Non-binary mode: more complicated.  We use writeCR to handle
            // the case that the last byte of b was a CR.

            // Bare carriage return -> CR NUL
            if (ch == C_CR) {
                if (writeCR == true) {
                    // Flush the previous CR to the stream.
                    // CR <anything> -> CR NULL
                    writeBuffer[writeBufferI++] = (byte) C_CR;
                    writeBuffer[writeBufferI++] = (byte) C_NUL;
                }
                writeCR = true;
            } else if (ch == C_LF) {
                if (writeCR == true) {
                    // CR LF -> CR LF
                    writeBuffer[writeBufferI++] = (byte) C_CR;
                    writeBuffer[writeBufferI++] = (byte) C_LF;
                    writeCR = false;
                } else {
                    // Bare LF -> LF
                    writeBuffer[writeBufferI++] = ch;
                }
            } else if (ch == TELNET_IAC) {
                if (writeCR == true) {
                    // CR <anything> -> CR NULL
                    writeBuffer[writeBufferI++] = (byte) C_CR;
                    writeBuffer[writeBufferI++] = (byte) C_NUL;
                    writeCR = false;
                }
                // IAC -> IAC IAC
                writeBuffer[writeBufferI++] = (byte) TELNET_IAC;
                writeBuffer[writeBufferI++] = (byte) TELNET_IAC;
            } else {
                if (writeCR == true) {
                    // CR <anything> -> CR NULL
                    writeBuffer[writeBufferI++] = (byte) C_CR;
                    writeBuffer[writeBufferI++] = (byte) C_NUL;
                    writeCR = false;
                } else {
                    // Normal character */
                    writeBuffer[writeBufferI++] = ch;
                }
            }

        } // while (i < userbuf.length)

        if (writeBufferI > 0) {
            // Flush what we have generated so far and reset the buffer.
            output.write(writeBuffer, 0, writeBufferI);
        }
    }

}
