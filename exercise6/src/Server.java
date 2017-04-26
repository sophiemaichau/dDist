import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Server extends Thread {
	private ServerSocket serverSocket;
	private int port = 40499;
	private ConnectionHandler handler;
	private EventHandler eventHandler;
	private DistributedTextEditor frame;
	private LinkedBlockingQueue<MyTextEvent> eventQueue = new LinkedBlockingQueue<MyTextEvent>();
	private ArrayList<ConnectionHandler> connectionList = new ArrayList<>();

	public Server(int port, DistributedTextEditor frame) throws IOException {
		this.port = port;
		this.frame = frame;
		serverSocket = new ServerSocket(port);
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
				} catch (IOException e) {
					e.printStackTrace();
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
				connectionList.add(handler);
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
				connectionList.remove(handler);
				break;
			} catch (IOException e) {
				System.err.println(e);
				handler.closeConnection();
				connectionList.remove(handler);
				break;
			}
		}
	}

	public void incomingEvents(ConnectionHandler handler) throws IOException {
		while(true) {
			if(!handler.isClosed()) {
				MyTextEvent textEvent = (MyTextEvent) handler.receiveObject();
				eventQueue.add(textEvent);
			}
		}
	}

	public void broadcastEvents() throws IOException, InterruptedException {
		while(true){
			if(!eventQueue.isEmpty()){
				MyTextEvent event = eventQueue.take();
				for(ConnectionHandler connection : connectionList){
					if(!connection.isClosed()) {
						connection.sendObject(event);
					} else {
						connectionList.remove(connection);
					}
				}
			}
		}
	}


	public boolean isReadyForConnection(){
		return serverSocket.isBound();
	}

	public void shutdown() {
		for(ConnectionHandler handler : connectionList) {
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
}
