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

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.RecordRouteHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.sip.Address;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.extensions.SupportedHeader;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.tu.TransactionUserImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * @author Amir Perlman, Feb 20, 2003
 *
 * Represents a Sip Servlet Request that was generated based on Jain Sip
 * Request that was received through a provider. 
 */
public class IncomingSipServletRequest extends SipServletRequestImpl
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 3933920609932862490L;

	/**
    * Class Logger. 
    */
    private static final LogMgr c_logger =
        Log.get(IncomingSipServletRequest.class);

    /**
     * Holds Authentication-Info Header data need to be sent back to server
     */
    private transient String m_authInfoHeaderData = null; 
	private static final String AUTHENTICATION_INFO_HEADER_NAME="Authentication-Info";    
 
	/**
     * Contains information if this Request SHOULD be answered Reliably
     * If the incomming request contains "Require" header it will be true
     */
    protected transient boolean m_shouldBeAnsweredReliable = false; 
    
    /**
     * Contains information if this Request MAY be answered Reliably
     * If the incomming request contains "Supported" header it will be true
     */
    protected transient boolean m_mayBeAnsweredReliable = false; 
	
    /**
     * Defines if this message was already checked for reliabiliry
     * The approprite headers will be parsed only if getMayBeAnsweredReliable()
     * of getShouldBeAnsweredReliable() will called
     */
    protected transient boolean m_reliableWasParsed = false;
    
    /**
     * Will be true if this IncomingRequest ACK was sent as part of the INVITE-Error-ACK transaction
     */
    protected transient boolean m_ackOnErrorResponse = false;
    
    /**
     * Pointer to the SIP Container to avoid a call to get instance on every
     * message sent. 
     */
    protected transient SipContainer c_sipContainer = SipContainer.getInstance();   
    
    /**
     * public no-arg constructor to satisfy Externalizable.readExternal()
     */
    public IncomingSipServletRequest() {
    }
    
    /**
    * Constructs a new Sip Servlet Request based on an existing Jain Sip Request
    * @param request The Jain Sip Request associated with this Sip Servlet Request
    * @param transactionId transaction id associated with this request. 
    * @param provider The Sip Provider that will be used for generating 
    * responses and acknowledgements to the request. 
    */
    public IncomingSipServletRequest(
        Request request,
        long transactionId,
        SipProvider provider)
    {
        super(request, transactionId, provider);
        setIsCommited(false); 
    }
    
    /**
     * Copy Constructor for Requests that should be create based on this
     * SipServletRequest.
     * Used for IncomingDeaeSipRequests 
     *
     */
    public IncomingSipServletRequest (IncomingSipServletRequest request){
    	super(request);
       m_shouldBeAnsweredReliable = request.getShouldBeAnsweredReliable(); 
       m_mayBeAnsweredReliable = request.getMayBeAnsweredReliable();
       m_reliableWasParsed = request.m_reliableWasParsed;
       m_sipProvider = request.getSipProvider();       
    }
    
    /**
     * Set status of this request. Will be changed when this is ACK request on non2xx response
     * @param status
     */
    public void setAckOnErrorResponse(boolean responseOnAck){
    	m_ackOnErrorResponse = responseOnAck;
    }
    
    /**
     * Return the requeset status.
     * @return
     */
    public boolean isAckOnErrorResponse(){
    	return m_ackOnErrorResponse;
    }
    
    /**
     * Helper function that will set the flag m_shouldBeAnsweredReliable
     * @throws NoSuchElementException
     * @throws HeaderParseException
     */
    private boolean setShouldAnswerReliable() throws HeaderParseException,
                                             NoSuchElementException {
        //  This function will search the "Require" header and look for the
        // "100rel" String there

        if (getRequest().hasRequireHeaders()) {
            HeaderIterator iter = getRequest().getRequireHeaders();
            Header hValue = null;
            if (iter != null)
                for (; iter.hasNext();) {
                    hValue = (Header) iter.next();
                    if (hValue.getValue().equalsIgnoreCase(ReliableResponse.RELIABLY_PARAM)) {
                        m_shouldBeAnsweredReliable = true;
                        return true;
                    }

                }

        }
        return false;
    }

    /**
     * Helper function that will set the flag m_shouldBeAnsweredReliable
     * @throws NoSuchElementException
     * @throws HeaderParseException
     */
    private void parseMayAnswerReliable() throws HeaderParseException,
                                       NoSuchElementException {
        // This function will search the "Supported" header and look for the
        // "100rel" String there
        HeaderIterator iter = getRequest().getHeaders(SupportedHeader.name);
        Header hValue = null;
        if (iter != null)
            for (; iter.hasNext();) {
                hValue = (Header) iter.next();
                if (hValue.getValue().equalsIgnoreCase(ReliableResponse.RELIABLY_PARAM)) {
                    m_mayBeAnsweredReliable = true;
                }

       }
    }
    
    /**
     * Helper method to check if this request SHOULD or MAY be answered reliably.
     */
    private void parseReliable() {
        try {
            // Olny if the message doesb't have "Require 100 rel" header we should
            // check the "Support 100 rel"
            if(setShouldAnswerReliable() == false){
                parseMayAnswerReliable();
            }
            m_reliableWasParsed = true;
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer();
                buff.append("shouldBeAnsweredReliably = ");
                buff.append(m_shouldBeAnsweredReliable);
                buff.append(" mayBeAnsweredReliably = ");
                buff.append(m_mayBeAnsweredReliable);
                c_logger.traceDebug(this, "parseReliable", 
                                    buff.toString());
            }
        }
        catch (HeaderParseException e) {
            if (c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception",Situation.SITUATION_REQUEST,null);
            }
            logExceptionToSessionLog(SipSessionSeqLog.PROCESS_REQ, e);
            
        } catch (NoSuchElementException e) {
            
            if (c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception",Situation.SITUATION_REQUEST,null);
            }
            logExceptionToSessionLog(SipSessionSeqLog.PROCESS_REQ, e);
        }
    }
    
    /**
     * @return Returns the m_mayBeAnsweredReliable.
     */
    public boolean getMayBeAnsweredReliable() {
        if(m_reliableWasParsed == false){
            parseReliable();
        }
        return m_mayBeAnsweredReliable;
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
     * @return Returns the m_shouldBeAnsweredReliable.
     */
    public boolean getShouldBeAnsweredReliable() {
        if(m_reliableWasParsed == false){
            parseReliable();
        }
        return m_shouldBeAnsweredReliable;
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
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { getRequest()};

                c_logger.error(
                    "error.get.method",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }

        return method;
    }


    /**
     * @see javax.servlet.sip.SipServletMessage#send()
     */
    public void send() throws IOException
    {
    	throw new IllegalStateException("This is an Incoming request - cannot be sent.");
    }

    /**
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#getLocalParty()
     */
    Address getLocalParty()
    {
        return getTo();
    }

    /**
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#getRemoteParty()
     */
    Address getRemoteParty()
    {
        return getFrom();
    }

    /**
	 * @see javax.servlet.sip.SipServletRequest#createResponse(int)
	 */
    public SipServletResponse createResponse(int statusCode)
    {
        return createResponse(statusCode, null);
    }

    /**
     * @see javax.servlet.sip.SipServletRequest#createResponse(int, java.lang.String)
     */
    public SipServletResponse createResponse(
        int statusCode,
        String reasonPhrase)
    {
    	if(isUnmatchedReqeust() == true){
    		throw new IllegalStateException("This is unmatched incoming request. It is incactive");
    	}
        String toTag = getRequest().getToHeader().getTag();
        return createResponse(statusCode, reasonPhrase, toTag);
    }

    /**
     * Should be implmemented by derived classes
     * @param tu
     * @param statusCode
     * @param reasonPhrase
     * @return
     */
    public OutgoingSipServletResponse createResponseForCommitedRequest(
        															int statusCode, String reasonPhrase) {
    	if(isUnmatchedReqeust() == true){
    		throw new IllegalStateException("This is unmatched incoming request. It is incactive");
    	}
        String toTag = getRequest().getToHeader().getTag();
        return createResponse(statusCode, reasonPhrase, toTag);
	}

    /**
     * Create a response with a specific toTag
     * 
     * @param statusCode the status of the response
     * @param reasonPhrase the response phrase
     * @param toTag the to tag of the response
     * @return the new response
     */
    public OutgoingSipServletResponse createResponse(
            int statusCode,
            String reasonPhrase,
            String toTag){
    	return createResponse( statusCode, reasonPhrase, toTag, null);
    }
    /**
     * Create a response with a specific toTag
     *
     * @param statusCode the status of the response
     * @param reasonPhrase the response phrase
     * @param toTag the to tag of the response
     * @param incomingResponseTransactionUser this will be passed when called from a BranchManager.
     * @return the new response
     */
    public OutgoingSipServletResponse createResponse(
        int statusCode,
        String reasonPhrase,
        String toTag,
        TransactionUserWrapper incomingResponseTransactionUser)
    {
    	if (c_logger.isTraceDebugEnabled()) {
  			Object[] params = { statusCode, reasonPhrase, toTag, incomingResponseTransactionUser};
  			c_logger.traceEntry(this, "createOutgoingResponse", params);
  		}
    	if(isUnmatchedReqeust() == true){
    		throw new IllegalStateException("This is unmatched incoming request. It is incactive");
    	}
    	
    	try{
	    	if (!isLiveMessage("createResponse"))
	    		return null;
	
	    	if(getMethod().equals(Request.ACK))
	        {
	            throw new IllegalStateException("Can not generate response for a ACK");
	        }
	        
	    	TransactionUserWrapper tUser = getTransactionUser();
	    	
	        if (getTransaction().isTerminated())
	        {
	            if(tUser != null)
	            {
	                tUser.logToContext(SipSessionSeqLog.ERROR_TRANSACTION_TERMINATED, this);
	            }
	            throw new IllegalStateException(
	                "Transaction terminated, Can not generate response. Call Id: " 
	                + getCallId());
	        }
	        
	        //MUST synchronize as we could have a race condition between two threads
	        //attempting to create responses at the same time. e.g. 200 and 180 in 
	        //that order which could happen on callbacks that we get from the Web 
	        //container after a servlet has been invoked. 
	        synchronized(this)
	        {
	            // If it is a final response, mark the request as committed,
	        	// to qualify with SipServletMessage.isCommitted().
	        	// Note this does not prevent the application from generating
	        	// another final response, as long as it was not sent.
	        	//for proxy application we cannot change the request for commited if this is the
	        	//initial request since initial proxy requests can send multiple responses
	        	//for parallel proxy.
	        	boolean proxying = false;
	        	if(incomingResponseTransactionUser == null){
	        		proxying = tUser != null && tUser.isProxying();
	        	}else{
	        		proxying = incomingResponseTransactionUser.isProxying();
	        	}
	        	
	            if (statusCode >= 200 && tUser != null && !(proxying && isInitial())){
	            	setIsCommited(true);
	            }else{
	            	if (c_logger.isTraceDebugEnabled() && tUser != null){
	            		c_logger.traceDebug(this, "createResponse", 
	            				"Message state was not changed to commited, isProxy: " + proxying + ", isInital: " + isInitial());
	            	}
	            }
	        }
	        
	    	return createOutgoingResponse(statusCode,reasonPhrase,toTag, incomingResponseTransactionUser);
    	}finally{
    		if (c_logger.isTraceDebugEnabled()) {
      			c_logger.traceExit(this, "createOutgoingResponse");
      		}
    	}
    	
    }

    /**
     * Helper method which is actually creates an outgoing response
     * @param statusCode
     * @param reasonPhrase
     * @param toTag
     * @return
     */
    protected OutgoingSipServletResponse createOutgoingResponse(int statusCode,	String reasonPhrase, String toTag,
    								TransactionUserWrapper incomingResponseTu) {
    	if (c_logger.isTraceDebugEnabled()) {
  			Object[] params = { statusCode, reasonPhrase, toTag, incomingResponseTu };
  			c_logger.traceEntry(this, "createOutgoingResponse", params);
  		}
    	if (statusCode < 100 || statusCode > 999) {
    		// RFC 3261 does not allow status codes higher than 699, but we
    		// are generous enough to allow any 3-digit status code.
    		throw new IllegalArgumentException("status code out of range ["
   				+ statusCode + ']');
    	}

        OutgoingSipServletResponse response = null;
        try
        {
            // Create the proper jain reponse
            Request jainRequest = getRequest();
            Response jainResponse =
                getMessageFactory().createResponse(statusCode, jainRequest);
            if (null != reasonPhrase)
            {
                jainResponse.setReasonPhrase(reasonPhrase);
            }

            //If available set the to tag of the response
            if (null != toTag)
            {
                jainResponse.getToHeader().setTag(toTag);

            }

            // Add the contact header to a "101 and above" response to a requests
            //that generate a dialog.
            // for derived sessions in proxy, if we have an incoming response TU that is different then
            //orig request TU, then we need to apply this one
            TransactionUserWrapper tu = incomingResponseTu != null ? incomingResponseTu : 
            														 getTransactionUser();
            /*
            if (statusCode >= 101 && SipUtil.isDialogInitialRequest(jainRequest.getMethod()))
            {
                //Do not change existing contact header if already set by the
                //application. 
                if (jainResponse.getContactHeaders() == null && !tu.isProxying())
                {
                    addContactHeader(jainResponse);
                }
            }
            */

            // Create the "Siplet" response
            response = new OutgoingSipServletResponse(jainResponse, this);
            response.setTransactionUser(tu);
            
          addAuthenticationHeader(response);
          
          updateViaHeader(jainResponse);
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { getRequest()};
                c_logger.error(
                    "error.create.response",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }

        if (c_logger.isTraceDebugEnabled()) {
  			c_logger.traceExit(this, "createOutgoingResponse", response);
  		}
        return response;
     }
            
		/**
     * Helper method which updates Via header and used by all extended classes
     * when creating the response
     * @param jainResponse
     * @throws SipParseException
     */
    protected void updateViaHeader(Response jainResponse) throws SipParseException{
            //Z Patch 
            //If we have a SLSP in front of the container then we will push the session 
            //identifier into the via for the SLSP to identify the connection/channel
            //that the response was received. This is only needed in the Z environment
            //in order to match channels to logical names. 
            if(c_sipContainer.isUsingExternalRouter()) {
      	TransactionUserWrapper tUser = getTransactionUser();
            	if(tUser != null)
            	{
            		ViaHeader viaH = (ViaHeader) jainResponse.getHeader(ViaHeader.name, true);
            		viaH.setParameter(TransactionUserImpl.SESSION_RR_PARAM_KEY, tUser.getId());
            	}
            }
        }

		/**
     * Helper method which adds AuthenticationHeader to the response
     * @param response
     */
    protected void addAuthenticationHeader(OutgoingSipServletResponse response) {
    	 //Add Authentication-Info header if needed
      if(m_authInfoHeaderData!=null){
      	response.addHeader(AUTHENTICATION_INFO_HEADER_NAME,m_authInfoHeaderData);
            }
        }

    /**
     * @see javax.servlet.sip.SipServletRequest#createCancel()
     */
    public SipServletRequest createCancel() 
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "createCancel",
                "Can not cancel an incoming request");
        }
        throw new IllegalStateException("Can not cancel an incoming request");
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getTransport()
     */
    public String getTransport()
    {
    	return getTransportInt();
    }

    private SipURL getUriForParams()
    {
        //	   From the SIP Servlet API 1.0 section 6.6.1:
        //	   For initial requests where the application is invoked because one of its rules matched, the
        //	   parameters are those present on the request URI, if this is a SIP or a SIPS URI. For other URI
        //	   schemes, the parameter set is undefined.
        //	   For initial requests where a preloaded Route header specified the application to be invoked,
        //	   the parameters are those of the SIP or SIPS URI in that Route header.
        if (isInitial())
        {
            if (getRequestURI().isSipURI())
            {
                SipURIImpl uri = (SipURIImpl) getRequestURI();
                return uri.getJainSipUrl();
            }
        }
        else
        {
            //  For subsequent requests in a dialog, the parameters presented to the application are those that
            //	   the application itself set on the Record-Route header for the initial request or response (see
            //	   section 8.4). These will typically be the URI parameters of the top Route header field but if the
            //	   upstream SIP element is a "strict router" they may be returned in the request URI (see RFC
            //	   3261). It's the containers responsibility to recognize whether the upstream element is a strict
            //	   router and determine the right parameter set accordingly.
            try
            {
                RequestImpl request = (RequestImpl) getJainSipMessage();
                HeaderIterator i = request.getRecordRouteHeaders();
                if (null != i && i.hasNext())
                {
                    RecordRouteHeader rrHeader = (RecordRouteHeader) i.next();
                    if (null != rrHeader)
                    {
                        SipURL url = (SipURL) rrHeader.getNameAddress().getAddress();
                        return url;
                    }
                }
                else
                {
                	//if this is 289 the first route was already popped and we need to use it
                	if (_poppedRoute != null){
                		SipURL url = (SipURL)((AddressImpl) _poppedRoute).getNameAddressHeader().getNameAddress().getAddress();
                		return url;
                	}
                	
                    i = request.getRouteHeaders();
                    if (null != i && i.hasNext())
                    {
                        RouteHeader rHeader = (RouteHeader) i.next();
                        if (null != rHeader)
                        {
                            SipURL url = (SipURL) rHeader.getNameAddress().getAddress();
                            return url;
                        }
                    }
                }
            }
            catch (HeaderParseException e)
            {
                log("getParameter", "can not get parameter", e);
            }
            catch (NoSuchElementException e)
            {
                log("getParameter", "can not get parameter", e);
            }
        }
        return null;
    }
    
    /**
     * @see javax.servlet.sip.SipServletMessage#getParameter(String)
     */
    public String getParameter(String name)
    {
    	SipURL url = getUriForParams();
    	if (null != url)
    	{
    		return url.getParameter(name);
    	}
    	return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameterMap()
     */
    public Map getParameterMap()
    {
    	Map parameterMap = null;
    	
    	SipURL url = getUriForParams();
    	if (null != url)
    	{
    		if (url.hasParameters())
    		{
    			parameterMap = new HashMap();
    			
    			Iterator i = url.getParameters();
    			while (i.hasNext())
    			{
    				String name = (String)i.next();
    				String[] value = new String[1];
    				value[0] = url.getParameter(name);
    				parameterMap.put(name,value);
    			}
    		}
    	}
    	return parameterMap;
    }

    private static Enumeration iteratorToEnumeration(final Iterator i)
    {
		return new Enumeration() 
		{
			public boolean hasMoreElements() {
				return i.hasNext();
			}

			public Object nextElement() {
				return i.next();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
 
    /**
	 * @see javax.servlet.ServletRequest#getParameterNames()
	 */
    public Enumeration getParameterNames()
    {
    	Enumeration parameterNames = null;
    	
    	SipURL url = getUriForParams();
    	if (null != url)
    	{
    		if (url.hasParameters())
    		{
        		parameterNames = iteratorToEnumeration(url.getParameters());
    		}
    	}
    	return parameterNames;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
     */
    public String[] getParameterValues(String name)
    {
    	// Uri: I don't think that SIP URIs support multiple values per param
    	// so I just returned the single value.
    	String[] parameterValues = null;
    	SipURL url = getUriForParams();
    	if (null != url)
    	{
        	parameterValues = new String[1];
        	parameterValues[0] = url.getParameter(name);
    	}
    	return parameterValues;
    }

    /**
     * Helper function - Adds to the outgoing response a contact header
     */
//    private void addContactHeader(Response res)
//        throws IllegalArgumentException, SipParseException
//    {
//        if (c_logger.isTraceEntryExitEnabled())
//        {
//            c_logger.traceEntry(this,"addContactHeader");
//        }
//
//        if (res.getHeader(ContactHeader.name, true) != null)
//        {
//            if (c_logger.isTraceDebugEnabled())
//            {
//                c_logger.traceDebug(
//                    this,
//                    "addContactHeader",
//                    "Can't addContactHeader, contact exist");
//            }
//
//            return;
//        }
//
//        // This if block will be skipped if the server is stand alone.
//        if (getMultihomeContactHeader (res) == null)
//    	    createAndSetContactHeader(res, true);
//
//        if (c_logger.isTraceEntryExitEnabled())
//        {
//            c_logger.traceExit(this,"addContactHeader");
//        }
//    }
    
    
    /*
	 * This block of code ensures that the correct contact header is specified when running in a 
	 * multi-home environment. We always need to be sure to specify the interface for the contact that 
	 * the initial request was received on. 
     */
//    private ContactHeader getMultihomeContactHeader (Response res)
//    				throws IllegalArgumentException, SipParseException
//    {
//    	ContactHeader contactHeader = null;
//	
//	    //	First check to see if there are multiple interfaces for the given transport. If not, we can allow
//	    //	the base implementation of createAndSetContactHeader to handle this request.
//	    String transport = getTransport();
//	
//	    // This if block will be skipped if the server is stand alone.
//	    if (SipProxyInfo.getInstance().getNumberOfInterfaces(transport) > 1)
//	    {
//	        if (c_logger.isTraceDebugEnabled())
//	        {
//	            c_logger.traceDebug(this, "addContactHeader","Multi-home response detected. Pulling contact header from received on interface");
//	        }
//	    	
//	        //	Here we grab the interface that the initial request was received on. This is the interface we will need to 
//	    	//	use for the outgoing contact.
//	    	SipURI receivedOnInterfaceURI = SipProxyInfo.getInstance().extractReceivedOnInterface(this);
//	     	
//	    	if(receivedOnInterfaceURI != null){
//		        String host = receivedOnInterfaceURI.getHost(); 
//		        int port = receivedOnInterfaceURI.getPort(); 
//		
//		        SipURL sipUrl = getAddressFactory().createSipURL(host);
//		        sipUrl.setPort(port);
//		        sipUrl.setTransport(transport);
//		
//		        setContactScheme(sipUrl);
//		        
//		        //we get the address for the contact header
//		        NameAddress address = getAddressFactory().createNameAddress(sipUrl);
//		        //we create the contact header from the address  
//		        contactHeader =  getHeadersFactory().createContactHeader(address);
//		        //we set the header in the given message  
//		        res.setHeader(contactHeader, true);
//		
//		        if (c_logger.isTraceDebugEnabled())
//		        {
//		            c_logger.traceDebug(this, "addContactHeader","Response contact: " + contactHeader.toString());
//		        }
//	    	}
//	    	else{
//	    		if (c_logger.isTraceDebugEnabled())
//		        {
//		            c_logger.traceDebug(this, "addContactHeader","Failed to find receivedOnInterfaceURI");
//		        }
//	    	}
//	    	
//	    }
//	    
//	    return (contactHeader);
//    }
    
	/**
	 * @param infoHeaderData The m_authInfoHeaderData to set.
	 */
	public void setAuthInfoHeaderData(String infoHeaderData) {
		m_authInfoHeaderData = infoHeaderData;
	}
	
	/**
     * 
     * @param isCommited
     * @return
     */
    protected void updateUnCommittedMessagesList(boolean isCommited){
    	TransactionUserWrapper transactionUser = getTransactionUser();
    	
    	if(!getMethod().equals(Request.ACK)){
    	  //Incomming ACK request should not be stored in the pending messages as 
    		//it never will be committed and removed from there
    	if(isCommited){
    		transactionUser.removeB2BPendingMsg(this, UAMode.UAS);
    	} else {
    		transactionUser.addB2BPendingMsg(this, UAMode.UAS);
    	}
    }
    	else{
    		if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "updateUnCommittedMessagesList",
            		"Incomming ACK request should not be stored in the pending messages");
        }
    	}
    }
    
    /**
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#createContactHeader()
     */
    protected ContactHeader createContactHeader() throws SipParseException {
    	// This is not relevant to the message context
    	return null;
    }

    /**
     * @see javax.servlet.sip.SipServletRequest#setRoutingDirective(SipApplicationRoutingDirective, SipServletRequest)
     */
	public void setRoutingDirective(SipApplicationRoutingDirective directive, SipServletRequest origRequest) {
		throw new IllegalStateException("This is an Incoming request - can not set routing directive");
	}



	/**
     * @see javax.servlet.sip.SipServletRequest#getB2buaHelper()
     */
    public B2buaHelper getB2buaHelper() throws IllegalStateException {
    	return getB2buaHelper(true,UAMode.UAS);
    }
	
    @Override
    protected boolean shouldCreateContactIfNotExist() {
    	return false;
    }

    /**
     * When application router throws some unknown exception we should
     * process 500 response for IncomingRequest 
     * @param request
     */
    public void processCompositionErrorResponse(){
    	SipRouter.sendErrorResponse(this, SipServletResponse.SC_SERVER_INTERNAL_ERROR);
    }
    
	/**
	 * Used in Outgoing request only
	 */
	public String getAppInvokedName() {
		return null;
	}

	/**
	 * Used in Outgoing request only
	 */
	public void setAppInvokedName(String appInvokedName) {
	}

	/**
	 * Incoming request is treated always as 289 request
	 */
	public boolean isAppInvoked116Type() {
		return false;
	}

	
    /**
	 * @see javax.servlet.sip.SipServletMessage#getLocalAddr()
	 */
	@Override
	public String getLocalAddr()
	{
		// @return null if it was locally generated.
		if (_isInternallyGenerated) {
			return null;
		}
		return getLocalAddrInt();
	}

    /**
     * @see javax.servlet.sip.SipServletMessage#getLocalPort()
     */
	@Override
    public int getLocalPort()
    {
		// @return -1 if it was locally generated.
		if (_isInternallyGenerated) {
			return -1;
		}
    	return getLocalPortInt();
    }

	/**
     * @see javax.servlet.sip.SipServletMessage#getRemoteAddr()
     */
    @Override
    public String getRemoteAddr()
    {
    	// @return null if it was locally generated.
		if (_isInternallyGenerated) {
			return null;
		}
    	return getRemoteAddrInt();
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getRemotePort()
     */
	@Override    
    public int getRemotePort()
    {
		// @return -1 if it was locally generated.
		if (_isInternallyGenerated) {
			return -1;
		}
    	return getRemotePortInt();
    }

	@Override
	public String getInitialRemoteAddr() 
	{
		return getInitialRemoteAddrInt();
	}

	@Override
	public int getInitialRemotePort() 
	{
		return getInitialRemotePortInt();
	}
	
	@Override
	public String getInitialTransport() 
	{
		return getInitialTransportInt();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.servlets.SipServletRequestImpl#cleanExpiredCompositionHeaders()
	 */
	public void cleanExpiredCompositionHeaders(){
		//only relevant for outgoing request
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

