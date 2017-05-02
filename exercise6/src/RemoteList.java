import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteList<E> extends Remote {
    void add(E e) throws RemoteException;
    boolean remove(E e) throws RemoteException;
    E remove(int i) throws RemoteException;
    boolean isEmpty() throws RemoteException;
    boolean contains(E e) throws RemoteException;
    E get(int i) throws RemoteException;
    String prettyToString() throws RemoteException;

}
