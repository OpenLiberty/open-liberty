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
package com.ibm.ws.sip.stack.transaction.transactions.ct;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.extensions.IbmRetransmissionIntervalHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.IbmRetransmissionMaxIntervalHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.IbmTransactionTimeoutHeader;
import com.ibm.ws.sip.stack.dispatch.TimerEvent;
import com.ibm.ws.sip.stack.transaction.SIPStackConfiguration;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transactions.BranchMethodKey;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionHelper;
import com.ibm.ws.sip.stack.transaction.transport.SIPTransportException;

public class SIPNonInviteClientTransactionImpl
	extends SIPClientTransactionImpl
{
	public static final int STATE_TRYING = 0;
	
	private TimerF m_timerF;
	private TimerE m_timerE;
	private TimerK m_timerK;
	
	/** value of timer F for this transaction, in milliseconds */
	private final int m_timerFvalue;
	
	/** value of timer E for this transaction, in milliseconds */
	private int m_timerEvalue;
	
	private Response m_lastResponse; 
	
	/** value of timer T2 for this transaction, in milliseconds */
	private final int m_timerT2value;

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger =
		Log.get(SIPNonInviteClientTransactionImpl.class);
	
						
	public SIPNonInviteClientTransactionImpl(
		SIPTransactionStack transactionStack,
		SipProvider provider,
		Request req, BranchMethodKey key, long transactionId) 
	{
		super(transactionStack, provider, req, key, transactionId);
		m_timerFvalue = getTimerF(req);
		m_timerEvalue = getTimerE(req);
		m_timerT2value = getTimerT2(req);
	}
	
	/**
	 * prosses the request in a state machine as stated in RFC 17.2.2
	 */
	public synchronized void processRequest(Request sipRequest)
		throws SipParseException
	{				
		try {				
			switch (getState()) {
			case STATE_BEFORE_STATE_MACHINE_PROCESSE:
				setState(STATE_TRYING);

				// timer f
				m_timerF = new TimerF(this, getCallId());
				addTimerTask(m_timerF, m_timerFvalue);
				sendRequestToTransport(sipRequest);
				
				// timer e
				if (!isTransportReliable()) {
					// first send, then set the timer, otherwise it's possible
					// the timer will fire before sending the request
					m_timerE = new TimerE(this, getCallId());
					addTimerTask(m_timerE, m_timerEvalue);
				}
				break;

			case STATE_TRYING:
				//no prosses																	
				break;

			case STATE_PROCEEDING:
				//no prosses
				break;

			case STATE_COMPLETED:
				//no prosses
				break;
			}
		}
		catch( SIPTransportException exp )
		{
			prossesTransportError();
		}
											 					  			
	}
	
	/**
	 * prosses the response in a state machine as stated in RFC 17.2.2
	 * 
	 */
	public synchronized void processResponse(Response sipResponse) 
		throws SipParseException
	{
		
			switch(getState())
			{
				case STATE_TRYING:									
					if( SIPTransactionHelper.isProvionalResponse(sipResponse.getStatusCode() ) )
					{
						setState( STATE_PROCEEDING);
						sendResponseToUA( sipResponse );
					}
					else if( SIPTransactionHelper.isFinalResponse(sipResponse.getStatusCode()))
					{
						setFinalResponse( sipResponse );
						sendResponseToUA( sipResponse );
						setCompletedState();
					}
					
					break;
					
				case STATE_PROCEEDING:
					
					if(SIPTransactionHelper.isFinalResponse(sipResponse.getStatusCode()))
					{
						setFinalResponse( sipResponse );
						sendResponseToUA( sipResponse );
						setCompletedState();
					}
					
					break;
				case STATE_COMPLETED:
					//if we reaced here , it means this is a response retransimision
					//so ignore this , since the response was allready sent to the aplication
					 break; 		
			}
		
		m_lastResponse = sipResponse;								 					  	

	}
	
	
	
	/**
	 *  prosses transport error
	 */	
	public synchronized void prossesTransportError()
	{
		notifyRequestErrorToUA( getFirstRequest());
		destroyTransaction();	
	}
	
	
	
	private synchronized void setCompletedState()
	{
		//cancel timers
		if( m_timerE!=null ) m_timerE.cancel();
		if( m_timerF!=null ) m_timerF.cancel();
		
		if( isTransportReliable())
		{
			//go strait to the terminated state
			//timer is 0
			destroyTransaction();
		}
		else
		{
			setState( STATE_COMPLETED );
			long delay = getParentStack().getConfiguration().getTimerK();			
			m_timerK = new TimerK(this, getCallId());
			addTimerTask( m_timerK , delay );
		}
	}
	
	/**
	 * gets the value of timer E for the given message:
	 * If the IBM-RetransmissionInterval header is present, this value is used. otherwise..
	 * If a timer value is specified in configuration, its value is used, otherwise..
	 * The default timer value (as specified in RFC 3261) is used.
	 * 
	 * @param request the request to send
	 * @return the value of timer E to use in this transaction
	 */
	private static int getTimerE(Request request) {
		// 1. get from message
		int timerValue = -1;
		try {
			IbmRetransmissionIntervalHeader header = (IbmRetransmissionIntervalHeader)
				request.getHeader(IbmRetransmissionIntervalHeader.name, true);
			if (header != null && header.applicationCreated()) {
				timerValue = header.getTimeValue();
				request.removeHeader(IbmRetransmissionIntervalHeader.name, true);
			}
		}
		catch (HeaderParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SIPNonInviteClientTransactionImpl.class, "getTimerE",
					"error getting header [" + IbmRetransmissionIntervalHeader.name
					+ "] in message\r\n" + request,
					e);
			}
		}

		// 2. get from configuration
		if (timerValue == -1) {
			SIPStackConfiguration config = SIPTransactionStack.instance().getConfiguration();
			timerValue = config.getTimerE();
		}
		return timerValue;
	}
	
	/**
	 * gets the value of timer F for the given message:
	 * If the IBM-TransactionTimeout header is present, this value is used. otherwise..
	 * If a timer value is specified in configuration, its value is used, otherwise..
	 * The default timer value (as specified in RFC 3261) is used.
	 * 
	 * @param request the request to send
	 * @return the value of timer F to use in this transaction
	 */
	private static int getTimerF(Request request) {
		// 1. get from message
		int timerValue = -1;
		try {
			IbmTransactionTimeoutHeader header = (IbmTransactionTimeoutHeader)
				request.getHeader(IbmTransactionTimeoutHeader.name, true);
			if (header != null && header.applicationCreated()) {
				timerValue = header.getTimeValue();
				request.removeHeader(IbmTransactionTimeoutHeader.name, true);
			}
		}
		catch (HeaderParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SIPNonInviteClientTransactionImpl.class, "getTimerF",
					"error getting header [" + IbmTransactionTimeoutHeader.name
					+ "] in message\r\n" + request,
					e);
			}
		}

		// 2. get from configuration
		if (timerValue == -1) {
			SIPStackConfiguration config = SIPTransactionStack.instance().getConfiguration();
			timerValue = config.getTimerF();
		}
		return timerValue;
	}
	
	/**
	 * gets the value of timer T2 for the given message:
	 * If the IBM-RetransmissionMaxInterval header is present, this value is used. otherwise..
	 * If a timer value is specified in configuration, its value is used, otherwise..
	 * The default timer value (as specified in RFC 3261) is used.
	 * 
	 * @param request the request to send
	 * @return the value of timer T2 to use in this transaction
	 */
	static int getTimerT2(Request request) {
		// 1. get from message
		int timerValue = -1;
		try {
			IbmRetransmissionMaxIntervalHeader header = (IbmRetransmissionMaxIntervalHeader)
				request.getHeader(IbmRetransmissionMaxIntervalHeader.name, true);
			if (header != null && header.applicationCreated()) {
				timerValue = header.getTimeValue();
				request.removeHeader(IbmRetransmissionMaxIntervalHeader.name, true);
			}
		}
		catch (HeaderParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SIPNonInviteClientTransactionImpl.class, "getTimerT2",
					"error getting header [" + IbmRetransmissionIntervalHeader.name
					+ "] in message\r\n" + request,
					e);
			}
		}

		// 2. get from configuration
		if (timerValue == -1) {
			SIPStackConfiguration config = SIPTransactionStack.instance().getConfiguration();
			timerValue = config.getTimerT2();
		}
		return timerValue;
	}

	/**
	 * called when timer E fires
	 */
	synchronized void timerEfired()
	{
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerEfired",
				"Timer E fired on transaction " + toString());
		}
		updateSipTimersInvocationsPMICounter();
		boolean resend;
		switch (getState()) {
		case STATE_TRYING:
			m_timerEvalue = 2*m_timerEvalue > m_timerT2value
				? m_timerT2value
				: 2*m_timerEvalue;
			resend = true;
			break;
		case STATE_PROCEEDING:
			m_timerEvalue = m_timerT2value;
			resend = true;
			break;
		default:
			resend = false;
		}
		if (resend) {
			try {
				sendRequestToTransport(getFirstRequest());
				m_timerE = new TimerE(this, getCallId());
				addTimerTask(m_timerE, m_timerEvalue);
			}
			catch (SIPTransportException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "timerEfired", "", e);
				}
				destroyTransaction();
			}
		}
	}
	
	
	/** destroy the transaction */
	public synchronized void destroyTransaction()
	{
		setState( STATE_TERMINATED );
		
		//destroy all transactions
		if( m_timerE!=null )
		{
			m_timerE.cancel();
		}
		if( m_timerF!=null )
		{
			m_timerF.cancel();
		}
		if( m_timerK!=null )
		{
			m_timerK.cancel();
		}
		
		startAPITimer();		
	}

	/** return the most recent response */
	public synchronized Response getMostRecentResponse()
	{
		return m_lastResponse;
	}

	/** type of transaction */
	protected  String getType()
	{	
		return "Client non-INVITE";
	}
	
	static class TimerE extends TimerEvent
	{
		/**
		 * parent transaction
		 */
		private SIPNonInviteClientTransactionImpl m_ct;
	
		TimerE(SIPNonInviteClientTransactionImpl ct, String callId) {
			super(callId);
			m_ct = ct;
		}
	
		public void onExecute()
		{
			if (m_ct != null) {
				m_ct.timerEfired();
				m_ct = null;
			}
		}
		
		public boolean cancel()
		{
			return super.cancel(); 
		}		
	}
	
	/**
	 * @author Amirk
	 *  timer F for this transaction
	 */
	public class TimerF extends TimerEvent
	{
		
		private SIPNonInviteClientTransactionImpl m_ct;
	
		TimerF( SIPNonInviteClientTransactionImpl ct, String callId)
		{
			super(callId);
			m_ct = ct;
		}
	
		public void onExecute()
		{	
			if (m_ct != null) {
				m_ct.timerFfired();
			}
		}
	
		public boolean cancel()
		{
			return super.cancel();
		}
	}

	/**
	 * called when TimerF fires
	 */
	void timerFfired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerFfired",
				"Timer F fired on transaction " + toString());
		}
		updateSipTimersInvocationsPMICounter();
		if (getState()==SIPNonInviteClientTransactionImpl.STATE_TRYING || 
			getState()==SIPNonInviteClientTransactionImpl.STATE_PROCEEDING)
		{
			//notify on time out
			notifyTransactionTimeoutToUA();
			destroyTransaction();
		}
	}

	static class TimerK extends TimerEvent
	{

		private SIPNonInviteClientTransactionImpl m_ct;
	
		TimerK( SIPNonInviteClientTransactionImpl ct, String callId)
		{
			super(callId);
			m_ct = ct;
		}
	
		public void onExecute()
		{	
			if (m_ct != null) {
				m_ct.timerKfired();
			}
		}	
	
		public boolean cancel()
		{
			return super.cancel();
		}			
	}

	/**
	 * called when TimerK fires
	 */
	void timerKfired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerKfired",
				"Timer K fired on transaction " + toString());
		}
		updateSipTimersInvocationsPMICounter();
		if (getState() == STATE_COMPLETED) {
			destroyTransaction();				
		}
	}

	/**
	 * log Mgr for class
	 */
	public LogMgr getLoger()
	{
		return c_logger;
	}	

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionImpl#getStateAsString()
	 */
	public String getStateAsString() {
		switch (getState()) {
		case STATE_TRYING:
			return "Trying";
		default:
			return super.getStateAsString();
		}
	}
	
}
