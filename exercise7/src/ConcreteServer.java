import Utilities.*;
import Utilities.Pair;
import Utilities.TextCopyEvent;
import Utilities.UpdateViewEvent;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.net.InetAddress;
import java.util.ArrayList;

public class ConcreteServer extends AbstractServer {
    private final JTextArea area;
    private int cServer;
    private ArrayList<MyTextEvent> eventHistory = new ArrayList<>();
    private Thread broadcastViewThread;


    public ConcreteServer(int port, JTextArea area) throws IOException {
        super(port);
        this.area = area;
        cServer = 0;
        broadcastViewThread = new Thread(() -> {
            try {
                while(true) {
                    Thread.sleep(3000);
                    UpdateViewEvent e = new UpdateViewEvent((ArrayList<Pair<InetAddress, Integer>>) getView().clone());
                    broadcast(e);
                }
            } catch (InterruptedException e) {
            }
        });
        broadcastViewThread.start();
    }

    @Override
    public synchronized void onNewConnection(int id, String ipAddress) {
        System.out.println("new connection from: " + ipAddress + " with id: " + id);
        broadcast(new UpdateViewEvent(getView()));
        sendToClient(id, new TextCopyEvent(0, area.getText(), id, cServer));
        UpdateViewEvent e = new UpdateViewEvent((ArrayList<Pair<InetAddress, Integer>>) getView().clone());
        broadcast(e);
    }

    @Override
    public synchronized void onLostConnection(String ipAddress) {
        System.out.println("lost connection with : " + ipAddress + "!");
        //send view update to clients
        UpdateViewEvent e = new UpdateViewEvent((ArrayList<Pair<InetAddress, Integer>>) getView().clone());
        if(e.getView().size() > 0 && e.getView().get(0).getSecond() == 0) {
            broadcast(e);
        }
    }

    @Override
    public synchronized void onShutDown() {
        System.out.println("Closing down server");
        broadcastViewThread.interrupt();
    }

    @Override
    public Object incomingEventsFilter(Object o) {
        if(o instanceof MyTextEvent){
            MyTextEvent b = (MyTextEvent) o;
            for(int i = eventHistory.size() - 1; i > 0; i--){
                    MyTextEvent a = eventHistory.get(i);
                    if(a.getCount() == b.getCount() && a.getOffset() <= b.getOffset()){
                        if(a instanceof TextInsertEvent) {
                            b.setOffset(b.getOffset() + 1);
                        } else if(a instanceof TextRemoveEvent){
                            b.setOffset(b.getOffset() - 1);
                        }
                    }
                }
            eventHistory.add(b);
            cServer++;
            b.setCount(cServer);
            return b;
        }
        return o;
    }
}
