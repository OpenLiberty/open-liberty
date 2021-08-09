/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.event.internal;

import java.util.*;
import java.util.concurrent.Future;

import org.osgi.framework.Filter;

import com.ibm.websphere.event.*;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Implementation of the WebSphere <code>Event</code> interface.
 */
public class EventImpl implements Event, EventHandle {
    /**
     * The {@link Topic} this event is published to.
     */
    private final Topic topic;

    /**
     * Context properties.
     */
    private MapDictionary<String, Object> properties = new MapDictionary<String, Object>();

    /**
     * Reference to topicData for this event.
     */
    private TopicData topicData;

    /**
     * List of future tasks associated with an async event.
     */
    private Future<?>[] futures;

    /** Possible parent event */
    private EventImpl parent = null;
    /** Non-inheritable event local storage */
    private EventLocalMap<EventLocal<?>, Object> locals = null;
    /** Inheritable event local storage */
    private EventLocalMap<EventLocal<?>, Object> inheritableLocals = null;

    /** List of named EventLocals for this Event **/
    private volatile Set<String> eventLocalNames = null;

    public boolean addEventLocalName(String name) {
        // Lazy-init of eventLocalNames
        Set<String> localNames = eventLocalNames;
        if (localNames == null) {
            synchronized (this) {
                localNames = eventLocalNames;
                if (localNames == null) {
                    localNames = eventLocalNames = Collections.synchronizedSet(new HashSet<String>());
                }
            }
        }
        return localNames.add(name);
    }

    public void removeEventLocalName(String name) {
        if (eventLocalNames != null) {
            eventLocalNames.remove(name);
        }
    }

    /**
     * Non-public constructor to prevent instantiation without extension.
     * 
     * @param value
     *            <code>Topic</code> to set for the event
     */
    protected EventImpl(Topic value) {
        this.topic = value;
    }

    /**
     * Sets the <code>TopicData</code> to be used while processing this event.
     * 
     * @param value
     *            <code>TopicData</code> to set for processing the event
     */
    void setTopicData(TopicData value) {
        this.topicData = value;
    }

    /**
     * Get the current <code>TopicData</code> associated with this event.
     * 
     * @return the <code>TopicData</code> associated with this event
     */
    TopicData getTopicData() {
        return this.topicData;
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(String name) {
        return this.properties.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(ReservedKey key) {
        return this.properties.get(key.getName());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked" })
    // Class.cast has associated cost
    public <T> T getProperty(String name, Class<T> type) {
        return (T) getProperty(name);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked" })
    // Class.cast has associated cost
    public <T> T getProperty(ReservedKey key, Class<T> type) {
        return (T) getProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getPropertyNames() {
        final int size = this.properties.size();
        if (0 == size) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<String>(size);
        names.addAll(this.properties.keySet());
        return names;
    }

    /**
     * Indicate whether or not the backing property set is read only.
     * 
     * @param readOnly
     *            true if properties can't be added, removed, or modified
     */
    void setReadOnly(boolean readOnly) {
        this.properties.setReadOnly(readOnly);
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(String name, Object value) {
        this.properties.put(name, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(ReservedKey key, Object value) {
        this.properties.put(key.getName(), value);
    }

    /**
     * Copy the provided properties into this event.
     * 
     * @param newProps
     */
    public void setProperties(Map<String, ? extends Object> newProps) {
        this.properties.putAll(newProps);
    }

    final Topic getTopicObject() {
        return this.topic;
    }

    /*
     * @see com.ibm.websphere.event.Event#getTopic()
     */
    @Override
    @Trivial
    public String getTopic() {
        return getTopicObject().getName();
    }

    /*
     * @see com.ibm.websphere.event.EventHandle#getProperties()
     */
    public final MapDictionary<String, Object> getProperties() {
        if (this.properties.isReadyOnly()) {
            return this.properties;
        }
        return new MapDictionary<String, Object>(this.properties);
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(Filter filter) {
        return filter.match(getProperties());
    }

    /**
     * Transform into a readable topic name for trace and human consumption.
     */
    @Override
    public String toString() {
        return "[" + getTopic() + "]";
    }

    /**
     * {@inheritDoc}
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (futures == null) {
            return false;
        }
        for (Future<?> future : futures) {
            if (!future.cancel(mayInterruptIfRunning)) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCancelled() {
        if (futures == null) {
            return false;
        }
        for (Future<?> future : futures) {
            if (!future.isCancelled()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDone() {
        if (futures == null) {
            return true;
        }
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void waitForCompletion() {
        if (futures == null) {
            return;
        }
        for (Future<?> future : futures) {
            try {
                future.wait();
            } catch (InterruptedException e) {
                // TODO Should we catch the exception or should we throw it?
                // Instrumentation will log to ffdc
            }
        }
    }

    public Future<?>[] getFutures() {
        return this.futures;
    }

    public void setFutures(Future<?>[] futures) {
        this.futures = futures;
    }

    /**
     * Set the reference to the parent event that spawned this new one.
     * 
     * @param event
     */
    public void setParent(EventImpl event) {
        this.parent = event;
    }

    private <T> EventLocalMap<EventLocal<?>, Object> getMap(EventLocal<T> key) {
        if (key.isInheritable()) {
            if (null == this.inheritableLocals && null != this.parent) {
                // scan up the 1..N chain of parent events looking for any
                // existing map. If one exists, new inheritable maps will be
                // created back down the chain to here.
                EventLocalMap<EventLocal<?>, Object> map = this.parent.getMap(key);
                if (null != map) {
                    this.inheritableLocals = new EventLocalMap<EventLocal<?>, Object>(map);
                }
            }
            return this.inheritableLocals;
        }
        return this.locals;
    }

    /**
     * Query the possible value for the provided EventLocal.
     * 
     * @param <T>
     * @param key
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T get(EventLocal<T> key) {
        EventLocalMap<EventLocal<?>, Object> map = getMap(key);
        if (null == map) {
            return key.initialValue();
        }
        return (T) map.get(key);
    }

    /**
     * Set the value for the provided EventLocal on this event.
     * 
     * @param <T>
     * @param key
     * @param value
     */
    public <T> void set(EventLocal<T> key, Object value) {
        EventLocalMap<EventLocal<?>, Object> map = getMap(key);
        if (null == map) {
            // the call to getMap would have created the inheritable map
            // if appropriate, thus always create a disconnected map here
            map = new EventLocalMap<EventLocal<?>, Object>();
            if (key.isInheritable()) {
                this.inheritableLocals = map;
            } else {
                this.locals = map;
            }
        }
        map.put(key, value);
    }

    /**
     * Remove any existing value for the provided EventLocal.
     * 
     * @param <T>
     * @param key
     * @return T, existing object being removed, null if none present
     */
    @SuppressWarnings("unchecked")
    public <T> T remove(EventLocal<T> key) {
        EventLocalMap<EventLocal<?>, Object> map = getMap(key);
        if (null != map) {
            return (T) map.remove(key);
        }
        return null;
    }

    /**
     * Look for the EventLocal of the given name in this particular Event
     * instance.
     * 
     * @param <T>
     * @param name
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextData(String name) {
        T rc = null;
        if (null != this.inheritableLocals) {
            rc = (T) this.inheritableLocals.get(name);
        }
        if (null == rc && null != this.locals) {
            rc = (T) this.locals.get(name);
        }
        return rc;
    }
}
