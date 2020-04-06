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

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.AddressFactory;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.AcceptLanguageHeader;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.ContentLengthHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;
import jain.protocol.ip.sip.header.ExpiresHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderFactory;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.ParametersHeader;
import jain.protocol.ip.sip.header.RecordRouteHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.MessageFactory;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RAckHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RSeqHeader;
import com.ibm.ws.jain.protocol.ip.sip.header.ExtendedHeader;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericNameAddressHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericParametersHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.NameAddressHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.servlets.ext.SipServletMessageExt;
import com.ibm.ws.sip.container.transaction.SipTransaction;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.virtualhost.VirtualHostAlias;
import com.ibm.ws.sip.parser.HeaderCreator;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.stack.properties.StackProperties;
import com.ibm.ws.sip.stack.util.SipStackUtil;
//TODO Liberty import com.ibm.ws.management.AdminHelper;

/**
 * @author Amir Perlman, Feb 16, 2003
 *
 * Implemenation for the SipServletMessage API
 */
public abstract class SipServletMessageImpl 
	implements SipServletMessageExt, Serializable
	 /*
	  * Anat: SPR #RDUH6C323P Serializable added for:
	  * SipServletMessage can be added as an attribute to the
	  * session or appSession and will be replicated after the
	  * dialog will be established
	  */
{
	
	/**
	 * The message attribute to set with the arrival time
	 */
    private static final String SIP_MESSAGE_ARRIVAL_TIME = "sip.message.arrival.time";

	/** Serialization UID (do not change) */
    static final long serialVersionUID = 2937453914520727560L;

    public static enum MessageType {INCOMING_REQUEST, OUTGOING_REQUEST, INCOMING_RESPONSE, OUTGOING_RESPONSE };
    /**
     * Content Language
     */
    protected static final String CONTENT_LANGUAGE = "Content-Language";

    /**
     * Text mime type
     */
    private static final String TEXT_MIME_TYPE = "text";

    /**
     * used by the digest TAI to tell the container that the user is not really authenticated
     */
    public static final String UNAUTHENTICATED = "com.ibm.sip.unauthenticated";
    
    /**
     * Default charset encoding for SIP is UTF-8
     */
    private static final String DEFAULT_CHARSET_ENCODING = "UTF-8";
    
    private static final String INITIAL_REMOTE_TAG = "initial-remote";
    
    private static final String LOCAL_ADDRESS_TAG = "local-address";

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipServletMessageImpl.class);

    /**
     * The Sip Message object wrapped by this Sip Servlet Message. 
     * The implementation in this class assumes that this object is initialized
     * at construction time. Derived classes that do not have a valid assignment
     * for this object at construction need to override the get/set methods to 
     * avoid a null pointer exception. 
     */
    private Message m_message;

    /**
     * The SIP Provider associated with this message. 
     */
    protected transient SipProvider m_sipProvider;

    /**
     * The transaction Id associated with this message. 
     */
    private transient long m_transactionId;

    /**
     *  The owner transaction of this message
     */
    private transient SipTransaction m_transaction;

    /**
     * The Message's from address.
     * Can be transient because can be created at any time
     */
    private transient Address m_fromAddress;

    /**
     * The Message's to address.
     * Can be transient because can be created at any time
     */
    private transient Address m_toAddress;
    
    /**
     * The Original Message's Contact header.
     * Can be transient because can be created at any time
     */
    protected Address _origContact;
    
    /**
     * Hold client transport info, from "IBM-CLIENT-ADDRESS" header
     */
    protected transient String m_clientAddress;

