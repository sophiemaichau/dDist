public class TextCopyEvent extends MyTextEvent {
    private String copiedText;

    TextCopyEvent(int offset, String copyText) {
        super(offset);
        this.copiedText = copyText;
    }

    public String getCopiedText(){
        return copiedText;
    }
}
