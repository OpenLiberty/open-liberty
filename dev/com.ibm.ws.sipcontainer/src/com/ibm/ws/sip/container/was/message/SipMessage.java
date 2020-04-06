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
package com.ibm.ws.sip.container.was.message;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.message.Request;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.websphere.security.WSSecurityHelper;
import com.ibm.websphere.servlet.request.extended.IRequestExtended;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersImpl;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.parser.SecurityResourceCollection;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.pmi.TaskDurationMeasurer;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.router.SipServletInvokerListener;
import com.ibm.ws.sip.container.security.IPAuthenticator;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.Queueable;
import com.ibm.ws.sip.container.was.EmptyHttpInboundConnection;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.util.ThreadPool;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHost;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHostManager;
import com.ibm.wsspi.http.HttpInboundConnection;

/**
 * @author yaronr
 *
 * Used by the Websphere configuration to route SIP request and response
 * 	to the application server
 */
public class SipMessage implements IRequestExtended, IResponse, Queueable
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipMessage.class);

    /**
     * Some values, needed for the get headers method 
     */
    private static final String HOST = "host";
    private static final String ACCEPT = "accept";
    private static final String ASTERISK_SLASH_ASTERISK = "*/*";
    private static final String ACCEPT_LANGUAGE = "accept-language";
    private static final String EN_US = "en-us";
    private static final String ACCEPT_ENCODING = "accept-encoding";
    private static final String ENCODING = "gzip, deflate";
    private static final String USER_AGENT = "user-agent";
    private static final String BROWSERS =
        "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)";
    private static final String CONNECTION = "connection";
    private static final String KEEP_ALIVE = "Keep-Alive";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC_AUTHORIZATION = "Basic";
    private static final String COOKIE = "Cookie";


	/** per-thread parser */
    private final ThreadLocal<SipParser> m_parser =
    	new ThreadLocal<SipParser>() {
    	protected SipParser initialValue() {
    		return new SipParser();
    	}
    };
    
    private ParametersImpl m_cookies = null;
    
    /**
     * Sip Servlet invoker listener. 
     */
    private SipServletInvokerListener _listener;
    
    /**
     * Sip servlet request 
     */
    private SipServletRequest _request;
    
    /**
     * Sip servlet response
     */
    private SipServletResponse _response;
    
    /**
     * List of Siplets that this Message is destined to.
     */
    private String m_appPath = "";

    /**
     * Hold Websphere HTTP port
     */
    private int m_serverPort = -1;
    
	/**
	 * Websphere's Host name.  
	 */
	private String m_httpHost = "no host was set";

	/**
	 * Flag indicating whether this request is sent on a SSL enabled WAS 
	 * transport. 
	 */
	private boolean m_isSSLEnabled;
	/**
	 * hold if server is secured
	 */
	private static boolean m_isServerSecured = WSSecurityHelper.isServerSecurityEnabled();
	
	/**
	 * holds sip applicarion descriptor
	 */
	private SipAppDesc m_appDesc=null;
	
	/**
	 * holds servlet name
	 */
	private String m_servletName = "";
	/**
	 * SIP Message wrapped by this object. 
	 */
	private SipServletMessage m_message;
	
	/**
	 * response status code
	 */
    private int m_statusCode; 
	
    /**
     * Holds request headers as name:value pairs
     */
    private Map<String, String> _wasRequestHeaders = new HashMap<String, String>();
    
    /**
     * Holds response headers as name:value pairs
     */
    private Map<String, String> _wasResponseHeaders = new HashMap<String, String>();
    
    /**
	 * Object that measures the duration of the task in the container queue
	 */
	private TaskDurationMeasurer _sipContainerQueueDuration= null;
	
	/**
	 * Object that measures the duration of the task in the application code
	 */
	private TaskDurationMeasurer _sipContainerApplicationCodeDuration= null;
    
    /**
     * Constructor for SipMessage.
     */
    public SipMessage()
    {
       
    }

	/**
     * @param request The Sip Request if available
     * @param response The Sip Response if available
     * @param appDesc application description
	 */
	public void setup(SipServletRequest request, SipServletResponse response,
			SipAppDesc appDesc) {
		if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "SipMessage", "created");
        }
        m_appDesc = appDesc;
        
        if (null != request)
        {
            _request = request;
            m_message = request; 
        }
        
        if (null != response)
        {
            _response = response;
            m_message = response;
            
            //BACKWARD COMPATSBILITY FOR LWP
            //Add the value of the To; header field as an attribute. 
            //Used by the Digest TAI for authenticating responses for requests
            //originated in the container. These responses will not have 
            //authentication headers on them but they still need to be passed
            //as authenticated messages to the siplets.   
            //We need to add only the user part of the To: header. 
            try
            {
                Address toAddr = response.getAddressHeader(ToHeader.name);
				URI uri = toAddr.getURI();
				if(uri.isSipURI())
				{
					//Reconstruct the user@host part only of the URI
					SipURI sipUri = (SipURI)uri; 
					String userName = sipUri.getUser();
					
					StringBuffer user = new StringBuffer(16);
					if(userName != null)
					{
					    user.append(userName); 
					}
					user.append('@');
					user.append(sipUri.getHost());
					
					ThreadLocalStorage.setSipResponseToHeader( user.toString());	
				}
            }
            catch (ServletParseException e)
            {
                if(c_logger.isErrorEnabled())
                {                
                    c_logger.error(
                        "error.adding.to.header",
                        Situation.SITUATION_REQUEST,
                        null,
                        e);
    		    }              
            }
        }

        String cookie = m_message.getHeader(COOKIE);

        if (cookie != null && cookie.length() > 0) {
        	SipParser parser = m_parser.get();

        	parser.setSrc(cookie.toCharArray(), cookie.length());
        	try {
        		m_cookies = parser.parseParametersMap(';', true,false);
        	} catch (SipParseException e) {
        		if(c_logger.isErrorEnabled()) {                
        			c_logger.error("Error while trying to parse cookie", Situation.SITUATION_REQUEST, cookie, e);
        		}              
        	}
        }
        
        createWASHeaders();
	}

    /**
     * Returns the application description
     * @return m_appDesc
     */
    public SipAppDesc getSipAppDesc(){
        return m_appDesc;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getAuthType()
     */
    public String getAuthType()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getAuthType", "");
        }
        return null;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getContentLength()
     */
    public int getContentLength()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getContentLength", "");
        }

        return 0;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getContentType()
     */
    public String getContentType()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getContentType", "");
        }
        return null;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getCookieValue(String)
     */
    public byte[] getCookieValue(String name) {
    	if (m_cookies == null || !m_cookies.hasParameters()) {
        	return null;
        }
        
        String str = m_cookies.getParameter(name);
        
        return str != null ? str.getBytes(Charset.defaultCharset()) : null; 
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getAllCookieValues(String)
     */
    public List getAllCookieValues(String cookieName) {
    	if (m_cookies == null || !m_cookies.hasParameters()) {
    		return Collections.EMPTY_LIST;
    	}

    	List <String> values = new ArrayList(m_cookies.size());

    	Iterator iter = m_cookies.getParameters();
    	while (iter.hasNext()) {
    		String key = (String)iter.next();
    		values.add(m_cookies.getParameter(key));
    	}

    	return values;
    }


    /**
     * check is its Basic Authentication Value, if so need to get rid of quotes
     * before and after Base64 encoded value
     * @param value
     * @param hide - indicate whether to hide the printing of the authorization header to the log
     * @return
     */
    private String createLegalAuthorizationHeaderValue(String value, boolean hide)
    {
        if (value.startsWith(BASIC_AUTHORIZATION))
        {
            if (c_logger.isTraceDebugEnabled() && !hide)
            {
                c_logger.traceDebug(
                    this,
                    "createLegalAuthorizationHeaderValue",
                    "old[" + value + "]");
            }
            //check if it is a valid Basic Http Header
            //we will acctually check if its our propritery Basic header and 
            //if not then its probably ok
            if (value.indexOf("cred") == -1)
            {
                //this is valid header
                return value;
            }
            //if it our proprietery Basic then parser call with {Basic cred="abcd"} 
            //should be {Basic abcd}
            StringBuffer newVal = new StringBuffer(BASIC_AUTHORIZATION);
            newVal.append(" ");
            newVal.append(
                value.substring(value.indexOf("\"") + 1, value.length() - 1));
            if (c_logger.isTraceDebugEnabled() && !hide)
            {
                c_logger.traceDebug(
                    this,
                    "createLegalAuthorizationHeaderValue",
                    "new [" + newVal.toString() + "]");
            }
            return newVal.toString();
        }
        else
        {
            return value;
        }
    }

    /**
     * Create SIP request headers
     */
    private void createWASHeaders()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getHeaders", "");
        }

        _wasRequestHeaders.put( HOST, getServerName() + ":" + getServerPort());

        _wasRequestHeaders.put( ACCEPT, ASTERISK_SLASH_ASTERISK);

        _wasRequestHeaders.put( ACCEPT_LANGUAGE, EN_US);

        _wasRequestHeaders.put( ACCEPT_ENCODING, ENCODING);

        _wasRequestHeaders.put( USER_AGENT, BROWSERS);

        _wasRequestHeaders.put( CONNECTION, KEEP_ALIVE);

        String authorization = m_message.getHeader(AUTHORIZATION);

        if (null != authorization)
        {
        	//this is a patch to be able to add Basic Authentication support throw parser
        	boolean hideAuthorization = SIPTransactionStack.instance().getConfiguration().getHiddenHeaders().contains(AUTHORIZATION);
            String lahv = createLegalAuthorizationHeaderValue(authorization, hideAuthorization);
            _wasRequestHeaders.put( AUTHORIZATION, lahv);

            if (c_logger.isTraceDebugEnabled()&& !hideAuthorization)
            {
                c_logger.traceDebug(
                    this,
                    "getHeaders",
                    "Authorization: " + lahv);
            }
        }

        String cookie = m_message.getHeader(COOKIE);
        if (null != cookie)
        {
            _wasRequestHeaders.put( COOKIE, cookie);

            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(
                    this,
                    "getHeaders",
                    "Cookie: " + cookie);
            }
        }
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getMethod()
     */
    public String getMethod() {
    	if(m_isServerSecured){
        	//check if ip is authenticated
        	if (IPAuthenticator.isIPAuthenticated(m_message)){
            	if (c_logger.isTraceDebugEnabled()) {
            		c_logger.traceDebug(this, "getMethod", "Message authenticated, skipping authentication.");
            	} 
        		return "GET";
        	}
        	
        	SipAppDesc app = (SipAppDesc)ThreadLocalStorage.getSipAppDesc(); 	
        	List collections = app.getSipServlet(getServletName()).getSecurityResourceCollections();
        	int len=collections.size();
        	for (int i = 0; i < len; i++) {
        		SecurityResourceCollection collection = (SecurityResourceCollection) collections.get(i);
        		List methods = collection.getMethods();
        		int mLen = methods.size();
        		for (int j = 0; j < mLen; j++) {
        			String method = (String)methods.get(j);
        			if(m_message.getMethod().equalsIgnoreCase(method)){
        				if (c_logger.isTraceDebugEnabled())
        				{
        					c_logger.traceDebug(this, "getMethod", "sip method["+m_message.getMethod()+"] http POST");
        				}
        				return "POST";
        			}
        		}
        	}
    	}
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug(this, "getMethod", "sip method["+m_message.getMethod()+"] http GET");
    	} 
    	return "GET";
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getProtocol()
     */
    public String getProtocol()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getProtocol", "");
        }
        return "SIP";
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getQueryString()
     */
    public String getQueryString()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getQueryString", "");
        }
        return null;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getRemoteAddr()
     */
    public String getRemoteAddr()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getRemoteAddr", "");
        }
        return null;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getRemoteHost()
     */
    public String getRemoteHost()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getRemoteHost", "");
        }
        return null;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getRemoteUser()
     */
    public String getRemoteUser()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getRemoteUser", "");
        }
        return null;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getRequestURI()
     */
    public String getRequestURI()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getRequestURI", m_appPath);
        }
        return m_appPath;
    }

    /**
     * Add a URI to the list of siplets that will handle this message (e.g. application path)
     */
    public void setRequestURI(String uri)
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setRequestURI", uri);
        }
        m_appPath = uri;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getScheme()
     */
    public String getScheme()
    {
        String scheme = isSSL() ? "https" : "http";
        
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getScheme", scheme);
        }
       
	
       return scheme;
    }
    public String getServletName()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getServletName", m_servletName);
        }
        return m_servletName;
    }

    public void setServletName(String name)
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setServletName", name);
        }
        m_servletName = name;
    }
    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getServerName()
     */
    public String getServerName()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getServerName", "Server: " + m_httpHost);
        }

        return m_httpHost; 
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getServerPort()
     */
    public int getServerPort()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getServerPort", "Port: " + m_serverPort);
        }

        return m_serverPort;
    }

    /**
     * 
     * @param port
     */
    public void setServerPort(int port)
    {
        m_serverPort = port;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IRequest#getSessionId()
     */
    public byte[] getSessionId()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getSessionId", "");
        }
        return null;
    }


    /**
     * @see om.ibm.wsspi.webcontainer.IRequest#isSSL()
     */
    public boolean isSSL()
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "isSSL", "SSL Enabled: " + m_isSSLEnabled);
        }
        return m_isSSLEnabled;
    }
    
    /**
     *  Sets response headers
     */
    public void prepareForWrite()
    {
        if(c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "prepareForWrite", String.valueOf(m_statusCode));       
        }
        
        //	get original request
        SipServletRequest request =  _request;
        if (null == request)
        {
            if(c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(this, "prepareForWrite", "NO REQUEST!!");
            }
            return;
        }
		
		
		//Amir 11 Aug, 2004 - Send a response back to the client also in a
		//case where the application is not (404 response) located by container. 
		//It is not the best solution but it is better then not sending a 
		//response to the client at all. 
        if (m_statusCode == SipServletResponse.SC_UNAUTHORIZED
            || m_statusCode == SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED
            || m_statusCode == SipServletResponse.SC_FORBIDDEN
            || m_statusCode == SipServletResponse.SC_NOT_FOUND)
        {
        	//check if final response was not sent yet and the request is not ACK, if not will send 40x
        	if ((!request.isCommitted())&&(!(request.getMethod().equals(Request.ACK)))){
	            // Create response and copy all headers to the response
        		SipServletResponse response = request.createResponse(m_statusCode);
        		Iterator<String> itr = _wasResponseHeaders.keySet().iterator();
	            while(itr.hasNext())
	            {
	                String key = itr.next();
	                String value = _wasResponseHeaders.get(key);
	                if (c_logger.isTraceDebugEnabled())
	                {
	                    StringBuffer buffer = new StringBuffer("name: ");
	                    buffer.append(key);
	                    buffer.append(" value: ");
	                    buffer.append(value);
	                    c_logger.traceDebug(this, "prepareForWrite", buffer.toString());
	                        
	                }
	
	                response.addHeader(key, value);
	            }
	
	            if(c_logger.isTraceDebugEnabled())
	            {
	                if(m_statusCode == SipServletResponse.SC_NOT_FOUND)
	            	{
	            	    if (c_logger.isTraceDebugEnabled()) {
	                        c_logger.traceDebug(this, "prepareForWrite", 
	                       "Warning!!! the Web Container failed to find " + 
	                       getRequestURI() +
	                       " Check that servlet mapping exists in web.xml");
	                    }
	            	}
	                
	                c_logger.traceDebug(this, "prepareForWrite" ,"Sending a " 
	            						+ m_statusCode + 
										" Automatic response by the container");
	            	
	            	
	            }
	            
	            sendResponse(response);
        	}else{
        		c_logger.traceDebug(this, "prepareForWrite" ,+ m_statusCode + 
						"  response already sent");
        	}
        }
        else if(m_statusCode != SipServletResponse.SC_OK)
        {
        	if(c_logger.isTraceDebugEnabled())
            {
        	    StringBuffer buffer = new StringBuffer("Failure, status code: ");
        	    buffer.append(m_statusCode);
        	    buffer.append(" \nRequest:");
        	    buffer.append( _request);
        	    buffer.append("\nResponse:\n");
        	    buffer.append( _response);
            	c_logger.traceDebug(this, "prepareForWrite" , buffer.toString());
            }
        	
        	if (!request.isCommitted() && !request.getMethod().equals(Request.ACK)) {
        	    SipServletResponse response = request.createResponse(m_statusCode);
        	    sendResponse(response);
        	}
        }
    }

    /**
     * Helper function for sending response and catching the exceptions 
     * in case of failure. 
     * @param response
     */
    private void sendResponse(SipServletResponse response) 
    {
        try
		{
		    response.send();
		}
		catch (IOException e) 
		{
		    if(c_logger.isErrorEnabled())
		    {
		        Object[] args = { response };
		        c_logger.error(
		            "error.sending.response.from.prepare.for.write",
		            Situation.SITUATION_REQUEST,
		            args,
		            e);
		    }
		}
    }

    /**
     * Sets the http host for the request.
     * @param httpHost
     */
    public void setHost(String httpHost)
    {
        m_httpHost = httpHost;
    }

    /**
     * Sets the is SSL flag for this request.  
     * @param isSSLEnabled
     */
    public void setSSLEnbaled(boolean isSSLEnabled)
    {
        m_isSSLEnabled = isSSLEnabled;
    }

    /**
     * @see com.ibm.wsspi.webcontainer.IResponse#setFlushMode(boolean)
     */
    public void setFlushMode(boolean b)
    {
    }

   /**
     * @see com.ibm.wsspi.webcontainer.IResponse#getFlushMode()
     */
    public boolean getFlushMode()
    {
		return false;
    }

