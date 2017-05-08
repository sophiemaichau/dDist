import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.Timestamp;
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
    private RemoteList<Pair<String, Long>> stub;
    private RemoteList<Pair<String, Long>> backupStub;
    private long timeStamp;
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
                    dec.disabled = true;
                    area.insert(tie.getText(), tie.getOffset());
                    dec.disabled = false;
                } catch (Exception e) {
                    System.err.println(e);
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
                    System.err.println(e);
                }
            });
        } else if (o instanceof TextCopyEvent) {
            final TextCopyEvent tce = (TextCopyEvent) o;
            EventQueue.invokeLater(() -> {
                dec.disabled = true;
                area.setText(tce.getCopiedText());
                timeStamp = tce.getTimeStamp();
                /*ArrayList<Pair<String, Long>> view = tce.getView();

                for (Pair<String, Long> p : view) {
                    stub
                }*/
                dec.disabled = false;
            });

        }
    }


    @Override
    public void onConnect(String serverIP) {
        sendLocalEventsThread = new Thread(() -> {
            while (true) {
                MyTextEvent e;
                try {
                    e = dec.take();
                } catch (InterruptedException e1) {
                    return;
                }
                boolean res = ConcreteClient.this.sendToServer(e);
            }
        });
        sendLocalEventsThread.start();
        setupRMI(serverIP);
    }

    @Override
    public void onDisconnect() {
        if (sendLocalEventsThread.isInterrupted() == false) {
            sendLocalEventsThread.interrupt();
        }
        if (rmiUpdateThread.isInterrupted() == false) {
            rmiUpdateThread.interrupt();
        }
        System.out.println("disconnected from server");
    }

    @Override
    public void onLostConnection() {
        disconnect();
        if (sendLocalEventsThread.isInterrupted() == false) {
            sendLocalEventsThread.interrupt();
        }
        if (rmiUpdateThread.isInterrupted() == false) {
            rmiUpdateThread.interrupt();
        }
        System.out.println("unexpectedly lost connection to server. Beginning election procedure...");
        beginElection();
    }

    private synchronized void beginElection() {
        boolean done = false;
        while (!done) {
            System.out.println("my timestamp: " + timeStamp);
            try {
                System.out.println("my view: " + backupStub.prettyToString());
                long elect =  backupStub.get(0).getTimestamp();
                backupStub.remove(0);
                if (elect == timeStamp) {
                    System.out.println("I won the election! Starting server.");
                    new Thread(() -> {
                        try {
                            ConcreteServer server = new ConcreteServer(40499, area);
                            server.replaceStub(backupStub);
                            server.startListening();
                            frame.server = server;
                            frame.serverStartedUpdateText();
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
                        beginElection();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void setupRMI(String serverIP) {
        String name = "connectionList";
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(serverIP);
            stub = (RemoteList<Pair<String, Long>>) registry.lookup(name);
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

    private RemoteList<Pair<String, Long>> deepCopy(RemoteList<Pair<String, Long>> stub) throws IOException, ClassNotFoundException {
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


    public void setElectionStrategy(ElectionStrategy electionStrategy) {
        this.electionStrategy = electionStrategy;
    }
}
