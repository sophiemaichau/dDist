import Utilities.Pair;

public interface ElectionStrategy {
    Pair<String, Long> getNextCandidate(RemoteList<Pair<String, Long>> remoteList);
    String nextServerIP();
}
