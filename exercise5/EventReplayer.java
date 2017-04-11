import javax.swing.JTextArea;

import java.awt.Color;
import java.awt.EventQueue;
import java.io.IOException;
import java.awt.Color;
/**
*
* Takes the event recorded by the DocumentEventCapturer and replays
* them in a JTextArea. The delay of 1 sec is only to make the individual
* steps in the reply visible to humans.
*
* @author Jesper Buus Nielsen
*
*/
public class EventReplayer implements Runnable {

	private DocumentEventCapturer dec;
	private JTextArea area1;
	private JTextArea area2;
	private ConnectionHandler connectionHandler;
	public EventReplayer(DocumentEventCapturer dec, JTextArea area1, JTextArea area2) {
		this.dec = dec;
		this.area1 = area1;
		this.area2 = area2;
	}

	private class ConnectionListener implements ClosedConnectionListener {
		public void notifyClosedConnection() {
			area1.setText("");
			area2.setBackground(Color.RED);
			area2.setText("");
		}
	}

	public void setConnectionHandler(ConnectionHandler h) {
		connectionHandler = h;
		connectionHandler.addListener(new ConnectionListener());
		area2.setBackground(Color.WHITE);
	}

	public void listenOnPeerEvent() {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (connectionHandler != null && !connectionHandler.isClosed()) {
					Object mte = null;
					try {
						//blocks until received object
						mte = connectionHandler.receiveObject();
					} catch (IOException ex) {
						sleep(10);
						System.out.println("closing connection with server.");
						connectionHandler.closeConnection();

					}
					
					if(mte instanceof JTextArea){
						JTextArea a = (JTextArea)mte;
						area2.setText("HELLO AGAIN");
					}
					
					mte = (MyTextEvent)mte;
					
					if (mte instanceof TextInsertEvent) {
						final TextInsertEvent tie = (TextInsertEvent)mte;
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
						final TextRemoveEvent tre = (TextRemoveEvent)mte;
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								try {
									area2.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
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

	public void run() {

		//runs in own thread
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
		System.out.println("I'm the thread running the EventReplayer, now I die!");
	}

	public void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch(InterruptedException e) {
		}
	}
}
