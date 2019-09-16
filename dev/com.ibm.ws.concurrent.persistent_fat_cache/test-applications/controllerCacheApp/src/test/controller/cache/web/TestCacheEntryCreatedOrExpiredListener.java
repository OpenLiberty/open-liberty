/**
 *
 */
package test.controller.cache.web;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;

/**
 *
 */
public class TestCacheEntryCreatedOrExpiredListener implements CacheEntryCreatedListener<String, String>, CacheEntryExpiredListener<String, String> {

    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends String, ? extends String>> events) throws CacheEntryListenerException {
        System.out.println("---- onCreated:");
        events.forEach(event -> {
            ControllerCacheTestServlet.created.put(event.getKey(), event.getValue());
            System.out.println("     " + event.getEventType().name() + ": " + event.getKey() + " with " + event.getValue() + ", old value is " +
                               (event.isOldValueAvailable() ? event.getOldValue() : "<unavailable>"));
        });
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends String, ? extends String>> events) throws CacheEntryListenerException {
        System.out.println("---- onExpired:");
        events.forEach(event -> {
            ControllerCacheTestServlet.expired.add(event.getKey() + ":" + event.getValue());
            System.out.println("     " + event.getEventType().name() + ": " + event.getKey() + " with " + event.getValue() + ", old value is " +
                               (event.isOldValueAvailable() ? event.getOldValue() : "<unavailable>"));
        });
    }
}
