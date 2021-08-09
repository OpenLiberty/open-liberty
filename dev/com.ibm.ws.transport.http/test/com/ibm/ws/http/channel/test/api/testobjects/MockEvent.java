/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.api.testobjects;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.ReservedKey;

/**
 * Fake Event object for testing.
 */
public class MockEvent implements Event {

    Map<String, Object> props;
    String topic;

    /**
     * Constructor.
     * 
     * @param topic
     * @param props
     */
    public MockEvent(String topic, Map<String, Object> props) {
        this.topic = topic;
        this.props = props;
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#matches(org.osgi.framework.Filter)
     */
    public boolean matches(Filter filter) {
        return filter.match(new Hashtable<String, Object>(props));
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#getProperty(java.lang.String)
     */
    public Object getProperty(String name) {
        return props.get(name);
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#getProperty(com.ibm.websphere.eventengine.ReservedKey)
     */
    public Object getProperty(ReservedKey key) {
        return props.get(key.getName());
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#getProperty(java.lang.String, java.lang.Class)
     */
    public <T> T getProperty(String name, Class<T> type) {
        return type.cast(getProperty(name));
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#getProperty(com.ibm.websphere.eventengine.ReservedKey, java.lang.Class)
     */
    public <T> T getProperty(ReservedKey key, Class<T> type) {
        return getProperty(key.getName(), type);
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#getPropertyNames()
     */
    public List<String> getPropertyNames() {
        return new ArrayList<String>(props.keySet());
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#getTopic()
     */
    public String getTopic() {
        return topic;
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#setProperty(java.lang.String, java.lang.Object)
     */
    public void setProperty(String name, Object value) {
        props.put(name, value);
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#setProperty(com.ibm.websphere.eventengine.ReservedKey, java.lang.Object)
     */
    public void setProperty(ReservedKey key, Object value) {
        props.put(key.getName(), value);
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#cancel(boolean)
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        // Not applicable to synchronous Events.
        return false;
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#isCancelled()
     */
    public boolean isCancelled() {
        // Not applicable to synchronous Events.
        return false;
    }

    /*
     * @see com.ibm.websphere.eventengine.Event#isDone()
     */
    public boolean isDone() {
        // Not applicable to synchronous Events.
        return true;
    }
}
