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
package com.ibm.ws.sip.container.transaction;


import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.message.Request;

import java.io.IOException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.naptr.ISenderListener;
import com.ibm.ws.sip.container.naptr.SendProcessor;
import com.ibm.ws.sip.container.naptr.SenderFactory;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.proxy.SubsequentRequestListener;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl.MessageType;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.servlets.SipSessionSeqLog;
import com.ibm.ws.sip.container.tu.TUKey;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.stack.transport.virtualhost.SipVirtualHostAdapter;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * @author yaronr
 * Created on Jul 15, 2003
 *
 * Represents a client transaction
 */
public class ClientTransaction extends SipTransaction implements ISenderListener
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ClientTransaction.class);
			
	/**
	 * Holder for the NAPTR processor
	 */
	private SendProcessor _sender;

	/**
	 * Defines the target of this request.
	 */
	private SipURL _target;
	

	/**
	 * Constructor
	 * 
	 * @param transactionID - the transaction ID
	 * @param request - the actual request that should be send downstream
	 */
	protected ClientTransaction(SipServletRequestImpl request) 
	{
		super(request);
		if(c_logger.isTraceDebugEnabled())
        {			
        	c_logger.traceDebug(this, "ClientTransaction", "request.getMethod()="+ request.getMethod()); 
        }
	}
	
	/**
	 * Set the listener for this transaction
	 * @param listener - the listener
	 */
	public void setClientTransactionListener
							(ClientTransactionListener listener)
	{
		setTransactionListener(listener);
	}
	
	/**
	 * Send the request. 
	 * @param request - the request to be sent
	 * @return true if the request was sent or false if the request was not 
	 * sent as the listener for the transaction cancelled the operation.
	 */
	public boolean sendRequest() throws IOException
	{
		boolean rc = true; 
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "sendRequest");
		}
		
		try{
			
		    OutgoingSipServletRequest request = 
							(OutgoingSipServletRequest) getOriginalRequest();
	
		    if(c_logger.isTraceDebugEnabled())
	        {
	        	c_logger.traceDebug(this, "sendRequest" ,
									"Client Transaction: send " + request.getMethod());
	        }
		    
		    rc = getListener().onSendingRequest(request);
			
		    if(rc)
			{
				// Flag is used to notify the SenderFactory about the case like
				// CANCEL when request should be sent to the same destination as 
		    	//initial INVITE.
				boolean destinationIsKnown = false;
		    	
		        if (request.isMarkedForErrorResponse()){
		        	processCompositionError();
		        	rc = false;
		        	return rc;
		        }
		        
		        //before deciding on targets, we need to make sure there's a request URI 
		        request.setupRequestUri();
		        
		        // The checkIsLoopback() method must be called here before we try to get the target
		        // since this method is doing more than what it is announcing.
		        // In case of application router, route mode, the route headers are added by this method
		        // to the request so we need to call it before we compute the request target
		        //
		        // TODO: Change the application router logic to add the route headers in another place
		        // and not as part of the checkIsLoopback() method
		        SipServletDesc desc = request.getTransactionUser().getSipServletDesc();
		        String vhName = null;
		        String appName = null;
		        if(desc != null){
			        vhName = desc.getSipApp().getVirtualHostName();
			        appName = desc.getSipApp().getApplicationName();
		        }

		        boolean isLoopBack = false;
		        if(desc == null || !SipVirtualHostAdapter.isVirtualHostExcludedFromComposition(vhName)){
		        	//note - if there is not desc, meaning no known originating application, this will still go through composition as before
		        	if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(null, "sendRequest", "appName = "+appName+", vhName="
								+ vhName + " not excluded from composition");
					}
			        try {
						if (!request.isExternalRoute() && request.checkIsLoopback()){
							isLoopBack = true;
						}
					} catch (SipParseException e){
			            if (c_logger.isErrorEnabled()){
			                c_logger.error("error.send.request", Situation.SITUATION_REQUEST, new Object[]{this}, e);
			            }
			            
			            request.logExceptionToSessionLog(SipSessionSeqLog.ERROR_SEND_REQ, e);
			            throw (new IOException(e.getMessage()));
			        }
		        }else{
		        	if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(null, "sendRequest", "appName = "+appName+", vhName="
								+ vhName + " is on excluded from composition list, continuing with request send without calling application router");
					}
		        }
	
		        if (request.getMethod().equals(Request.CANCEL)) {
					// The CANCEL request should be sent to the same destination as
					// original INVITE was sent.
					_target = getListener().getUsedDestination();
					destinationIsKnown = true;
					if( _target == null){
			        	rc = false;
			        	if (c_logger.isTraceDebugEnabled()) {
			                c_logger.traceDebug(this, "sendRequest",
			                			"Failed to send CANCEL - target was not found !!!! ");
			            }
			        }
		        }
		        else{
		        	_target = (SipURL)SipStackUtil.createTargetFromMessage(request.getRequest()).clone();
		        }	    
		        
				try {
					if (SipStackUtil.isOutOfDialogRequest(request.getRequest())) {
						SipStackUtil.fixHeaders(request.getRequest());
					}
				}
				catch (SipParseException e) {
					throw new IOException(e.getMessage());
				}
				
				// The actual implementation for sending is in the request object because 
				// it needs access to internal state of the request.               					 
				request.setupParametersBeforeSent(_target, isLoopBack);
				
		        if (rc) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "sendRequest", "TargetUri = "+ _target);
					}
					_sender = SenderFactory.getNaptrProcessor(destinationIsKnown);
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "sendRequest","Got new Sender = " + _sender);
					}
					_sender.sendRequest(request, this);
				}	        
			}
			else{
			    if (c_logger.isTraceDebugEnabled()) {
	                c_logger.traceDebug(this, "sendRequest", 
	                    "Request was not sent - intercepted by listener");
	            }
			}
			return rc; 
		}finally{
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "sendRequest" ,  Boolean.valueOf(rc));
			}
		}
	}

	/**
	 * A response arrived
	 * @param response - the response
	 */	
	public void processResponse(SipServletResponseImpl response) 
	{
		if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { Integer.toString(response.getStatus()), 
        						response.getReasonPhrase() }; 
        	c_logger.traceEntry(this, "processResponse", params); 
        }
		_sender.responseReceived(response,this);
        
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "processResponse");
		}
	}
	
	
	/**
	 *  @see com.ibm.ws.sip.container.naptr.ISenderListener#finalResponse(com.ibm.ws.sip.container.servlets.IncomingSipServletResponse)
	 */
	public void responseReceived(SipServletResponseImpl response){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { Integer.toString(response.getStatus()),
					response.getReasonPhrase() };
			c_logger.traceEntry(this, "responseReceived", params);
		}

		response.setRequest(getOriginalRequest());

