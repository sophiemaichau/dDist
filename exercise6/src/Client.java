import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client implements Runnable {

	private ConnectionHandler handler;
	private String serverIP;
	private EventHandler eventHandler;
	private Socket socket = null;
	private int port;
	DistributedTextEditor frame;

	public Client(String serverIP, EventHandler er, int port, DistributedTextEditor frame) {
		this.frame = frame;
		this.eventHandler = er;
		this.port = port;
		this.serverIP = serverIP;
	}

	/*
	 * If a client has connection with a server, we create a new
	 * ConnectionHandler object and sets this object with setConnectionHandler
	 * from EvenHandler. If an error occurs, we close the connection through
	 * ConnectionHandler.
	 */
	public void run() {
		System.out.println("Starting client. Type CTRL-D to shut down.");
		socket = connectToServer(serverIP);
		if (socket != null) {
			try {
				frame.clientConnected();
				RemoteList<Pair<String, Long>> stub = setupRMI(serverIP);
				stub.add(new Pair<>(socket.getInetAddress().getLocalHost().getHostAddress().toString(), System.currentTimeMillis()));
				System.out.println(stub.prettyToString());
				// For sending objects to the server
				ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
				objOutStream.flush();
				handler = new ConnectionHandler(socket, objInputStream, objOutStream);
				eventHandler.setConnectionHandler(handler);
			} catch (IOException e) {
				System.err.println(e);
				disconnect();
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		}
	}

	private RemoteList<Pair<String,Long>> setupRMI(String ip) throws RemoteException, NotBoundException {
		String name = "connectionList";
		Registry registry = LocateRegistry.getRegistry(ip);
		return (RemoteList<Pair<String, Long>>) registry.lookup(name);
	}

	public void disconnect() {
		if (handler != null) {
			handler.closeConnection();
		}
	}

	public ConnectionHandler getConnectionHandler() {
		return handler;
	}

	/**
	 * Connects to the server on IP address serverName and port number
	 * portNumber.
	 */
	protected Socket connectToServer(String serverName) {
		Socket res = null;
		try {
			res = new Socket(serverName, port);
		} catch (IOException e) {
			// We return null on IOExceptions
		}
		return res;
	}
}
