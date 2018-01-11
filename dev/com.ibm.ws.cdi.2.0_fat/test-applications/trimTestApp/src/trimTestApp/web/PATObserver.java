/**
 *
 */
package trimTestApp.web;

import java.util.concurrent.ConcurrentSkipListSet;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

/**
 *
 */
public class PATObserver implements Extension {

    static ConcurrentSkipListSet<String> observed = new ConcurrentSkipListSet<String>();

    public void t1Observer(@Observes ProcessAnnotatedType<ThingOne> event) {
        System.out.println("ProcessAnnotatedType<ThingOne> observed");
        observed.add(event.getAnnotatedType().getBaseType().getTypeName());
    }

    public void t2Observer(@Observes ProcessAnnotatedType<ThingTwo> event) {
        System.out.println("ProcessAnnotatedType<ThingTwo> observed");
        observed.add(event.getAnnotatedType().getBaseType().getTypeName());
    }

    public void tObserver(@Observes ProcessAnnotatedType<Thing> event) {
        System.out.println("ProcessAnnotatedType<Thing> observed");
        observed.add(event.getAnnotatedType().getBaseType().getTypeName());
    }
}