import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.io.IOException;

public class Client implements Runnable {

	ConnectionHandler handler;
	DocumentEventCapturer doc;
	String serverName;
	private EventReplayer er;
	private Socket socket = null;
	int port = 40499;

	public Client(String serverName, EventReplayer er, int port) {
		this.er = er;
		this.port = port;
		this.serverName = serverName;
	}

	/*
	 * If a client has connection with a server, we create a new
	 * ConnectionHandler object and sets this object with setConnectionHandler
	 * from EvenReplayer. If an error occurs, we close the connection through
	 * ConnectionHandler.
	 */
	public void run() {
		System.out.println("Starting client. Type CTRL-D to shut down.");
		socket = connectToServer(serverName);

		if (socket != null) {
			System.out.println("Connected to " + socket);
			try {
				// For sending objects to the server
				ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
				objOutStream.flush();
				handler = new ConnectionHandler(socket, objInputStream, objOutStream);
				er.setConnectionHandler(handler);
			} catch (IOException e) {
				System.err.println(e);
				if (handler != null) {
					handler.closeConnection();
				}
			}
		}
	}

	public void disconnect() {
		handler.closeConnection();
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
