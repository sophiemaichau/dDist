import javax.swing.JTextArea;
import java.awt.EventQueue;
import java.io.IOException;
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
	private JTextArea area;
	private ConnectionHandler connectionHandler;
	public EventReplayer(DocumentEventCapturer dec, JTextArea area) {
		this.dec = dec;
		this.area = area;
	}

	public void setConnectionHandler(ConnectionHandler h) {
		connectionHandler = h;
	}

	public void listenOnPeerEvent() {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (connectionHandler != null && !connectionHandler.isClosed()) {
					MyTextEvent mte = null;
					try {
						//blocks until received object
						mte = (MyTextEvent) connectionHandler.receiveObject();
					} catch (IOException ex) {
						sleep(10);
						System.out.println("closing connection with server.");
						connectionHandler.closeConnection();
					}
					if (mte instanceof TextInsertEvent) {
						final TextInsertEvent tie = (TextInsertEvent)mte;
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								try {
									area.insert(tie.getText(), tie.getOffset());
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
									area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
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
