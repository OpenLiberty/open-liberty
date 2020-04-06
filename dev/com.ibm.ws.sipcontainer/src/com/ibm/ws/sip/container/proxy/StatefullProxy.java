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
package com.ibm.ws.sip.container.proxy;

import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.ResponseImpl;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.router.SipServletInvokerListener;
import com.ibm.ws.sip.container.servlets.IncomingSipServletRequest;
import com.ibm.ws.sip.container.servlets.IncomingSipServletResponse;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletResponse;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;
import com.ibm.ws.sip.container.servlets.SipSessionSeqLog;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transactions.SIPTransactionHelper;

/**
 * @author yaronr Oct 27, 2003
 * 
 * Statefull Proxy works both in sequential and parallel mode.
 * 
 */
public class StatefullProxy	extends BranchManager 
							implements Proxy,
							SipServletInvokerListener
							
{
	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(StatefullProxy.class);

	/**
	 * Timer which is responsible for whole proxy timeout.
	 */
	private ProxyTimer _timer;

	/**
	 * Whether the application adds a Path header to the REGISTER request. 
	 * The default is false.
	 */
	private boolean _addPath = false;

	/**
	 * flag specifying whether whether the servlet is invoked to handle
	 * responses.
	 */
	private boolean _isSupervised = true;

	/**
	 * the time the container waits for a final response before it cancels
	 * the branch and proxies to the next destination in the target set. 
	 */
	private int _proxyTimeOut = SipConstants.DEFAULT_PROXY_TIMEOUT_SECONDS;


	/**
	 * flag specifying whether to proxy to multiple destinations in parallel 
	 * or sequentially. 
	 */
	protected boolean _isParallel = true;

	/**
	 * A flag indicating if a virtual brnach was already created
	 */
	private ProxyBranchImpl _virtualBranch = null;

	private boolean _isFinalResponseForwarded = false;
	
	private boolean _noCancelOfBranchesOnComplete = false;

	/**
	 * Constructs a new Proxy for the given request.
	 * @param originalReq The original Sip Servlet Request associated with this
	 * proxy.  
	 */
	public StatefullProxy(SipServletRequestImpl	 originalReq)
	{
		super(originalReq);

	    if(c_logger.isTraceDebugEnabled())
        {
	        c_logger.traceDebug(this, "StatefullProxy", 
	        		getMyInfo() + "created " + originalReq.getMethod());
		}

		SipServletDesc siplet = originalReq.getTransactionUser().getSipServletDesc();
		if(siplet != null)
		{
			_proxyTimeOut = siplet.getSipApp().getProxyTimeout();
		}
		if(SipSessionSeqLog.isEnabled())
        {
			// Update Session sequence log
			TransactionUserWrapper tu = getTransactionUser();

			tu.logToContext(SipSessionSeqLog.PROXY_IS_RR, getRecordRoute());
			tu.logToContext(SipSessionSeqLog.PROXY_IS_RECURSE, getRecurse());
			tu.logToContext(SipSessionSeqLog.PROXY_IS_PARALELL, getParallel());
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.BranchManager#setupTimeOut()
	 */
	void setupTimeOut() {
		if (_timer == null || _timer.isCancelled()) {
			// This is a first time when sent out proxied request from this
			// StatefullProxy object. ProxyTimer should be created
			// If timer is cancelled - meaning that this timer was cancelled
			// before new outgoing request sent. It can happen when timer was
			// fired and StatefullProxy is in sequential mode - timer cancelled
			// and request sent to next destination;
			_timer = new ProxyTimer(this);
			SipContainerComponent.getTimerService().schedule(_timer, false,
					getProxyTimeout() * 1000);
		}
	}

	/**
	 * @see javax.servlet.sip.Proxy#getParallel()
	 */
	public boolean getParallel()
	{
		return _isParallel;
	}

	/**
	 * @see javax.servlet.sip.Proxy#getRecordRoute()
	 */
	public boolean getRecordRoute()
	{
		return _isRecordRoute;
	}

	/**
	 * @see javax.servlet.sip.Proxy#getPathAddress()
	 */
	SipURI getPathAddress(String transport) 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getPathAddress");
		}
		
		if ( null == _pathUri )
		{
			try
            {
				//	BLP: First check to see if the application has set up a record route. If not we will just
				//	use the defaults for the Path.	
	        	if (getTransactionUser() != null && getTransactionUser().getPreferedOutboundIface(transport) >= 0)
	        	{
	        		//	An outbound interface is set on the session object.
	        		_pathUri = (SipURI)SipProxyInfo.getInstance().getOutboundInterface(getTransactionUser().getPreferedOutboundIface(transport), transport).clone();
	        	}
	        	else if (getPreferedOutboundIface(transport) >= 0)
	        	{
	        		//	An outbound interface is set on the proxy object.
	        		_pathUri = (SipURI)SipProxyInfo.getInstance().getOutboundInterface(getPreferedOutboundIface(transport), transport).clone();
	        	}
	        	else
	        	{
					SipServletsFactoryImpl f = SipServletsFactoryImpl.getInstance();
			    	SipURI _pathUri = f.createSipURI("",_host);
			    	_pathUri.setPort(_port);
			    	_pathUri.setTransportParam(_transport);
			    	
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "getPathAddress", getMyInfo() + "_pathUri = " + _pathUri);
					}
	        	}
            }
            catch (IllegalArgumentException e)
            {
            	Object[] args = { e };
            	if(c_logger.isErrorEnabled())
    		    {
            	    c_logger.error("error.create.record.route.uri", 
									Situation.SITUATION_REQUEST, args, e);
    		    }
            }
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getPathAddress");
		}
		return _pathUri;
	}

	/**
	 * @see javax.servlet.sip.Proxy#getRecurse()
	 */
	public boolean getRecurse()
	{
		return _isRecurse;
	}

	/**
	 * @see javax.servlet.sip.Proxy#getStateful()
	 */
	public boolean getStateful()
	{
		return true;
	}

	/**
	 * @see javax.servlet.sip.Proxy#getSupervised()
	 */
	public boolean getSupervised()
	{
		return _isSupervised;
	}

	/**
	 * @see javax.servlet.sip.Proxy#setParallel(boolean)
	 */
	public void setParallel(boolean parallel)
	{
		_isParallel = parallel;
		
		//TODO: How to change from sequential to parallel if already started
	}

	/**
	 * @see javax.servlet.sip.Proxy#setRecordRoute(boolean)
	 */
	public void setRecordRoute(boolean rr)
	{
		if(_started){
	        throw new IllegalStateException(
	                                    "ProxyTo() was already executed");
	    }
	    
		if (_isRecordRoute == rr){
			return;
		}
		
		// Anat: According to the JSR 289 - setRecordRoute and getRecordRoute - can be called
		// always not only on dialogs.
		_isRecordRoute = rr;
		
	}

	/**
	 * @see javax.servlet.sip.Proxy#setSequentialSearchTimeout(int)
	 */
	public void setSequentialSearchTimeout(int seconds)
	{
		if(seconds <= 0)
		{
		    if(c_logger.isWarnEnabled())
            {			
                Object[] args = { Integer.toString(seconds) }; 
                c_logger.error("warn.invalid.timeout.value", 
                    			Situation.SITUATION_CREATE, args); 
            }
		}
		
		_proxyTimeOut = seconds;
	}

	/**
	 * @see javax.servlet.sip.Proxy#setStateful(boolean)
	 */
	public void setStateful(boolean stateful) {
	}

	/**
	 * @see javax.servlet.sip.Proxy#setSupervised(boolean)
	 */
	public void setSupervised(boolean supervised)
	{
		_isSupervised = supervised;
	}

	
	/**
	 * Helper function for accessing the SipSession associated with this request
	 * 
	 * @return
	 */
	private final TransactionUserWrapper getTransactionUser() {
        IncomingSipServletRequest origRequest =
            (IncomingSipServletRequest) getOriginalRequest();
		return origRequest.getTransactionUser();
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.ProxyParent#process1xxResponse(javax.servlet.sip.SipServletResponse)
	 */
	public void process1xxResponse(SipServletResponse response,ProxyBranchImpl branch) {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "process1xxResponse", getMyInfo());
		}

		//Proxy Provisional response is not committed.
		((IncomingSipServletResponse)response).setIsCommited(false);
		
		if (response.getStatus() != 100) {
    		IncomingSipServletRequest origRequest =
                (IncomingSipServletRequest) getOriginalRequest();
    		associateResponseWithSipSession((SipServletResponseImpl)response,branch);
            forwardResponse(response, origRequest, ((IncomingSipServletResponse)response).getTransactionUser());
		}
	}
   
	/**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processResponse(javax.servlet.sip.SipServletResponse)
     * 
     * @param response - the response that arrived
     * 
     * Note that this function is synchronized to avoid a race condition with 
     * a response generated by the application. see 
     * 					processApplicationResponse(SipServletResponse response)   
     */
    public synchronized void processResponse(ProxyBranchImpl branch, SipServletResponse response)
    {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "processResponse", new Object[]{branch, response});
		}
		IncomingSipServletResponse inResponse = ((IncomingSipServletResponse)response);
		// Proxy Final response is not committed.
		inResponse.setIsCommited(false);
		
        IncomingSipServletRequest origRequest =
            (IncomingSipServletRequest) getOriginalRequest();

		// SIP Servlet API, version 1.0 8.2.3:
        // "...When in stateful mode, the servlet container is responsible for automatically forwarding the 
        // following responses received for a proxy operation upstream: 
        // 		* all information responses other than 100
        //  	* the best response received when final responses have been received from all destination
		// * all 2xx responses
		int status = inResponse.getStatus();

		// "...When a 2xx or 6xx response is received the server CANCELs all
		// outstanding branches and will not create new branches"
        if (isFinalResponse(status))
        {
        	associateResponseWithSipSession(inResponse, branch);
        	inResponse.getTransactionUser().setProxyReceivedFinalResponse(true, status);
        	updateBestResponse(inResponse, branch);
            cancelProxyTimer();
            if (!_noCancelOfBranchesOnComplete) {
            	cancellAllActiveBranches(null,null,null);
            } else {
    			if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "processResponse", "no Cancel set, skipping canceling of completed branches.");
    			}
            }
            
            //the transaction can be explicitly terminated by the application so we
            //must change the state before going to the application
            inResponse.getTransactionUser().setProxyReceivedFinalResponse(true, status);
            
            forwardResponse(inResponse, origRequest, inResponse.getTransactionUser());
            return;
		}

        //In sequential mode for each new branch new ProxyTimer should be created
		if (!getParallel()) {
			cancelProxyTimer();
		}

		if (branch.isCompleted()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processResponse", getMyInfo() + "This branch is completed");
			}
			branchCompleted(branch, inResponse);
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "processResponse");
		}
	}

 
	/**
	 * Helper method which cancelled the ProxyBranchTimer.
	 * 
	 */
	private void cancelProxyTimer() {
		if (_timer != null && !_timer.isCancelled()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "cancelProxyTimer", getMyInfo());
			}
			_timer.cancel();
			_timer = null;
    	}
    	else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "cancelProxyTimer", getMyInfo() + "Timer is null or cancelled = " + _timer);
			}
		}
	}

    
	/**
	 * @see javax.servlet.sip.Proxy#cancel()
	 */
	public synchronized void cancel() {
		cancel(null,null,null);

	}
	/**
	 * Cancels all the active branches of this proxy
	 * @param reasons all the reason headers to be inserted to the outgoing cancel messages.
	 * 
	 */
	public synchronized void cancel(String[] reasons) {
		 if(c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "cancel", getMyInfo() + "Proxy operation is canceled by the server");
			}
			
		if(_bestResponse.getBestResponse() != null && 
					isFinalResponse(_bestResponse.getBestResponse().getStatus())) {

				throw new IllegalStateException("Proxy completed");
			}
		 for (int i = 0; i < _proxyBranches.size(); i++) {
				ProxyBranchImpl branch = (ProxyBranchImpl) _proxyBranches.get(i);
				if (branch.isActive()) {
					branch.cancel(reasons);
				}
			}
	}

	/**
	/** @see javax.servlet.sip.Proxy#cancel(java.lang.String[], int[], java.lang.String[])
	 */
	public void cancel(String[] protocol, int[] reasonCode, String[] reasonText) {
		if(_bestResponse.getBestResponse() != null && 
				isFinalResponse(_bestResponse.getBestResponse().getStatus())){

			throw new IllegalStateException("Proxy completed");
		}

	    if(c_logger.isTraceDebugEnabled())
        {
			c_logger.traceDebug(this, "cancel", getMyInfo() + "Proxy operation is canceled by the application");
		}
		
		if((protocol  != null) && (reasonCode != null) && (reasonText != null)) {
			//checking that reason header protocol field is unique
			Set<String> uniqueProtocols = new HashSet<String>();
	        for(String prot : protocol){
	        	if(uniqueProtocols.contains(prot)) {
	        		throw new IllegalArgumentException(SipUtil.REASON_PROTOCOL_MULTIPLE);
	        	}
	        	else {
	        		uniqueProtocols.add(prot);
	        	}
	        }
        
		}
		cancellAllActiveBranches(protocol,reasonCode,reasonText);

		// Remove all branches
	}

	/**
	 * Helper method which cancels all Active branches.
	 * @param reasonText 
	 * @param reasonCode 
	 * @param protocol 
	 * 
	 */
	private void cancellAllActiveBranches(String[] protocol, int[] reasonCode, String[] reasonText) {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "cancellAllActiveBranches", getMyInfo());
		}


		for (int i = 0; i < _proxyBranches.size(); i++) {
			ProxyBranchImpl branch = (ProxyBranchImpl) _proxyBranches.get(i);
			if (branch.isActive()) {
				branch.cancel(protocol,reasonCode,reasonText);
			}
		}
	}

	/**
	 * Forward a response upstream (and to the application if needed)
	 * 
     * @param response the response that arrived and need to be sent 
     * @param origRequest the original request that we should response
     * @param onlyAfterAllBranchesCompleted if true, we need to check if all 
     * 				branches are completed 
     */
    protected void forwardResponse(
        SipServletResponse response,
        IncomingSipServletRequest origRequest,
        TransactionUserWrapper tu){

		// if the request is already committed, we cannot handle this
    	// in case when this is 2xx response we should forward it to the UAC
		if (_isFinalResponseForwarded && !SipUtil.is2xxResponse(response.getStatus())) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "forwardResponse", getMyInfo() 
						+ "Can not forward response - final response already forwarded. ");
			}

			return;
		}

		// if this is an application generated response
		// which is actually part of a virtual proxy branch
		if (response instanceof OutgoingSipServletResponse && ((OutgoingSipServletResponse) response).isOfVirtualProxyBranch()) {

			// This response MUST have been generated by the application itself
			// (virtual branch - JSR 116 - 8.2.2), send it upstream directly.

			// Change the Session mode from proxy to UAS as the application
			// generated the final response for the transaction.
			origRequest.getTransactionUser().setIsProxying(false);

			// Pass response up stream
			sendAppResponseUpstream(response);

			return;
		}

        if(c_logger.isTraceDebugEnabled())
    	{
			// Print after we have response object which might be null at first
			StringBuffer buffer = new StringBuffer(getMyInfo());
			buffer.append("response is ");
			buffer.append(response.getStatus());
			buffer.append(response.getReasonPhrase());
			c_logger.traceDebug(this, "forwardResponse", buffer.toString());
		}

		// Add the session to the SessionTable and
		// Update Routing info if needed
		if (((SipServletResponseImpl) response).isReliableResponse()) {
			((SipServletResponseImpl) response).getTransactionUser().updateWithProxyReliableResponse(response);
		}
		// Should we send to application too?
        if (getSupervised())
        {
            sendResponseToApplication(response, this,tu);
        }
        else
        {
			sendResponseUpstream(response);
		}
	}

    /**
     * Send a response upstream to the caller.  
     * @param response The response coming from the branch. A new response object
     * will be created and sent upstream to the caller.   
	 */
    protected void sendResponseUpstream(SipServletResponse response)
    {
    	if (c_logger.isTraceEntryExitEnabled())
        {
			c_logger.traceEntry(this, "sendResponseUpstream", response);
		}
    	
		// Create the response upstream in case a response has not been
		// generated yet to the original request.
		OutgoingSipServletResponse outgoingResponse = null;
		IncomingSipServletRequest req = (IncomingSipServletRequest)_originalReq;
		//if it is lock this object
		synchronized (this) {
			//recheck that no final response had been sent
			if (!_isFinalResponseForwarded){
				outgoingResponse = createOutgoingResponse(req, response);
				//check if its a final response
				if (SIPTransactionHelper.isFinalResponse(response.getStatus())){
					//set the flag to true
					_isFinalResponseForwarded = true;
				}
			}
		}
		
        if(outgoingResponse != null)
		{
			// Send the response
		    try
		    {
		    	if((outgoingResponse.getStatus() >= 200 && outgoingResponse.getStatus() < 300) ||
		    			outgoingResponse.getStatus() >= 400){
		    		if (c_logger.isTraceDebugEnabled())
		            {
		                c_logger.traceDebug(this, "sendResponseUpstream", 
		                			"This response is final response on original request." );
		            }	
		    	_originalReq.setIsCommited(true);
		    	}
				outgoingResponse.send();
		    }
		    catch (IOException e)
		    {
	            if(c_logger.isErrorEnabled())
	            {
					Object[] args = { response };
		            c_logger.error(
		                "error.forward.response",
		                Situation.SITUATION_REQUEST,
		                args,
		                e);
				}
			}
		}
		else
		{
			int status = response.getStatus();
		    if(status >= 200 && status < 300)
		    {
		    	if(c_logger.isTraceDebugEnabled())
	            {
	            	c_logger.traceDebug(this, "sendResponseUpstream" ,
	            			getMyInfo() + "Bypassing transaction layer sending response upstream, " +
							"response already generated by application");
	            }
	           	// We need to update state of TransactionUser (DerivedTU) but we can't
            	// use the ServerTransaction to send the response upstream 
            	// as is already completed by previous response.
            	SipServletResponseImpl resImpl = (SipServletResponseImpl) response;
            	resImpl.getTransactionUser().onSendingResponse(response);
            	
            	ResponseImpl jainRes = (ResponseImpl)resImpl.getResponse();
            	RequestImpl jainOrigReq = (RequestImpl)_originalReq.getRequest();
            	//Fix PMR 81183,756,000. If the incoming response was a loopback (since outgoing request was)
				//then it will stay loopback through all hopes, even if it needed to go externally.
            	//To fix this we need to make sure it will be loopback only if the incoming Request was received as loopback as well. 
            	jainRes.setLoopback(jainOrigReq.isLoopback()); 
            	SipRouter.sendResponseDirectlyToTransport( resImpl.getSipProvider(), 
			   	   resImpl.getResponse(),
			       true);
		    }
		    else
		    {
		        if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "sendResponseUpstream", 
                    		getMyInfo() + "Response NOT sent upstream, transaction already committed. " 
                        + response);
                }
		    }
		}
        
        if(response.getStatus() >= 200){
    		//need to understand what is the deal with 3xx messages in this point
        	//sending a final response upstream, so there is no need for a proxy timer,
        	//assuming all branches area completed or canceled at this point.
        	if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "sendResponseUpstream", 
                		getMyInfo() + response.getReasonPhrase() + 
                		response.getStatus() +" ,call-id="+response.getCallId()
                		+ " Going to cancel proxy timer when forwarding final response upstream.");
            }
        	cancelProxyTimer();
        }
        
        if (c_logger.isTraceEntryExitEnabled())
        {
			c_logger.traceExit(this, "sendResponseUpstream");
		}
        
	}

	/**
	 * @see javax.servlet.sip.Proxy#getAddToPath()
	 */
	public boolean getAddToPath() {
		return _addPath;
	}

	/**
	 * @see javax.servlet.sip.Proxy#getProxyBranch(javax.servlet.sip.URI)
	 */
	public ProxyBranch getProxyBranch(URI uri) {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getProxyBranch", 
					getMyInfo() + "GetBranch by URI = " + uri);
		}

		ProxyBranchImpl foundBranch = null;
		for (int i = 0; i < _proxyBranches.size() && foundBranch == null; i++) {
			ProxyBranchImpl proxyBranch = _proxyBranches.get(i);

			if (proxyBranch.getUri().equals(uri)) {
				foundBranch = proxyBranch;
			} 
			else {
				// look for recurse branches in each branch.
				foundBranch = proxyBranch.findRecurseBranchByUri(uri);
			}
		}

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getProxyBranch", 
					getMyInfo() + "Found Branch " + foundBranch);
		}

		return foundBranch;
	}



	/**
	 * Send a response to the application
     * @param response The response to be sent
     * @param listener Get notification after invoking the siplet
     */
    protected void sendResponseToApplication(
        SipServletResponse response,
        SipServletInvokerListener listener,
        TransactionUserWrapper tu)
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                getMyInfo() + "sendResponseToApplication",
                response.getReasonPhrase() + 
                " " + response.getStatus());
        }

		// Get the session of the original request

		// Patch for fixing SPR #SSUN6BCSWT, See JSR 116 2.2.1 . The response
		// passed to the application must enable accessing the session although
        //it is a separate transaction from the one initiating the proxy operation.
        //In our case we create sessions less outbound request but we now assign 
        //them a session . That should not effect as the req/res has already been 
		// sent and can only be used to access static data.
		SipServletResponseImpl resImpl = (SipServletResponseImpl) response;
		resImpl.setTransactionUser(tu);

		// Amir May 11, We need to fix the following problem: We need the make
        //sure that the call to get session on the request will return a session 
        //object - the same  session that is attached to the original request. 
        //The problem is if we set the session now, the next response that we 
        //will get for this request (e.g. 180 and then 200) will be passed through
		// the session and will update its state which it should not do in the
		// case of proxying as it break the proxying (ACK will not be received).
		// See ClientTransaction.processResponse line 93 - this is where the
		// session state is updated when it should not.
		// For now the request will not have session - we need to refactor this.
		// ((SipServletRequestImpl)resImpl.getRequest()).setSipSession(session);
        //TODO ANAT = can be null when response without tag received.
        if(tu == null){
			// in the associateResponseWithSipSession() method relaited tu is
			// found and corellated with branch - so we should not get here when 
        	// tu is null;
			if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this,
    					"sendResponseToApplication", 
    					 "We have best response without Tug - " + response);
    		}
        	throw new IllegalStateException("No TransactionUserFound");
        }
		// Send using the session
		tu.sendResponseToApplication(response, listener);
	}

	/**
	 * A servlet has been invoked
	 * 
	 * @param response - the response object 
	 * 		(which could be modified by the application)
	 */
    public void servletInvoked(SipServletResponse response)
    {
    	if(c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "servletInvoked",
                getMyInfo() + "call-id=["+response.getCallId()+"],status="+response.getStatus());
        }
        // If the response is not 2XX or 6XX
        // we have to check if the application added a target 
        int status = response.getStatus();
        if ((status > 299) && (status < 600))
        {
            if (areAllBranchesCompleted() == false)
            {
                if(c_logger.isTraceDebugEnabled())
                {
	                c_logger.traceDebug(
	                    this,
	                    "servletInvoked",
	                    getMyInfo() + "added to target set");
                }
                return;
			}
		}
		//	   
		// 
		sendResponseUpstream(response);
	}

	/**
	 * A servlet has been invoked
	 * 
	 * @param request - the request object 
	 * 		(which could be modified by the application)
	 */
	public void servletInvoked(SipServletRequest request)
	{
	    //We should never get here subsequent request should pass through the
	    //RecordRouteProxy
	    
	    if(c_logger.isErrorEnabled())
        {
            c_logger.error("error.call.should.be.invoked", 
                           Situation.SITUATION_CREATE, null);
        }
	    
	}

	/**
	 * @see javax.servlet.sip.Proxy#proxyTo(java.util.List)
	 */
    public synchronized void proxyTo(List uris) throws IllegalStateException
    {
    	if(_originalReq.isCommitted())
	    {
	        throw new IllegalStateException(
	            "Transaction already completed can not proxy request");
	    }
        
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "proxyTo", " " + uris);
        }

        _started = true;
            
		for (Iterator iter = uris.iterator(); iter.hasNext();) {
			
			URI target = (URI) iter.next();
			if(target == null){
	    		throw new NullPointerException(
	            	"One of URIs is null null !!!");
	    	}
			createBranch(target, true, this);
		}

		// call the real implementation to start sending
		startSending();
	}

   
    /**
     * @see com.ibm.ws.sip.container.proxy.BranchManager#proxyTo(javax.servlet.sip.URI)
     */
    public synchronized void proxyTo(javax.servlet.sip.URI nextHop) throws IllegalStateException
    {
        if (c_logger.isTraceEntryExitEnabled())
        {
			c_logger.traceEntry(this, "proxyTo", new Object[]{nextHop, _originalReq.isCommitted()});
		}
        
    	if(nextHop == null){
    		throw new NullPointerException(
            	"uri is null !!!");
    	}
    	
    	if(_originalReq.isCommitted())
	    {
	        throw new IllegalStateException(
	            "Transaction already completed can not proxy request");
	    }

		_started = true;

		createBranch(nextHop, true, this);

		// call the implementation to start sending
		startSending();
		
		if (c_logger.isTraceEntryExitEnabled())
        {
			c_logger.traceExit(this, "proxyTo");
		}
	}

    
	/**
	 * Method that will sent Recurse flag in all relatedProxy branches.
	 * @param recurse
	 */
	public void setRecurse(boolean recurse) {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setRecurse", 
					getMyInfo() + "Set recurse " + recurse);
		}

		// Set recurse in All branches.
		if (_isRecurse == recurse) {
			return;
		}

		_isRecurse = recurse;

		for (int i = 0; i < _proxyBranches.size(); i++) {
			ProxyBranch branch = _proxyBranches.get(i);
			branch.setRecurse(recurse);
		}
	}

	/**
	 * @see javax.servlet.sip.Proxy#setOutboundInterface(java.net.InetSocketAddress)
	 */
    public void setOutboundInterface(InetSocketAddress address) throws IllegalArgumentException
    {
        if (address != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setOutboundInterface", "Attempting to set outbound interface to: " + address);
			}

			boolean isSet = false;
			int index = SipProxyInfo.getInstance().getIndexOfIface(address, "udp");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxUDP = index;
			}

			index = SipProxyInfo.getInstance().getIndexOfIface(address, "tcp");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxTCP = index;
			}

			index = SipProxyInfo.getInstance().getIndexOfIface(address, "tls");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxTLS = index;
			}
			
			if (!isSet)
			{
				throw new IllegalArgumentException("address:" + address + " is not listed as allowed outbound interface.");
			}
		}
        else
			throw new NullPointerException("Invalid address = null");
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.sip.Proxy#setOutboundInterface(java.net.InetAddress)
     */
   	public void setOutboundInterface(InetAddress address) throws IllegalArgumentException
   	{
        if (address != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setOutboundInterface", "Attempting to set outbound interface to: " + address);
			}

			boolean isSet = false;
			int index = SipProxyInfo.getInstance().getIndexOfIface(address, "udp");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxUDP = index;
			}


			index = SipProxyInfo.getInstance().getIndexOfIface(address, "tcp");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxTCP = index;
			}

			index = SipProxyInfo.getInstance().getIndexOfIface(address, "tls");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxTLS = index;
			}
			
			
			if (!isSet)
			{
				throw new IllegalArgumentException("address:" + address + " is not listed as allowed outbound interface.");
			}
		}
        else
			throw new NullPointerException ("Invalid address = null");
	}
    
   	/*
   	 * (non-Javadoc)
   	 * @see com.ibm.ws.sip.container.proxy.BranchManager#getPreferedOutboundIface(java.lang.String)
   	 */
    public int getPreferedOutboundIface(String transport)
    {
    	if (SIPTransactionStack.instance().getConfiguration().getSentByHost() != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getPreferedOutboundIface", "Return OUTBOUND_INTERFACE_NOT_DEFINED since the sentByHost property is set");
			}
			return SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		}
    	
    	if (transport.equals("udp") == true)
    	{
	    	if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug(this, "getPreferedOutboundIface", "Index for udp: " + _preferedOutBoundIfaceIdxUDP);
    		return (_preferedOutBoundIfaceIdxUDP);
		}
    	else if (transport.equals("tcp") == true)
    	{
	    	if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug(this, "getPreferedOutboundIface", "Index for tcp: " + _preferedOutBoundIfaceIdxTCP);
    		return (_preferedOutBoundIfaceIdxTCP);
		}
    	else 
    	{
	    	if (c_logger.isTraceDebugEnabled())
				c_logger.traceDebug(this, "getPreferedOutboundIface", "Index for tls: " + _preferedOutBoundIfaceIdxTLS);
    		return (_preferedOutBoundIfaceIdxTLS);
		}
    }

	/**
	 * It will set AddToPath flag in all relatedProxy branches.
	 * 
	 * @see javax.servlet.sip.Proxy#setAddToPath(boolean) 
	 */
	public void setAddToPath(boolean addToPath) {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setAddToPath", getMyInfo() + "addToPath" + addToPath);
		}

		// Set recurse in All branches.
		if (_addPath == addToPath) {
			return;
		}

		_addPath = addToPath;

		for (int i = 0; i < _proxyBranches.size(); i++) {
			ProxyBranch branch = _proxyBranches.get(i);
			branch.setAddToPath(addToPath);
		}
	}

	/**
	 * A request in this branch is being sent
	 * 
	 * @param branch
	 * @param request
	 */
	public void onSendingRequest(ProxyBranchImpl branch, SipServletRequest request)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
            Object[] params = { branch, request};
            c_logger.traceEntry(this, "onSendingRequest", params);
        }
		
		setupTimeOut();
		// Probably nothing
		// yet, it seems like a good idea to call this method from the branch
		// when sending our new request, might be in use later

        SipServletRequestImpl impl = ((SipServletRequestImpl)request).getTransactionUser() == null ? 
        								(SipServletRequestImpl)getOriginalRequest() : 
        								(SipServletRequestImpl)request;
        impl.getTransactionUser().onSendingRequest(impl);
        
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "onSendingRequest");
        }
	}
	
	
	/**
     * Process a virtual branch response response The response was generated by 
     * the application after the application has previously proxied the response. 
     * See JSR 116 - 8.2.2
     * @param response
     * @return true if the calling thread should continue sending the response
     * on its own or false in case the proxy will send the response up stream at
     * later time if needed.
     * 
     * This function is synchronized to avoid a race condition with a response 
     * received on a branch. see  processResponse(ProxyBranch branch, 
     * 											  SipServletResponse response)
	 */
    public synchronized boolean processApplicationRespone(
                                             OutgoingSipServletResponse response) 
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "processApplicationRespone", getMyInfo() + "response = " + response);
		}

		ProxyBranchImpl proxyBranch = createVirtualBranch(this, response);

		boolean continueSending = true;
		if (response.getStatus() < 200) {
			// Provisional response should always be passed upstream as they
			// do not effect the best response selection process
			if (c_logger.isTraceDebugEnabled()) {
				c_logger
						.traceDebug(this, "processApplicationRespone",
								"Associate the ApplicationResponse with original Session");
			}
        	associateResponseWithSipSession(response,proxyBranch);

        }
        else if(response.getStatus() > 299)
        {
			// This is a non-2xx response
			// The response will be sent upstream later if chosen as the
			// best response when all branches are completed.
            updateBestResponse(response,proxyBranch);
			continueSending = false;
        }
        else
        {
			// If the response that was received refers to the original request
			// e.g. 200OK response on INVITE - this response will cancel all the
			// branches that was already sent

			if (_originalReq.getMethod().equals(response.getMethod())) {
//        		Its a 2xx response - Send upstream and cancel all pending branches
				cancel();

//           	Amir- We should not get here on a response generated by the proxy !!!
//              This response MUST have been generated by the application itself
//           	(virtual branch - JSR 116 - 8.2.2), send it upstream directly. 
           	    
				// We should to associate this response with the session
				// used for the case when additional 200 OK response will be
				// returned from one of the branches though the CANCELL request
//              was sent. In case the Derived Session should be created for the second response
//              TODO add virtual branch
           	    associateResponseWithSipSession(response,proxyBranch);

           	    if (c_logger.isTraceDebugEnabled()) {
					c_logger
							.traceDebug(this, "processApplicationRespone",
									getMyInfo() + "Cancel all the branches and forward the response upstream");
				}				
				// Continue to send the response upstream
				continueSending = true;
			}

			else {
//        		The response that in sending doesn't refer to the original request
//        		e.g. 200OK response on received PRACK request when original request
//        		that was proxied  was INVITE.
				continueSending = false;
			}
		}

		//if we continue sending we cannot call branch completed, 
		//because this will cause an unecessary call to message.send() again
		if (continueSending == false) {
			branchCompleted(proxyBranch, response);
		}

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "processApplicationResponse", "Exit code: " + continueSending);
		}
		return continueSending;
	}

	/**
	 * Send a response generated by the application itself as opposed to a
	 * response received via one of the branches created by the proxy.
     * @param response
     */
    private void sendAppResponseUpstream(SipServletResponse response) 
    {
        if (c_logger.isTraceEntryExitEnabled()) {
            Object[] params = { new Integer(response.getStatus()), 
                    			response.getReasonPhrase() };
            c_logger.traceEntry(this, "sendAppResponseUpstream", params);
        }
        
        try {
        	if((response.getStatus() >= 200 && response.getStatus() < 300) ||
        			response.getStatus() >= 400){
	    		if (c_logger.isTraceDebugEnabled())
	            {
	                c_logger.traceDebug(this, "sendResponseUpstream", 
	                			"This response is final response on original request." );
	            }	
	    	_originalReq.setIsCommited(true);
	    	}
            response.send();
        }
        catch (IOException e) 
        {
            logException(e);
        } 
    }

    
    /**Retransmission of 2xx response will get here directly from the stack
     * level as they do not a transaction associated with them any more. We 
     * need to associate the response with the request of that branch, pass 
     * through the application so it will have a chance to modify it in and then 
     * pass it up stream to the caller.  
     * @param servletResponse
     */
    public void handleStrayInvite2xx(Response response, SipProvider provider, TransactionUserWrapper newTuw)
    {
    	if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "handleStrayInvite2xx", 
					new Object[]{response, provider, newTuw});
		}

		try {

			
			boolean foundMatchingBranch = false;

			ViaHeader via = (ViaHeader) response.getHeader(ViaHeader.name, true);
			String branch = via.getBranch();

			ProxyBranchImpl proxyBranch = null;

			for (int i = 0; i < _proxyBranches.size() && !foundMatchingBranch; i++) {

				proxyBranch = _proxyBranches.get(i);

				if (proxyBranch.isRetransmission()) {

					if (proxyBranch.getBranchId().equals(branch)) {
						foundMatchingBranch = true;
	                }
	                else {
						// look in the recurse ProxyBranches related to this
						// ProxyBranch
						proxyBranch = proxyBranch.findRecurseBranch(branch);
						if (proxyBranch != null) {
							foundMatchingBranch = true;
						}
					}
				}else{
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "handle2xxRetransmission", "Retransmission is not allowed for this branch state");
					}
				}
			}

			// Matching was found update with final response..
			if (foundMatchingBranch) {
				// wrong casting - the request class is OutgoingSipServletRequest, not SipBranchRequest
				// SipServletRequestImpl branchRequest = ((SipBranchRequest)proxyBranch.getRequest()).getInternalSipServletRequest();
				SipServletRequestImpl branchRequest = (SipServletRequestImpl) proxyBranch.getRequest();

		    	if( branchRequest == null){
		    		//could only be in case of a virtual branch. Original request will be used.
		    		branchRequest = ((SipServletRequestImpl)proxyBranch.getOriginalRequest());
		    	}
		    	TransactionUserWrapper origTU = branchRequest.getTransactionUser();
		    	if (origTU == null || origTU.isInvalidating()) {
		    		if (c_logger.isTraceDebugEnabled()) {
		                c_logger.traceDebug(
		                    this,
		                    "handleStrayInvite2xx",
		                    "return because got stray response on invalidating transaction: " + branchRequest.getCallId());
						}
						return;
		    	}
            	SipServletResponseImpl servletResponse = 
                    new IncomingSipServletResponse(response, -1, provider);
               
            	servletResponse.setRequest((SipServletRequestImpl) proxyBranch.getRequestForInternalUse());
            	// This is Proxy - incoming response is not committed.
                servletResponse.setIsCommited(false);
        		processResponse(proxyBranch, servletResponse);
        	}        
            else {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "handle2xxRetransmission", 
                        getMyInfo() + "Error, Failed to find a matching branch");
				}
			}

		}

		catch (HeaderParseException e) {
			logException(e);
		}

		catch (IllegalArgumentException e) {
			logException(e);
		}
		
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "handleStrayInvite2xx");
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.BranchManager#allBranchesCompleted()
	 * 
	 * StatfullPrxy now should select the best response and forward it to the
	 * application.
	 */
	public void allBranchesCompleted() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "allBranchesCompleted");
		}
		SipServletResponse response = _bestResponse.getBestResponse();

    	ProxyBranchImpl pb = _bestResponse.getProxyBranch();
    	if (pb == null) {
			// this is probably an error response.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "allBranchesCompleted",
						"No branch selected yet for best response");
			}
			TransactionUserWrapper origTu = ((SipServletRequestImpl) getOriginalRequest())
						.getTransactionUser();
			if (origTu.getRemoteTag_2()!= null) {
				TransactionUserWrapper derived = 
					origTu.createDerivedTU(((SipServletResponseImpl) response).getResponse(),
								" StatefullProxy - response with different tag received");
				((SipServletResponseImpl) response).setTransactionUser(derived);
			} 
			else {
				((SipServletResponseImpl) response).setTransactionUser(origTu);
			}
		} 
    	else {
    		associateResponseWithSipSession((SipServletResponseImpl)response, pb);
    	}
		// only if all branches has completed
    	((SipServletResponseImpl)response).getTransactionUser().setProxyReceivedFinalResponse(true, response.getStatus());
		forwardResponse(response, (IncomingSipServletRequest)_originalReq, ((SipServletResponseImpl)response).getTransactionUser());
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "allBranchesCompleted");
		}
	}

	/**
	 * @see javax.servlet.sip.Proxy#setProxyTimeout(int)
	 * 
	 * Method which is responsible to reschedule the current timer
	 * @param seconds
	 */
	public void setProxyTimeout(int seconds) {

		if (seconds < 1) {
			throw new IllegalArgumentException(
					"TimeOut proxy interval should be higher than 0");
		}

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setProxyTimeout", getMyInfo() + "seconds = " + seconds);
		}

		_proxyTimeOut = seconds;

		if (!_started) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setProxyTimeout", 
						getMyInfo() + "Will not start ProxyTimer as this proxy wasn't started yet");
			}
			return;
		}

		if (_timer != null) {
			_timer.cancel();
		}

		_timer = new ProxyTimer(this);

		SipContainerComponent.getTimerService().schedule(_timer, false,
				_proxyTimeOut * 1000);
	}

	/**
	 * Notification about timeout according to the m_director.getProxyTimeout()
	 * 
	 */
	public synchronized void proxyTimeout() {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "proxyTimeout", getMyInfo());
		}

		cancelProxyTimer();

		timeoutAllChildBranches(false);
	}

	/**
	 * More than one branches are associated with a proxy when proxyTo(List) or
	 * createProxyBranches(List) is used. This method returns the top level
	 * branches thus created. If recursion is enabled on proxy or on any of its
	 * branches then on receipt of a 3xx class response on that branch, the
	 * branch may recurse into sub-branches. This method returns just the top
	 * level branches started.
	 * 
	 * @return all the the top level branches associated with this proxy
	 */

	public List<ProxyBranch> getProxyBranches() {

		return getAllBranches();
	}

	/**
	 * @see javax.servlet.sip.Proxy#getProxyTimeout()
	 */
	public int getProxyTimeout() {
		return _proxyTimeOut;
	}

	/**
	 * @see javax.servlet.sip.Proxy#getSequentialSearchTimeout()
	 */
	public int getSequentialSearchTimeout()
	{
		return getProxyTimeout();
	}

	/**
	 * @see javax.servlet.sip.Proxy#createProxyBranches(java.util.List)
	 * 
	 * @param targets
	 * @return
	 */

	public synchronized List<ProxyBranch> createProxyBranches(List<? extends URI> targets) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { targets };
			c_logger.traceEntry(this, "createProxyBranches", params);
		}
		if (targets == null || targets.isEmpty()) {
			return Collections.emptyList();
		}

		for (Iterator iter = targets.iterator(); iter.hasNext();) {
			URI target = (URI) iter.next();
			
			createBranch(target, false, this);
		}

		return getAllNewlyCreatedProxyBranches();
	}

	/**
	 * @see javax.servlet.sip.Proxy#startProxy()
	 */
	public void startProxy() throws IllegalStateException {

		_started = true;

		if(_bestResponse.getBestResponse() != null && 
				isFinalResponse(_bestResponse.getBestResponse().getStatus())){
			
			throw new IllegalStateException("Final Response was already forwarded to the application");
		}

		boolean foundAtLeastOneBranch = false;

		// Go over all newly created ProxyBranches which were created by the
		// application and set it's started flag to true;
		for (int i = 0; i < _proxyBranches.size(); i++) {
			ProxyBranchImpl branch = (ProxyBranchImpl) _proxyBranches.get(i);
			if (!branch.isInitial()) {
				branch.setStarted();
				foundAtLeastOneBranch = true;
			}
		}

		if (!foundAtLeastOneBranch) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "startProxy", getMyInfo()
						+ "No Branches were created for that proxy");
			}
			throw new IllegalStateException(
					"No new created ProxyBranches which should be started");
		}

		startSending();
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.BranchManager#getProxy()
	 */
	protected StatefullProxy getStatefullProxy() {
		// Nothing should be done here in SatefullProxy object
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getProxy", getMyInfo() + "Warning!!! Shouldn't get here");
		}
		return null;
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.BranchManager#getRecordRoute()
	 */
	protected boolean getIsRecordRoute() {
		return _isRecordRoute;
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.BranchManager#getIsParallel()
	 */
	protected boolean getIsParallel() {
		return _isParallel;
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.BranchManager#getRequest()
	 */
	SipServletRequest getRequestForInternalUse() {
		return _originalReq;
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.BranchManager#getAddToPathValue()
	 */
	boolean getAddToPathValue() {
		// TODO Auto-generated method stub
		return getAddToPath();
	}

	@Override
	protected boolean proxyBranchExists(URI nextHop) {
		for (ProxyBranchImpl branch : _proxyBranches) {
			if (nextHop.equals(branch.getUri())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the _virtualBranchCreated
	 */
	protected boolean isVirtualBranchExists() {
		return _virtualBranch != null;
	}

	/**
	 * @return the _virtualBranchCreated
	 */
	protected ProxyBranchImpl getVirtualBranch() {
		return _virtualBranch;
	}

	/**
	 * @param branchCreated
	 *            the _virtualBranchCreated to set
	 */
	protected void setVirtualBranch(ProxyBranchImpl virtualBranch) {
		_virtualBranch = virtualBranch;
	}
	
	/**
	 * @see javax.servlet.sip.Proxy#getPathURI()
	 */
	public SipURI getPathURI() throws IllegalStateException {
		
		if (!getAddToPath()) {
			throw new IllegalStateException("addToPath is not enabled");
		}
		//getting the path uri - if its not initialized it will be created by getPathAddress
		return getPathAddress(_originalReq.getTransport());
	}

	public boolean getNoCancel() {
		return _noCancelOfBranchesOnComplete;
	}

	public void setNoCancel(boolean noCancel) {
		_noCancelOfBranchesOnComplete = noCancel;
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.ProxyParent#getParent()
	 */
	public ProxyParent getParent() {
		// this is the root
		return null;
	}
	
	/**
	 * @return
	 */
	public ProxyTimer getTimer() {
		return _timer;
	}
}
