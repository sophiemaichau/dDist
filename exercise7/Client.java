import java.io.*;
import java.net.Socket;
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
	private DistributedTextEditor frame;
    private RemoteList<Pair<String, Long>> backupStub = null;

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
				//add eventlistener


				frame.clientConnectedUpdateText();

				RemoteList<Pair<String, Long>> stub = setupRMI(serverIP);
				stub.add(new Pair<>(socket.getInetAddress().getLocalHost().getHostAddress().toString(), System.currentTimeMillis()));
				backupStub = deepCopy(stub);
				// For sending objects to the server
				ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
				objOutStream.flush();
				handler = new ConnectionHandler(socket, objInputStream, objOutStream);
				eventHandler.setConnectionHandler(handler);
				eventHandler.clientClosed = false;
				eventHandler.setClient(this);
				new Thread(new Runnable(){
					@Override
					public void run() {
						try {
							while(true) {
								if(!handler.isClosed()) {
									stub.prettyToString();
									backupStub = deepCopy(stub);
								}
								Thread.sleep(10000);
							}
						} catch (RemoteException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
				}).start();
			} catch (IOException e) {
				System.err.println(e);
				disconnect();
			} catch (NotBoundException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private RemoteList<Pair<String,Long>> deepCopy(RemoteList<Pair<String, Long>> stub) throws IOException, ClassNotFoundException {
		// Convert stub to a stream of bytes
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(stub);
		oos.flush();
		oos.close();
		byte[] byteData = bos.toByteArray();

		// Restore copy of stub from a stream of bytes
		ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
		return (RemoteList<Pair<String, Long>>) new ObjectInputStream(bais).readObject();
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

    public RemoteList<Pair<String, Long>> getBackupStub() {
        return backupStub;
    }

    public ConnectionHandler getHandler() {
        return handler;
    }

    public void setHandler(ConnectionHandler handler) {
        this.handler = handler;
    }
}
