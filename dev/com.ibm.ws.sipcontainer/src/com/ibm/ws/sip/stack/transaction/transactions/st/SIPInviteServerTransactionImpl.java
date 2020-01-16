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
import jain.protocol.ip.sip.header.TimeStampHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.SipJainFactories;
import com.ibm.ws.sip.stack.dispatch.TimerEvent;
import com.ibm.ws.sip.stack.transaction.SIPStackConfiguration;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transactions.BranchMethodKey;
import com.ibm.ws.sip.stack.transaction.transactions.MergedRequestKey;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionHelper;
import com.ibm.ws.sip.stack.transaction.transport.SIPTransportException;

/**
|INVITE
							   |pass INV to TU
			INVITE             V send 100 if TU won't in 200ms
			send response+-----------+
				+--------|           |--------+101-199 from TU
				|        | Proceeding|        |send response
				+------->|           |<-------+
						 |           |          Transport Err.
						 |           |          Inform TU
						 |           |--------------->+
						 +-----------+                |
			300-699 from TU |     |2xx from TU        |
			send response   |     |send response      |
							|     +------------------>+
							|                         |
			INVITE          V          Timer G fires  |
			send response+-----------+ send response  |
				+--------|           |--------+       |
				|        | Completed |        |       |
				+------->|           |<-------+       |
						 +-----------+                |
							|     |                   |
						ACK |     |                   |
						-   |     +------------------>+
							|        Timer H fires    |
							V        or Transport Err.|
						 +-----------+  Inform TU     |
						 |           |                |
						 | Confirmed |                |
						 |           |                |
						 +-----------+                |
							   |                      |
							   |Timer I fires         |
							   |-                     |
							   |                      |
							   V                      |
						 +-----------+                |
						 |           |                |
						 | Terminated|<---------------+
						 |           |
						 +-----------+
*/
public class SIPInviteServerTransactionImpl
	extends SIPServerTransactionImpl
{

	//transaction states
	public static final int STATE_PROCEEDING  	= 0;
	public static final int STATE_COMPLETED 	= 1;
	public static final int STATE_CONFIRMED 	= 2;
	public static final int STATE_TERMINATED 	= 3;	
	
	//timers
	TimerH m_timerH;
	TimerI m_timerI;
	TimerG m_timerG;
	
	
	//prvsional and final responses
	private Response m_mostRecentProvisionalResponse; 
	private Response m_finalResponse;
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger =
		Log.get(SIPInviteServerTransactionImpl.class);
					
	public SIPInviteServerTransactionImpl(
		SIPTransactionStack transactionStack,
		SipProvider provider,
		Request req,
		BranchMethodKey key,
		MergedRequestKey mergedRequestKey) 
	{
		super(transactionStack, provider, req, key, mergedRequestKey);
	}
	
	/**
	 * prosses the request in a state machine as stated in RFC 17.2.1
	 */
	public synchronized void processRequest(Request sipRequest)
		throws SipParseException
	{				
		try
		{
			switch(getState())
			{
				case STATE_BEFORE_STATE_MACHINE_PROCESSE:
					setState(STATE_PROCEEDING);
					//amirk - sometimes we don't want this automatic response
					if( getParentStack().getConfiguration().isAuto100OnInvite())
					{
					    m_mostRecentProvisionalResponse = createTryingResponse( sipRequest );
					    //send the trying back to the user
					    sendResponseToTransport( m_mostRecentProvisionalResponse );
					}
					
					//update UA with message
					sendRequestToUA( sipRequest );
					
					break;
	
				//from the state machine , this could only be
				//a retransmition of the INVITE , not the ACK ?
				case STATE_PROCEEDING:
					if (m_mostRecentProvisionalResponse != null) {
						sendResponseToTransport( m_mostRecentProvisionalResponse );
					}
					break;
				
				case STATE_COMPLETED:
					if( Request.INVITE.equals( sipRequest.getCSeqHeader().getMethod() ) )
					{
						//don't forward to application - this is a retransmition
						
						// 3261 - 17.2.1
						// while in the "Completed" state, if a request retransmission is
						// received, the server SHOULD pass the response to the transport for
						// retransmission.
						if (m_finalResponse != null) {
							sendResponseToTransport(m_finalResponse);
						}
					}
					else if( Request.ACK.equals( sipRequest.getCSeqHeader().getMethod() ))
					{
						if( !isTransportReliable() )
						{
							long delay = getParentStack().getConfiguration().getTimerI();
							m_timerI = new TimerI(this, getCallId());
							addTimerTask( m_timerI , delay );
							setState( STATE_CONFIRMED);							
						}
						else
						{
							//since this is reliable transport  , go strait to terminated
							destroyTransaction();
						}
						// do not pass the ACK up to the TU
					}															
					break;
	
				case STATE_CONFIRMED:
				    if(c_logger.isTraceDebugEnabled())
				    {
				        c_logger.traceDebug("SIP INVITE Transaction Error - Got ACK in CONFIRMED state");
				    }
				   break;
					
				case STATE_TERMINATED:
					//Amir, May 29: We keep the transaction x seconds after it has 
				    //terminiated for the case where the UAC did not get the final
				    //response and try to retransmit. If we don't keep the transaction 
				    //a little bit longer, the stack will regard this as new transaction
				    //instead of a retransmit. 
				    if(null != m_finalResponse)
				    {
				        if(Request.INVITE.equals(sipRequest.getMethod()))
				        {
				            sendResponseToTransport(m_finalResponse);
				        }
				        else if(Request.ACK.equals(sipRequest.getMethod()))
				        {
				            //ACK requests for 2xx are always passed up with 
				            //transaction -1. In the past before we applied the
				            //fix above the transaction would have been destoryed
				            //so when the ACK was received a transaction was not 
				            //found and the SipTransactionStack (line 408) was 
				            //handling this behavior.
				            sendRequestToUA(sipRequest, -1);
				        }
				    }
				    
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
				case STATE_PROCEEDING:
					if( SIPTransactionHelper.isProvionalResponse(sipResponse.getStatusCode()))
					{
						m_mostRecentProvisionalResponse = sipResponse;
						sendResponseToTransport( sipResponse );						
					}
					else if( SIPTransactionHelper.isOKFinalResponse( sipResponse.getStatusCode()))
					{
						m_finalResponse = sipResponse;
						destroyTransaction();
						sendResponseToTransport( sipResponse );						
					}
					else if( SIPTransactionHelper.isNonOkFinalResponse(sipResponse.getStatusCode()))
					{
						m_finalResponse = sipResponse;
						setCompletedState();
						sendResponseToTransport(sipResponse);						
					}
					break;
				
				case STATE_COMPLETED:
					//no responses in completed state								
					break;
					
				case STATE_CONFIRMED:								
					//no responses in completed state								
					break;					
			}
		}
		catch( SIPTransportException exp )
		{
			prossesTransportError();
		}									 					  
	}
		
	/**
	 *  prosses transport error
	 */	
	public synchronized void prossesTransportError()
	{		
		notifyRespnseErrorToUA(getMostRecentResponse());				
		destroyTransaction();
	}
	
	private void setCompletedState()
	{
		SIPStackConfiguration config = getParentStack().getConfiguration();

		if( !isTransportReliable())
		{
			long delay = config.getTimerG();
			m_timerG = new TimerG(1, this, getCallId());
			addTimerTask( m_timerG , delay );
		}

			long delay = config.getTimerH();
			m_timerH = new TimerH(this, getCallId());
			addTimerTask( m_timerH , delay );
			setState(STATE_COMPLETED);			
	}
	
	synchronized void timerGfired( long interval )
	{
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerGfired",
				"Timer G fired on transaction " + toString());
		}
		updateSipTimersInvocationsPMICounter();
		if( getState()==STATE_COMPLETED)
		{
			try
			{

				double aDelay = Math.min(Math.pow(2,interval-1) *SIPTransactionConstants.T1,SIPTransactionConstants.T2);
				if( c_logger.isTraceDebugEnabled())
				{
					StringBuffer buf = new StringBuffer(256);
					buf.append("Resending message. Retransmission [");
					buf.append(interval);
					buf.append("]. Next resend in [");
					buf.append(aDelay);
					buf.append("] milliseconds");
					c_logger.traceDebug(this, "timerGfired", buf.toString());
				}
				sendResponseToTransport( getMostRecentResponse());
				m_timerG = new TimerG(interval+1 , this, getCallId());
				addTimerTask(m_timerG,(long)aDelay);							
			}
			catch (SIPTransportException e)
			{
				if( c_logger.isTraceDebugEnabled())
				{
				c_logger.traceDebug(this,"timerGfired",e.getMessage());
				}
			}
		}
		//else
		//if not in the completed state , ignore
	}
			
	
	/**
	 *  timer G for this transaction
	 *  only for reliable trasport
	 */
	static class TimerG extends TimerEvent
	{
		
		private long m_interval;
		SIPInviteServerTransactionImpl m_st;
		
		TimerG(long interval, SIPInviteServerTransactionImpl st, String callId)
		{
			super(callId);
			m_interval = interval;
			m_st = st;
		}
		
		public boolean cancel()
		{
			return super.cancel();
		}		

		public void onExecute()
		{
			m_st.timerGfired( m_interval );
		}
		
	}
	
	
	/**
	 *  timer F for this transaction
	 */
	static class TimerH extends TimerEvent
	{
		
		SIPInviteServerTransactionImpl m_st;
		
		TimerH(SIPInviteServerTransactionImpl st, String callId)
		{
			super(callId);
			m_st = st;
		}
		
		public boolean cancel()
		{
			return super.cancel();
		}		

		public void onExecute()
		{
			m_st.timerHfired();
		}				
	}

	/**
	 * called when TimerH fires
	 */
	void timerHfired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerHfired",
				"Timer H fired on transaction " + toString());
		}
		updateSipTimersInvocationsPMICounter();
		if (getState() == STATE_COMPLETED) {
			notifyTimeOutToUA();
			destroyTransaction();		
		}			
	}

	/**
	 *  timer I for this transaction
	 */
	static class TimerI extends TimerEvent
	{		
		
		SIPInviteServerTransactionImpl m_st;
		
		TimerI(SIPInviteServerTransactionImpl st, String callId)
		{
			super(callId);
			m_st = st;
		}
		
		public boolean cancel()
		{
			return super.cancel();
		}		
		
		public void onExecute()
		{
			m_st.timerIfired();
		}
				
	}

	/**
	 * called when TimerI fires
	 */
	void timerIfired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerIfired",
				"Timer I fired on transaction " + toString());
		}
		updateSipTimersInvocationsPMICounter();
		if (getState() == STATE_CONFIRMED) {
			notifyTimeOutToUA();
			destroyTransaction();											
		}
	}

	/**
	 * Timer for cleaning up the transaction from the transaction table. For 
	 * more information see destroyTransaction() method. 
	 */
	static class CleanupTimer extends TimerEvent
	{
	    SIPInviteServerTransactionImpl m_st;
		
	    CleanupTimer(SIPInviteServerTransactionImpl st, String callId)
		{
	    	super(callId);
			m_st = st;
		}
		
		public boolean cancel()
		{
			return super.cancel();
		}		
		
		public void onExecute()
		{
		    m_st.timerCleanupTimerFired();
		}
	}
	
	/**
	 * called when CleanupTimer fires
	 */
	void timerCleanupTimerFired() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "timerCleanupTimerFired",
				"Timer CleanupTimer fired on transaction " + toString());
		}
	    remove();
	}

	public Response getMostRecentResponse()
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
	
	private Response createTryingResponse( Request req ) 
		throws SipParseException
	{
		// rfc 3261-8.2.6.1
		Response response = SipJainFactories.getInstance().getMesssageFactory().createResponse( SIPTransactionConstants.RETCODE_INFO_TRYING , req );
		TimeStampHeader timeStamp = req.getTimeStampHeader();
		if (timeStamp != null) {
			response.setTimeStampHeader(timeStamp);
		}
		return response;
	}
	
	
	/** destroy transaction */
	public void destroyTransaction()
	{
		if(getState() == STATE_TERMINATED)
		{
		    return;
		}
		
	    setState( STATE_TERMINATED );
		
		//cancel all timers
		if( m_timerG!=null)
		{
			m_timerG.cancel();
		}
		if( m_timerH!=null)
		{
			m_timerH.cancel();
		}
		if( m_timerI!=null )
		{
			m_timerI.cancel();
		}
		
		//Amir, May 29: 
		//Determine if the transaction has ended with a 2xx response. Which means
		//we need to keep the terminated transaction a little bit longer as we
		//see the following scenario:
		//1. We (UAS) receive the INVITE and send back final response. 
		//2. The UAC does not get the 2xx and sends a retransmission. If we don't
		//keep the terminated transaction we will regard this retransmission as
		//a new request and pass it up to the as a new INVITE. 
		boolean is2xxUnreliableResponse = false;
		try {
            if(!isTransportReliable() && m_finalResponse != null && 
               SIPTransactionHelper.isOKFinalResponse(m_finalResponse.getStatusCode()))
            {
                is2xxUnreliableResponse = true;
            }
        }
        catch (SipParseException e) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "destroyTransaction", e.getMessage());
            }
        }

		if(!is2xxUnreliableResponse)
		{
		    //remove from transactions table
		    remove();
		}
		else
		{
		    //Keep the transaction around for another 32 seconds and then
		    //cleanup. 
			int delay = getParentStack().getConfiguration().getInviteServerTransactionTimer();
		    addTimerTask(new CleanupTimer(this, getCallId()), delay);
		}

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
			String oldToTag = initial.getToHeader().getTag();
			if (oldToTag == null) {
				oldToTag = "";
			}
			match = newRequestURI.equals(oldRequestURI)
				&& newToTag.equals(oldToTag)
				&& newFromTag.equals(oldFromTag)
				&& newCallId.equals(oldCallId)
				&& newCSeq.equals(oldCSeq)
				&& newVia.equals(oldVia);
		}
		else if (newMethod.equals(Request.ACK)) {
			// The ACK request matches a transaction if the Request-
			// URI, From tag, Call-ID, CSeq number (not the method), and top Via
			// header field match those of the INVITE request which created the
			// transaction, and the To tag of the ACK matches the To tag of the
			// response sent by the server transaction
			String oldToTag = m_finalResponse == null
				? null // got ACK without sending a final response
				: m_finalResponse.getToHeader().getTag();
			if (oldToTag == null) {
				oldToTag = "";
			}
			match = newRequestURI.equals(oldRequestURI)
				&& newFromTag.equals(oldFromTag)
				&& newCallId.equals(oldCallId)
				&& newCSeq.getSequenceNumber() == oldCSeq.getSequenceNumber()
				&& newVia.equals(oldVia)
				&& newToTag.equals(oldToTag);
		}
		else {
			// For all other request methods, a request is matched to a transaction
			// if the Request-URI, To tag, From tag, Call-ID, CSeq (including the
			// method), and top Via header field match those of the request that
			// created the transaction			
			match = false; // got non-INVITE/non-ACK on an INVITE transaction
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
		return "Server INVITE";
	}	

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionImpl#getStateAsString()
	 */
	public String getStateAsString() {
		switch (getState()) {
		case STATE_PROCEEDING:
			return "Proceeding";
		case STATE_COMPLETED:
			return "Completed";
		case STATE_CONFIRMED:
			return "Confirmed";
		case STATE_TERMINATED:
			return "Terminated";
		default:
			return super.getStateAsString();
		}
	}
	

}
