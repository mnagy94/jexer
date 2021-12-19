import jexer.TAction;
import jexer.TApplication;
import jexer.TDesktop;
import jexer.TTerminalWidget;
import jexer.TSplitPane;
import jexer.TWidget;
import jexer.event.TMenuEvent;
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
     * Menu item: recreate the root terminal.
     */
    private static final int MENU_RESPAWN_ROOT = 2002;

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

        final JexerTilingWindowManager2 jtwm = new JexerTilingWindowManager2();
        (new Thread(jtwm)).start();

        jtwm.invokeLater(new Runnable() {
            public void run() {
                // Spin up the root terminal
                jtwm.createRootTerminal();
            }
        });
    }

    /**
     * Public constructor chooses the ECMA-48 / Xterm backend.
     */
    public JexerTilingWindowManager2() throws Exception {
        super(BackendType.XTERM);

        // The stock tool menu has items for redrawing the screen, opening
        // images, and (when using the Swing backend) setting the font.
        addToolMenu();

        // We will have one menu containing a mix of new and stock commands
        TMenu tileMenu = addMenu("&Tile");

        // New commands for this example: split vertical and horizontal.
        tileMenu.addItem(MENU_SPLIT_VERTICAL, "&Vertical Split");
        tileMenu.addItem(MENU_SPLIT_HORIZONTAL, "&Horizontal Split");
        tileMenu.addItem(MENU_RESPAWN_ROOT, "&Respawn Root Terminal");

        // Stock commands: a new shell with resizable window, and exit
        // program.
        tileMenu.addSeparator();
        tileMenu.addItem(TMenu.MID_SHELL, "&New Windowed Terminal");
        tileMenu.addSeparator();
        tileMenu.addDefaultItem(TMenu.MID_EXIT);

        // TTerminalWidget can request the text-block mouse pointer be
        // suppressed, but the default TDesktop will ignore it.  Let's set a
        // new TDesktop to pass that mouse pointer visibility option to
        // TApplication.
        setDesktop(new TDesktop(this) {
            @Override
            public boolean hasHiddenMouse() {
                TWidget active = getActiveChild();
                if (active instanceof TTerminalWidget) {
                    return ((TTerminalWidget) active).hasHiddenMouse();
                }
                return false;
            }
        });

    }

    /**
     * Process menu events.
     */
    @Override
    protected boolean onMenu(TMenuEvent event) {
        TWidget active = getDesktop().getActiveChild();
        TSplitPane split = null;

        switch (event.getId()) {
        case MENU_RESPAWN_ROOT:
            assert (root == null);
            createRootTerminal();
            return true;

        case MENU_SPLIT_VERTICAL:
            if (root == null) {
                assert (getDesktop().getActiveChild() == null);
                createRootTerminal();
                return true;
            }
            split = active.splitVertical(false, createTerminal());
            if (active == root) {
                root = split;
            }
            return true;

        case MENU_SPLIT_HORIZONTAL:
            if (root == null) {
                assert (getDesktop().getActiveChild() == null);
                createRootTerminal();
                return true;
            }
            split = active.splitHorizontal(false, createTerminal());
            if (active == root) {
                root = split;
            }
            return true;

        default:
            return super.onMenu(event);
        }

    }

    /**
     * Create the root terminal.
     */
    private void createRootTerminal() {
        assert (root == null);
        disableMenuItem(MENU_RESPAWN_ROOT);
        enableMenuItem(MENU_SPLIT_VERTICAL);
        enableMenuItem(MENU_SPLIT_HORIZONTAL);
        root = createTerminal();
    }

    /**
     * Create a new terminal.
     *
     * @return the new terminal
     */
    private TWidget createTerminal() {
        return new TTerminalWidget(getDesktop(), 0, 0,
            getDesktop().getWidth(), getDesktop().getHeight(),
            new TAction() {
                public void DO() {
                    if (source.getParent() instanceof TSplitPane) {
                        ((TSplitPane) source.getParent()).removeSplit(source,
                            true);
                    } else {
                        source.getApplication().enableMenuItem(
                                MENU_RESPAWN_ROOT);
                        source.getApplication().disableMenuItem(
                                MENU_SPLIT_VERTICAL);
                        source.getApplication().disableMenuItem(
                                MENU_SPLIT_HORIZONTAL);
                        source.remove();
                        root = null;
                    }
                }
            });
    }

}
