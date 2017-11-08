/**
 *
 */
package asyncEventsApp.web;

public class CakeReport {

    private String cakeObserver = null;
    private long tid;

    public CakeReport(String obs, long tid) {
        this.cakeObserver = obs;
        this.tid = tid;
    }

    /**
     * @return the cakeObserver
     */
    public String getCakeObserver() {
        return cakeObserver;
    }

    /**
     * @return the tid
     */
    public long getTid() {
        return tid;
    }
}
