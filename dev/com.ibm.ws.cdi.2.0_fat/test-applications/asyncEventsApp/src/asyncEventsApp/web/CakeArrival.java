/**
 *
 */
package asyncEventsApp.web;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class CakeArrival {
    public List<CakeReport> getCakeReports() {
        return cakes;
    }

    public void addCake(String cakeObserver, long tid) {
        System.out.println("addCake - " + cakeObserver + ", tid - " + tid);
        CakeReport report = new CakeReport(cakeObserver, tid);
        this.cakes.add(report);
    }

    private List<CakeReport> cakes = new CopyOnWriteArrayList<>();
}
