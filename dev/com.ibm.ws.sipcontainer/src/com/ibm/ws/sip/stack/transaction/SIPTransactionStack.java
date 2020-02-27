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
package com.ibm.ws.sip.stack.transaction;

import jain.protocol.ip.sip.SipEvent;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.TransactionDoesNotExistException;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;
import java.util.Timer;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.SipJainFactories;
import com.ibm.ws.jain.protocol.ip.sip.SipProviderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.CancelRequest;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.ResponseImpl;
import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.context.MessageContextFactory;
import com.ibm.ws.sip.stack.transaction.transactions.BranchMethodKey;
import com.ibm.ws.sip.stack.transaction.transactions.MergedRequestKey;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransaction;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionsModel;
import com.ibm.ws.sip.stack.transaction.transactions.ct.SIPClientTranaction;
import com.ibm.ws.sip.stack.transaction.transactions.ct.SIPInviteClientTransactionImpl;
import com.ibm.ws.sip.stack.transaction.transactions.ct.SIPNonInviteClientTransactionImpl;
import com.ibm.ws.sip.stack.transaction.transactions.st.SIPInviteServerTransactionImpl;
import com.ibm.ws.sip.stack.transaction.transactions.st.SIPNonInviteServerTransactionImpl;
import com.ibm.ws.sip.stack.transaction.transactions.st.SIPServerTransaction;
import com.ibm.ws.sip.stack.transaction.transport.SIPTransportException;
import com.ibm.ws.sip.stack.transaction.transport.TransportCommLayerMgr;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.util.ThreadPool;

/**
 * @author Amirk
 *  this class acts as a conroller between layers
 *  it receives events - requests , reponses or timerEvents
 *  and prossecs them to the different layers 
 */
public class SIPTransactionStack
{					
	

	/** 
	 * holds transactions 
	 **/
	private SIPTransactionsModel        	 m_transactionsModel;
	

	/** 
	 * Transport communication layer object 
	 **/	
	private TransportCommLayerMgr            m_transportCommLayerMgr;
	
	/** 
	 * ua communication layer object 
	 **/
	private UACommLayer                      m_uaCommLayer;
	
	/** 
	 * the configuration object for this stack 
	 **/
	private SIPStackConfiguration		      m_stackConfiguration;
		
	/**
	 *  timer for transactions
	 */
	private Timer m_transactionsTimer;
					
	/** 
	 * is the stack running 
	 **/
	private boolean                        	 m_isRunning;
			
	/**
 	* Class Logger. 
 	*/
	private static final LogMgr c_logger = Log.get(SIPTransactionStack.class);
	
	/**
	 * singleton instance
	 */
	private static volatile SIPTransactionStack s_instance;

	/**
	 * @return singleton instance
	 */
	public static SIPTransactionStack instance() {
		SIPTransactionStack result = s_instance;
		if(result == null){
			synchronized (SIPTransactionStack.class) {
				result = s_instance;
				if(result == null){
					s_instance = result = new SIPTransactionStack();
				}
			}
		}
		return result;
	}
	
	/** 
	 * private constructor 
	 * @throws IOException
	 **/
	private SIPTransactionStack()
	{
		init();
	}
	
	/** init the stack 
	 * @throws IOException*/
	private void init()
	{										
		//load configuration parameters
		m_stackConfiguration = new SIPStackConfiguration();
		
		if( c_logger.isInfoEnabled())
		{
		c_logger.info("info.com.ibm.ws.sip.stack.transaction.SIPTransactionStack.init.1",
        				Situation.SITUATION_START,
        				null);
		}
		
		//create the stacks model
		m_transactionsModel = SIPTransactionsModel.instance();
		
		
		//create transport layer
		//and add the stack to listen to the transport layer on the specific listening point				
		m_transportCommLayerMgr = TransportCommLayerMgr.instance();						
		 										

		if( c_logger.isInfoEnabled())
		{
		c_logger.info("info.com.ibm.ws.sip.stack.transaction.SIPTransactionStack.init.2",
						Situation.SITUATION_START,
						null);
		}



		//the layer to interact with the ua
		m_uaCommLayer =  new UACommLayer();
		
		//create a timer for threads timeout 
		m_transactionsTimer = new Timer(true);
				
		m_isRunning = true;
		if( c_logger.isInfoEnabled())
		{
		c_logger.info( "info.com.ibm.ws.sip.stack.transaction.SIPTransactionStack.init.3",Situation.SITUATION_START,null);
		}
	}
	
