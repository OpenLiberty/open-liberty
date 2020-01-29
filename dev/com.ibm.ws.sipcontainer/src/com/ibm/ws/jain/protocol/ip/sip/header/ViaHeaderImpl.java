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
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.ViaHeader;

import java.net.InetAddress;

import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.InetAddressCache;

/**
* Via header implementation.
*/
public class ViaHeaderImpl extends ParametersHeaderImpl implements ViaHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -8826491277443321888L;

	// 
	// Constants.
	//
	public static final String BRANCH  ="branch";
    public static final String HIDDEN  ="hidden";
   	public static final String RECEIVED="received";
	public static final String RPORT = "rport";
   	public static final String MADDR	 ="maddr";
   	public static final String TTL	 ="ttl";
	public static final int TTL_MIN = 1;
	public static final int TTL_MAX = 255;
	public static final String TLS = "TLS";
	public final static String SCTP = "SCTP";    
	
	//this parameter is used to support mixed cells with old proxy and new container, (pre 7.0.0.15)
	//in the past the container used to send dummy address and no port in the via header of the startup messages.
	//the correct behavior is to just fix it but this will regress mixed cells so for now we will read the real
	//host:port from a new parameter called address
	private static final String IBMADDRESS = "ibmaddress";
	
	//
	// Members. 
	// 
	/**
	 * The host name. 
	 */
	private String m_host;
	
	/**
	 * The port.
	 */
	private int m_port = -1; 
	
	/** 
	 * The comment.
	 */       
	private String m_comment;
	
	/**
	 * The protocol version.
	 */
	private String m_protocolVersion = "2.0";
	
	/**
	 * The transport.
	 */
	private String m_transport = UDP;

    /** true if header is created in compact form, false if full form */
    private final boolean m_compactForm;

    /**
     * default constructor
     */
    public ViaHeaderImpl() {
        this(false);
    }

    /**
     * constructor with compact/full form specification
     */
    public ViaHeaderImpl(boolean compactForm) {
        super();
        m_compactForm = compactForm;
    }

    //
    // Methods.
    //
    
    /**
    * Returns boolean value indicating if ViaHeader is hidden
    * @return boolean value indicating if ViaHeader is hidden
    */
    public boolean isHidden()
    {
    	return hasParameter(HIDDEN);
    }

    /**
     * Sets whether ViaHeader is hidden or not
     * @param hidden boolean to set
     */
    public void setHidden(boolean hidden)
    {
        if (hidden)
        {
            if (!hasParameter(HIDDEN))
            {
            	try{
					setParameter(HIDDEN, HIDDEN);
            	}
            	catch (SipParseException e){
					e. printStackTrace();
            	// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:54 PM
            	}
            } 
        }
        else
        {
        	removeParameter(HIDDEN); 
        } 
    }

    /**
    * Returns boolean value indicating if ViaHeader has port
    * @return boolean value indicating if ViaHeader has port
    */
    public boolean hasPort()
    {
        return m_port != -1;
    }

    /**
    * Gets port of ViaHeader
    * @return port of ViaHeader
    */
    public int getPort()
    {
        return m_port;
    }

    /**
     * Sets port of ViaHeader
     * @param port int to set
     * @throws SipParseException if port is not accepted by implementation
     */
    public void setPort(int port) throws SipParseException
    {
        if (port <= 0)
        {
       		throw new SipParseException("Negative port", "" + port); 
       	} 
       	m_port = port;
    }

    /**
    * Removes port from ViaHeader (if it exists)
    */
    public void removePort()
    {
    	m_port = -1;
    }

    /**
    * Gets protocol version of ViaHeader
    * @return protocol version of ViaHeader
    */
    public String getProtocolVersion()
    {
        return m_protocolVersion;
    }

    /**
     * Sets protocol version of ViaHeader
     * @param protocolVersion String to set
     * @throws IllegalArgumentException if protocolVersion is null
     * @throws SipParseException if protocolVersion is not accepted by 
     * implementation
     */
    public void setProtocolVersion(String protocolVersion)
        throws IllegalArgumentException, SipParseException
    {
        if (protocolVersion == null)
        {    
        	throw new IllegalArgumentException("Protocol version null"); 
        } 
        
        m_protocolVersion = protocolVersion;
    }

    /**
    * Gets transport of ViaHeader
    * @return transport of ViaHeader
    */
    public String getTransport()
    {
        return m_transport;
    }

    /**
     * Sets transport of ViaHeader
     * @param transport String to set
     * @throws IllegalArgumentException if transport is null
     * @throws SipParseException if transport is not accepted by
     * implementation
     */
    public void setTransport(String transport)
        throws IllegalArgumentException, SipParseException
    {
        if (transport == null)
        {    
        	throw new IllegalArgumentException("Null transport"); 
        }
        
        // transport = "UDP" / "TCP" / "TLS" / "SCTP" / other-transport
        if (transport.equalsIgnoreCase(TCP)) {
        	m_transport = TCP;
        }
        else if (transport.equalsIgnoreCase(UDP)) {
        	m_transport = UDP;
        }
        else if (transport.equalsIgnoreCase(TLS)) {
        	m_transport = TLS;
        }
        else if (transport.equalsIgnoreCase(SCTP)) {
        	m_transport = SCTP;
        }
        else {
        	// do not throw a SipParseExcetpion.
        	// other transports are allowed here,
        	// even if not supported by this stack.
        	m_transport = transport;
        }
    }

    /**
    * Gets host of ViaHeader
    * @return host of ViaHeader
    */
    public String getHost()
    {
        return m_host;
    }

    /**
     * Sets host of ViaHeader
     * @param host String to set
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setHost(String host)
        throws IllegalArgumentException, SipParseException
    {
		if (host == null)
        {    
        	throw new IllegalArgumentException("Via: Null host"); 
        } 
        
        m_host = host;
    }

    /**
     * Sets host of ViaHeader
     * @param host InetAddress to set
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setHost(InetAddress host)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {    
        	throw new IllegalArgumentException("Via: null host"); 
        } 
        
        m_host = InetAddressCache.getHostAddress(host);
	}

    /**
    * Gets comment of ViaHeader
    * @return comment of ViaHeader
    */
    public String getComment()
    {
    	return m_comment;
    }

    /**
    * Gets boolean value to indicate if ViaHeader
    * has comment
    * @return boolean value to indicate if ViaHeader
    * has comment
    */
    public boolean hasComment()
    {
    	return m_comment != null;
    }

    /**
     * Sets comment of ViaHeader
     * @param comment String to set
     * @throws IllegalArgumentException if comment is null
     * @throws SipParseException if comment is not accepted by implementation
     */
    public void setComment(String comment)
        throws IllegalArgumentException, SipParseException
    {
		if (comment == null)
        {
        	throw new IllegalArgumentException("Via: null comment"); 
        } 
        if (comment.length() == 0)
		{            
			throw new IllegalArgumentException("Via: empty comment"); 
		} 
        
        m_comment = comment;
    }

    /**
    * Removes comment from ViaHeader (if it exists)
    */
    public void removeComment()
    {
    	m_comment = null;
    }

    /**
     * Sets TTL of ViaHeader
     * @param <var>ttl</var> TTL
     * @throws SipParseException if ttl is not accepted by implementation
     */
    public void setTTL(int ttl) throws SipParseException
    {
        if ((TTL_MIN <= ttl) && (ttl <= TTL_MAX))
        {
        	try{
				setParameter(TTL, String.valueOf(ttl));
        	}catch(SipParseException e){
        		// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:54 PM
        		e.printStackTrace();
        	// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:59 PM
        	}
       	}
        else
        {
			throw new SipParseException("Via: invalid ttl");
        }
         
    }

    /**
    * Removes TTL from ViaHeader (if it exists)
    */
    public void removeTTL()
    {
    	removeParameter(TTL);
    }

    /**
    * Gets TTL of ViaHeader
    * @return TTL of ViaHeader
    */
    public int getTTL()
    {
        String ttl = getParameter(TTL);
		if(ttl == null) 
		{
			return -1;
		} 
		
		return Integer.parseInt(ttl);
	}

    /**
    * Gets boolean value to indicate if ViaHeader
    * has TTL
    * @return boolean value to indicate if ViaHeader
    * has TTL
    */
    public boolean hasTTL()
    {
        return hasParameter(TTL);
    }

    /**
     * Sets MAddr of ViaHeader
     * @param mAddr String to set
     * @throws IllegalArgumentException if mAddr is null
     * @throws SipParseException if mAddr is not accepted by implementation
     */
    public void setMAddr(String mAddr)
        throws IllegalArgumentException, SipParseException
    {
        if (mAddr == null)
        {    
        	throw new IllegalArgumentException("Via: null mAddr"); 
        } 
        
		try{
			setParameter(MADDR, mAddr);
		}
		catch(SipParseException e){
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:54 PM
			e.printStackTrace();
		// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:59 PM
		}
	}
	

    /**
     * Sets MAddr of ViaHeader
     * @param mAddr InetAddress to set
     * @throws IllegalArgumentException if mAddr is null
     * @throws SipParseException if mAddr is not accepted by implementation
     */
    public void setMAddr(InetAddress mAddr)
        throws IllegalArgumentException, SipParseException
    {
		if (mAddr == null)
		{    
			throw new IllegalArgumentException("Via: null mAddr"); 
		} 
        
        String host = InetAddressCache.getHostAddress(mAddr);
        if (host == null)
        {        
        	throw new IllegalArgumentException("Via: null host address"); 
        } 
            
		try{
			setParameter(MADDR, host);
		}
		catch(SipParseException e){
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:59 PM
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:54 PM
			e.printStackTrace();
		// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 5:01 PM
		}
    }

    /**
    * Gets boolean value to indicate if ViaHeader
    * has MAddr
    * @return boolean value to indicate if ViaHeader
    * has MAddr
    */
    public boolean hasMAddr()
    {
    	return hasParameter(MADDR);
    }

    /**
    * Removes MAddr from ViaHeader (if it exists)
    */
    public void removeMAddr()
    {
    	removeParameter(MADDR);
    }

    /**
    * Gets MAddr of ViaHeader
    * @return MAddr of ViaHeader
    */
    public String getMAddr()
    {
        return getParameter(MADDR);
   	}

    /**
     * Sets received of ViaHeader
     * @param received String to set
     * @throws IllegalArgumentException if received is null
     * @throws SipParseException if received is not accepted by the implementation
     */
    public void setReceived(String received)
        throws IllegalArgumentException, SipParseException
    {
        if (received == null)
        {
    	    throw new IllegalArgumentException("Via: Null received"); 
    	} 
        
		try{
			setParameter(RECEIVED, received);
		}
		catch(SipParseException e){
			e.printStackTrace();
		// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:53 PM
		} 
       
    }

    /**
     * Sets received of ViaHeader
     * @param received InetAddress to set
     * @throws IllegalArgumentException if received is null
     * @throws SipParseException if received is not accepted by implementation
     */
    public void setReceived(InetAddress received)
        throws IllegalArgumentException, SipParseException
    {
        if (received == null)
        { 
        	throw new IllegalArgumentException("Via: null received"); 
        } 
        String host = received.getHostName();
        if (host == null)
        {    
        	throw new IllegalArgumentException("Via: null host"); 
        } 
        
		// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:54 PM
		try{
			setParameter(RECEIVED, host);
		}
		catch(SipParseException e){
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:59 PM
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 5:01 PM
			e.printStackTrace();
		// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 5:02 PM
		}
    }

    /**
    * Removes received from ViaHeader (if it exists)
    */
    public void removeReceived()
    {
        removeParameter(RECEIVED);
    }

    /**
    * Gets received of ViaHeader
    * @return received of ViaHeader
    */
    public String getReceived()
    {
        return getParameter(RECEIVED);
    }

    /**
    * Gets boolean value to indicate if ViaHeader
    * has received
    *
    * @return boolean value to indicate if ViaHeader
    * has received
    */
    public boolean hasReceived()
    {
        return hasParameter(RECEIVED);
    }

    /**
     * Sets branch of ViaHeader
     *
     * @param branch String to set
     * @throws IllegalArgumentException if branch is null
     * @throws SipParseException if branch is not accepted by implementation
     */
    public void setBranch(String branch)
        throws IllegalArgumentException, SipParseException
    {
        if (branch == null)
        {    
        	throw new IllegalArgumentException("Via: null branch"); 
        } 
    	
		try{
			setParameter(BRANCH, branch);
		}
		catch(SipParseException e){
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:53 PM
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 5:01 PM
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:59 PM
			e.printStackTrace();
		// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 5:02 PM
		}    
    }

    /**
    * Removes branch from ViaHeader (if it exists)
    */
    public void removeBranch()
    {
        removeParameter(BRANCH);
    }

    /**
    * Gets branch of ViaHeader
    * @return branch of ViaHeader
    */
    public String getBranch()
    {
    	return getParameter(BRANCH);
    }

    /**
    * Gets boolean value to indicate if ViaHeader
    * has branch
    * @return boolean value to indicate if ViaHeader
    * has branch
    */
    public boolean hasBranch()
    {
    	return hasParameter(BRANCH);
    }

	/**
	 * sets the rport parameter
	 * @param rport the rport value, or -1 if just a flag
	 */
	public void setRPort(int rport) {
		String value = rport == -1 ? "" : Integer.toString(rport);
		try {
			setParameter(RPORT, value);
		}
		catch (SipParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @see jain.protocol.ip.sip.header.ViaHeader#setRPort()
	 */
	public void setRPort() {
		setRPort(-1);
	}

	/**
	 * @see jain.protocol.ip.sip.header.ViaHeader#getRPort()
	 */
	public int getRPort() {
		String rportParam = getParameter(RPORT);
		int rport;
		if (rportParam == null || rportParam.length() == 0) {
			rport = -1;
		}
		else try {
			rport = Integer.parseInt(rportParam);
		}
		catch (NumberFormatException e) {
			rport = -1;
		}
		return rport;
	}

	// 
	// Encoding.
	//
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// protocol name and version
		parser.skip(SLASH); // skip "SIP"
		parser.lws();
        String protVersion = parser.parseSipVersion(SLASH);
        setProtocolVersion(protVersion);
        parser.lws();
        parser.match(SLASH);

        // transport
        String transport = parser.nextToken(SP);
        setTransport(transport);
        parser.lws();

        // sent-by
        String host;
        if (parser.LA() == '[') {
        	// IPv6
        	parser.consume();
        	host = parser.nextToken(']');
        	parser.consume();
        }
        else {
        	// IPv4 or hostname
	        char[] separators = { COLON, SEMICOLON };
	        host = parser.nextToken(separators);
        }
        setHost(host.trim());

        // port
        if (parser.LA(1) == COLON) {
        	parser.match(COLON);
        	parser.lws();
            setPort(parser.number());
            parser.lws();
        }
        else {
        	removePort();
        }

        // parameters
        super.parseValue(parser);

        String address = getParameter(IBMADDRESS);
        if (address != null && address.length() > 0){
        	//we found address parameter that was added by the proxy 
        	//we will replace the host:port with the address parameter values that represent the real
        	//proxy host and port
        	int colonIdx = address.lastIndexOf(COLON);
        	if (colonIdx > -1){
        		try {
					String portStr = address.substring(colonIdx + 1);
					int port = Integer.parseInt(portStr);
					setPort(port);
					//check if this is ipv6 address and if so remove the brackets
					String ibmHost = address.substring(0, colonIdx);
					if (ibmHost.charAt(0) == '['){
						ibmHost = ibmHost.substring(1, ibmHost.length() - 1);
					}
					setHost(ibmHost);
				} catch (NumberFormatException e) {
					throw new RuntimeException(e);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				}
        	}
        }

        // comment
        if (parser.LA(1) == LPAREN) {
        	parser.match(LPAREN);
            setComment(parser.nextToken(RPAREN));
            parser.match(RPAREN);
        }
        else {
        	removeComment();
        }
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer ret)
	{
		// protocol name and version
		ret.append("SIP");		
		ret.append(SLASH);
		ret.append(m_protocolVersion);
		
		// transport
		ret.append(SLASH);
		ret.append(m_transport);

		// sent-by
		ret.append(SP);
		boolean ipv6 = m_host.indexOf(':') != -1;
		if (ipv6) {
			ret.append('[');
		}
		ret.append(m_host);
		if (ipv6) {
			ret.append(']');
		}
		
		// port
		if (hasPort()) {
			ret.append(COLON);
			ret.append(m_port);
		}
		
		// parameters
		super.encodeValue(ret);
		
		// comment
		if (hasComment()) 
		{
			ret.append(LPAREN);
			ret.append(m_comment);
			ret.append(RPAREN);
		}
	} 
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!super.valueEquals(other)) {
			return false;
		}
		if (!(other instanceof ViaHeaderImpl)) {
			return false;
		}
		ViaHeaderImpl o = (ViaHeaderImpl)other;
		
		if (m_port != o.m_port) {
			return false;
		}

		if (m_host == null || m_host.length() == 0) {
			if (o.m_host != null && o.m_host.length() > 0) {
				return false;
			}
		}
		else {
			if (o.m_host == null || o.m_host.length() == 0) {
				return false;
			}
		}

		if (m_transport == null || m_transport.length() == 0) {
			if (o.m_transport != null && o.m_transport.length() > 0) {
				return false;
			}
		}
		else {
			if (o.m_transport == null || o.m_transport.length() == 0) {
				return false;
			}
		}

		if (m_protocolVersion == null || m_protocolVersion.length() == 0) {
			if (o.m_protocolVersion != null && o.m_protocolVersion.length() > 0) {
				return false;
			}
		}
		else {
			if (o.m_protocolVersion == null || o.m_protocolVersion.length() == 0) {
				return false;
			}
		}

		if (m_comment == null || m_comment.length() == 0) {
			if (o.m_comment == null || o.m_comment.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_comment == null || o.m_comment.length() == 0) {
				return false;
			}
			else {
				return m_comment.equals(o.m_comment);
			}
		}
	}
	
	/**
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creates and returns a copy of Header
	 * @returns a copy of Header
	 */
	public Object clone()
	{
		// This is required in case someone will inherit 
		// from this class.
		return super.clone(); 
	}
	
	/**
	 * determines whether or not this header can have nested values
	 */
	public boolean isNested() {
		return true;
	}

	/**
	 * @return the separator preceeding the list of parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SEMICOLON;
	}
	
	/**
	 * @return the separator between parameters
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return SEMICOLON;
	}

    /**
     * @return true if parameters should be escaped
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#escapeParameters()
     */
    protected boolean escapeParameters() {
    	return false;
    }
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName(boolean)
	 */
	@Override
	public String getName(boolean isUseCompactHeaders) {
		if (isUseCompactHeaders){
			return String.valueOf(SipConstants.VIA_SHORT);
		}
		return getName();
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isCompactFormSupported()
	 */
	public boolean isCompactFormSupported() {
		return true;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isCompactForm()
	 */
	public boolean isCompactForm() {
		return m_compactForm;
	}
}
