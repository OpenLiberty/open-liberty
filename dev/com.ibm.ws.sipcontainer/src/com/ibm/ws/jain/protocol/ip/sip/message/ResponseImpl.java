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
package com.ibm.ws.jain.protocol.ip.sip.message;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.AllowHeader;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.ProxyAuthenticateHeader;
import jain.protocol.ip.sip.header.ServerHeader;
import jain.protocol.ip.sip.header.UnsupportedHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.header.WWWAuthenticateHeader;
import jain.protocol.ip.sip.header.WarningHeader;
import jain.protocol.ip.sip.message.Response;

import java.util.List;

import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * Sip response implementation.
 * 
 * @author Assaf Azaria, April 2003.
 */
public class ResponseImpl extends MessageImpl implements Response
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 7369918283364553763L;

	//
	// Members.
	//
	/**
	 * The status line.
	 */
	StatusLine m_statusLine = new StatusLine();
	
	//
	// Constructors.
	//
	
    /**
     *Constructor.
     */
    public ResponseImpl()
    {    	
    }

    //
    // Operations.
    //
    
	/**
	 * Removes first ViaHeader from Response's ViaHeaders.
	 */
	public void removeViaHeader()
	{
		removeHeader(ViaHeader.name, true);
	}

    
    /**
     * Gets HeaderIterator of AllowHeaders of Response.
     * (Returns null if no AllowHeaders exist)
     * @return HeaderIterator of AllowHeaders of Response
     */
    public HeaderIterator getAllowHeaders()
    {
		return getHeaders(AllowHeader.name);
    }

    /**
     * Gets boolean value to indicate if Response
     * has AllowHeaders
     * @return boolean value to indicate if Response
     * has AllowHeaders
    */
    public boolean hasAllowHeaders()
    {
		return hasHeaders(AllowHeader.name);
	}

    /**
     * Sets AllowHeaders of Response.
     * @param <var>allowHeaders</var> List of AllowHeaders to set
     * @throws IllegalArgumentException if allowHeaders is null, empty, contains
     * any elements that are null or not AllowHeaders from the same
     * JAIN SIP implementation
    */
    public void setAllowHeaders(List allowHeaders)
        throws IllegalArgumentException
    {
		setHeaders(AllowHeader.name, allowHeaders);
    }

    /**
     * Removes AllowHeaders from Response (if any exist)
     */
    public void removeAllowHeaders()
    {
		removeHeaders(AllowHeader.name);
    }

    /**
     * Gets ProxyAuthenticateHeader of Response.
     * (Returns null if no ProxyAuthenticateHeader exists)
     * @return proxy Authenticate header of Response
     */
    public ProxyAuthenticateHeader getProxyAuthenticateHeader()
    {
		try
		{
			return (ProxyAuthenticateHeader)getHeader(ProxyAuthenticateHeader.name, 
															   true);
		}
		catch (HeaderParseException e)
		{
			return null;
		}
    }

    /**
     * Gets boolean value to indicate if Response
     * has ProxyAuthenticateHeader
     * @return boolean value to indicate if Response
     * has ProxyAuthenticateHeader
     */
    public boolean hasProxyAuthenticateHeader()
    {
		return hasHeaders(ProxyAuthenticateHeader.name);
    }

    /**
     * Removes ProxyAuthenticateHeader from Response (if it exists)
     */
    public void removeProxyAuthenticateHeader()
    {
		removeHeaders(ProxyAuthenticateHeader.name);
    }

    /**
     * Sets ProxyAuthenticateHeader of Response.
     * @param <var>ProxyAuthenticateHeader</var> ProxyAuthenticateHeader to set
     * @throws IllegalArgumentException if proxyAuthenticateHeader is null
     * or not from same JAIN SIP implementation
      */
    public void setProxyAuthenticateHeader(ProxyAuthenticateHeader proxyAuthenticateHeader)
        throws IllegalArgumentException
    {
		setHeader(proxyAuthenticateHeader, true);
    }

    /**
     * Gets HeaderIterator of WWWAuthenticateHeaders of Response.
     * (Returns null if no WWWAuthenticateHeaders exist)
     * @return HeaderIterator of WWWAuthenticateHeaders of Response
     */
    public HeaderIterator getWWWAuthenticateHeaders()
    {
		return getHeaders(WWWAuthenticateHeader.name);
	}

    /**
     * Gets boolean value to indicate if Response
     * has WWWAuthenticateHeaders
     * @return boolean value to indicate if Response
     * has WWWAuthenticateHeaders
     */
    public boolean hasWWWAuthenticateHeaders()
    {
		return hasHeaders(WWWAuthenticateHeader.name);
    }

    /**
     * Removes WWWAuthenticateHeaders from Response (if any exist)
     */
    public void removeWWWAuthenticateHeaders()
    {
		removeHeaders(WWWAuthenticateHeader.name);
    }

    /**
     * Sets WWWAuthenticateHeaders of Response.
     * @param <var>wwwAuthenticateHeaders</var> List of 
     * 		WWWAuthenticateHeaders to set
     * @throws IllegalArgumentException if wwwAuthenticateHeaders is null, 
     * 				empty, contains
     * any elements that are null or not WWWAuthenticateHeaders from the same
     * JAIN SIP implementation
     */
    public void setWWWAuthenticateHeaders(List wwwAuthenticateHeaders)
        throws IllegalArgumentException
    {
		setHeaders(WWWAuthenticateHeader.name, wwwAuthenticateHeaders);
    }

    /**
     * Gets ServerHeader of Response.
     * (Returns null if no ServerHeader exists)
     * @return ServerHeader of Response
     * @throws HeaderParseException if implementation cannot parse header value
    */
    public ServerHeader getServerHeader() throws HeaderParseException
    {
		return (ServerHeader)getHeader(ServerHeader.name, true);
    }

    /**
     * Gets boolean value to indicate if Response
     * has ServerHeader
     * @return boolean value to indicate if Response
     * has ServerHeader
     */
    public boolean hasServerHeader()
    {
		return hasHeaders(ServerHeader.name);
    }

    /**
     * Removes ServerHeader from Response (if it exists)
     */
    public void removeServerHeader()
    {
		removeHeaders(ServerHeader.name);
    }

    /**
     * Sets ServerHeader of Response. Note -- according to the RFC, 
     * there could be several Server headers but the JAIN spec allows only
     * one, so we add it to the top of the list.
     * @param <var>serverHeader</var> ServerHeader to set
     * @throws IllegalArgumentException if serverHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setServerHeader(ServerHeader serverHeader)
        throws IllegalArgumentException
    {
		setHeader(serverHeader, true);
    }

    /**
     * Gets HeaderIterator of UnsupportedHeaders of Response.
     * (Returns null if no UnsupportedHeaders exist)
     * @return HeaderIterator of UnsupportedHeaders of Response
     */
    public HeaderIterator getUnsupportedHeaders()
    {
		return getHeaders(UnsupportedHeader.name);
    }

    /**
     * Gets boolean value to indicate if Response
     * has UnsupportedHeaders
     * @return boolean value to indicate if Response
     * has UnsupportedHeaders
     */
    public boolean hasUnsupportedHeaders()
    {
		return hasHeaders(UnsupportedHeader.name);
    }

    /**
     * Removes UnsupportedHeaders from Response (if any exist)
     */
    public void removeUnsupportedHeaders()
    {
		removeHeaders(UnsupportedHeader.name);
    }

    /**
     * Sets UnsupportedHeaders of Response.
     * @param <var>unsupportedHeaders</var> List of UnsupportedHeaders to set
     * @throws IllegalArgumentException if unsupportedHeaders is null, empty, 
     * contains any elements that are null or not UnsupportedHeaders 
     * from the same JAIN SIP implementation
     */
    public void setUnsupportedHeaders(List unsupportedHeaders)
        throws IllegalArgumentException
    {
		setHeaders(UnsupportedHeader.name, unsupportedHeaders);
    }

    /**
     * Gets HeaderIterator of WarningHeaders of Response.
     * (Returns null if no WarningHeaders exist)
     * @return HeaderIterator of WarningHeaders of Response
     */
    public HeaderIterator getWarningHeaders()
    {
		return getHeaders(WarningHeader.name);
    }

    /**
     * Gets boolean value to indicate if Response
     * has WarningHeaders
     * @return boolean value to indicate if Response
     * has WarningHeaders
     */
    public boolean hasWarningHeaders()
    {
		return hasHeaders(WarningHeader.name);
    }

    /**
     * Removes WarningHeaders from Response (if any exist)
     */
    public void removeWarningHeaders()
    {
		removeHeaders(WarningHeader.name);
    }

    /**
     * Sets WarningHeaders of Response.
     * @param <var>warningHeaders</var> List of WarningHeaders to set
     * @throws IllegalArgumentException if warningHeaders is null, empty, 
     * contains any elements that are null or not WarningHeaders from the same
     * JAIN SIP implementation
     */
    public void setWarningHeaders(List warningHeaders)
        throws IllegalArgumentException
    {
		setHeaders(WarningHeader.name, warningHeaders);    
	}
		
	/**
     * Gets status code of Response.
     * @return status code of Response
     * @throws SipParseException if implementation cannot parse status code
     */
    public int getStatusCode() throws SipParseException
    {
		return m_statusLine.getStatusCode();
    }

    /**
     * Sets status code of Response.
     * @param <var>statusCode</var> status code to set
     * @throws SipParseException if statusCode is not accepted by implementation
     */
    public void setStatusCode(int statusCode) throws SipParseException
    {
		if((statusCode < 100) || (statusCode > 999))
		{
			throw new SipParseException("Response: Status code out of range", "");
		}
		
		m_statusLine.setStatusCode(statusCode);
		
    }

    /**
     * Gets reason phrase of Response.
     * @return reason phrase of Response
     * @throws SipParseException if implementation cannot 
     * parse reason phrase
     */
    public String getReasonPhrase() throws SipParseException
    {
    	return m_statusLine.getReasonPhrase();
    }

    /**
     * Sets reason phrase of Response.
     * @param <var>reasonPhrase</var> reason phrase to set
     * @throws IllegalArgumentException if reasonPhrase is null
     * @throws SipParseException if reasonPhrase is not accepted 
     * by implementation
     */
    public void setReasonPhrase(String reasonPhrase)
        throws IllegalArgumentException, SipParseException
    {
		if(reasonPhrase == null)
		{
			throw new IllegalArgumentException("Response: null Reason phrase");
		}
		if(reasonPhrase.length() == 0)
		{
			throw new IllegalArgumentException("Response: Empty Reason phrase");
		}
        
        m_statusLine.setReasonPhrase(reasonPhrase);
    }
    
    /**
	 * Sets version of Message. Note that the version defaults to 2.0.
	 * (i.e. version major of 2 and version minor of 0)
	 * @param <var>versionMajor</var> version major
	 * @param <var>versionMinor</var> version minor
	 * @throws SipParseException if versionMajor or versionMinor are not 
	 * accepted by implementation
	 */
	public void setVersion(int versionMajor, int versionMinor)
		throws SipParseException
	{
		super.setVersion(versionMajor, versionMinor);
		
		m_statusLine.setSipVersion(getVersion());
	}

	/**
	 * Returns boolean value to indicate if Message is a Request.
	 * @return boolean value to indicate if Message is a Request
	 */
	public boolean isRequest()
	{
		return false;
	}

	/**
	 * Returns start line of Message
	 * @return start line of Message
	 */
	public String getStartLine()
	{
		return m_statusLine.toString();
	}
	
	/**
     * @see com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl
     * #writeStartLineToBuffer(com.ibm.ws.jain.protocol.ip.sip.message.CharsBuffer)
     */
    public void writeStartLineToBuffer(CharsBuffer buffer, boolean network) 
    {
        m_statusLine.writeToCharBuffer(buffer);
    }
	
	/**
	 * Internal: set the status line.
	 */
	public void setStatusLine(StatusLine statusLine)
	{
		m_statusLine = statusLine;
	}
	
	/**
	 * Get the hash code for this object.
	 */
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	/**
	 * Gets string representation of Message
	 * @return string representation of Message
	 */
	public String toString()
	{
		return super.toString();
	}
}
