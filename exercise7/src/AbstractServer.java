import Utilities.MyTextEvent;
import Utilities.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Representation of a generic server class.
 */
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


    /**
     *  callbacks that are to be implemented in subclasses
     */
    public abstract void onNewConnection(int id, String ipAddress);
    public abstract void onLostConnection(String ipAddress);
    public abstract void onShutDown();
    public abstract Object incomingEventsFilter(Object o);

    /**
     * send an object to a client with a certain id.
     */
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

    /**
     * broadcast an object to all clients.
     */
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
            //remove all clients from view for which the sendObject method failed
            for (Pair<ConnectionHandler, Integer> c : removeList) {
                String ip = c.getFirst().getSocket().getInetAddress().toString();
                c.getFirst().closeConnection();
                connectionList.remove(c);
                view.remove(new Pair<>(c.getFirst().getSocket().getInetAddress(), c.getSecond()));
                onLostConnection(ip);
            }
        }

        return true;
    }
    public ArrayList<Pair<InetAddress, Integer>> getView() {
        return view;
    }


    /**
     * start listening for events from clients and then broadcast them back to all clients.
     * @param shouldBroadcast
     * @throws IOException
     */
    public void startListening(boolean shouldBroadcast) throws IOException {
        serverSocket = new ServerSocket(port);
        if (shouldBroadcast) {
            //start broadcasting events in new thread.
            new Thread(() -> {
                try {
                    broadcastEvents();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        }
        int idSequencer = 0; //id assigned to clients
        System.out.println("Waiting for client on " + serverSocket.getInetAddress().getLocalHost().getHostAddress()
                +  " : " + port);
        while(Thread.currentThread().isInterrupted() == false && serverSocket.isClosed() == false) {
            Socket socket = waitForConnectionFromClient();
            Pair<ConnectionHandler, Integer> pair = null;
            Pair<InetAddress, Integer> viewPair = null;
            try {
                if (socket == null) {
                    continue;
                }
                ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
                objOutStream.flush();
                //make new handler object for new connection, and add to view list.
                handler = new ConnectionHandler(socket, objInputStream, objOutStream);
                pair = new Pair<>(handler, idSequencer);
                viewPair = new Pair<>(handler.getSocket().getInetAddress(), idSequencer);
                connectionList.add(pair);
                view.add(viewPair);
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

    /**
     * add incoming events to eventQueue after applying the abstract filter method
     * @param handler
     */
    public void incomingEvents(ConnectionHandler handler) {
        while(true) {
            if(!handler.isClosed()) {
                try {
                    Object textEventReceived = handler.receiveObject();
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
        //acquire lock on connectionList and then close all connections in list
        synchronized (connectionList) {
            for(Pair<ConnectionHandler, Integer> p : connectionList) {
                if (p.getFirst() != null) {
                    p.getFirst().closeConnection();
                }
            }
        }
        //clear everything
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
    }

    public String getServerIpAddress() throws UnknownHostException {
        return serverSocket.getInetAddress().getLocalHost().getHostAddress();
    }

}
