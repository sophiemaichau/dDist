import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.io.IOException;

public class Client implements Runnable {

	private ConnectionHandler handler;
	private DocumentEventCapturer doc;
	private String serverName;
	private EventReplayer er;
	private Socket socket = null;
	private int port;

	public Client(String serverName, EventReplayer er, int port) {
		this.er = er;
		this.doc = doc;
		this.port = port;
		this.serverName = serverName;
	}

	public void run() {
		System.out.println("Starting client. Type CTRL-D to shut down.");
		// printLocalHostAddress();
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
	 *
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
