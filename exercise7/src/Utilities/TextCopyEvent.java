package Utilities;

public class TextCopyEvent extends MyTextEvent {
    private String copiedText;
    private int timeStamp;
    private int count;

    public TextCopyEvent(int offset, String copyText, int timeStamp, int count) {
        super(offset);
        this.copiedText = copyText;
        this.timeStamp = timeStamp;
        this.count = count;
    }

    public String getCopiedText(){
        return copiedText;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void setCount(int count) {
        this.count = count;
    }
}
