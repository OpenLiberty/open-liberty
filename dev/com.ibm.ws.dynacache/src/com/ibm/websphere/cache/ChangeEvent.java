/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache;

/**
 * An event object that provides information about the source of cache-related events.
 * ChangeEvent objects are generated when cache entries are changed in the cache.
 * The ChangeEvent object contains six pieces of information:
 * <ul>
 * <li><i>id</i> - the id that was changed
 * <li><i>value</i> - the new value
 * <li><i>causeOfChange</i> - the cause of change that generated this event (defined as EXISTING_VALUE_CHANGED or NEW_ENTRY_ADDED)
 * <li><i>sourceOfChange</i> - the source of change that generated this event (defined as LOCAL or REMOTE)
 * <li><i>cacheName</i> - the name of the cache
 * <li><i>timestamp</i> - the timestamp of when this event was generated
 * </ul>
 * @ibm-api 
 */
public class ChangeEvent extends java.util.EventObject
{
    private static final long serialVersionUID = -7681862873508155911L;
    /**
	 * Defines the cause of change for type EXISTING_VALUE_CHANGED 
     * @ibm-api 
     */
    public final static int EXISTING_VALUE_CHANGED = 1;

    /**
	 * Defines the cause of change for type NEW_ENTRY_ADDED 
     * @ibm-api 
     */
    public final static int NEW_ENTRY_ADDED = 2;

    /**
	 * Defines the cause of change for type EXPIRATION_TIMES_CHANGED. This constant is used by Validation Based Cache feature.
     * @ibm-api 
     */
    public final static int EXPIRATION_TIMES_CHANGED = 3;
    
    /**
	 * Define source of change for LOCAL (cache in memory or disk)
     * @ibm-api 
     */
    public final static int LOCAL = 1;

    /**
	 * Defines the source of change for type REMOTE 
     * @ibm-api 
     */
    public final static int REMOTE = 2;

    private Object m_value;
    private int  m_causeOfChange;
    private int  m_sourceOfChange;
    private long m_timeStamp = 0;
    public String m_cacheName;  // this cacheName will be set before firing event

    /**
     * Create a new ChangeEvent from id, value, cause of change, source of change and cache name
     * @ibm-api 
     */
    public ChangeEvent(Object id, Object value, int causeOfChange, int sourceOfChange, String cacheName) {
        super(id);
        m_value = value;
        m_causeOfChange = causeOfChange;
        m_sourceOfChange = sourceOfChange;
        m_cacheName = cacheName;
        m_timeStamp = System.currentTimeMillis();
    }

    /**
     * Gets the cache id that was changed. 
     *
     * @return the cache id that was changed. 
     * @ibm-api 
     */
    public Object getId() { 
        return getSource();
    }

    /**
     * Gets new value. The value might be serialized in a byte array format. In this case, you must 
     * deserialize the returned value.
     *
     * @return the new value.
     * @ibm-api 
     */
    public Object getValue() {
        return m_value;
    }

    /**
     * Gets the cause of change when this event was generated.
     * Use defined constants: EXISTING_VALUE_CHANGED or NEW_ENTRY_ADDED
     *
     * @return the cause of change
     * @ibm-api 
     */
    public int getCauseOfChange() {
        return m_causeOfChange;
    }

    /**
     * Gets the source of change when this event was generated.
     * Use defined constants: LOCAL and REMOTE
     *
     * @return the cause of change
     * @ibm-api 
     */
    public int getSourceOfChange() {
        return m_sourceOfChange;
    }

    /**
     * Gets the name of the cache
     *
     * @return the name of cache
     * @ibm-api 
     */
    public String getCacheName() {
        return m_cacheName;
    }

    /**
     * Gets the timestamp of when this event was generated.
     *
     * @return the timestamp
     * @ibm-api 
     */
    public long getTimeStamp() {
        return m_timeStamp;
    }
}

