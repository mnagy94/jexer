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

import jexer.backend.*;

/**
 * This class shows off the use of MultiBackend and MultiScreen.
 */
public class Demo6 {

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        try {
            /*
             * Spin up a Swing backend to match the ECMA48 backend on
             * System.in/out.
             */
            ECMA48Backend ecmaBackend = new ECMA48Backend(new Object(), null,
                null);
            MultiBackend multiBackend = new MultiBackend(ecmaBackend);
            DemoApplication demoApp = new DemoApplication(multiBackend);
            Screen multiScreen = multiBackend.getScreen();

            SwingBackend swingBackend = new SwingBackend(new Object(),
                multiScreen.getWidth(), multiScreen.getHeight(), 16);
            multiBackend.addBackend(swingBackend);
            multiBackend.setListener(demoApp);

            (new Thread(demoApp)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
