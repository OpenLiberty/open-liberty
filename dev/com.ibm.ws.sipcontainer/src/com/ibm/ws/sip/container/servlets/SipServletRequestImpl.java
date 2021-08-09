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
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RecordRouteHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.message.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.PathHeader;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericNameAddressHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;
import com.ibm.ws.sip.container.router.CompositionData;
import com.ibm.ws.sip.container.router.CompositionInfoMap;
import com.ibm.ws.sip.container.transaction.SipTransaction;
import com.ibm.ws.sip.container.tu.TransactionUserImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.dar.util.StateInfo;
import com.ibm.ws.sip.security.auth.AuthInfoFactory;
import com.ibm.ws.sip.security.auth.AuthInfoImpl;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.wsspi.webcontainer.servlet.IServletRequestWrapper;

/**
 * @author Amir Perlman, Feb 10, 2003
 * Implementation for the Sip Servlet Request API. 
 * @see javax.servlet.sip.SipServletRequest
 */
public abstract class SipServletRequestImpl
    extends SipServletMessageImpl
    implements SipServletRequest, IServletRequestWrapper,Serializable 
	 /*
	  * Anat: SPR #RDUH6C323P Serializable added for:
	  * SipServletRequest can be added as an attribute to the
	  * session or appSession and will be replicated after the
	  * dialog will be established
	  */
{
    /** Serialization UID (do not change) */
    
    static final long serialVersionUID = -546972310341594665L;

    
     /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipServletRequestImpl.class);

    /**
     * Indicates whether the Request is an inital Request. 
     */
    private boolean m_isInital = false;

    /**
     * List of apps excluded from being handled in the future by apps that
     * appear in the list. The list is put into hashtable for easier get and test
     * operations. 
     * Can be transient because used only for send request's wich will be
     * disabled in the replicated object
     */
    private transient HashMap m_excludeAppsList;

    /*
     * Used to hold the request URI object so we don't construct it from scratch 
     * every time.
     * Can be transient - it can be created at any time
     */
    protected transient URI m_requestURI;
    
    /**
     * Header containing the list of Siplets that are already in the application
     * path and should added to the list of excluded siplets.  
     */
    protected static final String APP_PATH_HEADER = "Siplets-App-Path";
    
    /**
     * Header contains application router state, next application will 
     * be invoked according to that state. 
     */
    public static final String	COMPOSITION_ID		 = "Composition-Id";
    
    

    /**
     * Separator used for separating the list of Siplets descriptions on the
     * APP_PATH_HEADER . 
     */
    private static final String SIPLETS_SEPARATOR = ";#$";

    /**
     * Indicates whether the call to check top route and see if it is matching
     * our listening point has been already called once. 
     * See the checkTopRouteDestination function for more details. 
     */
    private transient boolean _checkedTopRoute;
    
    /**
     * Holds reference to B2B linked SipServletRequest
     */
    private SipServletRequestImpl _b2bLinkedRequest;
    
    /**
     * Holds the top route header if it had been removed by the container
     * upon receiving this request
     */
    protected Address _poppedRoute = null;

    /**
     * Holds the top route header if it had been removed by the container
     * upon receiving this request
     */
    protected Address _initialPoppedRoute = null;
    /**
     * (JSR 289) composition info used to identify
     * next application in the execution chain.
     */
    private SipApplicationRoutingDirective directive = null;
    private Serializable stateInfo;
    private String nextApplication = null;
    private SipApplicationRoutingRegion routingRegion = null;

    /**
     * Subscriber URI is saved in the request
     * and latter passed to the transactionUserWrapper
     */
    private URI subscriberURI = null;
    
    /**
     * External router means that in the application 
     * composition process concluded that the request
     * should be routed to a different server for
     * application selection. 
     */
    private boolean externalRoute = false;
    private SipURIImpl externalRouteUri = null;

    /**
     * When this flag is true - this incoming request cannot be sent or modified.
     */
	private transient boolean m_isUnmatchedReqeust = false;
    
	/**
     * Copy Constructor for Requests that should be create based on this
     * SipServletRequest.
     * Used for IncomingDeaeSipRequests 
     *
     */
    public SipServletRequestImpl(IncomingSipServletRequest request)
    {
    	super(request);
    	
    	m_requestURI = request.getRequestURI();
      _checkedTopRoute = request.isCheckedTopRoute();
      _b2bLinkedRequest = request.getB2bLinkedRequest();
      _poppedRoute = request.getPoppedRoute();
      _initialPoppedRoute = request.getInitialPoppedRoute();
      subscriberURI= request.getSubscriberURI();     
    }
    
    /**
     * Constructor for Requests that do not have a matching Jain Sip Request at 
     * construction time. 
     * Important: Derived classes that use this constructor MUST overide the 
     * set/get operation that are provided in the base class. Base class 
     * implementation is dependent on the Jain Sip Message object. 
     *
     */
    public SipServletRequestImpl()
    {
    }

    /**
     * Constructs a new Sip Servlet Request based on an existing Jain Sip Request
     * @param request The Jain Sip Request associated with this Sip Servlet Request
     * @param transactionId transaction id associated with this request. 
     * @param provider The Sip Provider that will be used for generating 
     * responses and acknowledgements to the request. 
     */
    public SipServletRequestImpl(
        Request request,
        long transactionId,
        SipProvider provider)
    {
        super(request, transactionId, provider);

        //Get and remove all the composition related info
        extractCompositionInfo();
        
    }

    /**
	 * This method will return the virtual host relate to this sipMessage.
	 * 
	 * @return virutal host
	 */
	public String getVirtualHost() {
		// We will try to rescue the virtual Host from the SipMessage. We have 2
		// options to do it:
		// 1 - is to get it from the transactionUser (is exist)
		// 2 - if it has excludedAppList it will extract the virtual host from
		// one of the applications
		String virtualHost = null;

		TransactionUserWrapper tu = getTransactionUser();
		if (tu != null) {
			// the transactionUser exists
			virtualHost = tu.getSipServletDesc().getSipApp()
					.getVirtualHostName();
		} 
		else if(m_excludeAppsList != null && m_excludeAppsList.size() != 0){
			// try to find if this request has excludedAppList
			Set keySet = m_excludeAppsList.keySet();
			Iterator iter = keySet.iterator();
			String appName = null;
			if(iter.hasNext()){
	            appName = (String)iter.next();
	        }
			if(appName != null){
				SipAppDesc appDesc = SipContainer.getInstance().getSipApp(appName);
				if(appDesc != null){
					virtualHost = appDesc.getVirtualHostName();
				}
			}
		}

		return virtualHost;
	}
    
	
	/**
	 * The method extracts all the headers placed in the 
	 * request that is relevant to only for composition management.
	 * 
	 */
	private void extractCompositionInfo(){
		
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "extractCompositionInfo");
        }

		try {
			
            Header compositionIdHeader = getRequest().getHeader(COMPOSITION_ID, true);
            // if compositionId header exist, we need to retrieve the composition data from the CompositionInfoMap
            // with using compositionId value as the key
            if (compositionIdHeader != null) {
            	 String compositionId = compositionIdHeader.getValue();
            	 // Retrieve the compositionData from the map and copy it to the request's members
            	 CompositionData compositionData = CompositionInfoMap.getInstance().removeCompositionInfo(compositionId);
            	 if ( compositionData == null && c_logger.isTraceDebugEnabled()) {
            		 c_logger.traceDebug(this, "extractCompositionInfo", "Composition Id: " + compositionId + " was not found!");
            	 }
            	 this.setDirective(compositionData.getRoutingDirective());
            	 this.setSubscriberURI(compositionData.getSubscriberUri());
            	 this.setRoutingRegion(compositionData.getRoutingRegion());
            	 this.setStateInfo(compositionData.getStateInfo());
            	 this.setNextApplication(compositionData.getNextApplication());
            	 _initialPoppedRoute = compositionData.getInitialPoppedRoute();
                 getRequest().removeHeader(COMPOSITION_ID, true);
            }
           
		}catch (IllegalArgumentException e) {
			c_logger.error(e.getMessage());
		} catch (HeaderParseException e) {
			c_logger.error(e.getMessage());
		} catch (NullPointerException e) {
			c_logger.error(e.getMessage());
		}

		if (c_logger.isTraceDebugEnabled()){
			
			StringBuffer buff = new StringBuffer();
			
			buff.append("\n")
				.append("Composition data retrieved from the map: \n")
			    .append("----------------- \n")
		        .append("Next application  = " + this.getNextApplication() + " \n")
		        .append("Subscriber uri    = " + this.getSubscriberURI() + " \n")
		        .append("Routing region    = " + this.getRegion() + " \n")
		        .append("Routing directive = " + this.getDirective() + " \n\n");
		        
			c_logger.traceDebug(this, "extractCompositionInfo", buff.toString());
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "extractCompositionInfo");
        }
	}

    /**
     * Checks whether the exclude siplets map needs to be initialized. if so 
     * creates a new empty exclude map. 
     */
    private final void createExcludeMapIfNeeded() 
    {
        if(null == m_excludeAppsList)
        {
            m_excludeAppsList = new HashMap(3);
        }
    }

    /**
     * Helper function gets the Sip Request Object associated with this Servlet
     * Request. 
     */
    public Request getRequest()
    {
        return (Request) getMessage();
    }

    /**
     * @see javax.servlet.sip.SipServletRequest#fromCaller()
     */
    public boolean fromCaller()
    {
        return false;
    }

    /**
     * @see javax.servlet.sip.SipServletRequest#getMaxForwards()
     */
    public int getMaxForwards()
    {
        int rValue = -1;
        Request request = getRequest();
        if (null != request)
        {
            MaxForwardsHeader h;
            try
            {
                h = request.getMaxForwardsHeader();
                if (null != h)
                {
                    rValue = h.getMaxForwards();
                }
            }
            catch (HeaderParseException e)
            {
                if (c_logger.isErrorEnabled())
                {
                    Object[] args = {
                    };
                    c_logger.error(
                        "error.get.max.forwards",
                        Situation.SITUATION_REQUEST,
                        args,
                        e);
                }
            }
        }

        return rValue;
    }

    /**
     * @see javax.servlet.sip.SipServletRequest#getProxy(boolean)
     */
    public Proxy getProxy(boolean create) throws TooManyHopsException
    {
    	if(isUnmatchedReqeust() == true){
    		throw new IllegalStateException("This is unmatched incoming request. It is incactive");
    	}
    	
        SipTransaction transaction = getTransaction();
        if(transaction == null)
        {
            //We should not get here. 
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "getProxy", 
                  "Failed to get proxy - ServerTransaction object unavailable. ");
            }
            
            return null;
            //Anat: In case we are talking about derived Session and 
            //IncomingDeadRequest related to it in B2B mode can happen that we don't
            // have a Transaction and in this case null should be returned.
            // throw ( new IllegalStateException("can not create proxy without transaction"));
        }
        
        TransactionUserWrapper transactionUser = getTransactionUser();
        if(create && transactionUser.isB2B()){
        	throw (
        		new IllegalStateException("the application cannot act as " +
                "Proxy after this application wishes to be a B2BUA"));
        }
        
        if(create && transactionUser.isUAS()){
        	throw (
        		new IllegalStateException("the application cannot act as " +
                "Proxy after this application wishes to be a UAS"));
        }
        
        Proxy proxy = null;
        proxy = transaction.getProxy(false);
        if(!create || proxy != null)
        {
            //Either we already have proxy or we the create flag is set to false.
            return proxy; 
        }
        
        if (transaction.isTerminated())
        {
            //Do not create a proxy if the transaction already terminated.
            throw (new IllegalStateException("Transaction already completed"));
        }
        
        if(!isInitial() )
        {
        	if(proxy!= null){
        		if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getProxy", "not initial request, " +
					"return same proxy object, that was generated for initial request");
				}
        		return proxy;
        	} else {
            //Applications should not attempt to explicitly proxy subsequent
            //requests. The isInitial method on the SipServletRequest interface 
            //returns true if the request is initial as defined above, and can be 
            //used as an indication of whether the application should explicitly 
            //proxy an incoming request or not.
            if (c_logger.isTraceDebugEnabled()) 
            {
                c_logger.traceDebug(this, "getProxy", 
                 "Unable to get Proxy for Request, not initial request: " + this);
            }
            throw new IllegalStateException(
                "Unable to create proxy for subsequent requests on the same dialog");
        	}
        }
        
        //Since a SIP Servlet container cannot know a priori whether an 
        //application will proxy a request or not, it cannot perform the 
        //Max-Forwards check before invoking the application. Instead, the 
        //container performs the check when the application invokes getProxy() 
        //on a SipServletRequest, and if Max-Forwards is 0 a TooManyHopsException 
        //is thrown. If unhandled by the application, this will be caught by 
        //the container which must then generate a 483 (Too many hops) error response.
        int maxForwards = getMaxForwards();
        if (maxForwards == -1)
        {
            //If the copy does not contain a Max-Forwards header field, the
	        //proxy MUST add one with a field value, which SHOULD be 70.
        	addHeader("Max-Forwards", "70");
        }
        else if (maxForwards == 0)
        {
        	//SPR #IESR6AGKQA
        	//if the exception want be handled by the application, the container
        	//will catch it (SipServlet#doRequestImpl) and will generate 483 response
        	
            throw new TooManyHopsException();
        }

        proxy = transaction.getProxy(create);
        transactionUser.setIsProxying(true);
        return proxy; 
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getProxy()
     */
    public Proxy getProxy() throws TooManyHopsException, IllegalStateException
    {
        return getProxy(true);
    }
    
    /**
     * Removed from javax - hide 289 API
     * @see javax.servlet.sip.SipServletRequest#getB2buaHelper()
     */
    abstract public B2buaHelper getB2buaHelper();
    
    /**
     * Returns the B2buaHelperImpl associated with this request. 
      * Invocation of this method also indicates to the container that the 
      * application wishes to be a B2BUA, and any subsequent call to getProxy() 
      * will result in IllegalStateException. 
      * 
      * @param create flag that indicate to set a b2b mode or not
      * @param mode indicate the B2bua mode
      * @return the B2buaHelper for this request, or null in case create flag is
      * 	set to false and we don't already in B2b mode 
      * @throws IllegalStateException - if getProxy() had already been called
     */
    public B2buaHelper getB2buaHelper(boolean create, UAMode mode) throws IllegalStateException {
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SipServletRequestImpl.class.getName(),
					"getB2buaHelper");
		}
    	
    	TransactionUserWrapper transactionUser = getTransactionUser();
    	
    	B2buaHelper b2buaHelper = null;
    	if(transactionUser != null){
    		
//    		TODO if (transaction.isTerminated())
//            {
//                //Do not create a proxy if the transaction already terminated.
//                throw (new IllegalStateException("Transaction already completed"));
//            }
    		
    		b2buaHelper = transactionUser.getB2buaHelper(create, mode);
    	} else {
    		if(create){
	//    		We should not get here. 
	            if (c_logger.isTraceDebugEnabled()) {
	                c_logger.traceDebug(this, "getB2buaHelper", 
	                  "Failed to get B2buaHelper - Transaction object unavailable. ");
	            }
	            
	            throw (
	                new IllegalStateException("can not create getB2buaHelper without transaction"));
    		}
    	}
    	
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(SipServletRequestImpl.class.getName(), "getB2buaHelper",b2buaHelper);
		}
		return b2buaHelper;
	}

    /**
     * @see javax.servlet.sip.SipServletRequest#isInitial()
     */
    public boolean isInitial()
    {
        return m_isInital;
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
        
    	if(getMethod().equals(Request.REGISTER)){
        	isSystemHeader = false;
        }
        
    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(SipServletRequestImpl.class.getName(), "checkIsSystemContactHeader",isSystemHeader);
		}
        return isSystemHeader;
    }

    //
    //javax.servlet.ServletRequest functions. 
    //    
    /**
     * @see javax.servlet.ServletRequest#getLocale()
     */
    public Locale getLocale()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getLocales()
     */
    public Enumeration getLocales()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameter(String)
     */
    public String getParameter(String arg0)
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameterMap()
     */
    public Map getParameterMap()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameterNames()
     */
    public Enumeration getParameterNames()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getParameterValues(String)
     */
    public String[] getParameterValues(String arg0)
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getRealPath(String)
     * @deprecated
     */
    @Deprecated
	public String getRealPath(String arg0)
    {
        //Returns null according to spec. 
        //See Sip Servlets API 1.0 Section 4.3
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getRemoteHost()
     */
    public String getRemoteHost()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getRequestDispatcher(String)
     */
    public RequestDispatcher getRequestDispatcher(String arg0)
    {
        //Returns null according to spec. 
        //See Sip Servlets API 1.0 Section 4.3
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getScheme()
     */
    public String getScheme()
    {
        return getRequestURI().getScheme();
    }

    /**
     * @see javax.servlet.ServletRequest#getServerName()
     */
    public String getServerName()
    {
        return null;
    }

    /**
     * @see javax.servlet.ServletRequest#getServerPort()
     */
    public int getServerPort()
    {
        return 0;
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

    //
    // Internal Functions
    //

    /**
     * Sets the Is Inital flag
     * @param isInital
     */

    public void setIsInital(boolean isInital)
    {
        m_isInital = isInital;
    }

    /**
     * Checks if the specified appliation is in the list of applicationexcluded 
     * from handling this request. This list is built as part of the application 
     * composition process to avoid getting into an infinte loop.
     * The check includes two steps:
     * 1. Checking the Siplet designated as the handler for the session 
     *    associated with this request
     * 2. Checking the exclude list 
     * @param sipApp SIP Application checked whether it is included in the list. 
     * @return If either check is true then the response will be true 
     * otherwise false. 
     */
    public boolean isInExcludeAppsList(SipAppDesc appDesc)
    {
        boolean rc = false;
        TransactionUserWrapper tUser = getTransactionUser();
        
        if (tUser != null)
        {
            SipServletDesc handler = tUser.getSipServletDesc();
            if(null != handler)
            {
                if (appDesc.equals(handler.getSipApp()))
                {
                    rc = true;
                }
            }
            
        }

        //Check the exclude list only if we dont have a matching handler
        if (!rc && m_excludeAppsList != null 
            && m_excludeAppsList.containsKey(getHashkeyForApp(appDesc)))
        {
            rc = true;
        }

        return rc;
    }

    /**
     * Adds the specified Siplets to the list of excluded siplets. 
     * @param map A map containing the list of Siplets ids as the key values.  
     */
    public void addToExcludeSipletsList(HashMap excludeSiplets)
    {
        if(null == excludeSiplets)
        {
            return;
        }
        
        createExcludeMapIfNeeded();
        m_excludeAppsList.putAll(excludeSiplets);
    }

    /**
     * Adds the specified application to the list of excluded apps. 
     * @param appDesc   
     */
    public void addToExcludeAppsList(SipAppDesc appDesc)
    {
        if (null == appDesc)
        {
            return;
        }
        createExcludeMapIfNeeded();
        m_excludeAppsList.put(getHashkeyForApp(appDesc), null);
    }

    /**
     * Helper function: Creates a unique hash key for the specified application
     * This key is used both in the local hash table of exclude siplets and
     * also passed as part of the header added to identify the application path
     * for requests participating in application composition. 
     * @param siplet
     * @return
     */
    private String getHashkeyForApp(SipAppDesc sipApp)
    {
        return sipApp.getApplicationName();
    }

    /**
     * Gets the list of Siplets excluded from being the invoked for this
     * request. 
     */
    public String getExcludeSipletsList()
    {
        if(null == m_excludeAppsList)
        {
            return "";
        }
        
        StringBuffer buffer = new StringBuffer(32);
        Iterator iter = m_excludeAppsList.keySet().iterator();
        while (iter.hasNext())
        {
            buffer.append(iter.next());
            if (iter.hasNext())
            {
                buffer.append(SIPLETS_SEPARATOR);
            }
        }
        return buffer.toString();
    }

    /**
     * Gets the list of excluded Siplets mapped as the keys of the hashtable.
     * @return The exclude map list if exists otherwise null
     */
    public HashMap getExcludedAppsList()
    {
        return m_excludeAppsList;
    }

    /**
     * @see javax.servlet.sip.SipServletRequest#getRequestURI()
     */
    public URI getRequestURI()
    {
    	if (null == m_requestURI)
    	{
	        try
	        {
	            jain.protocol.ip.sip.address.URI jainUri =
	                getRequest().getRequestURI();
	            if (jainUri != null)
	            {
		            String scheme = jainUri.getScheme();
		            if (SipURIImpl.isSchemeSupported(scheme))
		            {
		            	m_requestURI = new SipURIImpl((SipURL) jainUri);
		            }
		            else if (TelURLImpl.isSchemeSupported(scheme))
		            {
		            	m_requestURI = new TelURLImpl(jainUri);
		            }
		            else
		            {
		            	m_requestURI = new URIImpl(jainUri);
		            }
	            }
	        }
	        catch (SipParseException e)
	        {
	            if (c_logger.isErrorEnabled())
	            {
	                Object[] args = { getRequest()};
	                c_logger.error(
	                    "error.get.request.uri",
	                    Situation.SITUATION_REQUEST,
	                    args,
	                    e);
	            }
	        }
    	}

        return m_requestURI;
    }
    
    /**
     * Perform validity checks before pushing route
     */
    private void validatePushRoute(){
    	if(isCommitted())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
        //Check if the top route is our own listening point - if so remove
        //it as we either reached our destination (proxy mode) or undesired
        //behavior in the application which will cause to get into a loop
    	boolean isIncomingRequest = false;
        checkTopRouteDestination(isIncomingRequest);
    }
    
    /**
     * @see javax.servlet.sip.SipServletRequest#pushRoute(javax.servlet.sip.SipURI)
     */
    public void pushRoute(SipURI uri)
    {
    	validatePushRoute();
    	
        jain.protocol.ip.sip.address.SipURL jainSipUri =
            ((SipURIImpl) uri).getJainSipUrl();
        NameAddress address =
            getAddressFactory().createNameAddress(jainSipUri);

        setRouteHeader(address);
    
    }
    
    /**
     * Set a NameAddress address to the route header, and add the header to the request
     * @param address
     */
    private void setRouteHeader(NameAddress address){
    	try{
	    	RouteHeader routeH =
	            getHeadersFactory().createRouteHeader(address);
	
	        addHeader(routeH, true);
    	}
        catch (IllegalArgumentException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { address };
                c_logger.error(
                    "error.push.route",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { address };
                c_logger.error(
                    "error.push.route",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }
    /**
	 * @see javax.servlet.sip.SipServletRequest#pushRoute(javax.servlet.sip.Address)
	 */
	public void pushRoute(Address uri) {
		
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "pushRoute", 
                "Pushing address to route header" + uri);
        }

		validatePushRoute();
		
		if( !(uri instanceof AddressImpl) ||
			!(uri.getURI() instanceof SipURIImpl)){
			throw new IllegalArgumentException("Pushing a non-SIP route header to this request is illegal");
		}
		
		setRouteHeader(((AddressImpl)uri).getNameAddressHeader().getNameAddress());	
	}
	
    /** 
     * @see javax.servlet.sip.SipServletRequest#pushPath(javax.servlet.sip.Address)
     */
    public void pushPath(Address uri){
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
    	if(getMethod()!= Request.REGISTER){
    		throw new IllegalStateException(
            "Can not add Path header to non REGISTER request.");
    	}
    	
    	jain.protocol.ip.sip.address.SipURL jainSipPathUri =
            ((SipURIImpl) uri.getURI()).getJainSipUrl();
        NameAddress address =
            getAddressFactory().createNameAddress(jainSipPathUri);

        NameAddressHeader pathHeader;
    	//createNameAddress(uri.getURI());
		
    	pathHeader = new GenericNameAddressHeaderImpl(PathHeader.name);
		
    	pathHeader.setNameAddress(address);

		addHeader(pathHeader, true);
    	
    }
    
    /**
     * Helper function for pushing record route headers to a request from a 
     * given javax.sip.servlet.SipURI
     */
    public void pushRecordRoute(SipURI uri)
    {
        try
        {
            jain.protocol.ip.sip.address.SipURL jainSipUri =
                ((SipURIImpl) uri).getJainSipUrl();
            NameAddress address =
                getAddressFactory().createNameAddress(jainSipUri);

            RecordRouteHeader recordRouteH =
                getHeadersFactory().createRecordRouteHeader(address);

            addHeader(recordRouteH, true);
        }
        catch (IllegalArgumentException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { uri };
                c_logger.error(
                    "error.push.route",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { uri };
                c_logger.error(
                    "error.push.route",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }

    /**
     * Helper Function - Adds the specified Header to the list of Headers at the
     * specified position (first or last)
     * @param header Jain Sip Header
     * @param first
     */
    public void addHeader(Header header, boolean first)
    {
        getRequest().addHeader(header, first);
    }

   
    
     /**
     * @see javax.servlet.sip.SipServletRequest#getReader()
     */
    public BufferedReader getReader() throws IOException {
        return null;
    }
    
    /**
     * @see javax.servlet.sip.SipServletRequest#getInputStream()
     */
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }
    
    
    /**
     * Checks the top route. If the top route indicates the local ip/host then
     * it is removed as we already reached our destination. We will not remove the
     * top route if it contains a session identifier which indicates that we
     * are in an application composition scenario and that there are more applications
     * waiting for this request.   
     */
    public void checkTopRouteDestination(boolean isIncomingRequest)
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "checkTopRouteDestination", new Boolean(isIncomingRequest));
        }
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug("checkTopRouteDestination: request="+ this);
    	}
        //This check should only be performed once. Either when the message is 
        //actually sent or when the application calls the push route for the first
        //time. So we remove the route header if it was put externally but if the 
        //application decides for some reason to add itself as the route for
        //request we would still send it to ourself (I don't see the need for
        //such behavior but in any case we support it). 
        //Note that due to this behavior calling push route on a incoming request 
        //might cause the message to drop the top route header and as result
        //not match the initial message that came in. 
        if(_checkedTopRoute)
        {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "checkTopRouteDestination", 
                    "Top Route already removed - not checking anymore");
            }
            return;
        }
        
        _checkedTopRoute = true;
        Request request = getRequest();
        
        try {
           boolean doubleRecordRoute = isDoubleRecordRoute(request);
           String removedSessionId = removeRoute(request, isIncomingRequest);
           
           //if we removed a route with the container sessionId we need to check 
           //the next route to see if it was added by the double record route mechanism 
           //and if so we need to remove it too
           if (removedSessionId != null && doubleRecordRoute){
        	   if (c_logger.isTraceDebugEnabled()) {
                   c_logger.traceDebug(this, "checkTopRouteDestination", 
                       "Trying to remove another route header in case that it was added by the double record route mechanism");
               }
        	   removeRoute(request, false);
           }
        }
        catch (ServletParseException e1) {
            logException(e1);
        }
        catch (HeaderParseException e2) {
            logException(e2);
        }
        catch (NoSuchElementException e) {
            logException(e);
        }
        
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "checkTopRouteDestination");
        }
    }
        
	    /**
	     * Check whether we suspect that we are in a Double Record Route Scenario. If the top two 
	     * Route headers contain the ibmdrr parameter we suspect that we are in  a Double Record Route Scenario.
	     * However we still need to check that both Route headers are addressed for local listening points
	     * we do that in removeRoute method.   
	     */
    	private boolean isDoubleRecordRoute(Request request) {
    		if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceEntry(this, "isDoubleRecordRoute");
            }
    		boolean rc = false;
    		try {
				HeaderIterator iter =  request.getHeaders(RouteHeader.name);
				if(iter != null) {
					NameAddressHeader firstRoute = iter.hasNext() ? (NameAddressHeader) iter.next() :null;
					NameAddressHeader secondRoute = iter.hasNext() ? (NameAddressHeader) iter.next() :null;
					if(firstRoute != null && secondRoute != null) {
						jain.protocol.ip.sip.address.URI firstRouteUri = firstRoute.getNameAddress().getAddress();
						jain.protocol.ip.sip.address.URI secondRouteUri = secondRoute.getNameAddress().getAddress();
						if((firstRouteUri instanceof SipURL) && (secondRouteUri instanceof SipURL)) {
							SipURL firstURL = (SipURL) firstRouteUri;
							SipURL secondURL = (SipURL) secondRouteUri;
							if(firstURL.hasParameter(SipProxyInfo.IBM_DOUBLE_RECORD_ROUTE_PARAMETER) 
									&& secondURL.hasParameter(SipProxyInfo.IBM_DOUBLE_RECORD_ROUTE_PARAMETER)) {
								rc = true;
								return rc;
							}
						}
					}
				}
			} catch (HeaderParseException e1) {
				logException(e1);
			} catch (IllegalArgumentException e2) {
				logException(e2);
			} catch (NoSuchElementException e3) {
				logException(e3);
			}finally{
				if (c_logger.isTraceEntryExitEnabled()) {
		            c_logger.traceExit(this, "isDoubleRecordRoute", rc);
		        }
			}
        	return rc;
        }
        
        private String removeRoute(Request request, boolean setPoppedRoute) throws ServletParseException, HeaderParseException, IllegalArgumentException{
        	NameAddressHeader route = (NameAddressHeader) request.getHeader(RouteHeader.name, true);
        	String removedSID = null;
        	
            if (route != null) {

                jain.protocol.ip.sip.address.URI routeUri = route
                        .getNameAddress().getAddress();
                if (routeUri instanceof SipURL) {
                    SipURL url = (SipURL) routeUri;
                    
                    //Check if Route contains session identifier - could be app
                    // composition. if found stop checking as we don't want to remove 
                    //this header because we want to get to the next app on this server
                    String sessionId = "";
                    TransactionUserWrapper tu = getTransactionUser();
                    if (tu != null){
                    	//if it is a derived session we need the original tu
                    	sessionId = tu.getSharedIdForDS();
                    }
                    String sessionParamID = url.getParameter(TransactionUserImpl.SESSION_RR_PARAM_KEY);
                    
                    if (sessionParamID == null || sessionParamID.equals(sessionId)) {
                        String host = url.getHost();
                        int port = url.getPort();
                        boolean secure = url.getScheme().equalsIgnoreCase("sips") || ("tls").equalsIgnoreCase(url.getTransport());

                        if (checkIsLocalListeningPoint(host, port, secure)) {

                        	// TODO Do we need to set popped route in case of call from 
                        	//pushRoute method ????
                        	
                        	//no need to save the popped route header if we're not incoming
                        	if (setPoppedRoute){
                            	// save the popped route header
                                setPoppedRoute(getAddressHeader(RouteHeader.name));
                        	}
                        	
                            request.removeHeader(RouteHeader.name, true);
                            removedSID = sessionParamID;
                        }
                    }
                }
            }else{
            	if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this,
                        "removeRoute", "There is no route header");
                }
            }
            
            if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceExit(this, "removeRoute", removedSID);
            }
            return removedSID;
                            
        }


    /**
	 * Checks if the specified host/port match the local listening point.
	 * 
	 * @param host host name or IP address
	 * @param port port number
	 * @param secure true if sips, false if sip
	 * @return true if matches otherwise false
	 */
    public boolean checkIsLocalListeningPoint(String host, int port, boolean secure) 
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(null,"checkIsLocalListeningPoint", 
            					new String[]{host,
            								String.valueOf(port),
            								String.valueOf(secure)});
        }
        boolean rc = false;
        
        
        Iterator<ListeningPointImpl> lpIter = SipContainer.getInstance().getListeningPoints();
        if(lpIter != null) {
	        while(lpIter.hasNext()) {
	        	ListeningPointImpl lpoint = lpIter.next();
	        	int lpointPort = lpoint.getPort();
	            if (isSamePort(lpointPort, port, secure))
	            {
	            	// compare host address.
	            	// note that we treat one special case, where the addresses
	            	// are different, as equal: if the container is listening to
	            	// "0.0.0.0" and the message arrived on "127.0.0.1", we would
	            	// like to consider it as equal addresses. this follows the
	            	// rationale that no other process, and no other listening point,
	            	// is taking this port (we're listening to "all") and therefore
	            	// "127.0.0.1" is not destined to anyone else on this machine.
	                if (SIPStackUtil.isSameHost(lpoint.getHost(), host) ||
	                	(lpoint.isAnyAddress() && isLocalHost(host)))
	                {
	                    rc = true;
	                    break;
	                }
	           }
	        }
        }
        else
        {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "checkIsLocalListeningPoint", 
                    				"Warning no listening points availalbe ");
            }
        }
        
        
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("checkIsLocalListeningPoint: " + rc);
        }
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(null,"checkIsLocalListeningPoint");
        }
        return rc;
    }

    /**
     * compares the two port numbers
     * 
     * @param port1 a port number
     * @param port2 another port number
     * @param secure true if sips, false if sip
     * @return true if equal, otherwise false
     */
    private static boolean isSamePort(int port1, int port2, boolean secure) {
        if (port1 == -1) {
            port1 = secure ? 5061 : 5060;
        }
        if (port2 == -1) {
            port2 = secure ? 5061 : 5060;
        }
        return port1 == port2;
    }
    


    /**
     * determines if the given host name or IP address represents
     * the local "loopback" adapter.
     * @param host a host name or IP address
     * @return true if the local "loopback" adapter, false otherwise
     */
    private static boolean isLocalHost(String host) {
    	return host.equals("127.0.0.1") || host.equals("::1")
    		|| host.equals("localhost");
    }

    /**
     * @see javax.servlet.ServletRequest#getLocalName()
     */
    public String getLocalName() {
//        Added to support J2ee 1.4 interface
        return null;
    }
    /**
     * We need this method for WebContainer internal use
     * @see com.ibm.wsspi.webcontainer.servlet.IServletRequestWrapper#getWrappedRequest()
     */
    public ServletRequest getWrappedRequest(){
    	return getHttpServletRequest();
    }
	
    /**
     * Removed from javax - hide 289 API
     * Add auth headers to the request.
     * @param response The response that created the auth challange. 
     * @param authInfo The auth info object.
     */
    public void addAuthHeader(SipServletResponse response, 
    		AuthInfo authInfo)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
    	// Though the API requires us to get an AuthInfo object, we can
    	// be sure it is a writable object, since we created it ourselves.
    	AuthInfoImpl writeableInfo = (AuthInfoImpl)authInfo;
    	SipServletResponseImpl responseImpl = (SipServletResponseImpl)response; 
    	writeableInfo.writeToRequest(this, responseImpl);
    }

    /**
     * Removed from javax - hide 289 API
     * Add the given authentication header to the request.
     * @param challangeResponse The response that created the auth challange. 
     * @param userName The auth username.
     * @param password The auth password.
     */
    public void addAuthHeader(SipServletResponse challangeResponse, 
    		String userName, String password)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        } 
    	
    	AuthInfo authInfo = AuthInfoFactory.createAuthInfo();
    	int statusCode = challangeResponse.getStatus();
    	for (Iterator iter = challangeResponse.getChallengeRealms(); iter.hasNext();) {
			String element = (String) iter.next();
			authInfo.addAuthInfo(statusCode, element, userName, password);
	    	addAuthHeader(challangeResponse, authInfo);
		}
    }
    
    /**
     * directive getter
     * @return
     */
	public SipApplicationRoutingDirective getDirective() {
		return directive;
	}

	/**
	 * directive setter
	 * @param directive
	 */
	public void setDirective(SipApplicationRoutingDirective directive) {
		this.directive = directive;
	}
	
	/**
	 * stateInfo getter
	 * @return
	 */
	public Serializable getStateInfo() {
		return stateInfo;
	}

	/**
	 * stateInfo setter
	 * @param stateInfo
	 */
	public void setStateInfo(Serializable stateInfo) {
		this.stateInfo = stateInfo;
		if (stateInfo != null && stateInfo instanceof StateInfo) {
			StateInfo info = (StateInfo)stateInfo;
			StateInfo temp = new StateInfo();
			temp.setIndex(info.getIndex());
			this.stateInfo = temp;
		}
	}

	/**
	 * nextApplication getter
	 * @return
	 */
	public String getNextApplication() {
		return nextApplication;
	}

	/**
	 * nextApplication setter
	 * @param nextApplication
	 */
	public void setNextApplication(String nextApplication) {
		this.nextApplication = nextApplication;
	}
	
	/**
	 * @see javax.servlet.sip.SipServletRequest#getRegion()
	 */
    public SipApplicationRoutingRegion getRegion() {
		return routingRegion;
	}

    /**
     * routing region setter
     * @param routingRegion
     */
	public void setRoutingRegion(SipApplicationRoutingRegion routingRegion) {
		this.routingRegion = routingRegion;
	}
	
	/**
	 * subscriberURI getter
	 * @return
	 */
	public URI getSubscriberURI() {
		return subscriberURI;
	}

	/**
	 * subscriberURI Setter
	 * @param subscriberURI
	 */
	public void setSubscriberURI(URI subscriberURI) {
		this.subscriberURI = subscriberURI;
	}
	
 
    /**
	 * Link the b2b peer SipServletRequest
	 * @param req the request to link to current request
	 */
	public void linkSipRequest(SipServletRequestImpl req) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SipServletRequestImpl.class.getName(),
					"linkSipRequest");
		}
		
		_b2bLinkedRequest = req;
	}
	
	/**
	 * Returns the B2B linked SipServletRequest, or null if none
	 */
    public SipServletRequestImpl getLinkedRequest() {
    	//check first if the request session is linked to another session
    	SipSessionImplementation ss = (SipSessionImplementation) getSession(false);
    	if (ss == null || ss.getLinkedSession() == null){
    		//SipSession does not exist or the linked session does not exist, 
    		//we can nullify the linkedRequest just in case that it was previously linked and it was unlinked 
    		//we are unlinking the requests in a lazy way to avoid saving them just on the sipsession just in case
    		//that we will need to unlink them in the future
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getLinkedRequest", "Sessions are not linked, request is unlinked");
			}
    		_b2bLinkedRequest = null;
    	}
    	
    	return _b2bLinkedRequest;
    }
    
    /**
     * Removed from javax - hide 289 API
     * @see javax.servlet.sip.SipServletRequest#getPoppedRoute()
     */
    public Address getPoppedRoute(){
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _poppedRoute};
			c_logger.traceEntry(SipServletRequestImpl.class.getName(),
					"getPoppedRoute", params);
		}
    	return _poppedRoute;
    }

    /**
     * Set the top route header that had been removed by the container
     * @param the removed popped route header
     */
    public void setPoppedRoute(Address route){
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { route };
			c_logger.traceEntry(SipServletRequestImpl.class.getName(),
					"setPoppedRoute", params);
		}
    	_poppedRoute = route;
    	RequestImpl jReq = (RequestImpl)getRequest();
    	if(!jReq.isLoopback()){//means this is coming from the outside
    		_initialPoppedRoute = route;
    	}
    }

    /**
     * @see javax.servlet.sip.SipServletRequest#getInitialPoppedRoute()
     */
    public Address getInitialPoppedRoute() {
    	// TODO application composition: return the Route that came in from the
    	// network, and not a different Route for each application on the chain.
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { _initialPoppedRoute};
			c_logger.traceEntry(SipServletRequestImpl.class.getName(),
				"getInitialPoppedRoute", params);
		}
    	return _initialPoppedRoute;
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
	 * @see javax.servlet.sip.SipServletRequest#getRoutingDirective()
	 */
	public SipApplicationRoutingDirective getRoutingDirective()
			throws IllegalStateException {
		if (!isInitial()) {
			throw new IllegalStateException("request is not initial");
		}
		return directive;
	}

	/**
	 * Setter for externalRouter 
	 * @return
	 */
	public boolean isExternalRoute() {
		return externalRoute;
	}

	/**
	 * Getter for externalRouter 
	 */
	public void setExternalRoute(boolean externalRoute) {
		this.externalRoute = externalRoute;
	}
	
	/**
	 * Unlink the b2b linked request
	 */
	public void unLinkSipRequest() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SipServletRequestImpl.class.getName(),
					"unLinkSipRequest");
		}
		
		_b2bLinkedRequest = null;
	}
	
	
	/**
	 * That method should be implemented in OutgoingSipServletRequest
	 * only, intent to send initial request to application router through
	 * check for composition process.
	 */
	public void imitateInitialForComposition(){
	}
	
	/**
	 * That method should be implemented in OutgoingSipServletRequest
	 * only, intent to send initial request to application router through
	 * check for composition process.
	 */
	public void cleanInitialForComposition(){
	}

	/**
	 * Composition error responses implemented in different ways
	 * if it is incoming or outgoing request.
	 *
	 */
    public abstract void processCompositionErrorResponse();

    /**
     * Used in OutgoingSipServletRequest for application composition	
     * JSR116 backward compatibility 
     * @return
     */
	public abstract boolean isAppInvoked116Type();
	public abstract String getAppInvokedName();
	public abstract void setAppInvokedName(String appInvokedName);

	/**
	 * Getter for _checkedTopRoute
	 * @return
	 */
	public boolean isCheckedTopRoute() {
		return _checkedTopRoute;
	}

	/**
	 * _checkedTopRoute setter
	 * @param topRoute
	 */
	public void setCheckedTopRoute(boolean topRoute) {
		_checkedTopRoute = topRoute;
	}

	/**
	 * _b2bLinkedRequest getter
	 * @return
	 */
	public SipServletRequestImpl getB2bLinkedRequest() {
		return _b2bLinkedRequest;
	}
	
	/**
     * When the request is routed across some nodes in the SIP net, 
     * the headers that are cleaned in that method are useful, 
     * but if the request is routed to the UAS the headers should
     * not appear in the request.
     */
	abstract public void cleanExpiredCompositionHeaders();
	

    /**
     * @see javax.servlet.sip.SipServletRequest#setMaxForwards(int)
     */
    public void setMaxForwards(int n) {
    	// validate input and state
    	if (isCommitted() && isJSR289Application()) {
	        throw new IllegalStateException("Can not modify committed message");
	    }

        if (n < 0 || n > 255) {
        	throw new IllegalArgumentException("Max-Forwards value out of range ["
        		+ n + ']');
        }

        // modify the message
        try {
        	Request request = getRequest();
            MaxForwardsHeader maxForwards = request.getMaxForwardsHeader();
            if (maxForwards == null) {
            	maxForwards = getHeadersFactory().createMaxForwardsHeader(n);
            	request.setMaxForwardsHeader(maxForwards);
            }
            else {
            	maxForwards.setMaxForwards(n);
            }
        }
        catch (HeaderParseException e) {
            logException(e);
        }
        catch (SipParseException e) {
            logException(e);
        }
    }
    
    
    
    /**
     * Construct jain.protocol.ip.sip.address.URI from javax.servlet.sip.URI
     * 
     * @param fromURI
     * @return jain.protocol.ip.sip.address.URI
     * @throws IllegalArgumentException
     * @throws SipParseException
     */
    protected jain.protocol.ip.sip.address.URI createJainRequestURI(URI fromURI)
       throws IllegalArgumentException,SipParseException {
        
        jain.protocol.ip.sip.address.URI ruri = null; 
        
        if(fromURI instanceof URIImpl)
        {
            ruri = (jain.protocol.ip.sip.address.URI)(((URIImpl)fromURI).getJainURI()).clone();
        }
        else
        {
            // Patch: Remove the Sip prefix from the URI string representation
            String uri = fromURI.toString();
            uri = uri.substring(uri.indexOf(':') + 1);
            ruri = getAddressFactory().createURI(fromURI.getScheme(), uri);
        }

        return ruri;
    }
    
    
    /**
     * @see javax.servlet.sip.SipServletRequest#setRequestURI(URI)
     */
    public void setRequestURI(URI uri)
    {
    	if(isCommitted())
	    {
    		if(isJSR289Application()){    			
    			throw new IllegalStateException(
    					"Can not modify committed message");
    		} else {
    			if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "setRequestURI", 
					"Set Request URI Failed, Message already Committed");
				}
    			
    			return;
    		}
	    }

    	if (uri == null) {
    		// do what the javadoc says
    		throw new NullPointerException("null URI");
    	}
    	
    	
        if( m_requestURI == uri)
        {
        	if( c_logger.isTraceEntryExitEnabled())
        		c_logger.traceExit(null, "setRequestUri", "same requestUri set");
        	return; // If the application reset the same URI, no need to change anything
        }    	
    	
		try {
			//This cloning is done for the case where this method is called from within the container, when creating
			//another request based on a previous one. This is unfortunate since when this method is called 
			//from the app with a new URI, it expect it to be the same URI instance on a subsequent get.
			//This is a temp fix for a customer, and the code needs to be changed so that the container factories will
			//call a different method then this one shouldn't do cloning.
			jain.protocol.ip.sip.address.URI reqURI = createJainRequestURI(uri);
			
			//fix the target in case it has anything illegal or deprecated
			Request req = getRequest();
	        req.setRequestURI(reqURI);
	        c_logger.traceDebug(
                    "previous requestUri is cleared. New uri=" +uri +", old uri=" + m_requestURI);
	        m_requestURI = null; // reset the cached request URI
	        
	        if( c_logger.isTraceEntryExitEnabled())
        		c_logger.traceExit(null, "setRequestUri", m_requestURI);
		} catch (IllegalArgumentException e) {
			logException(e);
		} catch (SipParseException e) {
			logException(e);
		}
    }
    
    /**
     * A stab method to support Servlets 3.0
     * @return
     */
    public boolean isAsyncSupported() {
    	return false;
    }
    
    /**
     * Returns true if this is unmatched reqeust.
     * @return
     */
    public boolean isUnmatchedReqeust() {
		return m_isUnmatchedReqeust;
	}
    
    /**
     * Set the m_isUnmatchedReqeust to true
     */
    public void setUnmatchedReqeust() {
		m_isUnmatchedReqeust = true;
	}
}