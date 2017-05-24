import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.UnknownHostException;
import javax.swing.*;
import javax.swing.text.*;
import java.net.InetAddress;
import java.util.Random;
import java.lang.Integer;

public class DistributedTextEditor extends JFrame {
	private static final long serialVersionUID = 1L;
	public JTextArea area = new JTextArea(18, 75);
	public JTextField redirectPort;
	public JTextField ipaddress; // "IP address here"
	public JTextField portNumber;
	private JFileChooser dialog = new JFileChooser(System.getProperty("user.dir"));
	private String currentFile = "Untitled";
	private boolean changed = false;
	private DocumentEventCapturer dec = new DocumentEventCapturer();
	public AbstractServer server;
	public ConcreteClient client;

	public DistributedTextEditor(int x, int y) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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

		((AbstractDocument) area.getDocument()).setDocumentFilter(dec);

		Container content = getContentPane();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JScrollPane scroll1 = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		content.add(scroll1, BorderLayout.CENTER);


        JPanel ipPanel = new JPanel();
        ipPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        ipPanel.add(new JLabel("ip address     "));
		ipPanel.add(ipaddress);
        content.add(ipPanel);

        JPanel portPanel = new JPanel();
        portPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        portPanel.add(new JLabel("port               "));
        portPanel.add(portNumber);
		content.add(portPanel, BorderLayout.WEST);

        JPanel redirectPanel = new JPanel();
        redirectPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        redirectPanel.add(new JLabel("redirect port"));
        redirectPanel.add(redirectPort);
		content.add(redirectPanel);



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
		Dimension windowSize = new Dimension(getPreferredSize());
		Dimension screenSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
		int wdwLeft = 300 + screenSize.width / 2 - windowSize.width / 2;
		int wdwTop = screenSize.height / 2 - windowSize.height / 2;
		setLocation(x, y);
		setVisible(true);
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
				e1.printStackTrace();
			}
			changed = false;
			Save.setEnabled(false);
			SaveAs.setEnabled(false);
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

	public boolean failedConnect = false ;
	Action Connect = new AbstractAction("Connect") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			setTitle("Trying to connect to " + ipaddress.getText() + ":" + portNumber.getText());
			client = new ConcreteClient(dec, area, new OldestFirstElectionStrategy(), DistributedTextEditor.this);
            try {
                client.startAndConnectTo(ipaddress.getText(), Integer.parseInt(portNumber.getText()));
            } catch (IOException e1) {
				e1.printStackTrace();
				failedConnect = true;
				/*int n = 1 - JOptionPane.showConfirmDialog(
						DistributedTextEditor.this,
						"Lost connection.",
						"Do you want to retry establishing the connection?",
						JOptionPane.YES_NO_OPTION);
				if (n == 1) { //if YES
					Disconnect.actionPerformed(null);
					try {
						Thread.sleep(500);
						Connect.actionPerformed(null);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}*/

			}
			failedConnect = false;

			changed = false;
			Save.setEnabled(false);
			SaveAs.setEnabled(false);

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
		new DistributedTextEditor(20, 0);
		new DistributedTextEditor(600, 0);
		new DistributedTextEditor(1200, 0);

		//new DistributedTextEditor(Integer.parseInt(arg[0]), Integer.parseInt(arg[1]));
	}

}
