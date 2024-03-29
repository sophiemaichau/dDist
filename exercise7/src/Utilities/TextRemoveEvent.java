package Utilities;

public class TextRemoveEvent extends MyTextEvent {

	private int length;
	
	public TextRemoveEvent(int offset, int length) {
		super(offset);
		this.length = length;
	}
	
	public int getLength() { return length; }

	public String toString() {
		return "remove(" + getOffset() + ", " + length + ")";
	}

	public Object clone() {
		TextRemoveEvent copy = new TextRemoveEvent(getOffset(), getLength());
		copy.setCount(getCount());
		return copy;
	}
}
