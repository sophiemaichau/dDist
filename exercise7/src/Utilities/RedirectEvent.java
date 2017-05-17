package Utilities;

import java.io.Serializable;

/**
 * Created by milo on 14-05-17.
 */
public class RedirectEvent implements Serializable {
    private String redirectIp;
    private int redirectPort;

    public RedirectEvent(String redirectIp, int redirectPort) {
        this.redirectIp = redirectIp;
        this.redirectPort = redirectPort;
    }

    public String getRedirectIp() {
        return redirectIp;
    }

    public int getRedirectPort() {
        return redirectPort;
    }
}
