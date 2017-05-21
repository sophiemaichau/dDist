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

public abstract class AbstractServer {
    private ServerSocket serverSocket;
    private int port;
    private ConnectionHandler handler;
    private LinkedBlockingQueue<Object> eventQueue = new LinkedBlockingQueue<>();
    private ArrayList<Thread> handlerThreads = new ArrayList<>();

    private ArrayList<Pair<InetAddress, Integer>> view = new ArrayList<>();
    private ArrayList<Pair<ConnectionHandler, Integer>> connectionList = new ArrayList<>();

    public AbstractServer(int port) throws IOException {
        this.port = port;

    }

    public abstract void onNewConnection(int id, String ipAddress);
    public abstract void onLostConnection(String ipAddress);
    public abstract void onShutDown();
    public abstract Object incomingEventsFilter(Object o);

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

    public boolean broadcast(Object o) {

        ArrayList<Pair<ConnectionHandler, Integer>> removeList = new ArrayList<>();
        synchronized (connectionList) {
            for(Pair<ConnectionHandler, Integer> p : connectionList){
                try {
                    p.getFirst().sendObject(o);
                } catch (IOException e) {
                    System.err.println(e);
                    removeList.add(p);
                }
            }
            for (Pair<ConnectionHandler, Integer> c : removeList) {
                String ip = c.getFirst().getSocket().getInetAddress().toString();
                c.getFirst().closeConnection();
                connectionList.remove(c);
                boolean r = view.remove(new Pair<>(c.getFirst().getSocket().getInetAddress(), c.getSecond()));
                onLostConnection(ip);
            }
        }

        return true;
    }
    public ArrayList<Pair<InetAddress, Integer>> getView() {
        return view;
    }

    public void setView(ArrayList<Pair<InetAddress, Integer>> view) {
        this.view = view;
    }



    public void startListening(boolean shouldBroadcast) throws IOException {
        serverSocket = new ServerSocket(port);
        if (shouldBroadcast) {
            new Thread(() -> {
                try {
                    broadcastEvents();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        }
        int idSequencer = 0;
        System.out.println("Waiting for client on "
                + serverSocket.getInetAddress().getLocalHost().getHostAddress() + " : " + port);
        while(Thread.currentThread().isInterrupted() == false && serverSocket.isClosed() == false) {
            Socket socket = waitForConnectionFromClient();
            Pair<ConnectionHandler, Integer> pair = null;
            Pair<InetAddress, Integer> viewPair = null;
            try {
                if (socket == null) {
                    continue;
                }
                System.out.println("Connection from " + socket.getRemoteSocketAddress());
                ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
                objOutStream.flush();
                handler = new ConnectionHandler(socket, objInputStream, objOutStream);
                pair = new Pair<>(handler, idSequencer);
                viewPair = new Pair<>(handler.getSocket().getInetAddress(), idSequencer);
                connectionList.add(pair);
                view.add(viewPair);
                //System.out.println("views after new connection:");
                //System.out.println("connectionList: " + connectionList);
                //System.out.println("view: " + view);
                Thread t = new Thread(() -> {
                    incomingEvents(handler);
                });

                handlerThreads.add(t);
                t.start();
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
                try {
                    MyTextEvent textEventReceived = handler.receiveObject();
                    Object filteredEvent = incomingEventsFilter(textEventReceived);
                    eventQueue.add(filteredEvent);
                } catch (IOException e) {
                    onLostConnection(handler.getSocket().getInetAddress().toString());
                    handler.closeConnection();
                    break;
                }
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
                Object event = eventQueue.take();
                broadcast(event);
            }
        }
    }


    public boolean isReadyForConnection(){
        if (serverSocket == null) {return false;}
        return serverSocket.isBound();
    }

    public void shutdown() {
        for (Thread t : handlerThreads) {
            t.interrupt();
        }
        synchronized (connectionList) {
            for(Pair<ConnectionHandler, Integer> p : connectionList) {
                if (p.getFirst() != null) {
                    p.getFirst().closeConnection();
                }
            }
        }
        handlerThreads.clear();
        connectionList.clear();
        eventQueue.clear();
        view.clear();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        onShutDown();
        //Thread.currentThread().interrupt();
    }

}
