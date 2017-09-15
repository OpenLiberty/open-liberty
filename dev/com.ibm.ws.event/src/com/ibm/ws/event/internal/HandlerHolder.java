/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.event.internal;

import static org.osgi.service.event.TopicPermission.SUBSCRIBE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.TopicPermission;
import org.osgi.service.log.LogService;

import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.EventHandler;
import com.ibm.ws.event.internal.adapter.OSGiHandlerAdapter;

/**
 * Encapsulation of data related to a registered <code>EventHandler</code>.
 */
// TODO: Extend linked queue to queue events and get rid of event holder?
public final class HandlerHolder implements Comparable<HandlerHolder>, Runnable {

    /**
     * Unbounded FIFO queue of <code>Event</code>s that must be delivered.
     * Sections 113.7.2 and 113.7.3 describe the semantics of an <code>EventHandler</code> as non-reentrant. As such, we need to queue
     * events to a handler and serialize delivery.
     */
    private final ConcurrentLinkedQueue<EventImpl> eventQueue = new ConcurrentLinkedQueue<EventImpl>();

    /**
     * Lock that must be acquired before polling and delivering events to a
     * non-reentrant event handler. Handlers that register with the reentrant
     * property do not require this lock.
     */
    Lock lock = new ReentrantLock();

    /**
     * List of fully qualified topic names that this handler is interested in.
     */
    private final List<String> discreteTopics = new ArrayList<String>();

    /**
     * List of wildcard topic hierarchies that this handler is interested in.
     */
    private final List<String> wildcardTopics = new ArrayList<String>();

    /**
     * Reference to the owning <code>EventEngine</code> implementation.
     */
    final EventEngineImpl eventEngine;

    /**
     * Dynamic <code>ServiceReference</code> to the target handler. We hold this
     * reference to allow for late activation of the handler and for tracking
     * purposes.
     */
    final ServiceReference<?> serviceReference;

    /**
     * Filter specification provided by the target handler.
     */
    final String filterSpec;

    /**
     * Resolved WebSphere handler instance.
     */
    EventHandler target = null;

    /**
     * Compiled <code>Filter</code>. May be null.
     */
    Filter filter = null;

    final String referenceName;

    /**
     * Indication that the target handler allows reentrant calls to its <code>handleEvent</code> method.
     */
    final boolean reentrant;

    /**
     * Id of the target handler (used to sort services w/ equivalent weight/rank
     */
    final long serviceId;

    /**
     * Id of the target handler (used to sort services w/ equivalent weight/rank
     */
    final int serviceRanking;

    /**
     * Create a new <code>HandlerHolder</code> instance that wrappers the target <code>EventHandler</code>.
     *
     * @param eventAdmin
     *            the owning implementation of <code>EventAdmin</code>
     * @param serviceReference
     *            the unresolved reference to the target handler
     * @param osgiHandler
     *            the serviceReference refers to an OSGi Event Handler
     */
    HandlerHolder(EventEngineImpl eventEngine, ServiceReference serviceReference, boolean osgiHandler) {
        this.eventEngine = eventEngine;
        this.serviceReference = serviceReference;

        // Get the topic subscription list
        Object topicsPropertyValue = serviceReference.getProperty(EventConstants.EVENT_TOPIC);
        if (topicsPropertyValue instanceof String) {
            populateTopics(new String[] { (String) topicsPropertyValue });
        } else if (topicsPropertyValue instanceof String[]) {
            populateTopics((String[]) topicsPropertyValue);
        } else if (topicsPropertyValue instanceof Collection<?>) {
            populateTopics(((Collection<?>) topicsPropertyValue).toArray(new String[0]));
        } else {
            populateTopics(new String[0]);
        }

        // Hold on to the filter
        filterSpec = (String) serviceReference.getProperty(EventConstants.EVENT_FILTER);
        serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

        // Service ranking is optional: if not specified, the default ranking is
        // 0
        Object tmp = serviceReference.getProperty(Constants.SERVICE_RANKING);
        serviceRanking = (tmp == null) ? 0 : (Integer) tmp;

        // Determine if handler is reentrant
        if (osgiHandler) {
            // OSGi handlers are never concurrent (reentrant)
            reentrant = false;
            referenceName = EventEngineImpl.OSGI_EVENT_HANDLER_REFERENCE_NAME;
        } else {

            String reentrantValue = (String) serviceReference.getProperty(EventEngine.REENTRANT_HANDLER);
            if (reentrantValue != null) {
                reentrant = Boolean.valueOf(reentrantValue);
            } else {
                reentrant = eventEngine.getDefaultReentrancy();
            }
            referenceName = EventEngineImpl.WS_EVENT_HANDLER_REFERENCE_NAME;
        }
    }

