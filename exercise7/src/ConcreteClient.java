import Utilities.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;

public class ConcreteClient extends AbstractClient {
    private DocumentEventCapturer dec;
    private JTextArea area;
    private Thread sendLocalEventsThread;
    private ElectionStrategy electionStrategy;
    private DistributedTextEditor frame;
    private ArrayList<Pair<InetAddress, Integer>> view = new ArrayList<>();
    private int id;
    private int count;
    private Thread redirectThread;
    private RedirectServer redirectServer;

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
                    count = tie.getCount();
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
                    count = tre.getCount();
                    dec.disabled = true;
                    area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                    dec.disabled = false;
                } catch (Exception e) {
                    System.err.println(e);
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

            //start new server for redirecting new peers to sequencer
            if (redirectThread == null) {
                redirectThread = new Thread(() -> {
                    try {
                        int redirectPort = Integer.parseInt(frame.portNumber.getText());
                        Thread.sleep(300);
                        redirectServer = new RedirectServer(Integer.parseInt(frame.redirectPort.getText()), getServerIP(), redirectPort);
                        System.out.println("Started redirect server!");
                        redirectServer.startListening(false);
                    } catch (IOException e1) {
                        System.out.println("redirect server failed:");
                        e1.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                redirectThread.start();

            }

        }else if(o instanceof UpdateViewEvent) {
            UpdateViewEvent e = (UpdateViewEvent) o;
            view = e.getView();
        } else if (o instanceof RedirectEvent) {
            RedirectEvent e = (RedirectEvent) o;
            new Thread(() -> {
                frame.ipaddress.setText(e.getRedirectIp());
                frame.portNumber.setText(Integer.toString(e.getRedirectPort()));
                frame.Disconnect.actionPerformed(null);
                frame.Connect.actionPerformed(null);
            }).start();

        }
    }

    @Override
    public synchronized void onConnect(String serverIP) {
        frame.clientConnectedUpdateText();
        //listen for local text events and send to sequencer
        sendLocalEventsThread = new Thread(() -> {
            while (true) {
                MyTextEvent e;
                try {
                    ArrayList<MyTextEvent> list = new ArrayList<>();
                    while (!dec.eventHistory.isEmpty()) {
                        e = dec.take();
                        e.setCount(count + 1);
                        list.add(e);
                    }
                    for (MyTextEvent m : list ) {
                        sendToServer(m);
                    }
                } catch (InterruptedException e1) {
                    return;
                }
            }
        });
        sendLocalEventsThread.start();
    }

    @Override
    public synchronized void onDisconnect() {
        frame.clientDisconnectedUpdateText();
        System.out.println("disconnected from server " + getServerIP() + " on port " + getPort());
        if (redirectThread != null) {
            redirectThread.interrupt();
            redirectThread = null;
        }
        if (redirectServer != null) {
            redirectServer.shutdown();
        }
        if (sendLocalEventsThread != null) {
            sendLocalEventsThread.interrupt();
            sendLocalEventsThread = null;
        }
    }

    @Override
    public void onLostConnection() {
        frame.clientDisconnectedUpdateText();
        sendLocalEventsThread.interrupt();
        redirectThread.interrupt();
        redirectServer.shutdown();
        System.out.println("unexpectedly lost connection to server. Beginning election procedure...");
        beginElection();
    }

    private synchronized void beginElection() {
        System.out.println("id" + view.get(1) + " should be the new sequencer");
        if (redirectThread != null) {
            redirectThread.interrupt();
            redirectThread = null;
        }

        if (id == view.get(1).getSecond()) {
            System.out.println("I should be sequencer with id: " + id);
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    frame.Disconnect.actionPerformed(null);
                    Thread.sleep(100);
                    frame.Listen.actionPerformed(null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            new Thread(() -> {
                int triesLimit = view.size() - 1;
                int tries = 1;
                frame.Disconnect.actionPerformed(null);
                frame.setTitle("trying to re-establish connection");
                while (tries <= triesLimit) {
                    System.out.println("trying to connect to new sequencer. Attempt no. " + tries);
                    if (id == view.get(tries).getSecond()) {
                        System.out.println("I'm new sequencer!");
                        new Thread(() -> {
                            try {
                                Thread.sleep(100);
                                frame.Disconnect.actionPerformed(null);
                                Thread.sleep(100);
                                frame.Listen.actionPerformed(null);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                        break;
                    }
                    frame.ipaddress.setText(String.valueOf(view.get(tries).getFirst()).substring(1));
                    try {
                        Thread.sleep(3000);
                        frame.Connect.actionPerformed(null);
                        Thread.sleep(400);
                        if (frame.failedConnect == false) {
                            System.out.println("successfully connected to new sequencer");
                            break;
                        } else {
                            tries++;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

}
