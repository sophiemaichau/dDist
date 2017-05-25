import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.UnknownHostException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import java.net.InetAddress;
import java.util.Random;
import java.lang.Integer;

public class DistributedTextEditor extends JFrame {
	private static final long serialVersionUID = 1L;
	public JTextArea area = new JTextArea(10, 60);
	public JTextField redirectPort;
	public JTextField ipaddress; // "IP address here"
	public JTextField portNumber;
	private String currentFile = "Untitled";
	private boolean changed = false;
	private DocumentEventCapturer dec = new DocumentEventCapturer();
	public AbstractServer server;
	public ConcreteClient client;
	public JComboBox<Integer> portNumberList;

	public DistributedTextEditor(int x, int y) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
		try {
			ipaddress = new JTextField(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		redirectPort = new JTextField();
		redirectPort.setText(Integer.toString(new Random().nextInt(9999) + 30000));
        portNumber = new JTextField("40499");
		area.setFont(new Font("Monospaced", Font.PLAIN, 12));
		portNumberList = new JComboBox<Integer>();
		addPortNumberList(portNumberList);

		((AbstractDocument) area.getDocument()).setDocumentFilter(dec);

		Container content = getContentPane();
		addComponentsToPane(content);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		area.setBackground(Color.black);
		area.setCaretColor(Color.cyan);
		area.setForeground(Color.cyan);
		portNumberList.setEditable(true);
		pack();
		area.addKeyListener(k1);
		setTitle("Disconnected");
		Dimension windowSize = new Dimension(getPreferredSize());
		Dimension screenSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
		int wdwLeft = 300 + screenSize.width / 2 - windowSize.width / 2;
		int wdwTop = screenSize.height / 2 - windowSize.height / 2;
		setLocation(x, y);
		setVisible(true);
	}


	public void addComponentsToPane(Container pane) {

		if (!(pane.getLayout() instanceof BorderLayout)) {
			pane.add(new JLabel("Container doesn't use BorderLayout!"));
			return;
		}

		JScrollPane scroll1 = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		//text area + scroll pane
		pane.add(scroll1, BorderLayout.PAGE_START);


		EmptyBorder border = new EmptyBorder(10, 10, 10, 10);
		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
		listPane.setBorder(new EmptyBorder(10, 10, 10, 10));
		JLabel p = new JLabel("port");
		p.setBorder(border);
		listPane.add(p);
		JLabel a = new JLabel("ip-address");
		a.setBorder(border);
		listPane.add(a);
		JLabel r = new JLabel("redirect port");
		r.setBorder(border);
		listPane.add(r);

		pane.add(listPane, BorderLayout.LINE_START);


		JPanel listPane1 = new JPanel();
		BoxLayout l = new BoxLayout(listPane1, BoxLayout.PAGE_AXIS);

		listPane1.setLayout(l);
		listPane1.setBorder(new EmptyBorder(10, 10, 10, 10));
		listPane1.add(portNumberList);
		listPane1.add(ipaddress);
		listPane1.add(redirectPort);

		pane.add(listPane1, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));

		JButton listenButton = new JButton("listen");
		JButton connectButton = new JButton("connect");
		JButton disconnectButton = new JButton("disconnect");

		listenButton.addActionListener(Listen);
		connectButton.addActionListener(Connect);
		disconnectButton.addActionListener(Disconnect);
		buttonPanel.add(listenButton);
		buttonPanel.add(connectButton);
		buttonPanel.add(disconnectButton);
		pane.add(buttonPanel, BorderLayout.LINE_END);
	}

	private KeyListener k1 = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			changed = true;
		}
	};

	Action Listen = new AbstractAction("Listen") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			try {
				server = new ConcreteServer(Integer.parseInt(portNumber.getText()), area);
				new Thread(() -> {
                    try {
                        setTitle("Listening on incoming connections...");
                        server.startListening(true);
					} catch (IOException e1) {
                        e1.printStackTrace();
                        setTitle("An error occurred starting the server");
                    }
                }).start();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			try {
				Thread.sleep(100);
				while(true) {
					if (server.isReadyForConnection()) {
                        client = new ConcreteClient(dec, area, new OldestFirstElectionStrategy(), DistributedTextEditor.this);
						try {
							client.startAndConnectTo(ipaddress.getText(), Integer.parseInt(portNumber.getText()));
							break;
						} catch (IOException e1) {
							e1.printStackTrace();
							break;
						}
					}
				}
                setTitle("Listening on incoming connections...");
			} catch (InterruptedException e1) {
				System.err.println(e1);
			}
			changed = false;
		}
	};

	public void clientConnectedUpdateText() {
		setTitle("Connected to " + ipaddress.getText() + ":" + portNumber.getText());

	}

	public void clientDisconnectedUpdateText() {
        setTitle("Disconnected");

    }

	public void serverStartedUpdateText() {
		setTitle("Listening on incoming connections...");
	}

	public boolean failedConnect = true ;
	Action Connect = new AbstractAction("Connect") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			setTitle("Trying to connect to " + ipaddress.getText() + ":" + portNumber.getText());
			client = new ConcreteClient(dec, area, new OldestFirstElectionStrategy(), DistributedTextEditor.this);
            try {
                failedConnect = !client.startAndConnectTo(ipaddress.getText(), Integer.parseInt(portNumber.getText()));
			} catch (IOException e1) {
				failedConnect = true;
				System.out.println("failedConnect: " + failedConnect);
				System.err.println(e1);
			}
		}
	};

	Action Disconnect = new AbstractAction("Disconnect") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			setTitle("Disconnected");
			if (client != null) {
				client.disconnect();
			}
			if (server != null) {
				server.shutdown();
			}
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
		} catch (IOException e) {
		}
	}

	public static void main(String[] arg) {
		DistributedTextEditor a;
		DistributedTextEditor b;
		DistributedTextEditor c;
		DistributedTextEditor d;
		if (arg.length <= 1) {
			try {
				if (arg.length == 0 || arg[0].equals("1")) {
					a = new DistributedTextEditor(20, 0);
				} else if (arg[0].equals("2")) {
					a = new DistributedTextEditor(20, 0);
					b = new DistributedTextEditor(600, 0);
					Thread.sleep(100);

				} else if (arg[0].equals("3")) {
					a = new DistributedTextEditor(20, 0);
					b = new DistributedTextEditor(600, 0);
					c = new DistributedTextEditor(1200, 0);

					Thread.sleep(100);
				} else if (arg[0].equals("4")) {
					a = new DistributedTextEditor(20, 0);
					b = new DistributedTextEditor(600, 0);
					c = new DistributedTextEditor(1200, 0);
					d = new DistributedTextEditor(20, 500);

					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				System.err.println(e);
			}


		} else {
			new DistributedTextEditor(Integer.parseInt(arg[0]), Integer.parseInt(arg[1]));
		}
	}

	public void addPortNumberList(JComboBox<Integer> portNumberList){
		for(int i=0; i < 10; i++){
			portNumberList.addItem(40489 + i);
		}
	}

}
