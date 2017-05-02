import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteList<E> extends Remote {
    void add(E e) throws RemoteException;
    boolean remove(E e) throws RemoteException;
    boolean isEmpty() throws RemoteException;
    boolean contains(E e) throws RemoteException;
    E get(int i) throws RemoteException;
    String prettyToString() throws RemoteException;
}
