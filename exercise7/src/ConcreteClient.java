import Utilities.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * A concrete client implementation
 */
public class ConcreteClient extends AbstractClient {
    private DocumentEventCapturer dec;
    private JTextArea area;
    private Thread sendLocalEventsThread;
    private DistributedTextEditor frame;
    private ArrayList<Pair<InetAddress, Integer>> view = new ArrayList<>();
    private int id;
    private int count;
    private Thread redirectThread;
    private RedirectServer redirectServer;

    public ConcreteClient(DocumentEventCapturer dec, JTextArea area, DistributedTextEditor frame) {
        this.dec = dec;
        this.area = area;
        this.frame = frame;
    }

    @Override
    public void onReceivedFromServer(Object o) {
        //switch on object type
        if (o instanceof TextInsertEvent) {
            final TextInsertEvent tie = (TextInsertEvent) o;
            EventQueue.invokeLater(() -> {
                try {
                    dec.disabled = true;
                    area.insert(tie.getText(), tie.getOffset());
                    count = tie.getCount();
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
            //copy events are only sent from server when it sees a new connection.
        } else if (o instanceof TextCopyEvent) {
            final TextCopyEvent tce = (TextCopyEvent) o;
            EventQueue.invokeLater(() -> {
                dec.disabled = true;
                area.setText(tce.getCopiedText());
                count = tce.getCount();
                id = tce.getTimeStamp();
                dec.disabled = false;
            });

            //start new server for redirecting new peers to sequencer
            if (redirectThread == null) {
                redirectThread = new Thread(() -> {
                    try {
                        int redirectPort = Integer.parseInt(frame.portNumberList.getSelectedItem().toString());
                        Thread.sleep(300);
                        redirectServer = new RedirectServer(Integer.parseInt(frame.redirectPort.getText()), getServerIP(), redirectPort);
                        System.out.println("Started redirect server!");
                        redirectServer.startListening(false);
                    } catch (IOException e1) {
                        System.out.println("redirect server failed:");
                        System.err.println(e1);
                    } catch (InterruptedException e) {
                        System.err.println(e);
                    }
                });
                redirectThread.start();

            }

        } else if (o instanceof UpdateViewEvent) {
            UpdateViewEvent e = (UpdateViewEvent) o;
            view = e.getView();
        } else if (o instanceof RedirectEvent) {
            //if redirect event => disconnect and connect to the redirectIP + port
            RedirectEvent e = (RedirectEvent) o;
            new Thread(() -> {
                frame.ipaddress.setText(e.getRedirectIp());
                frame.portNumberList.setSelectedItem(e.getRedirectPort());
                frame.Disconnect.actionPerformed(null);
                frame.Connect.actionPerformed(null);
            }).start();

        }
    }

    /*
        when connected, start taking events out of eventQueue and send to server
     */
    @Override
    public synchronized void onConnect(String serverIP) {
        frame.clientConnectedUpdateText();
        //listen for local text events and send to sequencer
        sendLocalEventsThread = new Thread(() -> {
            while (true) {
                MyTextEvent e;
                try {
                    e = dec.take();
                    e.setCount(count + 1);
                    sendToServer(e);
                } catch (InterruptedException e1) {
                    System.err.println(e1);
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

    /*
        If unexpectedly lost connection, start election.
     */
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
        if (redirectThread != null) {
            redirectThread.interrupt();
            redirectThread = null;
        }

        //if I am to be new sequencer, start listening
        if (id == view.get(1).getSecond()) {
            new Thread(() -> {
                try {
                    frame.Disconnect.actionPerformed(null);
                    Thread.sleep(100);
                    frame.Listen.actionPerformed(null);
                } catch (InterruptedException e) {
                    System.err.println(e);
                }
            }).start();

        } else {

            new Thread(() -> {
                int triesLimit = view.size() - 1;
                int tries = 1;
                frame.Disconnect.actionPerformed(null);
                frame.setTitle("trying to re-establish connection");
                //Loop through all elements in view and try to connect to them,
                //unless I see myself, then start listening and become new sequencer
                while (tries <= triesLimit) {
                    //if my id equals view.get(i) then become sequencer
                    if (id == view.get(tries).getSecond()) {
                        new Thread(() -> {
                            try {
                                Thread.sleep(100);
                                frame.Disconnect.actionPerformed(null);
                                Thread.sleep(100);
                                frame.Listen.actionPerformed(null);
                                frame.ipaddress.setText(String.valueOf(frame.server.getServerIpAddress().substring(1)));
                            } catch (InterruptedException e) {
                                System.err.println(e);
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                        }).start();
                        break;
                    //else try to connect to current index into view. If failed, continue iteration through view.
                    } else {
                        frame.ipaddress.setText(String.valueOf(view.get(tries).getFirst()).substring(1));
                        try {
                            Thread.sleep(1500);
                            frame.Connect.actionPerformed(null);
                            Thread.sleep(100);
                            if (frame.failedConnect == false) {
                                System.out.println("successfully connected to new sequencer");
                                break;
                            } else {
                                frame.Disconnect.actionPerformed(null);
                                tries++;
                            }
                        } catch (InterruptedException e) {
                            System.err.println(e);
                        }
                    }
                }
            }).start();
        }
    }

}