    /**
     * Parse the topic specifications into the appropriate lists.
     *
     * @param topics
     *            the topics the handler is interested in
     */
    private void populateTopics(String[] topics) {
        for (String t : topics) {
            // Clean up leading and trailing white space as appropriate
            t = t.trim();

            // Ignore topics that start or end with a '/'
            if (t.startsWith("/") || t.endsWith("/") || t.contains("//") || t.isEmpty()) {
                continue;
            }

            // Validate subscribe permission per section 113.10.2
            checkTopicSubscribePermission(t);

            if (t.equals("*")) {
                wildcardTopics.add("");
            } else if (t.endsWith("/*")) {
                wildcardTopics.add(t.substring(0, t.length() - 1));
            } else {
                discreteTopics.add(t);
            }
        }
    }

    /**
     * Check if the caller has permission to subscribe to events published to
     * the specified topic.
     *
     * @param topic
     *            the topic being subscribed to
     */
    private void checkTopicSubscribePermission(String topic) {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null)
            return;

        sm.checkPermission(new TopicPermission(topic, SUBSCRIBE));
    }

    /**
     * Get the <code>ServiceReference</code> representing the target <code>EventHandler</code> service.
     *
     * @return the <code>ServiceReference</code> of the target handler
     */
    ServiceReference getServiceReference() {
        return serviceReference;
    }

    /**
     * Get the list of fully qualified topic names that this handler is
     * interested in.
     *
     * @return the list of discrete topics
     */
    List<String> getDiscreteTopics() {
        return discreteTopics;
    }

    /**
     * Get this list of topic hierarchies that this handler is interested in.
     *
     * @return the list of topic roots without the * suffix. An empty string
     *         denotes all topics.
     */
    List<String> getWildcardTopics() {
        return wildcardTopics;
    }

    /**
     * Return whether or not the target handler is reentrant. This is an
     * extension that should be used by event handlers to prevent
     * synchronization bottlenecks.
     *
     * @return <code>true</code> if the target handler is reentrant
     */
    boolean isReentrant() {
        return reentrant;
    }

    /**
     * Get the resolved <code>EventHandler</code> service instance.
     *
     * @return the resolved <code>EventHandler</code> service instance
     */
    EventHandler getService() {
        if (target != null) {
            return target;
        }

        // Look for the handler
        ComponentContext context = eventEngine.getComponentContext();
        if (context == null) {
            return null;
        }
        Object svc = context.locateService(referenceName, serviceReference);

        if (svc instanceof EventHandler) {
            // it was a websphere handler
            target = (EventHandler) svc;
        } else if (svc instanceof org.osgi.service.event.EventHandler) {
            target = new OSGiHandlerAdapter((org.osgi.service.event.EventHandler) svc);
        } //if svc == null target remains null
        return target;
    }

    /**
     * Get the <code>Filter</code> representing the target handler's filter
     * specification.
     *
     * @return the compiled <code>Filter</code> instance or <code>null</code> if
     *         no filter specification was declared
     *
     * @throws InvalidSyntaxException
     *             if the filter specification syntax is not valid
     */
    Filter getFilter() throws InvalidSyntaxException {
        if (filter == null && filterSpec != null) {
            filter = eventEngine.getBundleContext().createFilter(filterSpec);
        }
        return filter;
    }

    /**
     * Compare two handlers: if there are somehow two handlerholders for the
     * same service (by instance of holder, or by service id), 0 will be
     * returned.
     *
     * Holders are otherwise sorted by service ranking (higher .. lower), and
     * then by service id (lower .. higher).
     *
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(HandlerHolder holderToCompare) {

        if (holderToCompare == this || this.serviceId == holderToCompare.serviceId)
            return 0;

        // service ranking is higher to lower
        int compare = holderToCompare.serviceRanking - this.serviceRanking;
        if (compare == 0) {
            // service id is lower to higher
            return holderToCompare.serviceId > this.serviceId ? -1 : 1;
        }

        // service ranking is higher to lower
        // Can't get here with equal ranking, falls into compare block w/
        // non-equal service ids.
        return holderToCompare.serviceRanking > this.serviceRanking ? 1 : -1;
    }

    @Override
    public void run() {
        fireEvent();
    }

    /**
     * Queue an event to the target <code>EventHandler</code>.
     *
     * @param event
     *            the event to queue
     */
    void addEvent(EventImpl event) {
        eventQueue.add(event);
    }

    /**
     * Deliver this event to the next target handler pulled from the
     * handlerQueue. If the handler is not reentrant, the delivery will be
     * serialized to prevent multiple threads entering the handler concurrently.
     */
    void fireEvent() {
        boolean useLock = !isReentrant();
        if (useLock) {
            lock.lock();
        }
        EventImpl event = eventQueue.poll();

        try {
            if (event != null) {
                fireEvent(event);
            }
        } finally {
            if (useLock) {
                lock.unlock();
            }
        }

    }

    /**
     * Deliver this event to the next <code>EventHandler</code> in the queue on
     * the current thread. If the handler is not reentrant, the delivery will be
     * serialized to prevent multiple threads from entering the handler
     * concurrently.
     */
    void fireSynchronousEvent(EventImpl event) {
        boolean useLock = !isReentrant();
        if (useLock) {
            lock.lock();
        }

        try {
            fireEvent(event);
        } finally {
            if (useLock) {
                lock.unlock();
            }
        }
    }

    private void fireEvent(EventImpl event) {
        // Associate this event with the thread
        CurrentEvent.push(event);
        try {
            Filter filter = getFilter();
            if (filter == null || event.matches(filter)) {
                EventHandler handler = getService();
                if (handler != null) {
                    handler.handleEvent(event);
                }
            }
        } catch (InvalidSyntaxException ise) {
            // TODO: Localized message
            // Logging a warning about the filter error and removing the
            // handler from future event handling per section 113.4
            String msg = "Invalid filter specification: " + filterSpec;
            eventEngine.log(serviceReference, LogService.LOG_WARNING, msg, ise);
            if (EventEngineImpl.OSGI_EVENT_HANDLER_REFERENCE_NAME.equals(referenceName))
                eventEngine.unsetWsEventHandler((ServiceReference<com.ibm.websphere.event.EventHandler>) serviceReference);
            else if (EventEngineImpl.OSGI_EVENT_HANDLER_REFERENCE_NAME.equals(referenceName))
                eventEngine.unsetOsgiEventHandler((ServiceReference<org.osgi.service.event.EventHandler>) serviceReference);
        } catch (Throwable t) {
            // TODO: Localized message
            // Logging the exception per section 113.8.1
            String msg = "EventHandler " + target + " raised an exception while handling an event";
            eventEngine.log(serviceReference, LogService.LOG_WARNING, msg, t);
        } finally {
            // Break association with this event
            CurrentEvent.pop();
        }
    }

    /**
     * Simple diagnostic aid that presents a human readable representation of
     * the object instance.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";target=").append(target);
        sb.append(",serviceReference=").append(serviceReference);
        return sb.toString();
    }
}
