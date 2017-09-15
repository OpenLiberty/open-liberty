/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.EventListener;
import java.util.EventObject;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
/**
 * List of EventListeners.  This class is optimized to allow unsynchronized
 * access to a list of listeners.  Management of listeners is done using
 * the addListener() and removeListener() methods.  There are 2 supported methods
 * for firing events to the listeners in the list.
 * <OL>
 * <LI>Use the getListenerArray() method to retrieve the array of listeners and then
 *     manually iterate through the listeners and invoke the appropriate listener method.
 * <LI>Define a EventListenerV visitor class and use the fireEvent() method to cause
 *    the visitor to visit each listener in the list.
 * </OL>
 * 
 */
public class EventListeners {
    private final static EventListener[] EMPTY_LISTENERS = new EventListener[0];
protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.util.EventListeners";
    /**
     * The list of listeners.
     */
    private EventListener[] listeners = EMPTY_LISTENERS;

    /**
     *
     */
    public final EventListener[] getListenerArray() {
        return listeners;
    }

    /**
     * Fire the event to all listeners by allowing the visitor
     * to visit each listener. The visitor is responsible for
     * implementing the actual firing of the event to each listener.
     */
    public final void fireEvent(EventObject evt, EventListenerV visitor){
        EventListener[] list = getListenerArray();
        for(int i=0; i<list.length; i++){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
        		logger.logp(Level.FINE, CLASS_NAME,"fireEvent", "Use visitor " + visitor + " to fire event to " + list[i] + ", class:" +list[i].getClass());
            visitor.fireEvent(evt, list[i]);
        }
    }

    /**
     * Return the total number of listeners for this listenerlist
     */
    public final int getListenerCount() {
        return listeners.length;
    }

    /**
     * Add the listener as a listener to the list.
     * @param l the listener to be added
     */
    public final synchronized void addListener(EventListener l) {
        if(l ==null) {
            throw new IllegalArgumentException("Listener " + l +
                                               " is null");
        }
        if(listeners == EMPTY_LISTENERS) {
            listeners = new EventListener[1];
            listeners[0] = l;
        }
        else {
            int i = listeners.length;
            EventListener[] tmp = new EventListener[i+1];
            System.arraycopy(listeners, 0, tmp, 0, i);

            tmp[i] = l;
            listeners = tmp;
        }
    }

    /**
     * Remove the listener.
     * @param l the listener to be removed
     */
    public final synchronized void removeListener(EventListener l) {
        if(l ==null) {
            throw new IllegalArgumentException("Listener " + l +
                                               " is null");
        }

        // Is l on the list?
        int index = -1;
        for(int i = listeners.length-1; i>=0; i--) {
            if(listeners[i].equals(l) == true) {
                index = i;
                break;
            }
        }

        // If so,  remove it
        if(index != -1) {
            EventListener[] tmp = new EventListener[listeners.length-1];
            // Copy the list up to index
            System.arraycopy(listeners, 0, tmp, 0, index);
            // Copy from two past the index, up to
            // the end of tmp (which is two elements
            // shorter than the old list)
            if(index < tmp.length)
                System.arraycopy(listeners, index+1, tmp, index, 
                                 tmp.length - index);
            // set the listener array to the new array or null
            listeners = (tmp.length == 0) ? EMPTY_LISTENERS : tmp;
        }
    }

    /**
     * Return a string representation of the EventListenerList.
     */
    public String toString() {
        Object[] lList = getListenerArray();
        String s = "EventListenerList: ";
        s += getListenerCount() + " listeners: ";
        for(int i = 0 ; i < lList.length ; i++) {
            s += " listener " + lList[i] + "\n";
        }
        return s;
    }
}

