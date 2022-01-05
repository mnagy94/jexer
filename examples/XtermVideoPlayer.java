import java.awt.image.BufferedImage;
import java.io.File;
import jexer.TApplication;
import jexer.TExceptionDialog;
import jexer.TImage;
import jexer.TWindow;
import jexer.event.TResizeEvent;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

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

        // Add a single image to a window.  We will load frames into this
        // image.
        TWindow window = new TWindow(this, filename, 0, 0,
            getScreen().getWidth(), getScreen().getHeight() - 2) {
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

        image = window.addImage(0, 0, window.getWidth() - 2,
            window.getHeight() - 2, new BufferedImage(720, 360,
                BufferedImage.TYPE_INT_ARGB), 0, 0);
        image.setScaleType(TImage.Scale.SCALE);
    }

    // Function that grabs frames from a file and displays them in image.
    private void getFrames(final File file) {
        FFmpegFrameGrabber grabber = null;
        try {
            Java2DFrameConverter conv = new Java2DFrameConverter();
            grabber = new FFmpegFrameGrabber(file);
            grabber.start();
            while (isRunning()) {
                BufferedImage frame;
                frame = conv.convert(grabber.grab());
                if ((image != null) && (frame != null)) {
                    invokeLater(new Runnable() {
                        public void run() {
                            image.setImage(frame);
                            // image.setScaleType(TImage.Scale.SCALE);
                            doRepaint();
                        }
                    });
                } else if ((image != null) && (frame == null)) {
                    // Frame is null.
                }
                try {
                    // Ideally we would sync with the real video speed and
                    // drop frames as needed.  For this example we will just
                    // grab at about 20 fps.
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // SQUASH
                }
            }
        } catch (Exception e) {
            // Show this exception to the user.
            invokeLater(new Runnable() {
                public void run() {
                    new TExceptionDialog(XtermVideoPlayer.this, e);
                }
            });
            // And exit the thread
        }
        if (grabber != null) {
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

        XtermVideoPlayer app = new XtermVideoPlayer(args[0]);
        // The application will spin on its thread.
        (new Thread(app)).start();

        app.getFrames(new File(args[0]));
    }
}
