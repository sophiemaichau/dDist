import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.UnknownHostException;
import javax.swing.*;
import javax.swing.text.*;
import java.net.InetAddress;
import java.rmi.AccessException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Integer;

public class DistributedTextEditor extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextArea area = new JTextArea(20, 60);
	private JTextField ipaddress; // "IP address here"
	private JTextField portNumber = new JTextField("40499");
	private EventHandler er;
	private Thread ert;
	private JFileChooser dialog = new JFileChooser(System.getProperty("user.dir"));
	private String currentFile = "Untitled";
	private boolean changed = false;
	private DocumentEventCapturer dec = new DocumentEventCapturer();
	private Server server;
	private Client client;

	public DistributedTextEditor() {
		try {
			ipaddress = new JTextField(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		area.setFont(new Font("Monospaced", Font.PLAIN, 12));

		((AbstractDocument) area.getDocument()).setDocumentFilter(dec);

		Container content = getContentPane();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JScrollPane scroll1 = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		content.add(scroll1, BorderLayout.CENTER);

		content.add(ipaddress, BorderLayout.CENTER);
		content.add(portNumber, BorderLayout.CENTER);

		JMenuBar JMB = new JMenuBar();
		setJMenuBar(JMB);
		JMenu file = new JMenu("File");
		JMenu edit = new JMenu("Edit");
		JMB.add(file);
		JMB.add(edit);

		file.add(Listen);
		file.add(Connect);
		file.add(Disconnect);
		file.addSeparator();
		file.add(Save);
		file.add(SaveAs);
		file.add(Quit);

		edit.add(Copy);
		edit.add(Paste);
		edit.getItem(0).setText("Copy");
		edit.getItem(1).setText("Paste");

		Save.setEnabled(false);
		SaveAs.setEnabled(false);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		area.addKeyListener(k1);
		setTitle("Disconnected");
		setVisible(true);

		Random rn = new Random();
		int i = rn.nextInt(10000);
		System.out.println(i);
		er = new EventHandler(dec, area, Integer.toString(i), Integer.parseInt(portNumber.getText()), DistributedTextEditor.this);
		ert = new Thread(er);
		ert.start();
	}

	private KeyListener k1 = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			changed = true;
			Save.setEnabled(true);
			SaveAs.setEnabled(true);
		}
	};

	Action Listen = new AbstractAction("Listen") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			saveOld();
			area.setText("");
			try {
				server = new Server(Integer.parseInt(portNumber.getText()), DistributedTextEditor.this);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			new Thread(server).start();

			while(true) {
				if (server.isReadyForConnection()) {
					try {
						//setup RMI
						String name = "connectionList";
						RemoteList<Pair<String, Long>> connectionList = new ConnectionList<>();
						RemoteList<Pair<String, Long>> stub = (RemoteList<Pair<String, Long>>) UnicastRemoteObject.exportObject(connectionList, 0);
						Registry registry = LocateRegistry.getRegistry();
						registry.rebind(name, stub);
						System.out.println("connectionList bound");
						server.setStub(stub);

						client = new Client(ipaddress.getText(), er, Integer.parseInt(portNumber.getText()), DistributedTextEditor.this);
						new Thread(client).start();
						System.out.println("Started client");
					} catch (AccessException e1) {
						e1.printStackTrace();
					} catch (RemoteException e1) {
						e1.printStackTrace();
					} finally {
						break;
					}
				}
			}
			changed = false;
			Save.setEnabled(false);
			SaveAs.setEnabled(false);
		}
	};

	public void clientConnected() {
		setTitle("Connected to " + ipaddress.getText() + ":" + portNumber.getText());

	}

	Action Connect = new AbstractAction("Connect") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			saveOld();
			area.setText("");
			client = new Client(ipaddress.getText(), er, Integer.parseInt(portNumber.getText()), DistributedTextEditor.this);
			new Thread(client).start();
			setTitle("Trying to connect to " + ipaddress.getText() + ":" + portNumber.getText());

			changed = false;
			Save.setEnabled(false);
			SaveAs.setEnabled(false);

		}
	};

	Action Disconnect = new AbstractAction("Disconnect") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			setTitle("Disconnected");
			area.setText("");
			if (client != null) {
				client.disconnect();
			}
			if (server != null) {
				server.shutdown();
			}
		}
	};

	Action Save = new AbstractAction("Save") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if (!currentFile.equals("Untitled"))
				saveFile(currentFile);
			else
				saveFileAs();
		}
	};

	Action SaveAs = new AbstractAction("Save as...") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			saveFileAs();
		}
	};

	Action Quit = new AbstractAction("Quit") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			saveOld();
			System.exit(0);
		}
	};

	ActionMap m = area.getActionMap();

	Action Copy = m.get(DefaultEditorKit.copyAction);
	Action Paste = m.get(DefaultEditorKit.pasteAction);

	private void saveFileAs() {
		if (dialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			saveFile(dialog.getSelectedFile().getAbsolutePath());
	}

	private void saveOld() {
		if (changed) {
			if (JOptionPane.showConfirmDialog(this, "Would you like to save " + currentFile + " ?", "Save",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
				saveFile(currentFile);
		}
	}

	private void saveFile(String fileName) {
		try {
			FileWriter w = new FileWriter(fileName);
			area.write(w);
			w.close();
			currentFile = fileName;
			changed = false;
			Save.setEnabled(false);
		} catch (IOException e) {
		}
	}

	public static void main(String[] arg) {
		new DistributedTextEditor();
	}

}
