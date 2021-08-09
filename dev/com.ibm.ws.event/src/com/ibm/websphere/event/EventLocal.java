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
package com.ibm.websphere.event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.event.internal.CurrentEvent;
import com.ibm.ws.event.internal.EventImpl;

/**
 * Similar to ThreadLocal, EventLocal allows setting and querying values
 * in the current Event runtime. This can be accessed at any time by the
 * owner while this event is being handled.
 * 
 * @param <T>
 * @see java.lang.ThreadLocal
 */
public class EventLocal<T> {
    private final static AtomicInteger count = new AtomicInteger(0);
    private static ConcurrentHashMap<String, EventLocal<?>> eventLocalNames = new ConcurrentHashMap<String, EventLocal<?>>();

    private String name = null;
    private int index;
    private boolean isInheritable = false;

    /**
     * Query possible EventLocal context data for the current Event runtime
     * based on the provided name value. If an EventLocal was created under
     * the target name then the currently running Event runtime would return
     * its value.
     * 
     * @param <T>
     * @param name
     * @return T, null if not set or the name does not exist
     */
    @SuppressWarnings("unchecked")
    public static <T> T getContextData(String name) {
        EventImpl event = (EventImpl) CurrentEvent.get();
        if (null != event) {
            return (T) event.getContextData(name);
        }
        return null;
    }

    /**
     * Query possible EventLocal for the current Event based on
     * the provided name value. If an EventLocal was created under the
     * target name then the currently running event runtime would return
     * its value.
     * 
     * @param <T>
     * @param name
     * @return T, null if not set or the name does not exist
     */
    public static <T> EventLocal<T> getLocal(String name) {
        return (EventLocal<T>) eventLocalNames.get(name);
    }

    /**
     * Create an CollaborationLocal that is not inheritable.
     * 
     * @param inheritable
     */
    public static <T> EventLocal<T> createLocal() {
        return new EventLocal<T>(false);
    }

    /**
     * Create an CollaborationLocal that is inheritable by children
     * collaborations based on the provided flag.
     * 
     * @param inheritable
     */
    public static <T> EventLocal<T> createInheritableLocal() {
        return new EventLocal<T>(true);
    }

    public static <T> EventLocal<T> createLocal(String name) {
        return internalCreateNamedLocal(name, false);
    }

    public static <T> EventLocal<T> createInheritableLocal(String name) {
        return internalCreateNamedLocal(name, true);
    }

    /**
     * @param name2
     * @param b
     * @return
     */
    private static <T> EventLocal<T> internalCreateNamedLocal(String name, boolean inheritable) {
        EventLocal<?> local = eventLocalNames.get(name);

        if (local == null) {
            local = new EventLocal<T>(name, inheritable);

            EventLocal<?> prev = eventLocalNames.putIfAbsent(name, local);
            if (prev != null)
                local = prev; // discard our new value, someone else beat us into the
                              // map..
        }

        return (EventLocal<T>) local;
    }

    /**
     * Create an EventLocal that is inheritable by children events based on
     * the provided flag.
     * 
     * @param inheritable
     */
    public EventLocal(boolean inheritable) {
        this.index = count.getAndIncrement();
        this.isInheritable = inheritable;
    }

    /**
     * Create a named EventLocal that is inheritable by children events
     * based on the provided flag. The name must be unique within the
     * current Event runtime.
     * 
     * @param name
     * @param inheritable
     */
    private EventLocal(String name, boolean inheritable) {
        this(inheritable);
        this.name = name;
    }

    /**
     * Query whether this EventLocal is inheritable by children events.
     * 
     * @return boolean
     */
    public boolean isInheritable() {
        return this.isInheritable;
    }

    /**
     * When no value is set, this returns the initial value.
     * 
     * @return T
     */
    public T initialValue() {
        return null;
    }

    /**
     * Query the current value, if any, of this EventLocal.
     * 
     * @return T, null if nothing is set
     */
    public T get() {
        EventImpl event = (EventImpl) CurrentEvent.get();
        if (null == event) {
            return null;
        }
        return event.get(this);
    }

    /**
     * Set the value of the current EventLocal to the input item.
     * 
     * @param value
     */
    public void set(T value) {
        EventImpl event = (EventImpl) CurrentEvent.get();
        if (null != event) {
            event.set(this, value);
        }
    }

    /**
     * Remove this EventLocal from the current event.
     * 
     * @return T, existing object being removed, null if none present
     */
    public T remove() {
        EventImpl event = (EventImpl) CurrentEvent.get();
        if (null != event) {
            // Also need to remove name, if specified, from the Event's Set.
            if (name != null) {
                event.removeEventLocalName(name);
            }
            return event.remove(this);
        }
        return null;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (null == o || !(o instanceof EventLocal)) {
            return false;
        }
        return ((EventLocal) o).index == this.index;
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.index;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name;
    }
}
