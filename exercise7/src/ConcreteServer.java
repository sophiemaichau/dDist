import Utilities.Pair;
import Utilities.TextCopyEvent;
import Utilities.UpdateViewEvent;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by milo on 02-05-17.
 */
public class ConcreteServer extends AbstractServer {
    private final JTextArea area;
    private Thread broadcastViewThread;


    public ConcreteServer(int port, JTextArea area) throws IOException {
        super(port);
        this.area = area;
        broadcastViewThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                UpdateViewEvent e = new UpdateViewEvent((ArrayList<Pair<InetAddress, Integer>>) getView().clone());
                broadcast(e);
            } catch (InterruptedException e) {
                System.out.println("stopped broadcasting view");
            }
        });
        broadcastViewThread.start();
    }

    @Override
    public synchronized void onNewConnection(int id, String ipAddress) {
        System.out.println("new connection from: " + ipAddress + " with id: " + id);
        sendToClient(id, new TextCopyEvent(0, area.getText(), id));
        UpdateViewEvent e = new UpdateViewEvent((ArrayList<Pair<InetAddress, Integer>>) getView().clone());
        broadcast(e);
    }

    @Override
    public synchronized void onLostConnection(String ipAddress) {
        System.out.println("lost connection with : " + ipAddress + "!");
        //send view update to clients
        UpdateViewEvent e = new UpdateViewEvent((ArrayList<Pair<InetAddress, Integer>>) getView().clone());
        broadcast(e);
    }

    @Override
    public synchronized void onShutDown() {
        System.out.println("Closing down server");
        broadcastViewThread.interrupt();
    }
}
