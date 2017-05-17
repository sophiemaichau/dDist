package Utilities;

import java.io.Serializable;

/**
 *
 * @author Jesper Buus Nielsen
 *
 */
public class MyTextEvent implements Serializable {
	private int offset;
	private int count;

	MyTextEvent(int offset) {
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int i){
		offset = i;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
