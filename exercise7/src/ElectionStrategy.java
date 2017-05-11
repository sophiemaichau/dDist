import Utilities.Pair;

/**
 * Created by milo on 02-05-17.
 */
public interface ElectionStrategy {
    Pair<String, Long> getNextCandidate(RemoteList<Pair<String, Long>> remoteList);
    String nextServerIP();
}
