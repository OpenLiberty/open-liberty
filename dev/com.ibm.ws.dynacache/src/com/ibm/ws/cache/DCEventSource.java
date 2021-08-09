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
package com.ibm.ws.cache;

import java.util.HashSet;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.cache.ChangeEvent;
import com.ibm.websphere.cache.ChangeListener;
import com.ibm.websphere.cache.InvalidationEvent;
import com.ibm.websphere.cache.InvalidationListener;
import com.ibm.websphere.cache.PreInvalidationListener;
import com.ibm.wsspi.cache.EventSource;

/**
 * An non-asynch event source is used to add listeners, remove listeners and fire events.
 */
public class DCEventSource implements EventSource {
	
	private static TraceComponent tc = Tr.register(DCEventSource.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    private String cacheNameNonPrefixed;
    
    static final InvalidationListener[] EMPTY_INV_LISTENERS = new InvalidationListener[0];
    static final ChangeListener[] EMPTY_CHANGE_LISTENERS    = new ChangeListener[0];

    private int invalidationListenerCount = 0;
    private int preInvalidationListenerCount = 0;
    private int changeListenerCount = 0;

    private boolean bUpdateInvalidationListener = false;
    private boolean bUpdateChangeListener       = false;

    private HashSet <InvalidationListener> hsInvalidationListeners = new HashSet <InvalidationListener>(2);
    private HashSet <ChangeListener> hsChangeListeners       = new HashSet <ChangeListener>(2); 

    private InvalidationListener currentInvalidationListeners[] = EMPTY_INV_LISTENERS;
    private PreInvalidationListener currentPreInvalidationListener;
    private ChangeListener currentChangeListeners[] = EMPTY_CHANGE_LISTENERS;
    
    private boolean _async = false;

    public DCEventSource(String cacheNameNonPrefixed, boolean async) {
    	this.cacheNameNonPrefixed = cacheNameNonPrefixed;
    	_async = async;
    }

    /**
     * Get invalidation listener count.
     */
    public int getInvalidationListenerCount() {
        return invalidationListenerCount;
    }
    
    /**
     * Get invalidation listener count.
     */
    public int getPreInvalidationListenerCount() {
        return preInvalidationListenerCount;
    }


    /**
     * Get change listener count.
     */
    public int getChangeListenerCount() {
        return changeListenerCount;
    }

    /**
     * The listeners are called when the fireEvent method is invoked.
     * @param event The invalidation event to be fired.
     */
    public void fireEvent(final InvalidationEvent event) {
        if (bUpdateInvalidationListener) {
            synchronized (hsInvalidationListeners) {
                if (invalidationListenerCount > 0) {
                    currentInvalidationListeners = new InvalidationListener[invalidationListenerCount];
                    hsInvalidationListeners.toArray(currentInvalidationListeners);
                } else {
                    currentInvalidationListeners = EMPTY_INV_LISTENERS;
                }
                bUpdateInvalidationListener = false;
            }
        }
        try {
        	event.m_cacheName = this.cacheNameNonPrefixed;
			for (int i = 0; i < currentInvalidationListeners.length; i++) {
				final InvalidationListener currentIL = currentInvalidationListeners[i];
				if (_async) {
					Scheduler.submit(new Runnable() {
						@Override
						public void run() {
							currentIL.fireEvent(event);
						}
					});
				} else {
					currentIL.fireEvent(event);
				}
			}
        }
        catch (Throwable t) {
        	com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.DCEventSource.fireEvent", "85", this);
        	if (tc.isDebugEnabled()) {
        		Tr.debug(tc, "Exception thrown in fireEvent method of InvalidationListener\n" + t.toString());
        	}
        }
    }

    /**
     * This adds a new listener to the Invalidation listener.
     * @param listener The listener to be added.
     */
    public void addListener(InvalidationListener listener) {
        synchronized (hsInvalidationListeners) {
            hsInvalidationListeners.add(listener);
            invalidationListenerCount = hsInvalidationListeners.size();
            bUpdateInvalidationListener = true;
        }
    }

    /**
     * This removes a specified listener for Invalidation listener. If it was not already registered
     * then this call is ignored.
     * @param listener The listener to be removed.
     */
    public void removeListener(InvalidationListener listener) {
        synchronized (hsInvalidationListeners) {
            hsInvalidationListeners.remove(listener);
            invalidationListenerCount = hsInvalidationListeners.size();
            bUpdateInvalidationListener = true;
        }
    }
    
    /**
     * The listeners are called when the preInvalidate method is invoked.
     * @param event The invalidation event to be pre-invalidated.
     */
    public boolean shouldInvalidate (Object id, int sourceOfInvalidation, int causeOfInvalidation) {
    	boolean retVal = true;
        if (preInvalidationListenerCount > 0) {
        	// In external implementation, catch any exceptions and process
        	try {
        		retVal = currentPreInvalidationListener.shouldInvalidate(id, sourceOfInvalidation, causeOfInvalidation);
        	}
        	catch (Throwable t) {
        		com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.DCEventSource.shouldInvalidate", "120", this);
        		if (tc.isDebugEnabled()) {
            		Tr.debug(tc, "Exception thrown in shouldInvalidate method of PreInvalidationListener\n" + t.toString());
            	}
        	}
        }
        
        return retVal; //invalidate
    }

    /**
     * This adds a new listener to the PreInvalidation listener.
     * @param listener The listener to be added.
     */
    public void addListener(PreInvalidationListener listener) {
    	if (preInvalidationListenerCount == 1 && tc.isDebugEnabled())
    		Tr.debug(tc, "Over-writing current PreInvalidationListener with new one");
        currentPreInvalidationListener = listener;
        preInvalidationListenerCount = 1;
    }

    /**
     * This removes a specified listener for PreInvalidation listener. If it was not already registered
     * then this call is ignored.
     * @param listener The listener to be removed.
     */
    public void removeListener(PreInvalidationListener listener) {
    	if (listener == currentPreInvalidationListener) {
	        currentPreInvalidationListener = null;
	        preInvalidationListenerCount = 0;
    	}
    }

    /**
     * The listeners are called when the cacheEntryChange method is invoked.
     * @param event The Change event to be fired.
     */
    public void cacheEntryChanged(final ChangeEvent event) {
        if (bUpdateChangeListener) {
            synchronized (hsChangeListeners) {
                if (changeListenerCount > 0) {
                    currentChangeListeners = new ChangeListener[changeListenerCount];
                    hsChangeListeners.toArray(currentChangeListeners);
                } else {
                    currentChangeListeners = EMPTY_CHANGE_LISTENERS;
                }
                bUpdateChangeListener = false;
            }
        }
        try {
        	event.m_cacheName = this.cacheNameNonPrefixed;
			for (int i = 0; i < currentChangeListeners.length; i++) {
				final ChangeListener cl = currentChangeListeners[i];
				if (_async) {
					Scheduler.submit(new Runnable() {
						@Override
						public void run() {
							cl.cacheEntryChanged(event);
						}
					});
				} else {
					cl.cacheEntryChanged(event);
				}
			}
        }
        catch (Throwable t) {
        	com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.DCEventSource.cacheEntryChanged", "169", this);
        	if (tc.isDebugEnabled()) {
        		Tr.debug(tc, "Exception thrown in cacheEntryChanged method of ChangeListener\n" + t.toString());
        	}
        }
    }

    /**
     * This adds a new change listener to the Change listener.
     * @param listener The listener to be added.
     */
    public void addListener(ChangeListener listener) {
        synchronized (hsChangeListeners) {
            hsChangeListeners.add(listener);
            changeListenerCount = hsChangeListeners.size();
            bUpdateChangeListener = true;
        }
    }

    /**
     * This removes a specified listener for the Change listener. If it was not already registered
     * then this call is ignored.
     * @param listener The listener to be removed.
     */
    public void removeListener(ChangeListener listener) {
        synchronized (hsChangeListeners) {
            hsChangeListeners.remove(listener);
            changeListenerCount = hsChangeListeners.size();
            bUpdateChangeListener = true;
        }
    }

}
