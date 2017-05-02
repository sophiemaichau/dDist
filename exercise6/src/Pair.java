/**
 * Created by sophiemaichau on 02/05/2017.
 */
public class Pair<I, T> {
    private I ip;
    private T timestamp;

    public Pair(I ip, T timestamp) {
        this.ip = ip;
        this.timestamp = timestamp;
    }

    public I getIp() {
        return ip;
    }

    public void setIp(I ip) {
        this.ip = ip;
    }

    public T getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(T timestamp) {
        this.timestamp = timestamp;
    }
}
