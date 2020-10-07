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
package com.ibm.ws.sip.stack.transaction.transactions.st;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.dispatch.TimerEvent;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transactions.BranchMethodKey;
import com.ibm.ws.sip.stack.transaction.transactions.MergedRequestKey;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionHelper;
import com.ibm.ws.sip.stack.transaction.transport.SIPTransportException;

/**
 * 
 * 
 * 
 * 
 * 
 * 
 *                                Request received
                                  |pass to TU
                                  V
                            +-----------+
                            |           |
                            | Trying    |-------------+
                            |           |             |
                            +-----------+             |200-699 from TU
                                  |                   |send response
                                  |1xx from TU        |
                                  |send response      |
                                  |                   |
               Request            V      1xx from TU  |
               send response+-----------+send response|
                   +--------|           |--------+    |
                   |        | Proceeding|        |    |
                   +------->|           |<-------+    |
            +<--------------|           |             |
            |Trnsprt Err    +-----------+             |
            |Inform TU            |                   |
            |                     |                   |
            |                     |200-699 from TU    |
            |                     |send response      |
            |  Request            V                   |
            |  send response+-----------+             |
            |      +--------|           |             |
            |      |        | Completed |<------------+
            |      +------->|           |
            +<--------------|           |
            |Trnsprt Err    +-----------+
            |Inform TU            |
            |                     |Timer J fires
            |                     |-
            |                     |
            |                     V
            |               +-----------+
            |               |           |
            +-------------->| Terminated|
                            |           |
                            +-----------+
 * 
 */
