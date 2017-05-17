import Utilities.MyTextEvent;
import Utilities.TextCopyEvent;
import Utilities.UpdateViewEvent;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by milo on 02-05-17.
 */
public class ConcreteServer extends AbstractServer {
    private final JTextArea area;
    private int cServer;

    public ConcreteServer(int port, JTextArea area) throws IOException {
        super(port);
        this.area = area;
        cServer = 0;
    }

    @Override
    public void onNewConnection(int id, String ipAddress) {
        System.out.println("new connection from: " + ipAddress + " with id: " + id);
        sendToClient(id, new TextCopyEvent(0, area.getText(), id, cServer));
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

    @Override
    public Object onBroadcastFiltered(Object o) {
        if(o instanceof MyTextEvent) {
            MyTextEvent m = (MyTextEvent) o;
            m.setCount(cServer);
            System.out.println("countServer: " + cServer);
            cServer++;
            return m;
        } else {
            return o;
        }
    }
}
