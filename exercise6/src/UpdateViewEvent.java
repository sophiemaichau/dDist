import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by milo on 08-05-17.
 */
public class UpdateViewEvent implements Serializable {
    ArrayList<Pair<InetAddress, Integer>> view;

    public UpdateViewEvent(ArrayList<Pair<InetAddress, Integer>> view) {
        this.view = view;
    }

    public ArrayList<Pair<InetAddress, Integer>> getView() {
        return view;
    }
}
