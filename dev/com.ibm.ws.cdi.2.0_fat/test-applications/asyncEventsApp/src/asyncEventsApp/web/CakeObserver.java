/**
 *
 */
package asyncEventsApp.web;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;

/**
 *
 */

public class CakeObserver {

    @ApplicationScoped
    public static class asyncCakeObserver {
        public void observes(@ObservesAsync CakeArrival cakearrival) {
            cakearrival.addCake(getClass().getSimpleName(), Thread.currentThread().getId());
            System.out.println("ASYNCCAKEOBSERVER: name " + getClass().getSimpleName());
        }
    }

    @ApplicationScoped
    public static class syncCakeObserver {
        public void observesSync(@Observes CakeArrival cakearrival) {
            cakearrival.addCake(getClass().getSimpleName(), Thread.currentThread().getId());
            System.out.println("SYNCCAKEOBSERVER: name " + getClass().getSimpleName());
        }
    }
}
