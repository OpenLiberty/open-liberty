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
package com.ibm.ws.sip.container.timer;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class ExpirationTimer extends BaseTimer {

	 /**
	 * 
	 */
	private static final long serialVersionUID = -3321722369180702164L;
	
    /**
     * Reference to the ExpirationTimerListener
     */
    private ExpirationTimerListener m_expInvoker;
    //amirp: Remove all references to invoker and replace with listener
    
    /**
     * The SAS synchronizer.
     */
    private Object m_synchronizer;
    
    /**
     * Constructs a new Servlet Timer associated with the given application 
     * session. 
     * @param expInvoker   the invoker of this timer
     * @param synchronizer the SAS synchronizer
     */
    public ExpirationTimer(ExpirationTimerListener expInvoker, Object synchronizer)
    {
        m_expInvoker = expInvoker;
        m_synchronizer = synchronizer;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer(96);
        buffer.append("Appication Session Timer:");
        buffer.append(m_expInvoker);
        buffer.append(" ,Delay:");
        buffer.append(getDelay());
        buffer.append(" ,Period:");
        buffer.append(getPeriod());
        buffer.append(" ,Fixed Delay:");
        buffer.append(isFixedDelay());
        buffer.append(" ,Cancelled:");
        buffer.append(isCancelled());

        return buffer.toString();
    }
     
    /** 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o)
    {
        int rc = super.compareTo(o);
        
        //We will get a value 0 when they have the same invocation time. 
        if(rc == 0  && o instanceof ExpirationTimerListener)
        {
            ExpirationTimerListener otherExpirationInvoker = (ExpirationTimerListener) o;
            
            //In case we have match in invocation time then compare the Ids
            //of the session to determine the order. 
            rc = otherExpirationInvoker.getId().compareTo(m_expInvoker.getId());
        }
        
        return rc; 
    }
    
    /**
     * This method is synchronized so that when rescheduling is called on expiration and on the same time
     * AppSession is invalidated, causing this method to be called, an unnecessary exception
     * can be avoided. We synchronize the rescheduling operation on this timer's handle. 
     * @see com.ibm.ws.sip.container.timer.BaseTimer#cancel()
     */
    public void cancel() {
    	synchronized (getSynchronizer()) {
    		super.cancel();
    		m_expInvoker = null;
    	}
    }
    
    /**
     * This method might be executed multi-threaded or single threaded,
     * depends on the TasksInvoker definition 
     * @see com.ibm.ws.sip.container.events.TasksInvoker
     * @see com.ibm.ws.sip.container.SipContainer#setTimerInvoker()
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	synchronized (getSynchronizer()) {
    		if(m_expInvoker != null) {
    			m_expInvoker.invokeExpiredTimer(); // defect 642320, the invocation has to be in-sync with the 
    		}									//cancel method of both the timer and the listener to avoid NPEs		
    	}								//Note that AppSessionTimerListener.cancel is also synced on 
    }										//the same handle

	/**
	 * Gets ExpirationTimerListener
	 * @return
	 */
	public ExpirationTimerListener getExpInvoker() {
		return m_expInvoker;
	}

	/**
	 * Sets ExpirationTimerListener
	 * @param invoker
	 */
	public void setExpInvoker(ExpirationTimerListener invoker) {
		m_expInvoker = invoker;
	}

	/**
	 * Extracts queue index from the related application session.
	 */
	protected int extractQueueIndex(){
		if(m_expInvoker!= null){
			String sessId = m_expInvoker.getApplicationId();
	    	int result = SipApplicationSessionImpl.extractAppSessionCounter(sessId);
			return result;
		}
		return 0;
	}

	/**
	 * An expiration timer task is critical to prevent memory leaks of unexpired sessions.
	 * @see com.ibm.ws.sip.container.util.Queueable#priority()
	 */
	public int priority(){
		return PRIORITY_CRITICAL;
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getServiceSynchronizer()
	 */
	public Object getServiceSynchronizer(){
		return m_expInvoker.getServiceSynchronizer();
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getAppName()
     */
	public String getAppName() {
		if(m_expInvoker != null) {
			return m_expInvoker.getApplicationName();
		}
		else return null;
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getAppIndexForPMI()
     */
	public Integer getAppIndexForPMI() {
		if(m_expInvoker != null) {
			return m_expInvoker.getApplicationIdForPMI();
		}
		else return null;
	}

	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getApplicationSession()
	 */
	@Override
	public SipApplicationSession getApplicationSession() {
		if (m_expInvoker instanceof AppSessionTimerListener) {
			return ((AppSessionTimerListener)m_expInvoker).getApplicationSession();
		}
		
		if (m_expInvoker instanceof TransactionUserTimerListener) {
			TransactionUserWrapper tu = ((TransactionUserTimerListener)m_expInvoker).getTuWrapper();
			return tu.getApplicationSession(false);
		}
		
		return null;
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getTuWrapper()
	 */
	@Override
	public TransactionUserWrapper getTuWrapper() {
		if (m_expInvoker instanceof TransactionUserTimerListener) {
			return ((TransactionUserTimerListener) m_expInvoker).getTuWrapper();
		}	
		return null;
	}
	
	/**
	 * @return the SAS synchronizer
	 */
	private Object getSynchronizer() {
		return m_synchronizer;
	}
}
