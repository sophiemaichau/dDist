import java.io.Serializable;

public class MessageWrapper implements Serializable{

    private String ip;
    private MyTextEvent mte;

    public MessageWrapper(String ip, MyTextEvent mte) {
        this.ip = ip;
        this.mte = mte;
    }

    public String getIp(){
        return ip;
    }

    public MyTextEvent getMte(){
        return mte;
    }
}