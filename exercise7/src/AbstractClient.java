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
    private Thread receiveDataFromServer;

    public AbstractClient() {
    }


    public boolean startAndConnectTo(String serverIP, int port) throws IOException {
        socket = null;
        System.out.println("Starting client. Type CTRL-D to shut down.");
        this.serverIP = serverIP;
        this.port = port;
        socket = connectToServer(serverIP, port);
        if (socket != null) {
            ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
            objOutStream.flush();
            handler = new ConnectionHandler(socket, objInputStream, objOutStream);
            if (handler.getSocket().isConnected() == false) {
                return false;
            }
            onConnect(serverIP); //call abstract method
            receiveDataFromServer = new Thread(() -> {
                while(true) {
                    try {
                        Object o = handler.receiveObject();
                        onReceivedFromServer(o);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });
            receiveDataFromServer.start();
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
            return false;
        }
        return true;
    }

    public void disconnect() {
        receiveDataFromServer.interrupt();
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
            e.printStackTrace();
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
