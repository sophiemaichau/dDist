
// File Name GreetingServer.java
import java.net.*;
import java.io.*;

public class Server extends Thread {
	private ServerSocket serverSocket;
	private int port = 40499;
	private ConnectionHandler handler;
	private EventReplayer er;

	public Server(EventReplayer er, int port) throws IOException {
		this.er = er;
		this.port = port;
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

	@SuppressWarnings("static-access")
	public void run() {
		while (Thread.currentThread().isInterrupted() == false) {
			try {
				System.out.println("Waiting for client on " + serverSocket.getInetAddress().getLocalHost().getHostAddress()
						+ " : " + serverSocket.getLocalPort());
				Socket socket = waitForConnectionFromClient();

				System.out.println("Connection from " + socket.getRemoteSocketAddress());
				DataInputStream in = new DataInputStream(socket.getInputStream());
				ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
				handler = new ConnectionHandler(socket, objInputStream, objOutStream);
				er.setConnectionHandler(handler);
			} catch (SocketTimeoutException s) {
				System.out.println("Socket timed out!");
				break;
			} catch (IOException e) {
				System.err.println(e);
				break;
			}
		}

	}

	public ConnectionHandler getConnectionHandler() {
		return handler;
	}

	public void shutdown() {
		if (handler != null) {
			handler.closeConnection();
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.err.println(e);
		}
		interrupt();
	}
}
