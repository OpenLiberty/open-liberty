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

import java.util.List;

import org.osgi.framework.Filter;
import org.osgi.service.event.EventConstants;

/**
 * An event. <code>Event</code> implementations are created by <code>EventEngine</code> on behalf of an event source.
 */
public interface Event {

    /**
     * Reserved key representing the topic associated with this event.
     */
    public final ReservedKey EVENT_TOPIC = new ReservedKey(EventConstants.EVENT_TOPIC);

    // /**
    // * Reserved key representing the <code>Topic</code> associated with this
    // event.
    // */
    // public final ReservedKey EVENT_TOPIC_OBJECT = new
    // ReservedKey(Topic.class.getName());

    /**
     * Get the name of the topic associated with this <code>Event</code>.
     * 
     * @return the topic name
     */
    public String getTopic();

    /**
     * Get the named context property.
     * 
     * @param name
     *            the name of the context property
     * @return the value of the context property or null
     */
    public Object getProperty(String name);

    /**
     * Get the context data associated with the reserved key.
     * 
     * @param key
     *            the context data key
     * @return the context value associated with the key
     */
    public Object getProperty(ReservedKey key);

    /**
     * Set the named context property.
     * 
     * @param name
     *            the context property name
     * @param value
     *            the context property value
     */
    public void setProperty(String name, Object value);

    /**
     * Associate the specified context value with the reserved key.
     * 
     * @param key
     *            the context data key
     * @param value
     *            the context data to associate with <code>key</code>
     */
    public void setProperty(ReservedKey key, Object value);

    /**
     * Get the named context property and cast to the provided type.
     * 
     * @param name
     *            the name of the context property
     * @param type
     *            the class representing the type associated with the named property
     * @return the value of the context property or null
     */
    public <T> T getProperty(String name, Class<T> type);

    /**
     * 
     * @param key
     *            the context key
     * @param type
     *            the class representing the type associated with the named property
     * @return the value o the context property or null
     */
    public <T> T getProperty(ReservedKey key, Class<T> type);

    /**
     * Get the names of the properties associated with this event.
     * 
     * @return the context property names.
     */
    public List<String> getPropertyNames();

    /**
     * Determine if this event's properties match the supplied filter.
     * This interface is unrelated to the <code>Condition</code> object.
     * 
     * @param filter
     * 
     * @return true iff the properties match the filter
     */
    public boolean matches(Filter filter);
}
