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
package com.ibm.wsspi.cache;

import com.ibm.websphere.cache.InvalidationListener;
import com.ibm.websphere.cache.InvalidationEvent;
import com.ibm.websphere.cache.ChangeListener;
import com.ibm.websphere.cache.ChangeEvent;
import com.ibm.websphere.cache.PreInvalidationListener;

/**
 * Runtime objects implement this when they are an event source
 * Java objects can be registered with an EventSource. The listeners
 * are called when the fireEvent, cacheEntryChanged, or 
 * shouldInvalidate methods is called.
 * <p>
 * Here are the functions for the interface:
 * <ul>
 * <li>To add invalidation, pre-invalidation and change listeners.
 * <li>To remove invalidation, pre-invalidation and change listeners.
 * <li>To fire invalidation, pre-invalidation and change events.
 * <li>To find how many listeners registered.
 * </ul>
 * 
 * @ibm-spi
 * @since WAS7.0
 */
public interface EventSource {

	/**
	 * Returns number of invalidation listeners registered.
	 * 
	 * @return invalidation listener count
	 */
    public int getInvalidationListenerCount();
    
	/**
	 * Returns number of pre-invalidation listener registered. It should be 0 or 1.
	 * 
	 * @return pre-invalidation listener count
	 */
    public int getPreInvalidationListenerCount();

	/**
	 * Returns mumber of change listeners registered.
	 * 
	 * @return change listener count
	 */
    public int getChangeListenerCount();

	/**
	 * Invokes this method when the invalidation event is being fired.
	 */
    public void fireEvent(InvalidationEvent event);
    
    /**
	 * Invokes this method prior to the invalidation event occuring and is used to provide
	 * a callback that allows the listener to approve/deny an invalidation from occuring.
	 */
    public boolean shouldInvalidate(Object id, int source, int cause);

	/**
	 * Invokes this method when the change event is being fired.
	 */
    public void cacheEntryChanged(ChangeEvent event);

    /**
     * Invokes this method when an invalidation listener is being added.
     *
     * @param listener the invalidation listener object
     * @see #removeListener(com.ibm.websphere.cache.InvalidationListener)
     */
    public void addListener(InvalidationListener listener);

    /**
     * Invokes this method when an invalidation listener is being removed.
     *
     * @param listener the invalidation listener object
     * @see #addListener(com.ibm.websphere.cache.InvalidationListener)
     */
    public void removeListener(InvalidationListener listener);

    /**
     * Invokes this method when a change listener is being added.
     *
     * @param listener the invalidation listener object
     * @see #removeListener(com.ibm.websphere.cache.ChangeListener)
     */
    public void addListener(ChangeListener listener);

    /**
     * Invokes this method when a change listener is being removed.
     *
     * @param listener the change listener object
     * @see #addListener(com.ibm.websphere.cache.ChangeListener)
     */
    public void removeListener(ChangeListener listener);
    
    /**
     * Invokes this method when a pre-invalidation listener is being added.
     * 
     * @param listener the pre-invalidation listener object
     * @see #removeListener(com.ibm.websphere.cache.PreInvalidationListener)
     */
    public void addListener(PreInvalidationListener listener);
    
    /**
     * Invokes this method when a pre-invalidation listener is being removed.
     * 
     * @param listener the pre-invalidation listener object
     * @see #addListener(com.ibm.websphere.cache.PreInvalidationListener)
     */
    public void removeListener(PreInvalidationListener listener);
    
    
}
