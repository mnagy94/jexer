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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Wrapper for integrating with Swing, because JFrame and JComponent have
 * separate hierarchies.
 */
class SwingComponent {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * If true, use triple buffering when drawing to a JFrame.
     */
    public static boolean tripleBuffer = true;

    /**
     * The frame reference, if we are drawing to a JFrame.
     */
    private JFrame frame;

    /**
     * The component reference, if we are drawing to a JComponent.
     */
    private JComponent component;

    /**
     * An optional border in pixels to add.
     */
    private static final int BORDER = 5;

    /**
     * Adjustable Insets for this component.  This has the effect of adding a
     * black border around the drawing area.
     */
    Insets adjustInsets = new Insets(BORDER, BORDER, BORDER, BORDER);

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Construct using a JFrame.
     *
     * @param frame the JFrame to draw to
     */
    public SwingComponent(final JFrame frame) {
        this.frame = frame;
        setupFrame();
    }

    /**
     * Construct using a JComponent.
     *
     * @param component the JComponent to draw to
     */
    public SwingComponent(final JComponent component) {
        this.component = component;
        setupComponent();
    }

    // ------------------------------------------------------------------------
    // SwingComponent ---------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the BufferStrategy object needed for triple-buffering.
     *
     * @return the BufferStrategy
     * @throws IllegalArgumentException if this function is called when
     * not rendering to a JFrame
     */
    public BufferStrategy getBufferStrategy() {
        if (frame != null) {
            return frame.getBufferStrategy();
        } else {
            throw new IllegalArgumentException("BufferStrategy not used " +
                "for JComponent access");
        }
    }

    /**
     * Get the JFrame reference.
     *
     * @return the frame, or null if this is drawing to a JComponent
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Get the JComponent reference.
     *
     * @return the component, or null if this is drawing to a JFrame
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * Setup to render to an existing JComponent.
     */
    public void setupComponent() {
        component.setBackground(Color.black);

        // Kill the X11 cursor
        // Transparent 16 x 16 pixel cursor image.
        BufferedImage cursorImg = new BufferedImage(16, 16,
            BufferedImage.TYPE_INT_ARGB);
        // Create a new blank cursor.
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
        cursorImg, new Point(0, 0), "blank cursor");
        component.setCursor(blankCursor);

