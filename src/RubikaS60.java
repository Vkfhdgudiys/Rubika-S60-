import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.InputStream;

public class RubikaS60 extends MIDlet implements CommandListener {
    private Display display;
    private Form form;
    private TextField serverField;
    private StringItem statusItem;
    private Command testCommand;
    private Command exitCommand;

    public void startApp() {
        display = Display.getDisplay(this);

        if (form == null) {
            form = new Form("Rubika S60");
            form.append(new StringItem(null, "Java Edition - Prototype 0.1"));
            form.append(new StringItem(null, "Connect to Python bridge server."));

            serverField = new TextField(
                "Server URL:",
                "http://192.168.1.100:8080/",
                120,
                TextField.URL
            );

            form.append(serverField);

            statusItem = new StringItem("Status: ", "Not connected");

            form.append(statusItem);

            testCommand = new Command("Test", Command.SCREEN, 1);
            exitCommand = new Command("Exit", Command.EXIT, 2);

            form.addCommand(testCommand);
            form.addCommand(exitCommand);
            form.setCommandListener(this);
        }

        display.setCurrent(form);
    }

    public void pauseApp() {}

    public void destroyApp(boolean unconditional) {}

    public void commandAction(Command command, Displayable screen) {
        if (command == exitCommand) {
            notifyDestroyed();
        } else if (command == testCommand) {
            statusItem.setText("Connecting...");

            final String url = serverField.getString();

            Thread worker = new Thread(new Runnable() {
                public void run() {
                    testServer(url);
                }
            });

            worker.start();
        }
    }

    private void testServer(String url) {
        HttpConnection connection = null;
        InputStream input = null;

        try {
            connection = (HttpConnection) Connector.open(url);
            connection.setRequestMethod(HttpConnection.GET);

            int code = connection.getResponseCode();

            input = connection.openInputStream();

            StringBuffer body = new StringBuffer();
            int value;
            int count = 0;

            while ((value = input.read()) != -1 && count < 512) {
                body.append((char) value);
                count++;
            }

            final String result = "HTTP " + code + "\n" + body.toString();

            display.callSerially(new Runnable() {
                public void run() {
                    statusItem.setText(result);
                }
            });

        } catch (final Exception error) {
            display.callSerially(new Runnable() {
                public void run() {
                    statusItem.setText("Connection failed: " + error.toString());
                }
            });

        } finally {
            try {
                if (input != null) input.close();
            } catch (Exception ignored) {}

            try {
                if (connection != null) connection.close();
            } catch (Exception ignored) {}
        }
    }
}
