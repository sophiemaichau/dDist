import java.net.*;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Server extends Thread {
	private ServerSocket serverSocket;
	private int port = 40499;
	private ConnectionHandler handler;
	private EventHandler eventHandler;
	private DistributedTextEditor frame;
	private LinkedBlockingQueue<MyTextEvent> eventQueue = new LinkedBlockingQueue<MyTextEvent>();
	private ArrayList<ConnectionHandler> connectionHandlerList = new ArrayList<>();
	private RemoteList<Pair<String, Long>> stub;

	public Server(int port, DistributedTextEditor frame) throws IOException {
		this.port = port;
		this.frame = frame;
		serverSocket = new ServerSocket(port);
		try {
			stub = setupRMI();
		} catch (RemoteException e) {
			e.printStackTrace();
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

	/*
	 * Starts a server and blocks until a connection from a client succeeds.
	 * Furthermore it creates a new ConnectionHandler object, when we have a
	 * connection to a client. Then we set the ConnectionHandler with the method
	 * setConnectopnHandler from EventHandler class.
	 */
	@SuppressWarnings("static-access")
	public void run() {

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					broadcastEvents();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		while (Thread.currentThread().isInterrupted() == false) {
			try {
				System.out.println("Waiting for client on "
						+ serverSocket.getInetAddress().getLocalHost().getHostAddress() + " : " + port);
				frame.setTitle("I'm listening on " + " : " + serverSocket.getInetAddress().getLocalHost().getHostAddress()
						+ " : " + port);
				Socket socket = waitForConnectionFromClient();
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
			} catch (SocketTimeoutException s) {
				System.out.println("Socket timed out!");
				handler.closeConnection();
				connectionHandlerList.remove(handler);
				break;
			} catch (IOException e) {
				System.err.println(e);
				handler.closeConnection();
				connectionHandlerList.remove(handler);
				break;
			}
		}
	}

	private RemoteList<Pair<String,Long>> setupRMI() throws RemoteException {
		//setup RMI
		String name = "connectionList";
		RemoteList<Pair<String, Long>> connectionList = new ConnectionList<>();
		RemoteList<Pair<String, Long>> stub = (RemoteList<Pair<String, Long>>) UnicastRemoteObject.exportObject(connectionList, 0);
		Registry registry = LocateRegistry.getRegistry();
		registry.rebind(name, stub);
		System.out.println("connectionList bound");
		return stub;
	}


	public void incomingEvents(ConnectionHandler handler) throws IOException {
		while(true) {
			if(!handler.isClosed()) {
				MyTextEvent textEvent = (MyTextEvent) handler.receiveObject();
				eventQueue.add(textEvent);
			}
		}
	}

	public void broadcastEvents() throws InterruptedException {
		while(true){
			if(!eventQueue.isEmpty()){
				MyTextEvent event = eventQueue.take();
				ArrayList<ConnectionHandler> removeList = new ArrayList<>();
				System.out.println("taking event out of queue: " + event + "and sending to: " + connectionHandlerList);
				for(ConnectionHandler connection : connectionHandlerList){
					System.out.println("in item: " + connection);
					try {
						connection.sendObject(event);
						System.out.println("succesfully sent object to: " + connection);
					} catch (IOException e) {
						e.printStackTrace();
						removeList.add(connection);
					}
				}
				for (ConnectionHandler c : removeList) {
					connectionHandlerList.remove(c);
				}
			}
		}
	}


	public boolean isReadyForConnection(){
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
		interrupt();
	}

	public void replaceStub(RemoteList<Pair<String, Long>> list) throws RemoteException {
		stub.clear();
		for (int i = 0; i < list.size() - 1; i++) {
			stub.add(list.get(i));
		}
	}
}
