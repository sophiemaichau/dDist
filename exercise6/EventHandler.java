import javax.swing.JTextArea;

import java.awt.Color;
import java.awt.EventQueue;
import java.io.IOException;

public class EventHandler implements Runnable {

	private DocumentEventCapturer dec;
	private JTextArea area1;
	private JTextArea area2;
	private ConnectionHandler connectionHandler;

	public EventHandler(DocumentEventCapturer dec, JTextArea area1, JTextArea area2) {
		this.dec = dec;
		this.area1 = area1;
		this.area2 = area2;
	}

	/*
	 * This class implements the interface ClosedConnectionListener. 
	 */
	private class ConnectionListener implements ClosedConnectionListener {
		public void notifyClosedConnection() {
			area1.setText("");
			area2.setBackground(Color.RED);
			area2.setText("");
		}
	}

	/*
	 * Sets the ConnectionHandler object and add a ConnectionListener to it, that we use in the run method.
	 */
	public void setConnectionHandler(ConnectionHandler h) {
		connectionHandler = h;
		connectionHandler.addListener(new ConnectionListener());
		area2.setBackground(Color.WHITE);
	}

	/*
	 * If we have a connection, we listen on MyTextEvents from the given connection.
	 * We execute the MyTextEvents on the bottom JTextArea.
	 */
	public void listenOnPeerEvent() {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (connectionHandler != null && !connectionHandler.isClosed()) {
						MyTextEvent mte = null;
						try {
							// blocks until received object
							mte = (MyTextEvent) connectionHandler.receiveObject();
						} catch (IOException ex) {
							sleep(10);
							System.out.println("closing connection with server.");
							connectionHandler.closeConnection();

						}

						if (mte instanceof TextInsertEvent) {
							final TextInsertEvent tie = (TextInsertEvent) mte;
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									try {
										area2.insert(tie.getText(), tie.getOffset());
									} catch (Exception e) {
										System.err.println(e);
									}
								}
							});
						} else if (mte instanceof TextRemoveEvent) {
							final TextRemoveEvent tre = (TextRemoveEvent) mte;
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									try {
										area2.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
									} catch (Exception e) {
										System.err.println(e);
									}
								}
							});
						}
					}
				}
			}
		}).start();
	}

	/*
	 * This run method runs listenOnPeerEvent in a seperate thread and sends MyTextEvents objects through sendObject method from ConnectionHandler class.
	 * If any errors occur we close the connection through ConnectionHandler.
	 */
	public void run() {

		// runs in own thread
		listenOnPeerEvent();

		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			try {
				MyTextEvent mte = dec.take();
				if (connectionHandler != null && !connectionHandler.isClosed()) {
					connectionHandler.sendObject(mte);
				}
			} catch (IOException ex) {
				connectionHandler.closeConnection();

			} catch (Exception e) {
				e.printStackTrace();
				wasInterrupted = true;
				connectionHandler.closeConnection();

			}
		}
		System.out.println("I'm the thread running the EventHandler, now I die!");
	}

	public void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
		}
	}
}
