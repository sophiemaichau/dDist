package Utilities;

import java.io.Serializable;

/**
 * Created by sophiemaichau on 02/05/2017.
 */
@SuppressWarnings("unchecked")
public class Pair<I, T> implements Serializable {
    private I first;
    private T second;

    public Pair(I ip, T timestamp) {
        this.first = ip;
        this.second = timestamp;
    }

    public I getFirst() {
        return first;
    }

    public void setFirst(I first) {
        this.first = first;
    }

    public T getSecond() {
        return second;
    }

    public void setSecond(T second) {
        this.second = second;
    }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    @Override
    public boolean equals(Object o) {
      Pair<I, T> p = (Pair<I, T>) o;
      return p.getFirst().equals(first) && p.getSecond().equals(second);
    }
}
