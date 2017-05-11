import Utilities.TextCopyEvent;
import Utilities.UpdateViewEvent;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by milo on 02-05-17.
 */
public class ConcreteServer extends AbstractServer {
    private final JTextArea area;


    public ConcreteServer(int port, JTextArea area) throws IOException {
        super(port);
        this.area = area;
    }

    @Override
    public void onNewConnection(int id, String ipAddress) {
        System.out.println("new connection from: " + ipAddress + " with id: " + id);
        sendToClient(id, new TextCopyEvent(0, area.getText(), id));
        System.out.println("view after new connection: " + getView());
        broadcast(new UpdateViewEvent(getView()));
    }

    @Override
    public void onLostConnection(String ipAddress) {
        System.out.println("lost connection with : " + ipAddress + "!");
        //send view update to clients
    }

    @Override
    public void onShutDown() {
        System.out.println("Closing down server");
    }
}
