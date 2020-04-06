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
package com.ibm.ws.sip.container.servlets;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import javax.servlet.*;
import javax.servlet.sip.*;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import com.ibm.sip.util.log.*;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.jain.protocol.ip.sip.SipProviderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.proxy.ProxyBranchImpl;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;
import com.ibm.ws.sip.container.router.*;
import com.ibm.ws.sip.container.sessions.SipTransactionUserTable;
import com.ibm.ws.sip.container.transaction.*;
import com.ibm.ws.sip.container.tu.TransactionUserImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
//TODO Liberty replace the fuctionality of this methos, as it's in  our HA component which is not supported in Liberty
//import com.ibm.ws.sip.hamanagment.util.SipClusterUtil;
import com.ibm.ws.sip.stack.util.SipStackUtil;

import jain.protocol.ip.sip.*;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.*;
import jain.protocol.ip.sip.message.Request;

/**
 * @author Amir Perlman, Feb 20, 2003
 * Represents a Sip Servlet Request that was generated locally and is intended
 * to be sent to a remote party. This implementation is different from other
 * requests in not being constructed initially around a Jain Sip Request. 
 * The internal Jain Sip Request is constructed only prior to actually passing
 * the message to the stack.  
 * 
 */
public class OutgoingSipServletRequest extends SipServletRequestImpl 
{
    /** Serialization UID (do not change) */
    
    static final long serialVersionUID = -756987237986445115L;

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger =
        Log.get(OutgoingSipServletRequest.class);

    /**
     * Indicates whether the Contact header for this request has been explicitly
     * removed. In case it has been remove it will not be added defaulty during
     * sending. 
     */
    private transient boolean m_contactRemovedByApp = false;

    /**
     * The listener for the transaction associated with this request. 
     */
    private transient ClientTransactionListener m_clientTransactionListener; 
    
	/**
	 *  Indicates whether it's the first outgoing request 
	 */
	private static transient boolean s_firstRequestSent = false;
    
    /**
     * Proxy object that used this request on an outbound branch. 
     * See getProxy(boolean).  
     */
    private transient Proxy m_proxy; 
    
    /**
     * Indicates whether the request is a subsequent request on an existing 
     * dialog. 
     */
    private transient boolean m_isSubsequentRequest;
    
    /**
     * The IBM-Destination header for INVITE requests.
     * We keep this when sending out the INVITE, and copy it into outgoing
     * CANCEL requests.
     * may be null 
     */ 
    private transient Header m_destinationHeader = null;
    
    /**
     * The IBM-PO header for INVITE requests.
     * We keep this when sending out the INVITE, and copy it into outgoing
     * CANCEL requests.
     * may be null
     */ 
    private transient Header m_preferredOutbound = null;
    
    /**
     * Outgoing request can be marked for error response
     * due composition check process.
     */
    private boolean markedForErrorResponse = false;
    
    
    private String appInvokedName = null;
    
    

    /**
     * Default constructor
     */
    public OutgoingSipServletRequest()
    {
    }
    
    /**
     * Constructs a new Outgoing Sip Servlet Request. 
     * @param sipSession The Sip Session that originated this request through 
     * the <link> javax.servlet.sip.SipSession#createRequest(String)</link> call.  
     * @param method The Request's Sip Method. 
     * @param from From Address. 
     * @param to To Address. 
     * @param initial true if initial or non-dialog request, false if in-dialog. 
     * 
     * @see javax.servlet.sip.SipSession#createRequest(String)
     */
    public OutgoingSipServletRequest(
        TransactionUserWrapper transcactionUser,
        String method,
        Address from,
        Address to,
        boolean initial)
    {
        super();
        String callId = transcactionUser.getCallId();
        createRequest(method, from, to, callId, transcactionUser.getSipProvider(), initial);
        
        long cSeq = transcactionUser.getNextCSeqNumber();
        addCSeqHeader(cSeq, method);
        
        setTransactionUser(transcactionUser);
        setIsCommited(false);
        
        //Set the from tag if it has not been set yet
        testAndSetFromTag();
        
        SipServletRequestImpl origReq =
            (SipServletRequestImpl) transcactionUser
                .getSipServletRequest();
        
        if (origReq != null) {
        	this.setExternalRoute(origReq.isExternalRoute());

        	// Update the request's exclude list according to the origianl request. 
	        //relevant to initial requsts only
	        addToExcludeSipletsList(origReq);

	        // copy application composition state, so the new request doesn't
	    	// loop back to the same servlet that created it.
	        // only relevant to initial requests
	        Serializable stateInfo = origReq.getStateInfo();
	    	String nextApp = origReq.getNextApplication();
	    	SipApplicationRoutingDirective appDir = origReq.getDirective();
	    	SipApplicationRoutingRegion routingRegion = origReq.getRegion();
	    	setStateInfo(stateInfo);
	    	setNextApplication(nextApp);
	    	setDirective(appDir);
	    	setRoutingRegion(routingRegion);
        }
    }
    
    /**
	 * Overrides the method in SipServletMessage to add the ability to get 
	 * Session by the To tag that is usefull in the Derived Session state 
	 * @see javax.servlet.sip.SipServletMessage#getSession(boolean)
	 * @param create
	 * @return
	 */
    public SipSession getProxySession(boolean create) {
		return getTransactionUser().getSipSession(create);
	}
    
    /**
     * Constructs a new Outgoing Sip Servlet Request. 
     * the <link> javax.servlet.sip.SipSession#createRequest(String)</link> call.  
     * @param method The Request's Sip Method. 
     * @param from From Address. 
     * @param to To Address. 
     * @param provider The Sip Provider that will be used for generating
     * @param origReq Optional origianl request from which the list of 
     * excluded siplets will be copied.  
     * @param provider The Sip Provider that will be used for generating
     * @param appSession The application session associated with this request. 
     * If null value is passed then a new application session will be created
     * and associated with the SIP Session of this request. 
     * @see javax.servlet.sip.SipSession#createRequest(String)
     */
    public OutgoingSipServletRequest(
        String method,
        Address from,
        Address to,
        String callId,
        SipProvider sipProvider,
        SipApplicationSessionImpl appSession,
        SipServletRequestImpl origReq)
    {
        super();
        createRequest(method, from, to, callId, sipProvider, true);
        //Assign a new SIP Session to this request. 
        TransactionUserWrapper tUser;
        SipTransactionUserTable transactionUsersTable = SipTransactionUserTable.getInstance();
        
        //The new session will be using the same app session. 
        tUser = transactionUsersTable.createTransactionUserWrapper(this, false, appSession, false);
        init(method, origReq, tUser, false);
    
        //Set the from tag if it has not been set yet
        testAndSetFromTag(); 
    }
    
    /**
     * Constructs a new Outgoing Sip Servlet Request for internal use (e.g. 
     * proxying).   
     * the <link> javax.servlet.sip.SipSession#createRequest(String)</link> call.  
     * @param method The Request's Sip Method. 
     * @param from From Address. 
     * @param to To Address. 
     * @param provider The Sip Provider that will be used for generating
     * @param origReq Optional origianl request from which the list of 
     * excluded siplets will be copied.  
     * @param provider The Sip Provider that will be used for generating
     * @see javax.servlet.sip.SipSession#createRequest(String)
     */
    public OutgoingSipServletRequest(
        String method,
        Address from,
        Address to,
        String callId,
        SipProvider sipProvider,
        SipServletRequestImpl origReq,
        boolean forProxy)
    {
        super();
        createRequest(method, from, to, callId, sipProvider, true);
        //init(method, origReq, null);
        init(method, origReq, origReq.getTransactionUser(), forProxy);
    }
    
    /**
     * Creating the JAIN request, setting provider and initial state
     * @param method
     * @param from
     * @param to
     * @param callId
     * @param sipProvider
     * @param initial
     */
    private void createRequest( String method,
            Address from,
            Address to,
            String callId,
            SipProvider sipProvider,
            boolean initial){
    	 createJainRequest(method, from, to, callId);
         setSipProvider(sipProvider);
     
         // The request which is not subequent request 
         // should be inital.
         setIsInital(initial);
    }
    
