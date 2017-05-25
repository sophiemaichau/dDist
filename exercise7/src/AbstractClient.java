import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


/**
 * Representation of a generic client class. Subclasses implement the specific behaviour.
 */
public abstract class AbstractClient {
    private ConnectionHandler handler;
    private Socket socket = null;
    private String serverIP;
    private int port;
    private Thread receiveDataFromServer;


    public abstract void onLostConnection();
    public abstract void onReceivedFromServer(Object o);
    public abstract void onConnect(String serverIP);
    public abstract void onDisconnect();

    /**
     * Connect to a server
     * @param serverIP
     * @param port
     * @return
     * @throws IOException
     */
    public boolean startAndConnectTo(String serverIP, int port) throws IOException {
        System.out.println("Starting client and connecting to " + serverIP + " on port " + port);
        this.serverIP = serverIP;
        this.port = port;
        socket = connectToServer(serverIP, port);
        if (handler != null && handler.isClosed() == false) {
            handler.closeConnection();
        }
        if (socket != null) {
            ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
            handler = new ConnectionHandler(socket, objInputStream, objOutStream);
            if (handler.getSocket().isConnected() == false) {
                return false;
            }
            receiveDataFromServer = new Thread(() -> {
                while(true) {
                    try {
                        Object o = handler.receiveObject();
                        onReceivedFromServer(o);
                    } catch (IOException e) {
                        System.err.println(e);
                        if (handler != null && handler.isClosed() == false) {
                            onLostConnection();
                        }
                        break;
                    }
                }
            });
            receiveDataFromServer.start();
            onConnect(serverIP); //call abstract method
        } else {
            return false;
        }
        return true;
    }

    protected boolean sendToServer(Object o) {
        try {
            handler.sendObject(o);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void disconnect() {
        if (handler != null) {
            handler.closeConnection();
        }
        if (receiveDataFromServer != null) {
            receiveDataFromServer.interrupt();
        }
        onDisconnect();
    }

    private Socket connectToServer(String serverName, int port) throws IOException {
        Socket res = null;
        res = new Socket(serverName, port);
        return res;
    }

    public int getPort() {
        return port;
    }

    public String getServerIP() {
        return serverIP;
    }
}
