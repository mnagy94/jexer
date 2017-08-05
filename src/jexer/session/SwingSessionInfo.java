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
package jexer.session;

import java.awt.Frame;
import java.awt.Insets;

/**
 * SwingSessionInfo provides a session implementation with a callback into an
 * Swing Frame to support queryWindowSize().  The username is blank, language
 * is "en_US", with a 132x40 text window.
 */
public final class SwingSessionInfo implements SessionInfo {

    /**
     * The Swing Frame.
     */
    private Frame frame;

    /**
     * The width of a text cell in pixels.
     */
    private int textWidth;

    /**
     * The height of a text cell in pixels.
     */
    private int textHeight;

    /**
     * User name.
     */
    private String username = "";

    /**
     * Language.
     */
    private String language = "en_US";

    /**
     * Text window width.
     */
    private int windowWidth = 80;

    /**
     * Text window height.
     */
    private int windowHeight = 25;

    /**
     * Username getter.
     *
     * @return the username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Username setter.
     *
     * @param username the value
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * Language getter.
     *
     * @return the language
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * Language setter.
     *
     * @param language the value
     */
    public void setLanguage(final String language) {
        this.language = language;
    }

    /**
     * Text window width getter.
     *
     * @return the window width
     */
    public int getWindowWidth() {
        return windowWidth;
    }

    /**
     * Text window height getter.
     *
     * @return the window height
     */
    public int getWindowHeight() {
        return windowHeight;
    }

    /**
     * Public constructor.
     *
     * @param frame the Swing Frame
     * @param textWidth the width of a cell in pixels
     * @param textHeight the height of a cell in pixels
     * @param windowWidth the number of text columns to start with
     * @param windowHeight the number of text rows to start with
     */
    public SwingSessionInfo(final Frame frame, final int textWidth,
        final int textHeight, final int windowWidth, final int windowHeight) {

        this.frame              = frame;
        this.textWidth          = textWidth;
        this.textHeight         = textHeight;
        this.windowWidth        = windowWidth;
        this.windowHeight       = windowHeight;
    }

    /**
     * Re-query the text window size.
     */
    public void queryWindowSize() {
        Insets insets = frame.getInsets();
        int height = frame.getHeight() - insets.top - insets.bottom;
        int width = frame.getWidth() - insets.left - insets.right;
        windowWidth = width / textWidth;
        windowHeight = height / textHeight;

        /*
        System.err.printf("queryWindowSize(): frame %d %d window %d %d\n",
            frame.getWidth(), frame.getHeight(),
            windowWidth, windowHeight);
         */

    }

}
