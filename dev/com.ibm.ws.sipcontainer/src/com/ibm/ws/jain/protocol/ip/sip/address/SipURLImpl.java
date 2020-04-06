/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
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
import jain.protocol.ip.sip.address.SipURL;

import java.net.InetAddress;
import java.util.Iterator;

import com.ibm.ws.jain.protocol.ip.sip.header.ParametersImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersImpl.Parameter;
import com.ibm.ws.sip.parser.Coder;
import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;

/**
 *	Sip url implementation.
 * Moti: I changed the accessibility of this class to package only.
 * If someone need to create a SipURL use the factory:
 * problem: presence server directly use sipurlImpl.
 * AddressFactoryImpl.CreateSIPURL(...)
 * @author  Assaf Azaria, April 2003.
 */
public class SipURLImpl extends URIImpl implements SipURL
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 4194354724484789196L;

	//
	// Constants.
	//
	public static final String USER      = "user";
	public static final String SIP 	     = "sip";
	public static final String TRANSPORT = "transport";
	public static final String METHOD    = "method";
	public static final String TTL       = "ttl";
	public static final String MADDR     = "maddr";
	
	//
	// Members.
	//
	
	/**
	 * The host.
	 */
	String m_host;
	
	/**
	 * The port number.
	 */
	int m_port = -1;
	
	/**
	 * The user's name.
	 */
	String m_userName;
	
	/**
	 * The user password. 
	 */
	String m_userPassword;
	
	/**
	 * The user type. (SipUrl.USER_TYPE_IP, SipUrl.USER_TYPE_PHONE) 
	 */
	String m_userType;	
	
	// TODO: incorporate TelNumber.
	/**
	 * The isdn subaddress (for phone user). 
	 */
	String m_isdnSubAddress;
	
	/**
	 * The post dial (for phone user).
	 */		
	String m_postDial;
		
	/**
	 * The global flag (for phone user).
	 */
	boolean m_isGlobal;
	
	/**
	 * The list of url headers.
	 */
	private ParametersImpl m_headers;

	/**
	 * The list of url parameters.
	 */
	private ParametersImpl m_params;
	
	/**
     */
    public SipURLImpl()
    {
    	super(SIP);
   	}

	//
	// Methods.
	//
	
	/**
	 * Sets ISDN subaddress of SipURL
	 * @param <var>isdnSubAddress</var> ISDN subaddress
	 * @throws IllegalArgumentException if isdnSubAddress is null
	 * @throws SipException if user type is not USER_TYPE_PHONE
	 * @throws SipParseException if isdnSubAddress is not accepted
	 * by implementation
	 */
	public void setIsdnSubAddress(String isdnSubAddress)
		throws IllegalArgumentException, SipException, SipParseException
	{
		if (isdnSubAddress == null)
		{
			throw new IllegalArgumentException("SipUrl: null isdnSubAddress"); 
		} 
		
		if (!m_userType.equals(USER_TYPE_IP) &&
            !m_userType.equals(USER_TYPE_PHONE))
        {
			throw new SipException("SipUrl : user type is not USER_TYPE_PHONE");
        }
				
		m_isdnSubAddress = isdnSubAddress;	
	}


    /**
     * Gets ISDN subaddress of SipURL
     * (Returns null if ISDN subaddress does not exist)
     * @return ISDN subaddress of SipURL
     */
    public String getIsdnSubAddress()
    {
		return m_isdnSubAddress;
    }

    /**
     * Gets boolean value to indicate if SipURL
     * has ISDN subaddress
     * @return boolean value to indicate if SipURL
     * has ISDN subaddress
     */
    public boolean hasIsdnSubAddress()
    {
		return m_isdnSubAddress != null;	
    }

    /**
     * Removes ISDN subaddress from SipURL (if it exists)
     */
    public void removeIsdnSubAddress()
    {
    	m_isdnSubAddress = null;
    }

    /**
     * Gets post dial of SipURL
     * (Returns null if post dial does not exist)
     * @return post dial of SipURL
     */
    public String getPostDial()
    {
    	return m_postDial;
	}

    /**
     * Gets boolean value to indicate if SipURL
     * has post dial
     * @return boolean value to indicate if SipURL
     * has post dial
     */
    public boolean hasPostDial()
    {
    	return m_postDial != null;
    }

    /**
    * Sets post dial of SipURL
    * @param <var>postDial</var> post dial
    * @throws IllegalArgumentException if postDial is null
    * @throws SipException if user type is not USER_TYPE_PHONE
    * @throws SipParseException if postDial is not accepted by implementation
    */
    public void setPostDial(String postDial)
        throws IllegalArgumentException, SipException, SipParseException
    {
        if (postDial == null)
        {
			throw new IllegalArgumentException("SipUrl: null postDial");
        }
        
        if (!m_userType.equals(USER_TYPE_PHONE))
        {
			throw new SipException("SipUrl: user type is not USER_TYPE_PHONE");
        }
           
        m_postDial = postDial;
    }

    /**
     * Removes post dial from SipURL (if it exists)
     */
    public void removePostDial()
    {
    	m_postDial = null;
    }

    /**
     * Gets user name of SipURL
     * (Returns null if user name does not exist)
     * @return user name of SipURL
     */
    public String getUserName()
    {
    	return m_userName;
    }

    /**
     * Gets boolean value to indicate if SipURL
     * has user name
     * @return boolean value to indicate if SipURL
     * has user name
     */
    public boolean hasUserName()
    {
    	return m_userName != null;
    }

    /**
     * Removes user name from SipURL (if it exists)
     */
    public void removeUserName()
    {
    	m_userName = null;
    }

    /**
     * Sets user name of SipURL
     * @param <var>userName</var> user name
     * @throws IllegalArgumentException if userName is null
     * @throws SipParseException if userName is not accepted by implementation
     */
    public void setUserName(String userName)
        throws IllegalArgumentException, SipParseException
    {
		if (userName == null)
        {
			throw new IllegalArgumentException("SipUrl: Null user name");
        }
		if (userName.length() == 0) {
			// empty user name has the same effect as removeUserName()
			userName = null;
		}
        
		m_userName = userName;
    }

    /**
     * Gets user password of SipURL
     * (Returns null if user pasword does not exist)
     * @return user password of SipURL
     */
    public String getUserPassword()
    {
		return m_userPassword;
    }

    /**
     * Gets boolean value to indicate if SipURL
     * has user password
     * @return boolean value to indicate if SipURL
     * has user password
     */
    public boolean hasUserPassword()
    {
    	return (m_userPassword != null && !m_userPassword.equals(""));
    }

    /**
     * Removes user password from SipURL (if it exists)
     */
    public void removeUserPassword()
    {
    	m_userPassword = null;
    }

    /**
     * Sets user password of SipURL
     * @param <var>userPassword</var> user password
     * @throws IllegalArgumentException if userPassword is null
     * @throws SipException if user name does not exist
     * @throws SipParseException if userPassword is not accepted
     * by implementation
     */
    public void setUserPassword(String userPassword)
        throws IllegalArgumentException, SipException, SipParseException
    {
		if (userPassword == null)
        {
            throw new IllegalArgumentException("SipUrl: Null password");
        }
        
        m_userPassword = userPassword;
    }
    

	/**
	 * Gets user type of SipURL
	 * (Returns null if user type does not exist)
	 * @return user type of SipURL
	 */
	public String getUserType()
	{
		return m_userType;
	}

	/**
	 * Gets boolean value to indicate if SipURL
	 * has user type
	 * @return boolean value to indicate if SipURL
	 * has user type
	 */
	public boolean hasUserType()
	{
		return m_userType != null;
	}

	/**
	 * Removes user type from SipURL (if it exists)
	 */
	public void removeUserType()
	{
		m_userType = null;
	}

	/**
	 * Sets user type of SipURL
	 * @param <var>userType</var> user type
	 * @throws IllegalArgumentException if userType is null
	 * @throws SipParseException if userType is not accepted by implementation
	 */
	public void setUserType(String userType)
		throws IllegalArgumentException, SipParseException
	{
		if (userType == null)
		{
			throw new IllegalArgumentException("SipUrl: null userType");
		}
        
		if (!userType.equals(USER_TYPE_IP) &&
			!userType.equals(USER_TYPE_PHONE))
		{
			throw new SipParseException("SipUrl: unknown user type", "");
		}
        
		m_userType = userType;
	}

    /**
     * Gets host of SipURL
     * @return host of SipURL
     */
    public String getHost()
    {
    	return m_host;
    }

    /**
     * Sets host of SipURL
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setHost(String host)
        throws IllegalArgumentException, SipParseException
    {
		if (host == null)
        {
			throw new IllegalArgumentException("SipUrl: Null host");
        }
        
        // TODO: parse the host?
        m_host = host;
	}

    /**
     * Sets host of SipURL
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setHost(InetAddress host)
        throws IllegalArgumentException, SipParseException
    {
		if (host == null)
        {
        	throw new IllegalArgumentException("SipUrl: Null host"); 
        } 
        
        m_host = host.getHostName();
           
    }

    /**
     * Gets port of SipURL
     * (Returns negative int if port does not exist)
     * @return port of SipURL
     */
    public int getPort()
    {
    	return m_port;
    }

    /**
     * Gets boolean value to indicate if SipURL
     * has port
     * @return boolean value to indicate if SipURL
     * has port
     */
    public boolean hasPort()
    {
    	return m_port != -1;
    }

    /**
     * Removes port from SipURL (if it exists)
     */
    public void removePort()
    {
        m_port = -1;
    }

    /**
     * Sets port of SipURL
     * @param <var>port</var> port
     * @throws SipParseException if port is not accepted by implementation
     */
    public void setPort(int port) throws SipParseException
    {
		if (port < 0)
		{
			throw new SipParseException("SipUrl: Negative port");
		}
		
		m_port = port;
    }

    /**
     * Gets TTL of SipURL
     * (Returns negative int if TTL does not exist)
     * @return TTL of SipURL
     */
    public int getTTL()
    {
		if (hasTTL())
    	{
    		return Integer.parseInt(getParameter(TTL));
    	}
    	
    	return -1;
    }

    /**
     * Gets boolean value to indicate if SipURL
     * has TTL
     * @return boolean value to indicate if SipURL
     * has TTL
     */
    public boolean hasTTL()
    {
        return (getParameter(TTL) != null);
    }

    /**
     * Removes TTL from SipURL (if it exists)
     */
    public void removeTTL()
    {
        removeParameter(TTL);
    }

    /**
     * Sets TTL of SipURL
     * @param <var>ttl</var> TTL
     * @throws SipParseException if ttl is not accepted by implementation
     */
    public void setTTL(int ttl) throws SipParseException
    {
		setParameter(TTL, String.valueOf(ttl));
    }

    /**
     * Gets transport of SipURL
     * (Returns null if transport does not exist)
     * @return transport of SipURL
     */
    public String getTransport()
    {
        return getParameter(TRANSPORT);
    }

    /**
     * Gets boolean value to indicate if SipURL
     * has transport
     * @return boolean value to indicate if SipURL
     * has transport
     */
    public boolean hasTransport()
    {
        return (getParameter(TRANSPORT) != null);
    }

    /**
     * Removes transport from SipURL (if it exists)
     */
    public void removeTransport()
    {
        removeParameter(TRANSPORT);
    }

    /**
     * Sets transport of SipURL
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if transport is null
     * @throws SipParseException if transport is not accepted by
     * implementation
     */
    public void setTransport(String transport)
        throws IllegalArgumentException, SipParseException
    {
        if (transport == null || transport.length() == 0) {
       		throw new IllegalArgumentException("SipUrl: null or empty transport"); 
       	}
        // Check removed on 23.2.09 - the application should generally be able
        // to create any general URI; this should only fail when we try
        // to send the message, if it's important. Also, this is needed
        // to pass the JSR289 TCK.    -- Ayelet Dekel
//        if (!SIPConnectionsModel.instance().isTransportSupported(transport)) {
//        	throw new SipParseException("Invalid transport [" + transport + ']');
//        }
        setParameter(TRANSPORT, transport);
    }

    /**
     * Gets method of SipURL
     * (Returns null if method does not exist)
     * @return method of SipURL
     */
    public String getMethod()
    {
        return getParameter(METHOD);
    }

    /**
     * Gets boolean value to indicate if SipURL
     * has method
     * @return boolean value to indicate if SipURL
     * has method
     */
    public boolean hasMethod()
    {
        return (getParameter(METHOD) != null);
    }

    /**
     * Removes method from SipURL (if it exists)
     */
    public void removeMethod()
    {
        removeParameter(METHOD);
    }

    /**
     * Sets method of SipURL
     * @param <var>method</var> method
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if method is not accepted by implementation
     */
    public void setMethod(String method)
        throws IllegalArgumentException, SipParseException
    {
        if (method == null)
        {
       		throw new IllegalArgumentException("SipUrl: null method"); 
       	} 
        
        setParameter(METHOD, method);
    }

    /**
     * Gets MAddr of SipURL
     * (Returns null if MAddr does not exist)
     * @return MAddr of SipURL
     */
    public String getMAddr()
    {
        return getParameter(MADDR);
    }

    /**
     * Gets boolean value to indicate if SipURL
     * has MAddr
     * @return boolean value to indicate if SipURL
     * has MAddr
     */
    public boolean hasMAddr()
    {
        return (getParameter(MADDR) != null);
    }

    /**
     * Removes MAddr from SipURL (if it exists)
     */
    public void removeMAddr()
    {
        removeParameter(MADDR);
    }

    /**
     * Sets MAddr of SipURL
     * @param <var>mAddr</var> MAddr
     * @throws IllegalArgumentException if mAddr is null
     * @throws SipParseException if mAddr is not accepted by implementation
     */
    public void setMAddr(String mAddr)
        throws IllegalArgumentException, SipParseException
    {
        if (mAddr == null)
        {
       		throw new IllegalArgumentException("SipUrl: null mAddr"); 
       	} 
            
        setParameter(MADDR, mAddr);
    }

    /**
     * Sets MAddr of SipURL
     * @param <var>mAddr</var> MAddr
     * @throws IllegalArgumentException if mAddr is null
     * @throws SipParseException if mAddr is not accepted by implementation
     */
    public void setMAddr(InetAddress mAddr)
        throws IllegalArgumentException, SipParseException
    {
		if (mAddr == null)
		{
			throw new IllegalArgumentException("SipUrl: null mAddr"); 
		} 
           
        setParameter(MADDR, mAddr.getHostName());
	}

    /**
     * Returns boolean value to indicate if the SipURL
     * has a global phone user
     * @return boolean value to indicate if the SipURL
     * has a global phone user
     * @throws SipException if user type is not USER_TYPE_PHONE
     */
    public boolean isGlobal() throws SipException
    {
		if (!m_userType.equals(USER_TYPE_PHONE))
		{
			throw new SipException("SipUrl: user type not Phone");
		}
        
        return m_isGlobal;
    }

    /**
     * Sets phone user of SipURL to be global or local
     * @param <var>global</var> boolean value indicating
     * if phone user should be global
     * @throws SipException if user type is not USER_TYPE_PHONE
     */
    public void setGlobal(boolean global)
        throws SipException, SipParseException
    {
		if (!m_userType.equals(USER_TYPE_PHONE))
		{
			throw new SipException("SipUrl: user type not phone");
		}
		
        m_isGlobal = global;
    }

	// 
	// Encoding.
	//

    /**
     * Gets string representation of URI
     * @return string representation of URI
     */
    public String toString()
    {
        CharsBuffer buffer = CharsBuffersPool.getBuffer();
        writeToCharBuffer(buffer);
        
        String value = buffer.toString();
        CharsBuffersPool.putBufferBack(buffer);
        
        return value; 
    }
    
    /**
     * Dump this object to the specified char array
     * @param ret
     */
    public void writeToCharBuffer(CharsBuffer ret)
    {
		ret.append(m_scheme);
		ret.append(Separators.COLON);
		
		if(m_isGlobal) 
		{
			ret.append('+');
		}
		
		// User info.
		if(hasUserName()) 
		{
			Coder.encodeUser(m_userName, ret);
			if(hasUserPassword()) 
			{
				ret.append(Separators.COLON);			
				Coder.encodePassword(m_userPassword, ret);
			}
			
			ret.append(Separators.AT);
		}
		
		// Host and port.
		boolean ipv6 = m_host.indexOf(':') != -1;
		if (ipv6) {
			ret.append('[');
		}
		ret.append(m_host);
		if (ipv6) {
			ret.append(']');
		}
		if(hasPort()) 
		{
			ret.append(Separators.COLON);
			ret.append(m_port);
		}
		
		// Parameters.
		if (hasParameters())
		{
			ret.append(Separators.SEMICOLON);
			m_params.encode(ret, Separators.SEMICOLON, true);
		}
		
		// Headers.
		if(hasHeaders()) 
		{
			ret.append(Separators.QUESTION);
			m_headers.encode(ret, Separators.AND, true);
		}
    }
    
    /**
     * Indicates whether some other Object is "equal to" this URI
     * (Note that obj must have the same Class as this URI - this means that it
     * must be from the same JAIN SIP implementation)
     * @param <var>obj</var> the Object with which to compare this URI
     * @return true if this URI is "equal to" the obj
     * argument; false otherwise (equality of URI's is defined in RFC 2068)
     */
    public boolean equals(Object obj)
    {
    	if (this == obj)
    	{return true;}
    	
		if(obj == null || !(obj instanceof SipURLImpl)) 
		{
			return false;
		}
		
		SipURLImpl other = (SipURLImpl)obj;
		
		if (!super.equals(obj))
		{return false;}
		
		// Go over the fields. 
		if(!m_host.equalsIgnoreCase(other.m_host)) 
		{return false;} 
		
		if(m_port != other.m_port) 
		{return false;} 

		if(hasUserName()) 
		{
			if(!m_userName.equals(other.m_userName)) 
			{return false;} 
		}
		else if(other.hasUserName()) 
		{return false;} 
			
		if(hasIsdnSubAddress()) 
		{
			if(!m_isdnSubAddress.equals(other.m_isdnSubAddress)) 
			{return false;} 
		}
		else if(other.hasIsdnSubAddress()) 
		{return false;} 
		
		if(hasPostDial()) 
		{
			if(!m_postDial.equals(other.m_postDial)) 
			{return false;} 
		}
		else if(other.hasPostDial()) 
		{return false;} 
		
		if(hasUserPassword()) 
		{
			if(!m_userPassword.equals(other.m_userPassword)) 
			{return false;} 
		}
		else if(other.hasUserPassword()) 
		{return false;} 
		
		if (!areHeadersEqual(m_headers, other.m_headers))
		{return false;}

		// Check both directions to make sure we don't loose some params
		if (!areParamsEqual(m_params, other.m_params) ||
		    !areParamsEqual(other.m_params, m_params))
		{return false;}

		return true;
	}
    
    /*
         URI header components are never ignored.  Any present header
         component MUST be present in both URIs and match for the URIs
         to match.  The matching rules are defined for each header field
         in Section 20.

     */
    private static boolean areHeadersEqual(ParametersImpl p1, ParametersImpl p2)
    {
		if (p1 == null || !p1.hasParameters()) {
			if (p2 == null || !p2.hasParameters()) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (p2 == null || !p2.hasParameters() || p1.size() != p2.size()) {
				return false;
			}
		}
		
		for (int i = 0; i < p1.size(); i++) {
			Parameter param1 = p1.get(i);
			String key1 = param1.getKey();
			String val1 = param1.getValue();
			String val2 = p2.getParameter(key1);
		
			if (val2 == null) { // null meaning it doesn't exist
				return false;
			}
			else if (!val1.equalsIgnoreCase(val2)) {
				return false;
			}
		}
		return true;
    }
	
    /*
         URI uri-parameter components are compared as follows:
         -  Any uri-parameter appearing in both URIs must match.
         -  A user, ttl, or method uri-parameter appearing in only one
            URI never matches, even if it contains the default value.
         -  A URI that includes an maddr parameter will not match a URI
            that contains no maddr parameter.
         -  All other uri-parameters appearing in only one URI are
            ignored when comparing the URIs.
         Uri: Even though TRANSPORT is not mentioned in the RFC, the samples
         show that it needs to be present in both to be equal.
     */
	private static boolean areParamsEqual(ParametersImpl p1, ParametersImpl p2) {
		if (p1 == null || !p1.hasParameters()) {
			return true;
		}
		
		for (int i = 0; i < p1.size(); i++) {
			Parameter param1 = p1.get(i);
			String key1 = param1.getKey();
			String val1 = param1.getValue();
			String val2 = p2 == null
				? null
				: p2.getParameter(key1);

			if (val2 == null) {
				if (key1.equals(TTL) || 
					key1.equals(MADDR) || 
					key1.equals(METHOD) ||
					key1.equals(TRANSPORT) ||
					key1.equals(USER))
				{
					return false;
				}
			}
			else if (!val1.equalsIgnoreCase(val2)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Get the hash code for this object.
	 */
	public int hashCode()
	{
		return computeHashCode(m_scheme,
				m_userName,
				m_userPassword,
				m_host,
				m_port,
				m_params,
				m_headers);
		// Assaf : SIPIT FIX: make this proper.
//		StringBuffer ret = new StringBuffer(16);
//		ret.append(m_scheme);
//		ret.append(Separators.COLON);
//
//		if(m_isGlobal) 
//		{
//			ret.append('+');
//		}
//
//		// User info.
//		if(hasUserName()) 
//		{
//			ret.append(m_userName);
//			if(hasUserPassword()) 
//			{
//				ret.append(Separators.COLON);			
//				ret.append(m_userPassword);
//			}
//	
//			ret.append(Separators.AT);
//		}
//
//		// Host and port.
//		ret.append(m_host);
//		if(hasPort()) 
//		{
//			ret.append(Separators.COLON);
//			ret.append(m_port);
//		}
//
//		return ret.toString().hashCode();
	}
	
	/**
	 * Moti: we need a static way to compute the hashcode
	 * (in order to apply a SipURIIpml cache)
	 * @author Moti. made private due to bad hash code
	 * 
	 */
	private static int computeHashCode(String schema,String username,String passwd,
			String hostname ,int hostport, ParametersImpl optionalParams, ParametersImpl optionalHeaders)
	{
		int result = 0 ;
		if (schema != null) result ^= schema.hashCode();
		if (username != null) result ^= username.hashCode();
		if (passwd != null) result ^= passwd.hashCode();
		if (hostname != null) result ^= hostname.hashCode();
		result ^= hostport; 
		if (optionalParams != null) result ^= optionalParams.hashCode();
		if (optionalHeaders != null) result ^= optionalHeaders.hashCode();
		
		return result;
	}
	
    /**
     * Creates and returns a copy of URI
     * @return a deep copy of URI
     */
    public Object clone()
    {
		SipURLImpl clone = (SipURLImpl)super.clone();
		
		if (m_params != null)
		{
		    clone.m_params = (ParametersImpl)m_params.clone();
		}
		if (m_headers != null)
		{
		    clone.m_headers = (ParametersImpl)m_headers.clone();
		}
		
		return clone;
    }

	//
	// Url Headers
	//
	/**
	 * Gets Iterator of header names
	 * (Returns null if no headers exist)
	 * @return Iterator of header names
	 */
	public Iterator getHeaders()
	{
		if (m_headers == null) {
			return null; 
		}
		return m_headers.getParameters();
	}

	/**
	 * Gets the value of specified header
	 * (Returns null if header does not exist)
	 * @param <var>name</var> name of header to retrieve
	 * @return the value of specified header
	 * @throws IllegalArgumentException if header is null
	 */
	public String getHeader(String name) throws IllegalArgumentException
	{
		if (m_headers == null) {
			return null;
		}
		return m_headers.getParameter(name);
	}

	/**
	 * Sets value of header
	 * @param <var>name</var> name of header
	 * @param <var>value</var> value of header
	 * @throws IllegalArgumentException if name or value is null
	 * @throws SipParseException if name or value is not accepted by
	 * implementation
	 */
	public void setHeader(String name, String value)
		throws IllegalArgumentException, SipParseException
	{
		if (m_headers == null) {
			m_headers = new ParametersImpl();
		}
		m_headers.setParameter(name, value);
	}

	/**
	 * Gets boolean value to indicate if SipURL
	 * has any headers
	 * @return boolean value to indicate if SipURL
	 * has any headers
	 */
	public boolean hasHeaders()
	{
		return m_headers != null && m_headers.hasParameters();
	}

	/**
	 * Gets boolean value to indicate if SipUrl
	 * has specified header
	 * @return boolean value to indicate if SipUrl
	 * has specified header
	 * @throws IllegalArgumentException if name is null
	 */
	public boolean hasHeader(String name) throws IllegalArgumentException
	{
		return m_headers != null && m_headers.hasParameter(name);
	}

	/**
	 * Removes specified header from SipURL (if it exists)
	 * @param <var>name</var> name of header
	 * @throws IllegalArgumentException if name is null
	 */
	public void removeHeader(String name) throws IllegalArgumentException
	{
		if (m_headers != null) {
			m_headers.removeParameter(name);
		}
	}

	/**
	 * Removes all parameters from Parameters (if any exist)
	 */
	public void removeHeaders()
	{
		if (m_headers != null) {	
	    	m_headers.removeParameters();
	    }
	}

    //
    // Parameters implementation.
    //
	/**
	 * Gets the value of specified parameter
	 * (Note - zero-length String indicates flag parameter)
	 * (Returns null if parameter does not exist)
	 * @param <var>name</var> name of parameter to retrieve
	 * @return the value of specified parameter
	 * @throws IllegalArgumentException if name is null
	 */
	public String getParameter(String name) throws IllegalArgumentException
	{
		if( m_params == null) {
			return null; 
		}
		return m_params.getParameter(name);
	}

	/**
	 * Sets value of parameter
	 * (Note - zero-length value String indicates flag parameter)
	 * @param <var>name</var> name of parameter
	 * @param <var>value</var> value of parameter
	 * @throws IllegalArgumentException if name or value is null
	 * @throws SipParseException if name or value is not accepted by implementation
	 */
	public void setParameter(String name, String value)
		throws IllegalArgumentException, SipParseException
	{
		if (m_params == null) {
			m_params = new ParametersImpl(); 
		}
	 	m_params.setParameter(name, value);
	}

	/**
	 * Gets boolean value to indicate if Parameters
	 * has any parameters
	 * @return boolean value to indicate if Parameters
	 * has any parameters
	 */
	public boolean hasParameters()
	{
		return m_params != null && m_params.hasParameters();
	}

	/**
	 * Gets boolean value to indicate if Parameters
	 * has specified parameter
	 * @return boolean value to indicate if Parameters
	 * has specified parameter
	 * @throws IllegalArgumentException if name is null
	 */
	public boolean hasParameter(String name) throws IllegalArgumentException
	{
		return m_params != null && m_params.hasParameter(name);
	}

	/**
	 * Removes specified parameter from Parameters (if it exists)
	 * @param <var>name</var> name of parameter
	 * @throws IllegalArgumentException if parameter is null
	 */
	public void removeParameter(String name) throws IllegalArgumentException
	{
		if (m_params != null) {
			m_params.removeParameter(name);
		}
	}

	/**
	 * Removes all parameters from Parameters (if any exist)
	 */
	public void removeParameters()
	{
		if (m_params != null) {
			m_params.removeParameters(); 
		}
	}

	/**
	 * Gets Iterator of parameter names
	 * (Note - objects returned by Iterator are Strings)
	 * (Returns null if no parameters exist)
	 * @return Iterator of parameter names
	 */
	public Iterator getParameters()
	{
		if (m_params == null) {
			return null; 
		}
		return m_params.getParameters();
	}
	
	/**
	 * Set the list of parameters as a bunch. 
	 * This makes life much easier for the parser.
	 */
	public void setParameters(ParametersImpl params)
	{
		m_params = params;
	}
	
	/**
	 * Set the list of headers as a bunch. 
	 * This makes life much easier for the parser.
	 */
	public void setHeaders(ParametersImpl headers)
	{
		m_headers = headers;
	}
}