public class SIPNonInviteServerTransactionImpl
	extends SIPServerTransactionImpl
{
	//transaction states
	public static final int STATE_TRYING 		= 0;
	public static final int STATE_PROCEEDING 	= 1;
	public static final int STATE_COMPLETED 	= 2;
	public static final int STATE_TERMINATED 	= 3;
	
		
	// we should keep the most recent provional response
	// so we could retransmit it
	private Response m_mostRecentProvisionalResponse;
	
	//the final response received
	private Response m_finalResponse;
	
	//Timer J should start in entering the completed 
	//state , for unreliable transport
	private TimerJ  m_timerJ; 
	
	//Api Timer will start when receiving the request
	private ApiTimer m_apiTimer;
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger =
		Log.get(SIPNonInviteServerTransactionImpl.class);
		
	public SIPNonInviteServerTransactionImpl(
		SIPTransactionStack transactionStack,
		SipProvider provider,
		Request req,
		BranchMethodKey key,
		MergedRequestKey mergedRequestKey)
	{
		super(transactionStack, provider, req, key, mergedRequestKey);
	}
				
	/**
	 * prosses the request in a state machine as stated in RFC 17.2.2
	 */
	public synchronized void processRequest(Request sipRequest)
		throws SipParseException
	{		
		try
		{
			switch(getState())
			{
				case STATE_BEFORE_STATE_MACHINE_PROCESSE:
					//this is the first entry point to the state machine				
					setState(STATE_TRYING);

					//Api timer
					long delay = getParentStack().getConfiguration().getNonInviteServerTransactionTimer();
					m_apiTimer = new ApiTimer(this, getCallId());
					addTimerTask( m_apiTimer , delay );
					
					//notify application
					sendRequestToUA(sipRequest);
					break;
				
				//all of these cases are retransmitions				
				case STATE_TRYING:
					//if in trying state  , and I get a retransmission , discard				
					break;
				case STATE_PROCEEDING:
					sendResponseToTransport(m_mostRecentProvisionalResponse);	
					break;
				case STATE_COMPLETED:
					sendResponseToTransport(m_finalResponse);
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
			
		try
		{	
			switch(getState())
			{
				case STATE_TRYING:
					 if(SIPTransactionHelper.isProvionalResponse( sipResponse.getStatusCode() ))
					 {
					 	setState( STATE_PROCEEDING );
					 	m_mostRecentProvisionalResponse = sipResponse;
					 	sendResponseToTransport(sipResponse);
					 }
					 else if (SIPTransactionHelper.isFinalResponse( sipResponse.getStatusCode() ))
					 {
						setStateCompleted(sipResponse);
						sendResponseToTransport(sipResponse);
					 }
					 
					
					//if in trying state  , and I get a retransmission , discard				
					break;
				case STATE_PROCEEDING:
					if(SIPTransactionHelper.isProvionalResponse( sipResponse.getStatusCode() ))
					{
						sendResponseToTransport(sipResponse);
					}
					else if(SIPTransactionHelper.isFinalResponse( sipResponse.getStatusCode() ))
					{
						setStateCompleted(sipResponse);
						sendResponseToTransport(sipResponse);
					}								
					break;
				case STATE_COMPLETED:
					 //we should discard any response we
					 //get in the completed state
					 break; 	
	
			}
		}
		catch( SIPTransportException exp )
		{
			prossesTransportError();								 					  	
		}
	}
	
	
	private void setStateCompleted( Response sipResponse )
	{
		//create timer task and add it
		m_finalResponse = sipResponse;				
		setState( STATE_COMPLETED );

		//cancel Api Timer
		m_apiTimer.cancel();
		
		if( !isTransportReliable()  )
		{
			long delay = getParentStack().getConfiguration().getTimerJ();
			m_timerJ = new TimerJ(this, getCallId());
			addTimerTask( m_timerJ , delay );						
		}
		else
		{
			destroyTransaction();
		}			
	}
	
	
	/**
	*  prosses transport error
	*/	
	public synchronized void prossesTransportError()
	{
		//move the dialog to terminated state and notify the UA
		Response lastResponcse =null;
		if( m_finalResponse!=null )
		{
			lastResponcse = m_finalResponse;
		}
		else if( m_mostRecentProvisionalResponse!=null )
		{
			lastResponcse = m_mostRecentProvisionalResponse;
		}		
		destroyTransaction();
		notifyRespnseErrorToUA(lastResponcse);				
	}
	
	
	public synchronized Response getMostRecentResponse()
	{
		Response retVal =null;
		if( m_finalResponse!=null )
		{
			retVal = m_finalResponse;
		}
		else if( m_mostRecentProvisionalResponse!=null )
		{
			retVal = m_mostRecentProvisionalResponse;
		}
		return retVal;	
	}
						
	/** destroy the transaction */
	public synchronized void destroyTransaction()	
	{
		setState( STATE_TERMINATED );
		
		//destroy all timers
		if( m_timerJ!=null)
		{
			m_timerJ.cancel();
		}
		
		m_apiTimer.cancel();
		
		//remove the transaction
		remove();
	}
		
			
	/**
	 *  timer J for this transaction
	 */
	static class TimerJ extends TimerEvent
	{
		
		SIPNonInviteServerTransactionImpl m_st;
		
		TimerJ(SIPNonInviteServerTransactionImpl st, String callId)
		{
			super(callId);
			m_st = st;
		}
		
		public void onExecute()
		{
			m_st.timerJfired();
		}
		
		public boolean cancel()
		{
			return super.cancel();
		}
	}

	/**
	 * called when TimerJ fires
	 */
	void timerJfired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerJfired",
				"Timer J fired on transaction " + toString());
		}
		updateSipTimersInvocationsPMICounter();
		if (getState() != STATE_COMPLETED) {
			notifyTimeOutToUA();
		}
		destroyTransaction();			
	}

	/**
	 *  ApiTimer for this transaction
	 *  there is a problem in the transaction state machine - if I don't get any response for
	 *  the Request , the transaction will never complete , causing a memory leek.
	 *  this timer ensures that the transaction will clean up in such a case.
	 *  the duration will be just a little more than 32 seconds , that is the default for Timer F - 
	 *  the default time out for the client side of this transaction.   
	 */
	static class ApiTimer extends TimerEvent
	{
		
		SIPNonInviteServerTransactionImpl m_st;
		
		ApiTimer(SIPNonInviteServerTransactionImpl st, String callId)
		{
			super(callId);
			m_st = st;
		}
		
		public void onExecute()
		{
			m_st.timerApifired();
		}
		
		public boolean cancel()
		{
			return super.cancel();
		}
	}

	/**
	 * called when ApiTimer fires
	 */
	void timerApifired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerApifired",
				"Timer API fired on transaction " + toString());
		}
	    if (getState() != STATE_COMPLETED) {
	        notifyTimeOutToUA();				
		}
		destroyTransaction();			
	}

	/**
	 * called when a request with no branch arrives from the network
	 * @param req the incoming request
	 * @param oldVia top via header in initial request
	 * @param newVia top via header in new request
	 * @return true if all request-matching rules pass, otherwise false
	 */
	protected boolean is2543RequestPartOfTransaction(Request req, ViaHeader oldVia, ViaHeader newVia) {
		Request initial = getFirstRequest();
		String oldToTag = initial.getToHeader().getTag();
		if (oldToTag == null) {
			oldToTag = "";
		}
		String oldFromTag = initial.getFromHeader().getTag();
		if (oldFromTag == null) {
			oldFromTag = "";
		}
		CallIdHeader oldCallId = initial.getCallIdHeader();
		CSeqHeader oldCSeq = initial.getCSeqHeader();
		URI oldRequestURI, newRequestURI;
		try {
			oldRequestURI = initial.getRequestURI();
			newRequestURI = req.getRequestURI();
		}
		catch (SipParseException e) {
			return false;
		}
		String newToTag = req.getToHeader().getTag();
		if (newToTag == null) {
			newToTag = "";
		}
		String newFromTag = req.getFromHeader().getTag();
		if (newFromTag == null) {
			newFromTag = "";
		}
		CallIdHeader newCallId = req.getCallIdHeader();
		CSeqHeader newCSeq = req.getCSeqHeader();
		String newMethod;
		try {
			newMethod = req.getMethod();
		}
		catch (SipParseException e) {
			return false;
		}
		boolean match;
		
		if (newMethod.equals(Request.INVITE)) {
			// The INVITE request matches a transaction if the Request-URI, To tag,
			// From tag, Call-ID, CSeq, and top Via header field match those of the
			// INVITE request which created the transaction
			
			match = false; // got INVITE for a non-INVITE transaction
		}
		else if (newMethod.equals(Request.ACK)) {
			// The ACK request matches a transaction if the Request-
			// URI, From tag, Call-ID, CSeq number (not the method), and top Via
			// header field match those of the INVITE request which created the
			// transaction, and the To tag of the ACK matches the To tag of the
			// response sent by the server transaction
			
			match = false; // got ACK for a non-INVITE transaction
		}
		else {
			// For all other request methods, a request is matched to a transaction
			// if the Request-URI, To tag, From tag, Call-ID, CSeq (including the
			// method), and top Via header field match those of the request that
			// created the transaction			
			match = newRequestURI.equals(oldRequestURI)
				&& newToTag.equals(oldToTag)
				&& newFromTag.equals(oldFromTag)
				&& newCallId.equals(oldCallId)
				&& newCSeq.equals(oldCSeq)
				&& newVia.equals(oldVia);
		}
		return match;
	}
	
	/**
	 * log Mgr for class
	 */
	public LogMgr getLoger()
	{
		return c_logger;
	}
	
	/** type of transaction */
	protected  String getType()
	{	
		return "Server non-INVITE";
	}	

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionImpl#getStateAsString()
	 */
	public String getStateAsString() {
		switch (getState()) {
		case STATE_TRYING:
			return "Trying";
		case STATE_PROCEEDING:
			return "Proceeding";
		case STATE_COMPLETED:
			return "Completed";
		case STATE_TERMINATED:
			return "Terminated";
		default:
			return super.getStateAsString();
		}
	}
	

}
