import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LongSummaryStatistics;

/**
 * Created by milo on 03-05-17.
 */
public class OldestFirstElectionStrategy implements ElectionStrategy {
    private Pair<String, Long> lowest;
    private int currIndex = 0;


    public OldestFirstElectionStrategy() {

    }
    @Override
    public Pair<String, Long> getNextCandidate(RemoteList<Pair<String, Long>> remoteList) {
        try {
            Pair<String, Long> localLowest = remoteList.get(currIndex);
            for (int i = currIndex; i < remoteList.size() - 1; i++) {
                if (remoteList.get(i).getTimestamp() < localLowest.getTimestamp()) {
                    localLowest = remoteList.get(i);
                }
            }
            lowest = localLowest;
            currIndex++;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return lowest;
    }

    @Override
    public String nextServerIP() {
        if (lowest == null) {return null;}
        return lowest.getIp();
    }
}
