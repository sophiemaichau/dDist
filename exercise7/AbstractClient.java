import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by milo on 02-05-17.
 */
public abstract class AbstractClient {
    private ConnectionHandler handler;
    private Socket socket = null;
    private String serverIP;
    private int port;

    public AbstractClient() {
    }


    public boolean startAndConnectTo(String serverIP, int port) throws IOException {
        System.out.println("Starting client. Type CTRL-D to shut down.");
        socket = connectToServer(serverIP, port);
        if (socket != null) {
            ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
            objOutStream.flush();
            handler = new ConnectionHandler(socket, objInputStream, objOutStream);
            onConnect(serverIP); //call abstract method
            new Thread(() -> {
                while(true) {
                    try {
                        Object o = handler.receiveObject();
                        onReceivedFromServer(o);
                    } catch (IOException e) {
                        System.err.println(e);
                        if (handler.isClosed() == false) {
                            onLostConnection();
                        }
                        break;
                    }
                }
            }).start();
        } else {
            return false;
        }
        return true;
    }

    public abstract void onLostConnection();
    public abstract void onReceivedFromServer(Object o);
    public abstract void onConnect(String serverIP);
    public abstract void onDisconnect();

    protected boolean sendToServer(Object o) {
        try {
            handler.sendObject(o);
        } catch (IOException e) {
            onLostConnection();
            return false;
        }
        return true;
    }

    public void disconnect() {
        if (handler != null) {
            handler.closeConnection();
        }
        onDisconnect();
    }

    private Socket connectToServer(String serverName, int port) {
        Socket res = null;
        try {
            res = new Socket(serverName, port);
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }

    public int getPort() {
        return port;
    }

    public String getServerIP() {
        return serverIP;
    }
}
