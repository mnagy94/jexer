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

import java.awt.Font;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import jexer.backend.SwingBackend;

/**
 * This class is the main driver for a simple demonstration of Jexer's
 * capabilities.  It shows two Swing demo applications running in the same
 * Swing UI.
 */
public class Demo5 implements WindowListener {

    /**
     * The first demo application instance.
     */
    DemoApplication app1 = null;

    /**
     * The second demo application instance.
     */
    DemoApplication app2 = null;

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowActivated(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowClosed(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowClosing(final WindowEvent event) {
        if (app1 != null) {
            app1.exit();
        }
        if (app2 != null) {
            app2.exit();
        }
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowDeactivated(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowDeiconified(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowIconified(final WindowEvent event) {
        // Ignore
    }

    /**
     * Pass window events into the event queue.
     *
     * @param event window event received
     */
    public void windowOpened(final WindowEvent event) {
        // Ignore
    }

    /**
     * Run two demo applications in separate panes.
     */
    private void addApplications() {

        // Spin up the frame
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(this);

        // Create two panels with two applications, each with a different
        // font size.
        JPanel app1Panel = new JPanel();
        SwingBackend app1Backend = new SwingBackend(app1Panel, new Object(),
            80, 25, 16);
        app1 = new DemoApplication(app1Backend);
        app1Backend.setListener(app1);

        JPanel app2Panel = new JPanel();
        SwingBackend app2Backend = new SwingBackend(app2Panel, new Object(),
            80, 25, 18);
        app2 = new DemoApplication(app2Backend);
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 18);
        app2Backend.setFont(font);
        app2Backend.setListener(app2);
        (new Thread(app1)).start();
        (new Thread(app2)).start();

        JSplitPane mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            app1Panel, app2Panel);
        mainPane.setOneTouchExpandable(true);
        mainPane.setDividerLocation(500);
        mainPane.setDividerSize(6);
        mainPane.setBorder(null);
        frame.setContentPane(mainPane);

        frame.setTitle("Two Jexer Apps In One Swing UI");
        frame.setSize(1000, 640);
        frame.setVisible(true);
    }

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(final String [] args) {
        try {
            Demo5 demo = new Demo5();
            demo.addApplications();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
