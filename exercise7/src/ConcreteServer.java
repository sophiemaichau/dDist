import Utilities.*;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

public class ConcreteServer extends AbstractServer {
    private final JTextArea area;
    private int cServer;
    private ArrayList<MyTextEvent> eventHistory = new ArrayList<>();

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
            return b;
        }
        return o;
    }
}
