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
                    System.out.println("InsertEvent!");
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
                    System.out.println("RemoveEvent!");
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

    public void setElectionStrategy(ElectionStrategy electionStrategy) {
        this.electionStrategy = electionStrategy;
    }
}
