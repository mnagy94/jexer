import jexer.TApplication;

public class HelloWorld {

    public static void main(String [] args) throws Exception {
        TApplication app = new TApplication(TApplication.BackendType.XTERM);
        app.addToolMenu();
        app.addFileMenu();
        app.addWindowMenu();
        app.run();
    }
}