    /**
     * Initialize the OutgoingSipServletRequest
     * @param method
     * @param origReq
     * @param tUser
     */
    private void init(String method,
            SipServletRequestImpl origReq,
            TransactionUserWrapper tUser,
            boolean forProxy){
    	
        setIsCommited(false);
        
        long cSeq = 1;
     
        if (origReq != null){
	        //Use the same cSeq number as the original request. Important
	        //when proxying request to send with the same cSeq and not start
	        //with 1 every time
	        CSeqHeader cSeqH = origReq.getJainSipMessage().getCSeqHeader();
	        if (null != cSeqH)
	        {
	            cSeq = cSeqH.getSequenceNumber();
	            if(tUser != null){
	            	tUser.setcSeq(cSeq);
	            }
	        }
	        
	        this.setExternalRoute(origReq.isExternalRoute());
	
	        //If the request is generated/derived from another request then 
	        //we copy the exclude Siplets from this request. 
	        addToExcludeSipletsList(origReq);
	        _initialPoppedRoute = origReq.getInitialPoppedRoute();
        }
    
        if(!forProxy){
        	cSeq = tUser.getNextCSeqNumber();//A proxy will not increment the outgoing cSeq
        }
        addCSeqHeader(cSeq, method);
        setTransactionUser(tUser);
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getSession(boolean)
     */
    public SipSession getSession(boolean create){
    	if (!isLiveMessage("getSession")){
    		return null;
    	}

    	//check if this is a proxy request(a request that is linked to a proxyBranch) that does not have a
    	//transaction user linked to it yet. the transaction user is linked when the response for this request 
    	//is received 
    	if (m_transactionUser == null && m_proxy != null){
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "getSession", 
    			"getting the session from the proxy original request session");
    		}
    		return m_proxy.getOriginalRequest().getSession(create);
    	}

    	if(m_transactionUser.isProxying()){
    		return getProxySession(create);
    	}