//		We need to use additional reference to the listerne because in case
//		when derived session created - we should use this derived session as 
//		a listenere and not the original TU.
		ClientTransactionListener listenerToUse = getListener();
		
		int status = response.getStatus();
		
		try {
		TransactionUserWrapper transactionUser = getOriginalRequest().getTransactionUser();

		// Session object might not be available when request/responses are used
		// internally for proxying
		// this part should not be called if this is a response for a proxy request
		if(transactionUser != null && response.getProxyBranch() == null && !(m_listener instanceof SubsequentRequestListener)){
			if(SipUtil.canCreateDerivedSession(response)== true){
				String receivedTag = response.getResponse().getToHeader().getTag();
				
				if(receivedTag!=null && transactionUser.getRemoteTag()!= null){
					String tagToCompare = null;
					boolean lookForTU = false;
					if(transactionUser.isProxying()){
						// In case this is Proxy and remote tag received
						// in the response doesn't match the getRemoteTag()
						// and neither the getRemoteTag_2() - Derived session
						// should be created. When this Proxy - we don't know 
						// which side sent the response so we should check both sides.
						if(!receivedTag.equals(transactionUser.getRemoteTag_2()) && 
								!receivedTag.equals(transactionUser.getRemoteTag())){
							lookForTU = true;
						}
					}
					else{
						if(!receivedTag.equals(transactionUser.getRemoteTag())){
							lookForTU = true;	
						}
					}
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "responseReceived",
								"Tag Received = " + receivedTag + "Tag toCompare = " + tagToCompare);
					}
					if(lookForTU){
						//Derived Session should be created if not exist
						TUKey key = ThreadLocalStorage.getTUKey();
						key.setParams(response.getResponse(), MessageType.INCOMING_RESPONSE);
						transactionUser = SessionRepository.getInstance().getTuWrapper(key);
						if(transactionUser == null){
							transactionUser = getOriginalRequest().
							getTransactionUser().createDerivedTU(response.getResponse(),
									"ClientTransactoin - response with different tag is received");
						}
						// Replace the listeren for future use on this response.
						listenerToUse = transactionUser;
					}
				}
			}
			response.setTransactionUser(transactionUser);
			transactionUser.updateSession(response);
		}
		
		
		if(response.getMethod().equals(Request.INVITE) && isProvisional(response)){
			// When we received the response on INVITE we should save the destination
			// where it was sent to prepare for the case when the UAC will want to send
			// CANCEL request which should be sent to the same destinatio as INIVTE.
			// As CANCEL can be sent only after provisional response - the destination
			// info should be saved only when provisional received.
			listenerToUse.setUsedDestination(_sender
					.getLastUsedDestination(this));
		}

		// mark the transaction as terminated for future processing
		if (status >= 200) {
			markAsTerminated();
		}

		// Let the listener process the response - either the session itself
		// or a proxy
		listenerToUse.processResponse(response);
		}
		
		finally {

			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "responseReceived", "Clean transaction if need for status = " + status);
			}
			if (status >= 200) {
				onFinalResponse(response);
				finishToUseSender();
			}
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "responseReceived");
		}
	}
	
	/**
	 * Helper method which checks if received response is provisional or not.
	 * @param response
	 * @return
	 */
	private boolean isProvisional(SipServletResponseImpl response) {
		if(response.getStatus() >= 100 && response.getStatus() < 200 ){
			return true;
		}
		return false;
	}

	/**
	 * Helper method that updates the listener after all hops were tried and
	 * no one was found as reachable.
	 *
	 */
	public void failedToSendRequest(boolean errorDuringSent) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "failedToSendRequest",
					"Request failed to be sent. errorDuringSent " + errorDuringSent + " Request = " + getOriginalRequest());
		}
		
		// Get a reference to the transaction table and check if this ClientTransaction was already
		// stored there.
		// The ClientTransaction is added to the table only when sent performed. If there is an error
		// during NAPTR resolve or message is failed to be sent because of network issues - such
		// ClientTransaction is not in the table.
   		TransactionTable tt = TransactionTable.getInstance();
   		SipTransaction clientTr = tt.getTransaction(getTransactionID());
   		
		timedOut();
		
		if(clientTr == null && errorDuringSent ){
   			if (c_logger.isTraceDebugEnabled()) {
   				c_logger.traceDebug(this, "failedToSendRequest",
   						"This ClientTransaction was ended with error during sent but not added to the TransactionTable yet. Notify Listener. TransactionId =  " + getTransactionID());
   			}
			// In such case, ClientTransaction was not added to the SipTransactions table
			// we need to update listener about failed request 
			getListener().removeTransaction(getOriginalRequest().getMethod());
   		}
	}	

	/**
     * Process Timeout Event for the transaction. 
     */
    public void processTimeout()
    {
    	_sender.processTimeout(this);
    }
    
    /**
     *  @see com.ibm.ws.sip.container.naptr.ISenderListener#timedOut()
     */
    public void timedOut() {
   		finishToUseSender();
   		getListener().processTimeout(getOriginalRequest());
   		
    	//Call on the base class to do the cleanup. 
   		super.processTimeout(); 
	}
    
    /**
     * Error in appilcation rotuer should response
     * 500 upstream.
     */
    public void processCompositionError(){
    	
        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "processCompositionError");
		}

        _sender = SenderFactory.getNaptrProcessor(true);
   		finishToUseSender();
    	
   		getListener().processCompositionError(getOriginalRequest());
    	//Call on the base class to do the cleanup. 
   		super.processTimeout(); 

   		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "processCompositionError");
		}
    }
	
    /**
     * remove the transaction from the transaction table
     * 
     */
    public void clearTransaction(){
    	markAsTerminated();  
		removeFromTransactionTable(null); 
		finishToUseSender();
    }
    
    /**
     * Helper method that notifies the SenderFactory that this
     * TransactionClient finished to use the sender received from 
     * SenderFactory.
     */
    private void finishToUseSender() {
    	SenderFactory.finishToUseSender(_sender);
	}

	/**
     * Update the transaction Id and also updated the transaction table at 
     * this time. Prior to this time the transaction id was not available 
     * therefore was not added to the transaction table.. 
     * @see com.ibm.ws.sip.container.transaction.Transaction#setId(long)
     */
    public void saveTransactionId(long transactionId)
    {
        super.setId(transactionId);
        
        //Amirp: No need to put ACKs in the transaction table as they will
        //never get a response back. 
        if(getTransactionID() != -1 && 
           !getOriginalRequest().getMethod().equals(Request.ACK))
        {
        	TransactionTable.getInstance().putTransaction(this); 
        	getOriginalRequest().getTransactionUser().storeClientTransaction(this);
        }
    }

	/**
	 * @see com.ibm.ws.sip.container.transaction.SipTransaction#transactionTerminated(com.ibm.ws.sip.container.servlets.SipServletRequestImpl)
	 */
	protected void transactionTerminated(SipServletRequestImpl request) {
		getListener().clientTransactionTerminated(request);
		
		request.getTransactionUser().removeClientTransaction(this);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.transaction.ISenderListener#getOutgoingRequest()
	 */
	public OutgoingSipServletRequest getOutgoingRequest() {
		return ((OutgoingSipServletRequest)getOriginalRequest());
	}

	/** 
	 * @see com.ibm.ws.sip.container.naptr.ISenderListener#getTarget()
	 */
	public SipURL getTarget() {
		return _target;
	}

	/**
	 * @return The listener for this transaction
	 */
	public ClientTransactionListener getListener() {
		return (ClientTransactionListener)m_listener;
	}
	
	@Override
	protected void notifyDerivedTUs() {
		//Nothing to do here
	}

	@Override
	public void addReferece(TransactionUserWrapper dtu) {
		//Nothing to do here
		
	}
	@Override
	protected void replaceListener() {
		//Nothing to do here		
	}
}
