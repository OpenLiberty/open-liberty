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

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.TransactionDoesNotExistException;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.RequireHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;

import javax.servlet.sip.Address;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.UAMode;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RSeqHeader;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;
import com.ibm.ws.sip.container.proxy.StatefullProxy;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.transaction.ServerTransaction;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;

/**
 * @author Amir Perlman, Feb 24, 2003
 * Out Going Servlet Response. The respnose was generated locally and will be 
 * sent to the remote party. 
 */
public class OutgoingSipServletResponse extends SipServletResponseImpl
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -5804127531338290805L;
	
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger =
        Log.get(OutgoingSipServletResponse.class);
   
    
    /**
     * Defines if the response was sent reliably or not.
     */
    private transient boolean m_sendingReliably = false;
        
    /**
     * Flag indicating whether the response was genearated by  a proxy or
     * by the application itself. 
     */
    private boolean m_isProxyResponse = false;
    
    /**
     * The flag indicating the response is 
     * from a virtual ProxyBranch
     */
    private boolean m_isOfVirtualProxyBranch = false;


    /**
     * public no-arg constructor to satisfy Externalizable.readExternal()
     */
    public OutgoingSipServletResponse() {
    }
      
   /**
     * Constructor for Out going Servlet Response.
     * @param response
     * @param transactionId
     * @param provider
     */
    public OutgoingSipServletResponse(
        Response response,
        SipServletRequestImpl request)
    {
        super(response, request.getTransactionId(), request.getSipProvider());

        //Set the request associated with this response. 
        setRequest(request);
        setTransaction(request.getTransaction());
        setIsCommited(false);
    }

    /**
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#getLocalParty()
     */
    @Override
	Address getLocalParty()
    {
        return getFrom();
    }

    /**
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#getRemoteParty()
     */
    @Override
	Address getRemoteParty()
    {
        return getTo();
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#send()
     */
	public synchronized void send() throws IOException
    {
    	if (!isLiveMessage("send"))
    		return;
        
    	// TODO during code review, why we send here an IOException ?
    	if(isCommitted())
	    {
    		if(isJSR289Application()){
    			throw new IllegalStateException(
    			"Can not modify committed message");
    		} else {
                String err =
                    "Can not Send, Operation not allowed on a committed Response";
                if (c_logger.isTraceDebugEnabled())
                {
                    c_logger.traceDebug(this, "send", err);

                }
                throw new IOException(err);
    		}
	    }

        if(!m_isProxyResponse && getStatus()>100 && getStatus()<200 && shouldBeSentReliably()){
            throw new IllegalStateException("This response should be sent reliably" + this);
        }
        
        continueToSend();
    }
	
	/**
	 * Replaces the behavior of createResponseToOriginal by createResponse such that the 
	 * response is sent on the original session rather than on the derived session.
	 * 
	 * The replacement is done in order some tests in TCK_SIP_SERVLET_1_1 would pass.
	 * The tests are:
	 * spec/ApplicationRouterTest/testApplicationRouterCase1, testApplicationRouterCase3 - 
	 * it does not link the sessions before sending but after it.
	 * The UAS servlet is found at sipservlet-1_1-tck.zip\tck\applications\spectestapps\ar-continue\src\com\bea\sipservlet\tck\apps\spec\ar\cont\MainServlet.java
	 *  
	 * spec/B2buaHelperTest/testB2buaHelper - 
	 * it does not link the sessions but only sending. In addition, it checks the number of pending messages for the original session.
	 * The UAS servlet is found at sipservlet-1_1-tck.zip\tck\applications\spectestapps\b2bua\src\com\bea\sipservlet\tck\apps\spec\b2bua\B2buaHelperServlet.java
	 * 
	 * api/B2buaHelperTest/testCreateRequest002 - 
	 * it does not link the sessions but only sending. In addition, it try to get the pending messages with linked session which is null.
	 * The UAS servlet is found at sipservlet-1_1-tck.zip\tck\applications\apitestapp\src\com\bea\sipservlet\tck\apps\api\B2buaHelperServlet.java
	 */
	private void sendOnOriginalRatherOnDerivedTCK_SIP_SERVLET_1_1() {
		
		TransactionUserWrapper transactionUser = getTransactionUser();

    	if (transactionUser != null && transactionUser.isDerived()) {
        	
        	SipServletRequestImpl req = (SipServletRequestImpl) getRequest();
        	SipSessionImplementation session = (SipSessionImplementation) getRequest().getSession(false);

    		if (req.getLinkedRequest() == null && session != null && 
    				session.getLinkedSession() == null
    				&& transactionUser.isB2B()) {
    			
    			TransactionUserWrapper derivedTransactionUser = transactionUser;
        		transactionUser = transactionUser.getOrigTUWrapper();
            	if (c_logger.isTraceDebugEnabled()){
            		c_logger.traceDebug(this,"continueToSend1", "override transactionUser");
            	}
        		setTransactionUser(transactionUser); // sets the message's TUWrapper.
        		
        		session = (SipSessionImplementation) getRequest().getSession();
        		session.setTransactionUser(transactionUser); // sets the session's TUWrapper.
        		
        		transactionUser.overridePendingMessagesByDerived(derivedTransactionUser);
    		}
    	}
	}
	

    /**
     * Helper method that helps us to continue sending from the 
     * send() and sendReliable() methods
     * @throws IOException
     */
    private synchronized void continueToSend() throws IOException {
    	resetContentLength(); 
    	
    	Response response = getResponse();

    	boolean continueSending = true;

    	if (c_logger.isTraceDebugEnabled()){
    		c_logger.traceDebug(this,"continueToSend1",getCallId() + " ,status = " + getStatus());
    	}

    	sendOnOriginalRatherOnDerivedTCK_SIP_SERVLET_1_1();
    	
    	TransactionUserWrapper transactionUser = getTransactionUser();

    	//if we have not started the proxy mode and we are send 
    	if (transactionUser != null && 
    			!transactionUser.isProxying() &&  
    			!m_isProxyResponse){
    		// we are now in UAS mode
    		transactionUser.setUASMode();
    		
        	// When the response is processed 
        	// not by proxy and the to tag is empty 
        	// we should update to tag.
        	updateToTag(response);
    	}

		try {
			SipServletRequest request = getRequest();
			// Add the contact header to a "101 and above" response to a requests
			//that generate a dialog. 
	    	
			boolean addContact = false;
			
			addContact = SipUtil.shouldAddContact(getStatus());
			
			if (addContact && SipUtil.isDialogInitialRequest(getMethod())) {
				//Do not change existing contact header if already set by the application. 
				if (transactionUser != null && !transactionUser.isProxying()) {
					if (getContactHeader() == null) {
						boolean created = createAndSetMultihomeContactHeader();

						if (!created)
							createAndSetContactHeader(getMessage(), null, true);
					} else {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(
									this,
									"addContactHeader",
							"Can't addContactHeader, contact exist");
						}
					}
				}
			}       
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SipParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	//      If the request is sending reliably it can be only that provisional response
    	//      forward it to the Transaction User first.
    	if(isSendingReliably()){
    		transactionUser.onSendingReliableProvisionalResponse(this);
    	}
    	//      if this is a final response and reliable responses were sent over this dialog before
    	//      the Transaction user should be updated about this and if this response
    	//      is not allowed ( in case there some reliable response with SDP that is
    	//      still waiting for the PRACK ) the exception will be thrown.
    	//      this is not relevant to the 200 OK on the Cancel that  needs to sent out anyway
    	else if(transactionUser.wasAnsweredReliable() && getStatus() >= 200 && Request.INVITE.equals(getMethod())){
    		transactionUser.onSendingFinalResponseAfterProvisional(this);
    	}

    	//According to JSR 116, 8.2.2 .... 
    	//"Once a request has been proxied, the application will not usually 
    	//generate a final response of its own. However, there are cases where 
    	//being able to do so is useful and so it's allowed but with some
    	//additional constraints." For complete details see the JSR
    	boolean isProxy = transactionUser.isProxying();
    	StatefullProxy proxy = ((StatefullProxy)getProxy()); 
    	if(proxy != null){//since getProxy will mark this flag as true, we will reset it to what it was
    		transactionUser.setIsProxying(isProxy); //for the case that the app generated a final response to 
    	}											//a proxied request. In which case we want to deliver the response in 
    												//UAS mode.
    	if(!m_isProxyResponse && null != proxy 
    			&& null != transactionUser && isProxy 
    			&& !isOfVirtualProxyBranch() /*if its already marked as virtual, then we already been through
    			 							  the process of selecting best response, and chose the application generated one. 
    										  we need to forward the message upstream this time, and not get through the process again*/
    			){
    		//mar the message as a message of a virtual branch
    		markAsOfVirtualProxyBranch();
    		//this assignment is needed here
    		//because it does not happen for virtual proxy branch flow
    		//as it does in actual proxy flow
    		TransactionUserWrapper tu = transactionUser.createDerivedTU(getResponse(),
    		" OutgoingSipServletResponse - VirutalBranch causes to Derived Session");
    		setTransactionUser(tu);
    		
    		//adding the contact header - the container should generate the contact header for virtual branch response 
    		try {
				createAndSetContactHeader(response, null, true);
			} catch (SipParseException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "continueToSend",	"Exception while adding contact header: " + e.getLocalizedMessage());
				}
			}
    		//Update the TO tag to be a related TU id.
    		updateToTag(getResponse());
    		
    		tu.setIsVirtualBranch(response);
    		tu.setIsRRProxying(proxy.getRecordRoute());
    		//Call the proxy and check the response code to determine whether
    		//the proxy will handle this or should we continue normal processing
    		continueSending = proxy.processApplicationRespone(this);
    		
    		if (continueSending && getTransaction().isTerminated()){
    			if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "continueToSend",	"The original server transaction is closed, This response will be sent directly over the SipStack.");
				}
    			setShouldBeSentWithoutST(true);
    		}
    	}
    	
    	if(continueSending)
    	{
    		if (c_logger.isTraceDebugEnabled()){
    			c_logger.traceDebug(this,"continueToSend2",getCallId() + " ,status = " +getStatus());
    		}

    		if (shouldBeSentWithoutST()) {
    			if (c_logger.isTraceDebugEnabled()) {
    				c_logger
    				.traceDebug(this, "continueToSend",
    				"This response will be sent directly over the SipStack.");
    			}
    			getTransactionUser().onSendingResponse(this);
    			// Update the performance manager about new outgoing response
    			PerformanceMgr perfMgr = PerformanceMgr.getInstance();
    			if (perfMgr != null) {
    				perfMgr.responseSent(((SipServletRequestImpl) getRequest())
    					.getArrivedTime());
    			}
    			SipRouter.sendResponseDirectlyToTransport(getSipProvider(),
    					getResponse(), false);
    		}
    		else{
    			// Send the response through the transaction to allow listener to
    			//intercept the event. 
    			ServerTransaction serverTransaction = ((ServerTransaction) getTransaction());
    			
    			// in proxy case the response maybe sent on a different TU than the original TU
    			// for derived sessions. the TU listener of the transaction is changed to the current
    			// TU.
    			if (isProxy){
    				TransactionUserWrapper listener = (TransactionUserWrapper) serverTransaction.getServerTransactionLisener();
    				if (listener != null && listener != getTransactionUser()){
    					serverTransaction.setTransactionListener(getTransactionUser());
    					if (c_logger.isTraceDebugEnabled()) {
    						c_logger.traceDebug(this, "continueToSend",	"The response server transaction listener is changed to: " + getTransactionUser());
    					}
    				}
    			}
    			
    			serverTransaction.sendResponse(this);

    		}
    		setIsCommited(true);
    	}
    }
    
    
    /*
	 * This block of code ensures that the correct contact header is specified when running in a 
	 * multi-home environment. We always need to be sure to specify the interface for the contact that 
	 * the initial request was received on. 
     */
    private boolean createAndSetMultihomeContactHeader() throws IllegalArgumentException, SipParseException
    {
    	boolean created = false;

    	//	First check to see if there are multiple interfaces for the given transport. If not, we can allow
    	//	the base implementation of createAndSetContactHeader to handle this request.
    	SipServletRequest request = getRequest();
    	String transport = request.getTransport();

    	// This if block will be skipped if the server is stand alone.
    	if (SipProxyInfo.getInstance().getNumberOfInterfaces(transport) > 1)
    	{
    		if (c_logger.isTraceDebugEnabled())
    		{
    			c_logger.traceDebug(this, "addContactHeader","Multi-home response detected. Pulling contact header from received on interface");
    		}

    		//	Here we grab the interface that the initial request was received on. This is the interface we will need to 
    		//	use for the outgoing contact.
    		SipURI receivedOnInterfaceURI = SipProxyInfo.getInstance().extractReceivedOnInterface(request);

    		if(receivedOnInterfaceURI != null){
    			String host = receivedOnInterfaceURI.getHost(); 
    			int port = receivedOnInterfaceURI.getPort(); 

    			SipURL sipUrl = getAddressFactory().createSipURL(host);
    			sipUrl.setPort(port);
    			sipUrl.setTransport(transport);

    			setContactScheme(sipUrl);

    			//we get the address for the contact header
    			NameAddress address = getAddressFactory().createNameAddress(sipUrl);
    			//we create the contact header from the address  
    			ContactHeader contactHeader =  getHeadersFactory().createContactHeader(address);
    			//we set the header in the given message

    			Message res = getMessage();

    			res.setHeader(contactHeader, true);

    			created = true;
    			if (c_logger.isTraceDebugEnabled())
    			{
    				c_logger.traceDebug(this, "addContactHeader","Response contact: " + contactHeader.toString());
    			}

    		}
    		else{
    			if (c_logger.isTraceDebugEnabled())
    			{
    				c_logger.traceDebug(this, "addContactHeader","Failed to find receivedOnInterfaceURI");
    			}
    		}

    	}

    	return created;
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
            
            SipServletRequest request = getRequest();
            
            if (request.getRequestURI() != null && request.getRequestURI().getScheme().equalsIgnoreCase("sips")) {
                // request URI is secure
                secure = true;
            }
            else {
            	try {
            		Address topRoute = request.getAddressHeader("Route");
	                if (topRoute != null) {
		                String scheme = topRoute.getURI().getScheme();
		                if (scheme.equalsIgnoreCase("sips")) {
		                    secure = true;
		                }
		            }					
				} catch (ServletParseException e) {
	                if (c_logger.isErrorEnabled()) {
	                    c_logger.error("error.send.response", Situation.SITUATION_REQUEST, this, e);
	                }
				}
            }
            
            if (secure) {
                sipUrl.setScheme("sips");
            }
        }
	}    
    
    /**
     * Helper function that will test if the original request contains Require
	 * header and the response should be sent reliably
	 * 
	 * @return
     */
    private boolean shouldBeSentReliably() {
        IncomingSipServletRequest req = (IncomingSipServletRequest)getRequest();
        return req.getShouldBeAnsweredReliable();
    }
    
    /**
     * Sets the To tag of the response. 
     * @param response
     */
    private void updateToTag(Response response) throws IOException 
    {
    	if (!isLiveMessage("updateToTag"))
    		return;
        
        String toTag = response.getToHeader().getTag();

        if (null == toTag || toTag.length() == 0)
        {
            TransactionUserWrapper transactionUser = getTransactionUser();
            
            //Try to use the tag from the session if available
            AddressImpl toAddr = (AddressImpl) transactionUser.getLocalParty();

            if (null != toAddr)
            {
                toTag = toAddr.getTag();
            }

            if (null == toTag || toTag.length() == 0)
            {
                //Still no tag so generate a new tag
                toTag = transactionUser.generateLocalTag();
            }

            //Assign a To tag prior to sending so we can map session ids before
            // the responses are actually sent. 
            try
            {
                response.getToHeader().setTag(toTag);
            }
            catch (IllegalArgumentException e)
            {
                if (c_logger.isErrorEnabled())
                {
                    Object[] args = { this };
                    c_logger.error(
                        "error.send.response",
                        Situation.SITUATION_REQUEST,
                        args,
                        e);
                }

                throw (new IOException(e.getMessage()));
            }
            catch (SipParseException e)
            {
                if (c_logger.isErrorEnabled())
                {
                    Object[] args = { this };
                    c_logger.error(
                        "error.send.response",
                        Situation.SITUATION_REQUEST,
                        args,
                        e);
                }

                throw (new IOException(e.getMessage()));
            }

        }
    }

    /**
     * Does the actual sending of the repsponse. 
     */
    public void sendImpl() throws IOException
    {
    	if (!isLiveMessage("sendImpl"))
    		return;
        
        try
        {
            //Update the performance manager about new outgoing response
        	PerformanceMgr perfMgr = PerformanceMgr.getInstance();
    		if (perfMgr != null) {
    			perfMgr.responseSent(((SipServletRequestImpl)getRequest()).getArrivedTime());
    		}
            getSipProvider().sendResponse(getTransactionId(), getResponse());
        }
        catch (TransactionDoesNotExistException e)
        {
            if (c_logger.isTraceDebugEnabled()) {
            	// do not print the exception, it creates too much logging noise in
            	// a legitimate scenario. for example, if a proxy app forwards
            	// the 200 before the 180, due to thread timing in message dispatching.
                c_logger.traceDebug("error sending response. "
                	+ "no transaction for ID [" + getTransactionId()
                	+ "] top via [" + getTopVia()
                	+ "] code [" + getStatus() + ']');
            }
            logExceptionToSessionLog(SipSessionSeqLog.ERROR_SEND_RESP, e);
        }
        catch (SipException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { this };
                c_logger.error(
                    "error.send.response",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
            logExceptionToSessionLog(SipSessionSeqLog.ERROR_SEND_RESP, e);
            throw (new IOException(e.getMessage()));
        }
    }
        
    /**
     * @see javax.servlet.sip.SipServletResponse#sendReliably()
     */
    @Override
	public void sendReliably() throws Rel100Exception
    {
        if (!isLiveMessage("sendReliably")){
            return;
        }
        
        if(isCommitted())
	    {
	        throw new IllegalStateException(
	           "Can not modify committed message");
	    }

        if(getStatus() <= 100 || getStatus()>=200){
            // it is not provisional response so it can't be sent reliably
            String err =
                "Can not Send, Operation not allowed on not provisional Response";
            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(this, "send", err);
            }
            throw new Rel100Exception(Rel100Exception.NOT_1XX); 
        }
        
        else if (!canBeSentReliably()){
            throw new Rel100Exception(Rel100Exception.NO_REQ_SUPPORT);
        }

        markAsSentReliably();

        TransactionUserWrapper transactionUser = getTransactionUser();

        long rSeq = transactionUser.getNextRSegNumber();
        
        Header header;
        try {
            header = getHeadersFactory().createHeader(RSeqHeader.name, Long.toString(rSeq));
            getResponse().setHeader(header, false);
            
            header = getHeadersFactory().createHeader(RequireHeader.name, 
                                                      ReliableResponse.RELIABLY_PARAM);
            getResponse().addHeader(header, false);
            
            continueToSend();
            
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage() + this);
           } 
        catch (SipParseException e) {
            throw new IllegalStateException(e.getMessage() + this);
        }
        catch (IOException e) {
            throw new IllegalStateException(e.getMessage() + this);
        }
    }

    /**
     * @see javax.servlet.sip.SipServletResponse#createAck()
     */
    @Override
	public SipServletRequest createAck()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "createAck",
                ""
                    + "Create Ack Operation not allowed on an outgoing response");

        }
        
        throw new IllegalStateException("Can not create ACK for a locally generated request");
        
    }
    
    /**
     * @see javax.servlet.sip.SipServletResponse#createPrack()
     */
	public SipServletRequest createPrack() throws Rel100Exception {
		// can't create PRACK for outgoing response.
		// throw the most relevant exception.
		String method = getMethod();
		if (method == null || !method.equals(Request.INVITE)) {
    		throw new Rel100Exception(Rel100Exception.NOT_INVITE);
		}
    	if (!isReliableResponse()) {
    		throw new Rel100Exception(Rel100Exception.NOT_100rel);
    	}
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(
                this,
                "createPrack",
                "Create Prack Operation not allowed on an outgoing response");

        }
        throw new IllegalStateException("Can not create PRACK for a locally generated response");
    }
        
    /**
     * Determines if the response is a 2xx response. 
     * @param response
     * @return
     */
    public boolean isNon2xxResponse() 
    {
        Response response = getResponse();
        boolean rc = false;
        int status = 0;
        try {
            status = response.getStatusCode();
        }
        catch (SipParseException e) 
        {
            logException(e);
        }
        if(status < 200 || status >= 300)
        {
             rc = true; 
        }
        
        return rc;
    }
    
    /**
     * Marks the response as response genearated by a proxy. 
     * @param isBranch
     */
    public void markAsProxyResponse()
    {
    	if (!isLiveMessage("MarkAsPeoxyResponse"))
    		return;
        
        m_isProxyResponse = true;
    }
    
    
    /**
     * Set the  m_sendingReliably flag to be true
     */
    public void markAsSentReliably() {
        m_sendingReliably = true;
    }
    
    
    /**
     * @return Returns the m_sendingReliably.
     */
    public boolean isSendingReliably() {
        return m_sendingReliably;
    }

    /**
     * Returns the flag indicating if the best response received so far
     * is of a virtual branch
     * 
     * @return the best response
     */
    public void markAsOfVirtualProxyBranch(){
        m_isOfVirtualProxyBranch = true;
    }
    
    /**
     * Returns the flag indicating if the best response received so far
     * is of a virtual branch
     * 
     * @return the best response
     */
    public boolean isOfVirtualProxyBranch(){
        return m_isOfVirtualProxyBranch;
    }
    


    /**
	 * Overrides the method in SipServletMessage to add the ability to get 
	 * Session by the To tag that is usefull in the Derived Session state 
	 * @see javax.servlet.sip.SipServletMessage#getSession(boolean)
	 * @param create
	 * @return
	 */
    @Override
	public SipSession getProxySession(boolean create) {
    	//TODO was not supposed to be executed here! we need to understand
    	//why we create and invoke to a servlet an outgoing response (on doresponse)
    	//and not an incomnig one
		return getTransactionUser().getSipSession(create);
	}
    
    /**
     * 
     * @param isCommited
     * @return
     */
    @Override
	protected void updateUnCommittedMessagesList(boolean isCommited){
    	//we should not handle b2b issues if we're a proxy
    	if (m_isProxyResponse){
            //we exit
    		return;
    	}
    	TransactionUserWrapper transactionUser = getTransactionUser();
    	if(isCommited){
    		transactionUser.removeB2BPendingMsg(this, UAMode.UAS);
    	} else {
    		transactionUser.addB2BPendingMsg(this, UAMode.UAS);
    	}
    }
    
    @Override
    protected boolean shouldCreateContactIfNotExist() {
    	 if (c_logger.isTraceDebugEnabled())
         {
             c_logger.traceDebug(this, "shouldCreateContactIfNotExist", "m_isProxyResponse=" + m_isProxyResponse +
            		 ", m_isOfVirtualProxyBranch=" + m_isOfVirtualProxyBranch);

         }
    	if(m_isOfVirtualProxyBranch) return true;
    	
    	return !m_isProxyResponse;
    }
}