/**
     * @see com.ibm.wsspi.webcontainer.IResponse#flushBufferedContent()
     */
	public void flushBufferedContent(){

	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getHeader(java.lang.String)
	 */
	public String getHeader(String headerName) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getHeader", "headerName: " + headerName);
        }
		return _wasRequestHeaders.get( headerName);
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getHeaders(java.lang.String)
	 */
	public Enumeration getHeaders(String headerName) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getHeaders enumeration", "headerName: " + headerName);
        }
	    String val = _wasRequestHeaders.get( headerName);
	    if (val!=null){
	        Vector v = new Vector(1);
	        v.add(val);
	        return v.elements();
	    }
		return Collections.enumeration(Collections.EMPTY_SET);
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getDateHeader(java.lang.String)
	 */
	public long getDateHeader(String name) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getDateHeader", "name: " + name);
        }
		return 0;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getIntHeader(java.lang.String)
	 */
	public int getIntHeader(String name) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getIntHeader", "name: " + name);
        }
		return 0;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#clearHeaders()
	 */
	public void clearHeaders() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "clearHeaders", "");
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getHeaderNames()
	 */
	public Enumeration getHeaderNames() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getHeaderNames", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getRemotePort()
	 */
	public int getRemotePort() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getRemotePort", "");
        }
		return 0;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getInputStream", "");
        }
		return new InputStream() {
			
			@Override
			public int read() throws IOException {
				// TODO Auto-generated method stub
				return 0;
			}
		};
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getLocalAddr()
	 */
	public String getLocalAddr() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getLocalAddr", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getLocalName()
	 */
	public String getLocalName() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getLocalName", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getLocalPort()
	 */
	public int getLocalPort() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getLocalPort", "");
        }
		return 0;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getSSLSessionID()
	 */
	public byte[] getSSLSessionID() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getSSLSessionID", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getSessionID()
	 */
	public String getSessionID() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getSessionID", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#isProxied()
	 */
	public boolean isProxied() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "isProxied", "");
        }
		return false;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getWCCResponse()
	 */
	public IResponse getWCCResponse() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getWCCResponse", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getCipherSuite()
	 */
	public String getCipherSuite() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getCipherSuite", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getPeerCertificates()
	 */
	public X509Certificate[] getPeerCertificates() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getPeerCertificates", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IRequest#getCookies()
	 */
	public Cookie[] getCookies() {
		if (m_cookies == null || !m_cookies.hasParameters()) {
			return null;
		}

		Cookie[] cookies = new Cookie[m_cookies.size()];

		Iterator localIter = m_cookies.getParameters();
		int i = 0;

		while(localIter != null && localIter.hasNext()) {
			String paramName = (String) localIter.next();
			String localParam = m_cookies.getParameter(paramName);
			Cookie c = new Cookie(paramName, localParam);
			cookies[i] = c;
			i++;
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getCookies", "returned " + i + " cookies.");
        }

		return cookies;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setStatusCode(int)
	 */
	public void setStatusCode(int code) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setStatusCode", "code="+code);
        }
	    m_statusCode = code;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#getOutputStream()
	 */
	/*TODO Liberty public OutputStream getOutputStream() throws IOException {
		return System.out;
	}*/
	
	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#isCommitted()
	 */
	public boolean isCommitted() {
		return false;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#addHeader(java.lang.String, java.lang.String)
	 */
	public void addHeader(String name, String value) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "addHeader", "name= " + name + ", value= " + value);
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#addHeader(byte[], byte[])
	 */
	public void addHeader(byte[] name, byte[] value) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "addHeader", "name= " + Arrays.toString(name) + ", value= " + Arrays.toString(value));
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#addDateHeader(java.lang.String, long)
	 */
	public void addDateHeader(String name, long t) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "addDateHeader", "name= " + name + ", t= " + t);
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#addIntHeader(java.lang.String, int)
	 */
	public void addIntHeader(String name, int i) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "addIntHeader", "");
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setDateHeader(java.lang.String, long)
	 */
	public void setDateHeader(String name, long t) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setDateHeader", "name= " + name + ", t= " + t);
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setIntHeader(java.lang.String, int)
	 */
	public void setIntHeader(String name, int i) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setIntHeader", "name= " + name + ", i= " + i);
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#getHeaderTable()
	 */
	public Vector[] getHeaderTable() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getHeaderTable", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#getHeader(byte[])
	 */
	public String getHeader(byte[] name) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getHeader", "name= " + Arrays.toString(name));
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#containsHeader(java.lang.String)
	 */
	public boolean containsHeader(String name) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "containsHeader", "name= " + name);
        }
		return false;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#containsHeader(byte[])
	 */
	public boolean containsHeader(byte[] name) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "containsHeader", "name= " + Arrays.toString(name));
        }
		return false;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#removeHeader(java.lang.String)
	 */
	public void removeHeader(String name) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "removeHeader", "name= " + name);
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#removeHeader(byte[])
	 */
	public void removeHeader(byte[] name) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "removeHeader", "name= " + Arrays.toString(name));
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#getWCCRequest()
	 */
	public IRequestExtended getWCCRequest() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "getWCCRequest", "");
        }
		return null;
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setReason(java.lang.String)
	 */
	public void setReason(String reason) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setReason", " reason= " + reason);
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setReason(byte[])
	 */
	public void setReason(byte[] reason) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setReason", " reason= " + String.valueOf( reason));
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#addCookie(javax.servlet.http.Cookie)
	 */
	public void addCookie(Cookie cookie) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "addCookie", " cookie= " + cookie);
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#prepareHeadersForWrite()
	 */
	public void prepareHeadersForWrite() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "prepareHeadersForWrite", "");
        }
	    
	    prepareForWrite();
	    
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#writeHeaders()
	 */
	public void writeHeaders() {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "writeHeaders", "");
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String name, String s) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setHeader", "name= " + name + ", s= " + s);
        }
	    
	    _wasResponseHeaders.put( name, s);
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setHeader(byte[], byte[])
	 */
	public void setHeader(byte[] name, byte[] bs) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setHeader", "name= " + Arrays.toString(name) + ", bs= " + Arrays.toString(bs));
        }
	    _wasResponseHeaders.put( new String(name,Charset.defaultCharset()), new String(bs,Charset.defaultCharset()));
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setContentType(java.lang.String)
	 */
	public void setContentType(String value) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setContentType", "value= " + value);
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setContentType(byte[])
	 */
	public void setContentType(byte[] value) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setContentType", "value= " + Arrays.toString(value));
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setContentLanguage(java.lang.String)
	 */
	public void setContentLanguage(String value) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setContentLanguage", "value= " + value);
        }
	}

	/**
	 * @see com.ibm.wsspi.webcontainer.IResponse#setContentLanguage(byte[])
	 */
	public void setContentLanguage(byte[] value) {
	    if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, "setContentLanguage", "value= " + Arrays.toString(value));
        }
	}
	
    /**
     * @return Returns the _listener.
     */
    public SipServletInvokerListener getListener() {
        return _listener;
    }
    /**
     * @param _listener The _listener to set.
     */
    public void setListener(SipServletInvokerListener listener) {
        _listener = listener;
    }
    /**
     * @return Returns the _request.
     */
    public SipServletRequest getRequest() {
        return _request;
    }
    
    /**
     * @return Returns the _response.
     */
    public SipServletResponse getResponse() {
        return _response;
    }    
    
    /**
     * This method is relevant only when the SipMesage is processed 
     * using a WAS ThreadPool
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
    	if( c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("Dispatching a SipMessage= " + this + " on thread " + Thread.currentThread());
		}
    	if( c_logger.isTraceDebugEnabled()){
    		SipServletResponse response = getResponse();
            if (null != response)
            {
            	c_logger.traceEntry( this, "Sipmessage.run() response callId["+response.getCallId()+"] status["+response.getStatus()+"]");
            }
    		
    	}
    	
    	dispatch();
    }
    
    /**
     * Dispatching this message to WebContainer. 
     * This method will be used in  
     */
    public void dispatch()
    {
    	if( c_logger.isTraceDebugEnabled()){
    		c_logger.traceEntry( this, "dispatch");
    	}
    	
    	try
        {
    		//Store locally for current thread. Will be retrieved by the 
    		// SipFilter when siplet is found 
	    	ThreadLocalStorage.setSipMessage( this);
	    	if( c_logger.isTraceDebugEnabled()){
	    		SipServletResponse response = getResponse();
	            if (null != response)
	            {
	            	c_logger.traceEntry( this, "dispatching response callId["+response.getCallId()+"] status["+response.getStatus()+"] to webcontainer");
	            }
	    		
	    	}
	    	
	    	//dispatch to through the Web Container's engine
	    	//passing the message to the virtualHostSelector so the message will be passed to the application useing the correct virtual host
	    	DynamicVirtualHostManager dhostm = SipContainerComponent.getVirtualHostMgr(); 
	    	DynamicVirtualHost dhost = dhostm.getVirtualHost(m_appDesc.getVirtualHostName(), null);
	    	if(dhost == null) {
	    		//Request did not match any matching rule. This is a configuration
        		//problem
        		int errorCode = PropertiesStore.getInstance().getProperties().getInt(CoreProperties.SIP_NO_ROUTE_ERROR_CODE_PROPERTY);
        		
        	
	    		//In case that the host was stopped after we routed the message or if there is a problem with the host.
	    		SipRouter.sendErrorResponse((SipServletRequestImpl)_request, errorCode);
		        ThreadLocalStorage.setSipMessage( null); 
		        return;
	    	}
	    	  //creating the task on the correct virtual host
	    	 Runnable task =  dhost.createRunnableHandler(this, this,new EmptyHttpInboundConnection());
	    	 //running the task not as a runnable as expected but synchronized to stay on the same thread and keep the message handling synchronized
	    	 task.run();
	    	if( c_logger.isTraceDebugEnabled()){
	    		SipServletResponse response = getResponse();
	            if (null != response)
	            {
	            	c_logger.traceEntry( this, "dispatch response callId["+response.getCallId()+"] status["+response.getStatus()+"] to webcontainer");
	            }
	    		
	    	}
	        //If there is a listener, tell him that we done
	        SipServletInvokerListener listener = getListener();
	        if (null != listener)
	        {
	            SipServletResponse response = getResponse();
	            if (null != response)
	            {
	                listener.servletInvoked(response);
	            }
	            SipServletRequest request = getRequest();
	            if (null != request)
	            {
	                listener.servletInvoked(request);
	            }
	
	        }
	        //We need to set that to null, so that there won't be any access to this message
	        //on another service executed on this thread that did not call this 
	        //dispatch() method (like timer event etc..) and did not replace the SipMessage 
	        ThreadLocalStorage.setSipMessage( null); 
	    }
	    catch(Throwable e)
	    {
	        if(c_logger.isErrorEnabled())
		    {
	            Object[] args = { this };
	            c_logger.error(
	                "error.invoking.request",
	                Situation.SITUATION_REQUEST,
	                args,
	                e);
		    }
	    }
    	
        if( c_logger.isTraceDebugEnabled()){
    		c_logger.traceExit( this, "dispatch");
    	}
    }

	/**
	 *  @see com.ibm.ws.sip.container.util.Queueable#getQueueIndex()
	 */
    public int getQueueIndex() {
		TransactionUserWrapper tu = null;
    	if(_request != null){
			tu = ((SipServletMessageImpl)_request).getTransactionUser();
		}
    	else{
    		tu = ((SipServletMessageImpl)_response).getTransactionUser();
    	}
		return SipApplicationSessionImpl.extractAppSessionCounter(tu.getApplicationId());
	}

    /**
     * We define sequential messages as critical, while new dialogs messages can be left out when queue is full
     * @see com.ibm.ws.sip.container.util.Queueable#priority()
     */
	public int priority() {
		if (null != _response){
			return PRIORITY_CRITICAL;
		}
		
		if( !_request.isInitial()){;
			return PRIORITY_CRITICAL;
		}
		return PRIORITY_NORMAL;
	}
           //Begin:LIDB3518-1.1, can be stubbed since we only use with ARD which is not supported for siplets.
           public boolean getShouldDestroy(){return false;}
           
           public void setShouldDestroy(boolean shouldDestroy){};
        
           public void setShouldReuse(boolean b){};

           public void setShouldClose(boolean b){};
           //End:LIDB3518-1.1

               /**
     * Set whether to allocate direct or indirect byte buffers
     * @param allocateDirect
     */
    public void setAllocateDirect(boolean allocateDirect){};    
    
    /**
     * Get whether to allocate direct or indirect byte buffers
     */
    public boolean isAllocateDirect(){return true;}
    
    /**
     * @see com.ibm.websphere.servlet.response.IResponse#setLastBuffer(boolean)
     */
    public void setLastBuffer(boolean writeLastBuffer){
    	//just a stab
     }
    
    /**
     * @see com.ibm.websphere.servlet.response.IResponse#releaseChannel()
     */
    public void releaseChannel(){
//    	just a stab
    }

	public boolean isStartAsync() {
		return false;
	}

	public void lock() {
	}

	public void startAsync() {
	}

	public void unlock() {
	}
	
    @Override
    public ThreadPool getThreadPool() {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * @see com.ibm.ws.sip.container.util.Queueable#getServiceSynchronizer()
     */
    public Object getServiceSynchronizer() {
    	TransactionUserWrapper tu;
    	if(_request != null){
    		tu = ((SipServletMessageImpl)_request).getTransactionUser();
    	}
    	else{
    		tu = ((SipServletMessageImpl)_response).getTransactionUser();
    	}
    	return tu.getServiceSynchronizer();
    }

    public void removeCookie(String cookieName){
    	if (m_cookies == null || !m_cookies.hasParameters()) {
        	return;
        }
        
    	m_cookies.removeParameter(cookieName);
    }
    
    /**
	 * @see com.ibm.ws.sip.container.util.Queueable#getSipContainerQueueDuration()
	 */
	public TaskDurationMeasurer getSipContainerQueueDuration() {
		return _sipContainerQueueDuration;
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getApplicationCodeDuration()
     */
	public TaskDurationMeasurer getApplicationCodeDuration() {
		return _sipContainerApplicationCodeDuration;
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getAppName()
     */
	public String getAppName() {
		return null;
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getAppIndexForPMI()
     */
	public Integer getAppIndexForPMI() {
		return 0;
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#setSipContainerQueueDuration(TaskDurationMeasurer)
     */
	public void setSipContainerQueueDuration(TaskDurationMeasurer tm) {
		_sipContainerQueueDuration = tm;
		
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#setApplicationCodeDuration(TaskDurationMeasurer)
     */
	public void setApplicationCodeDuration(TaskDurationMeasurer tm) {
		_sipContainerApplicationCodeDuration = tm;
		
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getApplicationSession()
	 */
	@Override
	public SipApplicationSession getApplicationSession() {
		return m_message.getApplicationSession(false);
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getTuWrapper()
	 */
	@Override
	public TransactionUserWrapper getTuWrapper() {    	
		if (_request != null) {
			return((SipServletMessageImpl)_request).getTransactionUser();
		}
    	return ((SipServletMessageImpl)_response).getTransactionUser();
	}

	@Override
	public void setIsClosing(boolean isClosing) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContentLength(int length) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetBuffer() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setBufferSize(int bufferSize) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return null
	 */
	@Override
	public HttpInboundConnection getHttpInboundConnection() {
		return null;
	}
}
