import jexer.TAction;
import jexer.TApplication;
import jexer.TDesktop;
import jexer.TTerminalWidget;
import jexer.TWidget;
import jexer.event.TKeypressEvent;
import jexer.event.TMenuEvent;
import jexer.event.TMouseEvent;
import jexer.event.TResizeEvent;
import jexer.menu.TMenu;

/**
 * Implements a simple tiling window manager.  A terminal widget is added to
 * the desktop, which can be split horizontally or vertically.  A close
 * action is provided to each window to remove the split when its shell
 * exits.
 *
 * This example shows what can be done with minimal changes to stock Jexer
 * widgets.
 */
public class JexerTilingWindowManager2 extends TApplication {

    /**
     * Menu item: split the terminal vertically.
     */
    private static final int MENU_SPLIT_VERTICAL = 2000;

    /**
     * Menu item: split the terminal horizontally.
     */
    private static final int MENU_SPLIT_HORIZONTAL = 2001;

    /**
     * Handle to the root widget.
     */
    private TWidget root = null;
    
    /**
     * Main entry point.
     */
    public static void main(String [] args) throws Exception {
        // For this application, we must use ptypipe so that the terminal
        // shells can be aware of their size.
        System.setProperty("jexer.TTerminal.ptypipe", "true");

        // Let's also suppress the status line.
        System.setProperty("jexer.hideStatusBar", "true");

        JexerTilingWindowManager2 jtwm = new JexerTilingWindowManager2();
        (new Thread(jtwm)).start();
    }

    /**
     * Public constructor chooses the ECMA-48 / Xterm backend.
     */
    public JexerTilingWindowManager2() throws Exception {
        super(BackendType.SWING);

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

        // Spin up the root terminal
        root = new TTerminalWidget(getDesktop(), 0, 0,
            getDesktop().getWidth(), getDesktop().getHeight(),
            new TAction() {
                public void DO() {
                    // TODO: if root's parent is TSplitPane, call
                    // TSplitPane.removeSplit(TWidget).
                    if (root != null) {
                        root.remove();
                    }
                }
            });
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
        // TODO
    }

    /**
     * Perform the horizontal split.
     */
    private void splitHorizontal() {
        // TODO
    }

}
