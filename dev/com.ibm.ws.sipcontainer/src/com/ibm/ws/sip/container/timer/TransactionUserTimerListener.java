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

import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * 
 * @author anat, Dec 1, 2005
 *
 * Perform the expiration operation for the Timer of the TransactionUser
 */
public class TransactionUserTimerListener extends ExpirationTimerListener {
    
    /**
     * Reference to the expiration Timer
     */
    private ExpirationTimer m_timer;

    /**
     * Application Session associated with this timer. 
     */
    private TransactionUserWrapper m_transactionUser;

    /**
     * Constructs a new TransactionUserTimerListener associated with the given transactionUser 
     * session. 
     * @param transactionUser
     */
    public TransactionUserTimerListener(TransactionUserWrapper transactionUser)
    {
    	m_transactionUser = transactionUser;
    	m_timer = new ExpirationTimer(this, m_transactionUser.getSynchronizer());
    }
	
	
    /**
     * Invoke the timer when expired
     */
    protected void invokeExpiredTimer() {
		m_transactionUser.transactionUserExpired();	
	}

	/**
	 * return Expires of the TransactinUser
	 */
	 public long getExpires() {
//		  return 0 because transaction User cannot be rescheduled to 
//		 more then Time defined in the Application Description
		 return 0;
	}

	/**
	 * Returns ID of this transactionUser
	 */
	 public String getId() {
		return m_transactionUser.getId();
	}
	
	/**
	 * Schedule this timer
	 * @param delay
	 */
	public void schedule(boolean isPersistent,long delay) {
		SipContainerComponent.getTimerService().schedule(m_timer,isPersistent,delay);
	}


	/**
	 * Cancel this Transaction User object 
	 */
	public void cancel() {
		m_timer.cancel();
	}


	/**
	 * Returns timer object associated with this TransactionUser
	 * @return
	 */
	public ExpirationTimer getTimer() {
		return m_timer;
	}


	/** @see com.ibm.ws.sip.container.timer.ExpirationTimerListener#getApplicationId()
	 */
	public String getApplicationId() {
		return m_transactionUser.getApplicationId();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.timer.ExpirationTimerListener#getApplicationName()
	 */
	public String getApplicationName() {
		if(m_transactionUser != null && m_transactionUser.getSipServletDesc() != null) {
			return m_transactionUser.getSipServletDesc().getSipApp().getAppName();
		}
		else return null;
	}
	
	/**
	 * @see com.ibm.ws.sip.container.timer.ExpirationTimerListener#getApplicationIdForPMI()
	 */
	public Integer getApplicationIdForPMI() {
		if(m_transactionUser != null && m_transactionUser.getSipServletDesc() != null) {
			return m_transactionUser.getSipServletDesc().getSipApp().getAppIndexForPmi();
		}
		else return null; 
	}


	public Object getServiceSynchronizer(){
		return m_transactionUser.getServiceSynchronizer();
	}
	
	/**
	 * @return the TransactionUserWrapper
	 */
	TransactionUserWrapper getTuWrapper() {
		return m_transactionUser;
	}
}
