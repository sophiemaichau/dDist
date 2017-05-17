import Utilities.MyTextEvent;
import Utilities.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
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
    private int port;
    private ConnectionHandler handler;
    private LinkedBlockingQueue<MyTextEvent> eventQueue = new LinkedBlockingQueue<MyTextEvent>();
    private ArrayList<Pair<InetAddress, Integer>> view = new ArrayList<>();
    private ArrayList<Pair<ConnectionHandler, Integer>> connectionList = new ArrayList<>();

    public AbstractServer(int port) throws IOException {
        this.port = port;

    }

    public abstract void onNewConnection(int id, String ipAddress);
    public abstract void onLostConnection(String ipAddress);
    public abstract void onShutDown();
    public abstract Object onBroadcastFiltered(Object o);

    public boolean sendToClient(int clientID, Object data) {
        for (Pair<ConnectionHandler, Integer> p : connectionList) {
            if (p.getSecond() == clientID) {
                try {
                    p.getFirst().sendObject(data);
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean broadcast(Object q) {
        Object o = onBroadcastFiltered(q);

        ArrayList<Pair<ConnectionHandler, Integer>> removeList = new ArrayList<>();
        for(Pair<ConnectionHandler, Integer> p : connectionList){
            try {
                System.out.println("sending " + o + " to: " + p.getSecond());
                p.getFirst().sendObject(o);
            } catch (IOException e) {
                e.printStackTrace();
                removeList.add(p);
            }
        }

        for (Pair<ConnectionHandler, Integer> c : removeList) {
            onLostConnection(c.getFirst().getSocket().getInetAddress().toString());
            c.getFirst().closeConnection();
            connectionList.remove(c);
            System.out.println("trying to remove id: " + c.getSecond());
            boolean r = view.remove(new Pair<>(c.getFirst().getSocket().getInetAddress(), c.getSecond()));
            System.out.println("removed pair: " + r);
        }

        return true;
    }
    public ArrayList<Pair<InetAddress, Integer>> getView() {
        return view;
    }

    public void setView(ArrayList<Pair<InetAddress, Integer>> view) {
        this.view = view;
    }



    public void startListening() throws IOException {
        serverSocket = new ServerSocket(port);

        new Thread(() -> {
            try {
                broadcastEvents();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        int idSequencer = 0;
        while(Thread.currentThread().isInterrupted() == false) {
            System.out.println("Waiting for client on "
                    + serverSocket.getInetAddress().getLocalHost().getHostAddress() + " : " + port);
            Socket socket = waitForConnectionFromClient();
            Pair<ConnectionHandler, Integer> pair = null;
            Pair<InetAddress, Integer> viewPair = null;

            try {
                //System.out.println("Connection from " + socket.getRemoteSocketAddress());
                ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
                handler = new ConnectionHandler(socket, objInputStream, objOutStream);
                pair = new Pair<>(handler, idSequencer);
                viewPair = new Pair<>(handler.getSocket().getInetAddress(), idSequencer);
                connectionList.add(pair);
                view.add(viewPair);
                //System.out.println("views after new connection:");
                //System.out.println("connectionList: " + connectionList);
                //System.out.println("view: " + view);
                new Thread(
                    () -> incomingEvents(handler)
                ).start();
                onNewConnection(idSequencer, handler.getSocket().getInetAddress().toString());
                idSequencer++;
            } catch (SocketTimeoutException e) {
                System.out.println("Socket timed out!");
                onLostConnection(handler.getSocket().getInetAddress().toString());
                handler.closeConnection();
                connectionList.remove(pair);
                view.remove(viewPair);
            } catch (IOException e) {
                onLostConnection(handler.getSocket().getInetAddress().toString());
                System.out.println("IOException!");
                handler.closeConnection();
                connectionList.remove(pair);
                view.remove(viewPair);
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


    public void incomingEvents(ConnectionHandler handler) {
        while(true) {
            if(!handler.isClosed()) {
                MyTextEvent textEvent = null;
                try {
                    textEvent = (MyTextEvent) handler.receiveObject();
                } catch (IOException e) {
                    onLostConnection(handler.getSocket().getInetAddress().toString());
                    handler.closeConnection();
                    break;
                }
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
                broadcast(event);
            }
        }
    }


    public boolean isReadyForConnection(){
        if (serverSocket == null) {return false;}
        return serverSocket.isBound();
    }

    public void shutdown() {
        for(Pair<ConnectionHandler, Integer> p : connectionList) {
            if (p.getFirst() != null) {
                p.getFirst().closeConnection();
            }
        }
        connectionList.clear();
        view.clear();
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println(e);
        }
        onShutDown();
        Thread.currentThread().interrupt();
    }

}
