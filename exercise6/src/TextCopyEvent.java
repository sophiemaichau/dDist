public class TextCopyEvent extends MyTextEvent {
    private String copiedText;
    private long timeStamp;

    TextCopyEvent(int offset, String copyText, long timeStamp) {
        super(offset);
        this.copiedText = copyText;
        this.timeStamp = timeStamp;
    }

    public String getCopiedText(){
        return copiedText;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
