/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi.basic;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author anat, Mar 22, 2005
 */
public class SipContainerCounter {

    SessionsCounter _sessionsCounter = new SessionsCounter();
    
    /**  Sip counter that counts invoked tasks */
    private long _invokeQueueSize = 0;
    
    /** Replicated Sip Sessions counter */
    private final AtomicLong _replicatedSipSessions = new AtomicLong(0);
    
    /** Not replicated Sip Sessions counter */
    private final AtomicLong _notReplicatedSipSessions = new AtomicLong(0);
    
    /** Replicated Sip Application Sessions */
    private final AtomicLong _replicatedSipAppSessions = new AtomicLong(0);
    
    /** Not replicated Sip Application Sessions */
    private final AtomicLong _notReplicatedSipAppSessions = new AtomicLong(0);
    
    /** a lock that will use to sync the counter when it is updated from different threads**/
    private Object counterLock = new Object();
      
    /**
     * Ctor
     */
    public SipContainerCounter() {
    	
    }
    
    /**
     * Increment replicated Sip Sessions counter
     */
    public void incrementReplicatedSipSessionsCounter() {
    	_replicatedSipSessions.incrementAndGet();
    }
    
    /**
     * Decrement replicated Sip Sessions counter
     */
    public void decrementReplicatedSipSessionsCounter() {
    		_replicatedSipSessions.decrementAndGet();
    }
    
    /**
     * Increment not replicated Sip Sessions counter
     */
    public void incrementNotReplicatedSipSessionsCounter() {
    		_notReplicatedSipSessions.incrementAndGet();
    }
    
    /**
     * Decrement not replicated Sip Sessions counter
     */
    public void decrementNotReplicatedSipSessionsCounter() {
    		_notReplicatedSipSessions.decrementAndGet();
    }
    
    /**
     * Increment replicated Sip App Sessions counter
     */
    public void incrementReplicatedSipAppSessionsCounter() {
    		_replicatedSipAppSessions.incrementAndGet();
    }
    
    /**
     * Decrement replicated Sip App Sessions counter
     */
    public void decrementReplicatedSipAppSessionsCounter() {
    	_replicatedSipAppSessions.decrementAndGet();
    }
    
    /**
     * Increment not replicated Sip App Sessions counter 
     */
    public void incrementNotReplicatedSipAppSessionsCounter() {
    	_notReplicatedSipAppSessions.incrementAndGet();
    }
    
    /**
     * Decrement not replicated Sip App Sessions counter
     */
    public void decrementNotReplicatedSipAppSessionsCounter() {
    	_notReplicatedSipAppSessions.decrementAndGet();
    }
    
    /**
     * Increment objects number in invoker queue
     *  
     */
    public void invokeQueueIncrement() {
    	synchronized (counterLock){
            _invokeQueueSize ++;
        }
    }
    
    /**
     * Increment objects number in invoker queue
     *  
     */
    public void invokeQueueDecrement() {
    	synchronized (counterLock){
            _invokeQueueSize --;
        }
    }
    
    /**
     * @return Returns the invokeQueueSize.
     */
    public long getInvokeQueueSize() {
        return _invokeQueueSize;
    }
    
    /**
     * Returns object that represent Sessions counter
     * @return sessions counter
     */
    public SessionsCounter getSessionsCounter(){
        return _sessionsCounter;
    }
   
    /**
     * Get number of SipSessions
     * @return number of SipSessions
     */
    public long getSipSessions() {
       return _sessionsCounter.getSipSessions();
    }
    
    /**
     * Get number of SipApplicationSessions
     * @return number of SipAppSessions
     */
    public long getSipAppSessions() {
    	return _sessionsCounter.getSipAppSessions();
    }
    
    /**
     * Get number of replicated Sip Sessions
     */
    public long getReplicatedSipSessionsCounter() {
    	return _replicatedSipSessions.get();
    }
    
    /**
     * Get number of not replicated Sip Sessions
     */
    public long getNotReplicatedSipSessionsCounter() {
    	return _notReplicatedSipSessions.get();
    }
    
    /**
     * Get number of replicated Sip App Sessions
     */
    public long getReplicatedSipAppSessionsCounter() {
    	return _replicatedSipAppSessions.get();
    }
    
    /**
     * Get number of not replicated Sip App Sessions
     */
    public long getNotReplicatedSipAppSessionsCounter() {
    	return _notReplicatedSipAppSessions.get();
    }

}

