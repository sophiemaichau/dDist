import javax.swing.*;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/**
 * Created by milo on 02-05-17.
 */
public class ConcreteServer extends AbstractServer {
    private final JTextArea area;


    public ConcreteServer(int port, JTextArea area) throws IOException {
        super(port);
        this.area = area;
        //setupRMI();
    }

    /*private void setupRMI() throws RemoteException {
        //setup RMI
        String name = "connectionList";
        RemoteList<Pair<String, Long>> connectionList = new ConnectionList<>();
        stub = (RemoteList<Pair<String, Long>>) UnicastRemoteObject.exportObject(connectionList, 0);
        registry = LocateRegistry.getRegistry();
        registry.rebind(name, stub);
        System.out.println("connectionList bound in server");
    }*/

    @Override
    public void onNewConnection(int id, String ipAddress) {
        System.out.println("new connection from: " + ipAddress + " with id: " + id);
        sendToClient(id, new TextCopyEvent(0, area.getText(), id));
        System.out.println("view after new connection: " + getView());
        broadcast(new UpdateViewEvent(getView()));
    }

    @Override
    public void onLostConnection(String ipAddress) {
        System.out.println("lost connection with : " + ipAddress + "!");
        //send view update to clients
    }

    @Override
    public void onShutDown() {
        /*try {
            //registry.unbind("connectionList");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }*/
        System.out.println("Closing down server");
    }

    /*public void replaceStub(RemoteList<Pair<String, Long>> list) throws RemoteException {
        stub.clear();
        for (int i = 0; i < list.size() - 1; i++) {
            stub.add(list.get(i));
        }
    }*/

    /*private <E, T> ArrayList<Pair<E, T>> convertToArrayList(RemoteList<Pair<E, T>> remoteList) throws RemoteException {
        ArrayList<Pair<E, T>> result = new ArrayList<>();
        for (int i = 0; i < remoteList.size() - 1; i++) {
            result.add(remoteList.get(i));
        }
        return result;
    }*/
}
