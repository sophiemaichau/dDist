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

/**
 * A concrete server implementation, which sends and handles MyTextEvents and the like.
 * All incoming events are checked if they are 'conflicting' (same count/logical timestamp),
 * and if so, then the server fixes the offset before it broadcasts it back to the clients.
 */
public class ConcreteServer extends AbstractServer {
    private final JTextArea area;
    private int cServer;
    private ArrayList<MyTextEvent> eventHistory = new ArrayList<>();
    private Thread broadcastViewThread;


    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public synchronized void onNewConnection(int id, String ipAddress) {
        System.out.println("new connection from: " + ipAddress + " with id: " + id);
        broadcast(new UpdateViewEvent(getView()));
        sendToClient(id, new TextCopyEvent(0, area.getText(), id, cServer));
        UpdateViewEvent e = new UpdateViewEvent((ArrayList<Pair<InetAddress, Integer>>) getView().clone());
        broadcast(e);
    }

    @Override
    @SuppressWarnings("unchecked")
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

    /**
     * Applies a filter on incoming MyTextevents. This is where the offset-handling is done.
     * @param o
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object incomingEventsFilter(Object o) {
        //only apply filter on MyTextEvents
        if(o instanceof MyTextEvent){
            MyTextEvent b = (MyTextEvent) o;
            //System.out.print("received conflicting event: " + b + ", offset: " + b.getOffset() +  ", count: " + b.getCount());
            int difference = 0; //offset difference to be added to b.

            //for every element in history with same count and <= offset, add length of event to difference
            for(MyTextEvent a : eventHistory){
                if(a.getCount() == b.getCount() && a.getOffset() <= b.getOffset()){
                    if(a instanceof TextInsertEvent) {
                        difference++; //insert events have length 1
                    } else if(a instanceof TextRemoveEvent){
                        difference--; //remove events have length -1
                    }
                }
            }

            if (b instanceof TextInsertEvent) {
                TextInsertEvent clone = (TextInsertEvent) b.clone(); //clone to avoid nasty old references
                eventHistory.add(clone);
            } else if (b instanceof TextRemoveEvent) {
                TextRemoveEvent clone = (TextRemoveEvent) b.clone();
                eventHistory.add(clone);
            }
            b.setOffset(b.getOffset() + difference); //update offset
            cServer++;
            b.setCount(cServer);
            //System.out.println(" and updated it to offset: " + b.getOffset() + ", count: " + b.getCount());
            return b;
        }
        return o;
    }
}
