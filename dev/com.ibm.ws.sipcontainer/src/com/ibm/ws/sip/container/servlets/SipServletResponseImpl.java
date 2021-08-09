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
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.RequireHeader;
import jain.protocol.ip.sip.header.SecurityHeader;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.TooManyHopsException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.proxy.ProxyBranchImpl;
import com.ibm.ws.sip.container.transaction.ClientTransaction;
import com.ibm.ws.sip.container.transaction.ClientTransactionListener;
import com.ibm.ws.sip.container.transaction.SipTransaction;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.security.auth.DigestConstants;
import com.ibm.wsspi.webcontainer.servlet.IServletResponseWrapper;

/**
 * @author Amir Perlman, Feb 16, 2003
 *
 * Implementation for the Sip Servlet Response API. 
 * @see javax.servlet.sip.SipServletResponse
 */
public abstract class SipServletResponseImpl
    extends SipServletMessageImpl
    implements SipServletResponse, IServletResponseWrapper
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger =
        Log.get(SipServletResponseImpl.class);

    /**
     * The Sip Servlet Request associated with this response. 
     */
    private SipServletRequest m_sipServletRequest;

    /**
     * public no-arg constructor to satisfy Externalizable.readExternal()
     */
    public SipServletResponseImpl() {
    }

    /**
     * This flag is true when we are creating response that should be sent
     * over the stack as is doesn't relates to the ServerTransaction 
     * Used for Derived Sessions.
     */
    private boolean _shouldBeSentWithoutST = false;

    /**
     * the UnsupportedEncodingException that was caught by the last call to
     * {@link #setCharacterEncoding(String)} or null if last call was successful.
     * this exception is not thrown from setCharacterEncoding, but instead it is
     * thrown from the following call to {@link #setContent(Object, String)}
     * or {@link #getContent()}.
     */
    private transient UnsupportedEncodingException m_unsupportedEncodingException = null;

    /**
     * indicates if this response is sent to the application as a intermediate proxy response according
     * to JSR289
     */
    private transient boolean _isBranchResponse = false;
    /**
     * Constructs a new Sip Servlet Response
     * @param The Jain Sip Response associated with this object. 
     * @param transactionId transaction id associated with this request. 
     * @param provider The Sip Provider that will be used for generating 
     * responses and acknowledgements to the request. 
     */
    public SipServletResponseImpl(
        Response response,
        long transactionId,
        SipProvider provider)
    {
        super(response, transactionId, provider);
    }

    /**
     * @see javax.servlet.sip.SipServletResponse#getReasonPhrase()
     */
    public String getReasonPhrase()
    {
        String rValue = null;
        try
        {
            rValue = getResponse().getReasonPhrase();
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { this };

                c_logger.error(
                    "error.get.reason.phrase",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }

        return rValue;
    }

    /**
     * @see javax.servlet.sip.SipServletResponse#getRequest()
     */
    public SipServletRequest getRequest()
    {
        return m_sipServletRequest;
    }

    /**
     * @see javax.servlet.sip.SipServletResponse#getStatus()
     */
    public int getStatus()
    {
        int rValue = -1;
        try
        {
            rValue = getResponse().getStatusCode();
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { this };

                c_logger.error(
                    "error.get.status",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }

        return rValue;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getMethod()
     */
	public String getMethod()
    {
    	return m_sipServletRequest == null
			? null
			: m_sipServletRequest.getMethod();
    }

    /**
     * @see javax.servlet.sip.SipServletResponse#setStatus(int, java.lang.String)
     */
    public void setStatus(int statusCode, String reasonPhrase) {
    	setStatus(statusCode);

    	Response response = getResponse();
        try {
            response.setReasonPhrase(reasonPhrase);
        }
        catch (SipParseException e) {
        	throw new IllegalArgumentException("bad reason phrase ["
        		+ reasonPhrase + ']', e);
        }
    }

    /**
     * @see javax.servlet.sip.SipServletResponse#setStatus(int)
     */
    public void setStatus(int statusCode)
    {
    	// the javadoc doesn't say anything about exceptions, but the TCK
    	// expects IllegalArgumentException for bad status code

    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }

    	Response response = getResponse();
        try {
            response.setStatusCode(statusCode);
        }
        catch (SipParseException e) {
        	throw new IllegalArgumentException("bad status code ["
        		+ statusCode + ']', e);
        }
    }

    /**
     * Overrides the SipServletMessageImpl#checkIsSystemContactHeader and perform check
     * relate to the Request message
     * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#checkIsSystemContactHeader()
     */
    @Override
	protected boolean checkIsSystemContactHeader()
    {
    	boolean isSystemHeader = true;
        
//    	Applications must not add, delete, or modify socalled "system"
        // headers. These are header fields that the servlet container
        // manages: From, To, Call-ID, CSeq, Via, Route (except through
        // pushRoute), Record-Route. Contact is a system header field in
        // messages other than REGISTER requests and responses, as well as
        // 3xx and 485 responses. Additionally, for containers implementing
        // the reliable provisional responses extension, RAck and RSeq are
        // considered system headers also.
        if(((getStatus()== SipServletResponse.SC_AMBIGUOUS )||
                (getStatus()>=300 &&getStatus()<400 )||
                (getMethod().equals(Request.REGISTER))
                || (getMethod().equals(Request.OPTIONS) && getStatus()== SipServletResponse.SC_OK)))
        {
        	isSystemHeader = false;
        }
        
        return isSystemHeader;
    }

    /**
    * @see javax.servlet.sip.SipServletResponse#sendReliably()
    */
    public abstract void sendReliably() throws Rel100Exception;

    /**
     * @see javax.servlet.sip.SipServletResponse#createAck()
     */
    public abstract SipServletRequest createAck();

    //
    //javax.servlet.ServletResponse Fucntions. 
    //
    /**
     * @see javax.servlet.ServletResponse#flushBuffer()
     */
    public void flushBuffer() throws IOException
    {
    }

    /**
     * @see javax.servlet.ServletResponse#getBufferSize()
     */
    public int getBufferSize()
    {
        return 0;
    }

    /**
     * @see javax.servlet.ServletResponse#getLocale()
     */
    public Locale getLocale()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletResponse#reset()
     */
    public void reset()
    {
    }

    /**
     * @see javax.servlet.ServletResponse#resetBuffer()
     */
    public void resetBuffer()
    {
    }

    /**
     * @see javax.servlet.ServletResponse#setBufferSize(int)
     */
    public void setBufferSize(int arg0)
    {
    }

    /**
     * @see javax.servlet.ServletResponse#setLocale(Locale)
     */
    public void setLocale(Locale arg0)
    {
    }

    //
    //Internal Utitlity Functions
    //
    /**
     * Helper function gets the Sip Response Object associated with this Servlet
     * Response. 
     */
    public Response getResponse()
    {
        return (Response) getMessage();
    }

    /**
     * Sets the Request associated with this response
     * @param request The request associated with this response. 
     */
    public void setRequest(SipServletRequestImpl request)
    {
        m_sipServletRequest = request;
    }

    /* (non-Javadoc)
     * @see javax.servlet.sip.SipServletResponse#getProxy()
     */
    public Proxy getProxy()
    {
        Proxy proxy = null;
        try
        {
            proxy = m_sipServletRequest.getProxy(false);
        }
        catch (TooManyHopsException e)
        {
            if (c_logger.isTraceDebugEnabled())
            {
                // Not suppose to get here
                c_logger.traceDebug(
                    this,
                    "getProxy",
                    "TooManyHopsException!!!");
            }
        }

        return proxy;
    }

    /**
     * Helper method that checks if the response sent reliable or no
     * @param response
     */
    public boolean isReliableResponse(){
    	if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceEntry(this,"isReliableResponse");
        }
    	boolean result = false;
    	try{
	        if (getStatus() > 100 && getStatus() < 200) {
	            HeaderIterator iter = getResponse().getHeaders(RequireHeader.name);
	            if (iter != null){
	                for (; iter.hasNext();) {
	                    try {
	                        RequireHeader header = (RequireHeader) iter.next();
	                        if (header.getValue().equals(ReliableResponse.RELIABLY_PARAM)) {
	                        	result = true;
	                            return true;
	                        }
	                    } 
	                    catch (HeaderParseException e) {
	                        if(c_logger.isErrorEnabled())
	                        {
	                            c_logger.error("error.exception", Situation.SITUATION_REPORT, null);
	                        }
	                    } 
	                    catch (NoSuchElementException e) {
	                        if(c_logger.isErrorEnabled())
	                        {
	                            c_logger.error("error.exception", Situation.SITUATION_REPORT, null);
	                        }
	                    }                
	                }           
	            }
	        }
	        return result;
    	}finally{
    		if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceExit(this,"isReliableResponse", new Boolean(result));
            }
    	}
    }
    /**
     * @see javax.servlet.sip.SipServletResponse#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        return null;
    }
    
    /**
     * @see javax.servlet.sip.SipServletResponse#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }
    
    /**
     * We need this method for WebContainer internal use
     * @see com.ibm.wsspi.webcontainer.servlet.IServletRequestWrapper#getWrappedRequest()
     */
    public ServletResponse getWrappedResponse(){
    	return getHttpServletResponse();
    }

	/**
	 * Get the first auth request header from the response, matching the given 
	 * realm.
	 * There's actually a problem with the current version of JSR 289 (at the
	 * time of writing - July 2007) which expects exactly one header. So we
	 * simply pick the first one we find (also notice that it could be either a
	 * Proxy-Authenticate or a WWW-Authenticate header).
	 * 
	 * @param wantedRealm An optional realm parameter. If provided, only auth request
	 *            headers for the given realm will be returned. null means match
	 *            any realm.
	 * @return a SecurityHeader object, or null if the response contains no
	 *         security header matching the required realm.
	 */
	public SecurityHeader getAuthHeader(String wantedRealm) {
		SecurityHeader authHeader = null;
		try {
			Response response = getResponse();
			if (getStatus() == SC_PROXY_AUTHENTICATION_REQUIRED) {
				authHeader = response.getProxyAuthenticateHeader();
				String headerRealm = 
				   authHeader.getParameter(DigestConstants.PROPERTY_REALM);
				// If a realm is defined, only return matching 
				if (wantedRealm != null && !wantedRealm.equals(headerRealm)) 
				{
					authHeader = null;
				}
					
			} else if (getStatus() == SC_UNAUTHORIZED) {
				HeaderIterator headers = response.getWWWAuthenticateHeaders();
				authHeader = getFirstDigestAuthHeader(headers, wantedRealm);
			}
			// For responses other than 401 and 407, we return null.
		} catch (SipParseException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.parse.auth.challange",
						Situation.SITUATION_REQUEST, null, e);
			}
		}
		return authHeader;
	}

	/**
	 * Return the first www-auth header with the 'digest' auth scheme, matching
	 * the provided realm.
	 * 
	 * @param headers
	 *            A HeaderIterator object pointing to SecurityHeader objects.
	 * @param wantedRealm
	 *            An optional realm parameter. If provided, only auth request
	 *            headers for the given realm will be returned. null means match
	 *            any realm.
	 * @return The first matching www-auth header for 'digest' auth scheme, or
	 *         null if none.
	 * @throws NoSuchElementException
	 * @throws HeaderParseException
	 */
	private SecurityHeader getFirstDigestAuthHeader(HeaderIterator headers, 
			String wantedRealm) 
		throws HeaderParseException
	{
		SecurityHeader digestHeader = null;
		SecurityHeader currHeader;
		String scheme;
		while (headers.hasNext())
		{
			currHeader = (SecurityHeader)headers.next();
			scheme = currHeader.getScheme();
			if (scheme.equals(DigestConstants.DIGEST))
			{
				String headerRealm = 
					currHeader.getParameter(DigestConstants.PROPERTY_REALM);
				if (wantedRealm == null || wantedRealm.equals(headerRealm))
				{
					digestHeader = currHeader;
					break;
				}
			}
		}
		return digestHeader;
	}

	/**
	 * overrides setCharacterEncoding() without throwing UnsupportedEncodingException.
	 * this is needed for compiling with ServletResponse.setCharacterEncoding()
	 * which does not declare UnsupportedEncodingException in its throws clause.
	 * 
	 * @see javax.servlet.ServletResponse#setCharacterEncoding(java.lang.String)
	 * @see javax.servlet.sip.SipServletMessage#setCharacterEncoding(java.lang.String)
	 */
	public void setCharacterEncoding(String enc) {
		try {
			super.setCharacterEncoding(enc);
			m_unsupportedEncodingException = null;
		}
		catch (UnsupportedEncodingException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setCharacterEncoding",
					"invalid encoding", e);
			}
			m_unsupportedEncodingException = e;
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#setContent(java.lang.Object, java.lang.String)
	 */
	public void setContent(Object content, String contentType)
		throws UnsupportedEncodingException
	{
		if (m_unsupportedEncodingException != null) {
			UnsupportedEncodingException e = new UnsupportedEncodingException();
			e.initCause(m_unsupportedEncodingException);
			throw e;
		}
		super.setContent(content, contentType);
	}

	/**
	 * @see com.ibm.ws.sip.container.servlets.SipServletMessageImpl#getContent()
	 */
	public Object getContent() throws IOException, UnsupportedEncodingException
	{
		if (m_unsupportedEncodingException != null) {
			UnsupportedEncodingException e = new UnsupportedEncodingException();
			e.initCause(m_unsupportedEncodingException);
			throw e;
		}
		return super.getContent();
	}

	/**
     *  @see javax.servlet.sip.SipServletMessage#getContent(java.lang.Class[])
     */
    @Override
	public Object getContent(Class[] classes) throws IOException,
			UnsupportedEncodingException {
		throw new UnsupportedOperationException("getContent: Not Done Yet");
	}

    /**
     * @see javax.servlet.ServletRequest#removeAttribute(String)
     */
    @Override
	public void removeAttribute(String key)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
    	if( m_attributes != null){
    		m_attributes.remove(key);
    	}
    }
    
    /**
     *  @see javax.servlet.sip.SipServletResponse#getChallengeRealms()
     */
	public Iterator<String> getChallengeRealms() {
    	List<String> realms = null;
		SecurityHeader authHeader = null;
		try {
			Response response = getResponse();
			if (getStatus() == SC_PROXY_AUTHENTICATION_REQUIRED) {
				authHeader = response.getProxyAuthenticateHeader();
				if (authHeader != null) {
					String headerRealm = authHeader.getParameter(DigestConstants.PROPERTY_REALM);
					realms = new ArrayList<String>(1);
					realms.add(headerRealm);
				}
			} else if (getStatus() == SC_UNAUTHORIZED) {
				HeaderIterator headers = response.getWWWAuthenticateHeaders();
				while (headers.hasNext()) {
					SecurityHeader currHeader = (SecurityHeader)headers.next();
					String scheme = currHeader.getScheme();
					if (scheme.equals(DigestConstants.DIGEST)) {
						if (realms == null) {
							realms = new ArrayList<String>();
						}
						String headerRealm = currHeader.getParameter(DigestConstants.PROPERTY_REALM);
						realms.add(headerRealm);
					}
				}
			}
			// For responses other than 401 and 407, we return null.
		} catch (SipParseException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.parse.auth.challange",
						Situation.SITUATION_REQUEST, null, e);
			}
		}
		
		Iterator <String> iterator;
		if (realms != null)
			iterator = realms.iterator();
		else
			iterator = EmptyIterator.getInstance();
		
		return iterator;     	
    }
    
    /**
     * Helper function that will test if the original request contains Supported
	 * or Require headers and decide if the response can be sent reliably
	 * 
	 * @return
     */
    protected boolean canBeSentReliably() {
    	if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "canBeSentReliably");
		}
        
    	IncomingSipServletRequest req = (IncomingSipServletRequest)getRequest();
        String method = getMethod();
        int status = getStatus();
      
        //proxy can not send reliable responses for requests with To Tag - RFC3262
        if (req.getTransactionUser() != null && req.getTransactionUser().isProxying()) {
        	if (req.getRequest().getToHeader().hasTag()){
        		if (c_logger.isTraceDebugEnabled()){
        			c_logger.traceDebug(this, "canBeSentReliably", "Trying to answer with reliable response in proxy mode for requests with to tag");
        		}
        		if (c_logger.isTraceEntryExitEnabled()){
        			c_logger.traceExit(this, "canBeSentReliably", false);
        		}
        		return false;
        	}
        }
        
        if (((status > 100) && (status < 200)) &&
                method.equals(Request.INVITE)&& 
                (req.getShouldBeAnsweredReliable() || req.getMayBeAnsweredReliable())) {
        	 if (c_logger.isTraceEntryExitEnabled()){
     			c_logger.traceExit(this, "canBeSentReliably", true);
     		}
            return true;
        }
        
        if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "canBeSentReliably", false);
		}
        return false;
    }

	/**
	 * Helper method which returns the value of the _shouldBeSentWithoutST flag;
	 * @return
	 */
    public boolean shouldBeSentWithoutST() {
		return _shouldBeSentWithoutST;
	}

	/**
	 * Helper method which sets the value of _shouldBeSentWithoutST
	 * @param beSentWithoutST
	 */
    public void setShouldBeSentWithoutST(boolean beSentWithoutST) {
		_shouldBeSentWithoutST = beSentWithoutST;
	}

    /**
     * @see javax.servlet.sip.SipServletResponse#getProxyBranch()
     */
    public ProxyBranch getProxyBranch() {
    	SipTransaction transaction = getTransaction();
    	if (!(transaction instanceof ClientTransaction)) {
    		// response of a server transaction is outbound response,
    		// which is not associated with a branch
    		return null;
    	}
    	ClientTransaction clientTransaction = (ClientTransaction)transaction;
    	ClientTransactionListener listener = clientTransaction.getListener();
    	if (!(listener instanceof ProxyBranch)) {
    		return null;
    	}
    	ProxyBranch branch = (ProxyBranch)listener;
    	return branch;
	}

	/**
	 * The function return true if the response is a branch response and false otherwise
	 * @see javax.servlet.sip.SipServletResponse#isBranchResponse()
	 */
	public boolean isBranchResponse() {
		return (ThreadLocalStorage.getCurrentBranch() != null);
	}

	/**
	 * The function set The response as a branch response and saves the branch for further usage.
	 * @param branchResponse
	 */
	public void setIsBranchResponse(ProxyBranchImpl branch) {
		ThreadLocalStorage.setCurrentBranch(branch);
	}
}