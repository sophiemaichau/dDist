import javax.swing.*;

import java.awt.Color;
import java.awt.EventQueue;
import java.io.IOException;

public class EventHandler implements Runnable {

	private String ip;
	private DocumentEventCapturer dec;
	private JTextArea area;
	private ConnectionHandler connectionHandler;
	private int portNumber;
	private DistributedTextEditor distributedTextEditor;
	private Client client;
	public boolean clientClosed = false;

	public EventHandler(DocumentEventCapturer dec, JTextArea area, String ip, int portNumber, DistributedTextEditor distributedTextEditor) {
		this.dec = dec;
		this.area = area;
		this.ip = ip;
        this.portNumber = portNumber;
        this.distributedTextEditor = distributedTextEditor;
	}

    public void setClient(Client client) {
        this.client = client;
    }



    /*
     * This class implements the interface ClosedConnectionListener.
     */
	private class ConnectionListener implements ClosedConnectionListener {
		public void notifyClosedConnection() {
			/*area.setText("");
			area.setBackground(Color.RED);
			area.setText("");
			*/

			try {
                if (!clientClosed && client.getBackupStub().get(1).getIp().equals(ip)) {
                    //client.getBackupStub().remove(0);
                    System.out.println("Trying to start new server...");
                    distributedTextEditor.Listen.actionPerformed(null);
                }
			} catch (IOException e) {
				e.printStackTrace();
			}

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
												area.insert(tie.getText(), tie.getOffset());
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
								} else if(mw instanceof TextCopyEvent){
									final TextCopyEvent tce = (TextCopyEvent) mw;
									dec.disabled = true;
									area.setText(tce.getCopiedText());
									dec.disabled = false;
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

		while (true) {
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
	}

	public void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
		}
	}
}
