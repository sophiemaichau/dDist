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

    public ConnectionHandler(Socket s, ObjectInputStream in, ObjectOutputStream out) {
        this.in = in;
        this.out = out;
        this.socket = s;
        isClosed = false;
    }

    /**
     * Send object to socket
     * @param o
     * @param <E>
     * @throws IOException
     */
    public <E> void sendObject(E o) throws IOException {
        out.writeObject(o);
    }

    /**
     * Receive object from socket. Blocks until received.
     * @param <E>
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public <E> E receiveObject() throws IOException {
        E o;
        try {
            while ((o = (E) in.readObject()) != null) {
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

    /**
     * Properly closes connection and corresponding streams and notifies listeners.
     */
    public void closeConnection() {
        try {
            isClosed = true;
            in.close();
            out.flush();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }


    public Socket getSocket() {
        return socket;
    }

}
