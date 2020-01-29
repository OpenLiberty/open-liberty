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
package com.ibm.ws.jain.protocol.ip.sip.address;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.AddressFactory;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.address.URI;

import java.net.InetAddress;
import java.util.Map;

import com.ibm.ws.jain.protocol.ip.sip.header.ParametersImpl;
import com.ibm.ws.sip.parser.URIParser;
import com.ibm.ws.sip.properties.StackProperties;

/**
 * A factory for addresses. 
 * 
 * @author Assaf Azaria, April 2001.
 */
public class AddressFactoryImpl implements AddressFactory
{
	/**
	* cache constants maybe overridden by some properties file
	*/
	public static int s_sipurl_cache_init_size = StackProperties.SIPURL_CACHE_INIT_SIZE_DEFAULT;
	/**
	 * when the LRU cache reach this size , we shall begin
	 * throwing the eldest entry.
	 */
	public static  int s_sipurl_cache_max_size = StackProperties.SIPURL_CACHE_MAX_SIZE_DEFAULT;
	private static float SIPRL_CACHE_LOAD_FACTOR = 0.75f; 
	
	/**
	 * a LRU cache of SIPURLImpl classes...
	 * Moti: I'm not sure about the synchronized here but
	 * I put it anyway , to be safe. We shall see how this affects
	 * performance. 
	 */
	private static Map m_sipurlLRUCache = null;

	/** per-thread parser */
	private final ThreadLocal<URIParser> m_uriParser =
		new ThreadLocal<URIParser>() {
			protected URIParser initialValue() {
				return new URIParser();
			}
		};
	
	/**
	 * This factory method is not part of the JAIN API (AddressFactory)
	 * but is needed in order to manage the SipURLImpl 
	 * @return a new ImmutableSipURLImpl
	 * @throws SipParseException
	 */
	public static SipURL createSipURL(String schema,String username,String passwd,
			String hostname ,int hostport, ParametersImpl optionalParams, ParametersImpl optionalHeaders,String userType,String transport)
		throws  SipParseException
	{
			SipURLImpl impl = new SipURLImpl();
			if (schema != null) impl.setScheme(schema);
			if (username != null) impl.setUserName(username);
			
			if (passwd != null)
				try {
					impl.setUserPassword(passwd);
				} catch (SipException e) {
					throw new SipParseException(e.getMessage());
				}
			if (hostname != null) impl.setHost(hostname);
			if (hostport > 0) impl.setPort(hostport);
			if (optionalParams != null) impl.setParameters(optionalParams);
			if (optionalHeaders != null) impl.setHeaders(optionalHeaders);
			if (userType != null) impl.setUserType(userType);
			if (transport != null) impl.setTransport(transport);
			return impl;
	}
	
