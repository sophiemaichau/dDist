import javax.swing.*;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by milo on 02-05-17.
 */
public class ConcreteServer extends AbstractServer {
    private final JTextArea area;
    private RemoteList<Pair<String, Long>> stub;
    private Registry registry;

    public ConcreteServer(int port, JTextArea area) throws IOException {
        super(port);
        this.area = area;
        setupRMI();
    }

    private void setupRMI() throws RemoteException {
        //setup RMI
        String name = "connectionList";
        RemoteList<Pair<String, Long>> connectionList = new ConnectionList<>();
        stub = (RemoteList<Pair<String, Long>>) UnicastRemoteObject.exportObject(connectionList, 0);
        registry = LocateRegistry.getRegistry();
        registry.rebind(name, stub);
        System.out.println("connectionList bound");
    }

    @Override
    public void onNewConnection(ConnectionHandler connectionHandler, String ipAddress) {
        try {
            long timestamp = System.currentTimeMillis();
            connectionHandler.sendObject(new TextCopyEvent(0, area.getText(), timestamp));
            stub.add(new Pair<>(ipAddress, timestamp));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLostConnection(String ipAddress) {
        //somehow remove from stub list
    }

    @Override
    public void onShutDown() {
        try {
            registry.unbind("connectionList");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }

    public void replaceStub(RemoteList<Pair<String, Long>> list) throws RemoteException {
        stub.clear();
        System.out.println("cleared!");
        for (int i = 0; i < list.size() - 1; i++) {
            stub.add(list.get(i));
            System.out.println("added!");
        }
    }
}