//    Remove
//    /**
//     * The Sip Session associated with this message. 
//     */
//    private transient SipSession m_sipSession;
    

    /**
     * The Transaction User associated with this message
     */
    protected TransactionUserWrapper m_transactionUser;

     /**
     * Map of message attributes
     */
    protected Hashtable m_attributes;

    /**
     * Flag indicating whether the Message is already commited. Some operations
     * are not allowed once the message is commited. 
     */
    private boolean m_isCommited = true;
    
    
    /**
     * Flag that will show if object was failovered or not
     */
    
   private boolean m_isFailovered = false;

    /**
     * Http Servlet Request associated with this message. Used for accessing
     * WAS security/authentication API. 
     * Can be transient - nobody will use it after failover
     */
    private transient HttpServletRequest m_httpServletRequest = null;
    
    /**
     * Http Servlet Response associated with this message. Used for forwarding 
     * message between servlets.
     * Can be transient - nobody will use it after failover
     */
    private transient HttpServletResponse m_httpServletResponse = null;

    /**
     * Flag indicating whether application composition routing mode is 
     * enabled. By default it is enabled. 
     */
    protected static boolean c_appCompositionEnabled = 
    	CoreProperties.ENABLE_APP_COMPOSITION_DEFAULT;
    
	protected static final String TLS = "tls";

	protected static final String UDP = "udp";

	protected static final String SIPS = "sips";

    /**
     * Used for getAcceptLanguages in case of no header
     */
    private static transient List<Locale> c_defaultLocales;
    
    /**
     * the time <msec> when the request arrived
     * Can be transient because used only for send response that establish
     * the dialog. and WLM calculations
     */
    
    private transient long m_arrivedTime = -1;
    
    /**
     * The Message's remote address.
     * Can be transient because can be created at any time
     */
    protected transient String m_initialRemoteAddr = null;
    
    /**
     * The Message's remote port.
     * Can be transient because can be created at any time
     */
    protected transient int m_initialRemotePort = -1;    
    
    /**
     * The Message's remote address.
     * Can be transient because can be created at any time
     */
    protected transient String m_remoteAddr = null;
    
    /**
     * The Message's remote port.
     * Can be transient because can be created at any time
     */
    protected transient int m_remotePort = -1;
    
    /**
     * The Message's local address.
     * Can be transient because can be created at any time
     */
    protected transient String m_localAddr = null;
    
    /**
     * The Message's local port.
     * Can be transient because can be created at any time
     */
    protected transient int m_localPort = -1;
    
    /**
     * The message's initial transport 
     * Can be transient because can be created at any time
     */
    protected transient String m_initialTransport = null; 
    
    /**
     * Flag indicating whether to update Un-CommittedMessagesList later,
     * when transaction user will be already set 
     */
    private transient boolean _updateMessageListLater = false;

    /**
     * Flag indicating whether the contact header is a default one
     * created due to a getHeader("Contact") call, at a stage when the contact
     * header did not exist  
     */
    protected transient boolean m_isDefaultContactHeader = false;
    
    /** 
	 * Indicates whether this response is generated internally
	 */
	protected boolean _isInternallyGenerated = false;

    //
    //Static initializer
    // 
    static {
        PropertiesStore store = PropertiesStore.getInstance();
        c_appCompositionEnabled =
            store.getProperties().getBoolean(CoreProperties.ENABLE_APP_COMPOSITION);

        if (!c_appCompositionEnabled)
        {
            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug("SipServletMessageImpl", "Static Init",
                    "Application Composition is Disabled");
            }
        }


    }

    /**
     * Constructor Requests/Response that do not have a matching Jain Sip 
     * Message at construction time. 
     * Important: Derived classes that use this constructor MUST overide the 
     * set/get operation that are provided in the base class. This 
     * implementation is dependent on the Jain Sip Message object. 
     *
     */
    public SipServletMessageImpl()
    {
    }
    
    /**
     * This method used to create a message based on IncomingSipServletRequest.
     * This message will not be exact copy of the incoming request and SHOULD NOT
     * not used as fully copy constractor for future fully use of new created
     * request !!!
     * @param request
     */
    public SipServletMessageImpl(IncomingSipServletRequest request)
    {
    	if (c_logger.isTraceDebugEnabled()) {
				c_logger
						.traceDebug(this, "SipServletMessageImpl",
								"Dead copy of incoming request created !!! - this requeset is not for use");
			}
			m_message = request.getMessage();
			m_fromAddress = request.getFrom();
			m_toAddress = request.getTo();
			_origContact = request._origContact;
			m_clientAddress = request.m_clientAddress;
			m_attributes = request.m_attributes;
			m_httpServletRequest = request.getHttpServletRequest();
			m_httpServletResponse = request.getHttpServletResponse();
			m_arrivedTime = request.getArrivedTime();
			m_remoteAddr = request.getRemoteAddr();
			m_remotePort = request.getRemotePort();
			m_localAddr = request.getLocalAddr();
			m_localPort = request.getLocalPort();
			m_initialRemoteAddr = request.getInitialRemoteAddr();
			m_initialRemotePort = request.getInitialRemotePort();
			m_initialTransport = request.getInitialTransport();
			_updateMessageListLater = getUpdateMessageListLater();
			m_isDefaultContactHeader = getIsDefaultContactHeader();
    }

    public boolean getUpdateMessageListLater()
    {
        return _updateMessageListLater;
    }

    public boolean getIsDefaultContactHeader()
    {
        return m_isDefaultContactHeader;
    }

    /**
     * Constructs a new Sip Servlet Message based on an existing Jain Sip Message
     * @param message The Jain Sip Message associated with this Sip Servlet Message
     * @param transactionId transaction id associated with this message. 
     * @param provider The Sip Provider that will be used for generating 
     * requests/responses and acknowledgements for this message.
     * @pre message != null
     * @pre transactionId != null
     * @pre provider != null
     */
    public SipServletMessageImpl(
        Message message,
        long transactionId,
        SipProvider provider)
    {
        this();
        if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { this.getClass().getName() + "@" + Integer.toHexString(this.hashCode())};
			c_logger.traceEntry(SipServletMessageImpl.class.getName(),
					"SipServletMessageImpl", params);
		}
		try {
			// read client transport info from header and save it for future usage.
			Header header = message.getHeader(SipUtil.IBM_CLIENT_ADDRESS, true);
			if(header!=null){        	
				m_clientAddress = header.getValue();
				message.removeHeader(SipUtil.IBM_CLIENT_ADDRESS, true);
			}
		} catch (Exception e) {
			// no valid IBM_CLIENT_ADDRESS
			if (c_logger.isTraceDebugEnabled())
	        {
	            c_logger.traceDebug("SipServletMessageImpl", 
	            	"SipServletMessageImpl", "there is no valid IBM_CLIENT_ADDRESS header");
	        }
		} 
		
		m_message = message;
		m_transactionId = transactionId;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "SipServletMessageImpl", "provider=" + provider);
		}
		m_sipProvider = provider;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getAcceptLanguage()
     * @pre m_message != null
     */
    public Locale getAcceptLanguage()
    {
        Locale rValue = null;

        HeaderIterator iterator = m_message.getAcceptLanguageHeaders();
        if (iterator != null && iterator.hasNext())
        {
            try {
        		AcceptLanguageHeader header = (AcceptLanguageHeader)iterator.next();
    			String acceptLanguage = header.getLanguageRange();
                float qValue = header.getQValue();

                // find the highest priority value
    			while (iterator.hasNext()) {
            		header = (AcceptLanguageHeader)iterator.next();
            		if (header.getQValue() > qValue) {
            			acceptLanguage = header.getLanguageRange();
            			qValue = header.getQValue();
            		}
            	}
                rValue = getLocale(acceptLanguage);
            } catch (HeaderParseException e) {
                if (c_logger.isErrorEnabled()) {
                    Object[] args = { this };
                    c_logger.error(
                        "error.get.accept.language",
                        Situation.SITUATION_REQUEST,
                        args,
                        e);
                }
            } catch (NoSuchElementException e) {
                if (c_logger.isErrorEnabled()) {
                    Object[] args = { this };
                    c_logger.error(
                        "error.get.accept.language",
                        Situation.SITUATION_REQUEST,
                        args,
                        e);
                }
            }
        }
        
        if (!isJSR289Application()) {
            if (rValue == null) {
                rValue = Locale.getDefault();
            }        	
        }

        return rValue;
    }

    /**
     * converts language code from Java Locale object to SIP 
     * @param locale language code as Java Locale object
     * @return language code in SIP format
     */
    private String getLocale(Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        int languageLen = language.length();
        int countryLen = country.length();

        if (countryLen == 0) {
            // only language code. might not need to allocate another string
            return language.toLowerCase();
        }

        // language and country code need to be slash-separated, lower case
        StringBuilder buf = new StringBuilder(languageLen + countryLen + 1);

        // append language code, lowercase
        for (int i = 0; i < languageLen; i++) {
            char ch = language.charAt(i);
            buf.append(Character.toLowerCase(ch));
        }
        buf.append('-');

        // append country code, lowercase
        for (int i = 0; i < countryLen; i++) {
            char ch = country.charAt(i);
            buf.append(Character.toLowerCase(ch));
        }
        return buf.toString();
    }

    /**
     * converts language code from SIP to Java Locale object 
     * @param localeString language code in SIP format
     * @return language code as Java Locale object
     */
    private Locale getLocale(String localeString)
    {
        Locale locale = null;
        String lang;
        String country= "";
        String variant = ""; 
       
        StringTokenizer tokenizer = null;
        if( localeString.indexOf("_") >=0){ 
        	// localeString is in the Java Locale format 
        	tokenizer = new StringTokenizer(localeString, "_");
        }
        else{
        // localeString is in the SIP Accepted-Language header format
        	tokenizer = new StringTokenizer(localeString, "-");
        }
        
        lang = tokenizer.nextToken();
        
        
        if(tokenizer.hasMoreTokens())
        {
            country = tokenizer.nextToken();
            
            if(tokenizer.hasMoreTokens())
            {
                variant = tokenizer.nextToken();
            }
        }
        locale = new Locale(lang, country, variant);
        return locale;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getAcceptLanguages()
     */
    @SuppressWarnings("unchecked")
	public Iterator<Locale> getAcceptLanguages()
    {
        // Jain define Header iterator, we need to create a new list

        // Get the HeaderIterator from the message
        HeaderIterator iterator = m_message.getAcceptLanguageHeaders();
        
        
        if (null == iterator || !iterator.hasNext()) {
            if (isJSR289Application()) {
            	return EmptyIterator.getInstance();
            } else {
            	// if no accept language headres, return a list with the default locale
            	
            	if (c_defaultLocales == null) {
                    // Generate an unmodifable list with the default locale that will be used
                    // when getAcceptLanguages is called, but there are no Accept_Language headres
                    // in the message
                    List<Locale> lst = new LinkedList<Locale>();
                    lst.add(Locale.getDefault());
                    c_defaultLocales = Collections.unmodifiableList(lst);
            	}
            	
                return c_defaultLocales.iterator();
            }
        }
        

        boolean first = true;
       
        //hold the headers by order
        List<AcceptLanguageHeader> orderList = new LinkedList<AcceptLanguageHeader>();
        while (iterator.hasNext())
        {
            try
            {
                AcceptLanguageHeader h = (AcceptLanguageHeader)iterator.next();
                
                //the first local is also the preffered
                //should be added at the begining 
                if(first){
                	orderList.add(0,h);
                	first=false;
                }else{
                	//should be enter order by the "q" value
                	int i;
                	float q=1.0f; //q is the current "q" value - default is 1.0
                	if (h.hasQValue()){
						q = h.getQValue();
					}
                	
                	//find the place to add
            		for (i=1; i < orderList.size(); i++){            			
            			AcceptLanguageHeader tmpH = orderList.get(i);
            			float tmpQ = 1.0f; //Q is the current locale int the list "q" value            			
						if (tmpH.hasQValue()){
							tmpQ = tmpH.getQValue();
						}
						
						if (q>tmpQ){
            				break;
            			}
            		}
            		orderList.add(i,h);
                }
            }
            catch (HeaderParseException e)
            {
                if (c_logger.isErrorEnabled())
                {
                    Object[] args = { this };
                    c_logger.error(
                        "error.get.accept.languages",
                        Situation.SITUATION_REQUEST,
                        args,
                        e);
                }
            }
            catch (NoSuchElementException e)
            {
                if (c_logger.isErrorEnabled())
                {
                    Object[] args = { this };
                    c_logger.error(
                        "error.get.accept.languages",
                        Situation.SITUATION_REQUEST,
                        args,
                        e);
                }
            }
        }
        
//      Iterate and ad each header
        List<Locale> lst = new LinkedList<Locale>();
        
        Locale locale = null;
        //create the list of
        for (Iterator iter = orderList.iterator(); iter.hasNext();) {
			AcceptLanguageHeader h = (AcceptLanguageHeader)iter.next();
            locale = getLocale(h.getValue());
            lst.add(locale);
			
		}
        return lst.iterator();
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getAddressHeader(String)
     * @pre m_message != null
     */
    public Address getAddressHeader(String name) throws ServletParseException
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { name };
			c_logger.traceEntry(SipServletMessageImpl.class.getName(),
					"getAddressHeader", params);
		}
    	//isCommitted()
		boolean outboundEnable = PropertiesStore.getInstance().getProperties().getBoolean(
				CoreProperties.ENABLE_SET_OUTBOUND_INTERFACE);
    	Address address = null;
        try
        {
            Header header = m_message.getHeader(name, true);
            if(name.equalsIgnoreCase(ContactHeader.name) 
            		&& (header == null)
            		&& (shouldCreateContactIfNotExist())){
            	// Create and set to message new Contact header
            	if (outboundEnable) {
            		header = createAndSetContactHeader(m_message, 
            			constructContactHeaderURI(), true);
            	} else {
            		header = createAndSetContactHeader(m_message, 
                			null, true);
            	}
            }
            
            if(header!= null){            	       		
        		if (header instanceof ContactHeader)
        		{
        			address = getContactHeaderAddress(header);
        			
        			if(address instanceof WildcardNameAddress){
        				_origContact = (Address)address.clone();
        			}else{
        				_origContact = (Address) ((AddressImpl)address).clone(true);
        			}
                }
                else if (header instanceof NameAddressHeader)
                {
                	address = getNameAddress(header,name);
                	
                } else {                	
                	address = getGenericNameAddress(header,name);

                    // and replace the existing header with the new one
                	NameAddressHeader newHeader = ((AddressImpl) address).getNameAddressHeader();
                    m_message.setHeader(newHeader, true);
                }
            } 
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name };
                c_logger.error(
                    "error.get.address.header",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
            
            throw new ServletParseException(e.getMessage());
        }
        if (address != null && isCommitted()) {
        	address = new CommitedAddressImpl(((AddressImpl) address).getNameAddressHeader());
        }

    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(SipServletMessageImpl.class.getName(),
					"getAddressHeader", address);
		}
        return address;
    }

    /**
     * This method allows each class that inherit from this class to decide 
     * whether the contact header should be created if it does not exist (usually outgoing) 
     *  
     * @return
     */
    protected abstract boolean shouldCreateContactIfNotExist();

    /**
	 * Helper method that retrieves relevant to message context 
	 * Address implementation to 
	 * 
	 * @param header the NameAddressHeader
	 * @param name the header name
	 * 
	 * @return Address object
     * @throws SipParseException 
     * @throws IllegalArgumentException 
	 */
    private Address getGenericNameAddress(Header header, String name) 
    		throws IllegalArgumentException, SipParseException {
    	
//    	 Get the existing header, as a string
    	String strAddress = header.getValue();
    	
        // Create a new header with the existing header string
        NameAddressHeaderImpl naHeader = new GenericNameAddressHeaderImpl(name);
        naHeader.setValue(strAddress);
        naHeader.parse();
        return new AddressImpl(naHeader);
	}

	/**
	 * Helper method that retrieves relevant to message context 
	 * Address implementation
	 * 
	 * @param header the NameAddressHeader
	 * @param name the header name
	 * 
	 * @return Address object
	 */
	private Address getNameAddress(Header header, String name) {
		Address address = null;

		if(checkIsSystemHeader(name)){
			if (isJSR289Application() && 
				(ToHeader.name.equalsIgnoreCase(name) || FromHeader.name.equalsIgnoreCase(name))) 
			{
		    	if (!ServletConfigWrapper.getSupportedList().contains(ServletConfigWrapper.PARAM_FROM_CHANGE))
		    		address = new SystemFromToAddressImpl((NameAddressHeader) header);
		    	else
		    		address = new FromToAddressImpl((NameAddressHeader) header);
			} else {
				address = new SystemAddressImpl((NameAddressHeader) header);
			}
    	} else {
			address = new AddressImpl((NameAddressHeader) header);            		
    	}
		
		return address;
	}
	
	/**
	 * This method checks if possible is this message belongs to a JSR 289 application
	 * @return true if this JSR 289 applications
	 */
	public boolean isJSR289Application() {
		//We can assume that if the  TU is null then we didn't get here from an application code.
		//In this case there there is no meaning for the difference between 116 and 289 behaviors  
		if( m_transactionUser == null) return false; 
													
		SipServletDesc desc = m_transactionUser.getSipServletDesc();
		return desc != null && desc.getSipApp().isJSR289Application();
	}

	/**
	 * Helper method that retrieves relevant to message context 
	 * Address implementation
	 * 
	 * @param header
	 * @return Address object
	 */
	private Address getContactHeaderAddress(Header header) {
		Address address = null;

		if(((ContactHeader)header).isWildCard()){
			// wildCard Contact Header
			address = new WildcardNameAddress();
		}else {
			if (SipContainer.getInstance().canModifySysHeaders()) {
				address = new AddressImpl((NameAddressHeader) header);
			} else {
				if(checkIsSystemContactHeader()){
					address = new ContactSystemAddressImpl((NameAddressHeader) header);
				} else {
					address = new AddressImpl((NameAddressHeader) header);					
				}
			}
		}
		
		return address;
	}

	/**
     * @see javax.servlet.sip.SipServletMessage#getParameterableHeader(String)
     */
    public Parameterable getParameterableHeader(String name) throws ServletParseException {
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {name};
			c_logger.traceEntry(SipServletMessageImpl.class.getName(), "getParameterableHeader", params);
		}
    	
    	Parameterable param = null;

    	if(name == null) {
    		throw new NullPointerException("name argument is null");
    	} 
    	
    	if(checkIsSystemAddressHeader(name)) {
    		param = getAddressHeader(name);
    		
    	} else {
	        try {
	            Header header = m_message.getHeader(name, true);
	            if (header != null) {
	            	param = createAndParseParameterableHeader(name, header);
	            }
	        }
	        catch (SipParseException e) {
	            if (c_logger.isErrorEnabled()) {
	                Object[] args = { name};
	                c_logger.error("error.get.address.header",Situation.SITUATION_REQUEST, args, e);
	            }
	            throw new ServletParseException(e.getMessage());
	        }
    	}

    	 if(c_logger.isTraceEntryExitEnabled()){
 			c_logger.traceExit(SipServletMessageImpl.class.getName(), "getParameterableHeader", param);
 		}
        return param;
    }
    
    /**
     * @see SipServletMessageImpl#createAndParseParameterableHeader(String, Header, Boolean)
     */
    private Parameterable createAndParseParameterableHeader(String name, Header header) throws SipParseException{
    	return createAndParseParameterableHeader(name, header, null);
    }
    /**
     * Creating and parsing (if not yet parsed) a parameterable header object from a  given JAIN header. 
     * @param name
     * @param header
     * @return
     * @throws SipParseException
     */
    private Parameterable createAndParseParameterableHeader(String name, Header header, 
    													List<ParametersHeaderImpl> multipleHeadersToReplace) 
    													throws SipParseException{
    	Parameterable param = null;
    	ParametersHeaderImpl newHeader = null;
    	if (header instanceof ParametersHeaderImpl )
        {
    		newHeader = (ParametersHeaderImpl)header;
    		param = new ParameterableImpl(newHeader);
        }
        else
        {
            //We will be trying to parse this as nameAddress header at first, since the parameterable header could be also 
        	//a name address one. If that fails, we will try to parse as parameterable.
        	//A better fix for the long term would not count on catching the exception, but would do initial parsing to verify 
        	//if this is a nameAddress header first. 
        	newHeader = new GenericNameAddressHeaderImpl(name);
        	String strAddress = header.getValue();
        	newHeader.setValue(strAddress);
            try{
            	newHeader.parse();
            	param =  new AddressImpl((GenericNameAddressHeaderImpl)newHeader);
            }
            catch(SipParseException spe){
            	if (c_logger.isTraceDebugEnabled()) {
            		c_logger.traceDebug("createAndParseParameterableHeader: trying to parse " + name +
            				" header as GenericNameAddressHeaderImpl failed, trying to parse as GenericParametersHeaderImpl now....");
            	}
            	newHeader = new GenericParametersHeaderImpl(name);
            	strAddress = header.getValue();
            	newHeader.setValue(strAddress);
            	newHeader.parse();
            	param = new ParameterableImpl(newHeader);
            }
            
            if(multipleHeadersToReplace == null) 
            	// Replace the existing header in the message with the new one, for a single header handling
            	m_message.setHeader(newHeader, true);
        }
    	
    	if(multipleHeadersToReplace != null){ 
    		multipleHeadersToReplace.add(newHeader);
    	}
    	
    	return param;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getAddressHeaders(String)
     */
    public ListIterator<Address> getAddressHeaders(String name)
        throws ServletParseException
    {
        List rList = new LinkedList();
        HeaderIterator iter = m_message.getHeaders(name);
        boolean foundNonAddressHeader = false;
        Header header;
        Address address = null;
        
		boolean outboundEnable = PropertiesStore.getInstance().getProperties().getBoolean(
				CoreProperties.ENABLE_SET_OUTBOUND_INTERFACE);
        try
        {
	        if(name.equalsIgnoreCase(ContactHeader.name) 
	        		&& (iter == null)
            		&& (shouldCreateContactIfNotExist())){
	        	// Create and set to message new Contact header
	        	if (outboundEnable) {
		        	header = createAndSetContactHeader(m_message, constructContactHeaderURI(), true);
	        	} else {
		        	header = createAndSetContactHeader(m_message, null, true);
	        	}
	        	
	        	if(header != null){            		
	        		iter = m_message.getHeaders(name);
	        	}
	        }
	        	
	        while (iter != null && iter.hasNext())
	        {
                header = iter.next();
                if(header!= null){
                	
                	if (header instanceof ContactHeader) {
                		address = getContactHeaderAddress(header);
                		
                    } else if (header instanceof NameAddressHeader) {
                    	address = getNameAddress(header,name);
                    	
                    } else {
                    	address = getGenericNameAddress(header,name);
		                foundNonAddressHeader = true;
	                }
                	
//                	 add to list
	                rList.add(address);
                }
            }
        }
        catch (SipParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name };
                c_logger.error(
                    "error.get.address.headers",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
            
            throw new ServletParseException(e.getMessage());
        }
        catch (NoSuchElementException e)
        {
            // We should never get here as we test for hasNext()
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name };
                c_logger.error(
                    "error.get.address.headers",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
            
            throw new ServletParseException(e.getMessage());
        }

        // If we got here, all headers can be translated
    	// If we had to translate, removed the existing headers 
        // in JAIN and replace with new ones
        if (foundNonAddressHeader)
        {
	    	m_message.removeHeaders(name);
	    	
	    	// And add the new ones
	    	AddressImpl a;
	    	ListIterator lIter = rList.listIterator();
	    	while ((lIter != null) && lIter.hasNext())
	    	{
	    		a = (AddressImpl)lIter.next();
	    		m_message.addHeader(a.getNameAddressHeader(), false);
	    	}
        }
        
        //will guarantee that the external iterator will no break when another thread
        //modifies the original list.
        ArrayList externalList = new ArrayList(rList);
        List unModify = Collections.unmodifiableList(externalList);
    	return unModify.listIterator();
    }
    
    /**
     * @see javax.servlet.sip.SipServletMessage#getParameterableHeaders(String)
     */
    public ListIterator<Parameterable> getParameterableHeaders(String name)
    	throws ServletParseException{
    	
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { name };
			c_logger.traceEntry(SipServletMessageImpl.class.getName(),
					"getParameterableHeaders", params);
		}
    	
    	ListIterator listIter = null;    	
    	if(checkIsSystemAddressHeader(name))
    	{
    		listIter = getAddressHeaders(name);
    		
    	} else {
    		ArrayList<Parameterable> externalList = new ArrayList<Parameterable>();
    		List<ParametersHeaderImpl> rList = new LinkedList<ParametersHeaderImpl>();
	        HeaderIterator iter = m_message.getHeaders(name);
	        
	        //indicates whether at least one of the headers was stored unparsed
			//since it was never previously fetched. In this case we will 
			//have to create a specific type of JAIN object, and replace the general one.
	        boolean hadToCreateAndParseNewHeader = false;
	        
	        Header header;
	        Parameterable param;
	        while (iter != null && iter.hasNext())
	        {
	            try
	            {
	                header = iter.next();
	                param =	createAndParseParameterableHeader(name, header, rList);
	                hadToCreateAndParseNewHeader = 
	                	hadToCreateAndParseNewHeader ||
	                	(rList.get(rList.size()-1) != header);// a different header instance was appended to the list
	                										  // Enough that only one will be new, to replace all the 
	                										  //headers of same name in the message.
	                externalList.add(param);
	            }
	            catch (SipParseException e)
	            {
	                if (c_logger.isErrorEnabled())
	                {
	                    Object[] args = { name };
	                    c_logger.error(
	                        "error.get.address.headers",
	                        Situation.SITUATION_REQUEST,
	                        args,
	                        e);
	                }
	                
	                throw new ServletParseException(e.getMessage());
	            }
	            catch (NoSuchElementException e)
	            {
	                // We should never get here as we test for hasNext()
	                if (c_logger.isErrorEnabled())
	                {
	                    Object[] args = { name };
	                    c_logger.error(
	                        "error.get.address.headers",
	                        Situation.SITUATION_REQUEST,
	                        args,
	                        e);
	                }
	                
	                throw new ServletParseException(e.getMessage());
	            }
	        }
	
	        // If we got here, all headers can be translated
	        if (hadToCreateAndParseNewHeader){
	        	// If we had to translate, must remove the existing headers 
		        // in JAIN and replace with new ones
		    	m_message.removeHeaders(name);
		    	// And add the new ones
		    	ListIterator<ParametersHeaderImpl> lIter = rList.listIterator();
		    	while ((lIter != null) && lIter.hasNext())
		    	{
		    		m_message.addHeader(lIter.next(), false);
		    	}
	        }  
	        
	        //will guarantee that the external iterator will not break when another thread
	        //modifies the original list.
	        List<Parameterable> unModify = Collections.unmodifiableList(externalList);
	        listIter = unModify.listIterator();
    	}

    	return listIter;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getApplicationSession()
     */
    public SipApplicationSession getApplicationSession()
    {
        return getApplicationSession(true);
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getApplicationSession(boolean)
     */
    public SipApplicationSession getApplicationSession(boolean create)
    {
    	if (!isLiveMessage("getApplicationSession"))
    		return null;
    	if (m_transactionUser == null)
    		return null;
    	return m_transactionUser.getApplicationSession(create);
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getAttribute(String)
     */
    public Object getAttribute(String name)
    {
        Object obj = null;
        if(m_attributes != null)
        {
            obj = m_attributes.get(name);
        }
        if ((obj == null) &&(m_httpServletRequest!=null)){
            try {
				obj=m_httpServletRequest.getAttribute(name);
			} catch (NullPointerException e) {
				//The webcontainer is throwing NPE when the request getAttribute is invoked after the httpRequest is not valid, 
				//(when it is accessed after the request thread was finished). there is nothing in the sip container jsr  that 
				//allow us to thrown NPE in such a case so we have no choice but to catch the webcontainer NPE and ignore it
				
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getAttribute", "Internal HttpRequest is not valid, can not get attributes from it, httprequest: " + m_httpServletRequest);
				}
			}
        }
        return obj;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getAttributeNames()
     */
    public Enumeration getAttributeNames()
    {
        if(null != m_attributes)
        {
            return m_attributes.keys();
        }
        else
        {
            return EmptyEnumeration.getInstance();
        }
    }
    
    /**
     * Method that verify the current state of sipSession and if it is 
     * invalid/inactive - exception will be throw
     * @throws IllegalStateException
     */
    public void isSessionIsValid() throws IllegalStateException
    {
//    	Remove
//        SipSessionImplementation session = getSessionForInrernalUse(); 
//        if(session != null)
//        {
//            //The IllegalStateException will be throw 
//            // if current session is invalid / inActive
//            session.isSessionActive();
//        }
//    	End
    	
    	if(m_transactionUser != null){
    		m_transactionUser.ensureTUActive();
    	}
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#setAttribute(String, Object)
     */
    public synchronized void setAttribute(String name, Object o)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
        if(m_attributes == null)
        {
            m_attributes = new Hashtable();
        }
        m_attributes.put(name, o);
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getCallId()
     * @pre m_message != null
     */
    public String getCallId()
    {
        String rValue = null;
        CallIdHeader header = m_message.getCallIdHeader();
        if (null != header)
        {
            rValue = header.getCallId();
        }
        return rValue;
    }

    /**
     * Return the Jain Sip Call Id Header. convenient for internal use
     * @pre m_message != null
     */
    public CallIdHeader getCallIdHeader()
    {
        return m_message.getCallIdHeader();
    }

    /**
     * checks the given character-encoding string, and throws an exception
     * if the encoding is not supported, or if the encoding string is illegal.
     * @param charset the character set to validate
     * @throws UnsupportedEncodingException if not supported or if not a
     *  character set
     */
    private static void assertCharacterEncoding(String charset)
    	throws UnsupportedEncodingException
    {
    	boolean supported;
    	try {
    		supported = Charset.isSupported(charset);
    	}
    	catch (Exception e) {
    		UnsupportedEncodingException unsupportedEncodingException =
    			new UnsupportedEncodingException(charset);
    		unsupportedEncodingException.initCause(e);
    		throw unsupportedEncodingException;
    	}
    	if (!supported) {
    		throw new UnsupportedEncodingException(charset);
    	}
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#setCharacterEncoding(String)
     */
    public void setCharacterEncoding(String enc)
    	throws UnsupportedEncodingException
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }

    	assertCharacterEncoding(enc);

        try
        {
        	//// set the encoding
            ContentTypeHeader cType =
                (ContentTypeHeader) findHeader(ContentTypeHeader.name);
            if (null == cType)
            {
                //No content type header set so create a default one for plain text 
                setHeader(ContentTypeHeader.name, "text/plain;charset=" + enc);
            }
            else
            {
                cType.setParameter("charset", enc);
            }
        }
        catch (Exception e) {
        	UnsupportedEncodingException unsupportedEncodingException =
        		new UnsupportedEncodingException();
        	unsupportedEncodingException.initCause(e);
        	throw unsupportedEncodingException;
        }
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getCharacterEncoding()
     */
    public String getCharacterEncoding()
    {
        String rValue = null;
        ContentTypeHeader cType;
        try
        {
            cType = (ContentTypeHeader) findHeader(ContentTypeHeader.name);
            if (null != cType)
            {
                rValue = cType.getParameter("charset");
            }

        }
        catch (HeaderParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                c_logger.error(
                    "error.get.character.encoding",
                    Situation.SITUATION_CREATE,
                    null,
                    e);
            }
        }

        return rValue;

    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getContent()
     * @pre m_message != null
     */
    public Object getContent() throws IOException, UnsupportedEncodingException
    {
        byte[] body = m_message.getBodyAsBytes();
        Object rValue = body;

        if (null != rValue && getContentType().startsWith(TEXT_MIME_TYPE))
        {
            String encoding = getCharacterEncoding();
            if (null == encoding || encoding.length() == 0)
            {
                encoding = DEFAULT_CHARSET_ENCODING;
            }

            rValue = new String(body, encoding);
        }

        return rValue;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getContentLanguage()
     */
    public Locale getContentLanguage()
    {
        Locale locale = null;
        String sLocale = getHeader(CONTENT_LANGUAGE);
        if (null != sLocale && sLocale.length() > 0 )
        {
            locale = getLocale(sLocale);
        }

        return locale;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getContentLength()
     * @pre m_message != null
     */
    public int getContentLength()
    {
        int rValue = -1;
        try
        {
        	ContentLengthHeader h = m_message.getContentLengthHeader();
        	if (h != null)
        		rValue = h.getContentLength();
        	else
        		rValue = 0;
        }
        catch (HeaderParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { this };
                c_logger.error(
                    "error.get.content.length",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }

        return rValue;
    }

    /**
     * Removed from javax - hide 289 API
     */
    Object getContent(Class[] classes)
        throws IOException, UnsupportedEncodingException{
    	throw new UnsupportedOperationException(
		"getContent: Not Done Yet");
    }
    
    /**
     * @see javax.servlet.sip.SipServletMessage#getContentType()
     * @pre m_message != null
     */
    public String getContentType()
    {
        String contentType = null;
        
        // If the message is null, we should return null (based on the JSR)
        if (m_message != null && m_message.getBodyAsBytes() != null)
        {
	        try
	        {
	            ContentTypeHeader header = m_message.getContentTypeHeader();
	            if (header != null)
	            {
	                contentType = header.getValue();
	            }
	        }
	        catch (HeaderParseException e)
	        {
	            if (c_logger.isErrorEnabled())
	            {
	                Object[] args = { this };
	                c_logger.error(
	                    "error.get.content.type",
	                    Situation.SITUATION_REQUEST,
	                    args,
	                    e);
	            }
	        }
        }
        return contentType;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getExpires()
     * @pre m_message != null
     */
    public int getExpires()
    {
        int rc = -1;
        ExpiresHeader eHeader;
        try
        {
            eHeader = m_message.getExpiresHeader();
            if (eHeader != null)
            {
                rc = Integer.parseInt(eHeader.getValue());
            }
        }
        catch (HeaderParseException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { this };
                c_logger.error(
                    "error.get.expires",
                    Situation.SITUATION_REQUEST,
                    args);
            }
        }
        return rc;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getFrom()
     * @pre m_message != null
     */
    public Address getFrom(){
    	Address address = m_fromAddress;
    	
    	// Assumption is that nobody can change the From header
    	if (null == address){
	        try {
	        	address = getAddressHeader("From");
	        	//the from address will be saved only if we have the transaction user attached to this message
	        	//this is because we need to know if this is a jsr116 to jsr289 application to decide if this 
	        	//should be a system header or not. when the application is accessing this header the tu should be set
	        	//it can be null only if accessed before the application is called in the container code, in this case
	        	//there is no real meaning if it system header or not
	        	if (m_transactionUser != null){
	        		m_fromAddress = address;
	        	}else{
	        		if (c_logger.isTraceDebugEnabled()) {
	            		c_logger.traceDebug(this, "getFrom", "TU is not set on the request, From header is not saved");
	        		}
	        	}
			} catch (ServletParseException e) {
				log("getFrom", "Unable to get Form header", e);
			}
	    }
    	
        return address;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getHeader(String)
     * @pre m_message != null
     */
    public String getHeader(String name)
    {
        String rValue = null;
        Header h;
        try
        {
            h = m_message.getHeader(name, true);
            if (null != h)
            {
                rValue = h.getValue();
            }
        }
        catch (HeaderParseException e)
        {
        	rValue = e.getHeader().getValue();
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getHeader", "Failed to parse header = " + name + " return as a string.");
			}
        }
        catch (IllegalArgumentException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name };
                c_logger.error(
                    "error.get.header",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }

        return rValue;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getHeaderNames()
     * @pre m_message != null
     */
    public Iterator getHeaderNames()
    {
        Iterator rValue = null;
        HeaderIterator headerIterator = ((MessageImpl)m_message).getHeadersUnparsed();
        if (null != headerIterator)
        {
            // we need to return the keys name i.e. no duplicate value
            // so, we'll use a map to override duplicated keys
            HashMap map = new HashMap(20);        
            while (headerIterator.hasNext())
            {
                try
                {
                    map.put(headerIterator.next().getName(), "");
                }
                catch (HeaderParseException e)
                {
                    if (c_logger.isErrorEnabled())
                    {
                        Object[] args = { this };
                        c_logger.error(
                            "error.get.header.names",
                            Situation.SITUATION_REQUEST,
                            args,
                            e);
                    }

                }
                catch (NoSuchElementException e)
                {
                    if (c_logger.isErrorEnabled())
                    {
                        Object[] args = { this };
                        c_logger.error(
                            "error.get.header.names",
                            Situation.SITUATION_REQUEST,
                            args,
                            e);
                    }
                }
            }
            
            // get an iterator from the map
            rValue = map.keySet().iterator();
        }

        return rValue;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getHeaders(String)
     * @pre m_message != null
     */
    public ListIterator getHeaders(String name)
    {
        List list = new LinkedList();
        HeaderIterator headerIterator = ((MessageImpl)m_message).getHeadersUnparsed(name);
        Header h;
        if (null != headerIterator)
        {
            while (headerIterator.hasNext())
            {
                try
                {
                    h = headerIterator.next();
                    list.add(h.getValue());
                }
                catch (HeaderParseException e)
                {
                    if (c_logger.isErrorEnabled())
                    {
                        Object[] args = { name };
                        c_logger.error(
                            "error.get.headers",
                            Situation.SITUATION_REQUEST,
                            args,
                            e);
                    }
                }
                catch (NoSuchElementException e)
                {
                    // This should never happen
                    if (c_logger.isErrorEnabled())
                    {
                        Object[] args = { name };
                        c_logger.error(
                            "error.get.headers",
                            Situation.SITUATION_REQUEST,
                            args,
                            e);
                    }
                }
            }
        }

    	List unModify = Collections.unmodifiableList(list);
    	return unModify.listIterator();
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getLocalAddr()
     */
    public String getLocalAddr()
    {
        return null; 
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getLocalPort()
     */
    public int getLocalPort()
    {
        return -1; 
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getProtocol()
     */
    public String getProtocol()
    {
        return "SIP/2.0";
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getRawContent()
     * @pre m_message != null
     */
    public byte[] getRawContent() throws IOException
    {
        byte[] content = null;
        if (null != m_message)
        {
            content = m_message.getBodyAsBytes();
        }

        return content;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getRemoteAddr()
     */
    public String getRemoteAddr() { 
    	return null;
    }
    /**
     * @see javax.servlet.sip.SipServletMessage#getRemotePort()
     */
    public int getRemotePort() {
    	return -1;
    }
    
    /**
     * @see javax.servlet.sip.SipServletMessage#getInitialRemoteAddr()
     */
    public String getInitialRemoteAddr() { 
    	return null;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getInitialRemotePort()
     */
    public int getInitialRemotePort() {
    	return -1;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getRemoteUser()
     */
    public String getRemoteUser()
    {
        if (null != m_httpServletRequest)
        {
        	if (isUnauthenticatedUser()){
        		return null;
        	}
        	
            return m_httpServletRequest.getRemoteUser();
        }

        return null;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getSession()
     */
    public SipSession getSession()
    {
    	if(getTransactionUser().isValid()){
    		return getSession(true);
    	}else{
    		return getSession(false);
    	}
    }
    
    /**
     * Returns the TransactionUser associated with this SipServletMessage
     */
    public TransactionUserWrapper getTransactionUser()
    {
    	if (!isLiveMessage("getSessionForInrernalUse"))
    		return null;
        
        return m_transactionUser;
    }
    
    /**
     * @see javax.servlet.sip.SipServletMessage#getSession(boolean)
     */
    public SipSession getSession(boolean create)
    {
    	if (!isLiveMessage("getSession"))
    		return null;
    	if(create && !getTransactionUser().isValid()){
    		throw new IllegalStateException("The session of this message was invalidated. " +
    				"Calling getSession(boolean) with create argument of the value ture, is not allowed.");
    	}
    	if(m_transactionUser.isProxying()){
    		return getProxySession(create);
    	}
    	
    	return m_transactionUser.getSipSession(create);
    }

    
    /**
     * This method will be overrided by IncomingSipSessionRequest and 
     * OutgoingSipSessionResponse.
     * @param create
     * @return
     */
    SipSession getProxySession(boolean create){
//    	This method used when TU in the Proxy mode and in this care it can has
//    	one or more Sip Session. 
    	
    	if (c_logger.isTraceDebugEnabled()) {
    		StringBuffer buff = new StringBuffer();
    		buff.append(" WRONG STATE !! ");
    		buff.append("This method can be called only from IncomingSipServletRequest ");
    		buff.append("or from IncomingSipServletResponse. Transaction Id = ");
    		buff.append(m_transactionUser.getId());
    		c_logger.traceDebug(this, "getDerivedSession", buff.toString());
		}
    	return null;
    }
    
    /**
     * @see javax.servlet.sip.SipServletMessage#getTo()
     * @pre m_message != null
     */
    public Address getTo() {
    	Address address = m_toAddress;
    	// Assumption is that nobody can change the To header
    	if (null == address) {
	        try {
	        	address = getAddressHeader("To");
	        	
	        	//the from address will be saved only if we have the transaction user attached to this message
	        	//this is because we need to know if this is a jsr116 to jsr289 application to decide if this 
	        	//should be a system header or not. when the application is accessing this header the tu should be set
	        	//it can be null only if accessed before the application is called in the container code, in this case
	        	//there is no real meaning if it system header or not
	        	if (m_transactionUser != null){
	        		m_toAddress = address;
	        	}else{
	        		if (c_logger.isTraceDebugEnabled()) {
	            		c_logger.traceDebug(this, "getTo", "TU is not set on the request, To header is not saved");
	        		}
	        	}
			} catch (ServletParseException e) {
				log("getTo", "Unable to get To header", e);
			}
	    }
    	
        return address;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getTransport()
     */
    public String getTransport()
    {
        return null; //TODO check if we need to return "udp" here for AsyncWorkTask
    }

    
    /**
     * @see javax.servlet.sip.SipServletMessage#getInitialTransport()
     */
    public String getInitialTransport()
    {
        return null;
    }
    

    /**
     * @see javax.servlet.sip.SipServletMessage#getUserPrincipal()
     */
    public Principal getUserPrincipal()
    {
        if (null != m_httpServletRequest)
        {
        	if (isUnauthenticatedUser()){
        		return null;
        	}
        	
            return m_httpServletRequest.getUserPrincipal();
        }

        return null;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#isCommitted()
     */
    public boolean isCommitted()
    {
        return m_isCommited;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#isSecure()
     */
    public boolean isSecure()
    {
        return false;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#isUserInRole(String)
     */
    public boolean isUserInRole(String role)
    {
        if (null != m_httpServletRequest)
        {
        	if (isUnauthenticatedUser()){
        		return false;
        	}
        	
            return m_httpServletRequest.isUserInRole(role);
        }
        return false;
    }

    //
    //Set & Add Operations provide default implementation for sub classes. 
    //Sub class that need to disalble or provide different behavior will have to 
    //to overide these methods. 
    //

    /**
     * @see javax.servlet.sip.SipServletMessage#addAcceptLanguage(Locale)
     */
    public void addAcceptLanguage(Locale locale)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
        addHeader(AcceptLanguageHeader.name, getLocale(locale));
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#addAddressHeader(String, Address, boolean)
     */
    public void addAddressHeader(String name, Address addr, boolean first)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	handleAddressHeader(name, addr, false, first);
    }
    
    /**
     * Removed from javax - hide 289 API
     * @throws SipParseException 
     * @see javax.servlet.sip.SipServletMessage#addParameterableHeader(String, Parameterable, boolean)
     */
    public void addParameterableHeader(String name, Parameterable param, boolean first) 
    	throws IllegalArgumentException
    {
    	if(isCommitted())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	handleParameterableHeader(name, param, false, first);
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
    	
        //Check if it a no system header. 
        checkIsLegalHeader(name);

        addHeader(name, value, false);
    }

    /**
     * Adds a header with the given name and value. 
     * This mathod allows it users to ignore the legal check
     * This method is not part of JSR 116 and intend for internal use only
     * 
     * @param ignoreLegalCheck if true - allow systen header to be added
     * @param name header name
     * @param value header value
     */
    public void addHeader(boolean ignoreLegalCheck, String name, String value)
    {
        if (!ignoreLegalCheck)
        {
            // Check if it a no system header. 
            checkIsLegalHeader(name);
        }

        addHeader(name, value, false);
    }

    /**
     * Helper Function - Adds a Header to the response at the specified position
     * Function does not check whether the header is a system header, that
     * is the respoinsiblity of the calling function. 
     */
    public void addHeader(String name, String value, boolean first)
    {
    	if(name == null || name.equals("") || value == null){
    		throw new NullPointerException("The name or value is null");
    	}
    	
        try
        {
            Header header = getHeadersFactory().createHeader(name, value);
            m_message.addHeader(header, first);
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
     * @see javax.servlet.sip.SipServletMessage#setAcceptLanguage(Locale)
     */
    public void setAcceptLanguage(Locale locale)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
        String sipLanguage = getLocale(locale);
        setHeader(AcceptLanguageHeader.name, sipLanguage);
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#setAddressHeader(String, Address)
     */
    public void setAddressHeader(String name, Address addr)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	handleAddressHeader(name, addr, true, true);
    }
    
    /**
     * Removed from javax - hide 289 API
     * @see javax.servlet.sip.SipServletMessage#setParameterableHeader(String, Parameterable)
     */
    public void setParameterableHeader(String name, Parameterable param)
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { name, param };
			c_logger.traceEntry(SipServletMessageImpl.class.getName(),
					"setParameterableHeader", params);
		}
    	
    	if(isCommitted())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
    	handleParameterableHeader(name, param, true, true);
    }
    
    /*
     * helper function to set/add Address headers
     */
    protected void handleAddressHeader(String name, Address addr, boolean remove, boolean first)
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { name, addr, remove, first };
			c_logger.traceEntry(SipServletMessageImpl.class.getName(),
					"handleAddressHeader", params);
		}
    	
        //Check if it a no system header. 
        checkIsLegalHeader(name);

        try
        {
            HeaderImpl header = HeaderCreator.createHeader(name);
            if (header instanceof NameAddressHeaderImpl)
            {
            	NameAddressHeaderImpl h = (NameAddressHeaderImpl)header;
            	if (addr.isWildcard())
            	{
            		if (h instanceof ContactHeader)
            		{
            			((ContactHeader)h).setWildCard();
            		}
            		else
            		{
            			throw new IllegalArgumentException("Wildcard Address " +
            				"objects are legal only in Contact header fields");
            		}
            	}
            	else
            	{
            		h.assign(((AddressImpl)addr).getNameAddressHeader());
            	}
            }
            else
            {
            	header = new GenericNameAddressHeaderImpl (name);
            	((NameAddressHeaderImpl)header).
					assign(((AddressImpl)addr).getNameAddressHeader()); 
            }

            // JSR 116 : Sets a header with the given name and value. If the header
            // had already been set, the new value overwrites the previous one.
            if (remove)
            	removeHeader(name,false);
            
            m_message.addHeader(header, first);
            
            //if this header is a contact header, we will set the flag to false
            //because the contact is not created from a default sip provider 
            if (header instanceof ContactHeader){
            	m_isDefaultContactHeader = false;
            }
        }
        catch (Exception e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name, addr };
                c_logger.error(
                    "error.set.header",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
            
            throw new IllegalArgumentException("The specified header field is a " +
            	"system header or it cannot legally appear in this message");
        }
    }
    
    /*
     * helper function to set/add Parameterable headers
     */
    private void handleParameterableHeader(String headerName, Parameterable param, 
    						boolean remove, boolean first) throws IllegalArgumentException
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { headerName, param, remove, first };
			c_logger.traceEntry(SipServletMessageImpl.class.getName(),
					"handleParameterableHeader", params);
		}

    	// validate input 
        checkIsLegalHeader(headerName);
    	if (param == null) {
    		throw new IllegalArgumentException("null parameterable");
    	}
        if (!(param instanceof ParameterableImpl)) {
        	throw new IllegalArgumentException("expected ["
        		+ ParameterableImpl.class.getName()
        		+ "] but got [" + param.getClass().getName() + ']');
        }
        ParameterableImpl parameterable = (ParameterableImpl)param;

        // create header, and assign value and parameters
        HeaderImpl header = HeaderCreator.createHeader(headerName);
        if (header instanceof NameAddressHeaderImpl) {
        	// setting a "name-addr" header in the message
        	NameAddressHeaderImpl nameAddressHeader = (NameAddressHeaderImpl)header;
            ParametersHeader source = parameterable.getParametersHeader();
            NameAddressHeader nameAddressSource;
            if (source instanceof NameAddressHeader) {
            	nameAddressSource = (NameAddressHeader)source;
            	nameAddressHeader.assign(nameAddressSource);
            }
            else {
        		// trying to set a "name-addr" header with a non-"name-addr"
        		// header value. try to convert the value to name-addr.
        		try {
					header.setValue(source.getValue());
					header.parse();
				}
        		catch (SipParseException e) {
                	// standard "name-addr" header, which is not a name-addr value.
                	// technically it's possible to handle this same as with
                	// ExtendedHeader below, but it's too risky to add a well-known header
                	// (such as Route) with the wrong class (GenericParametersHeaderImpl
                	// instead of RouteHeader).
                	throw new IllegalArgumentException("address header [" + headerName
                		+ "] cannot have non-address value [" + param.toString() + ']', e);
				}
            }
        }
        else if (header instanceof ParametersHeaderImpl) {
        	// setting a parameters header which is not a "name-addr" header
            ParametersHeaderImpl source =
            	(ParametersHeaderImpl)parameterable.getParametersHeader();
            String sourceValue = source.getValue();
            try {
				header.setValue(sourceValue);
	            header.parse();
			}
            catch (SipParseException e) {
            	throw new IllegalArgumentException("parameters header [" + headerName
               		+ "] cannot have non-parameters value [" + param.toString() + ']', e);
			}
        }
        else if (header instanceof ExtendedHeader) {
        	// proprietary header. there is no risk that the container will
        	// try to cast it to a well-known header, because the container does
        	// not attempt to cast anything to ExtendedHeader.
            ParametersHeaderImpl source =
            	(ParametersHeaderImpl)parameterable.getParametersHeader();
            String sourceFieldValue = parameterable.getValue();
        	GenericParametersHeaderImpl genericParametersHeaderImpl = new GenericParametersHeaderImpl(headerName);
        	genericParametersHeaderImpl.setFieldValue(sourceFieldValue);
        	header = genericParametersHeaderImpl;
        	genericParametersHeaderImpl.assign(source);
        }
        else {
        	// standard header, which is not a parameters header. technically it's
        	// possible to handle this same as in the ExtendedHeader above, but
        	// it's too risky to add a well-known header (such as Route) with the
        	// wrong class (GenericParametersHeaderImpl instead of RouteHeader).
        	throw new IllegalArgumentException("not a parameterable header name ["
        		+ headerName + "] value [" + param.toString() + ']');
        }

        try
        {
            // JSR 289 : Sets a header with the given name and value. If the header
            // had already been set, the new value overwrites the previous one.
            if (remove)
            	removeHeader(headerName,false);
            
            m_message.addHeader(header, first);
        }
        catch (IllegalArgumentException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { headerName, param };
                c_logger.error(
                    "error.set.header",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }


    /**
     * Removed from javax - hide 289 API
     */
    public void removeAttribute(String name){
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
    	if( m_attributes != null){
    		m_attributes.remove(name);
    	}
    }

    
    /**
     * @see javax.servlet.sip.SipServletMessage#setContent(Object, String)
     */
    public void setContent(Object content, String contentType)
        throws UnsupportedEncodingException
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
    	if (null == content)
    	{
    		throw new IllegalArgumentException("setContent: content can't be null");
    	}
    	
    	if (null == contentType)
    	{
    		throw new IllegalArgumentException("setContent: contentType can't be null");
    	}
    	
        try
        {
        	ContentTypeHeader newContentTypeHeader = 
        		(ContentTypeHeader)getHeadersFactory().createHeader(ContentTypeHeader.name, contentType);
        	String encoding = newContentTypeHeader.getParameter("charset");
        	
        	if (null==encoding)
        	{
	        	ContentTypeHeader contentTypeHeader =
	                (ContentTypeHeader) findHeader(ContentTypeHeader.name);
	            if (null != contentTypeHeader)
	            {
	            	encoding = contentTypeHeader.getParameter("charset");
	            	if (null != encoding)
	            		newContentTypeHeader.setParameter("charset", encoding);
	            }
        	}
        	if (encoding != null) {
        		assertCharacterEncoding(encoding);
        	}

            m_message.setBody(
                getBodyAsBytes(content, newContentTypeHeader),
                newContentTypeHeader);
        }
        catch (SipParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#setContentLanguage(Locale)
     */
    public void setContentLanguage(Locale locale)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
        String sipLanguage = getLocale(locale);
        setHeader(CONTENT_LANGUAGE, sipLanguage);
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#setContentLength(int)
     */
    public void setContentLength(int len)
    {
    	if(isCommitted())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "setContentLength",
                "use SetContent instead");
        }
    	HeaderFactory hf = getHeadersFactory();
    	ContentLengthHeader contentLength;

    	try {
			contentLength = hf.createContentLengthHeader(len);
		}
    	catch (SipParseException e) {
    		throw new RuntimeException(e);
		}
		m_message.setContentLengthHeader(contentLength);
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#setContentType(String)
     */
    public void setContentType(String type)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
        setHeader(ContentTypeHeader.name, type);
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#setExpires(int)
     */
    public void setExpires(int seconds)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
    	
        setHeader(ExpiresHeader.name, String.valueOf(seconds));
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#setHeader(String, String)
     */
    public void setHeader(String name, String value)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
       
    	setHeader(name,value,true);
    }

    /**
     * Halper function that allows to set header without check if it is 
     * system header or not
     * @param name
     * @param value
     * @param checkIsLegalHeader
     */
    protected void setHeader(
        String name,
        String value,
        boolean checkIsLegalHeader)
    {
    	if(name == null || name.equals("") || value == null ){
    		throw new NullPointerException("The name or value is null");
    	}
    	
        if (checkIsLegalHeader)
        {
            //Check if it a no system header. 
            checkIsLegalHeader(name);
        }

        try
        {
            // JSR 116 : Sets a header with the given name and value. If the header
            // had already been set, the new value overwrites the previous one.
            removeHeader(name,false);
            
            Header header = getHeadersFactory().createHeader(name, value);
            m_message.setHeader(header, false);
            
            //nullify the To/From cached value in case that they were changed
            //they will get cached the next time that get is called
            if (FromHeader.name.equalsIgnoreCase(name)){
            	m_fromAddress = null;
            }else if (ToHeader.name.equalsIgnoreCase(name)){
            	m_toAddress = null;
            }
        }
        catch (IllegalArgumentException e)
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { name, value };
                c_logger.error(
                    "error.set.header",
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
                    "error.set.header",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
        }
    }

    /**
     * Checks whether it is legal to set the specified header field for the 
     * message. if fails the function throws.
     *<p><b>Note:</b> applications should never attempt to set the
     * From, To, Call-ID, CSeq, Via, Record-Route, and Route headers.
     * Also, setting of the Contact header is subject to the constraints
     * mentioned in the <a href="#syshdr">introduction</a>.
     * @param name
     */
    protected void checkIsLegalHeader(String name)
    {
        if(checkIsSystemHeader(name)){
        	throw new IllegalArgumentException("Illegal operation, trying to modify system header");
        }
        
        if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(SipServletMessageImpl.class.getName(), "checkIsLegalHeader", "header name: " + name + " true");
		}
    }
    
    /**
     * Checks whether it is a System Header 
     *<p><b>Note:</b> System headers are :
     * From, To, Call-ID, CSeq, Via, Record-Route, and Route headers.
     * Also, the Contact header in some cases
     * @param name The header name 
     */
    protected boolean checkIsSystemHeader(String name)
    {
        boolean isSystemHeader = false;
        
    	if(!SipContainer.getInstance().canModifySysHeaders()){
    		boolean isJSR289 = isJSR289Application();
    		if(name.equalsIgnoreCase(ContactHeader.name))
    		{
    			if(checkIsSystemContactHeader()){
    				isSystemHeader = true;
    			}
    		}   
    		else if((name.equalsIgnoreCase(ViaHeader.name)
    	            || name.equalsIgnoreCase(RecordRouteHeader.name)
    	            || name.equalsIgnoreCase(RouteHeader.name)
    	            || name.equalsIgnoreCase(CSeqHeader.name)
    	            || name.equalsIgnoreCase(ToHeader.name)
    	            || name.equalsIgnoreCase(FromHeader.name)
    	            || name.equalsIgnoreCase(CallIdHeader.name)))
    	    {
    	       	isSystemHeader = true;
    	    } else if(isJSR289){
    	    	// in JSR 289 "RAck" and "RSeq" headers are System header
    	    	if(name.equalsIgnoreCase(RAckHeader.name)
    	    		|| name.equalsIgnoreCase(RSeqHeader.name)){
    	    		isSystemHeader = true;
    	    	}
    	    }
    	}

    	if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(SipServletMessageImpl.class.getName(), 
			"checkIsSystemHeader","header name: " + name + " "+ isSystemHeader );
		}
        return isSystemHeader;   
    }
    
    /**
     * Verify whether this is a system address header
     * @param name
     * @return
     */
    private boolean checkIsSystemAddressHeader(String name){
    	if(name.equalsIgnoreCase(ContactHeader.name))
		{
			if(checkIsSystemContactHeader()){
				return true;
			}
		}   
		else if( name.equalsIgnoreCase(RecordRouteHeader.name)
	            || name.equalsIgnoreCase(RouteHeader.name)
	            || name.equalsIgnoreCase(ToHeader.name)
	            || name.equalsIgnoreCase(FromHeader.name))
	    {
	       	return true;
	    }
    	return false;
    }
    /**
     * Checks whether it is a System Contact Header according to
     * Message context.
     */
    abstract protected boolean checkIsSystemContactHeader();

    /**
     * @see javax.servlet.sip.SipServletMessage#removeHeader(java.lang.String)
     */
    public void removeHeader(String name)
    {
    	if(isCommitted() && isJSR289Application())
        {
            throw new IllegalStateException(
               "Can not modify committed message");
        }
        removeHeader(name, true);
    }
    
    /**
     * Remove the specified header(s) with an option to bypass the container's
     * check for a legal operation.
     * This method is not part of JSR 116 and intend for internal use only 
     */
    public void removeHeader(String name, boolean isLegal)
    {
        if(isLegal)
        {
            checkIsLegalHeader(name);
        }
        m_message.removeHeaders(name);
    }

    //
    //Internal Utility Functions
    //
    //    
    /**
     * Returns the Sip Message object associated with this object. 
     * @return Message
     */
    public Message getMessage()
    {
        return m_message;
    }

    /**
     * Sets the Sip Message object associated with this object.
     * @pre message != null 
     */
    protected void setMessage(Message message)
    {
        m_message = message;
    }

    /**
     * Returns the Jain Sip Provider assciated with this message. 
     * @return SipProvider
     */
    public SipProvider getSipProvider()
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getSipProvider", "provider=" + m_sipProvider);
		}
        return m_sipProvider;
    }

	/**
     * Returns the Jain Sip Transaction Id associated with this message. 
     * @return long
     */
    public long getTransactionId()
    {
        return m_transactionId;
    }

    /**
     * Returns the Jain Sip Message Factory associated with this message. 
     */
    protected MessageFactory getMessageFactory()
    {
        return getStackProperties().getMessageFactory();
    }

    /**
     * Returns the Jain Sip Header Factory associated with this message. 
     */
    protected HeaderFactory getHeadersFactory()
    {
        return getStackProperties().getHeadersFactory();
    }

    /**
     * Returns the Jain Sip Address Factory associated with this message. 
     */
    protected AddressFactory getAddressFactory()
    {
        return getStackProperties().getAddressFactory();
    }

    /**
     * Returns the Jain stack properties associated with this message. 
     * @return StackProperties
     */
    protected StackProperties getStackProperties()
    {
        return StackProperties.getInstance();
    }
    
    /**
     * parsing remotePort out of a string without String allocations
     * @param str String to parse from
     * @param start start index
     * @param end end index
     * @return port number
     */
    private int parsePortNumber(String str, int start, int end) {
    	int number = 0;
		for (int i = start; i < end; i++) {
			char c = str.charAt(i);
			int d = c - '0';
			if (0 <= d && d <= 9) {
				number *= 10;
				number += d;
			}
			else {
				number = -1;
				break;
			}
		}
    	return number;
    }
    
    /**
     * Helper function that will set the m_localAddr, m_localPort
     * m_remoteAddr, and m_remotePort members
     * @throws HeaderParseException 
     */
    /**
     * Helper function that will set the m_localAddr, m_localPort
     * m_remoteAddr, and m_remotePort members
     * @throws HeaderParseException 
     */
    protected void parseTransport() throws HeaderParseException {
    	// The "IBM-Client-Address" header looks like this 
    	// IBM-Client-Address:  1.1.1.1:5555;local-address=2.2.2.2:5060. 
    	if (m_clientAddress != null && m_clientAddress.length()>0){
    		if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug("parseTransport: parsing clientAddress=" + m_clientAddress);
            }
    		
    		int delimeter = m_clientAddress.indexOf(";");
    		int initialRemoteDelim = m_clientAddress.indexOf(";", delimeter+1);
    		int transportDelim = m_clientAddress.indexOf(";", initialRemoteDelim+1);
    		String remoteAddrAndPort = m_clientAddress.substring(0, delimeter);
    		String localAddrAndPort = null;
    		if( initialRemoteDelim < 0 ){
    			localAddrAndPort = m_clientAddress.substring(delimeter + 1);
    		}
    		else{
    			localAddrAndPort = m_clientAddress.substring(delimeter + 1, initialRemoteDelim);
    		}
    		 
			int remotePortStart = remoteAddrAndPort.lastIndexOf(":");
			int localPortStart = localAddrAndPort.lastIndexOf(":");
			int initialRemotePortDelim = m_clientAddress.lastIndexOf(":");//the very last
			
            m_remoteAddr =  m_clientAddress.substring(0,remotePortStart);

            //parsing remotePort out of a string without String allocations
            m_remotePort = parsePortNumber(m_clientAddress, remotePortStart+1, delimeter);
            
            //local-address=
            m_localAddr = localAddrAndPort.substring(LOCAL_ADDRESS_TAG.length() + 1,localPortStart);
            
            if (initialRemoteDelim < 0) {
            	initialRemoteDelim = m_clientAddress.length();
        		m_initialRemoteAddr = m_remoteAddr;
        		m_initialRemotePort = m_remotePort;
        		m_initialTransport = getTransportInt();
            	
            } else {
    			m_initialRemoteAddr = m_clientAddress.substring(initialRemoteDelim + INITIAL_REMOTE_TAG.length() + 2,initialRemotePortDelim);
    			m_initialRemotePort = parsePortNumber(m_clientAddress, initialRemotePortDelim+1, transportDelim);
    			m_initialTransport = m_clientAddress.substring(transportDelim + 1, m_clientAddress.length()); 
            }
            	
    		m_localPort = parsePortNumber(localAddrAndPort, localPortStart+1, localAddrAndPort.length()); 
    	}   
    	if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                "SipServletMessageImpl",
                "parseTransport",
                "localAddr: " + m_localAddr + " , localPort: " + m_localPort +
                " ,remoteAddr: " + m_remoteAddr + " , remotePort: " + m_remotePort + 
                " ,initialRemoteAddress: " + m_initialRemoteAddr + " , initialRemotePort: " + m_initialRemotePort +
                " ,initialTransport: " + m_initialTransport);
        }
	}

    /**
     * The default implementation return <code>null</code>. 
     * @return
     */
    protected SipURI constructContactHeaderURI() {
    	return null;
    }
    
    /**
     * This methods creates the value of IBM-Client-Address header that should be added
     * when a messages is sent in a loopback (i.e. between 2 applications on the same container) 
     * @return
     */
    protected String createIBMClientAddrHeaderForLoopbackMessages() {
    	String host = null;
		int port = -1;
		String transport = null;
		
		String initialRemoteHost = null;
		int initialRemotePort = -1;
		
    	if (m_clientAddress == null){
    		//if this is the case, then this message was originated on an internal application and
    		// is sent to another application on this container
    		
    		//Use the provider's listening point as the Contact Address. 
            ListeningPoint point = getSipProvider().getListeningPoint();

            host = point.getHost();
            port = point.getPort();

            transport = point.getTransport();
            initialRemoteHost = host;
    		initialRemotePort = port;
    	}else{
    		//in this case, the message got the m_clientAddress from an original incoming request
    		// this will happen if the application is a proxy.
    		host = getLocalAddrInt();
    		port = getLocalPortInt();
    		transport = getInitialTransportInt();
    		initialRemoteHost = getInitialRemoteAddrInt();
    		initialRemotePort = getInitialRemotePortInt();
    	}
    		
    	
    	StringBuilder sb = new StringBuilder(100);
    	
    	sb.append(host)
    	  .append(':')
    	  .append(port)
    	  .append(';')
    	  .append(LOCAL_ADDRESS_TAG)
    	  .append('=')
    	  .append(host)
    	  .append(':')
    	  .append(port)
    	  .append(';')
    	  .append(INITIAL_REMOTE_TAG)
    	  .append('=')
    	  .append(initialRemoteHost)
    	  .append(':')
    	  .append(initialRemotePort)
    	  .append(';')
    	  .append(transport);
    	
    	return sb.toString();
    }

    /**
     * Gets the underlaying Jain Sip Message wrapped by this Sip Servlet 
     * Message.
     * @return The underlaying Sip Message if available, otherwise null
     */
    Message getJainSipMessage()
    {
        return m_message;
    }

    /**
     * Gets the Via Headers associated with this message.
     * @pre m_message != null 
     */
    List getViaHeaders()
    {
        HeaderIterator iterator = m_message.getViaHeaders();
        Vector viaHeaders = new Vector(10);

        while (iterator.hasNext())
        {
            try
            {
                viaHeaders.addElement(iterator.next());
            }
            catch (HeaderParseException e)
            {
                if (c_logger.isErrorEnabled())
                {
                    Object[] args = { this };
                    c_logger.error(
                        "error.get.via.headers",
                        Situation.SITUATION_REQUEST,
                        args,
                        e);
                }
            }
        }

        return viaHeaders;
    }

    /**
     * retrieves the top via header value as a string.
     * called for logging purposes to keep track of a transaction
     * @return the top via header value, null on error
     */
    String getTopVia() {
        try {
            Header via = m_message.getHeader(ViaHeader.name, true);
            if (via != null) {
                return via.getValue();
            }
        }
        catch (Exception e) {
            // don't produce extra noise
        }
        return null;
    }

    /**
    * Return the Jain Sip Contact Header. If more then one exist the first
    * one will be returned. 
    */
    ContactHeader getContactHeader()
    {
        ContactHeader contactHeader = null;
        if (null != m_message)
        {
            HeaderIterator iterator = m_message.getContactHeaders();
            if (null != iterator && iterator.hasNext())
            {
                try
                {
                    contactHeader = (ContactHeader) iterator.next();
                }
                catch (HeaderParseException e)
                {
                    if (c_logger.isErrorEnabled())
                    {
                        Object[] args = { this };
                        c_logger.error(
                            "error.get.contact.header",
                            Situation.SITUATION_REQUEST,
                            args,
                            e);
                    }
                }
            }
        }

        return contactHeader;
    }

    /**
     * Sets the Sip Provider associated with this message
     * @param sipProvider The Sip Provider to set. 
     */
    void setSipProvider(SipProvider sipProvider)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setSipProvider", "provider=" + sipProvider);
		}
        m_sipProvider = sipProvider;       
    }

//    /**
//     * Sets the Sip Session associated with this message. 
//     * @param sipSession The Sip Session to set. 
//     */
//    public void setSipSession(SipSession sipSession)
//    {
//        if (m_sipSession != null)
//        {
//            if (c_logger.isTraceDebugEnabled())
//            {
//                c_logger.traceDebug(
//                    this,
//                    "setSipSession",
//                    "Attempting to overide existing session binding to "
//                        + "a message. Operation aborted");
//
//            }
//
//            return;
//        }
//
//        m_sipSession = sipSession;
//    }
    
    /**
     * Sets the Sip Session associated with this message. 
     * @param sipSession The Sip Session to set. 
     */
    public void setTransactionUser(TransactionUserWrapper transactionUser)
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SipServletMessageImpl.class.getName(),
					"setTransactionUser");
		}
    	
    	if(transactionUser== null){
    		// we have this case in proxing request
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setTransactionUser", 
						"transaction user is null");
			}
    		return;
    	}
        if (m_transactionUser!= null) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "setTransactionUser",
                        "Overwriting old TU: " + m_transactionUser +
                        " With TU: " + transactionUser);

            }
        }
        m_transactionUser = transactionUser;
        
        if(_updateMessageListLater){
        	updateUnCommittedMessagesList(m_isCommited);
        	_updateMessageListLater = false;
        }
    }
    

    /**
     * Sets the Jain Sip transaction Id.
     * @param transaction Id
     */
    void setTransactionId(long transactionId)
    {
        m_transactionId = transactionId;
    }

    /**
     * @see javax.servlet.sip.SipSession#getLocalParty()
     */
    abstract Address getLocalParty();

    /**
     * @see javax.servlet.sip.SipSession#getRemoteParty()
     */
    abstract Address getRemoteParty();

    /**
     * Updates the committed state of this message. 
     * @param isCommited 
     */
    public void setIsCommited(boolean isCommited)
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { isCommited ,Integer.toHexString(this.hashCode())};
			c_logger.traceEntry(SipServletMessageImpl.class.getName(),
					"setIsCommited", params);
		}
    	
        m_isCommited = isCommited;
        if(m_transactionUser != null){
        	if (!m_transactionUser.isValid()){
        		return; //defect 591291: TU can be invalidated once a transaction was terminated 
        		//(if it was invalidation was pending while it had that transaction opened), in
        		//this case we shouldn't continue to update the pending messages
        	}
        	// we can update un-committed Messages List only if
        	// transaction user has been already set
        	updateUnCommittedMessagesList(isCommited);
        } else {
        	_updateMessageListLater = true;
        }
    }
    
    /**
     * This method will be overrode by IncomingSipServletRequest, 
     * IncomingSipServletResponse, OutgoingSipServletRequest, and 
     * OutgoingSipServletResponse.
     * @param isCommited
     * @return
     */
    abstract void updateUnCommittedMessagesList(boolean isCommited);

    /**
     *  
     * @return A iterator of all Jain Sip Headers.
     */
    protected Iterator getJainSipHeaders()
    {
        final HeaderIterator hIterator = m_message.getHeaders();

        //Wrap the Jain Sip Header Iterator with a standard Iterator
        return new Iterator()
        {
            public boolean hasNext()
            {
                return hIterator.hasNext();
            }

            public Object next()
            {
                Object next = null;
                try
                {
                    next = hIterator.next();
                }
                catch (HeaderParseException e)
                {
                    if (c_logger.isErrorEnabled())
                    {
                        Object[] args = { this };
                        c_logger.error(
                            "error.get.jain.sip.headers",
                            Situation.SITUATION_REQUEST,
                            args,
                            e);
                    }
                }
                catch (NoSuchElementException e)
                {
                    if (c_logger.isErrorEnabled())
                    {
                        Object[] args = { this };
                        c_logger.error(
                            "error.get.jain.sip.headers",
                            Situation.SITUATION_REQUEST,
                            args,
                            e);
                    }
                }

                return next;
            }

            public void remove()
            {
                if (c_logger.isTraceDebugEnabled())
                {
                    c_logger.traceDebug(
                        this,
                        "getJainSipHeaders",
                        "Remove operation not supported by this iterator");

                }
            }
        };
    }

    /**
     * @return Transaction
     */
    public SipTransaction getTransaction()
    {
        return m_transaction;
    }

    /**
     * Sets the transaction.
     * @param transaction The transaction to set
     */
    public void setTransaction(SipTransaction transaction)
    {
        m_transaction = transaction;
    }

    /**
     * Sets the Http Servlet Request that is assosicated this Sip Message. 
     * @param request
     */
    public void setHttpServletRequest(HttpServletRequest request)
    {
        m_httpServletRequest = request;
    }

    /**
     * Gets the Http Servlet Request that is assosicated this Sip Message. 
     */
    public HttpServletRequest getHttpServletRequest()
    {
        return m_httpServletRequest;
    }
    
    /**
     * Sets the Http Servlet Response that is assosicated this Sip Message. 
     * @param request
     */
    public void setHttpServletResponse(HttpServletResponse response)
    {
        m_httpServletResponse = response;
    }

    /**
     * Gets the Http Servlet Response that is assosicated this Sip Message. 
     */
    public HttpServletResponse getHttpServletResponse()
    {
        return m_httpServletResponse;
    }

    /**
     * Generates the body content as a byte array. converts the content 
     * according to the content type and character encoding. Possible conversions:
     * 1. Byte arrays contents remain as is no conversion is made. 
     * 2. String content or Content-Type of text/* is converted to byte array
     *    using the specified character set or UTF-8 if one is not specified.
     * 3. If neither 1 and 2 match then the object is conveted to String by 
     * 	  calling toString() and then encoded by using the specified Character 
     *    set or UTF-8 if one is not available.
     *    TODO:I am not sure whether we should do option 3 or throw an exception.    
     * @return byte array representation of the body. 
     */
    protected byte[] getBodyAsBytes(Object body, ContentTypeHeader contentTypeHeader)
        throws UnsupportedEncodingException
    {
        if (null == body || body instanceof byte[])
        {
            //No need to convert already provided as byte array. 
            return (byte[]) body;
        }

        byte[] rValue = null;

        //The only purpose of the check bellow is to verify that we are doing
        //the right conversion. If not then we need to alert about this.
        String contentType = contentTypeHeader == null
        	? null
        	: contentTypeHeader.getContentType();
        if (!(body instanceof String || contentType.equals(TEXT_MIME_TYPE))) {
            //We should only use string conversion for String and content of type text/* 
            //TODO Add support for other types besides Strings.
        	throw new IllegalArgumentException("unsupported content type [" + + ']');
        }

        //Convert the String/Text to byte array accoding the appropriate 
        //encoding.  
        String encoding = contentTypeHeader.getParameter("charset");
        if (null == encoding || encoding.length() == 0)
        {
            encoding = DEFAULT_CHARSET_ENCODING;
        }

        rValue = body.toString().getBytes(encoding);

        return rValue;
    }

    /**
     * Gets HeaderIterator of all Headers in the internal Jain SIP Message.
     * Note that order of Headers in HeaderIterator is same as order in
     * Message
     * (Returns null if no Headers exist)
     * @return HeaderIterator of all Headers in Message
     */
    public HeaderIterator getJainHeaders()
    {
        return m_message.getHeaders();
    }
    
    /**
     * Gets HeaderIterator of all headers in the internal JAIN SIP Message,
     * that match the given header name.
     * Note that order of headers in HeaderIterator is same as order in message
     * (Returns null if no headers exist with the requested name)
     * @param headerName The header name to find and return
     * @return HeaderIterator of all headers in message matching the given name,
     *  null if no such headers
     */
    public HeaderIterator getJainHeaders(String headerName) {
        return m_message.getHeaders(headerName);
    }

    /**
     * Helper functions - Finds a Jain Sip Header in the list of available
     * headers. If more then one exists the first found will be used as 
     * a match. 
     * Derived class should provide specific implementation according to 
     * their internal data structure. 
     * @return The matching header if avaliable otherwise null. 
     */
    protected Header findHeader(String name) throws HeaderParseException
    {
        return m_message.getHeader(name, true);
    }
    
    /**
     * Log a message to the class logger, use this method to improve the
     * 	the readability of this class implementation
     * @param method the calling method
     * @param message the message
     * @param t the exception if exist
     */
    protected void log(String method, String message, Throwable t)
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, method, message, t);
        }

    }

    /**
     * Log a message to the class logger, use this method to improve the
     * 	the readability of this class implementation
     * @param method the calling method
     * @param message the message
     */
    protected void log(String method, String message)
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, method, message);
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        if (null != m_message)
        {
            return m_message.toString();
        }
        else
        {
            return super.toString();
        }
    }

    /**
     * Utilitiy function for logging exceptions. 
     * @param e
     */
    protected void logException(Exception e) 
    {
        if(c_logger.isErrorEnabled())
        {
            c_logger.error("error.exception", 
                           Situation.SITUATION_CREATE, 
                    	   null, e);
        }
    }

	/**
	 * @see javax.servlet.sip.SipServletMessage#getHeaderForm()
	 */
	public HeaderForm getHeaderForm() {
		MessageImpl message = (MessageImpl)m_message;
		com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl.HeaderForm form =
			message.getHeaderForm();
		switch (form) {
		case COMPACT:
			return HeaderForm.COMPACT;
		case DEFAULT:
			return HeaderForm.DEFAULT;
		case LONG:
			return HeaderForm.LONG;
		default:
			throw new RuntimeException("unknown header form [" + form + ']');
		}
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#setHeaderForm(javax.servlet.sip.SipServletMessage.HeaderForm)
	 */
	public void setHeaderForm(HeaderForm form) {
		MessageImpl message = (MessageImpl)m_message;
		switch (form) {
		case COMPACT:
			message.setHeaderForm(com.ibm.ws.jain.protocol.ip.sip.message.
				MessageImpl.HeaderForm.COMPACT);
			break;
		case DEFAULT:
			message.setHeaderForm(com.ibm.ws.jain.protocol.ip.sip.message.
				MessageImpl.HeaderForm.DEFAULT);
			break;
		case LONG:
			message.setHeaderForm(com.ibm.ws.jain.protocol.ip.sip.message.
				MessageImpl.HeaderForm.LONG);
			break;
		default:
			throw new RuntimeException("unknown header form [" + form + ']');
		}
	}

	/**
     * @return Returns the m_isFailovered.
     */
    public boolean isFailovered() {
        return m_isFailovered;
    }
    
    /*
     * Helper function used to check if live message and print a message in trace
     */
	protected boolean isLiveMessage(String methodName)
	{
	    if(isFailovered()){
	        if (c_logger.isTraceDebugEnabled()) {
	            c_logger.traceDebug(this, methodName, 
	           "This Request was failovered - cannot perform this action");
	        }
	        return false;
	    }
	    return true;
	}
	
	/**
	 * An exception has occured log it to the session sequence log. 
     * @param status Status/Reason code for the specified error
     * @param e
     */
    public void logExceptionToSessionLog(int status, Exception e) {

//    	Remove
//        SipSessionImplementation session = getSessionForInrernalUse();
//        if(session != null)
//        {
//            session.logToContext(status, e.getMessage(), this);
//        }
//    	End
    	if (m_transactionUser != null) {
			m_transactionUser.logToContext(status, e.getMessage(), this);
		}
    }

	/**
     * Create a contact header based either on the slsp/network dispatcher 
     * address if available otherwise using the local listening point. 
     * @return the newly created Contact header, 
     * 			or null if it is not relevant for the message Context
     */
    protected ContactHeader createAndSetContactHeader(Message message, SipURI sipURI, boolean first) throws SipParseException {
        
    	if (m_sipProvider == null){
    		//this method already sets the sip provider into the message's member
    		selectProvider(getTransport());
    		//we now state that our contact header is the default one. 
    		m_isDefaultContactHeader = true;
    	}
    	
    	ListeningPoint point = getSipProvider().getListeningPoint();
    	String transport = point.getTransport();
    	String host = null;
        int port = -1; 
        
        if (sipURI == null) {
	    	//Use the provider's listeing point as the Contact Address. 
        	host = point.getSentBy();
            port = point.getPort();
            if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createAndSetContactHeader",
						"Using the local listening point - " + host + ":" + port);
			}
    	}
        else
	    {
       		//	Setup the contact header to match the passed in SIP URI. This is typically used when the
	    	//	prefered outbound interface is specified.
        	host = sipURI.getHost();
        	port = sipURI.getPort();
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createAndSetContactHeader",
						"Using the prefered outbound interface - " + host + ":" + port);
			}
	    }

        SipURL sipUrl = null;
    	ContactHeader currentContact = getContactHeader();
        if(currentContact != null){
        	sipUrl = (SipURL)currentContact.getNameAddress().getAddress();
        	sipUrl.setHost(host);
        }else{
        	sipUrl = getAddressFactory().createSipURL(host);
        }
    	
        sipUrl.setPort(port);
        sipUrl.setTransport(transport);

        setContactScheme(sipUrl);
        SipStackUtil.fixTargetSipUri(sipUrl);
        
        if(currentContact == null){        	
        	NameAddress address = getAddressFactory().createNameAddress(sipUrl);
        	currentContact = getHeadersFactory().createContactHeader(address);
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createAndSetContactHeader",
						"Create a new Contact header - " + currentContact);
			}
        	message.setHeader(currentContact, first);
        }
        
        return currentContact;
    }
   

	/**
	 * Helper method to set relevant Scheme to Contact header.
	 * This method is an extension point to override in the derived classes if
	 * relevant
	 * @param sipUrl
	 */
	protected void setContactScheme(SipURL sipUrl) 
		throws IllegalArgumentException, SipParseException {
	}
	
	/**
	* @see com.ibm.ws.sip.container.servlets.ext.SipServletMessageExt#getArrivedTime()
	*/
	public long getArrivedTime() {
		return m_arrivedTime;
	}
	
	/**
     * @param arrivedTime The arrivedTime to set.
     */
    public void setArrivedTime(long arrivedTime) {
        m_arrivedTime = arrivedTime;
        
        //if the propertis is true then we should save the arraival time as an attribute in addition to the variable.
        boolean prop = PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.SAVE_MESSAGE_ARRIVAL_TIME_ATTRIBUTE);
        if(prop) {
        	setAttribute(SIP_MESSAGE_ARRIVAL_TIME, arrivedTime);
        }
    }

	/**
	 * Helper method which is actually selects the provider
	 * according to received transport.
	 * @param transport
	 */
	protected boolean selectProvider(String transport) {
	
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SipServletMessageImpl.class.getName(),
					"selectProvider", transport);
		}
		
		//TODO Liberty the code in this method is confusing and generally terrible. We need to investigate again if all of this is necessary
		
		boolean changed = false;
		
	
		// If the transport wasn't found (if sceme = sips -> transport will be
		// TLS) - by default transport should be UDP
		if (transport == null) {
			transport = UDP;
		}
		
		SipProvider provider = m_sipProvider;
		
		// If we don't have a provider or it does not match the transport needed
		// then we select a new provider. Preferably we use the provider
		// already associated with this request so inbound and outbound messages
		// will be sent/received from the same port. e.g B2B application
		if (null == provider
			|| !provider.getListeningPoint().getTransport().equalsIgnoreCase(transport)) {
	
			
			provider = StackProperties.getInstance().getProvider(transport);
			setSipProvider(provider);
			
			changed = true;
		}
	
		TransactionUserWrapper tUser = getTransactionUser();
		
		// In Proxy OutgoingRequest doesn't relate to any
		// SipSession - in this case we will send the new OutgoingRequest
		// over new provider.
		if (tUser != null) {
			//TODO Liberty as long as we are not supporting virtual hosts, this following code has no meaning 
			
//			/*
//			 * select the listening point (provider) according to the application virtual host.
//			 * we want to make sure that lp will use the same port as the relevant application listenting point (VH)
//			 */
//			SipAppDesc desc =  tUser.getSipServletDesc().getSipApp();
//	    	if(desc != null){
//	    		List<VirtualHostAlias> vhAliases = desc.getVirtualHostAliases();
//	    		for (VirtualHostAlias alias : vhAliases) {
//		    		//check if the selected lp has the same port
//		    		if (!compareLPToVHAlias(alias, provider.getListeningPoint())) {	 
//		    			if (c_logger.isTraceDebugEnabled()) {
//		    				c_logger.traceDebug(this, "selectProvider",
//		    						"selected provider (" + provider + ") is different from the VH:" + alias);
//		    			}
//		    			boolean match = false;
//		    			Iterator<SipProvider> iter = StackProperties.getInstance().getProviders(transport, alias.getPort());
//		    			if (iter != null){
//			    			while (iter.hasNext() && !match) {
//			    				SipProvider sipProvider = iter.next();
//			    				if (compareHosts(alias.getHost(), sipProvider.getListeningPoint().getHost())) {
//			    					setSipProvider(sipProvider);
//				    		      	provider = sipProvider;
//					    			changed = true;
//					    			match = true;
//			    				}else{
//					    			if (c_logger.isTraceDebugEnabled()) {
//					    				c_logger.traceDebug(this, "selectProvider",
//					    						"provider (" + provider + ") is different from the VH:" + alias);
//					    			}
//			    				}
//							}
//			    			
//			    			if (match){
//				    			if (c_logger.isTraceDebugEnabled()) {
//				    				c_logger.traceDebug(this, "selectProvider",
//				    						"matched provider - " + provider + ". VH:" + alias);
//				    			}		    				
//			    				break;
//			    			}
//		    			}else{
//			    			if (c_logger.isTraceDebugEnabled()) {
//			    				c_logger.traceDebug(this, "selectProvider",
//			    						"can not locate providers for transport:" + transport+ " port:" + alias.getPort());
//			    			}		    				
//		    			}
//		    		}				
//				}
//	        }
			
			tUser.setProvider(provider);
		}
	
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "selectProvider",
					"Allocated provider: " + provider);
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "selectProvider", provider);
		}
		return changed;
	}
	
	/**
	 * Comparing 2 hosts
	 * @param aliasHost
	 * @param lpHost
	 * @return
	 */
	private boolean compareHosts(String aliasHost, String lpHost){
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "compareHosts",
					"comparing hosts: aliasHost: " + aliasHost + "lpHost: " + lpHost);
		}		
		if (aliasHost.equals("*")){
			return true;
		}
		
		return aliasHost.equals(lpHost);
	}
	
	/**
	 * Compare a VH alias with a listening point
	 * @param alias
	 * @param lp
	 * @return
	 */
	private boolean compareLPToVHAlias(VirtualHostAlias alias, ListeningPoint lp){
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "compareLPToVHAlias",
					"comparing: alias: " + alias + "listeningPoint: " + lp);
		}	
		boolean result = true;
		if (!alias.isAnyHost()){
			result = alias.getHost().equals(lp.getHost());
		}
		if(result && !alias.isAnyPort()){
			result = result && alias.getPort() == lp.getPort();
		}
		
		return result;
	}
	
    
    /**
     * get IBM-Client-Address header value
     * @return
     */
    public String getIbmClientAddress() {
    	return m_clientAddress;
    }
    
    /**
     * set IBM-Client-Address header value
     * @return
     */    
    public void setIbmClientAddress(String address) {
    	m_clientAddress = address;
    }
    
    
	/**
     * @see javax.servlet.sip.SipServletMessage#getLocalAddr()
     */
	protected String getLocalAddrInt()
	{
//      Assumption is that nobody can change the "IBM-Client-Address" header
    	if (null == m_localAddr)
    	{
	        try {
	        	parseTransport();
			} catch (HeaderParseException e) {
				log("getLocaleAddr", "Unable to get local transport", e);
			}
	    }
        return m_localAddr;
	}

	/**
	 * @see javax.servlet.sip.SipServletMessage#getLocalPort()
	 */
	protected int getLocalPortInt()
	{
//      Assumption is that nobody can change the "IBM-Client-Address" header
    	if (-1 == m_localPort)
    	{
	        try {
	        	parseTransport();
			} catch (HeaderParseException e) {
				log("getLocalPort", "Unable to get local transport", e);
			}
	    }
        return m_localPort;
    }

	/**
     * @see javax.servlet.sip.SipServletMessage#getRemoteAddr()
     */
	protected String getRemoteAddrInt()
    {
//      Assumption is that nobody can change the "IBM-Client-Address" header
    	if (null == m_remoteAddr)
    	{
	        try {
	        	parseTransport();
			} catch (HeaderParseException e) {
				log("getRemoteAddr", "Unable to get remote transport", e);
			}
	    }
        return m_remoteAddr;
    }

    /**
     * @see javax.servlet.sip.SipServletMessage#getRemotePort()
     */
	protected int getRemotePortInt()
    {
//      Assumption is that nobody can change the "IBM-Client-Address" header
    	if (-1 == m_remotePort)
    	{
	        try {
	        	parseTransport();
			} catch (HeaderParseException e) {
				log("getRemotePort", "Unable to get remote transport", e);
			}
	    }
        return m_remotePort;
    }
    
	protected String getInitialRemoteAddrInt() {
//      Assumption is that nobody can change the "IBM-Client-Address" header
    	if (null == m_initialRemoteAddr)
    	{
	        try {
	        	parseTransport();
			} catch (HeaderParseException e) {
				log("getInitialRemoteAddr", "Unable to get initial remote transport", e);
			}
	    }
        return m_initialRemoteAddr;
	}
    

	protected int getInitialRemotePortInt() {
//      Assumption is that nobody can change the "IBM-Client-Address" header
    	if (-1 == m_initialRemotePort)
    	{
	        try {
	        	parseTransport();
			} catch (HeaderParseException e) {
				log("getInitialRemotePort", "Unable to get initial remote port", e);
			}
	    }
        return m_initialRemotePort;
	}
	
    /**
     * @see javax.servlet.sip.SipServletMessage#getTransport()
     */
	protected String getTransportInt()
    {
    	if (!isLiveMessage("getTransport"))
    		return null;

    	if (getSipProvider() != null){
    		return getSipProvider().getListeningPoint().getTransport();
    	}else{
    		return null;
    	}
    }	

	protected String getInitialTransportInt() {
		if (m_initialTransport == null) {
	        try {
	        	parseTransport();
			} catch (HeaderParseException e) {
				log("getInitialTransport", "Unable to get initial transport", e);
			}			
		}
		return m_initialTransport;
	}	
	

	 /**
     * Added to prevent a case described in defect 568973 were
     * ObjectGrid seialized the SIP message while it was modified from
     * another thread of the SIP stack.
     * @param out
     * @author mordechai
     * @throws IOException 
     */
    private void cloneForObjectGrid(ObjectOutput out) throws IOException {
    	// Moti: added for OG defect 568973 : upon serialization of SIPMessageImpl a
    	// SIP container thread changes that object and we got NPE
    	MessageImpl msg = (MessageImpl)m_message.clone();
    	out.writeObject(msg);
    } 
	
	/**
	 * This will reset the content length to a correct length in case that the user changed it
	 * incorrectly. This is required on certain platforms so that the messaeg will be accepted.
	 */
	protected void resetContentLength(){
		/*TODO Liberty if(AdminHelper.getPlatformHelper().isZOS()){
        	//This is done so that TCK 289 will pass on zOS platform 
        	//the proxy on the CR cannot tolerate the content-length header being wrong
        	//so we remove any such header that the application might have put there and 
        	//let the stack calculate it automatically.
        	if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "resetContentLength", "checking content-length header before send on z/OS");
    		}
        	byte[] body = getMessage().getBodyAsBytes();
        	
    		if( body == null && getContentLength() !=0 ){
    			if (c_logger.isTraceDebugEnabled()) {
        			c_logger.traceDebug(this, "resetContentLength", 
        					"resetting content-length header before send on z/OS body.length=" + 0 +
        					"current content length header="+getContentLength());
        		}
    			setContentLength(0);
    		}else if(body != null && getContentLength() != body.length){
    			if (c_logger.isTraceDebugEnabled()) {
        			c_logger.traceDebug(this, "resetContentLength", 
        					"resetting content-length header before send on z/OS body.length=" + body.length +
        					"current content length header="+getContentLength());
        		}
    			setContentLength(body.length);
    		}else{
    			if (c_logger.isTraceDebugEnabled()) {
        			c_logger.traceDebug(this, "resetContentLength", 
        					"no need for resetting content-length header before send on z/OS. " +
        					(body == null ? "body is null" : "body.length=" + body.length) +
        					", current content length header="+getContentLength());
        		}
    		}
		}*/
		//TODO the same needs to be done in case of TCP prtocol, regradless to z/OS
	}
    
	/**
     * check if the TAI indicated that the current user is unauthenticated
     * 
     * @return
     */
    private boolean isUnauthenticatedUser(){
    	String remoteUser = m_httpServletRequest.getRemoteUser();
    	
    	//check if the TAI indicates that this user is not authenticated
    	if (remoteUser != null && UNAUTHENTICATED.equals(remoteUser)){
    		 if (c_logger.isTraceDebugEnabled()) {
 	            c_logger.traceDebug(this, "isUnauthenticatedUser", "user is unauthenticated according to the TAI");
 	        }
    		return true;
    	}else{
    		return false;
    	}
    }
}
