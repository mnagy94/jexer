import java.awt.image.BufferedImage;
import java.io.File;
import jexer.TApplication;
import jexer.TExceptionDialog;
import jexer.TImage;
import jexer.TWindow;
import jexer.event.TResizeEvent;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_QUIET;

/**
 * XtermVideoPlayer plays videos in an Xterm.  It uses the JavaCV library to
 * obtain images from a video file and puts them into a TImage.  The initial
 * few seconds will often stutter as the Java JIT compiler steps in.
 *
 * See JavaCV at https://github.com/bytedeco/javacv .
 *
 * Compile it with:
 *    javac -cp javacv.jar:ffmpeg.jar:jexer.jar XtermVideoPlayer.java
 *
 * Run it with (assuming platform is linux-x86_64):
 *    java -cp javacv.jar:ffmpeg.jar:ffmpeg-linux-x86_64.jar:jexer.jar:. XtermVideoPlayer filename
 */
public class XtermVideoPlayer extends TApplication {

    // The image reference.
    private TImage image;

    // Constructor sets up widgets.
    public XtermVideoPlayer(String filename) throws Exception {
        super(BackendType.XTERM);

        // Create standard menus for Tool, File, and Window.
        addToolMenu();
        addFileMenu();
        addWindowMenu();

        // Make it cute.
        getTheme().setFemme();
        System.setProperty("jexer.TWindow.borderStyleForeground", "round");
        System.setProperty("jexer.TWindow.borderStyleModal", "round");
        System.setProperty("jexer.TWindow.borderStyleMoving", "round");
        System.setProperty("jexer.TWindow.borderStyleInactive", "round");
        setDesktop(null);
        setHideStatusBar(true);

        // Create a window for the image.  The resize event is overridden so
        // that the internal image field changes in size with the window.
        TWindow window = new TWindow(this, filename, 0, 0,
            getScreen().getWidth() / 2, getScreen().getHeight() / 2) {

            @Override
            public void onResize(final TResizeEvent event) {
                if (event.getType() == TResizeEvent.Type.WIDGET) {
                    TResizeEvent imageSize;
                    imageSize = new TResizeEvent(event.getBackend(),
                        TResizeEvent.Type.WIDGET, event.getWidth() - 2,
                        event.getHeight() - 2);
                    image.onResize(imageSize);
                }
            }
        };
        window.setAlpha(255);

        // Add the image field.  We will load frames into this.
        image = window.addImage(0, 0, window.getWidth() - 2,
            window.getHeight() - 2, new BufferedImage(720, 360,
                BufferedImage.TYPE_INT_ARGB), 0, 0);
        // Be able to see the movie no matter what size the image/window is.
        image.setScaleType(TImage.Scale.SCALE);
    }

    // Function that grabs frames from a file and displays them in image.
    private void getFrames(final File file) {
        FFmpegFrameGrabber grabber = null;
        try {
            // Make ffmpeg quiet.  If we don't do this there will be stuff
            // emitted to stderr.
            av_log_set_level(AV_LOG_QUIET);

            // Start the video decoder.
            Java2DFrameConverter conv = new Java2DFrameConverter();
            grabber = new FFmpegFrameGrabber(file);
            grabber.start();
            long lastFrameMillis = 0;

            // Ideally we would sync with the real video speed and drop
            // frames as needed.  For this example we will grab at the
            // reported fps, and (due to how TApplication runs all of its
            // invokeLaters at once) catch up to dropped frames.
            double frameRate = grabber.getVideoFrameRate();
            int fps = (int) frameRate;
            final long FRAME_TIME = 1000 / fps;

            // Get a rough frame count.
            int totalFrames = grabber.getLengthInFrames();
            int frameCount = 0;

            // Keep adding images as long as the application is running.
            while (isRunning()) {
                BufferedImage frame;
                frame = conv.convert(grabber.grabImage());
                long now = System.currentTimeMillis();
                if ((image != null) && (frame != null)) {
                    frameCount++;
                    if (now - lastFrameMillis > FRAME_TIME) {
                        invokeLater(new Runnable() {
                            public void run() {
                                image.setImage(frame);
                                doRepaint();
                            }
                        });
                        lastFrameMillis = now;
                    }
                } else if (((image != null) && (frame == null)
                        && (frameCount > 0))
                    || (frameCount >= totalFrames)
                ) {
                    // We are out of frames, the movie is over.
                    image.getWindow().setTitle(image.getWindow().getTitle() +
                        " (Completed)");
                    doRepaint();
                    break;
                }

                // Pause until is time for the next frame.
                long sleepMillis = FRAME_TIME - (now - lastFrameMillis);
                if ((sleepMillis > 0) && (sleepMillis <= (int) (1000 / fps))) {
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        // SQUASH
                    }
                }
            }
        } catch (Exception e) {
            // Show this exception to the user.
            invokeLater(new Runnable() {
                public void run() {
                    new TExceptionDialog(XtermVideoPlayer.this, e);
                }
            });
            // And exit the thread ...
        }
        if (grabber != null) {
            // Try to shut down FFmpeg.
            try {
                grabber.release();
            } catch (Exception e) {
                // Show this exception to the user.
                invokeLater(new Runnable() {
                    public void run() {
                        new TExceptionDialog(XtermVideoPlayer.this, e);
                    }
                });
            }
        }
    }

    /**
     * Show FPS.
     */
    @Override
    protected void onPreDraw() {
        menuTrayText = String.format("FPS %d", getFramesPerSecond());
    }

    // Main entry point.
    public static void main(String [] args) throws Exception {
        if (args.length == 0) {
            System.err.println("USAGE: java XtermVideoPlayer filename");
            System.exit(-1);
        }
        File file = new File(args[0]);
        if (!file.canRead()) {
            System.err.println("Cannot read from file: " + args[0]);
            System.exit(-1);
        }

        XtermVideoPlayer app = new XtermVideoPlayer(file.getName());
        // The application will spin on its thread.
        (new Thread(app)).start();

        // Frames will continue to grab on the main thread.
        app.getFrames(new File(args[0]));
    }
}
