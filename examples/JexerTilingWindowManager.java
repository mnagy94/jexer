import jexer.TApplication;
import jexer.TTerminalWindow;
import jexer.TWindow;
import jexer.event.TKeypressEvent;
import jexer.event.TMenuEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.menu.TMenu;

/**
 * Implements a simple tiling window manager.  A root non-moveable
 * non-resizable terminal window is created first, which can be split
 * horizontally or vertically.  Each new window retains a reference to its
 * "parent", and upon closing resizes that parent back to its original size.
 *
 * This example shows what can be done with minimal changes to stock Jexer
 * widgets. You will quickly see that closing a "parent" tile does not cause
 * the "child" tile to resize.  You could make a real subclass of
 * TTerminalWindow that has extra fields and/or communicates more with
 * JexerTilingWindowManager to get full coverage of tile creation,
 * destruction, placement, movement, and so on.
 */
public class JexerTilingWindowManager extends TApplication {

    /**
     * Menu item: split the terminal vertically.
     */
    private static final int MENU_SPLIT_VERTICAL = 2000;

    /**
     * Menu item: split the terminal horizontally.
     */
    private static final int MENU_SPLIT_HORIZONTAL = 2001;

    /**
     * Main entry point.
     */
    public static void main(String [] args) throws Exception {
        // For this application, we must use ptypipe so that the tile shells
        // can be aware of their size.
        System.setProperty("jexer.TTerminal.ptypipe", "true");

        JexerTilingWindowManager jtwm = new JexerTilingWindowManager();
        (new Thread(jtwm)).start();
    }

    /**
     * Public constructor chooses the ECMA-48 / Xterm backend.
     */
    public JexerTilingWindowManager() throws Exception {
        super(BackendType.XTERM);

        // The stock tool menu has items for redrawing the screen, opening
        // images, and (when using the Swing backend) setting the font.
        addToolMenu();

        // We will have one menu containing a mix of new and stock commands
        TMenu tileMenu = addMenu("&Tile");

        // New commands for this example: split vertical and horizontal.
        tileMenu.addItem(MENU_SPLIT_VERTICAL, "&Vertical Split");
        tileMenu.addItem(MENU_SPLIT_HORIZONTAL, "&Horizontal Split");

        // Stock commands: a new shell with resizable window, previous, next,
        // close, and exit program.
        tileMenu.addItem(TMenu.MID_SHELL, "&Floating");
        tileMenu.addSeparator();
        tileMenu.addDefaultItem(TMenu.MID_WINDOW_PREVIOUS);
        tileMenu.addDefaultItem(TMenu.MID_WINDOW_NEXT);
        tileMenu.addDefaultItem(TMenu.MID_WINDOW_CLOSE);
        tileMenu.addSeparator();
        tileMenu.addDefaultItem(TMenu.MID_EXIT);

        // Spin up the root tile
        TTerminalWindow rootTile = makeTile(0, 0, getScreen().getWidth(),
            getDesktopBottom() - 1, null);

        // Let's add some bling!  Enable focus-follows-mouse.
        setFocusFollowsMouse(true);
    }

    /**
     * Process menu events.
     */
    @Override
    protected boolean onMenu(TMenuEvent event) {
        if (event.getId() == MENU_SPLIT_VERTICAL) {
            splitVertical();
            return true;
        }
        if (event.getId() == MENU_SPLIT_HORIZONTAL) {
            splitHorizontal();
            return true;
        }

        return super.onMenu(event);
    }

    /**
     * Perform the vertical split.
     */
    private void splitVertical() {
        TWindow window = getActiveWindow();
        if (!(window instanceof TTerminalWindow)) {
            return;
        }

        TTerminalWindow tile = (TTerminalWindow) window;
        // Give the extra column to the new tile.
        int newWidth = (tile.getWidth() + 1) / 2;
        int newY = tile.getY() - 1;
        int newX = tile.getX() + tile.getWidth() - newWidth;
        makeTile(newX, newY, newWidth, tile.getHeight(), tile);
        tile.setWidth(tile.getWidth() - newWidth);
        tile.onResize(new TResizeEvent(getBackend(), TResizeEvent.Type.WIDGET,
                tile.getWidth(), tile.getHeight()));
    }

    /**
     * Perform the horizontal split.
     */
    private void splitHorizontal() {
        TWindow window = getActiveWindow();
        if (!(window instanceof TTerminalWindow)) {
            return;
        }

        TTerminalWindow tile = (TTerminalWindow) window;
        // Give the extra row to the new tile.
        int newHeight = (tile.getHeight() + 1) / 2;
        int newY = tile.getY() - 1 + tile.getHeight() - newHeight;
        int newX = tile.getX();
        makeTile(newX, newY, tile.getWidth(), newHeight, tile);
        tile.setHeight(tile.getHeight() - newHeight);
        tile.onResize(new TResizeEvent(getBackend(), TResizeEvent.Type.WIDGET,
                tile.getWidth(), tile.getHeight()));
    }

    /**
     * Create a non-resizable non-movable terminal window.
     *
     * @param x the column number to place the top-left corner at.  0 is the
     * left-most column.
     * @param y the row number to place the top-left corner at.  0 is the
     * top-most column.
     * @param width the width of the window
     * @param height the height of the window
     * @param otherTile the other tile to resize when this window closes
     */
    private TTerminalWindow makeTile(int x, int y, int width, int height,
        final TTerminalWindow otherTile) {

        // We pass flags to disable the zoom (maximize) button, disable
        // "smart" window placement, and set the specific location.
        TTerminalWindow tile = new TTerminalWindow(this, x, y,
            TWindow.NOZOOMBOX | TWindow.ABSOLUTEXY,
            new String[] { "/bin/bash", "--login" }, true) {

            /**
             * When this terminal closes, if otherTile is defined then resize
             * it to overcover me.
             */
            @Override
            public void onClose() {
                super.onClose();

                if (otherTile != null) {
                    if (otherTile.getX() != getX()) {
                        // Undo the vertical split
                        otherTile.setX(Math.min(otherTile.getX(), getX()));
                        otherTile.setWidth(otherTile.getWidth() + getWidth());
                    }
                    if (otherTile.getY() != getY()) {
                        otherTile.setY(Math.min(otherTile.getY(), getY()));
                        otherTile.setHeight(otherTile.getHeight() + getHeight());
                    }
                    otherTile.onResize(new TResizeEvent(getBackend(),
                            TResizeEvent.Type.WIDGET,
                            otherTile.getWidth(), otherTile.getHeight()));
                }
            }

            /**
             * Prevent the user from resizing or moving this window.
             */
            @Override
            public void onMouseDown(final TMouseEvent mouse) {
                super.onMouseDown(mouse);
                stopMovements();
            }

            /**
             * Prevent the user from resizing or moving this window.
             */
            @Override
            public void onKeypress(final TKeypressEvent keypress) {
                super.onKeypress(keypress);
                stopMovements();
            }

            /**
             * Permit the user to use all of the menu items.
             */
            @Override
            public void onIdle() {
                super.onIdle();
                removeShortcutKeypress(jexer.TKeypress.kbAltT);
                removeShortcutKeypress(jexer.TKeypress.kbF6);
            }

        };

        // The initial window size was stock VT100 80x24.  Change that now,
        // and then call onResize() to notify ptypipe to set the shell's
        // window size.
        tile.setWidth(width);
        tile.setHeight(height);
        tile.onResize(new TResizeEvent(getBackend(), TResizeEvent.Type.WIDGET,
                tile.getWidth(), tile.getHeight()));

        return tile;
    }

}
