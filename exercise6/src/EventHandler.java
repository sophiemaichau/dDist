import javax.swing.JTextArea;

import java.awt.Color;
import java.awt.EventQueue;
import java.io.IOException;

public class EventHandler implements Runnable {

	private String ip;
	private DocumentEventCapturer dec;
	private JTextArea area;
	private ConnectionHandler connectionHandler;

	public EventHandler(DocumentEventCapturer dec, JTextArea area, String ip) {
		this.dec = dec;
		this.area = area;
		this.ip = ip;
	}

	/*
	 * This class implements the interface ClosedConnectionListener.
	 */
	private class ConnectionListener implements ClosedConnectionListener {
		public void notifyClosedConnection() {
			area.setText("");
			area.setBackground(Color.RED);
			area.setText("");
		}
	}


	/*
	 * Sets the ConnectionHandler object and add a ConnectionListener to it, that we use in the run method.
	 */
	public void setConnectionHandler(ConnectionHandler h) {
		connectionHandler = h;
		connectionHandler.addListener(new ConnectionListener());
		area.setBackground(Color.WHITE);
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
						MyTextEvent mw = null;
						try {
							// blocks until received object
							mw = (MyTextEvent) connectionHandler.receiveObject();
								if (mw instanceof TextInsertEvent) {
									final TextInsertEvent tie = (TextInsertEvent) mw;
									EventQueue.invokeLater(new Runnable() {
										public void run() {
											try {
												dec.disabled = true;
												//area.insert(tie.getText(), tie.getOffset());
												area.append(tie.getText());
												dec.disabled = false;
											} catch (Exception e) {
												System.err.println(e);
											}
										}
									});
								} else if (mw instanceof TextRemoveEvent) {
									final TextRemoveEvent tre = (TextRemoveEvent) mw;
									EventQueue.invokeLater(new Runnable() {
										public void run() {
											try {
												dec.disabled = true;
												area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
												dec.disabled = false;
											} catch (Exception e) {
												System.err.println(e);
											}
										}
									});
								}
						} catch (IOException ex) {
							sleep(10);
							ex.printStackTrace();
							System.out.println("closing connection with server.");
							connectionHandler.closeConnection();

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
				//wasInterrupted = true;
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
