import java.util.ArrayList;

public class TextCopyEvent extends MyTextEvent {
    private String copiedText;
    private int timeStamp;

    TextCopyEvent(int offset, String copyText, int timeStamp) {
        super(offset);
        this.copiedText = copyText;
        this.timeStamp = timeStamp;
    }

    public String getCopiedText(){
        return copiedText;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

}
