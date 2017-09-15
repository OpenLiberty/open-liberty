package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * The EventSemaphore interface provides operations that wait for and post an
 * event semaphore.
 * <p>
 * This is specifically to handle the situation where the event may have been
 * posted before the wait method is called.  This behaviour is not supported by
 * the existing wait and notify methods.
 */
public final class EventSemaphore
{
    boolean _posted;

    /**
     * Default Constructor
     */
    public EventSemaphore() {}


    /**
     * Creates the event semaphore in the given posted state.
     * 
     * @param posted  Indicates whether the semaphore should be posted.
     */
    EventSemaphore( boolean posted )
    {
        _posted = posted;
    }


    /**
     * Waits for the event to be posted.
     *  <p>
     *  If the event has already been posted, then the operation returns immediately.
     * 
     * @exception InterruptedException
     *                   The wait was interrupted.
     */
    synchronized public void waitEvent() throws InterruptedException
    {
        while ( !_posted )
        {
            wait();            
        }
    }


    /**
     * Posts the event semaphore.
     *  <p>
     *  All waiters are notified.
     */
    public synchronized void post()
    {
        if ( !_posted )
            notifyAll();
        _posted = true;
    }


    /**
     * Clears a posted event semaphore.
     */
    public synchronized void clear()
    {
        _posted = false;
    }
}