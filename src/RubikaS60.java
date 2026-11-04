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
    private StringItem nameItem;
    private StringItem versionItem;
    private StringItem messageItem;
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

            statusItem = new StringItem("Connection: ", "Not connected");
nameItem = new StringItem("Server: ", "Unknown");
versionItem = new StringItem("Version: ", "-");
messageItem = new StringItem("Message: ", "-");

form.append(statusItem);
form.append(nameItem);
form.append(versionItem);
form.append(messageItem);

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

            StringBuffer resultBuffer = new StringBuffer();
resultBuffer.append("HTTP ");
resultBuffer.append(code);
resultBuffer.append('\n');
resultBuffer.append(body.toString());
final String result = resultBuffer.toString();

            final String serverName = getJsonValue(body.toString(), "name");
final String serverVersion = getJsonValue(body.toString(), "version");
final String serverStatus = getJsonValue(body.toString(), "status");
final String serverMessage = getJsonValue(body.toString(), "message");

final String connectionStatus =
    code == 200 ? "Online" : "HTTP " + code;

display.callSerially(new Runnable() {
    public void run() {
        statusItem.setText(connectionStatus);
        nameItem.setText(serverName);
        versionItem.setText(serverVersion);
        messageItem.setText(serverMessage);
    }
});

        } catch (final Exception error) {
            display.callSerially(new Runnable() {
                public void run() {
                    StringBuffer errorBuffer = new StringBuffer();
errorBuffer.append("Connection failed: ");
errorBuffer.append(error.toString());
final String result = errorBuffer.toString();
statusItem.setText(result);
nameItem.setText("Unknown");
versionItem.setText("-");
messageItem.setText(result);
                }
            });

        } finally {
            try {
                if (input != null) input.close();
            } catch (Exception ignored) {}

            try {
                if (connection != null) connection.close();
            } catch (Exception ignored) {}
        private String getJsonValue(String json, String key) {
    String search = "\"" + key + "\"";
    int start = json.indexOf(search);

    if (start < 0) return "-";

    start = json.indexOf(':', start);
    if (start < 0) return "-";

    start++;

    while (start < json.length() &&
           json.charAt(start) == ' ') {
        start++;
    }

    if (start >= json.length()) return "-";

    if (json.charAt(start) == '"') {
        start++;
        int end = json.indexOf('"', start);

        if (end < 0) return "-";

        return json.substring(start, end);
    }

    int end = json.indexOf(',', start);
    if (end < 0) {
        end = json.indexOf('}', start);
    }

    if (end < 0) return "-";

    return json.substring(start, end).trim();
        }