    /**
     * Creates a SipURL based on given host
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     */
    public SipURL createSipURL(InetAddress host)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {
        	throw new IllegalArgumentException("Address Factory: Null host"); 
        } 
    	return createSipURL(host.getHostName());

    }

    /**
     * Creates a SipURL based on given host
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public SipURL createSipURL(String host)
        throws IllegalArgumentException, SipParseException
    {
		if (host == null)
		{
			throw new IllegalArgumentException("Address Factory: Null host"); 
		} 
		
		SipURLImpl impl = new SipURLImpl();
		impl.setHost(host);
		return impl;
    }

    /**
     * Creates a SipURL based on given user and host
     * @param <var>user</var> user
     * @param <var>host</var> host
     * @throws IllegalArgumentException if user or host is null
     * @throws SipParseException if user or host is not accepted
     * by implementation
     */
    public SipURL createSipURL(String user, InetAddress host)
        throws IllegalArgumentException, SipParseException
    {
        if (user == null)
        {
        	throw new IllegalArgumentException("Address Factory: null user"); 
        } 
		if (host == null)
		{
			throw new IllegalArgumentException("Address Factory: Null host"); 
		} 
		
		return createSipURL(user, host.getHostName());

    }

    /**
     * Creates a SipURL based on given user and host
     * @param <var>user</var> user
     * @param <var>host</var> host
     * @throws IllegalArgumentException if user or host is null
     * @throws SipParseException if user or host is not accepted 
     * by implementation
     */
    public SipURL createSipURL(String user, String host)
        throws IllegalArgumentException, SipParseException
    {
        if (user == null)
        {
        	throw new IllegalArgumentException("Address Factory: null user"); 
        } 
		if (host == null)
		{
			throw new IllegalArgumentException("Address Factory: Null host"); 
		} 

		SipURLImpl url = new SipURLImpl();
		url.setHost(host);
		url.setUserName(user);
		return url;
    }

    /**
     * Creates a NameAddress based on given address
     * @param <var>address</var> address URI
     * @throws IllegalArgumentException if address is null or not from same
     * JAIN SIP implementation
     */
    public NameAddress createNameAddress(URI address)
        throws IllegalArgumentException
    {
        if (address == null)
        {
        	throw new IllegalArgumentException("AddressFactory: null address"); 
        } 
        
        NameAddressImpl nameAddress = new NameAddressImpl();
		nameAddress.setAddress(address);

        return nameAddress;
    }

    /**
     * Creates a NameAddress based on given diaplay name and address
     * @param <var>displayName</var> display name
     * @param <var>address</var> address URI
     * @throws IllegalArgumentException if displayName or address is null, or
     * address is not from same JAIN SIP implementation
     * @throws SipParseException if displayName is not accepted
     * by implementation
     */
    public NameAddress createNameAddress(String displayName, URI address)
        throws IllegalArgumentException, SipParseException
    {
        if (address == null)
        {
        	throw new IllegalArgumentException("AddressFactory: Null address"); 
        } 
        if (displayName == null)
        {
			throw new IllegalArgumentException("AddressFactory: Null display name");
        }

        
        NameAddressImpl nameAddress = new NameAddressImpl();
		nameAddress.setAddress(address);
		nameAddress.setDisplayName(displayName);

        return nameAddress;
    }

    /**
     * Creates a URI based on given scheme and data. Bug reported 
     * by Lamine Brahimi (IBM Zurich) was included here.
     * @param <var>scheme</var> scheme
     * @param <var>schemeData</var> scheme data
     * @throws IllegalArgumentException if scheme or schemeData are null
     * @throws SipParseException if scheme or schemeData is not accepted by 
     * implementation
     */
    public URI createURI(String scheme, String schemeData) 
    	throws IllegalArgumentException, SipParseException
    {
        if (scheme == null)
        {
        	throw new IllegalArgumentException("AddressFactory: Null scheme"); 
        } 
        if (schemeData == null)
        {
        	throw new IllegalArgumentException("AddressFactory: Null schemeData"); 
        } 
        
        // Try to parse the URI.
        URIParser uriParser = getParser();
        URI uri = uriParser.parse(scheme, schemeData);
        return uri;
    }
    
    /**
     * @return - the current URI parser instance.
     */
    private URIParser getParser()
    {
    	URIParser uriParser = m_uriParser.get();
    	return uriParser;
    }
    
    private static Map GetCache()
    {
    	if (m_sipurlLRUCache == null)
    	{
			//Moti: since in JDK 1.3 there is no LRUMap I use
			// apache's one...
    		m_sipurlLRUCache = 
    			new java.util.Hashtable(s_sipurl_cache_init_size,SIPRL_CACHE_LOAD_FACTOR);

    		//LinkedHashMap exists only in JDK 1.4+
			// and LWP 2.5 runs on JDK 1.3//			
//    		m_sipurlLRUCache = Collections.synchronizedMap(
//    				new java.util.LinkedHashMap.LinkedHashMap(s_sipurl_cache_init_size,SIPRL_CACHE_LOAD_FACTOR,true)
//					{
//    					protected boolean removeEldestEntry(Map.Entry eldest) {
//    						if (this.size() > s_sipurl_cache_max_size)
//    							return true;
//    						return false;
//    					}
//					});
    	}
    	return m_sipurlLRUCache;
    	
    }
    
    private static void UpdateCache(Object key, Object value)
    {
    	// Moti: until a better caching mechanism is deployed
    	// empty all the cache .
    	if (GetCache().size() > s_sipurl_cache_max_size)
    		m_sipurlLRUCache = null;
    	
    	GetCache().put(key,value);
    }
}
