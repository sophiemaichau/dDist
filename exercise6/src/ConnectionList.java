import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Created by sophiemaichau on 02/05/2017.
 */
public class ConnectionList<E> implements RemoteList<E>, Serializable {
    private ArrayList<E> connectionList = new ArrayList<E>();

    @Override
    public void add(E e) throws RemoteException {
        connectionList.add(e);
    }

    @Override
    public boolean remove(E e) throws RemoteException {
        return connectionList.remove(e);
    }

    @Override
    public boolean isEmpty() throws RemoteException {
        return connectionList.isEmpty();
    }

    @Override
    public boolean contains(E e) throws RemoteException {
        return connectionList.contains(e);
    }
}