    	return m_transactionUser.getSipSession(create);
    }
    
    /**
     * @see javax.servlet.sip.SipServletMessage#getApplicationSession(boolean)
     */
    public SipApplicationSession getApplicationSession(boolean create){
    	if (!isLiveMessage("getApplicationSession")){
    		return null;
    	}
    	
    	//check if this is a proxy request(a request that is linked to a proxyBranch) that does not have a
    	//transaction user linked to it yet. the transaction user is linked when the response for this request 
    	//is received 
    	if (m_transactionUser == null && m_proxy != null){
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "getSession", 
    			"getting the app session from the proxy original request app session");
    		}
    		return m_proxy.getOriginalRequest().getApplicationSession(create);
    	}
    	
    	if (m_transactionUser == null){
    		return null;
    	}

    	return m_transactionUser.getApplicationSession(create);
    }
    
    /**
     * Override method from the SipServletRequest
     * if this outgoing request is created by the proxy - will extract the virtual host from the 
     * original request.
     */
     public String getVirtualHost() {
    	String virtualHost = null;
    	
//    	If we are in the proxy mode - extract the virtual host from the original request
    	if(m_proxy != null){
    		virtualHost = ((SipServletRequestImpl)m_proxy.getOriginalRequest()).getVirtualHost();
    	}
    	else{
//    		Otherwise get the virual host in the original way (code iln the SipServletRequestImpl)
    		virtualHost = super.getVirtualHost();
    	}
    	
    	return virtualHost;
    }
    
    /**
     * Generate the internal Jain Request wrapped by this object. 
     * @param method
     * @param from
     * @param to
     * @param callId
     */
    private void createJainRequest(String method, Address from, 
            					   Address to, String callId)
    {
        RequestImpl req = new RequestImpl();
        try
        {
            req.setMethod(method);
            
            FromHeader fromH = createFromHeader((AddressImpl)from);
            req.setFromHeader(fromH);
            
            ToHeader toH = createToHeader((AddressImpl)to);
            req.setToHeader(toH);
            
            CallIdHeader callIdH = createCallIdHeader(callId);
            req.setCallIdHeader(callIdH);
            
            MaxForwardsHeader  maxForwards  = 
                			getHeadersFactory().createMaxForwardsHeader(70);
            req.setHeader(maxForwards, true);
            
            //bing the Jain Request to this Sip Servlet Request
            setMessage(req);
        }
        catch (IllegalArgumentException e)
        {
            if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", Situation.SITUATION_CREATE, 
                        		null, e);
            }
        }
        catch (SipParseException e)
        {
            if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", Situation.SITUATION_CREATE, 
                        		null, e);
            }
        }
    }
    
    /**
     * Add a CSeq header to the request. 
     * @param seq
     * @param method
     * @return
     * @throws SipParseException
     * @throws IllegalArgumentException
     */
    private void addCSeqHeader(long cSeq, String method) 
    {
        CSeqHeader cSeqHeader;
        try
        {
            cSeqHeader = getHeadersFactory().createCSeqHeader(cSeq, method);
            getRequest().setCSeqHeader(cSeqHeader);
        }
        catch (IllegalArgumentException e)
        {
            if(c_logger.isErrorEnabled())
            {
                
                c_logger.error("error.exception", Situation.SITUATION_CREATE, 
                        		null, e);
            }
        }
        catch (SipParseException e)
        {
            if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", Situation.SITUATION_CREATE, 
                        		null, e);
            }
        }
        
    }

    /**
     * Updates the list of excluded Siplets with the list of Siplest from
     * the given request. Includes both the origianl request exclude list and
     * it associated handler siplet. 
     * @param origReq
     */
    protected void addToExcludeSipletsList(SipServletRequestImpl origReq)
    {
        addToExcludeSipletsList(origReq.getExcludedAppsList());

        TransactionUserWrapper tUser =
            	origReq.getTransactionUser();

        //Also add the handler of original request to exclude list of this request
        SipServletDesc siplet = tUser.getSipServletDesc();
        if(siplet != null)
        {
            addToExcludeAppsList(siplet.getSipApp());
        }
        
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getMethod()
     */
    public String getMethod()
    {
        String method = null;
        
        try
        {
            method = getRequest().getMethod();
        }
        catch (SipParseException e)
        {
            if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", Situation.SITUATION_CREATE, 
                        		null, e);
            }
        }
        
        return method;
    }

    /**
     * Helper method that is updating the request URI according to the Route set
     * of the request. If next Route header is strict route (not contian "lr")
     * the Request URI should be replaced with the top Route and the requestURI
     * should be saved as a bottom Route.
     * At the end of this method the requestURI will be contain the  correct 
     * URI for this request.
     * @throws NoSuchElementException 
     * @throws HeaderParseException 
     *
     */
    public void setRequestUriAccordingToRouteSet(Address remoteTarget){
    	Request jainReq = getRequest();
    	HeaderIterator routeSet = jainReq.getRouteHeaders();
    	boolean reqUriWasSet = false;
    	if(routeSet != null && routeSet.hasNext()){
    		try {    			
				RouteHeader topRoute = (RouteHeader) routeSet.next();
				if(!((SipURL)topRoute.getNameAddress().getAddress()).hasParameter("lr")){
					
					// Top Route is a strict route. It URI should replace the 
					// requestURI.
					jainReq.setRequestURI(topRoute.getNameAddress().getAddress());
					
					if (routeSet.hasNext()) {
						// Mark the next Route header with the STRICT_ROUTING_PARAM.
						// This route will be the first Route after the previous
						// will be removed from the request as it moved to reqUri
						topRoute = (RouteHeader) routeSet.next();
						((SipURL)topRoute.getNameAddress().getAddress()).setParameter(
                				SipUtil.STRICT_ROUTING_PARAM, "");
					}
					else{
						// If there was only one Route in the request the remote 
						// target will be moved to the Route set because of the 
						// Strict Routing mechanism.
						remoteTarget.setParameter(SipUtil.STRICT_ROUTING_PARAM, "");
					}
                    
					jainReq.removeHeader(RouteHeader.name, true);
					addAddressHeader(RouteHeader.name,remoteTarget,false);
					
					reqUriWasSet = true;
				}
			}
    		catch (SipParseException e) {
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer buff = new StringBuffer();
					buff.append("Failed to parse the top Route in request ");
					buff.append(jainReq);
					c_logger.traceDebug(this,
									"setRequestUriAccordingToRouteSet", buff
											.toString());
				}
			}             
    	}
    	if(!reqUriWasSet ){
    		setRequestURI(remoteTarget.getURI());
    	}
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#send()
     */
    public void send() throws IOException {
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "send");
		}
    	
		//if its the first request we need to initialize the SIP application router
		synchronized (OutgoingSipServletRequest.class) {
			if(!s_firstRequestSent) {
				SipContainerComponent.activateSipApplicationRouter();
				s_firstRequestSent = true;
			}
		}
		
    	if (!isLiveMessage("send"))
    		return;
        
    	if(isCommitted()) {
	        throw new IllegalStateException(
	           "Can not modify committed message");
	    }
    	
        isSessionIsValid();
        resetContentLength(); 
        
        //By default when called from a user application the listener for 
        //the transaction will be the session associated with this request.
        if(null == m_clientTransactionListener) {
            m_clientTransactionListener = getTransactionUser();
        }
        
        // JSR 289, section 4.1.3
        // In case the application modifies the Contact as specified above but
        // decides to proxy the request subsequently, 
        // the containers MUST ignore any modification made to the Contact header. 
        if(getTransactionUser() != null && getTransactionUser().isProxying() && _origContact!=null){
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "send", "");
			}
        	
        	super.setAddressHeader(ContactHeader.name,_origContact);
        }
        
        send(m_clientTransactionListener);
        
        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "send");
		}
    }

    /**
     * Send the specified request and specify the listener for transaction 
     * events. 
     * @see javax.servlet.sip.SipServletMessage#send()
     */
    public synchronized void send(ClientTransactionListener listener)
        throws IOException
    {
    	 if (c_logger.isTraceEntryExitEnabled()) {
 			c_logger.traceEntry(this, "send", listener);
 		 }
    	 
    	if (!isLiveMessage("send"))
    		return;
        
        if (isCommitted())
        {
            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(
                    this,
                    "send",
                    "Send Failed, Request already Commited");
            }

            return;
        }

        //Create a client transaction associated with this request.  
        ClientTransaction transaction =
            TransactionTable.getInstance().createClientTransaction(this);
        transaction.setClientTransactionListener(listener);
        setTransaction(transaction);

        //Call on the transaction to do the actual sending.  
        boolean wasSent = transaction.sendRequest();
        
        //In some case the request might have not been actually sent, e.g. 
        //CANCEL when a provisional response has been not received yet. Verify that
        //it was sent before changing the state. 
        if(wasSent)
        {
            //Changes the state of the Message to commited so it no longer can 
            //be modified after it is sent.
            setIsCommited(true);
            
            //clean up the client transaction listener - no longer needed
            m_clientTransactionListener = null;
        }
        
        if (c_logger.isTraceEntryExitEnabled()) {
 			c_logger.traceExit(this, "send");
 		 }
    }
    
    public long sendImpl() throws IOException
    {
    	return 1;
    }

    
    /**
     * Method which is updates the outgoing request with new sipProvider
     * used in Via header according to the destination where the request 
     * is actually send. (In NAPTR environment only).
     *
     */
    public void updateParamAccordingToDestination()throws IOException{
    	try {
			NameAddressHeader destination = (NameAddressHeader) getRequest()
					.getHeader(SipUtil.DESTINATION_URI, true);
			if (destination != null) {
				String transport = null;
				
				if (destination.getNameAddress().getAddress().getScheme().equalsIgnoreCase(SIPS)) {
					transport = TLS;
				}

				// In this case we didn't find transport till now because
				// there was no Route header in the request or sceme in the requestUri
				// wasn't SIPS. Now we will try to rescue it from requestUri.
				if (transport == null) {
					transport = ((SipURL) destination.getNameAddress().getAddress()).getTransport();
				}	
				
				if (selectProvider(transport)) {
					//Only when provider changed
					getRequest().removeHeader("Via", true);
					addViaHeader();
				}
			}
			
		}

		catch (SipParseException e) {
			throw new IOException(e.getMessage());
		}
    }
    
    /**
     * @return URI to be used in the Contact header field. 
     */
    protected SipURI constructContactHeaderURI() {
		if (c_logger.isTraceDebugEnabled()){
            c_logger.traceEntry(this, "constructContactHeaderURI");
        }

		//	if there is a preferred outbound interface that needs to be
		//	reflected in the Contact header.
		TransactionUserWrapper tu = getTransactionUser();
		SipURI sipURI = null;

		if (tu != null && ! tu.isProxying()) {
			if (c_logger.isTraceDebugEnabled()){
	            c_logger.traceDebug(this, "constructContactHeaderURI",
	                "TU Id [" + tu.getId() + "] ,is proxy mode [" + tu.isProxying() +"]");
	        }
			SipProxyInfo proxyInfo = SipProxyInfo.getInstance();
			
	    	if (getSipProvider() == null){
				try {
					SipURL target = (SipURL) SipStackUtil.createTargetFromMessage(getRequest()).clone();
		    		// set the SIP provider according to the destination 
		    		setProvider(target);
				} catch (IllegalArgumentException e) {
		            if(c_logger.isErrorEnabled()) {
		                c_logger.error("error.exception", Situation.SITUATION_CREATE, 
		                        		null, e);
		            }
				} catch (IOException e) {
		            if(c_logger.isErrorEnabled()) {
		                c_logger.error("error.exception", Situation.SITUATION_CREATE, 
		                        		null, e);
		            }
				}
	    	}
	    	
	    	if (getSipProvider() != null){

        		ListeningPoint lPoint = getSipProvider().getListeningPoint();
				String transport = lPoint.getTransport();
				
				int index = tu.getPreferedOutboundIface(transport);				
				if (index == SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
					// if the preferred outbound was not set
					// use the provider's listening point as the Contact Address
					sipURI = proxyInfo.getDefaultOutboundIface(transport);
				}
				else {
					sipURI = proxyInfo.getOutboundInterface(index, transport);
				}
	    	}
	    	else {
				if (c_logger.isTraceDebugEnabled()){
		            c_logger.traceDebug(this, "constructContactHeaderURI",
		                "Can't set Contact header, provider is null");
		        }
	    	}
		}
		
		if (c_logger.isTraceDebugEnabled()){
            c_logger.traceExit(this, "constructContactHeaderURI", sipURI);
        }
		return sipURI;
    }
    
    /**
     * This method MAY be overrided by all implemented objects. Each object
     * can behave differently and decide which parameters are need to be added.
     * 
     * Actual implementation for sending the request through the Jain Sip Stack. 
     * A cleaner design would have been to put this code within the Client
     * Transaction object but that would require exposing all internal state 
     * variable. 
     * This method should be called from the Client Tranasacion only.
     * @param target request destination  
     */
    public void setupParametersBeforeSent(SipURL target, boolean isLoopBack) throws IOException
    {
    	if (!isLiveMessage("sendImpl"))
    		return;
        
        try
        {
            Request req = getRequest(); 

			// Outgoing request should use Provider according to the
			// Request target
			setProvider(target);

			// Add a via according to the provider/transport selected.
			addViaHeader();

			//	This code handles adding the correct contact header. 
			//	If there is a preferred outbound interface that needs to be
			//	reflected in the contact header.
			TransactionUserWrapper tu = getTransactionUser();
			SipURI sipURI = null;
			
			//if this is a proxy application there is no need to update the contact header 
			if (tu != null && ! tu.isProxying())
			{
/*				if (c_logger.isTraceDebugEnabled()){
		            c_logger.traceDebug(this, "setupParametersBeforeSent",
		                "TU is not null. TuId=" + tu.getId() + " ,isProxying=" + tu.isProxying());
		        }
				SipProxyInfo proxyInfo = SipProxyInfo.getInstance();
				ListeningPoint lPoint = getSipProvider().getListeningPoint();
				String transport = lPoint.getTransport();
				
				int index = tu.getPreferedOutboundIface(transport);				
				if (index == SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
					// the sipURI is null if the preferred outbound was not set
					// in this case, we use the provider's listening point as the Contact Address
					sipURI = proxyInfo.getDefaultOutboundIface(transport);
				}
				else {
					sipURI = proxyInfo.getOutboundInterface(index, transport);
				}
				
*/				sipURI = constructContactHeaderURI();
				setupContactHeaderBeforeSent(sipURI);
			}
			else
			{
				if (c_logger.isTraceDebugEnabled()){
					if (tu == null){
						c_logger.traceDebug(this,  "setupParametersBeforeSent", "TU is null. Can't access prefered outbound interface. Using default for contact header.");
					}else{
						c_logger.traceDebug(this, "setupParametersBeforeSent", "TU is for proxy, no need to change the contact header");
					}
				}
			}
				
        	
			if (isLoopBack) {
				String ibmClientAddr = createIBMClientAddrHeaderForLoopbackMessages();
				if (ibmClientAddr != null) {
					addHeader(SipUtil.IBM_CLIENT_ADDRESS, ibmClientAddr);	
				}				
		        if (c_logger.isTraceDebugEnabled())
		        {
		            c_logger.traceDebug(
		                this,
		                "setupParametersBeforeSent",
		                "Loopback message, adding header " + SipUtil.IBM_CLIENT_ADDRESS + " = " + ibmClientAddr);
		        }				
				
				// Send the request in loop back mode
				((MessageImpl)req).setLoopback(true);
			}
			
	        //	Here we tack on the PO header to tell the proxy which interface to send on.
	        //	We have to do this here to ensure the application has had time to set the interface on the session.
			//	Note that tu is null when an application is proxying messages.
			//
			// jlawwill (PI62617) -  We must set the preferred outbound header properly.   Currently, for a 
			//   proxied connection, we aren't checking to see if the outbound interface has been set.
			//   The following code checks to see if a proxy branch exists.   If so, it checks to see
			//   what direction the message is flowing, and then uses the correct outbound interface
			//   for that particular connection.
			//
			//  One thing to note, the BranchManager also sets this outbound interface in some instances.
			//    At some point, we should refactor this code so that it sets the header consistently.
			//
			String	ibmPOHeader = getHeader(SipProxyInfo.PEREFERED_OUTBOUND_HDR_NAME);
			boolean outboundEnable = PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.ENABLE_SET_OUTBOUND_INTERFACE);
	        if (c_logger.isTraceDebugEnabled())
	        	c_logger.traceDebug(this, "setupParametersBeforeSent", "current IBM-PO =  " + ibmPOHeader);
	        
	        if (tu != null && !outboundEnable ){
	        	SipProxyInfo.getInstance().addPreferedOutboundHeader(this, tu.getPreferedOutboundIface(getTransport()));
	        }

	        else if (ibmPOHeader == null) {
	        	int ibmPOIndex = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
	        	if (tu != null) {
	        		//  Compare the From headers of the original message and this current message.    
	        		//  If they are the same, use the "outbound interface" associated w/ the proxy branch.
	        		boolean isSenderOriginator = true;
	        		
	        		ProxyBranchImpl proxyBranch = tu.getBranch();
	        		if (proxyBranch != null) {
	        			isSenderOriginator = isSameSender(tu.getBranch().getOriginalRequest(), req);
	        			if (isSenderOriginator) {
        					ibmPOIndex = proxyBranch.getPreferedOutboundIface(getTransport());
        					if (c_logger.isTraceDebugEnabled())
        						c_logger.traceDebug(this, "setupParametersBeforeSent", "using proxy IBM-PO =  " + ibmPOIndex);
        				}
	        				
	        		}

	        		//  If the ibmPOIndex hasn't been set already, use the "outbound interface" of the tu.
	        		if (ibmPOIndex == SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
	        			if (isSenderOriginator) {
		        			ibmPOIndex = tu.getPreferedOutboundIface(getTransport());
	        			} else {
		        			ibmPOIndex = tu.getOriginatorPreferedOutboundIface(getTransport());
	        			}
	        			if (c_logger.isTraceDebugEnabled()) {
	        				c_logger.traceDebug(this, "setupParametersBeforeSent", "using tu IBM-PO =  " + ibmPOIndex);
	        			}
	        		}
	        	}

	        	// Set the ibmPOIndex in the message. 
	        	if (ibmPOIndex != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
	        		if (c_logger.isTraceDebugEnabled()) {
	        			c_logger.traceDebug(this, "setupParametersBeforeSent", "setting IBM-PO to =  " + ibmPOIndex);
	        		}
	        		SipProxyInfo.getInstance().addPreferedOutboundHeader(this, ibmPOIndex);
	        	}
	        }
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { this };
                c_logger.error(
                    "error.send.request",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
            logExceptionToSessionLog(SipSessionSeqLog.ERROR_SEND_REQ, e);
            throw (new IOException(e.getMessage()));
        }
    }
    
    
    private boolean isSameSender(SipServletRequest request1, Request request2) {
		boolean returnVal = false;
		
    	String fromHeaderTag1 = request1.getFrom().getParameter(AddressImpl.TAG);
		String fromHeaderTag2 = request2.getFromHeader().getTag();
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "isSameSender", "originalFromHeaderTag = " + fromHeaderTag1);
			c_logger.traceDebug(this, "isSameSender", "fromHeaderTag = " + fromHeaderTag2);
		}

		if(fromHeaderTag1 !=null && fromHeaderTag2 != null &&
				fromHeaderTag1.compareTo(fromHeaderTag2) == 0) {
			returnVal = true;
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "isSameSender", "" + returnVal);
		}
		return returnVal;
    }
    
    /*
     * (non-Javadoc)
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#getTransport()
     */
    public String getTransport(){
    	String transport = getTransportInt();
    	
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(OutgoingSipServletRequest.class.getName(),
					"getTransport", transport);
		}
    	return transport;
    }
    
    /**
     * Helper method to update ContactHeader according to outbound interface.
     * 
     * @param sipURI
     * @throws IllegalArgumentException
     * @throws SipParseException
     */
    private void setupContactHeaderBeforeSent(SipURI sipURI) throws IllegalArgumentException, SipParseException {
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { sipURI };
			c_logger.traceEntry(OutgoingSipServletRequest.class.getName(),
					"setupContactHeaderBeforeSent", params);
		}
    	
    	if(checkIsSystemContactHeader()){
	    	if (sipURI != null)
	    	{
		        if (c_logger.isTraceDebugEnabled())
		        {
		            c_logger.traceDebug(
		                this,
		                "setupContactHeaderBeforeSent",
		                "Using preferred outbound interface for contact header.");
		        }
	
		        //	Set the contact header to match the preferedOutboundInterface
	    		addContactHeader(sipURI);
	    	}
	    	else
	    	{
	        	//	Passing in null here will cause the method to determine the correct contact header to use.
				addContactHeader(null);
	    	}
    	} else {
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setupContactHeaderBeforeSent", "the Contact is not a System header, no update.");
			}
    	}
	}

	/**
     * Actually sends the request to the UAS through the SIPProvider. 
     * @param transactionId
     * @param req
     * @return
     * @throws IOException
     */
    public long sendImpl(long transactionId) throws IOException{
    	
    	Request req = getRequest(); 
    	
    	// at this stage, the request has an IBM-Destination header.
    	// if this is an INVITE, we want to keep the IBM-Destination header,
    	// because that's exactly where we want to send future CANCELs.
    	try {
			if (req.getMethod().equals(Request.INVITE)) {
				m_destinationHeader = req.getHeader(SipUtil.DESTINATION_URI, true);
				m_preferredOutbound = req.getHeader(SipProxyInfo.PEREFERED_OUTBOUND_HDR_NAME, true);
			}
		}
		catch (HeaderParseException headerParseException) {
			throw new IOException("bad header [" + SipUtil.DESTINATION_URI + ']');
		}
		catch (SipParseException sipParseException) {
			throw new IOException("unknown request method");
		}
    	
    	setTransactionId(transactionId);
        SipProviderImpl provider = (SipProviderImpl)getSipProvider();
        try {
        	if (c_logger.isTraceDebugEnabled())
        	{
				StringBuffer b = new StringBuffer(64);
				b.append("Sent OutGoing Request, Transaction Id:");
				b.append(transactionId);
				b.append("\r\n");
				b.append(req);
				c_logger.traceDebug(this, "sendImpl", b.toString());
			}
        	provider.sendRequest(req, transactionId);
        	return transactionId;
		    	
		} 
        catch (SipException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { this };
                c_logger.error(
                    "error.send.request",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
            
            logExceptionToSessionLog(SipSessionSeqLog.ERROR_SEND_REQ, e);
            throw (new IOException(e.getMessage()));
        }
    }

    /**
     * Determine if the message should be sent in loopback mode or to the 
     * network. 
     * @return
     * @throws SipParseException
     * @throws IllegalArgumentException
     */
    public boolean checkIsLoopback() throws IllegalArgumentException, 
    										 SipParseException {
        if(!c_appCompositionEnabled){
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "checkIsLoopback", "Application composition is not enabled: loopback = false");
            }
            return false;
        }
        
        boolean isLoopback = false;
 
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, 
            		"checkIsLoopback", "m_isSubsequentRequest: " + m_isSubsequentRequest + " isExternalRoute: " + isExternalRoute());
        }
        
        //Check if the request need to be sent out of the container to 
        //the network or passed to another application as part of application
        //composition. Note that subsequent requests in a dialog should not 
        //go through rule matching, another case is routing before application
        //was selected, in that case there is no another application check the 
        //request is proxied to the external route URI.
        if (!m_isSubsequentRequest && !isExternalRoute()){
            
        	if(checkForApplicationComposition()){
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "checkIsLoopback", "application composition is true");
                }

                //Send in loopback mode. 
        	    isLoopback = true;
        	}
        	
    		// In that case the request is still take part
        	// in application selection process and is routed
        	// appropriately.
        	if (isLoopback || !this.isExternalRoute()){//TODO this will always be true!!!!

        		// JSR 289 adding state for application router 
        	    addCompositionHeader();
        	}
        }else{
            TransactionUserWrapper tUser = 
                SipTransactionUserTable.getInstance().getTransactionUserForOutboundRequest(this);
            if(tUser != null){
            	
            	//if the outbound session (TU) exists, verify loopback is enabled only if the request VH 
            	//and the outbound session VH are equals 
            	String outboundReqVH = tUser.getSipServletDesc().getSipApp().getVirtualHostName();
            	String requestVH = getVirtualHost();
            	
            	if (outboundReqVH != null && requestVH != null && !outboundReqVH.equals(requestVH)){
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "checkIsLoopback", 
                        			"outbound request VH and current request VH are different, loopback is false");
                    }
            		return false;
            	}
            	
                //TODO: Amir May 24, To improve performance and avoid going 
                //through the session lookup it will be best to attach the 
                //session to the request to avoid the lookup when we get it 
                //back. Note that it will be the same internal Request object
                //but wrapped with by an IncomingSipServletRequest
                isLoopback = true;
            }
        }
        
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "checkIsLoopback", "Is loopback: " + isLoopback);
        }
        return isLoopback; 

    }

    /**
     * Add a via with the local host/port of the provider associated with this
     * message. 
     * @throws SipParseException
     * @throws IllegalArgumentException
     */
    private void addViaHeader() throws IllegalArgumentException, SipParseException
    {
        ListeningPoint lPoint = getSipProvider().getListeningPoint();
        String transport = ((ListeningPointImpl) lPoint).isSecure() ? 
                			 TLS : lPoint.getTransport();
    
        ViaHeader viaHeader;
        TransactionUserWrapper tUser = getTransactionUser();
		int index = (null == tUser) ? SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED : tUser.getPreferedOutboundIface(transport);
		if (index < 0) {
			viaHeader = 
					getHeadersFactory().createViaHeader(lPoint.getPort(), lPoint.getSentBy());
			viaHeader.setTransport(transport);
		}
		else {
			SipProxyInfo proxyInfo = SipProxyInfo.getInstance();
			SipURI sipURI = proxyInfo.getOutboundInterface(index, transport);
			viaHeader = 
					getHeadersFactory().createViaHeader(sipURI.getHost(), sipURI.getPort(), transport);
		}
		viaHeader.setRPort(); // RFC 3581-3
		
        // Add the session identifier to the outbound Via header.
        // For INVITE requests, this serves for associating retransmitted
        // 2xx responses with the session, after the transaction has terminated.
        // Under Z, it is also needed for non-INVITE, in case there's a SIP router
        // sitting in the CR, in between the SIP proxy and the container.
        // The container runs in one of the SRs, and all SRs have the same address.
        // The sentBy address in this setup is not unique per container, so when
        // the router receives the response, it cannot tell which container instance
        // owns the transaction just by looking at the sentBy address.
        // The session identifier resolves the ambiguity.
        
        if(null == tUser && m_proxy != null)  {
            //If we got here then this must be an outbound branch for a 
            //proxy operation. Use the session identifier of the original
            //request being proxied
            tUser = 
                ((SipServletRequestImpl)m_proxy.getOriginalRequest()).getTransactionUser();
        }
        if(null != tUser)
        {
            viaHeader.setParameter(TransactionUserImpl.SESSION_RR_PARAM_KEY, tUser.getId());
            createViaBranchBasedOnIncomingRequest(viaHeader, tUser);
        }
        getRequest().addHeader(viaHeader, true);
    }
    
    /**
     * Creates the via branch based on previous incoming request. This is to help in cases of 
     * B2B applications where a retransmission was handled by the application again after failover
     * (since to transactional failover is supported on the stack). If the branch is always based
     * on the incoming request branch, then after failover, although the request will be handled twice
     * by the application, the outgoing message to the UAS will be recognised on its side according
     * to the branch and will not be handled there as new request again.    
     * @param viaHeader
     * @throws IllegalArgumentException 
     * @throws SipParseException 
     */
    private void createViaBranchBasedOnIncomingRequest(ViaHeader viaHeader, TransactionUserWrapper tUser) throws SipParseException, IllegalArgumentException
    {
    	B2buaHelper helper = getB2buaHelper(false, UAMode.UAC);
    	if(helper == null){
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this,"createViaBranchBasedOnIncomingRequest", 
    			"we are not in B2b mode, branch will be created randomly on stack");
    		}
    		return;
    	}
    	SipServletRequestImpl req = (SipServletRequestImpl)helper.getLinkedSipServletRequest(this);
    	
    	if(req == null){
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this,"createViaBranchBasedOnIncomingRequest",
    			"no B2B request came in, branch will be created randomly on stack");
    		}
    		return;
    	}
    	String reqViaBranch = ((ViaHeader)req.getMessage().getHeader(ViaHeader.name, true)).getBranch();
    	if(reqViaBranch == null){
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "createViaBranchBasedOnIncomingRequest",
    			"Incoming message had no VIA branch, branch will be created randomly on stack");
    		}
    		return;
    	}
    	
    	if(tUser == null){
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this,
    							"createViaBranchBasedOnIncomingRequest", "TransactionUser was null, branch will be created randomly on stack");
    		}
    		return;
    	}
    	    	
    	StringBuffer hashedID = new StringBuffer(reqViaBranch.length() + 9);
    	
    	hashedID.append(reqViaBranch);
    	hashedID.append("_");
    	
    	int result = 1;
    	result = 31 * result + tUser.getSharedID().hashCode();
    	result = 31 * result + (int)getRequest().getCSeqHeader().getSequenceNumber();
    	hashedID.append(result);
    	
		viaHeader.setBranch( hashedID.toString());
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this,
							"createViaBranchBasedOnIncomingRequest", "reqViaBranch = " + reqViaBranch +
							", Incoming Request: " + req.getMethod() + " incoming callid= " + req.getCallId() +
							", tUser.getSharedID() = " + tUser.getSharedID() +
							", getRequest().getCSeqHeader().getSequenceNumber() = " + getRequest().getCSeqHeader().getSequenceNumber() +
							", via branch = " + viaHeader.getBranch());
		}
    }
    
    /**
	 * Selects a provider according to the transport defined in the Route header
	 * or in the URI if Route not exists. If the default provider of this
	 * message is not match the transport that defind in the Route or URI of
	 * this message = new provider will be selected
	 * 
	 * @param reqURI
	 */
	public void setProvider(SipURL sipURL) {
		String transport = null;
		String scheme = null;
		try {
			transport = sipURL.getTransport();
			scheme = sipURL.getScheme();
			if  (transport == null){
				transport = UDP;
			}
						
			if (scheme.equalsIgnoreCase(SIPS)) {
				transport = TLS;
			}

			//finally, we select the provider from the transport
			selectProvider(transport);
		} catch (Exception e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setProvider",
						"The following Exception has occurred", e);
			}
		}
	}


	/**
	 * intent to loopback request the state info should be serialized 
	 * in to request internals headers, it is the way it can pass through
	 * the stack.
	 * @throws SipParseException 
	 * @throws IllegalArgumentException 
	 */
	public void addCompositionHeader() throws IllegalArgumentException, SipParseException{
		
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "addCompositionHeader");
        }
		
		try {

			// Save compositionId header and copies the relevant data to a map if the chain continue
			if (this.getNextApplication() != null){
				
				// generate a new unique compositionId that will be the key of the composition data for the next application to retrieve
				String compositionId = UUID.randomUUID().toString();
				// copy the composition info from the request object to a CompositionData object that will be added to the map
				CompositionData compositionData = new CompositionData();
				compositionData.setInitialPoppedRoute(getInitialPoppedRoute());
				compositionData.setNextApplication(getNextApplication());
				compositionData.setRoutingDirective(getDirective());
				compositionData.setRoutingRegion(getRegion());
				compositionData.setStateInfo(getStateInfo());
				compositionData.setSubscriberUri(getSubscriberURI());
				// add the compositionId as key and compositionData as value to the map
				CompositionInfoMap.getInstance().addCompositionInfo(compositionId, compositionData);
				Header compositionIdHeader = getHeadersFactory().createHeader(COMPOSITION_ID, compositionId);
				getRequest().addHeader(compositionIdHeader, true);
				
				if (c_logger.isTraceDebugEnabled()){
					
					StringBuffer buff = new StringBuffer();
					
					buff.append("\n")
						.append("Composition data copied to the map: \n")
					    .append("----------------- \n")
					    .append("Composition Id    = " + compositionId + " \n")
				        .append("Next application  = " + this.getNextApplication() + " \n")
				        .append("Subscriber uri    = " + this.getSubscriberURI() + " \n")
				        .append("Routing region    = " + this.getRegion() + " \n")
				        .append("Routing directive = " + this.getDirective() + " \n\n");
				        
					c_logger.traceDebug(this, "addCompositionHeader", buff.toString());
				}
	
			}

		} catch (HeaderParseException e) {
			c_logger.error(e.getMessage());
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "addCompositionHeader");
        }
	}
	

    /**
     * Check whether there additional applications on the path that should be
     * handle this request before it is sent outside of the container. 
     * @return true if needed to be sent in loop back mode otherwise false
     */
    private boolean checkForApplicationComposition()
    {
        SipRouter router = SipContainer.getInstance().getRouter();

        String nextApplication = router.checkForApplicationComposition(this);
        if (nextApplication != null)
        	this.setNextApplication(nextApplication);
        return (nextApplication != null); 
    }
    /**
     * Helper function. Creates the call Id Header for the request
     * @return CallIdHeader
     */
    private CallIdHeader createCallIdHeader(String callId)
        throws IllegalArgumentException, SipParseException
    {
        if (callId == null)
        {	
        	// PM61939 - hide local IP in Call-ID
        	// getCallIdValue() returns the value of the new custom property com.ibm.ws.sip.callid.value if set
    		// if not set returns a local IP
    		callId = SIPStackUtil.generateCallIdentifier(getSipProvider().getListeningPoint().getCallIdValue());
        }

        return getHeadersFactory().createCallIdHeader(callId);
    }

    /**
     * Helper Function - Creates the To header of the Request
     * @param to JSR-116 To header
     * @return the new JAIN To header
     */
    private ToHeader createToHeader(AddressImpl to)
        throws IllegalArgumentException, SipParseException
    {
        // create a new JAIN header
        NameAddress nameAddress = (NameAddress)to.getNameAddressHeader().getNameAddress().clone();
        ToHeader toHeader = getHeadersFactory().createToHeader(nameAddress);

        // copy header parameters
        copyParameters(to, toHeader);
        return toHeader;
    }
    
    /**
     * Helper Function - Creates the From header of the Request
     * @param to JSR-116 From header
     * @return the new JAIN From header
     */
    private FromHeader createFromHeader(AddressImpl from)
        throws IllegalArgumentException, SipParseException
    {
        // create a new JAIN header
        NameAddress nameAddress = (NameAddress)from.getNameAddressHeader().getNameAddress().clone();
        FromHeader fromHeader = getHeadersFactory().createFromHeader(nameAddress);

        // copy header parameters
        copyParameters(from, fromHeader);
        return fromHeader;
    }
    
    /**
     * Helper Function - copies address header parameters
     * from a JSR-116 message to a JAIN message
     * 
     * @param jsr JSR-116 message
     * @param jain JAIN message
     */
    private void copyParameters(AddressImpl jsr, EndPointHeader jain)
    	throws IllegalArgumentException, SipParseException
    {
        // copy all parameters (including the tag)
        Iterator i = jsr.getParameterNames();
        while (i.hasNext()) {
        	String name = (String)i.next();
        	String value = jsr.getParameter(name);
        	jain.setParameter(name, value);
        }
    }

    /**
     * Sets the local from tag in case it has not been set yet. 
     */
    private void testAndSetFromTag()
    {
        FromHeader from  = getRequest().getFromHeader();
        String tag = from.getTag();
        if (null == tag || tag.trim().length() == 0)
        {

            //Generate the tag so we can have it prior to send. So we can map
            //session ids prior to sending. 
            tag = getTransactionUser().generateLocalTag();
            try
            {
                from.setTag(tag);
            }
            catch (IllegalArgumentException e)
            {
                if(c_logger.isErrorEnabled())
                {
                    
                    c_logger.error("error.exception", Situation.SITUATION_CREATE, 
                            		null, e);
                }
            }
            catch (SipParseException e)
            {
                if(c_logger.isErrorEnabled())
                {
                    
                    c_logger.error("error.exception", Situation.SITUATION_CREATE, 
                            		null, e);
                }
            }
        }
    }
    /**
     * Helper Function - Get the Content Type Header from the list. If one
     * is not available, a new empty header will be created. 
     * @return ContentTypeHeader
     */
    protected ContentTypeHeader getContentTypeHeader()
        throws IllegalArgumentException, SipParseException
    {
        ContentTypeHeader contentTypeHeader =
            (ContentTypeHeader) findHeader(ContentTypeHeader.name);

        return contentTypeHeader;
    }

    /**
     * Helper method. Generate a Jain Request URI for a new Jain Request. 
     * If the Request URI is set then this address will be used otherwise 
     * the Contact Header of the originating session will be used. 
     * @return ContactHeader
     */
    private jain.protocol.ip.sip.address.URI generateJainRequestURI()
        throws IllegalArgumentException, SipParseException, IOException
    {
        jain.protocol.ip.sip.address.URI reqURI = null;
        TransactionUserWrapper tUser = getTransactionUser();

        if (null != tUser && tUser.isServerTransaction())
        {
            Address contact = tUser.getContactHeader();
            if (contact != null)
            {
                reqURI = createJainRequestURI(contact.getURI());
            }

        }

        // Still not good?
        if (null == reqURI)
        {
            //The default value of the request URI is the URI of the To header, 
            //with the additional requirement that for REGISTER requests the user 
            //part of a SIP request URI is empty.
            reqURI = (jain.protocol.ip.sip.address.URI) 
            		 getRequest().getToHeader().getNameAddress().getAddress().clone();
            
            if(getMethod().equalsIgnoreCase(Request.REGISTER))
            {
                if(reqURI instanceof SipURL)
                {
                    ((SipURL)reqURI).removeUserName();
                    ((SipURL)reqURI).removeUserPassword();
                    ((SipURL)reqURI).removeUserType();
                }
            }
        }

        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "generateJainRequestURI",
                "createdURI: " + reqURI);
        }

        if (null == reqURI)
        {
            String err = "Failed to generate a URI for the outgoing request";
            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(this, "generateJainRequestURI", err);
            }
            throw new IOException(err);
        }

        return reqURI;
    }
    

    /**
     * @see javax.servlet.sip.SipServletRequest#getRequestURI()
     */
    public URI getRequestURI()
    {
        URI reqUri = super.getRequestURI();
        if (null == reqUri)
        {
            try {
				jain.protocol.ip.sip.address.URI reqURI = generateJainRequestURI();
				getRequest().setRequestURI(reqURI);
                reqUri = super.getRequestURI();
			} catch (IllegalArgumentException e) {
				logException(e);
			} catch (SipParseException e) {
				logException(e);
			} catch (IOException e) {
				logException(e);
			}
            
        }
        return reqUri;
    }

    /**
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#getLocalParty()
     */
    Address getLocalParty()
    {
        return getFrom();
    }

    /**
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#getRemoteParty()
     */
    Address getRemoteParty()
    {
        return getTo();
    }

    /**
     * @see javax.servlet.sip.SipServletRequest#createCancel()
     */
    public SipServletRequest createCancel()
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "createCancel");
    	}
    	
    	if (!isLiveMessage("createCancel"))
    		return null;
    	
    	//Invite message that was not sent yet, therefore is not in commit state, cannot get canceled 
    	if (!isCommitted())
    	{
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "createCancel", "Can not cancel an outgoing request that is not commited");
    		}

    		throw new IllegalStateException("Can not cancel an outgoing request that is not commited");
    	}

    	//if the outgoing request is committed, the transaction must be set on it.
    	//if the Invite transaction is already terminated (final response was  received)
    	//Cancel can not get received
    	if (getTransaction().isTerminated()){
    		//can not cancel terminated transactions
    		if (c_logger.isTraceDebugEnabled()){
    			c_logger.traceDebug(this, "createCancel",
    			"Can not cancel an outgoing request when transaction is terminated");
    		}

    		throw new IllegalStateException("Can not cancel an outgoing request when transaction is terminated");
    	}
    	
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceExit(this, "createCancel");
    	}
        return new OutgoingSipServletCancelRequest(this);
    }

    /**
     * @see javax.servlet.sip.SipServletRequest#createResponse(int, String)
     */
    public SipServletResponse createResponse(
        int statusCode,
        String reasonPhrase)
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "createResponse",
                "Can not create a response for outgoing request");
        }
        throw new IllegalStateException(
        		"Can not create responses for locally generated requests");

    }

    /**
     * @see javax.servlet.sip.SipServletRequest#createResponse(int)
     */
    public SipServletResponse createResponse(int statuscode)
    {

        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "createResponse",
                "Can not create a response for outgoing request");
        }
        
        throw new IllegalStateException(
                "Can not create responses for locally generated requests");
    }

    /**
     * Helper method that is called in case of NAPTR resolve when
     * request was send (as application point of view ) and flag of 
     * isCommited is true but actually it was not sent. When first NAPTR 
     * resolve response received new AddressHeader should be set.
     * @param name
     * @param addr
     */
    public void setAddrHeader(String name, Address addr)
    {
        super.setAddressHeader(name, addr);
    }
    
    /**
     * @see javax.servlet.sip.SipServletMessage#addHeader(String, String)
     */
    public void addHeader(String name, String value)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
    	//Check if it not a system header. 
    	checkIsLegalHeader(name);

        checkNewHeader (name,value);
        
        addHeaderImpl(name, value, false);
    }
    
    /**
     * Check if a header is legal, and in case of To and From removes the To tag.
     */
    public void addHeaderToFromAllowed(String name, List<String> values, boolean allowToFromChange){
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(OutgoingSipServletRequest.class.getName(),
    				"addHeaderToFromAllowed", new Object[]{name, values, allowToFromChange});
    	}

    	if (allowToFromChange) {
    		if(FromHeader.name.equalsIgnoreCase(name)){
    			if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "addHeaderToFromAllowed", "adding From header without tag part");
    			}
    			String tag = getRequest().getFromHeader().getTag(); // tag that was generated on request creation
    			//in case of setting a From header it should only have one value
    			if (values.size() > 1){
    				throw new IllegalArgumentException("Illegal operation, Trying to set more than one From header value");
    			}
    			String value = values.get(0);
    			removeHeader(FromHeader.name, false);
    			addHeaderImpl(FromHeader.name, value, true);
    			//remove from tag
    			getRequest().getFromHeader().removeTag();
    			try {
    				getRequest().getFromHeader().setTag(tag);
    			} catch (IllegalArgumentException e) {
    				throw e;
    			} catch (SipParseException e) {
    				if (c_logger.isErrorEnabled()) {
    					c_logger.error("setting tag failed", null, null, e);
    				}
    			}
    			return;
    		} else if(name.equals(ToHeader.name)){
    			//in case of setting a To header it should only have one value
    			if (values.size() > 1){
    				throw new IllegalArgumentException("Illegal operation, Trying to set more than one To header value");
    			}
    			String value = values.get(0);
    			removeHeader(ToHeader.name, false);
    			addHeaderImpl(name, value, false);
    			getRequest().getToHeader().removeTag();
    			return;
    		}	
    	} 
    	
		if (!RouteHeader.name.equalsIgnoreCase(name)){
			// Check if it a no system header others than To , and Record-Route 
    		checkIsLegalHeader(name);
    		removeHeader(name, false);
		}
		
		//add all values
		for (String value : values) {
			checkNewHeader (name,value);
	        addHeaderImpl(name, value, false);
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(OutgoingSipServletRequest.class.getName(), "addHeaderToFromAllowed");
		}
    }

    /**
     * Helper Function - Adds the specified Header to the list of Headers at the
     * specified position (first or last)
     * @param name
     * @param value
     * @param first
     */
    private void addHeaderImpl(String name, String value, boolean first)
    {
        if (isCommitted())
        {
            if (c_logger.isTraceDebugEnabled())
            {
                if (c_logger.isTraceDebugEnabled())
                {
                    c_logger.traceDebug(
                        this,
                        "addHeaderImpl",
                        "addHeader failed, Request already Commited");

                }
            }

            return;
        }

        try
        {
            Header header = getHeadersFactory().createHeader(name, value);
            addHeader(header, first);
        }
        catch (IllegalArgumentException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name, value };
                c_logger.error(
                    "error.add.header",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name, value };
                c_logger.error(
                    "error.add.header",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }

    /**
     * Check the value of the header if it is OK
     * @param name
     * @param value
     */
    public void checkNewHeader(String name, String value){
    	
//      Verify that the header is not a require header with parameter 100rel that cannot be sent for non INVITE requests
        if(name.equalsIgnoreCase(RequireHeader.name)){
         	if(value.equals(ReliableResponse.RELIABLY_PARAM) &&
         			! (getMethod().equals(Request.INVITE)|| getMethod().equals(RequestImpl.PRACK))){
					throw new IllegalStateException ("Illegal operation, 100rel can be requiered only for INVITE");
				}
         }
        
    }

    /**
     * Set a header 
     * @see javax.servlet.sip.SipServletMessage#setHeader(String, String)
     * 
     * @param name the header name
     * @param value the header value
     * @param checkIsLegalHeader should we check if it's legal to add this header
     */
    public void setHeader( String name, String value, boolean checkIsLegalHeader)
    {
    	// TODO during code review, make sure we need this overrides
	    if(isCommitted())
	    {
	    	if( isJSR289Application()){
	        throw new IllegalStateException(
	           "Can not modify committed message");
	    	} else {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "setHeader", 
                    "setHeader failed, Request already Committed");
				}
				return;
			}
	    }
        
        checkNewHeader(name,value);
        
        super.setHeader(name, value, checkIsLegalHeader);
        
    }

    /**
     * Assya: PMR 34451,724,724 - Contact header in REGISTER, defect PK47359
     * Remove header
     * 
     * @param name the header name
     */
    public void removeHeader(String name){
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "removeHeader", name);
    	}

    	if(isCommitted()){
    		if(isJSR289Application()){
    			throw new IllegalStateException(
    			"Can not modify committed message");
    		} else {
    			if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "removeHeader", 
    				"removeHeader failed, Request already Committed");
    			}
    			return;
    		}
    	}

    	if (ContactHeader.name.equalsIgnoreCase(name)){
    		//Contact has been removed and will not be added by default when 
    		//the container is sent. 
    		m_contactRemovedByApp = true;
    	}

    	removeHeader(name, true);

    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceExit(this, "removeHeader");
    	}
    }

    /**
     * Sets the Client Transaction Listener associated with this request. 
     * @param listener
     */
    public void setClientTransactionListener(ClientTransactionListener listener)
    {
        m_clientTransactionListener = listener; 
    }
    
    /**
     * Proxy objects will use create outbound branches by using this request 
     * object, same object as used in the case of UAC application.  
     * When get proxy is called on this request in case it has been used by 
     * a proxy object then it should return the same proxy object used on 
     * the original incoming request. Otherwise use the base class implementation.
     * @see javax.servlet.sip.SipServletRequest#getProxy(boolean)
     */
    public Proxy getProxy(boolean create) throws TooManyHopsException 
    {
    	if (!isLiveMessage("getProxy"))
    		return null;
        
        if(m_proxy != null)
        {
            return m_proxy;
        }
        else
        {
            return super.getProxy(create);
        }
    }
    
    /**
     * Sets the proxy object that used this request on an outbound proxy branch. 
     * @see getProxy(boolean)
     * @param proxy
     */
    public void setProxy(Proxy proxy)
    {
    	if (!isLiveMessage("setProxy"))
    		return;
        
        m_proxy = proxy; 
    }
    
    /**
     * Mark the request as a subsequent request in an exising dialog. 
     * @param 
     */
    public void setIsSubsequentRequest(boolean isSubsequentRequest) {
        m_isSubsequentRequest = isSubsequentRequest;
    }
    
    /**
     * called when creating a CANCEL request, do determine the message
     *  destination
     * @return the same destination as the original INVITE request,
     *  or null if unknown or not an INVITE request
     */
    Header getDestinationURI() {
    	return m_destinationHeader;
    }
    
    /**
     * called when creating a CANCEL request, to determine the local source listening point.
     * @return the same IBM-PO as the original request,
     *  or null if unknown or not an INVITE request
     */
    Header getPreferredOutbound() {
    	return m_preferredOutbound;
    }
    
    /**
     * 
     * @param isCommited
     * @return
     */
    protected void updateUnCommittedMessagesList(boolean isCommited){
    	//there is not need to add outgoing requests to the uncommitted
    	//Messages list since it will be committed when it is sent,
    }
    
    /**
     * Helper function - Adds to the outgoing message a contact header
     */
    private void addContactHeader(SipURI	sipURI)
        throws IllegalArgumentException, SipParseException
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { sipURI };
			c_logger.traceEntry(OutgoingSipServletRequest.class.getName(),
					"addContactHeader", params);
		}
    	try{
	    	//First check that we don't already have a contact header
	        if (m_contactRemovedByApp)
	        {
	            //Contact header has been explicitly removed by the application,
	            //therefore we do not  add our own contact header.
	            if (c_logger.isTraceDebugEnabled())
	            {
	                c_logger.traceDebug(
	                    this,
	                    "addContactHeader",
	                    "Can't addContactHeader, contact explicitly removed by App");
	            }
	
	            return;
	        }
	                
	        String method = getMethod(); 
	        // Add Contact header only for requests that should contain this header
	        if(SipUtil.shouldContainContact(method))
	        {
			    Request req = getRequest();
			    if (req.getHeader(ContactHeader.name, true) != null && !m_isDefaultContactHeader)
			    {
			        if (c_logger.isTraceDebugEnabled())
			        {
			            c_logger.traceDebug(
			                this,
			                "addContactHeader",
			                "Can't addContactHeader, contact exist");
			        }
			        return;
			    }
			    createAndSetContactHeader(req, sipURI, true);
	        }   
    	} finally{
    		if (c_logger.isTraceEntryExitEnabled()) {
    			c_logger.traceExit(OutgoingSipServletRequest.class.getName(), "addContactHeader", getContactHeader());
    		}
    	}
    }
    
    /**
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#setContactScheme(jain.protocol.ip.sip.address.SipURL)
     * 
	 * Helper method to set relevant Scheme to Contact header
	 * @param sipUrl
     * @throws SipParseException 
     * @throws IllegalArgumentException 
	 */
	protected void setContactScheme(SipURL sipUrl) 
			throws IllegalArgumentException, SipParseException {
		if(isCommitted())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
		
		if (sipUrl.getScheme().equalsIgnoreCase("sip")) {
            // rfc 3261 8.1.1.8:
            // If the Request-URI or top Route header field value contains a SIPS
            // URI, the Contact header field MUST contain a SIPS URI as well.
            boolean secure = false;
            if (getRequestURI() != null && getRequestURI().getScheme().equalsIgnoreCase("sips")) {
                // request URI is secure
                secure = true;
            }
            else {
                // check if top Route is secure
                RouteHeader topRoute = (RouteHeader)findHeader("Route");
                if (topRoute != null) {
	                String scheme = topRoute.getNameAddress().getAddress().getScheme();
	                if (scheme.equalsIgnoreCase("sips")) {
	                    secure = true;
	                }
	            }
            }
            if (secure) {
                sipUrl.setScheme("sips");
            }
        }
	}

	/**
	 * JSR289 logic
	 */
	public void setRoutingDirective(SipApplicationRoutingDirective directive, SipServletRequest origRequest) {
		
		if (directive.equals(SipApplicationRoutingDirective.CONTINUE) ||
				directive.equals(SipApplicationRoutingDirective.REVERSE)){

			// Can't set CONTINUE or REVERSE directive 
			// when  origRequest is not initial
			if (origRequest!= null && !origRequest.isInitial()){
				throw new IllegalStateException("Can't set CONTINUE or REVERSE directive when  origRequest is not initial");
			}
			
			// If the request should continue application chain, 
			// we pass the state from the origin request. 
			if (origRequest!= null){
				this.setStateInfo(((SipServletRequestImpl)origRequest).getStateInfo());
			}
		}
		
		// If the directive is NEW the composition selection 
		// process will start then the old state info recived from 
		// application router is irrelevant
		if (directive.equals(SipApplicationRoutingDirective.NEW)){
			this.setStateInfo(null);
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("setRoutingDirective: " + directive);
		}
		
		this.setDirective(directive);
	}

	
	/**
     * @see javax.servlet.sip.SipServletRequest#getB2buaHelper()
     */
    public B2buaHelper getB2buaHelper() throws IllegalStateException {
    	return getB2buaHelper(true,UAMode.UAC);
    }
   
    @Override
    protected boolean shouldCreateContactIfNotExist() {
    	return true;
    }

	public boolean isMarkedForErrorResponse() {
		return markedForErrorResponse;
	}

	public void setMarkedForErrorResponse(boolean markedForErrorResponse) {
		this.markedForErrorResponse = markedForErrorResponse;
	}

	/**
	 * @see SipServletRequestImpl#imitateInitialForComposition
	 */
	@Override
	public void imitateInitialForComposition() {
		this.setIsInital(true);
	}

	/**
	 * @see SipServletRequestImpl#cleanInitialForComposition
	 */
	@Override
	public void cleanInitialForComposition() {
		this.setIsInital(false);
	}

    /**
     * @see com.ibm.ws.sip.container.servlets.SipServletRequestImpl#cleanExpiredCompositionHeaders()
     */
    public void cleanExpiredCompositionHeaders(){
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("cleanExpiredCompositionHeaders. CallID=" + this.getCallId());
		}
    	this.removeHeader(COMPOSITION_ID);
    }
    
    /**
     * When appilcation router throws some unknown exception we should
     * process 500 response for IncomingRequest 
     * @param request
     */
    public void processCompositionErrorResponse(){
		this.setMarkedForErrorResponse(true);
    }

	public String getAppInvokedName() {
		return appInvokedName;
	}

	public void setAppInvokedName(String appInvokedName) {
		this.appInvokedName = appInvokedName;
	}

	public boolean isAppInvoked116Type() {
		return this.appInvokedName != null;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.sip.container.servlets.SipServletResponseImpl#getProxy()
	 */
	@Override
	public Proxy getProxy() {
		//there should be no proxy object for an outgoing request
		return null;
	}

    /**
     * determines the message's Request Uri
     * 
     * @throws IOException
     */
    public void setupRequestUri() throws IOException{
    	try{
    		Request req = getRequest(); 
    		
    		// Only for the first send request should be updated with the
    		// following parameters. Otherwith revious parameters 
    		// can be used.
    		jain.protocol.ip.sip.address.URI reqURI = req.getRequestURI();
    		
    		// Create the Request URI`
    		if (reqURI == null) {
    			if (m_isSubsequentRequest) {
    				TransactionUserWrapper tu = getTransactionUser();
    				String sessionId = tu == null ? "null" : tu.getId();
    				throw new IllegalStateException(
    						"no remote contact for sending in-dialog request ["
    						+ req.getCallIdHeader()
    						+ "] session [" + sessionId + ']');
    			}
    			reqURI = generateJainRequestURI();
    			req.setRequestURI(reqURI);
    		}
    	}        
    	catch (SipParseException e){
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { this };
                c_logger.error(
                    "error.send.request",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
            logExceptionToSessionLog(SipSessionSeqLog.ERROR_SEND_REQ, e);
            throw (new IOException(e.getMessage()));
        }

    }

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext startAsync() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	
	    
}
