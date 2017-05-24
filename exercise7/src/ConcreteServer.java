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

@SuppressWarnings("unchecked")
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
            System.out.print("received conflicting event: " + b + ", offset: " + b.getOffset() +  ", count: " + b.getCount());
            int difference = 0;
            for(MyTextEvent a : eventHistory){
                if(a.getCount() == b.getCount() && a.getOffset() <= b.getOffset()){
                    if(a instanceof TextInsertEvent) {
                        difference++;
                    } else if(a instanceof TextRemoveEvent){
                        difference--;
                    }
                }
            }
            if (b instanceof TextInsertEvent) {
                TextInsertEvent clone = (TextInsertEvent) b.clone();
                eventHistory.add(clone);
            } else if (b instanceof TextRemoveEvent) {
                TextRemoveEvent clone = (TextRemoveEvent) b.clone();
                eventHistory.add(clone);
            }
            b.setOffset(b.getOffset() + difference);
            cServer++;
            b.setCount(cServer);
            System.out.println(" and updated it to offset: " + b.getOffset() + ", count: " + b.getCount());
            System.out.println("difference: " + difference);
            return b;
        }
        return o;
    }
}
