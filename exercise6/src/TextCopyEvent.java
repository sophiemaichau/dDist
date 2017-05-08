import java.util.ArrayList;

public class TextCopyEvent extends MyTextEvent {
    private String copiedText;
    private long timeStamp;
    private ArrayList<Pair<String, Long>> view;

    TextCopyEvent(int offset, String copyText, long timeStamp, ArrayList<Pair<String, Long>> view) {
        super(offset);
        this.copiedText = copyText;
        this.timeStamp = timeStamp;
        this.view = view;
    }

    public String getCopiedText(){
        return copiedText;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public ArrayList<Pair<String, Long>> getView() {
        return view;
    }
}