	/** 
	 * is the stack running 
	 **/
	public boolean isRunning()
	{
		return m_isRunning;
	}
		
	/**
	 * get the transaction timer
	 * @return Timer - transactions timer
	 */
	public Timer getTimer()
	{
		return m_transactionsTimer;
	}
	
		
	/**
	 * return the stack configuration object
	 * @return SIPStackConfiguration
	 */
	public SIPStackConfiguration getConfiguration()
	{
		return m_stackConfiguration;
	}
	
	
 
	/**
	 * @return SIPTransactionsModel
	 */
	public SIPTransactionsModel getTransactionsModel()
	{
		return m_transactionsModel;
	}
	
	
	/**
	 * @return UACommLayer
	 */
	public UACommLayer  getUACommLayer()
	{
		return m_uaCommLayer;
	}	
	
	/**
	 * @return TransportCommLayer
	 */
	public TransportCommLayerMgr getTransportCommLayerMgr()
	{
		return m_transportCommLayerMgr; 
	}
	
	/**
	 *  send the request to the Transport Layer
	 *  after prossesing it
	 */
	public void sendRequestToTransportCore(MessageContext messageContext, SipProvider provider, SIPConnection connection)
		throws SIPTransportException
	{							 		
		m_transportCommLayerMgr.sendMessage(messageContext, provider, connection);
	}
			
							
	/** prosses request from the UA */
	public long prossesUASipRequest(Request sipRequest, SipProvider provider, long transactionId)
			throws SipParseException
	{
		
		long retTidVal = 0;
		SIPClientTranaction  sipClientTransaction = null;
		
		//the only Exception here is an ack sended
		//in this state , there is not transaction to match to
		//and no transaction is needed to be created , since there is
		//no responce that should be arrived on this request
		if(  Request.ACK.equals( sipRequest.getMethod()) )
		{
			MessageContext messageContext= null;
			//send to the transport with no transport
			try
			{
				messageContext= MessageContextFactory.instance().getMessageContext(sipRequest);
				sendRequestToTransportCore(messageContext, provider, null);
			}
			catch( SIPTransportException exp )
			{
				//no transaction associated , egnor
				if( c_logger.isTraceDebugEnabled())
				{
				c_logger.traceDebug(this,"processsUASipRequest",exp.getMessage());
				}
				if (messageContext!= null){
					messageContext.writeError(exp);
				}
			}						
		}
		else
		{
			if (sipRequest.getMethod().equals(Request.CANCEL)) {
				// set timer per rfc3261 9.1
				SIPInviteClientTransactionImpl original = m_transactionsModel.getInviteFromCancel(sipRequest);
				if (original == null) {
					// no matching INVITE found
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "processUASipRequest",
							"Cannot match INVITE transaction to CANCEL request");
					}
				}
				else {
					original.setCancelTimer();
				}
			}
			BranchMethodKey key = m_transactionsModel.createTransactionId(sipRequest);
			sipClientTransaction = createClientTransaction(sipRequest, provider, key, transactionId);
			m_transactionsModel.putClientTransaction( sipClientTransaction );
			sipClientTransaction.processRequest(sipRequest);
			retTidVal = sipClientTransaction.getId(); 
		}	
		return retTidVal;
	}
	
	
	/**
	 *  The ACK and cancel Requests are the only requests
	 *  that should be assosiated with a transaction - since 
	 *  they are both a part of the INVITE transaction , that
	 *  is a 3 stage handshek , and not 2 stages
	 *  
	 * @param clientTransactionId - the transaction Id this message is part of
	 * @param sipAckRequest - the request
	 * @return the id of the client transaction
	 * @throws SipParseException
	 */
	public long prossesUASipACKRequest( long inviteClientTransactionId , Request sipAckRequest)
			throws SipParseException,TransactionDoesNotExistException
	{			
		SIPTransaction sipTransaction = m_transactionsModel.getClientTransaction( inviteClientTransactionId );
		sipTransaction.processRequest( sipAckRequest );
		return inviteClientTransactionId;
	}

	/** 
	 * prossec response from the UA layer 
	 **/
	public long prossesUASipResponse(Response sipResponse, long transactionId)
				throws SipParseException,TransactionDoesNotExistException
	{
		SIPTransaction sipTransaction =	m_transactionsModel.getServerTransaction( transactionId );
		sipTransaction.processResponse(sipResponse);
		return sipTransaction.getId();				
	}
		

	/**
	 * Notification on a message from the Transport
	 * @param sipMsg The Message from the Transport Layer
	 */
	
	public void prossesTransportSipMessage( Message sipMsg , SipProvider provider , SIPConnection sipTransportConnection )
		throws SipParseException
	{
		
		/*
		 *  got notification on a message from the transport
		 *  this could be either a request , so it should open a server transactio
		 *  or a response , so it should be matched to the client transaction created it
		 */
		
		BranchMethodKey key = m_transactionsModel.createTransactionId(sipMsg);
		
		if (sipMsg.isRequest()) {
			RequestImpl sipRequest = (RequestImpl) sipMsg;
			SIPServerTransaction serverTransaction = m_transactionsModel.getServerTransaction(key, sipRequest);

			if (serverTransaction == null) {
				// the request does not match a pending transaction
				boolean auto482toMergedRequests = m_stackConfiguration.isAuto482ResponseToMergedRequests();
				MergedRequestKey sourceId;
				
				if (auto482toMergedRequests) {
					// check merged request per rfc3261 8.2.2.2
					sourceId = m_transactionsModel.createMergedTransactionId(sipRequest);
					ToHeader to = sipRequest.getToHeader();
					String toTag = to.getTag();
					if (toTag == null || toTag.length() == 0) {
						if (m_transactionsModel.isMergedServerTransaction(sourceId)) {
							// this is a merged request, and the 1st one
							// that arrived is already pending.
							Response response = SipJainFactories.getInstance().getMesssageFactory().createResponse(
								Response.LOOP_DETECTED,
								sipRequest);
							MessageContext messageContext = null;
							try {
								messageContext = MessageContextFactory.instance().getMessageContext(response);
								m_transportCommLayerMgr.sendMessage(messageContext, provider, sipTransportConnection);	
								}
							catch (SIPTransportException e) {
								if (c_logger.isTraceDebugEnabled()) {
									c_logger.traceDebug(this, "processTransportSipMessage", e.getMessage());
								}
								if (messageContext!= null){
									messageContext.writeError(e);
								}
							}
							return;
						}
					}
				}
				else {
					sourceId = null;
				}
				
				//amirk - 30/08/2004
				//if the request is an ACK that is not related to a previouse transaction
				//this is an ACK for a 2XX response.
				//hence
				//don't create a transaction to send it on , just send it to the application
				if (!Request.ACK.equals( sipRequest.getMethod())) {
					serverTransaction = createServerTransaction(sipRequest, provider, key, sourceId);
					m_transactionsModel.putServerTransaction(serverTransaction);
					serverTransaction.setTransportConnection(sipTransportConnection);
				}
			}

			if (sipRequest instanceof CancelRequest) {
				CancelRequest cancelRequest = (CancelRequest)sipRequest;
				try {
					m_transactionsModel.correlateCancelToInviteTransaction(cancelRequest);
				}
				catch (TransactionDoesNotExistException e) {
					try {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "processTransportSipMessage", "Can't correlate CANCEL to INVITE transaction");
						}
						//Anat Jan 10, 2013  - we should remove this transaction from transactions table.
						m_transactionsModel.remove(serverTransaction);
						
						Response response = SipJainFactories.getInstance().getMesssageFactory().createResponse(
							Response.CALL_LEG_OR_TRANSACTION_DOES_NOT_EXIST,
							sipRequest);
						int length = response.getReasonPhrase().length()
							+ e.getMessage().length()
							+ 16;
						StringBuffer reason = new StringBuffer(length);
						reason.append(response.getReasonPhrase());
						reason.append("; ");
						reason.append(e.getMessage());
						response.setReasonPhrase(reason.toString());
						MessageContext messageContext = MessageContextFactory.instance().getMessageContext(response);
						messageContext.setSipConnection(sipTransportConnection);
						
						m_transportCommLayerMgr.sendMessage(messageContext, provider, sipTransportConnection);
					}
					catch (SIPTransportException transportX) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "processTransportSipMessage", transportX.getMessage());
						}
					}
					
					//there is no need to send the Cancel request to the transport layer 
					//since we already sent 481 for it
					return;
				}
			}					
			
			if( serverTransaction==null)
			{
				//we will enter here on an ACK with no Transaction
				SipEvent event = new SipEvent(  provider , -1, sipRequest );
				m_uaCommLayer.sendEventToUA( event );
			}
			else
			{
			    if (serverTransaction.getTransportConnection() == null) {
			    	serverTransaction.setTransportConnection(sipTransportConnection);      
			    }

			    //send request to transport layer
			    serverTransaction.processRequest(sipRequest);
			}
		}
		else {
			// Handle a SIP Reply message.
			ResponseImpl sipResponse = (ResponseImpl) sipMsg;
			SIPClientTranaction clientTransaction = m_transactionsModel.getClientTransaction(key);
			if (clientTransaction != null &&
				clientTransaction.getState() != SIPClientTranaction.STATE_TERMINATED)
			{
				clientTransaction.setTransportConnection( sipTransportConnection );					
				clientTransaction.processResponse(sipResponse);
			}
			else
			{
				if(c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "processTransportSipMessage",
						"Got a stray response");
				}
				// 3261 18.1.2 - handle a "stray" response
				SipEvent event = new SipEvent(provider, -1, sipResponse);
				m_uaCommLayer.sendEventToUA(event);		
			}
		}	
	}
	
	/**
	 *  method is calles from the Transport Layer
	 *  on receiving a request
	 *  this is checked if the transaction is Invite or not
	 *  and send to the right function
	 */
	public SIPClientTranaction createClientTransaction(Request req,
		SipProvider provider,
		BranchMethodKey key, long transactionId)
		throws SipParseException
	{
		SIPClientTranaction retVal = null;
		
		String method = req.getMethod();
		if( Request.INVITE.equals(method))
		{
			retVal = new SIPInviteClientTransactionImpl(this, provider, req, key, transactionId);
		}
		else
		{
			retVal = new SIPNonInviteClientTransactionImpl(this, provider, req, key, transactionId);
		}                                                

		return retVal;

	}
	
	/**
	 *  method is calles from the Transport Layer
	 *  on receiving a request
	 */
	private SIPServerTransaction createServerTransaction(
		Request req,
		SipProvider provider,
		BranchMethodKey key,
		MergedRequestKey fromCallIdCSeqKey)
		throws SipParseException
	{                                    
		SIPServerTransaction transaction;
		if (req.getMethod().equals(Request.INVITE)) {
			transaction = new SIPInviteServerTransactionImpl(this, provider, req, key, fromCallIdCSeqKey);
		}
		else {
			transaction = new SIPNonInviteServerTransactionImpl(this, provider, req, key, fromCallIdCSeqKey);
		}
		return transaction;
	}
	
	
						
	
	/**
	 * a dispacher of messages to the UA
	 * @author Amirk
	 *
	 * To change this generated comment go to 
	 * Window>Preferences>Java>Code Generation>Code and Comments
	 */
	public class UACommLayer
	{
	
		/** constructor */
		public UACommLayer()
		{
		}
	
		/** put transport message on UA queue */
		public void sendEventToUA( final SipEvent event )
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					if( c_logger.isTraceDebugEnabled())
					{
						c_logger.traceDebug(this,"sendEventToUA","dispaching transaction " + event.getTransactionId());
					}
					SipProviderImpl provider =  ( SipProviderImpl )event.getSource();
					provider.onTransportEvent( event );
				}
			};
		
			ThreadPool.instance().invoke( r );
		}	
	}
		
}
