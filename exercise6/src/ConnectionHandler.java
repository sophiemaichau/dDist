import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Handles sending and receiving objects to a particular socket.
 */
public class ConnectionHandler {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean isClosed;
    private ArrayList<ClosedConnectionListener> listeners = new ArrayList<>();

    public ConnectionHandler(Socket s, ObjectInputStream in, ObjectOutputStream out) {
        this.in = in;
        this.out = out;
        this.socket = s;
        isClosed = false;
    }

    /*
    * Send object to socket
    */
    public <E> void sendObject(E o) throws IOException {
        out.writeObject(o);
    }

    /*
    * Receive object from socket. Blocks until received.
    */
    @SuppressWarnings("unchecked")
    public <E> E receiveObject() throws IOException {
        E o;
        try {
            while ((o = (E) in.readObject()) != null) {
                System.out.println("received object: " + o);
                return o;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isClosed() {
        return isClosed;
    }

    /*
    * Properly closes connection and corresponding streams and notifies listeners.
    */
    public void closeConnection() {
        try {
            isClosed = true;
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            notifyListeners();
        }
    }

    public void addListener(ClosedConnectionListener e) {
        listeners.add(e);
    }

    private void notifyListeners() {
        for (ClosedConnectionListener e : listeners) {
            e.notifyClosedConnection();
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean equal(Object o) {
        if (o instanceof ConnectionHandler) {
            return o.toString().equals(this.toString());
        } else { return false;}
    }
}
