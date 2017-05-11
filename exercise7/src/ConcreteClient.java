import Utilities.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by milo on 02-05-17.
 */
public class ConcreteClient extends AbstractClient {
    private DocumentEventCapturer dec;
    private JTextArea area;
    private Thread sendLocalEventsThread;
    private ElectionStrategy electionStrategy;
    private DistributedTextEditor frame;
    private ArrayList<Pair<InetAddress, Integer>> view = new ArrayList<>();
    private int id;
    private volatile boolean undergoingElection = false;
    private Thread rmiUpdateThread;

    public ConcreteClient(DocumentEventCapturer dec, JTextArea area, ElectionStrategy electionStrategy, DistributedTextEditor frame) {
        this.dec = dec;
        this.area = area;
        this.electionStrategy = electionStrategy;
        this.frame = frame;
    }

    @Override
    public void onReceivedFromServer(Object o) {
        if (o instanceof TextInsertEvent) {
            final TextInsertEvent tie = (TextInsertEvent) o;
            EventQueue.invokeLater(() -> {
                try {
                    System.out.println("received insert event!");
                    dec.disabled = true;
                    area.insert(tie.getText(), tie.getOffset());
                    dec.disabled = false;
                } catch (Exception e) {
                    System.err.println(e);
                    dec.disabled = false;
                }
            });
        } else if (o instanceof TextRemoveEvent) {
            final TextRemoveEvent tre = (TextRemoveEvent) o;
            EventQueue.invokeLater(() -> {
                try {
                    dec.disabled = true;
                    area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                    dec.disabled = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    dec.disabled = false;
                }
            });
        } else if (o instanceof TextCopyEvent) {
            final TextCopyEvent tce = (TextCopyEvent) o;
            EventQueue.invokeLater(() -> {
                dec.disabled = true;
                area.setText(tce.getCopiedText());
                id = tce.getTimeStamp();
                dec.disabled = false;
            });

        } else if(o instanceof UpdateViewEvent) {
            UpdateViewEvent e = (UpdateViewEvent) o;
            view = e.getView();
            System.out.println("updated view to: " + e.getView());
        }
    }


    @Override
    public void onConnect(String serverIP) {
        sendLocalEventsThread = new Thread(() -> {
            while (true) {
                MyTextEvent e;
                try {
                    e = dec.take();
                    boolean res = sendToServer(e);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        sendLocalEventsThread.start();
    }

    @Override
    public void onDisconnect() {
        if (sendLocalEventsThread.isInterrupted() == false) {
            sendLocalEventsThread.interrupt();
        }
        System.out.println("disconnected from server");
    }

    @Override
    public void onLostConnection() {
        disconnect();
        if (sendLocalEventsThread.isInterrupted() == false) {
            sendLocalEventsThread.interrupt();
        }

        System.out.println("unexpectedly lost connection to server. Beginning election procedure...");
        beginElection();
    }

    private synchronized void beginElection() {
        boolean done = false;
        while (!done) {
            System.out.println("my timestamp: " + id);
                System.out.println("my view: " + view);
                Pair<InetAddress, Integer> elect = view.get(0);
                if (elect.getSecond() == id) {
                    System.out.println("I won the election! Starting server.");
                    new Thread(() -> {
                        try {
                            ConcreteServer server = new ConcreteServer(40499, area);
                            server.startListening();
                            frame.server = server;
                            //frame.serverStartedUpdateText();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }).start();
                    try {
                        Thread.sleep(800);
                        startAndConnectTo(getServerIP(), 40499);
                        done = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(400);
                        System.out.println("I lost. Connecting to new server.");
                        boolean connected = startAndConnectTo(electionStrategy.nextServerIP().substring(1, electionStrategy.nextServerIP().length()), 40499);
                        if (connected) { done = true; }
                    } catch (IOException e) {
                        e.printStackTrace();
                        view.remove(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        view.remove(0);
                    }
                }
        }
    }

    /*private void setupRMI(String serverIP) {
        String name = "connectionList";
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(serverIP);
            stub = (RemoteList<Utilities.Pair<String, Long>>) registry.lookup(name);
            rmiUpdateThread = new Thread(() -> {
                try {
                    while (true) {
                        stub.prettyToString(); //just do something random which updates the stub
                        backupStub = deepCopy(stub);
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    //beginElection();
                }
            });
            rmiUpdateThread.start();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    private RemoteList<Utilities.Pair<String, Long>> deepCopy(RemoteList<Utilities.Pair<String, Long>> stub) throws IOException, ClassNotFoundException {
        // Convert stub to a stream of bytes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(stub);
        oos.flush();
        oos.close();
        byte[] byteData = bos.toByteArray();

        // Restore copy of stub from a stream of bytes
        ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
        return (RemoteList<Utilities.Pair<String, Long>>) new ObjectInputStream(bais).readObject();
    }

*/
    public void setElectionStrategy(ElectionStrategy electionStrategy) {
        this.electionStrategy = electionStrategy;
    }
}