        // Be capable of seeing Tab / Shift-Tab
        component.setFocusTraversalKeysEnabled(false);
    }

    /**
     * Setup to render to an existing JFrame.
     */
    public void setupFrame() {
        frame.setTitle("Jexer Application");
        frame.setBackground(Color.black);
        frame.pack();

        // Kill the X11 cursor
        // Transparent 16 x 16 pixel cursor image.
        BufferedImage cursorImg = new BufferedImage(16, 16,
            BufferedImage.TYPE_INT_ARGB);
        // Create a new blank cursor.
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
        cursorImg, new Point(0, 0), "blank cursor");
        frame.setCursor(blankCursor);

        // Be capable of seeing Tab / Shift-Tab
        frame.setFocusTraversalKeysEnabled(false);

        // Setup triple-buffering
        if (tripleBuffer) {
            frame.setIgnoreRepaint(true);
            frame.createBufferStrategy(3);
        }
    }

    /**
     * Set the window title.
     *
     * @param title the new title
     */
    public void setTitle(final String title) {
        if (frame != null) {
            frame.setTitle(title);
        }
    }

    /**
     * Paints this component.
     *
     * @param g the graphics context to use for painting
     */
    public void paint(Graphics g) {
        if (frame != null) {
            frame.paint(g);
        } else {
            component.paint(g);
        }
    }

    /**
     * Repaints this component.
     */
    public void repaint() {
        if (frame != null) {
            frame.repaint();
        } else {
            component.repaint();
        }
    }

    /**
     * Repaints the specified rectangle of this component.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width
     * @param height the height
     */
    public void repaint(int x, int y, int width, int height) {
        if (frame != null) {
            frame.repaint(x, y, width, height);
        } else {
            component.repaint(x, y, width, height);
        }
    }

    /**
     * If a border has been set on this component, returns the border's
     * insets; otherwise calls super.getInsets.
     *
     * @return the value of the insets property
     */
    public Insets getInsets() {
        Insets swingInsets = null;
        if (frame != null) {
            swingInsets = frame.getInsets();
        } else {
            swingInsets = component.getInsets();
        }
        Insets result = new Insets(swingInsets.top + adjustInsets.top,
            swingInsets.left + adjustInsets.left,
            swingInsets.bottom + adjustInsets.bottom,
            swingInsets.right + adjustInsets.right);
        return result;
    }

    /**
     * Returns the current width of this component.
     *
     * @return the current width of this component
     */
    public int getWidth() {
        if (frame != null) {
            return frame.getWidth();
        } else {
            return component.getWidth();
        }
    }

    /**
     * Returns the current height of this component.
     *
     * @return the current height of this component
     */
    public int getHeight() {
        if (frame != null) {
            return frame.getHeight();
        } else {
            return component.getHeight();
        }
    }

    /**
     * Gets the font of this component.
     *
     * @return this component's font; if a font has not been set for this
     * component, the font of its parent is returned
     */
    public Font getFont() {
        if (frame != null) {
            return frame.getFont();
        } else {
            return component.getFont();
        }
    }

    /**
     * Sets the font of this component.
     *
     * @param f the font to become this component's font; if this parameter
     * is null then this component will inherit the font of its parent
     */
    public void setFont(final Font f) {
        if (frame != null) {
            frame.setFont(f);
        } else {
            component.setFont(f);
        }
    }

    /**
     * Shows or hides this Window depending on the value of parameter b.
     *
     * @param b if true, make visible, else make invisible
     */
    public void setVisible(final boolean b) {
        if (frame != null) {
            frame.setVisible(b);
        } else {
            component.setVisible(b);
        }
    }

    /**
     * Creates a graphics context for this component. This method will return
     * null if this component is currently not displayable.
     *
     * @return a graphics context for this component, or null if it has none
     */
    public Graphics getGraphics() {
        if (frame != null) {
            return frame.getGraphics();
        } else {
            return component.getGraphics();
        }
    }

    /**
     * Releases all of the native screen resources used by this Window, its
     * subcomponents, and all of its owned children. That is, the resources
     * for these Components will be destroyed, any memory they consume will
     * be returned to the OS, and they will be marked as undisplayable.
     */
    public void dispose() {
        if (frame != null) {
            frame.dispose();
        } else {
            component.getParent().remove(component);
        }
    }

    /**
     * Resize the component to match the font dimensions.
     *
     * @param width the new width in pixels
     * @param height the new height in pixels
     */
    public void setDimensions(final int width, final int height) {
        // Figure out the thickness of borders and use that to set the final
        // size.
        if (frame != null) {
            Insets insets = frame.getInsets();
            frame.setSize(width + insets.left + insets.right,
                height + insets.top + insets.bottom);
        } else {
            Insets insets = component.getInsets();
            component.setSize(width + insets.left + insets.right,
                height + insets.top + insets.bottom);
        }
    }

    /**
     * Adds the specified component listener to receive component events from
     * this component. If listener l is null, no exception is thrown and no
     * action is performed.
     *
     * @param l the component listener
     */
    public void addComponentListener(ComponentListener l) {
        if (frame != null) {
            frame.addComponentListener(l);
        } else {
            component.addComponentListener(l);
        }
    }

    /**
     * Adds the specified key listener to receive key events from this
     * component. If l is null, no exception is thrown and no action is
     * performed.
     *
     * @param l the key listener.
     */
    public void addKeyListener(KeyListener l) {
        if (frame != null) {
            frame.addKeyListener(l);
        } else {
            component.addKeyListener(l);
        }
    }

    /**
     * Adds the specified mouse listener to receive mouse events from this
     * component. If listener l is null, no exception is thrown and no action
     * is performed.
     *
     * @param l the mouse listener
     */
    public void addMouseListener(MouseListener l) {
        if (frame != null) {
            frame.addMouseListener(l);
        } else {
            component.addMouseListener(l);
        }
    }

    /**
     * Adds the specified mouse motion listener to receive mouse motion
     * events from this component. If listener l is null, no exception is
     * thrown and no action is performed.
     *
     * @param l the mouse motion listener
     */
    public void addMouseMotionListener(MouseMotionListener l) {
        if (frame != null) {
            frame.addMouseMotionListener(l);
        } else {
            component.addMouseMotionListener(l);
        }
    }

    /**
     * Adds the specified mouse wheel listener to receive mouse wheel events
     * from this component. Containers also receive mouse wheel events from
     * sub-components.
     *
     * @param l the mouse wheel listener
     */
    public void addMouseWheelListener(MouseWheelListener l) {
        if (frame != null) {
            frame.addMouseWheelListener(l);
        } else {
            component.addMouseWheelListener(l);
        }
    }

    /**
     * Adds the specified window listener to receive window events from this
     * window. If l is null, no exception is thrown and no action is
     * performed.
     *
     * @param l the window listener
     */
    public void addWindowListener(WindowListener l) {
        if (frame != null) {
            frame.addWindowListener(l);
        }
    }

}
