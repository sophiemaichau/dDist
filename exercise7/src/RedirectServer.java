import Utilities.RedirectEvent;

import java.io.IOException;

public class RedirectServer extends AbstractServer {


    private String serverIp;
    private int redirectPort;

    public RedirectServer(int port, String serverIp, int redirectPort) throws IOException {
        super(port);
        this.serverIp = serverIp;
        this.redirectPort = redirectPort;
    }

    @Override
    public void onNewConnection(int id, String ipAddress) {
        //System.out.println("redirecting client to: " + ipAddress + ": " + redirectPort);
        RedirectEvent e = new RedirectEvent(serverIp, redirectPort);
        sendToClient(id, e);
    }

    @Override
    public void onLostConnection(String ipAddress) {

    }

    @Override
    public void onShutDown() {

    }

    @Override
    public Object incomingEventsFilter(Object o) {
        return null;
    }
}
