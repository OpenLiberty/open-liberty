/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction.transactions;

import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.message.Request;

import java.util.TimerTask;

import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr;

public abstract class SIPTransactionImpl
		implements SIPTransaction
{
	public static final int STATE_BEFORE_STATE_MACHINE_PROCESSE = -1;
	
	/** 
	 * id to set to the transactions 
	 **/
	private static long s_transactionId = 0;
	
	/** 
	 * logical jain id 
	 **/
	private long m_id;
	
	/** 
	 * transaction method 
	 **/
	private BranchMethodKey m_branchMethodId;
			
	/** 
	 * transactionState 
	 **/
	private int m_state;
		
	/** 
	 * Original request that is being handled by this transaction 
	 **/
	private Request			m_firstReq;
			
	/** 
	 * the provider on which the transaction is handled 
	 **/
	private final SipProvider m_provider;
		
	/** 
	 * parent stack 
	 **/
	private SIPTransactionStack m_stack;
	
	/** 
	 * transport connection 
	 **/
	private SIPConnection m_transportConnection;

	/**
	 *	Transaction constructor.
	 *	@param newParentStack Parent stack for this transaction.
	 *	@param newEncapsulatedChannel 
	 * 		Underlying channel for this transaction.
	 */
	public SIPTransactionImpl(SIPTransactionStack transactionStack,
		SipProvider provider,
		Request req, BranchMethodKey key, long transactionId) 
	{
		m_id = transactionId == -1 ? getNextTransactionId() : transactionId;
		m_stack = transactionStack;
		setState(STATE_BEFORE_STATE_MACHINE_PROCESSE);
		m_firstReq = req;
		m_branchMethodId = key;
		if (provider == null) {
			throw new IllegalArgumentException("null provider [" + this + ']');
		}
		m_provider = provider;
	}
		
	/**
	 * Allocate the next transaction id. 
     * @return
     */
    public final static synchronized long getNextTransactionId()
    {
		return s_transactionId++;
    }

    /** 
	 * get the stack for the transaction 
	 **/
	public SIPTransactionStack getParentStack()
	{
		return m_stack;
	}

	/** 
	 * the request that started the transaction 
	 **/
	public final Request getFirstRequest()
	{
		return m_firstReq;
	}
	
	/** the transaction id */
	public long getId()
	{
		return m_id;
	}
	
	/**
	 * get the Id by branch and name
	 */
	public BranchMethodKey getBranchMethodId()
	{
		return m_branchMethodId;
	}

	/**
	 *Gets the current setting for the branch parameter of this transaction.
	 *@return Branch parameter for this transaction.
	 */
	public final String getBranch() 
	{	
		return m_branchMethodId.getBranch();
	}


	/**
	 *Changes the state of this transaction.
	 *@param newState New state of this transaction.
	 */
	public final void setState(int newState) 
	{
		m_state = newState;
	}

	/**
	 *	Gets the current state of this transaction.
	 *	@return Current state of this transaction.
	 */
	public final int getState() 
	{
		return m_state;
	}

	/**
	 * gets the transaction state as a String for logging purposes
	 * @return one of: "Init", "Calling", "Trying", "Proceeding", "Completed",
	 *  "Confirmed", "Terminated", or a number
	 */
	public String getStateAsString() {
		return m_state == STATE_BEFORE_STATE_MACHINE_PROCESSE
			? "Init"
			: String.valueOf(m_state);
	}

	/** 
	 * should return false on UDP , true on TCP or tls
	 *   
	 */
	public boolean isTransportReliable()
	{
		return ( (ListeningPointImpl)getProviderContext().getListeningPoint()).isReliable();
	}
	
	/** return the provider in which this transaction acts */
	public SipProvider getProviderContext()
	{
		return m_provider;	
	}

	/** the connection to the transport layer */
	public void setTransportConnection( SIPConnection connection )
	{
		m_transportConnection = connection;	
	}
	
	/** the connection to the transport layer */
	public SIPConnection getTransportConnection()
	{
		return m_transportConnection;		
	}
	
	/** 
	 * has the transaction initiated yet 
	 **/
	public synchronized boolean hasInitiated()
	{
		return m_state != STATE_BEFORE_STATE_MACHINE_PROCESSE;
	}
	
	
	/**
 	 * @param task - the task to execute
	 * @param delay - delay to wait until fire
	 */
	protected void addTimerTask( TimerTask task , long delay )
	{
		// a timer value of 0 implies the timer is disabled (never fires)
		if (delay > 0) {
			getParentStack().getTimer().schedule( task , delay );
		}
	}
	
	/**
	 * @return the SIP call ID related to this transaction
	 */
	public final String getCallId() {
		return m_firstReq.getCallIdHeader().getCallId();
	}
	
	/** get the type of the transaction , for print only */
	protected abstract String getType();
	
	/** to String */
	public String toString()
	{
		StringBuffer retVal = new StringBuffer(256);
		retVal.append("type [");
		retVal.append(getType());
		retVal.append("] id [");
		retVal.append(m_id);
		retVal.append("] branch+method [" );
		retVal.append(m_branchMethodId);
		retVal.append("] state [");
		retVal.append(getStateAsString());
		retVal.append(']');
		return retVal.toString();
	}
	
	/**
	 * update SIP timers invocations counters
	 */
	protected void updateSipTimersInvocationsPMICounter() {
		StackExternalizedPerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if(perfMgr != null) {
			perfMgr.updateSipTimersInvocationsCounter();
		}
	}
}
