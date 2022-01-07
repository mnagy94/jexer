/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2022 Autumn Lamonte
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
 * @author Autumn Lamonte ⚧ Trans Liberation Now
 * @version 1
 */
package jexer.bits;

import java.awt.image.BufferedImage;
import java.util.List;

import jexer.TAction;
import jexer.TApplication;
import jexer.TTimer;

/**
 * An animation that can be displayed in an image.
 */
public class Animation {

    /**
     * List of images for an animation.
     */
    private List<BufferedImage> frames;

    /**
     * The index of the frame that is currently visible.
     */
    private int currentFrame = 0;

    /**
     * Whether or not the current frame has been returned.
     */
    private boolean gotFrame = false;

    /**
     * Number of millis to wait until next animation frame.
     */
    private int frameDelay = 0;

    /**
     * Number of times to loop the animation.  1 means play it once.  0 means
     * to play it forever.
     */
    private int frameLoops = 0;

    /**
     * The number of loops executed so far.
     */
    private int loopCount = 0;

    /**
     * If true, the animation is running.
     */
    private boolean running;

    /**
     * The timer that is incrementing the current frame index.
     */
    private TTimer timer;

    /**
     * Public constructor.
     *
     * @param frames the frames
     * @param frameDelay the number of millis to wait until next animation
     * frame
     * @param frameLoops the number of times to loop the animation.  0 means
     * play it once.  -1 means to play it forever.
     */
    public Animation(final List<BufferedImage> frames, final int frameDelay,
        final int frameLoops) {

        assert (frames != null);
        assert (frames.size() > 0);

        this.frames = frames;
        this.frameDelay = frameDelay;
        this.frameLoops = frameLoops;
    }

    /**
     * Check if this animation is running.
     *
     * @return true if the animation is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Start the animation.
     *
     * @param application the application
     */
    public void start(final TApplication application) {
        if (running) {
            return;
        }
        running = true;

        assert (application != null);

        /*
        System.err.printf("start() %d frames loops %d delay %d\n",
            frames.size(), frameLoops, frameDelay);
         */

        if (frames.size() > 1) {
            timer = application.addTimer(frameDelay, true,
                new TAction() {
                    public void DO() {
                        if (running) {
                            if (gotFrame) {
                                currentFrame++;
                                gotFrame = false;
                            }
                            if (currentFrame >= frames.size()) {
                                currentFrame = 0;
                            }
                            loopCount++;
                            if ((frameLoops > 0) && (loopCount >= frameLoops)) {
                                if (timer != null) {
                                    timer.setRecurring(false);
                                }
                            }
                        } else {
                            if (timer != null) {
                                timer.setRecurring(false);
                            }
                        }
                        application.doRepaint();
                    }
                });
        }
    }

    /**
     * Stop the animation.
     */
    public void stop() {
        running = false;
    }

    /**
     * Reset the animation.
     */
    public void reset() {
        loopCount = 0;
    }

    /**
     * Get the number of frames in this animation.
     *
     * @return the number of frames
     */
    public int count() {
        return frames.size();
    }

    /**
     * Get the number of the current frame in view.
     *
     * @return the frame number
     */
    public int currentFrameNumber() {
        return currentFrame;
    }

    /**
     * Get a frame by number.
     *
     * @param frameNumber the frame number
     * @return the frame
     */
    public BufferedImage getFrame(final int frameNumber) {
        gotFrame = true;
        return frames.get(frameNumber);
    }

    /**
     * Get the frame currently in view.
     *
     * @return the frame
     */
    public BufferedImage getFrame() {
        gotFrame = true;
        return frames.get(currentFrame);
    }

}
