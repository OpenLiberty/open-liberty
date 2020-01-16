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

import jain.protocol.ip.sip.SipEvent;
import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.SipJainFactories;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.context.MessageContextFactory;
import com.ibm.ws.sip.stack.dispatch.TimerEvent;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transactions.BranchMethodKey;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionImpl;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionsModel;
import com.ibm.ws.sip.stack.transaction.transport.Hop;
import com.ibm.ws.sip.stack.transaction.transport.SIPTransportException;
import com.ibm.ws.sip.stack.util.SipStackUtil;

public abstract class SIPClientTransactionImpl
	extends SIPTransactionImpl
	implements SIPClientTranaction
{
	/** class Logger */
	private static final LogMgr s_logger = Log.get(SIPClientTransactionImpl.class);

	/**
	 * the client transaction final response
	 */
	private Response m_finalResponse;
	
	/**
	 * the hop for sending retransmissions.
	 * this ensures all retransmissions are sent to the same hop
	 */
	private Hop m_hop;
	
	/**
	 * this timer is only to support the API
	 * since the has a function sendACK(long transactionId) , hold this transaction a little
	 * just in case the user will want to use this API
	 */
	private TimerAPI  m_timerAPI;


	public SIPClientTransactionImpl(SIPTransactionStack transactionStack,
		SipProvider provider,
		Request req, BranchMethodKey key, long transactionId) 
	{
		super(transactionStack, provider, req, key, transactionId);
		m_hop = null;
	}
	
	
	public void sendRequestToTransport( Request req )
		throws SIPTransportException
	{
		SipProvider provider = getProviderContext();
		MessageContext messageContext = MessageContextFactory.instance().getMessageContext(req,this);
		try{
			getParentStack().getTransportCommLayerMgr().sendMessage(messageContext, provider, getTransportConnection(), this);
		}catch(Exception e){
			messageContext.writeError(e);
		}
	}
	
	
	public void sendResponseToUA( Response sipResponse )
	{
		SipEvent event = new SipEvent(getProviderContext(), getId(), sipResponse );
		getParentStack().getUACommLayer().sendEventToUA( event );		
	}


	public void notifyRequestErrorToUA(Request sipRequest)
	{
		try
		{
			Response transportErorResponse = 
				SipJainFactories.getInstance().getMesssageFactory().createResponse( 
						Response.SERVICE_UNAVAILABLE , sipRequest );
			
			//if this is an initial request (does not have a to tag) we need to add the to tag
			//according to RFC3261 12.1.1
			SipStackUtil.addToTag(transportErorResponse);
			
			SipEvent event = new SipEvent(getProviderContext() , getId(), transportErorResponse );
			event.setEventId(SipEvent.ERROR_RESPONSE_CREATED_INTERNALLY);
			getParentStack().getUACommLayer().sendEventToUA( event );
		}
		catch( SipException exp )
		{
			getLoger().traceDebug( this,"notifyRequestErrorToUA", exp.getMessage());
		}
	}
	
	
	public void notifyTransactionTimeoutToUA()
	{
		SipEvent event = new SipEvent( getProviderContext() , getId(), false);
		getParentStack().getUACommLayer().sendEventToUA( event );
	}
	
	
	/**
	 * remove the transaction
	 */
	public final void remove()
	{
		SIPTransactionsModel.instance().remove( this );	
	}
	
	
	public final void startAPITimer()
	{
		//remove transaction
		m_timerAPI = new TimerAPI(this, getCallId());
		//just wait for 32 seconds before we throw this transaction
		addTimerTask( m_timerAPI , 32*1000);					
	}
	
	/**
	 *  timer API for this transaction
	 */
	static class TimerAPI extends TimerEvent
	{
		SIPClientTransactionImpl m_ct;
				
		TimerAPI(SIPClientTransactionImpl ct, String callId)
		{
			super(callId);
			m_ct = ct;
		}
				
		public void onExecute()
		{
			if( m_ct!=null )
			{		
				m_ct.timerApiFired();
			}
		}
				
		public boolean cancel()
		{
			return super.cancel(); 
		}		
	}
	
	/**
	 * called when TimerAPI fires
	 */
	void timerApiFired() {
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "timerApiFired",
				"Timer ApiTimer fired on transaction " + toString());
		}
		remove();
	}
		
	/**
	 * @return - the final response
	 */
	public Response getFinalResponse()
	{
		return m_finalResponse;
	}

	/**
	 * @param response - final response received
	 */
	public void setFinalResponse(Response response)
	{
		m_finalResponse = response;
	}

	/** @return the hop for sending retransmissions */
	public Hop getHop() {
		return m_hop;
	}
	
	/** @param hop the hop for sending retransmissions */
	public void setHop(Hop hop) {
		m_hop = hop;
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
		case STATE_TERMINATED:
			return "Terminated";
		default:
			return super.getStateAsString();
		}
	}
}
