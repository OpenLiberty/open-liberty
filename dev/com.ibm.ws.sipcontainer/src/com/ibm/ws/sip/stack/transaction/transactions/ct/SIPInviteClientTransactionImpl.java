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
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.SipJainFactories;
import com.ibm.ws.jain.protocol.ip.sip.extensions.IbmRetransmissionIntervalHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.IbmTransactionTimeoutHeader;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.dispatch.TimerEvent;
import com.ibm.ws.sip.stack.transaction.SIPStackConfiguration;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transactions.BranchMethodKey;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionHelper;
import com.ibm.ws.sip.stack.transaction.transport.SIPTransportException;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;
import com.ibm.ws.sip.stack.util.SipStackUtil;

public class SIPInviteClientTransactionImpl
	extends SIPClientTransactionImpl
{
	


	public static final int STATE_CALLING = 0;
	
	/**
	 * retransamission timer for UDP
	 */
	private TimerA m_timerA;
	
	/**
	 * timer for all transport types , controlls transaction timeouts
	 */
	private TimerB m_timerB;
	
	/**
	 * Timer D reflects the amount of time that the server transaction can remain in the "Completed" 
	 * state when unreliable transports are used
	 */
	private TimerD m_timerD;
	
	/** value of timer A for this transaction, in milliseconds */
	private int m_timerAvalue;
	
	/** value of timer B for this transaction, in milliseconds */
	private final int m_timerBvalue;
	
	/**
	 * the last response that was receives ( could be provisionning like RINGING )
	 */
	private Response  m_lastResponse;
	
	/**
	 * This parameter should contain the "IBM-Destination" header from original
	 * INVITE request. In case an error will be returned on the INVITE
	 * automatic ACK should contain this header.
	 * The "IBM-Destination" header will be removed from the outgoing request
	 * so after the request is sent - we cannot extract it from original INVITE.
	 */
	private Header _ibmDestinationHeader = null;
	
	/**
	 * IBM-Destination header name
	 */
	public final static String DESTINATION_URI = SipStackUtil.DESTINATION_URI;
	
	/**
	 * "IBM-PO" header name
	 */
	private static final String IBM_PO_HEADER = "IBM-PO";
	private Header _ibmPO = null;
	boolean outboundEnable = ApplicationProperties.getProperties().getBoolean(StackProperties.ENABLE_SET_OUTBOUND_INTERFACE);

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger =
		Log.get(SIPInviteClientTransactionImpl.class);
	
	public SIPInviteClientTransactionImpl(SIPTransactionStack transactionStack,
		SipProvider provider,
		Request req, BranchMethodKey key, long transactionId) 
	{
		super(transactionStack, provider, req, key, transactionId);
		m_timerAvalue = getTimerA(req);
		m_timerBvalue = getTimerB(req);
		SIPNonInviteClientTransactionImpl.getTimerT2(req); // just remove this header if it exists

		try {
			_ibmDestinationHeader = req.getHeader(DESTINATION_URI,true);
			
			// jlawwill (PI62617)
			//   Pull the IBM_PO_HEADER from the original packet.   We may have to reuse it later.
			_ibmPO = req.getHeader(IBM_PO_HEADER, true);
		} 
		catch (HeaderParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "SIPInviteClientTransactionImpl",
						"Failed to extract the IBM-Destination header");
			}
		} 
	}
	
	
	/**
	 * prosses the request in a state machine as stated in RFC 17.2.2
	 */
	public synchronized void processRequest(Request sipRequest)
		throws SipParseException
	{				
			try
			{
				//this could be either 
				String method = sipRequest.getMethod();
				switch(getState())
				{			
					case STATE_BEFORE_STATE_MACHINE_PROCESSE:					
							setState(STATE_CALLING);
							sendRequestToTransport(sipRequest);
							if (!isTransportReliable()) {
								m_timerA = new TimerA(this, getCallId());
								addTimerTask(m_timerA, m_timerAvalue);								
							}
							
							m_timerB = new TimerB(this, getCallId());
							addTimerTask(m_timerB, m_timerBvalue);
							break;
		
					case STATE_CALLING:					
						if( Request.INVITE.equals( method ) )
						{
							sendRequestToTransport( sipRequest );
						}
						else if( Request.ACK.equals( method ))
						{
							handleACK( sipRequest );
						}					
						break;																				
		
					case STATE_PROCEEDING:
						//this can be only ACK
						handleACK( sipRequest );
						break;
		
					case STATE_COMPLETED:
						//this can be only ACK
						handleACK( sipRequest );
						break;
				}
			}
			catch( SIPTransportException exp )
			{
				if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "processRequest", 
                                        "Exception in transport exception", exp);
                }
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
		
			m_lastResponse = sipResponse;
			switch(getState())
			{
				case STATE_CALLING:
						if( SIPTransactionHelper.isProvionalResponse(sipResponse.getStatusCode() ) )
						{
							setState( STATE_PROCEEDING);
							notCalling();
							sendResponseToUA( sipResponse );
						}
						else if( SIPTransactionHelper.isOKFinalResponse( sipResponse.getStatusCode() ) )
						{
							setFinalResponse(sipResponse);
							destroyTransaction();
							sendResponseToUA( sipResponse );							
						}	
						else if( SIPTransactionHelper.isNonOkFinalResponse( sipResponse.getStatusCode()))
						{
							setFinalResponse(sipResponse);
							sendAutomaticAckRequest();
							notCalling();
							setCompletedState();
							sendResponseToUA( sipResponse );				
						}												
						break;
					
				case STATE_PROCEEDING:
						if( SIPTransactionHelper.isProvionalResponse(sipResponse.getStatusCode() ) )
						{
							sendResponseToUA( sipResponse );
						}
						else if( SIPTransactionHelper.isOKFinalResponse( sipResponse.getStatusCode() ) )
						{
							setFinalResponse(sipResponse);
							destroyTransaction();
							sendResponseToUA( sipResponse );							
						}	
						else if( SIPTransactionHelper.isNonOkFinalResponse( sipResponse.getStatusCode() ) )
						{							
							setFinalResponse(sipResponse);
							setCompletedState();
							sendAutomaticAckRequest();							
							sendResponseToUA( sipResponse );
						}
									
						break;
						
				case STATE_COMPLETED:
						if( SIPTransactionHelper.isNonOkFinalResponse( sipResponse.getStatusCode() ) )
						{							
							sendAutomaticAckRequest();
						}				
					    break; 		
			}
														 					  	
	}
	
	/** handle ACK request */
	private void handleACK( Request sipRequest )
		throws SIPTransportException
	{
		sendRequestToTransport( sipRequest );
		setCompletedState();
	}
	
	/** move to completed state */
	private synchronized void setCompletedState()
	{
		if( isTransportReliable() )
		{
			//timer is 0 - go stait to terminate
			destroyTransaction();
		}
		else
		{
			setState( STATE_COMPLETED );
			long delay = getParentStack().getConfiguration().getTimerD();		
			m_timerD = new TimerD(this, getCallId());
			addTimerTask( m_timerD , delay );						
		}
	}
	
	/**
	 * gets the value of timer A for the given message:
	 * If the IBM-RetransmissionInterval header is present, this value is used. otherwise..
	 * If a timer value is specified in configuration, its value is used, otherwise..
	 * The default timer value (as specified in RFC 3261) is used.
	 * 
	 * @param request the request to send
	 * @return the value of timer A to use in this transaction
	 */
	private static int getTimerA(Request request) {
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
				c_logger.traceDebug(SIPInviteClientTransactionImpl.class, "getTimerA",
					"error getting header [" + IbmRetransmissionIntervalHeader.name
					+ "] in message\r\n" + request,
					e);
			}
		}

		// 2. get from configuration
		if (timerValue == -1) {
			SIPStackConfiguration config = SIPTransactionStack.instance().getConfiguration();
			timerValue = config.getTimerA();
		}
		return timerValue;
	}
	
	/**
	 * gets the value of timer B for the given message:
	 * If the IBM-TransactionTimeout header is present, this value is used. otherwise..
	 * If a timer value is specified in configuration, its value is used, otherwise..
	 * The default timer value (as specified in RFC 3261) is used.
	 * 
	 * @param request the request to send
	 * @return the value of timer B to use in this transaction
	 */
	private static int getTimerB(Request request) {
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
				c_logger.traceDebug(SIPNonInviteClientTransactionImpl.class, "getTimerB",
					"error getting header [" + IbmTransactionTimeoutHeader.name
					+ "] in message\r\n" + request,
					e);
			}
		}

		// 2. get from configuration
		if (timerValue == -1) {
			SIPStackConfiguration config = SIPTransactionStack.instance().getConfiguration();
			timerValue = config.getTimerB();
		}
		return timerValue;
	}

	/**
	 * timer A fired , try to send again
	 */
	synchronized void timerAfired()
	{
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerAfired",
				"Timer A fired on transaction " + toString());
		}
		
		updateSipTimersInvocationsPMICounter();
		// When timer A fires,
		// the client transaction MUST retransmit the request by passing it to the transport layer,
		// and MUST reset the timer with a value of 2*T1
		if (getState() == STATE_CALLING) {
			m_timerAvalue *= 2;
			try {
				sendRequestToTransport(getFirstRequest());
				m_timerA = new TimerA(this, getCallId());
				addTimerTask(m_timerA, m_timerAvalue);
			}
			catch (SIPTransportException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "timerAfired", "", e);
				}
				destroyTransaction();
			}
		}
	}

	/**
	 * called when TimerB fires
	 */
	void timerBfired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerBfired",
				"Timer B fired on transaction " + toString());
		}
		updateSipTimersInvocationsPMICounter();
		if (getState() == STATE_CALLING) {
			notifyTransactionTimeoutToUA();
			destroyTransaction();
		}
	}

	/**
	 * called when TimerD fires
	 */
	void timerDfired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerDfired",
				"Timer D fired on transaction " + toString());
		}
		updateSipTimersInvocationsPMICounter();
		if (getState() == STATE_COMPLETED) {
			notifyTransactionTimeoutToUA();
			destroyTransaction();
		}
	}

	/**
	 *  prosses transport error
	 */	
	public synchronized void prossesTransportError()
	{
		destroyTransaction();
		notifyRequestErrorToUA( getFirstRequest() );
	}
	
		
		
		
	/**
	 *  timer A for this transaction
	 */
	static class TimerA extends TimerEvent
	{
		SIPInviteClientTransactionImpl m_transaction;
	
		TimerA(SIPInviteClientTransactionImpl transaction, String callId) {
			super(callId);
			m_transaction = transaction;
		}
		
		public void onExecute()
		{
			m_transaction.timerAfired();
		}
	}
		
			/**
			 *  timer B for this transaction
			 */
			static class TimerB extends TimerEvent
			{
				SIPInviteClientTransactionImpl m_ct;
				
				TimerB( SIPInviteClientTransactionImpl ct, String callId)
				{
					super(callId);
					m_ct = ct;
				}
				
				public void onExecute()
				{
					//timer B
					if (m_ct != null) {
						m_ct.timerBfired();
					}
				}
				
				public boolean cancel()
				{
					return super.cancel();
				}
			}	
			
			/**
			 *  timer D for this transaction
			 */
			static class TimerD extends TimerEvent
			{
				
				SIPInviteClientTransactionImpl m_ct;
				
				TimerD(SIPInviteClientTransactionImpl ct, String callId)
				{
					super(callId);
					m_ct = ct;
				}
				
				
				public void onExecute()
				{
					//timer D
					if (m_ct != null) {
						m_ct.timerDfired();
					}
				}
				
				
				public boolean cancel()
				{
					return super.cancel(); 
				}
			}

	/**
	 * timer set when this transaction is cancelled per rfc3261 9.1
	 */
	private static class CancelTimer extends TimerEvent {
		private SIPInviteClientTransactionImpl m_ct;
		
		CancelTimer(SIPInviteClientTransactionImpl ct, String callId) {
			super(callId);
			m_ct = ct;
		}
		
		public void onExecute() {
			m_ct.cancelTimerFired();
		}
	}
	
	/**
	 * sets timer when this transaction is cancelled per rfc3261 9.1
	 * to be fired if we don't receive the 487 response
	 */
	public void setCancelTimer() {
		CancelTimer timer = new CancelTimer(this, getCallId());
		int delay = getParentStack().getConfiguration().getCancelTimer();
		addTimerTask(timer, delay);
	}
	
	/**
	 * called 32 seconds after this transaction was CANCELled
	 */
	private void cancelTimerFired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "cancelTimerFired",
				"Timer CancelTimer fired on transaction " + toString());
		}
		if (getState() == STATE_PROCEEDING) {
			notifyTransactionTimeoutToUA();
			destroyTransaction();
		}
	}
	
	/**
	 * sends an automatic ack when receiving 300-699 response
	 * according to rfc3261 17.1.1.3
	 */
	private void sendAutomaticAckRequest() {
		RequestImpl invite = (RequestImpl)getFirstRequest();
		RequestImpl ack = new RequestImpl();
		
		try {
			ack.setMethod(Request.ACK);
			ack.setCallIdHeader(invite.getCallIdHeader());
			ack.setFromHeader(invite.getFromHeader());
			ack.setRequestURI(invite.getRequestURI());
			ack.setToHeader(m_lastResponse.getToHeader());
			ack.setHeader(invite.getHeader(ViaHeader.name, true), true);
			
			CSeqHeader cseqHeader = SipJainFactories.getInstance().getHeaderFactory().createCSeqHeader(
				invite.getCSeqHeader().getSequenceNumber(),
				Request.ACK);
			ack.setCSeqHeader(cseqHeader);
			
			HeaderIterator routes = invite.getRouteHeaders();
			if (routes != null) {
				while (routes.hasNext()) {
					Header route = routes.next();
					ack.addHeader(route, false);
				}
			}
			MaxForwardsHeader maxForwardsHeader = invite.getMaxForwardsHeader();
			if (maxForwardsHeader != null) {
				ack.setMaxForwardsHeader(maxForwardsHeader);
			}
			
			if (_ibmDestinationHeader != null) {
				ack.addHeader(_ibmDestinationHeader, true);
			}
			
			if (_ibmPO != null && outboundEnable) {
				ack.addHeader(_ibmPO, true);
			}			
			
			// if the original INVITE was sent using loopback, need to send
			// the ACK on loopback too
			if (invite.isLoopback()) {
				ack.setLoopback(true);
			}
			try {
				sendRequestToTransport(ack);
			}
			catch (SIPTransportException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "sendAutomaticAckRequest", e.getMessage(), e);
				}
			}
		}
		catch (IllegalArgumentException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "sendAutomaticAckRequest", e.getMessage(), e);
			}
		}
		catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "sendAutomaticAckRequest", e.getMessage(), e);
			}
		}
	}
	 
	/** destroy the transaction */
	public synchronized void destroyTransaction() 
	{
		setState( STATE_TERMINATED );
		
		//destro all timers
		if( m_timerA!=null)
		{
			m_timerA.cancel();
		}
		if( m_timerB!=null)
		{
			m_timerB.cancel();
		}
		if( m_timerD!=null)
		{
			m_timerD.cancel();
		}
		
		startAPITimer();		
	}
	
	/**
	 * called when this transaction is no longer in the "calling" state 
	 */
	private void notCalling() {
		// timer A and timer B are only relevant in the "calling" state
		if (m_timerA != null) {
			m_timerA.cancel();
		}
		if (m_timerB != null) {
			m_timerB.cancel();
		}
	}

	/** return the most recent response */
	public Response getMostRecentResponse()
	{
		return m_lastResponse;
	}			
	
	/** type of transaction */
	protected  String getType()
	{	
		return "Client INVITE";
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
		case STATE_CALLING:
			return "Calling";
		default:
			return super.getStateAsString();
		}
	}
}
