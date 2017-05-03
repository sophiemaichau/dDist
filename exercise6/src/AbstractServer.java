import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by milo on 02-05-17.
 */
public abstract class AbstractServer {
    private ServerSocket serverSocket;
    private int port = 40499;
    private ConnectionHandler handler;
    private LinkedBlockingQueue<MyTextEvent> eventQueue = new LinkedBlockingQueue<MyTextEvent>();
    private ArrayList<ConnectionHandler> connectionHandlerList = new ArrayList<>();

    public AbstractServer(int port) throws IOException {
        this.port = port;

    }

    public abstract void onNewConnection(ConnectionHandler connectionHandler, String ipAddress);
    public abstract void onLostConnection(String ipAddress);

    /*public boolean sendToClient(int clientID, Object data) {
        ConnectionHandler h = connectionHandlerMap.get(clientID);
        if (h == null) { return false;}
        try {
            h.sendObject(data);
        } catch (IOException e) {
            return false;
        }
        return true;
    }*/


    public void startListening() throws IOException {
        serverSocket = new ServerSocket(port);

        new Thread(() -> {
            try {
                broadcastEvents();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        while(Thread.currentThread().isInterrupted() == false) {
            System.out.println("Waiting for client on "
                    + serverSocket.getInetAddress().getLocalHost().getHostAddress() + " : " + port);
            Socket socket = waitForConnectionFromClient();
            try {
                System.out.println("Connection from " + socket.getRemoteSocketAddress());
                ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
                handler = new ConnectionHandler(socket, objInputStream, objOutStream);
                connectionHandlerList.add(handler);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            incomingEvents(handler);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                onNewConnection(handler, handler.getSocket().getInetAddress().toString());
            } catch (SocketTimeoutException e) {
                System.out.println("Socket timed out!");
                onLostConnection(handler.getSocket().getInetAddress().toString());
                handler.closeConnection();
                connectionHandlerList.remove(handler);
            } catch (IOException e) {
                onLostConnection(handler.getSocket().getInetAddress().toString());
                System.out.println("IOException!");
                handler.closeConnection();
                connectionHandlerList.remove(handler);
            }

        }
    }


    protected Socket waitForConnectionFromClient() {
        Socket res = null;
        try {
            res = serverSocket.accept();
        } catch (IOException e) {
            System.err.println(e);
        }
        return res;
    }


    public void incomingEvents(ConnectionHandler handler) throws IOException {
        while(true) {
            if(!handler.isClosed()) {
                MyTextEvent textEvent = (MyTextEvent) handler.receiveObject();
                eventQueue.add(textEvent);
            } else {
                onLostConnection(handler.getSocket().getInetAddress().toString());
                handler.closeConnection();
                break;
            }
        }
    }

    public void broadcastEvents() throws InterruptedException {
        while(true){
            if(!eventQueue.isEmpty()){
                MyTextEvent event = eventQueue.take();
                ArrayList<ConnectionHandler> removeList = new ArrayList<>();
                for(ConnectionHandler connection : connectionHandlerList){
                    try {
                        connection.sendObject(event);
                    } catch (IOException e) {
                        removeList.add(connection);
                    }
                }
                for (ConnectionHandler c : removeList) {
                    onLostConnection(c.getSocket().getInetAddress().toString());
                    c.closeConnection();
                    connectionHandlerList.remove(c);
                }
            }
        }
    }


    public boolean isReadyForConnection(){
        if (serverSocket == null) {return false;}
        return serverSocket.isBound();
    }

    public void shutdown() {
        for(ConnectionHandler handler : connectionHandlerList) {
            if (handler != null) {
                handler.closeConnection();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println(e);
        }
        Thread.currentThread().interrupt();
    }

}
