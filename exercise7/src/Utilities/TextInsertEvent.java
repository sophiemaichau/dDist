package Utilities;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class TextInsertEvent extends MyTextEvent {

	private String text;
	
	public TextInsertEvent(int offset, String text) {
		super(offset);
		this.text = text;
	}
	public String getText() { return text; }


	public String toString() {
		return "insert(" + text + ", " + getOffset() + ")";
	}

	public boolean equals(Object o) {
		if (o instanceof TextInsertEvent) {
			return ((TextInsertEvent) o).getText().equals(text) && ((TextInsertEvent) o).getOffset() == getOffset();
		}
		return false;
	}

}